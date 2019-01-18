package de.mklinger.commons.httpclient.internal.jetty;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.internal.HttpHeadersImpl;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class HeadersTransformation {
	public static HttpHeaders toHttpHeaders(final HttpFields jettyHeaders) {
		final HttpHeadersImpl httpHeaders = new HttpHeadersImpl();
		for (final HttpField jettyHeader : jettyHeaders) {
			final String name = jettyHeader.getName();
			for (final String value : jettyHeader.getValues()) {
				httpHeaders.addHeader(name, value);
			}
		}
		return httpHeaders;
	}
}
