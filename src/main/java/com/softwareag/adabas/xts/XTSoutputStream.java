/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class provides a non-synchronized, buffered data output stream. It is
 * provided for performance in tightly controlled circumstances.
 ** 
 **/
// Maintenance History:
// Reworked by usamih (Michael Chirila) 10/06/2022

public class XTSoutputStream {
	public static final String VERSION = XTSversion.VERSION; // [0001]
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	byte[] buf;
	int count = 0;
	int lastcount = 0;
	OutputStream os;

	/**
	 * Constructor.
	 ** 
	 * @param out
	 *            the underlying output stream that should be written to.
	 ** @param size
	 *            the default size of the buffer. The buffer is expanded as
	 *            needed.
	 **/
	public XTSoutputStream(OutputStream out, int size) {
		os = out;
		buf = new byte[size];
	}

	/** Write a single byte to the stream. **/
	public final void write(int b) {
		try {
			buf[count] = (byte) b;
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf();
			buf[count] = (byte) b;
		}
		count++;
	}

	/** Write a byte sub-array to the stream. **/
	public final void write(byte[] b, int off, int len) {
		try {
			System.arraycopy(b, off, buf, count, len);
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf(len);
			System.arraycopy(b, off, buf, count, len);
		}
		count += len;
	}

	/** Write a byte array to the stream. **/
	public final void write(byte[] b) {
		try {
			System.arraycopy(b, 0, buf, count, b.length);
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf();
			System.arraycopy(b, 0, buf, count, b.length);
		}
		count += b.length;
	}

	private final void newbuf() {
		byte[] b = new byte[buf.length * 3 / 2]; // 50% increase
		System.arraycopy(buf, 0, b, 0, count);
		buf = b;
	}

	private final void newbuf(int len) {
		int i = buf.length * 3 / 2;
		if (len > (i - count))
			i = count + len * 3 / 2;
		byte[] b = new byte[i];
		System.arraycopy(buf, 0, b, 0, count);
		buf = b;
	}

	/**
	 * Flush the stream to its underlying output stream.
	 ** 
	 * @param t
	 *            the trace indicator - if set then the buffer is traced.
	 ** @param s
	 *            the text to be placed with the trace.
	 **/
	public final void flush(boolean t, String s) throws IOException {
		if (count > 0) {
			if (t)
				XTStrace.dump(s, "flush", buf, count, true);
			lastcount = count;
			flush();
		}
	}

	/** Flush the stream to its underlying output stream. **/
	public final void flush() throws IOException {
		if (count > 0) {
			os.write(buf, 0, count);
			// os.flush(); [0004]
			lastcount = count;
			count = 0;
		}
	}

	/** Close the underlying stream. **/
	public void close() throws IOException {
		os.close();
	}

	/** Write an int to the stream. **/
	public void writeInt(int i) {
		try {
			buf[count] = (byte) (i >> 24);
			buf[count + 1] = (byte) (i >> 16);
			buf[count + 2] = (byte) (i >> 8);
			buf[count + 3] = (byte) i;
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf();
			writeInt(i);
		}
		count += 4;
	}

	/** Write a short to the stream. **/
	public void writeShort(int i) {
		try {
			buf[count] = (byte) (i >> 8);
			buf[count + 1] = (byte) i;
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf();
			writeShort(i);
		}
		count += 2;
	}

	/** Write a long to the stream. **/
	public void writeLong(long i) {
		try {
			buf[count] = (byte) (i >> 56);
			buf[count + 1] = (byte) (i >> 48);
			buf[count + 2] = (byte) (i >> 40);
			buf[count + 3] = (byte) (i >> 32);
			buf[count + 4] = (byte) (i >> 24);
			buf[count + 5] = (byte) (i >> 16);
			buf[count + 6] = (byte) (i >> 8);
			buf[count + 7] = (byte) i;
		} catch (ArrayIndexOutOfBoundsException ae) {
			newbuf();
			writeLong(i);
		}
		count += 8;
	}

	/** Trace the current buffer contents. **/
	public final void trace(String s) {
		XTStrace.dump(s, "trace", buf, lastcount, true);
	}

}
