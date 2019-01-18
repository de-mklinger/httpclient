package de.mklinger.commons.httpclient.internal.jetty;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.jetty.client.api.Request;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class JettyHttpRequest implements HttpRequest {
	private final Request jettyRequest;

	public JettyHttpRequest(final Request jettyRequest) {
		this.jettyRequest = jettyRequest;
	}

	@Override
	public Optional<BodyProvider> bodyProvider() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String method() {
		return jettyRequest.getMethod();
	}

	@Override
	public Optional<Duration> timeout() {
		return Optional.of(Duration.ofMillis(jettyRequest.getTimeout()));
	}

	@Override
	public URI uri() {
		return jettyRequest.getURI();
	}

	@Override
	public HttpHeaders headers() {
		return HeadersTransformation.toHttpHeaders(jettyRequest.getHeaders());
	}

}
