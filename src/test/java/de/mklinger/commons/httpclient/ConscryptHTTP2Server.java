//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package de.mklinger.commons.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Security;
import java.util.Collections;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Test server that verifies that the Conscrypt ALPN mechanism works.
 */
public class ConscryptHTTP2Server
{
	public static void main(final String[] args) throws Exception {
		final Server server = newServer(8443, TestServlet.class, Collections.emptyMap());
		server.start();
	}

	public static Server newServer(final int port, final Class<? extends Servlet> servletClass, final Map<String, String> initParameters) {
		Security.addProvider(new OpenSSLProvider());

		final Server server = new Server();

		final HttpConfiguration httpsConfig = new HttpConfiguration();
		httpsConfig.setSecureScheme("https");
		httpsConfig.setSecurePort(port);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());


		final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setProvider("Conscrypt");
		sslContextFactory.setKeyStorePath(Settings.get("server.ssl.key-store").replace("classpath:", "src/test/resources/"));
		sslContextFactory.setKeyStorePassword(Settings.get("server.ssl.key-store-password"));
		sslContextFactory.setKeyManagerPassword(Settings.get("server.ssl.key-password"));
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		sslContextFactory.setNeedClientAuth(true);
		sslContextFactory.setTrustStorePath(Settings.get("server.ssl.trust-store").replace("classpath:", "src/test/resources/"));
		sslContextFactory.setTrustStorePassword(Settings.get("server.ssl.trust-store-password"));

		final HttpConnectionFactory http = new HttpConnectionFactory(httpsConfig);
		final HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);
		final ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol(http.getProtocol());
		final SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

		final ServerConnector http2Connector = new ServerConnector(server, ssl, alpn, h2, http);
		http2Connector.setPort(port);
		server.addConnector(http2Connector);

		server.setHandler(createServletHandler(servletClass, initParameters));
		return server;
	}

	private static ServletContextHandler createServletHandler(final Class<? extends Servlet> servletClass, final Map<String, String> initParameters) {
		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");

		initParameters.forEach(context::setInitParameter);

		context.addServlet(servletClass, "/");

		return context;
	}

	public static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			// Make client time out:
			//			try {
			//				Thread.sleep(30000);
			//			} catch (final InterruptedException e) {
			//				Thread.currentThread().interrupt();
			//				throw new RuntimeException("Interrupted", e);
			//			}
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello World!");
		}

		@Override
		protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
			final Path tmp = Files.createTempFile("server", ".bin");
			System.out.println("SERVER: Saving to " + tmp);
			int bytes;
			try (InputStream in = req.getInputStream()) {
				try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.WRITE)) {
					bytes = IOUtils.copy(in, out);
				}
			}
			System.out.println("SERVER: Copied " + bytes + " bytes");

			// Make client time out:
			//			try {
			//				Thread.sleep(30000);
			//			} catch (final InterruptedException e) {
			//				Thread.currentThread().interrupt();
			//				throw new RuntimeException("Interrupted", e);
			//			}
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello World!");
		}
	}

}