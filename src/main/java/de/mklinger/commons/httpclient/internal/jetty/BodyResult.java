package de.mklinger.commons.httpclient.internal.jetty;

import org.eclipse.jetty.client.api.Result;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class BodyResult<T> {
	private final Result result;
	private final T body;

	public BodyResult(final Result result, final T body) {
		this.result = result;
		this.body = body;
	}

	public Result getResult() {
		return result;
	}

	public T getBody() {
		return body;
	}
}
