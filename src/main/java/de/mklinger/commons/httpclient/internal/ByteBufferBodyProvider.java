package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;

import org.eclipse.jetty.client.util.ByteBufferContentProvider;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class ByteBufferBodyProvider extends ByteBufferContentProvider implements HttpRequest.BodyProvider {
	public ByteBufferBodyProvider(ByteBuffer... buffers) {
		super(buffers);
	}

	public ByteBufferBodyProvider(String contentType, ByteBuffer... buffers) {
		super(contentType, buffers);
	}
}