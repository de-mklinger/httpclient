package de.mklinger.commons.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import de.mklinger.commons.httpclient.HttpRequest.BodyProvider;
import de.mklinger.commons.httpclient.internal.ByteArrayBodyProvider;
import de.mklinger.commons.httpclient.internal.ByteBufferBodyProvider;
import de.mklinger.commons.httpclient.internal.FileBodyProvider;
import de.mklinger.commons.httpclient.internal.InputStreamBodyProvider;
import de.mklinger.commons.httpclient.internal.NoBodyProvider;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class BodyProviders {
	private BodyProviders() {
	}

	public static HttpRequest.BodyProvider noBody() {
		return NoBodyProvider.getInstance();
	}

	public static HttpRequest.BodyProvider fromByteArray(final byte[] b) {
		return new ByteArrayBodyProvider(b);
	}

	public static HttpRequest.BodyProvider fromByteArray(String contentType, final byte[] b) {
		return new ByteArrayBodyProvider(contentType, b);
	}

	public static HttpRequest.BodyProvider fromByteBuffer(final ByteBuffer byteBuffer) {
		return new ByteBufferBodyProvider(byteBuffer);
	}

	public static HttpRequest.BodyProvider fromByteBuffer(String contentType, final ByteBuffer byteBuffer) {
		return new ByteBufferBodyProvider(contentType, byteBuffer);
	}

	public static BodyProvider fromFile(final Path file) {
		try {
			return new FileBodyProvider(file);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static BodyProvider fromFile(String contentType, final Path file) {
		try {
			return new FileBodyProvider(contentType, file);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static BodyProvider fromInputStream(final InputStream inputStream) {
		return new InputStreamBodyProvider(inputStream);
	}

	public static BodyProvider fromInputStream(String contentType, final InputStream inputStream) {
		return new InputStreamBodyProvider(contentType, inputStream);
	}
}
