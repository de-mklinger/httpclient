package de.mklinger.commons.httpclient.internal.jetty;

import java.util.Objects;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class JettyHttpResponse<T> implements HttpResponse<T> {
	private final int statusCode;
	private final HttpHeaders headers;
	private final T body;

	public JettyHttpResponse(final int statusCode, final HttpHeaders headers, final T body) {
		if (statusCode < 100 || statusCode > 999) {
			throw new IllegalArgumentException();
		}
		this.statusCode = statusCode;
		this.headers = Objects.requireNonNull(headers);
		// Body may be null in case of discard:
		this.body = body;
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
