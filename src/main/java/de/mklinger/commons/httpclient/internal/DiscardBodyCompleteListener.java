package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;

import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class DiscardBodyCompleteListener<U> implements HttpResponse.BodyCompleteListener<U> {
	private final U body;

	public DiscardBodyCompleteListener(final U value) {
		this.body = value;
	}

	@Override
	public void onNext(final ByteBuffer content) throws Exception {
	}

	@Override
	public void onComplete() throws Exception {
	}

	@Override
	public U getBody() throws Exception {
		return body;
	}

	@Override
	public void close() throws Exception {
	}
}
