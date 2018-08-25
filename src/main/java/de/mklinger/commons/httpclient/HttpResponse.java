package de.mklinger.commons.httpclient;

import java.nio.ByteBuffer;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public interface HttpResponse<T> {
	/**
	 * Returns the status code for this response.
	 *
	 * @return the response code
	 */
	int statusCode();

	/**
	 * Returns the received response headers.
	 *
	 * @return the response headers
	 */
	HttpHeaders headers();

	/**
	 * Returns the body. Depending on the type of {@code T}, the returned body
	 * may represent the body after it was read (such as {@code byte[]}, or
	 * {@code String}, or {@code Path}) or it may represent an object with
	 * which the body is read, such as an {@link java.io.InputStream}.
	 *
	 * @return the body
	 */
	T body();

	@FunctionalInterface
	public interface BodyHandler<T> {
		/**
		 * Returns a {@link BodySubscriber BodySubscriber} considering the given
		 * response status code and headers. This method is always called before
		 * the body is read and its implementation can decide to keep the body
		 * and store it somewhere, or else discard it by returning the {@code
		 * BodySubscriber} returned from {@link BodyHandlers#discard(Object)
		 * discard}.
		 *
		 * @param statusCode the HTTP status code received
		 * @param responseHeaders the response headers received
		 * @return a body complete listener
		 */
		BodyCompleteListener<T> apply(int statusCode, HttpHeaders responseHeaders);
	}

	public interface BodyCompleteListener<T> extends AutoCloseable {
		void onNext(final ByteBuffer content) throws Exception;
		void onComplete() throws Exception;
		T getBody() throws Exception;
	}
}
