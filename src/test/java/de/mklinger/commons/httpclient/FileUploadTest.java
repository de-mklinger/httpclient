package de.mklinger.commons.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpResponse.BodyHandler;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FileUploadTest extends ServerTestBase {
	private static final int UPLOAD_COUNT = 20;
	private static final int MIN_FILE_SIZE = 500;
	private static final int MAX_FILE_SIZE = 20 * 1024 * 1024;

	private static final Logger LOG = LoggerFactory.getLogger(FileUploadTest.class);

	@Override
	protected Class<? extends Servlet> getServletClass() {
		return TestServlet.class;
	}

	public static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			final Path tmpDir = Paths.get(getServletContext().getInitParameter("tmpDir"));
			final Path outputFile = Files.createTempFile(tmpDir, "output", ".bin");
			LOG.debug("SERVER: Saving to {}", outputFile);
			int bytes;
			try (InputStream in = req.getInputStream()) {
				try (OutputStream out = Files.newOutputStream(outputFile, StandardOpenOption.WRITE)) {
					bytes = IOUtils.copy(in, out);
				}
			}
			LOG.debug("SERVER: Copied {} bytes", bytes);
			resp.setContentType("text/plain;charset=UTF-8");
			resp.getWriter().print(outputFile.toString());
		}
	}

	@Test
	public void test() throws Throwable {
		final HttpClient httpClient = newHttpClient();

		final CountDownLatch cdl = new CountDownLatch(UPLOAD_COUNT);
		final AtomicReference<Throwable> error = new AtomicReference<>(null);

		long fileSize = 0L;

		final Path[] files = new Path[UPLOAD_COUNT];
		for (int i = 0; i < UPLOAD_COUNT; i++) {
			files[i] = createInputFile();
			fileSize += Files.size(files[i]);
		}

		final long start = System.currentTimeMillis();

		for (int i = 0; i < UPLOAD_COUNT; i++) {
			final Path inputFile = files[i];
			sendFile(httpClient, inputFile, cdl, error);
		}

		try {
			cdl.await();

			final long millis = System.currentTimeMillis() - start;
			final long mib = fileSize / 1024 / 1024;
			final double mibPerSec = Math.round(mib / (millis / 1000.0) * 100.0) / 100.0;
			LOG.info("Took {} millis for {} MiB ({} MiB/s)", millis, mib, mibPerSec);

		} finally {
			LOG.info("Closing client");
			httpClient.close();
		}
		if (error.get() != null) {
			throw error.get();
		}
	}

	private HttpClient newHttpClient() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		return HttpClient.newBuilder()
				.trustStore(getClientTrustStore())
				.keyStore(getClientKeyStore(), getClientKeyPassword())
				.build();
	}

	public Path createInputFile() throws IOException {
		final Path inputFile = tmp.newFile().toPath();
		final Random random = new Random();
		final int size = random.nextInt(MAX_FILE_SIZE - MIN_FILE_SIZE + 1) + MIN_FILE_SIZE;
		LOG.info("Using {} MiB input file ({} bytes)", size / 1024 / 1024, size);
		final byte[] buf = new byte[size];
		random.nextBytes(buf);
		Files.write(inputFile, buf, StandardOpenOption.WRITE);
		return inputFile;
	}

	private void sendFile(final HttpClient httpClient, final Path inputFile, final CountDownLatch cdl, final AtomicReference<Throwable> error) throws Throwable {
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(getBaseUrl()))
				.POST(BodyProviders.fromFile(inputFile))
				//.timeout(Duration.ofSeconds(5))
				.build();

		httpClient.sendAsync(request, requireSuccess(BodyHandlers.asString()))
		.thenAccept(response -> {
			final Path outputFile = Paths.get(response.body());
			try {
				final byte[] clientBytes = Files.readAllBytes(inputFile);
				final byte[] serverBytes = Files.readAllBytes(outputFile);
				Assert.assertArrayEquals(clientBytes, serverBytes);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		})
		.whenComplete((response, err) -> {
			if (err != null) {
				LOG.error("Error result", err);
				if (!error.compareAndSet(null, err)) {
					error.get().addSuppressed(err);
				}
			} else {
				LOG.debug("Success result");
			}
			cdl.countDown();
		});
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
