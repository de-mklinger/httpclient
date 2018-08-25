package de.mklinger.commons.httpclient.internal.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.Callback;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class ResponseListenerWrapper implements Listener {
	private final Listener delegate;

	public ResponseListenerWrapper(final Listener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onBegin(final Response response) {
		delegate.onBegin(response);
	}

	@Override
	public boolean onHeader(final Response response, final HttpField field) {
		return delegate.onHeader(response, field);
	}

	@Override
	public void onHeaders(final Response response) {
		delegate.onHeaders(response);
	}

	@Override
	public void onContent(final Response response, final ByteBuffer content) {
		delegate.onContent(response, content);
	}

	@Override
	public void onContent(final Response response, final ByteBuffer content, final Callback callback) {
		delegate.onContent(response, content, callback);
	}

	@Override
	public void onSuccess(final Response response) {
		delegate.onSuccess(response);
	}

	@Override
	public void onFailure(final Response response, final Throwable failure) {
		delegate.onFailure(response, failure);
	}

	@Override
	public void onComplete(final Result result) {
		delegate.onComplete(result);
	}
}
