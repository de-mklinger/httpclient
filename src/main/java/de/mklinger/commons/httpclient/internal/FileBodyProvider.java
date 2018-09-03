package de.mklinger.commons.httpclient.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FileBodyProvider implements HttpRequest.BodyProvider {
	private static final int CHUNK_SIZE = 1024 * 100;

	private static final Logger LOG = LoggerFactory.getLogger(FileBodyProvider.class);

	private final Path file;

	public FileBodyProvider(final Path file) {
		this.file = file;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		try {
			// FIXME how can we close this safely?
			try {
				final AsynchronousFileChannel channel = AsynchronousFileChannel.open(file, StandardOpenOption.READ);
				return new AsynchronousFileChannelIterator(channel);
			} catch (final UnsupportedOperationException e) {
				LOG.info("Falling back to blocking file channel to read from {}", file);
				final FileChannel channel = FileChannel.open(file,  StandardOpenOption.READ);
				return new FileChannelIterator(channel);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public long contentLength() {
		try {
			return Files.size(file);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static class AsynchronousFileChannelIterator implements Iterator<CompletableFuture<ByteBuffer>> {
		private final AsynchronousFileChannel channel;
		private final AtomicLong position;
		private final AtomicBoolean reading;

		private AsynchronousFileChannelIterator(final AsynchronousFileChannel channel) {
			this.channel = channel;
			this.position = new AtomicLong(0L);
			this.reading = new AtomicBoolean(false);
		}

		@Override
		public boolean hasNext() {
			if (reading.get()) {
				throw new IllegalStateException("Reading in progress");
			}

			try {
				final boolean hasNext = channel.isOpen() && position.get() < channel.size();
				if (!hasNext) {
					LOG.debug("Closing channel in hasNext()");
					channel.close();
				}
				return hasNext;
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public CompletableFuture<ByteBuffer> next() {
			if (!reading.compareAndSet(false, true)) {
				throw new IllegalStateException("Reading in progress");
			}

			final CompletableFuture<ByteBuffer> cf = new CompletableFuture<>();
			final ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);

			LOG.debug("Start reading chunk at position {}", position.get());

			readChannel(
					byteBuffer,
					position.get(),
					result -> handleReadResult(cf, byteBuffer, result),
					error -> closeChannelOnError(cf, error));

			return cf;
		}

		private void readChannel(final ByteBuffer byteBuffer, final long position, final IntConsumer onCompleted, final Consumer<Throwable> onFailed) {
			channel.read(byteBuffer, position, null, new CompletionHandler<Integer, Void>() {
				@Override
				public void completed(final Integer result, final Void attachment) {
					onCompleted.accept(result);
				}

				@Override
				public void failed(final Throwable exc, final Void attachment) {
					onFailed.accept(exc);
				}
			});
		}

		private void handleReadResult(final CompletableFuture<ByteBuffer> cf, final ByteBuffer byteBuffer, final int result) {
			try {
				LOG.debug("Done reading chunk at position {}", position.get());

				if (!reading.compareAndSet(true, false)) {
					throw new IllegalStateException("Reading was not in progress");
				}

				LOG.debug("Result: {}", result);
				if (result == -1) {
					throw new IOException("Unable to read another chunk");
				}

				byteBuffer.flip();
				final int remaining = byteBuffer.remaining();
				position.addAndGet(remaining);

				LOG.debug("Read {} bytes from file", remaining);
				assert remaining == result;

				// This completes the future on the thread this callback is running in, which is
				// a thread provided by AsynchronousFileChannel
				cf.complete(byteBuffer);

			} catch (final Throwable e) {
				closeChannelOnError(cf, e);
			}
		}

		private void closeChannelOnError(final CompletableFuture<?> cf, final Throwable error) {
			Throwable e = error;
			try {
				LOG.debug("Closing channel on error");
				channel.close();
			} catch (final Throwable ex) {
				if (e == null) {
					e = ex;
				} else {
					e.addSuppressed(ex);
				}
			}
			cf.completeExceptionally(e);
		}
	}


	private static class FileChannelIterator implements Iterator<CompletableFuture<ByteBuffer>> {
		private final FileChannel channel;
		private final AtomicBoolean reading;

		private FileChannelIterator(final FileChannel channel) {
			this.channel = channel;
			this.reading = new AtomicBoolean(false);
		}

		@Override
		public boolean hasNext() {
			if (reading.get()) {
				throw new IllegalStateException("Reading in progress");
			}

			try {
				final boolean hasNext = channel.isOpen() && channel.position() < channel.size();
				if (!hasNext) {
					LOG.debug("Closing channel in hasNext()");
					channel.close();
				}
				return hasNext;
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public CompletableFuture<ByteBuffer> next() {
			if (!reading.compareAndSet(false, true)) {
				throw new IllegalStateException("Reading in progress");
			}

			// Read blocking :-(
			final ByteBuffer byteBuffer = ByteBuffer.allocate(CHUNK_SIZE);
			try {
				channel.read(byteBuffer);
				byteBuffer.flip();
				unsetReading();
			} catch (final Throwable e) {
				closeChannelAndRethrowUncheckedAfterRead(e);
			}

			return CompletableFuture.completedFuture(byteBuffer);
		}

		private void closeChannelAndRethrowUncheckedAfterRead(final Throwable e1) {
			final RuntimeException ex;
			if (e1 instanceof IOException) {
				ex = new UncheckedIOException((IOException) e1);
			} else {
				ex = new RuntimeException(e1);
			}
			try {
				channel.close();
			} catch (final Throwable e2) {
				ex.addSuppressed(e2);
			}
			try {
				unsetReading();
			} catch (final Throwable e2) {
				ex.addSuppressed(e2);
			}
			throw ex;
		}

		private void unsetReading() {
			if (!reading.compareAndSet(true, false)) {
				throw new IllegalStateException("Reading was not in progress");
			}
		}
	}
}
