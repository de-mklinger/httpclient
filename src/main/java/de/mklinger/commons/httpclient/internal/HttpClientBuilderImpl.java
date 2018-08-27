package de.mklinger.commons.httpclient.internal;

import java.security.KeyStore;
import java.security.Security;

import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import de.mklinger.commons.httpclient.HttpClient;
import de.mklinger.commons.httpclient.internal.jetty.JettyHttpClient;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class HttpClientBuilderImpl implements HttpClient.Builder {
	private KeyStore trustStore;
	private KeyStore keyStore;
	private String keyPassword;

	// Stick to JDK client default:
	private boolean followRedirects = false;

	private static volatile boolean securityProviderAdded = false;

	private static void addSecurityProvider() {
		if (!securityProviderAdded) {
			synchronized (HttpClientBuilderImpl.class) {
				if (!securityProviderAdded) {
					if (Security.getProvider("Conscrypt") == null) {
						Security.addProvider(new OpenSSLProvider());
					}
					securityProviderAdded = true;
				}
			}
		}
	}

	@Override
	public HttpClient.Builder trustStore(final KeyStore trustStore) {
		this.trustStore = trustStore;
		return this;
	}

	@Override
	public HttpClient.Builder keyStore(final KeyStore keyStore, final String keyPassword) {
		this.keyStore = keyStore;
		this.keyPassword = keyPassword;
		return this;
	}

	@Override
	public HttpClient.Builder followRedirects(final boolean followRedirects) {
		this.followRedirects = followRedirects;
		return this;
	}

	@Override
	public HttpClient build() {
		addSecurityProvider();

		final SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setProvider("Conscrypt");

		if (trustStore != null) {
			sslContextFactory.setTrustStore(trustStore);
		}
		if (keyStore != null) {
			sslContextFactory.setKeyStore(keyStore);
		}
		if (keyPassword != null) {
			sslContextFactory.setKeyManagerPassword(keyPassword);
		}

		final HTTP2Client http2Client = new HTTP2Client();
		http2Client.addBean(sslContextFactory);
		final HttpClientTransportOverHTTP2 clientTransport = new HttpClientTransportOverHTTP2(http2Client);

		final org.eclipse.jetty.client.HttpClient jettyClient = new org.eclipse.jetty.client.HttpClient(clientTransport, sslContextFactory);

		jettyClient.setFollowRedirects(followRedirects);

		try {
			jettyClient.start();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		return new JettyHttpClient(jettyClient);
	}
}
