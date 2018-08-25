package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class StringCompleteListener implements HttpResponse.BodyCompleteListener<String> {
	private final Charset charset;
	private final ByteArrayCompleteListener byteArrayCompleteListener;
	private volatile String body;

	public StringCompleteListener(final Charset charset) {
		this.charset = charset;
		this.byteArrayCompleteListener = new ByteArrayCompleteListener();
	}

	@Override
	public void onNext(final ByteBuffer content) throws Exception {
		byteArrayCompleteListener.onNext(content);
	}

	@Override
	public void onComplete() throws Exception {
		byteArrayCompleteListener.onComplete();
		body = new String(byteArrayCompleteListener.getBody(), charset);
		byteArrayCompleteListener.close();
	}

	@Override
	public String getBody() throws Exception {
		return body;
	}

	@Override
	public void close() throws Exception {
		body = null;
	}
}
