package de.mklinger.commons.httpclient.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FileCompleteListener implements HttpResponse.BodyCompleteListener<Path> {
	private static final Logger LOG = LoggerFactory.getLogger(FileCompleteListener.class);

	private final Path targetFile;
	private final OpenOption[] openOptions;
	private volatile SeekableByteChannel channel;
	private final CompletableFuture<Path> result;

	public FileCompleteListener(final Path targetFile, final OpenOption... openOptions) {
		this.targetFile = targetFile;
		this.openOptions = openOptions;
		this.result = new CompletableFuture<>();
	}

	public CompletableFuture<Path> getResult() {
		return result;
	}

	@Override
	public void onNext(final ByteBuffer content) throws IOException {
		if (channel == null) {
			// We expect this method not to be called concurrently. Thus, no locking needed here.
			channel = Files.newByteChannel(targetFile, openOptions);
		}

		LOG.info("Writing {} bytes", content.remaining());
		channel.write(content);
	}

	@Override
	public void onComplete() throws Exception {
		close();
	}

	@Override
	public Path getBody() throws Exception {
		return targetFile;
	}

	@Override
	public void close() throws Exception {
		final SeekableByteChannel c = channel;
		channel = null;
		if (c != null) {
			c.close();
		}
	}
}
