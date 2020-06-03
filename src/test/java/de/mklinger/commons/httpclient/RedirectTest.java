package de.mklinger.commons.httpclient;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class RedirectTest extends ServerTestBase {
	private static final Logger LOG = LoggerFactory.getLogger(RedirectTest.class);

	@Override
	protected Class<TestServlet> getServletClass() {
		return TestServlet.class;
	}

	public static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			if (req.getParameter("wasredirected") == null) {
				LOG.info("Redirect");
				resp.sendRedirect("?wasredirected=true");
			} else {
				resp.setContentType("text/plain;charset=UTF-8");
				resp.getWriter().print("Ok");
			}
		}
	}

	@Test
	public void test() throws Throwable {
		final HttpRequest request = HttpRequest.newBuilder(URI.create(getBaseUrl()))
				.build();

		try (final HttpClient httpClientWithoutRedirect = HttpClient.newBuilder()
				.trustStore(getClientTrustStore())
				.keyStore(getClientKeyStore(), getClientKeyPassword())
				.build()) {

			final HttpResponse<String> response1 = httpClientWithoutRedirect.sendAsync(request, BodyHandlers.asString()).join();
			assertEquals(302, response1.statusCode());
		}

		try (final HttpClient httpClientWithRedirect = HttpClient.newBuilder()
				.trustStore(getClientTrustStore())
				.keyStore(getClientKeyStore(), getClientKeyPassword())
				.followRedirects(true)
				.build()) {


			final HttpResponse<String> response2 = httpClientWithRedirect.sendAsync(request, BodyHandlers.asString()).join();
			assertEquals(200, response2.statusCode());
			assertEquals("Ok", response2.body());
			assertThat(response2.uri().toASCIIString(), containsString("?wasredirected=true"));
		}
	}
}
