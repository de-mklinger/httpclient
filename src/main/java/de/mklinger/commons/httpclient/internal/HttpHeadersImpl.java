package de.mklinger.commons.httpclient.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.mklinger.commons.httpclient.HttpHeaders;

public class HttpHeadersImpl implements HttpHeaders {
	private final Map<String, List<String>> map;

	public HttpHeadersImpl() {
		this.map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}

	// TODO use builder pattern for HTTPHeaders
	public static HttpHeaders deepCopy(final HttpHeadersImpl other) {
		final HttpHeadersImpl copy = new HttpHeadersImpl();
		final Set<String> keys = other.map.keySet();
		for (final String key : keys) {
			final List<String> values = other.map.get(key);
			final List<String> copiedValues = new ArrayList<>(values);
			copy.map.put(key, copiedValues);
		}
		return copy;
	}

	@Override
	public Map<String, List<String>> map() {
		return Collections.unmodifiableMap(map);
	}

	public void addHeader(final String name, final String value) {
		map.computeIfAbsent(name, unused -> new ArrayList<>(1))
		.add(value);
	}

	public void setHeader(final String name, final String value) {
		final List<String> values = new ArrayList<>(1);
		values.add(value);
		map.put(name, values);
	}
}
