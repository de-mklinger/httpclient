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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

	private final List<Server> servers = new ArrayList<>();
	private int port;

	@Before
	public void setUpServer() throws Exception {
		newServer();
	}

	protected Server newServer() throws IOException, Exception {
		try (ServerSocket ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}

		final Server server = ConscryptHTTP2Server.newServer(
				port,
				getServletClass(),
				singletonMap("tmpDir", tmp.newFolder().getAbsolutePath()));

		servers.add(server);

		server.start();

		return server;
	}

	protected abstract Class<? extends Servlet> getServletClass();

	public List<Server> getServers() {
		return servers;
	}

	protected String getBaseUrl() {
		return getBaseUrl(servers.get(0));
	}

	protected String getBaseUrl(Server server) {
		final int port = ((ServerConnector)server.getConnectors()[0]).getPort();
		return "https://localhost:" + port;
	}

	protected URI getBaseUri() {
		return URI.create(getBaseUrl());
	}

	protected URI getBaseUri(Server server) {
		return URI.create(getBaseUrl(server));
	}

	@After
	public void tearDownServer() throws Exception {
		for (final Server server : servers) {
			server.stop();
		}
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
