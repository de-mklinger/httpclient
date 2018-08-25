package de.mklinger.commons.httpclient;

import java.nio.file.Path;

import de.mklinger.commons.httpclient.HttpRequest.BodyProvider;
import de.mklinger.commons.httpclient.internal.ByteArrayBodyProvider;
import de.mklinger.commons.httpclient.internal.FileBodyProvider;
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

	public static BodyProvider fromFile(final Path file) {
		return new FileBodyProvider(file);
	}
}
