package de.mklinger.commons.httpclient.internal;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jetty.client.util.PathContentProvider;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class FileBodyProvider extends PathContentProvider implements HttpRequest.BodyProvider {
	public FileBodyProvider(Path filePath) throws IOException {
		super(filePath);
	}

	public FileBodyProvider(String contentType, Path filePath) throws IOException {
		super(contentType, filePath);
	}
}
