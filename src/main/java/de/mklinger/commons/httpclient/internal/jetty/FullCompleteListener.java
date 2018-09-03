package de.mklinger.commons.httpclient.internal.jetty;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpResponse;
import de.mklinger.commons.httpclient.HttpResponse.BodyCompleteListener;
import de.mklinger.commons.httpclient.internal.HttpHeadersImpl;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FullCompleteListener<T> extends Listener.Adapter {
	private static final Logger LOG = LoggerFactory.getLogger(FullCompleteListener.class);

	private final Executor completionExecutor;
	private final CompletableFuture<T> result;
	private final HttpResponse.BodyHandler<T> responseBodyHandler;

	private volatile URI uri;
	private volatile int statusCode;
	private volatile HttpHeaders responseHeaders;

	private volatile BodyCompleteListener<T> bodyCompleteListener;

	public FullCompleteListener(final Executor completionExecutor, final HttpResponse.BodyHandler<T> responseBodyHandler) {
		this.completionExecutor = completionExecutor;
		this.result = new CompletableFuture<>();
		this.responseBodyHandler = responseBodyHandler;
	}

	@Override
	public void onHeaders(final Response response) {
		uri = response.getRequest().getURI();
		statusCode = response.getStatus();
		responseHeaders = toHttpHeaders(response.getHeaders());

		LOG.debug("Response: Have headers, setting up body handling");
		LOG.debug("Headers: {}", responseHeaders.map());

		try {
			bodyCompleteListener = responseBodyHandler.apply(statusCode, responseHeaders);
		} catch (final Throwable e) {
			handleError(response, e);
		}
	}

	private HttpHeaders toHttpHeaders(final HttpFields jettyHeaders) {
		final HttpHeadersImpl httpHeaders = new HttpHeadersImpl();
		for (final HttpField jettyHeader : jettyHeaders) {
			final String name = jettyHeader.getName();
			for (final String value : jettyHeader.getValues()) {
				httpHeaders.addHeader(name, value);
			}
		}
		return httpHeaders;
	}

	@Override
	public void onContent(final Response response, final ByteBuffer content) {
		try {
			bodyCompleteListener.onNext(content);
		} catch (final Throwable e) {
			handleError(response, e);
		}
	}

	@Override
	public void onComplete(final Result result) {
		try {

			final Throwable resultFailure = result.getFailure();
			if (resultFailure != null) {
				handleError(null, resultFailure);
			} else {
				try {
					bodyCompleteListener.onComplete();
					complete(bodyCompleteListener.getBody());
				} catch (final Throwable e) {
					handleError(null, e);
				}
			}

		} finally {

			final BodyCompleteListener<T> l = bodyCompleteListener;
			bodyCompleteListener = null;
			if (l != null) {
				try {
					l.close();
				} catch (final Throwable e) {
					// TODO call handleError() from here?
					completeExceptionally(e);
				}
			}

		}
	}

	private void handleError(final Response response, final Throwable e) {
		final BodyCompleteListener<T> l = bodyCompleteListener;
		bodyCompleteListener = null;
		if (l != null) {
			try {
				l.close();
			} catch (final Throwable suppressed) {
				e.addSuppressed(suppressed);
			}
		}

		if (response != null) {
			try {
				response.abort(e);
			} catch (final Throwable suppressed) {
				e.addSuppressed(suppressed);
			}
		}

		try {
			completeExceptionally(e);
		} catch (final Throwable suppressed) {
			e.addSuppressed(suppressed);
		}

		LOG.debug("Error in complete listener", e);
	}

	private void complete(final T result) {
		completionExecutor.execute(() -> this.result.complete(result));
	}

	private void completeExceptionally(final Throwable e) {
		completionExecutor.execute(() -> this.result.completeExceptionally(e));
	}

	public CompletableFuture<T> getResult() {
		return result;
	}

	public URI getUri() {
		return uri;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public HttpHeaders getResponseHeaders() {
		return responseHeaders;
	}
}
