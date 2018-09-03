package de.mklinger.commons.httpclient;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import de.mklinger.commons.httpclient.HttpResponse.BodyHandler;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public abstract class ClientServerTestBase extends ServerTestBase {
	protected HttpClient newHttpClient() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		return HttpClient.newBuilder()
				.trustStore(getClientTrustStore())
				.keyStore(getClientKeyStore(), getClientKeyPassword())
				.build();
	}

	protected static <T> BodyHandler<T> requireSuccess(final BodyHandler<T> onSuccess) {
		return (statusCode, responseHeaders) -> {
			if (statusCode < 200 || statusCode > 299) {
				throw new RuntimeException("Non success status code: " + statusCode);
			}
			return onSuccess.apply(statusCode, responseHeaders);
		};
	}
}
