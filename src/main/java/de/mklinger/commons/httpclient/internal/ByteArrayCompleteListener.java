package de.mklinger.commons.httpclient.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import de.mklinger.commons.httpclient.HttpResponse;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class ByteArrayCompleteListener implements HttpResponse.BodyCompleteListener<byte[]> {
	private volatile List<byte[]> byteArrays;
	private volatile byte[] body;

	@Override
	public void onNext(final ByteBuffer content) throws Exception {
		if (byteArrays == null) {
			byteArrays = new ArrayList<>();
		}
		final int len = content.remaining();
		final byte[] dst = new byte[len];
		content.get(dst);
		byteArrays.add(dst);
	}

	@Override
	public void onComplete() throws Exception {
		if (byteArrays == null) {
			body = new byte[0];
		} else {
			body = concat(byteArrays);
			byteArrays = null;
		}
	}

	@Override
	public byte[] getBody() throws Exception {
		return body;
	}

	private byte[] concat(final List<byte[]> byteArrays) {
		int fullLen = 0;
		for (final byte[] bs : byteArrays) {
			fullLen += bs.length;
		}
		if (fullLen < 0) {
			throw new IllegalStateException("Overflow");
		}
		final byte[] full = new byte[fullLen];
		int dstIdx = 0;
		for (final byte[] bs : byteArrays) {
			System.arraycopy(bs, 0, full, dstIdx, bs.length);
			dstIdx += bs.length;
		}
		return full;
	}

	@Override
	public void close() throws Exception {
		byteArrays = null;
		body = null;
	}
}
