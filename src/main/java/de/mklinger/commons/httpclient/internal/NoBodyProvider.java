package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import de.mklinger.commons.httpclient.HttpRequest;

public class NoBodyProvider implements HttpRequest.BodyProvider {
	private static final NoBodyProvider INSTANCE = new NoBodyProvider();

	public static NoBodyProvider getInstance() {
		return INSTANCE;
	}

	private NoBodyProvider() {}

	@Override
	public long contentLength() {
		return 0;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		throw new UnsupportedOperationException();
	}
}