/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/* Project started at May 2006 (initial by Thorsten Knieling) 
 * Reworked by usamih (Michael Chirila) 10/06/2022
*/
package com.softwareag.adabas.xts.common;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;

/**
 * Adabas ByteBuffer help class containing methods working with
 * byte buffer.
 * 
 * @author Thorsten A. Knieling
 * 
 */
public final class AdaByteHelper {

	private AdaByteHelper() {		
	}
	
	/**
	 * Convert byte array to long with specified order.
	 * 
	 * @param b
	 *            byte array
	 * @param offset
	 *            offset to begin with
	 * @param len
	 *            length of array used
	 * @param order
	 *            Used order of byte array
	 * @return long value
	 */
	public static long convertByteToLong(final byte[] b, final int offset,final int len, final ByteOrder order) {
		long l = 0L;
		if (order == ByteOrder.BIG_ENDIAN) {
			for (int i = len, x = 1; i > 0; i--, x *= 256L) {
				l += ((b[offset + i - 1] & 0xff) * x);
			}
		} else {
			for (int i = 0, x = 1; i < len; i++, x *= 256L) {
				l += ((b[offset + i] & 0xff) * x);
			}
		}
		return l;
	}

	/**
	 * Convert long value into byte array
	 * @param l long value need to be converted
	 * @param b byte array buffer to store the long into
	 * @param offset offset in the byte buffer to start storing
	 * @param len length in the buffer
	 * @param order Used Byte order for storing data
	 */
	public static void convertLongToBytes(final long l, final byte[] b, final int offset,final int len,final ByteOrder order) {

		if (order == ByteOrder.BIG_ENDIAN) {
			long x = l;
			for (int i = len; i > 0; i--) {
				b[offset + i - 1] = (byte) (x & 0xff);
				x = (x / 256);
			}
		} else {
			long x = l;
			for (int i = 0; i < len; i++) {
				b[offset + i] = (byte) (x & 0xff);
				x = (x / 256);
				// System.out.println(i+": "+b[i]+" x="+x);
			}
		}
	}

	/**
	 * Convert given EBCDIC character into ASCII byte
	 * 
	 * @param ebcdicChar given character
	 * @param isEbcdic Input character type
	 * @return ASCII character value
	 */
	public static byte convertToAscii(final byte ebcdicChar,final boolean isEbcdic) {
		if (isEbcdic)
			try {
				byte[] ebcdic = new byte[1];
				ebcdic[0] = ebcdicChar;
				final String test = new String(ebcdic, "Cp037");
				return convertToCharset(test, "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		return ebcdicChar;
	}

	/**
	 * Convert given ASCII character into EBCDIC byte
	 * 
	 * @param asciiChar given character
	 * @param isEbcdic Input character type
	 * @return EBCDIC character value
	 */
	public static byte convertToEbcdic(final byte asciiChar,final boolean isEbcdic) {
		try {
			if (isEbcdic) {
				byte[] ascii = new byte[1];
				ascii[0] = asciiChar;
				return convertToCharset(new String(ascii, "ISO-8859-1"),
						"Cp037");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return asciiChar;
	}

	/**
	 * Convert String input first character based on given charset to
	 * a byte value  
	 * @param val String input given as value
	 * @param charset Charset used for conversion
	 * @return Byte value for first character in given String
	 */
	public static byte convertToCharset(final String val, final String charset) {
		try {
			final byte[] ebcdic = val.getBytes(charset);
			// AdabasTrace.debug(String.format("char for %s %x",charset,ebcdic[0]));
			return (byte) ebcdic[0];
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return (byte) val.charAt(0);

	}

	/**
	 * Swap bytes in the byte array 
	 * 1 - len -1
	 * 2 - len -2
	 * etc.
	 * @param b byte array buffer to swap
	 */
	public static final void swap(final byte[] b) {
		swap(b,0,b.length);
	}

	/**
	 * Swap bytes in the byte array beginning at an offset for
	 * a predefined length 
	 * 1 - len -1
	 * 2 - len -2
	 * etc.
	 * @param b byte array buffer to swap
	 */
	public  static final void swap(final byte[] b, final int offset,final int len) {
		if (len < 2)
			return;
		byte x;
		for (int i = offset; i < (offset + (len / 2)); i++) {
			int end = 2 * offset + len - i - 1;
			x = b[i];
			b[i] = b[end];
			b[end] = x;
		}
	}

}
