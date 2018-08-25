package de.mklinger.commons.httpclient.internal;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mklinger.commons.httpclient.HttpHeaders;
import de.mklinger.commons.httpclient.HttpResponse;
import de.mklinger.commons.httpclient.HttpResponse.BodyCompleteListener;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public final class StringBodyHandler implements HttpResponse.BodyHandler<String> {
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final Pattern CHARSET_PATTERN = Pattern.compile(".*;\\s*charset\\s*=([^;]+)", Pattern.CASE_INSENSITIVE);

	private static final Logger LOG = LoggerFactory.getLogger(StringBodyHandler.class);

	@Override
	public BodyCompleteListener<String> apply(final int statusCode, final HttpHeaders responseHeaders) {
		final Charset charset = findCharset(responseHeaders);
		return new StringCompleteListener(charset);
	}

	private Charset findCharset(final HttpHeaders responseHeaders) {
		final Optional<String> contentType = responseHeaders.firstValue("content-type");
		return findCharset(contentType);
	}

	private Charset findCharset(final Optional<String> contentType) {
		if (contentType.isPresent()) {
			return findCharset(contentType.get());
		} else {
			LOG.warn("No Content-Type header present in response. Assuming charset {}", DEFAULT_CHARSET);
			return DEFAULT_CHARSET;
		}
	}

	private Charset findCharset(final String contentType) {
		final Matcher matcher = CHARSET_PATTERN.matcher(contentType);
		if (!matcher.matches()) {
			LOG.warn("No charset parameter present in Content-Type header. Assuming charset {}", DEFAULT_CHARSET);
			return DEFAULT_CHARSET;
		} else {
			final String charset = matcher.group(1).trim();
			return charsetForName(charset);
		}
	}

	private Charset charsetForName(final String cs) {
		try {
			return Charset.forName(cs);
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			LOG.warn("Unable to use charset parameter present in Content-Type header. Assuming charset {}", DEFAULT_CHARSET);
			return DEFAULT_CHARSET;
		}
	}
}