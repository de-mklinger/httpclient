package de.mklinger.commons.httpclient.internal;

import static java.util.Objects.requireNonNull;

import java.security.KeyStore;
import java.security.Security;
import java.time.Duration;
import java.util.concurrent.Executor;

import org.conscrypt.Conscrypt;
import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.io.LeakTrackingByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import de.mklinger.commons.httpclient.HttpClient;
import de.mklinger.commons.httpclient.HttpClient.Builder;
import de.mklinger.commons.httpclient.internal.hostnameverifier.DefaultHostnameVerifier;
import de.mklinger.commons.httpclient.internal.jetty.JettyHttpClient;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class HttpClientBuilderImpl implements HttpClient.Builder {
	private KeyStore trustStore;
	private KeyStore keyStore;
	private String keyPassword;

	// Currently defaults to Jetty's default connect timeout, which is 15 seconds
	private Duration connectTimeout;

	// Stick to JDK client default:
	private boolean followRedirects = false;

	private String name = "HttpClient";

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
	public Builder connectTimeout(final Duration duration) {
		requireNonNull(duration);
		if (duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException("Invalid duration: " + duration);
		}
		this.connectTimeout = duration;
		return this;
	}

	@Override
	public HttpClient.Builder followRedirects(final boolean followRedirects) {
		this.followRedirects = followRedirects;
		return this;
	}

	@Override
	public Builder name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public HttpClient build() {
		addSecurityProvider();

		final DefaultHostnameVerifier defaultHostnameVerifier = new DefaultHostnameVerifier();
		Conscrypt.setDefaultHostnameVerifier(defaultHostnameVerifier::verify);

		final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
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

		if (connectTimeout != null) {
			final long timeoutMillis = connectTimeout.toMillis();
			jettyClient.setConnectTimeout(timeoutMillis);
			if (timeoutMillis < jettyClient.getAddressResolutionTimeout()) {
				jettyClient.setAddressResolutionTimeout(timeoutMillis);
			}
		}

		jettyClient.setFollowRedirects(followRedirects);

		jettyClient.setExecutor(newExecutor(getClass().getClassLoader(), name));

		final MappedByteBufferPool delegate = new MappedByteBufferPool(2048,
				jettyClient.getExecutor() instanceof ThreadPool.SizedThreadPool
				? ((ThreadPool.SizedThreadPool)jettyClient.getExecutor()).getMaxThreads() / 2
						: ProcessorUtils.availableProcessors() * 2);

		jettyClient.setByteBufferPool(new LeakTrackingByteBufferPool(delegate));

		try {
			jettyClient.start();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		return new JettyHttpClient(jettyClient);
	}

	private Executor newExecutor(ClassLoader classLoader, String name) {
		final QueuedThreadPool threadPool = new ThreadContextClassLoaderQueuedThreadPool(classLoader);
		threadPool.setName(name);
		return threadPool;
	}
}
