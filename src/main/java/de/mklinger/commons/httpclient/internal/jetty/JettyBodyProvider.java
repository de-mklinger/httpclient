package de.mklinger.commons.httpclient.internal.jetty;

import org.eclipse.jetty.client.api.ContentProvider;

import de.mklinger.commons.httpclient.HttpRequest.BodyProvider;

public interface JettyBodyProvider extends BodyProvider {
	ContentProvider getContentProvider();
}
