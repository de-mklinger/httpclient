package de.mklinger.commons.httpclient.internal.jetty;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpResponse;
import de.mklinger.commons.httpclient.HttpResponse.BodyCompleteListener;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FullCompleteListener<T> extends Listener.Adapter {
	private static final Logger LOG = LoggerFactory.getLogger(FullCompleteListener.class);

	private final CompletableFuture<T> result;
	private final HttpResponse.BodyHandler<T> responseBodyHandler;

	private volatile int statusCode;
	private volatile HttpHeaders responseHeaders;

	private volatile BodyCompleteListener<T> bodyCompleteListener;

	public FullCompleteListener(final HttpResponse.BodyHandler<T> responseBodyHandler) {
		this.result = new CompletableFuture<>();
		this.responseBodyHandler = responseBodyHandler;
	}

	@Override
	public void onHeaders(final Response response) {
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
		final Map<String, List<String>> modifiableMap = new HashMap<>();
		jettyHeaders.forEach(
				header -> modifiableMap.put(header.getName(), unmodifiableList(asList(header.getValues()))));
		final Map<String, List<String>> map = unmodifiableMap(modifiableMap);
		return () -> map;
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
					this.result.complete(bodyCompleteListener.getBody());
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
					this.result.completeExceptionally(e);
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
			result.completeExceptionally(e);
		} catch (final Throwable suppressed) {
			e.addSuppressed(suppressed);
		}

		LOG.debug("Error in complete listener", e);
	}

	public CompletableFuture<T> getResult() {
		return result;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public HttpHeaders getResponseHeaders() {
		return responseHeaders;
	}
}
