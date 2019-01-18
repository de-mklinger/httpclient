package de.mklinger.commons.httpclient.internal.jetty;

import java.net.URI;
import java.util.Objects;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpRequest;
import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class JettyHttpResponse<T> implements HttpResponse<T> {
	private final HttpRequest request;
	private final int statusCode;
	private final HttpHeaders headers;
	private final T body;

	public JettyHttpResponse(final int statusCode, final HttpRequest request, final HttpHeaders headers, final T body) {
		this.request = Objects.requireNonNull(request);
		this.statusCode = requireValidStatusCode(statusCode);
		this.headers = Objects.requireNonNull(headers);
		// Body may be null in case of discard:
		this.body = body;
	}

	private static int requireValidStatusCode(final int statusCode) {
		if (statusCode < 100 || statusCode > 999) {
			throw new IllegalArgumentException();
		}
		return statusCode;
	}

	@Override
	public URI uri() {
		return request.uri();
	}

	@Override
	public HttpRequest request() {
		return request;
	}

	@Override
	public int statusCode() {
		return statusCode;
	}

	@Override
	public HttpHeaders headers() {
		return headers;
	}

	@Override
	public T body() {
		return body;
	}
}
