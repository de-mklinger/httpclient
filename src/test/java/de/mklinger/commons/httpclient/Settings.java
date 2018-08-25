package de.mklinger.commons.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class Settings {
	final static Properties props;
	static {
		props = new Properties();
		try (InputStream in = ConscryptHTTP2Server.class.getClassLoader().getResourceAsStream("application-testkeys.properties")) {
			props.load(in);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String get(final String key) {
		final String value = props.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException("Not found: " + key);
		}
		return value;
	}
}
