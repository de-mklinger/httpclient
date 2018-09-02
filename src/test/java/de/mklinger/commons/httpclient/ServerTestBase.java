package de.mklinger.commons.httpclient;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public abstract class ServerTestBase {
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private Server server;
	private int port;

	@Before
	public void setUpServer() throws Exception {
		try (ServerSocket ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		server = ConscryptHTTP2Server.newServer(
				port,
				getServletClass(),
				singletonMap("tmpDir", tmp.newFolder().getAbsolutePath()));
		server.start();
	}

	protected abstract Class<? extends Servlet> getServletClass();

	protected String getBaseUrl() {
		return "https://localhost:" + port;
	}

	protected URI getBaseUri() {
		return URI.create(getBaseUrl());
	}

	@After
	public void tearDownServer() throws Exception {
		server.stop();
	}

	protected KeyStore getClientKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore;
		final String keyStorePassword = Settings.get("client.ssl.key-store-password");
		final String keyStoreLocation = Settings.get("client.ssl.key-store").replace("classpath:", "");
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(keyStoreLocation)) {
			keyStore = KeyStore.getInstance("JKS");
			keyStore.load(in, keyStorePassword.toCharArray());
		}
		return keyStore;
	}

	protected String getClientKeyPassword() {
		return Settings.get("client.ssl.key-password");
	}

	protected KeyStore getClientTrustStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore trustStore;
		final String trustStorePassword = Settings.get("client.ssl.trust-store-password");
		final String trustStoreLocation = Settings.get("client.ssl.trust-store").replace("classpath:", "");
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(trustStoreLocation)) {
			trustStore = KeyStore.getInstance("JKS");
			trustStore.load(in, trustStorePassword.toCharArray());
		}
		return trustStore;
	}
}
