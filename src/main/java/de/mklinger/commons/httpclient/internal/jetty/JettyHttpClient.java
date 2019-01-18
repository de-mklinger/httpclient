package de.mklinger.commons.httpclient.internal.jetty;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http2.HTTP2Session;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpClient;
import de.mklinger.commons.httpclient.HttpRequest;
import de.mklinger.commons.httpclient.HttpRequest.BodyProvider;
import de.mklinger.commons.httpclient.HttpResponse;
import de.mklinger.commons.httpclient.HttpResponse.BodyHandler;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class JettyHttpClient implements HttpClient {
	private static final Logger LOG = LoggerFactory.getLogger(JettyHttpClient.class);

	private final org.eclipse.jetty.client.HttpClient jettyClient;
	private volatile boolean closed = false;

	public JettyHttpClient(final org.eclipse.jetty.client.HttpClient jettyClient) {
		this.jettyClient = jettyClient;
		this.jettyClient.addEventListener(new SessionCountListener());
	}

	// For gauge metrics
	private static final AtomicLong openSessions = new AtomicLong();

	private org.eclipse.jetty.client.HttpClient getJettyClient() {
		if (closed) {
			throw new IllegalStateException("Closed");
		}
		return jettyClient;
	}

	private final class SessionCountListener implements Container.InheritedListener {
		@Override
		public void beanAdded(final Container parent, final Object child) {
			if (child instanceof HTTP2Session) {
				final HTTP2Session session = (HTTP2Session) child;
				LOG.info("Opened HTTP/2 session: {}", session.getEndPoint().getRemoteAddress());
				openSessions.incrementAndGet();
			}
		}

		@Override
		public void beanRemoved(final Container parent, final Object child) {
			if (child instanceof HTTP2Session) {
				final HTTP2Session session = (HTTP2Session) child;
				LOG.info("Closed HTTP/2 session: {}", session.getEndPoint().getRemoteAddress());
				openSessions.decrementAndGet();
			}
		}
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request, final BodyHandler<T> responseBodyHandler) {
		try {

			final Request jettyRequest = getJettyClient().newRequest(request.uri())
					.method(request.method());

			applyTimeout(request, jettyRequest);
			applyHeaders(request, jettyRequest);
			applyBody(request, jettyRequest);

			//final Executor completionExecutor = getJettyClient().getExecutor();
			final Executor completionExecutor = ForkJoinPool.commonPool();
			final FullCompleteListener<T> fullCompleteListener = new FullCompleteListener<>(completionExecutor, responseBodyHandler);

			final CompleteListener possibleTimeoutCompleteListener = applyTimeout(request, jettyRequest, fullCompleteListener);

			LOG.debug("Sending jetty request");
			jettyRequest.send(possibleTimeoutCompleteListener);

			return fullCompleteListener.getResult()
					.thenApply(this::toHttpResponse);

		} catch (final Throwable e) {
			// TODO is this a good pattern? Better directly throw?
			final CompletableFuture<HttpResponse<T>> errorResult = new CompletableFuture<>();
			errorResult.completeExceptionally(e);
			return errorResult;
		}
	}

	private void applyTimeout(final HttpRequest request, final Request jettyRequest) {
		final Optional<Duration> timeout = request.timeout();
		if (!timeout.isPresent()) {
			return;
		}

		// Jetty client impl uses millis internally. No need for more precision here.
		// See org.eclipse.jetty.client.HttpRequest.timeout(long, TimeUnit)
		try {
			jettyRequest.timeout(timeout.get().toMillis(), TimeUnit.MILLISECONDS);
		} catch (final ArithmeticException ex) {
			jettyRequest.timeout(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		}
	}

	private <T> CompleteListener applyTimeout(final HttpRequest request, final Request jettyRequest, final FullCompleteListener<T> fullCompleteListener) {
		if (request.timeout().isPresent()) {
			// TODO underlying implementation supports more than millis
			return new TimeoutResponseListener(
					fullCompleteListener,
					jettyRequest,
					request.timeout().get().toMillis(),
					TimeUnit.MILLISECONDS,
					getJettyClient().getScheduler());
		} else {
			return fullCompleteListener;
		}
	}

	private void applyHeaders(final HttpRequest request, final Request jettyRequest) {
		request.headers().map().forEach(
				(name, values) -> values.forEach(
						value -> jettyRequest.header(name, value)));
	}

	private void applyBody(final HttpRequest request, final Request jettyRequest) {
		final Optional<BodyProvider> optionalBodyProvider = request.bodyProvider();
		if (!optionalBodyProvider.isPresent()) {
			return;
		}

		final BodyProvider bodyProvider = optionalBodyProvider.get();

		final long contentLength = getContentLength(bodyProvider);
		if (contentLength == 0) {
			return;
		}

		final Optional<String> bodyProviderContentType = bodyProvider.contentType();
		if (bodyProviderContentType.isPresent() && jettyRequest.getHeaders().get("Content-Type") == null) {
			jettyRequest.header("Content-Type", bodyProviderContentType.get());
		}

		final DeferredContentProvider deferredContentProvider = new DeferredContentProvider() {
			@Override
			public long getLength() {
				return contentLength;
			}
		};

		final RequestBodyFiller requestBodyFiller = new RequestBodyFiller(bodyProvider.iterator(), deferredContentProvider, jettyRequest);
		requestBodyFiller.start();

		jettyRequest.content(deferredContentProvider);
	}

	private static class RequestBodyFiller {
		private final Iterator<CompletableFuture<ByteBuffer>> chunkFutureIterator;
		private final DeferredContentProvider deferredContentProvider;
		private final Request jettyRequest;
		private final AtomicReference<Throwable> error = new AtomicReference<>();

		private final Object pendingLock = new Object();
		private long pendingOffers = 0L;
		private boolean pendingRead = false;

		private final static long MAX_PENDING_OFFERS = 5;

		private final Callback offerCallback = new Callback() {
			@Override
			public void succeeded() {
				synchronized (pendingLock) {
					pendingOffers--;
					fillIfPossible();
				}
			}

			@Override
			public void failed(final Throwable e) {
				synchronized (pendingLock) {
					pendingOffers--;
				}
				error(e);
			}
		};

		public RequestBodyFiller(final Iterator<CompletableFuture<ByteBuffer>> chunkFutureIterator, final DeferredContentProvider deferredContentProvider, final Request jettyRequest) {
			this.chunkFutureIterator = chunkFutureIterator;
			this.deferredContentProvider = deferredContentProvider;
			this.jettyRequest = jettyRequest;
		}

		private void fillIfPossible() {
			if (!Thread.holdsLock(pendingLock)) {
				throw new IllegalStateException();
			}
			if (pendingOffers < MAX_PENDING_OFFERS && !pendingRead) {
				LOG.debug("Filling with {} pending offers", pendingOffers);
				fill();
			}
		}

		public void start() {
			synchronized (pendingLock) {
				fill();
			}
		}

		private void fill() {
			if (isError()) {
				return;
			}
			try {

				if (!Thread.holdsLock(pendingLock)) {
					throw new IllegalStateException();
				}
				if (pendingRead) {
					throw new IllegalStateException();
				}
				pendingRead = true;

				if (chunkFutureIterator.hasNext()) {
					chunkFutureIterator.next().whenComplete(this::chunkFutureComplete);
				} else {
					done();
				}
			} catch (final Throwable e) {
				error(e);
			}
		}


		private void chunkFutureComplete(final ByteBuffer byteBuffer, final Throwable error) {
			if (error != null) {
				error(error);
				return;
			}

			final boolean success = deferredContentProvider.offer(byteBuffer, offerCallback);
			if (!success) {
				error(new RuntimeException("Failed to offer content to deferred content provider"));
			}

			synchronized (pendingLock) {
				pendingOffers++;
				pendingRead = false;
				fillIfPossible();
			}
		}

		private void done() {
			deferredContentProvider.close();
		}

		public void error(final Throwable error) {
			final boolean set = this.error.compareAndSet(null, error);
			if (!set && this.error.get() != error) {
				this.error.get().addSuppressed(error);
			}
			jettyRequest.abort(this.error.get());
			deferredContentProvider.close();
		}

		public boolean isError() {
			return error.get() != null;
		}
	}

	private long getContentLength(final BodyProvider bodyProvider) {
		final long contentLength = bodyProvider.contentLength();
		if (contentLength >= 0) {
			return contentLength;
		}
		return -1;
	}

	private <T> HttpResponse<T> toHttpResponse(final BodyResult<T> result) {
		LOG.debug("Building final HttpResponse");
		return new JettyHttpResponse<>(
				result.getResult().getResponse().getStatus(),
				new JettyHttpRequest(result.getResult().getRequest()),
				HeadersTransformation.toHttpHeaders(result.getResult().getResponse().getHeaders()),
				result.getBody());
	}

	@Override
	public void close() {
		closed = true;
		try {
			jettyClient.stop();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
