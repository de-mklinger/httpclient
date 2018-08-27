package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import de.mklinger.commons.httpclient.HttpRequest;
import de.mklinger.commons.httpclient.HttpRequest.BodyProvider;

public class ContentTypeBodyProvider implements HttpRequest.BodyProvider {
	private final String contentType;
	private final HttpRequest.BodyProvider delegate;

	public ContentTypeBodyProvider(final String contentType, final BodyProvider delegate) {
		this.contentType = Objects.requireNonNull(contentType);
		this.delegate = delegate;
	}

	@Override
	public Iterator<CompletableFuture<ByteBuffer>> iterator() {
		return delegate.iterator();
	}

	@Override
	public void forEach(final Consumer<? super CompletableFuture<ByteBuffer>> action) {
		delegate.forEach(action);
	}

	@Override
	public long contentLength() {
		return delegate.contentLength();
	}

	@Override
	public Optional<String> contentType() {
		return Optional.of(contentType);
	}

	@Override
	public Spliterator<CompletableFuture<ByteBuffer>> spliterator() {
		return delegate.spliterator();
	}
}