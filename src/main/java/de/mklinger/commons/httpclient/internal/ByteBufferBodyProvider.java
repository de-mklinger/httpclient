package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public final class ByteBufferBodyProvider implements HttpRequest.BodyProvider {
	private final long contentLength;
	private final ByteBuffer byteBuffer;

	public ByteBufferBodyProvider(final ByteBuffer byteBuffer) {
		this.contentLength = byteBuffer.remaining();
		this.byteBuffer = byteBuffer;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		return Collections.singleton(
				CompletableFuture.completedFuture(byteBuffer)
				).iterator();
	}

	@Override
	public long contentLength() {
		return contentLength;
	}
}