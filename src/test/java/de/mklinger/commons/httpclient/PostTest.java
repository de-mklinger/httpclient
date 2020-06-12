package de.mklinger.commons.httpclient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
@Ignore
public class PostTest extends ClientServerTestBase {
	private static final Logger LOG = LoggerFactory.getLogger(PostTest.class);

	private byte[] bytes;

	@Before
	public void generateBytes() {
		bytes = new byte[100000];
		ThreadLocalRandom.current().nextBytes(bytes);
	}

	@Before
	public void setUpAdditionalServers() throws IOException, Exception {
		final int numOfAdditionalServers = 4;
		for (int i = 0; i < numOfAdditionalServers ; i++) {
			newServer();
		}
	}

	@Override
	protected Class<? extends Servlet> getServletClass() {
		return TestServlet.class;
	}

	@SuppressWarnings("serial")
	public static class TestServlet extends HttpServlet {
		private static final NullOutputStream OUTPUT = new NullOutputStream();

		@Override
		protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			try {
				final int contentLength = req.getContentLength();
				if (contentLength == -1) {
					throw new IllegalArgumentException();
				}
				final int len;
				try (ServletInputStream in = req.getInputStream();) {
					len = IOUtils.copy(in, OUTPUT);
				}
				//				final byte[] requestBody = IOUtils.toByteArray(req.getInputStream());
				if (len != contentLength) {
					fail("Header length: " + contentLength + " != actual length: " + len);
				}
				final String testHeader = req.getHeader("x-test-request");;
				resp.setHeader("x-test-response", testHeader);
			} catch (final Exception e) {
				LOG.error("Error in test servlet", e);
				throw e;
			}
		}
	}

	@Test
	public void test() throws Throwable {
		try (final HttpClient httpClient = newHttpClient()) {

			final int threadCount = 16;
			final int num = 50000;
			final int concurrent = 10 * getServers().size();
			final AtomicInteger current = new AtomicInteger();

			final List<CompletableFuture<HttpResponse<Void>>> cfs = new ArrayList<>(num);
			final List<Thread> threads = new ArrayList<>();

			for (int threadIdx = 0; threadIdx < threadCount; threadIdx++) {

				final Thread thread = new Thread(() -> {
					for (int i = 0; i < num; i++) {
						while (current.get() > concurrent) {
							Thread.yield();
						}
						current.incrementAndGet();

						final boolean small = ThreadLocalRandom.current().nextBoolean();
						int length;
						if (small) {
							length = ThreadLocalRandom.current().nextInt(1, 500);
						} else {
							length = ThreadLocalRandom.current().nextInt(35000, bytes.length);
						}
						LOG.info("Sending {} bytes", length);

						final HttpRequest request = HttpRequest.newBuilder(getRandomBaseUri())
								.PUT(BodyProviders.fromByteBuffer(ByteBuffer.wrap(bytes, 0, length)))
								//								.POST(BodyProviders.fromByteArray(bytes))
								.header("x-test-request", "test-" + i)
								.build();

						final CompletableFuture<HttpResponse<Void>> cf = httpClient.sendAsync(request, BodyHandlers.discard());

						cf.whenComplete((x, e) -> current.decrementAndGet());

						synchronized (cfs) {
							cfs.add(cf);
						}

					}
				});
				thread.start();
				threads.add(thread);
			}

			for (final Thread thread : threads) {
				thread.join();
			}

			for (final CompletableFuture<HttpResponse<Void>> cf : cfs) {
				final HttpResponse<Void> response = cf.get(5, TimeUnit.SECONDS);
				assertThat(response.statusCode(), is(200));
			}

		}
	}

	private URI getRandomBaseUri() {
		final List<Server> servers = getServers();
		final int idx = ThreadLocalRandom.current().nextInt(servers.size());
		return getBaseUri(servers.get(idx));
	}
}
