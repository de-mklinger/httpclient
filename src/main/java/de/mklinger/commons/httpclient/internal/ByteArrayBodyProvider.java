package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public final class ByteArrayBodyProvider implements HttpRequest.BodyProvider {
	private final byte[] b;

	public ByteArrayBodyProvider(final byte[] b) {
		this.b = b;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		return Collections.singleton(
				CompletableFuture.completedFuture(ByteBuffer.wrap(b))
				).iterator();
	}

	@Override
	public long contentLength() {
		return b.length;
	}
}