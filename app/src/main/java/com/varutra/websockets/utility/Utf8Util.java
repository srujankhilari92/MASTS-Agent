package com.varutra.websockets.utility;

import java.nio.charset.Charset;

public abstract class Utf8Util {
	
	protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public static String encodePayloadToUtf8(byte[] utf8bytes) throws InvalidUtf8Exception {
		return encodePayloadToUtf8(utf8bytes, 0, utf8bytes.length);
	}
	
	public static String encodePayloadToUtf8(byte[] utf8bytes, int offset, int length) throws InvalidUtf8Exception {
		try {
			Utf8StringBuilder builder = new Utf8StringBuilder(length);
			builder.append(utf8bytes, offset, length);

			return builder.toString();
		} catch (IllegalArgumentException e) {
			if (e.getMessage().equals("!utf8")) {
				throw new InvalidUtf8Exception("Given bytes are no valid UTF-8!");
			}
			throw e;
			
		} catch (IllegalStateException e) {
			if (e.getMessage().equals("!utf8")) {
				throw new InvalidUtf8Exception("Given bytes are no valid UTF-8!");
			}
			throw e;
		}
	}
	
	public static byte[] decodePayloadFromUtf8(String utf8string) {
		return utf8string.getBytes(UTF8_CHARSET);
	}
}
