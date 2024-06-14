/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;

import com.softwareag.adabas.xts.interfaces.ITransmitCallback;

//----------------------------------------------------------------------
/**
 * The XTS message class. Basically just the header and the body. And some
 * methods for management, reception and transmission.
 **/
public final class Message {
	private static AtomicInteger currentMessageNo = new AtomicInteger(0);
	private static ReentrantLock freeLock = new ReentrantLock();
	private static Message MessagesList = null;
	private static int  Messages;              // Number of byte buffers allocations
	private static int  FreedMessages;         // Number of freed buffers available
	private static long Memory;                // Memory allocated
	private static long MaxLength = 0;         // Max Length
	private static long BiggestLength = 0;     // Biggest Length
 	private static byte[] eyecatcher = null;
	static {
		try {
			eyecatcher = "SAG3".getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	/** The length of the message, excluding the header. **/
	private AtomicBoolean isFree = new AtomicBoolean(true); // sanity check

	public int length;

	/** The header length of the message. **/
	public int hdrlen;

	/** The Target ID of the message. **/
	public int target;

	/** The route. **/
	public int route;

	/** The route timeout value, in seconds. **/
	public int timeout;

	/** The time-to-live of the message. **/
	public byte ttl;

	/** The priority of the message. **/
	public byte priority;

	/** The message number. **/
	public int msgno;

	/** The version number **/
	protected byte version;

	/** The XTS header **/
	protected byte[] header = new byte[32];

	/** The body of the message. **/
	public byte[] body;

	/** The source of the message when received. **/
	public String from;

	/** indicator to set ttl/priority. **/
	public boolean isFresh; // true if brand-new message

	/** Token (set by driver, must be returned in reply. **/
	public Object token = null;

	/** Transmit callback, set by driver. **/
	public ITransmitCallback callback;

	/** User value for transmit callback. **/
	public Object userval;

	private int bodyIndex;           // index for placing data in body
	public String freedBy = null;

	/** Chain for lists. **/
	public Message nextMessage = null;

	// ----------------------------------------------------------------------
	// Constructor
	// ----------------------------------------------------------------------
	private Message(final int length) {
		int final_length = (length/1024)*1024 + 1024;
		Messages++;
		body = new byte[final_length];
		this.length = final_length;
		Memory += final_length;
		if (final_length > BiggestLength)
			BiggestLength = final_length;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(">>NewMessage=" + Messages + " Length=" + length + " AllocatedLength=" + final_length + " BiggestLength=" + BiggestLength + " Memory=" + Memory + "b Message=" + this);
	}

	// ----------------------------------------------------------------------
	/**
	 * Get an XTS message from a data Input Stream. Called by the receive
	 * thread, it blocks until an entire message has been received.
	 ** 
	 * @param i
	 *            a DataInputStream from which to receive the message.
	 ** @return a message read from the stream.
	 ** @exception IOException
	 *                can be thrown as a result of operations on the
	 *                DataInputStream.
	 **/
	// ----------------------------------------------------------------------
	public static Message getMessage(final DataInputStream i) throws IOException, XTSException {
		int len;
		Message p;

		for (;;) {
			len = i.readInt();
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug(">>getMessage Read from Data Stream length=" + len + " available=" + i.available());
			if (len < 12 || len > XTS.maxmsglength) { 
				XTStrace.warn("getMessage Invalid message length=" + len + " hex=" + XTS.hexof(len) + " perhaps, due to protocol incompatibility");
				int k = Math.min (i.available(), 1024);
				byte[] b = new byte[k];
				try {
					i.readFully(b); 
 				} catch(Exception e) {
        				// if any error occurs
					Thread.dumpStack();
				}
				XTStrace.dump("Invalid Message Length:", "getMessage", b, k, true); 
				throw new XTSException("getMessage : Invalid message length received=" + Integer.toString(len) + " perhaps, due to protocol incompatibility",	XTSException.XTS_INVALID_GETMSG_LEN);
			}
			/* ============ Allocate buffer ============== */
			p = newMessage(len - 16); // will be at least 16 in hdr
			/* =========================================== */
			i.readFully(p.header, 4, 12); // read the first 16 (inc len)
			if (p.header[4] == 'S' && p.header[5] == 'A' && p.header[6] == 'G') {
				if (p.header[7] < 0x36)
					break;
				else {
					if (XTStrace.bGlobalDebugEnabled) {
						XTStrace.dump("Invalid header version:", "getMessage", p.header, 16, true); 
						StringBuffer sb = XTStrace.dumpToStringBuffer("Invalid header version:", "getMessage", p.header, 16, true); 
						XTStrace.error(sb.toString());
					}
				}
			} else {
				if (XTStrace.bGlobalWarnEnabled) {
					XTStrace.dump("Invalid header:", "getMessage", p.header, 16, true); 
					StringBuffer sb = XTStrace.dumpToStringBuffer("Invalid header:", "getMessage", p.header, 16, true); 
					XTStrace.error(sb.toString());
				}
			}
			p.freeMessage("getMessage");
			throw new XTSException("getMessage : Invalid message header received", XTSException.XTS_INVALID_GETMSG_HDR);
		}
// reduce trace	if (XTStrace.bGlobalDebugEnabled) {
//			XTStrace.dump(">>getMessage Received header:", "getMessage", p.header, p.header.length, true);
//		}
		p.isFresh = false; // it is not fresh
		p.header[0] = (byte) (len >> 24);
		p.header[1] = (byte) (len >> 16);
		p.header[2] = (byte) (len >> 8);
		p.header[3] = (byte) len;

		p.version = p.header[7];
		p.hdrlen = ((p.header[8] & 0xff) << 8) | (p.header[9] & 0xff);// get header length
		if (p.hdrlen > p.header.length) {
			byte[] temp = new byte[p.hdrlen];
			System.arraycopy(p.header, 0, temp, 0, p.header.length);
			p.header = temp;
		}
		p.ttl = p.header[10];
		p.priority = p.header[11];

		p.target = (p.header[12] << 24) | ((p.header[13] & 0xff) << 16) | ((p.header[14] & 0xff) << 8) | (p.header[15] & 0xff);
		len = len - p.hdrlen;
		p.length = len;
		if (p.isFreed()) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("getMessage: Got freed message during between copy");
			Thread.dumpStack();
			throw new XTSException("getMessage : Invalid buffer - Got freed message in during getMessage before copy ", XTSException.XTS_MESSAGE_REJECTED);
		}

		p.route = 0;
		p.timeout = 0;
		p.msgno = 0;

		if (p.hdrlen > 16) {
			i.readFully(p.header, 16, p.hdrlen - 16);
			p.msgno = (p.header[16] << 24) | ((p.header[17] & 0xff) << 16) | ((p.header[18] & 0xff) << 8) | (p.header[19] & 0xff);
			if (p.hdrlen > 20) {
				p.route = (p.header[20] << 24) | ((p.header[21] & 0xff) << 16) | ((p.header[22] & 0xff) << 8) | (p.header[23] & 0xff);
				if (p.hdrlen > 24) {
					p.timeout = (p.header[24] << 24)
							| ((p.header[25] & 0xff) << 16)
							| ((p.header[26] & 0xff) << 8)
							| (p.header[27] & 0xff);
				}
			}
		}
		if (len > 0) {
			i.readFully(p.body, 0, len);
		} 
		else {
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug("getMessage: No message body received len=0");
		}
		p.nextMessage = null;
		if ((p.body[0] == 'W') && (p.body[1] == '1')) {
			if (XTStrace.bGlobalDebugEnabled)
				XTStrace.debug("getMessage: W1 message !!!!!!!!!!!");
			p.target = -1;
		}
		if (XTStrace.bGlobalDebugEnabled) {
			XTStrace.debug("getMessage full message from Data Stream returned " + p);
		}
		return p;
	}

	// ----------------------------------------------------------------------
	/**
	 * Get a new Message object with the required body capacity.
	 ** 
	 * @param len
	 *            the length of the body of the message.
	 ** @return a message with body capacity given. Please use free() to free it,
	 *         so that it can be re-used and thus avoid memory fragmentation.
	 **/
	// ----------------------------------------------------------------------
	public static Message newMessage(final int len) {
		freeLock.lock();
//		if (XTStrace.bGlobalDebugEnabled) 
//			XTStrace.debug(">>get newMessage Length=" + len + " FreedMessages=" + FreedMessages);
		Message p, head, current, previous=null;
		try {
//			if (XTStrace.bGlobalDebugEnabled) 
//				XTStrace.debug(">>newMessage from MessageList Head=" + MessagesList);
			if (MessagesList == null) {
				p = new Message(len);
//				if (XTStrace.bGlobalDebugEnabled) 
//					XTStrace.debug("Create newMessage=" + p + " length=" + len);
			} else {
				current = MessagesList;
				while (current != null) {
//					if (XTStrace.bGlobalDebugEnabled) 
//						XTStrace.debug("newMessageList=" + current + " ReqLength=" + len + " MessageLength=" + current.body.length);
					if (current.body.length >= len) {
						if (current == MessagesList) {
							// change first to point to next link
							MessagesList = MessagesList.nextMessage;
						} else {
							// bypass the current link
							previous.nextMessage = current.nextMessage;
						}
  						FreedMessages--;
						if (XTStrace.bGlobalDebugEnabled) 
							XTStrace.debug("newMessage=" + current + " we got !!a match!! FreedMessages=" + FreedMessages);
						break;
					} else {
						previous = current;
						// move to next link
						current = current.nextMessage;
//						if (XTStrace.bGlobalDebugEnabled) 
//							XTStrace.debug("newMessage go to next Message="  + current);
					}
				}
				if (current == null) {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.debug("No Message in the message list");
					p = new Message(len);
				} else {
					p = current;
				}
			}
			if (!p.isFree.get()) {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error("<<<Got non freed message in during newMessage call>>>");
				Thread.dumpStack();
				throw new RuntimeException("Got non freed message in during newMessage call");
			}
			p.msgno = currentMessageNo.incrementAndGet();
			p.nextMessage = null;
			p.isFree.set(false);
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug("Got newMessage=" + Messages + " " + p + " of size=" + len + " msgno=" + p.msgno);
		} finally {
			freeLock.unlock();
		}
		Arrays.fill (p.body, (byte) 0);
		p.target = 0;
		p.route = 0;
		p.hdrlen = 0;
		p.length = len;
		p.bodyIndex = 0;
		p.priority = 0;
		p.callback = null;
		p.token = null;
		p.isFresh = true;
		return p;
	}

	// ----------------------------------------------------------------------
	/** Free up a Message clone. **/
	// ----------------------------------------------------------------------
	public void free(final String freedBy) {
		freeMessage (freedBy);
        }

	// ----------------------------------------------------------------------
	/** Free up a Message. **/
	// ----------------------------------------------------------------------
	public void freeMessage(final String freedBy) {
		freeLock.lock();

		try {
			if (isFree.get()) {
				if (XTStrace.bGlobalDebugEnabled) 
					XTStrace.debug(">>freeMessage=" + this + " Length=" + this.length  + " FreedMessages=" + FreedMessages + " <<<Already FreedBy>>>:" + freedBy);
//				if (XTStrace.bGlobalErrorEnabled) 
//					XTStrace.error("freeMessage=" + this + " Error Message already freed by=" + this.freedBy);
//				System.err.println("freeMessage: Error Message already freed by=" + this.freedBy);
//				Thread.dumpStack();
				return;
			}
			callback = null;
			target = 0;
			route = 0;
			this.freedBy = freedBy;
			isFree.set(true);
			nextMessage = MessagesList;
			FreedMessages++;
			MessagesList = this;
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug(">>freeMessage=" + this + " Length=" + this.length  + " FreedMessages=" + FreedMessages+ " FreedBy:" + freedBy);
		} finally {
			freeLock.unlock();
		}
	}

	// ----------------------------------------------------------------------
	/**
	 * Put an XTS message to a data Output Stream.
	 ** 
	 * @param o
	 *            the XTSoutputStream to send the message to.
	 ** @exception IOException
	 *                can be thrown as a result of operations on the
	 *                XTSOutputStream.
	 **/
	// ----------------------------------------------------------------------
	public void putMessage(final XTSoutputStream o) throws IOException {
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.debug(">>putMessage into Data Stream " + this + " route=" + this.route);
		putHeader();
// reduce trace	if (XTStrace.bGlobalDebugEnabled) 
//			dump("Transport Send", "putMessage", this);
		o.write(header, 0, hdrlen);
		if (length > 0) {
			o.write(body, 0, length);
		}
		o.flush();
	}

	public void putHeader() {
		if (route == 0) {
			hdrlen = 20;
		} else {
			hdrlen = 28;
			header[20] = (byte) (route >> 24);
			header[21] = (byte) (route >> 16);
			header[22] = (byte) (route >> 8);
			header[23] = (byte) route;
			header[24] = (byte) (timeout >> 24);
			header[25] = (byte) (timeout >> 16);
			header[26] = (byte) (timeout >> 8);
			header[27] = (byte) timeout;
		}
		int len = length + hdrlen;
		header[0] = (byte) (len >> 24);
		header[1] = (byte) (len >> 16);
		header[2] = (byte) (len >> 8);
		header[3] = (byte) len;
		System.arraycopy(eyecatcher, 0, header, 4, 4);

		header[8] = (byte) (hdrlen >> 8);
		header[9] = (byte) hdrlen;
		header[10] = ttl;
		header[11] = priority;

		header[12] = (byte) (target >> 24);
		header[13] = (byte) (target >> 16);
		header[14] = (byte) (target >> 8);
		header[15] = (byte) target;

                // Change to msgno to zero to sync with XTS-C where msgno is used for streaming
		header[16] = header[17] = header[18] = header[19] = 0;

//		header[16] = (byte) (msgno >> 24);
//		header[17] = (byte) (msgno >> 16);
//		header[18] = (byte) (msgno >> 8);
//		header[19] = (byte) msgno;
	}

	// ----------------------------------------------------------------------
	/**
	 * Check the status of a message
	 **/
	public boolean isFreed() {
		return isFree.get();
	}

	// ----------------------------------------------------------------------
	/**
	 * Make a copy of a message.
	 * @return an exact duplicate of the message.
	 **/
	// ----------------------------------------------------------------------
	public Message cloneMessage() {
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.debug("cloneMessage=" + this);
		Message p = newMessage(length);
		p.length = length;
		p.target = target;
		p.route = route;
		p.timeout = timeout;
		p.ttl = ttl;
		p.token = token;
		p.priority = priority;
		p.version = version;
		// p.msgno=msgno;
		p.hdrlen = hdrlen;
		System.arraycopy(header, 0, p.header, 0, hdrlen);
		p.isFree = new AtomicBoolean(isFree.get());
		p.callback = callback;
		p.userval = userval;
		System.arraycopy(body, 0, p.body, 0, length);
		return p;
	}



	// ----------------------------------------------------------------------
	/** Put a <i>long</i> into the next free bytes in the body. **/
	// ----------------------------------------------------------------------
	public void putLong(final long l) {
		body[bodyIndex++] = (byte) (l >>> 56);
		body[bodyIndex++] = (byte) (l >>> 48);
		body[bodyIndex++] = (byte) (l >>> 40);
		body[bodyIndex++] = (byte) (l >>> 32);
		body[bodyIndex++] = (byte) (l >>> 24);
		body[bodyIndex++] = (byte) (l >>> 16);
		body[bodyIndex++] = (byte) (l >>> 8);
		body[bodyIndex++] = (byte) l;
	}

	// ----------------------------------------------------------------------
	/** Put an <i>int</i> into the next free bytes in the body. **/
	// ----------------------------------------------------------------------
	public void putInt(final int i) {
		body[bodyIndex++] = (byte) (i >>> 24);
		body[bodyIndex++] = (byte) (i >>> 16);
		body[bodyIndex++] = (byte) (i >>> 8);
		body[bodyIndex++] = (byte) i;
	}

	// ----------------------------------------------------------------------
	/** Put a <i>short</i> into the next free bytes in the body. **/
	// ----------------------------------------------------------------------
	public void putShort(final int i) {
		body[bodyIndex++] = (byte) (i >>> 8);
		body[bodyIndex++] = (byte) i;
	}

	/**
	 * Put short value into buffer
	 * 
	 * @param i
	 */
	public void putShort(final short i) {
		putShort((int) i);
	}

	// ----------------------------------------------------------------------
	/** Put a <i>byte array</i> into the next free bytes in the body. **/
	// ----------------------------------------------------------------------
	public void putBytes(final byte[] b) {
		System.arraycopy(b, 0, body, bodyIndex, b.length);
		bodyIndex += b.length;
	}

	// ----------------------------------------------------------------------
	/** Put a <i>byte array</i> into the next free bytes in the body. **/
	// ----------------------------------------------------------------------
	public void putBytes(final byte[] b, final int pos, final int length, int index) {
		System.arraycopy(b, pos, body, index, length);
	}

	public void putBytes(final byte[] b, final int pos, final int length) {
		System.arraycopy(b, pos, body, bodyIndex, length);
		bodyIndex += length;
	}

	// ----------------------------------------------------------------------
	/** Put a <i>byte</i> into the message. **/
	// ----------------------------------------------------------------------
	/**
	 * put bytes into message
	 * 
	 * @deprecated use put instead
	 * @param b
	 */
	public void putByte(final byte b) {
		put(b);
	}

	public void put(final byte b) {
		body[bodyIndex++] = b;
	};

	// ----------------------------------------------------------------------
	/**
	 * Put a <i>long</i> into the next free bytes in the body in Little Endian
	 * order.
	 **/
	// ----------------------------------------------------------------------
	public void putLongReverse(final long l) {
		body[bodyIndex++] = (byte) l;
		body[bodyIndex++] = (byte) (l >>> 8);
		body[bodyIndex++] = (byte) (l >>> 16);
		body[bodyIndex++] = (byte) (l >>> 24);
		body[bodyIndex++] = (byte) (l >>> 32);
		body[bodyIndex++] = (byte) (l >>> 40);
		body[bodyIndex++] = (byte) (l >>> 48);
		body[bodyIndex++] = (byte) (l >>> 56);
	}

	// ----------------------------------------------------------------------
	/**
	 * Put an <i>int</i> into the next free bytes in the body in Little Endian
	 * order.
	 **/
	// ----------------------------------------------------------------------
	public void putIntReverse(final int i) {
		body[bodyIndex++] = (byte) i;
		body[bodyIndex++] = (byte) (i >>> 8);
		body[bodyIndex++] = (byte) (i >>> 16);
		body[bodyIndex++] = (byte) (i >>> 24);
	}

	// ----------------------------------------------------------------------
	/**
	 * Put a <i>short</i> into the next free bytes in the body in Little Endian
	 * order.
	 **/
	// ----------------------------------------------------------------------
	public void putShortReverse(final int i) {
		body[bodyIndex++] = (byte) i;
		body[bodyIndex++] = (byte) (i >>> 8);
	}

	// ----------------------------------------------------------------------
	/** Get an <i>int</i> from the body. **/
	// ----------------------------------------------------------------------
	public int getInt() {
		return (body[bodyIndex++] << 24) | ((body[bodyIndex++] & 0xff) << 16)
				| ((body[bodyIndex++] & 0xff) << 8)
				| (body[bodyIndex++] & 0xff);
	}

	// ----------------------------------------------------------------------
	/** Get a <i>long</i> from the body. **/
	// ----------------------------------------------------------------------
	public long getLong() {
		return (((long) body[bodyIndex++]) << 56)
				| (((long) (body[bodyIndex++] & 0xff)) << 48)
				| (((long) (body[bodyIndex++] & 0xff)) << 40)
				| (((long) (body[bodyIndex++] & 0xff)) << 32)
				| (((long) (body[bodyIndex++] & 0xff)) << 24)
				| ((body[bodyIndex++] & 0xff) << 16)
				| ((body[bodyIndex++] & 0xff) << 8)
				| (body[bodyIndex++] & 0xff);
	}

	// ----------------------------------------------------------------------
	/** Get a <i>short</i> from the body. **/
	// ----------------------------------------------------------------------
	public int getShort() {
		return (body[bodyIndex++] << 8) | (body[bodyIndex++] & 0xff);
	}

	// ----------------------------------------------------------------------
	/** Get a <i>long</i> from the body in Little Endian format. **/
	// ----------------------------------------------------------------------
	public long getLongReverse() {
		return (body[bodyIndex++] & 0xff) | ((body[bodyIndex++] & 0xff) << 8)
				| ((body[bodyIndex++] & 0xff) << 16)
				| (((long) (body[bodyIndex++] & 0xff)) << 24)
				| (((long) (body[bodyIndex++] & 0xff)) << 32)
				| (((long) (body[bodyIndex++] & 0xff)) << 40)
				| (((long) (body[bodyIndex++] & 0xff)) << 48)
				| (((long) body[bodyIndex++]) << 56);
	}

	// ----------------------------------------------------------------------
	/** Get an <i>int</i> from the body in Little Endian format. **/
	// ----------------------------------------------------------------------
	public int getIntReverse() {
		return (body[bodyIndex++] & 0xff) | ((body[bodyIndex++] & 0xff) << 8)
				| ((body[bodyIndex++] & 0xff) << 16)
				| (body[bodyIndex++] << 24);
	}

	// ----------------------------------------------------------------------
	/** Get a <i>short</i> from the body in Little Endian format. **/
	// ----------------------------------------------------------------------
	public int getShortReverse() {
		return (body[bodyIndex++] & 0xff) | (body[bodyIndex++] << 8);
	}

	// ----------------------------------------------------------------------
	/**
	 * Get a <i>byte array</i> from the body.
	 ** 
	 * @param length
	 *            the number of bytes in the new array.
	 **/
	// ----------------------------------------------------------------------
	public byte[] getBytes(final int length) {
		byte[] b = new byte[length];
		System.arraycopy(body, bodyIndex, b, 0, length);
		bodyIndex += length;
		return b;
	}

	// ----------------------------------------------------------------------
	/** Get  <i> bytes into array. **/
	// ----------------------------------------------------------------------
	public void getBytes(final byte[] b, final int length) {
		System.arraycopy(body, bodyIndex, b, 0, length);
		bodyIndex += length;
	}

	// ----------------------------------------------------------------------
	/**
	 * Get a <i>byte array</i> from the body.
	 ** 
	 * @param bytearray
	 *            the byte array where the value is to be returned.
	 **/
	// ----------------------------------------------------------------------
	public void getBytes(final byte[] bytearray) {
		int i = bytearray.length;
		System.arraycopy(body, bodyIndex, bytearray, 0, i);
		bodyIndex += i;
	}

	// ----------------------------------------------------------------------
	/** Get a <i>byte</i> from the body. **/
	// ----------------------------------------------------------------------
	public byte getByte() {
		return body[bodyIndex++];
	}

	// ----------------------------------------------------------------------
	/** Return the number of unconsumed bytes in the body. **/
	// ----------------------------------------------------------------------
	public int lengthLeft() {
		return length - bodyIndex;
	}

	// ----------------------------------------------------------------------
	/** Reset internal body pointer to point to the start of the body. **/
	// ----------------------------------------------------------------------
	public void reset() {
		bodyIndex = 0;
	}

	// ----------------------------------------------------------------------
	/** Skip bytes, by incrementing internal body pointer. **/
	// ----------------------------------------------------------------------
	public void skip(final int count) {
		bodyIndex += count;
	}



	// ----------------------------------------------------------------------
	/** Make a pretty string describing this message. **/
	// ----------------------------------------------------------------------
//	public String toString() {
//		return "MsgNo=" + msgno + " Length=" + length + " Free=" + isFree + " Target=0x"
//				+  Integer.toHexString(target) + " Route=" + route + " callback:" + callback;
//	}

	public static void dump (final String prefix, final String fct, final Message p) {
		if (!XTStrace.bGlobalDebugEnabled) {
			return;
		}
		if (p.hdrlen > 0) {
			XTStrace.dump(prefix + " Header:", fct, p.header, p.header.length, true);
		}
		XTStrace.dump(prefix + " Body:", fct, p.body, p.length, true);
	}

}
