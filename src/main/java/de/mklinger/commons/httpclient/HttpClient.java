package de.mklinger.commons.httpclient;

import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import de.mklinger.commons.httpclient.internal.HttpClientBuilderImpl;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public interface HttpClient extends AutoCloseable {
	<T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler);

	@Override
	void close();

	/**
	 * Creates a new {@code HttpClient} builder.
	 *
	 * @return a {@code HttpClient.Builder}
	 */
	static Builder newBuilder() {
		return new HttpClientBuilderImpl();
	}

	/**
	 * A builder of immutable {@link HttpClient}s.
	 *
	 * <p> Builders are created by invoking {@linkplain HttpClient#newBuilder()
	 * newBuilder}. Each of the setter methods modifies the state of the builder
	 * and returns the same instance. Builders are not thread-safe and should not be
	 * used concurrently from multiple threads without external synchronization.
	 */
	public interface Builder {

		Builder trustStore(KeyStore trustStore);

		/**
		 * @param keyPassword the password for recovering keys in the KeyStore
		 */
		Builder keyStore(KeyStore keyStore, String keyPassword);

		/**
		 * Sets the connect timeout duration for this client.
		 *
		 * @param duration the duration to allow the underlying connection to be
		 *                 established
		 * @return this builder
		 * @throws IllegalArgumentException if the duration is non-positive
		 */
		Builder connectTimeout(Duration duration);

		/**
		 * Specifies whether requests will automatically follow redirects issued
		 * by the server.
		 *
		 * <p> If this method is not invoked prior to {@linkplain #build()
		 * building}, then newly built clients will not follow redirects.
		 *
		 * @return this builder
		 */
		Builder followRedirects(boolean followRedirects);

		/**
		 * Returns a new {@link HttpClient} built from the current state of this
		 * builder.
		 *
		 * @return this builder
		 */
		HttpClient build();
	}
}
