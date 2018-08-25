package de.mklinger.commons.httpclient;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.mklinger.commons.httpclient.HttpResponse.BodyHandler;
import de.mklinger.commons.httpclient.internal.ByteArrayCompleteListener;
import de.mklinger.commons.httpclient.internal.DiscardBodyCompleteListener;
import de.mklinger.commons.httpclient.internal.FileCompleteListener;
import de.mklinger.commons.httpclient.internal.StringBodyHandler;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class BodyHandlers {
	private BodyHandlers() {
	}

	public static BodyHandler<Path> asFile(final Path file, final OpenOption... openOptions) {
		return (statusCode, responseHeaders) -> new FileCompleteListener(file, openOptions);
	}

	public static BodyHandler<Path> asFile(final Path file) {
		return asFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
	}

	public static BodyHandler<byte[]> asByteArray() {
		return (statusCode, responseHeaders) -> new ByteArrayCompleteListener();
	}

	public static BodyHandler<String> asString() {
		return new StringBodyHandler();
	}

	public static <U> BodyHandler<U> discard(final U value) {
		return (statusCode, responseHeaders) -> new DiscardBodyCompleteListener<>(value);
	}

	public static BodyHandler<Void> discard() {
		return discard(null);
	}
}
