package de.mklinger.commons.httpclient;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public interface HttpRequest {
	/**
	 * Returns an {@code Optional} containing the {@link BodyProvider} set on this
	 * request. If no {@code BodyProvider} was set in the requests's builder, then
	 * the {@code Optional} is empty.
	 *
	 * @return an {@code Optional} containing this request's {@code BodyProvider}
	 */
	Optional<BodyProvider> bodyProvider();

	/**
	 * Returns the request method for this request. If not set explicitly, the
	 * default method for any request is "GET".
	 *
	 * @return this request's method
	 */
	String method();

	/**
	 * Returns an {@code Optional} containing this request's timeout duration. If
	 * the timeout duration was not set in the request's builder, then the
	 * {@code Optional} is empty.
	 *
	 * @return an {@code Optional} containing this request's timeout duration
	 */
	Optional<Duration> timeout();

	/**
	 * Returns this request's request {@code URI}.
	 *
	 * @return this request's URI
	 */
	URI uri();

	/**
	 * The (user-accessible) request headers that this request was (or will be) sent
	 * with.
	 *
	 * @return this request's HttpHeaders
	 */
	HttpHeaders headers();

	public interface BodyProvider {
	}

	//	public interface BodyProvider extends Iterable<CompletableFuture<ByteBuffer>> {
	//		long contentLength();
	//
	//		/**
	//		 * Get an optional Content-Type HTTP header value for the body. This is taken
	//		 * into account, if no other (user set) Content-Type header is available when
	//		 * sending the request.
	//		 */
	//		default Optional<String> contentType() {
	//			return Optional.empty();
	//		}
	//	}

	public interface Builder {
		/**
		 * Sets this {@code HttpRequest}'s request {@code URI}.
		 *
		 * @param uri the request URI
		 * @return this request builder
		 * @throws IllegalArgumentException if the {@code URI} scheme is not supported
		 */
		Builder uri(URI uri);

		/**
		 * Adds the given name value pair to the set of headers for this request. The
		 * given value is added to the list of values for that name.
		 *
		 * @param name the header name
		 * @param value the header value
		 * @return this request builder
		 * @throws IllegalArgumentException if the header name or value is not valid,
		 *         see <a href="https://tools.ietf.org/html/rfc7230#section-3.2"> RFC
		 *         7230 section-3.2</a>
		 */
		Builder header(String name, String value);

		/**
		 * Adds the given name value pairs to the set of headers for this request. The
		 * supplied {@code String} instances must alternate as header names and header
		 * values. To add several values to the same name then the same name must be
		 * supplied with each new value.
		 *
		 * @param headers the list of name value pairs
		 * @return this request builder
		 * @throws IllegalArgumentException if there are an odd number of parameters, or
		 *         if a header name or value is not valid, see
		 *         <a href="https://tools.ietf.org/html/rfc7230#section-3.2"> RFC 7230
		 *         section-3.2</a>
		 */
		Builder headers(String... headers);

		/**
		 * Sets a timeout for this request. If the response is not received within the
		 * specified timeout then a {@link HttpTimeoutException} is thrown from
		 * {@link HttpClient#sendAsync(HttpRequest, de.mklinger.commons.httpclient.HttpResponse.BodyHandler)
		 * HttpClient::sendAsync} completes exceptionally with a
		 * {@code TimeoutException}.
		 *
		 * @param duration the timeout duration
		 * @return this request builder
		 * @throws IllegalArgumentException if the duration is non-positive
		 */
		Builder timeout(Duration duration);

		/**
		 * Sets the given name value pair to the set of headers for this request. This
		 * overwrites any previously set values for name.
		 *
		 * @param name the header name
		 * @param value the header value
		 * @return this request builder
		 * @throws IllegalArgumentException if the header name or value is not valid,
		 *         see <a href="https://tools.ietf.org/html/rfc7230#section-3.2"> RFC
		 *         7230 section-3.2</a>
		 */
		Builder setHeader(String name, String value);

		/**
		 * Sets the request method of this builder to GET. This is the default.
		 *
		 * @return a {@code HttpRequest}
		 */
		Builder GET();

		/**
		 * Sets the request method of this builder to POST and sets its request body
		 * provider to the given value.
		 *
		 * @param bodyProvider the body provider
		 *
		 * @return a {@code HttpRequest}
		 */
		Builder POST(BodyProvider bodyProvider);

		/**
		 * Sets the request method of this builder to PUT and sets its request body
		 * provider to the given value.
		 *
		 * @param bodyProvider the body provider
		 *
		 * @return a {@code HttpRequest}
		 */
		Builder PUT(BodyProvider bodyProvider);

		/**
		 * Sets the request method of this builder to DELETE and sets its request body
		 * provider to the given value.
		 *
		 * @param bodyProvider the body provider
		 *
		 * @return a {@code HttpRequest}
		 */
		Builder DELETE(BodyProvider bodyProvider);

		/**
		 * Sets the request method and request body of this builder to the given values.
		 *
		 * @apiNote The {@linkplain BodyProviders#noBody() noBody} request body provider
		 *          can be used where no request body is required or appropriate.
		 *
		 * @param method the method to use
		 * @param bodyProvider the body provider
		 * @return a {@code HttpRequest}
		 */
		Builder method(String method, BodyProvider bodyProvider);

		/**
		 * Builds and returns a {@link HttpRequest}.
		 *
		 * @return the request
		 */
		HttpRequest build();

	}

	/**
	 * Creates a {@code HttpRequest} builder.
	 *
	 * @param uri the request URI
	 * @return a new request builder
	 * @throws IllegalArgumentException if the URI scheme is not supported.
	 */
	static HttpRequest.Builder newBuilder(final URI uri) {
		return new de.mklinger.commons.httpclient.internal.HttpRequestBuilderImpl(uri);
	}

	/**
	 * Creates a {@code HttpRequest} builder.
	 *
	 * @return a new request builder
	 */
	static HttpRequest.Builder newBuilder() {
		return new de.mklinger.commons.httpclient.internal.HttpRequestBuilderImpl();
	}
}
