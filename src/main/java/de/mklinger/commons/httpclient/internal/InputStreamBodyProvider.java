package de.mklinger.commons.httpclient.internal;

import java.io.InputStream;

import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class InputStreamBodyProvider extends InputStreamContentProvider implements HttpRequest.BodyProvider, ContentProvider.Typed {
	private final String contentType;

	public InputStreamBodyProvider(String contentType, InputStream stream) {
		super(stream);
		this.contentType = contentType;
	}

	public InputStreamBodyProvider(InputStream stream) {
		this("application/octet-stream", stream);
	}

	@Override
	public String getContentType() {
		return contentType;
	}
}
