package de.mklinger.commons.httpclient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpResponse.BodyHandler;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class HeadersTest extends ServerTestBase {
	private static final String ECHO_HEADER_PREFIX = "X-Test-Echo-";

	private static final Logger LOG = LoggerFactory.getLogger(HeadersTest.class);

	@Override
	protected Class<? extends Servlet> getServletClass() {
		return TestServlet.class;
	}

	@SuppressWarnings("serial")
	public static class TestServlet extends HttpServlet {
		@Override
		protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			// Echo all headers given by client prefixed by ECHO_HEADER_PREFIX
			final Enumeration<String> headerNames = req.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				final String headerName = headerNames.nextElement();
				final String echoHeaderName = ECHO_HEADER_PREFIX + headerName;
				final Enumeration<String> headerValues = req.getHeaders(headerName);
				while (headerValues.hasMoreElements()) {
					final String headerValue = headerValues.nextElement();
					resp.addHeader(echoHeaderName, headerValue);
				}
			}
		}
	}

	@Test
	public void test() throws Throwable {
		final HttpClient httpClient = newHttpClient();

		final HttpRequest request = HttpRequest.newBuilder(getBaseUri())
				.header("X-Multi-Header", "Value1")
				.header("X-Multi-Header", "Value2")
				.header("X-Single-Header", "Value")
				.header("X-To-Be-Replaced-Header", "Unreplaced Value")
				.setHeader("X-To-Be-Replaced-Header", "Replaced Value")
				.headers("X-Multi-Header", "Value3", "X-Other-Header", "Other Value")
				.build();

		final HttpHeaders responseHeaders = httpClient.sendAsync(request, requireSuccess(BodyHandlers.discard()))
				.get(5, TimeUnit.SECONDS)
				.headers();

		assertSingleHeaderEchos(responseHeaders);
		assertMultiHeaderEchos(responseHeaders);
		assertReplacedHeaderEchos(responseHeaders);
		assertNonExistingHeaderEchos(responseHeaders);
	}

	private void assertSingleHeaderEchos(final HttpHeaders responseHeaders) {
		final String singleHeaderEcho = ECHO_HEADER_PREFIX + "X-Single-Header";
		assertThat(
				responseHeaders.firstValue(singleHeaderEcho).orElse(null),
				is("Value"));
		assertThat(
				responseHeaders.allValues(singleHeaderEcho),
				contains("Value"));
		assertThat(
				responseHeaders.allValues(singleHeaderEcho.toLowerCase()),
				contains("Value"));
		assertThat(
				responseHeaders.allValues(singleHeaderEcho.toUpperCase()),
				contains("Value"));
	}

	private void assertMultiHeaderEchos(final HttpHeaders responseHeaders) {
		final String multiHeaderEcho = ECHO_HEADER_PREFIX + "X-Multi-Header";
		assertThat(
				responseHeaders.firstValue(multiHeaderEcho).orElse(null),
				is("Value1"));
		assertThat(
				responseHeaders.allValues(multiHeaderEcho),
				contains("Value1", "Value2", "Value3"));
		assertThat(
				responseHeaders.allValues(multiHeaderEcho.toLowerCase()),
				contains("Value1", "Value2", "Value3"));
		assertThat(
				responseHeaders.allValues(multiHeaderEcho.toUpperCase()),
				contains("Value1", "Value2", "Value3"));
	}

	private void assertReplacedHeaderEchos(final HttpHeaders responseHeaders) {
		final String replacedHeaderEcho = ECHO_HEADER_PREFIX + "X-To-Be-Replaced-Header";
		assertThat(
				responseHeaders.firstValue(replacedHeaderEcho).orElse(null),
				is("Replaced Value"));
	}

	private void assertNonExistingHeaderEchos(final HttpHeaders responseHeaders) {
		assertThat(responseHeaders.allValues("doesnotexist"), empty());
		assertThat(responseHeaders.firstValue("doesnotexist"), is(Optional.empty()));
	}

	private HttpClient newHttpClient() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		return HttpClient.newBuilder()
				.trustStore(getClientTrustStore())
				.keyStore(getClientKeyStore(), getClientKeyPassword())
				.build();
	}

	private static <T> BodyHandler<T> requireSuccess(final BodyHandler<T> onSuccess) {
		return (statusCode, responseHeaders) -> {
			if (statusCode < 200 || statusCode > 299) {
				throw new RuntimeException("Non success status code: " + statusCode);
			}
			return onSuccess.apply(statusCode, responseHeaders);
		};
	}
}
