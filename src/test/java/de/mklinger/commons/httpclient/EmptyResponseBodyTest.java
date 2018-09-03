package de.mklinger.commons.httpclient;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class EmptyResponseBodyTest extends ClientServerTestBase {
	@Override
	protected Class<? extends Servlet> getServletClass() {
		return TestServlet.class;
	}

	@SuppressWarnings("serial")
	public static class TestServlet extends HttpServlet {
		@Override
		protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			resp.setHeader("X-Done", "true");
		}
	}

	@Test
	public void test() throws Throwable {
		try (final HttpClient httpClient = newHttpClient()) {
			final HttpRequest request = HttpRequest.newBuilder(getBaseUri())
					.method("DELETE", BodyProviders.noBody())
					.build();

			final HttpResponse<byte[]> response = httpClient.sendAsync(request, requireSuccess(BodyHandlers.asByteArray()))
					.get(5, TimeUnit.SECONDS);

			assertEquals("true", response.headers().firstValue("X-Done").orElse(null));
			assertEquals(0, response.body().length);
		}
	}
}
