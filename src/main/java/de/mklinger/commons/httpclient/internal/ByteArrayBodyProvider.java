package de.mklinger.commons.httpclient.internal;

import org.eclipse.jetty.client.util.BytesContentProvider;

import de.mklinger.commons.httpclient.HttpRequest;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public final class ByteArrayBodyProvider extends BytesContentProvider implements HttpRequest.BodyProvider {
	public ByteArrayBodyProvider(byte[]... bytes) {
		super(bytes);
	}

	public ByteArrayBodyProvider(String contentType, byte[]... bytes) {
		super(contentType, bytes);
	}
}

//public class ByteArrayBodyProvider extends ByteBufferBodyProvider implements HttpRequest.BodyProvider {
//	private static final Logger LOG = LoggerFactory.getLogger(ByteArrayBodyProvider.class);
//
//	private static final int MAX_SIZE = 8 * 1024;
//
//	public ByteArrayBodyProvider(byte[]... bytes) {
//		super(toByteBuffers(bytes));
//	}
//
//	public ByteArrayBodyProvider(String contentType, byte[]... bytes) {
//		super(contentType, toByteBuffers(bytes));
//	}
//
//	private static ByteBuffer[] toByteBuffers(byte[][] bytes) {
//		if (bytes.length == 1 && bytes[0].length <= MAX_SIZE) {
//			return new ByteBuffer[] { ByteBuffer.wrap(bytes[0]) };
//		}
//
//		final List<ByteBuffer> byteBuffers = new ArrayList<>();
//
//		for (final byte[] bs : bytes) {
//			if (bs.length <= MAX_SIZE) {
//				byteBuffers.add(ByteBuffer.wrap(bs));
//			} else {
//				int offset = 0;
//				while (offset < bs.length) {
//					final int len = Math.min(MAX_SIZE, bs.length - offset);
//					//LOG.info("Chunk len {} for full len {}", len, bs.length);
//					byteBuffers.add(ByteBuffer.wrap(bs, offset, len));
//					offset += len;
//				}
//			}
//		}
//
//		return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
//	}
//}