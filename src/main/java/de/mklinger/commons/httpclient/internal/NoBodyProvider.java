package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.jetty.client.api.ContentProvider;

import de.mklinger.commons.httpclient.HttpRequest;

public class NoBodyProvider implements HttpRequest.BodyProvider, ContentProvider {
	private static final NoBodyProvider INSTANCE = new NoBodyProvider();

	public static NoBodyProvider getInstance() {
		return INSTANCE;
	}

	@Override
	public Iterator<ByteBuffer> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public long getLength() {
		return 0;
	}
}