package de.mklinger.commons.httpclient.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class InputStreamBodyProvider implements HttpRequest.BodyProvider {
	private static final int CHUNK_SIZE = 1024 * 4;

	private final Supplier<InputStream> inputStreamSupplier;

	public InputStreamBodyProvider(final Supplier<InputStream> inputStreamSupplier) {
		this.inputStreamSupplier = inputStreamSupplier;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		return new InputStreamIterator(inputStreamSupplier.get());
	}

	@Override
	public long contentLength() {
		return -1;
	}

	private static class InputStreamIterator implements Iterator<CompletableFuture<ByteBuffer>> {
		private final InputStream inputStream;
		private final AtomicBoolean reading;
		private final AtomicBoolean done;
		private final byte[] buf;

		private InputStreamIterator(final InputStream inputStream) {
			this.inputStream = inputStream;
			this.reading = new AtomicBoolean(false);
			this.done = new AtomicBoolean(false);
			this.buf = new byte[CHUNK_SIZE];
		}

		@Override
		public boolean hasNext() {
			if (reading.get()) {
				throw new IllegalStateException("Reading in progress");
			}

			return !done.get();
		}

		@Override
		public CompletableFuture<ByteBuffer> next() {
			if (!reading.compareAndSet(false, true)) {
				throw new IllegalStateException("Reading in progress");
			}

			// Read blocking :-(

			final ByteBuffer byteBuffer;
			try {
				final int numRead = inputStream.read(buf, 0, buf.length);
				if (numRead == -1) {
					done.set(true);
					byteBuffer = ByteBuffer.allocate(0);
				} else if (numRead == 0) {
					byteBuffer = ByteBuffer.allocate(0);
				} else {
					byteBuffer = ByteBuffer.allocate(numRead);
					byteBuffer.put(buf, 0, numRead);
					byteBuffer.flip();
				}
				unsetReading();
			} catch (final Throwable e) {
				throw closeStreamAndRethrowUncheckedAfterRead(e);
			}

			return CompletableFuture.completedFuture(byteBuffer);
		}

		private RuntimeException closeStreamAndRethrowUncheckedAfterRead(final Throwable e1) {
			final RuntimeException ex;
			if (e1 instanceof IOException) {
				ex = new UncheckedIOException((IOException) e1);
			} else {
				ex = new RuntimeException(e1);
			}
			try {
				inputStream.close();
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
