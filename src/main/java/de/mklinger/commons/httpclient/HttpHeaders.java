package de.mklinger.commons.httpclient;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public interface HttpHeaders {
	/**
	 * Returns an unmodifiable multi Map view of this HttpHeaders.
	 *
	 * Keys are case insensitive.
	 *
	 * @return the Map
	 */
	Map<String, List<String>> map();

	/**
	 * Returns an {@link Optional} containing the first value of the given named
	 * (and possibly multi-valued) header. If the header is not present, then the
	 * returned {@code Optional} is empty.
	 *
	 * @param name the header name
	 * @return an {@code Optional<String>} for the first named value
	 */
	default Optional<String> firstValue(final String name) {
		return allValues(name).stream().findFirst();
	}

	/**
	 * Returns an {@link OptionalLong} containing the first value of the named
	 * header field. If the header is not present, then the Optional is empty. If
	 * the header is present but contains a value that does not parse as a
	 * {@code Long} value, then an exception is thrown.
	 *
	 * @param name the header name
	 * @return an {@code OptionalLong}
	 * @throws NumberFormatException if a value is found, but does not parse as a
	 *                               Long
	 */
	default OptionalLong firstValueAsLong(final String name) {
		return allValues(name).stream().mapToLong(Long::valueOf).findFirst();
	}

	/**
	 * Returns an unmodifiable List of all of the values of the given named header.
	 * Always returns a List, which may be empty if the header is not present.
	 *
	 * @param name the header name
	 * @return a List of String values
	 */
	default List<String> allValues(final String name) {
		requireNonNull(name);
		final List<String> values = map().get(name);
		if (values == null) {
			return unmodifiableList(emptyList());
		} else {
			return values;
		}
	}
}
