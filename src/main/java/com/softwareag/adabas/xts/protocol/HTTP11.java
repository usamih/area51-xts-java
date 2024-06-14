/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTSException;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.interfaces.IConnectCallback;
import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.network.IPtransport;

//-----------------------------------------------------------------------
/**
 * This class implements HTTP/1.1 protocol driver for XTS. URLs for this
 * protocol driver must be: http11://host:port or
 * http11://proxy:port?host=host&port=port to communicate via a proxy.
 ** 
 * @version 2.1.1.0
 **/
class HTTP11 extends IPtransport {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	static Vector<Thread> tvec = new Vector<Thread>(); // list of threads in
														// this prot
	static byte[] head1 = null;
	static byte[] head2 = null;
	static byte[] server = null;
	static byte[] client = null;
	static // initialize static fields above.
	{
		try {
			head1 = "POST * HTTP/1.1".getBytes("UTF8");
			head2 = "HTTP/1.1 200 OK".getBytes("UTF8");
			server = "Server: XTS".getBytes("UTF8");
			client = "User-agent: XTS".getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/** Default constructor. **/
	public HTTP11() {
		super(tvec);
	}

	private HTTP11(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect) {
		super(tvec, url, listen, callback, userval, retry_interval,
				retry_count, reconnect);
		protocol = "http";
	}

	private HTTP11(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect, final int connTo) {
		super(tvec, url, listen, callback, userval, retry_interval,
				retry_count, reconnect, connTo);
		protocol = "http";
	}

	
	/**
	 * Listen for an incoming connection.
	 ** 
	 * @param url
	 *            The URL of the connection to listen on.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return the instance of protocol driver.
	 **/
	public final IDriver listen(final XTSurl url,
			final IConnectCallback callback, final Object userval) {
		HTTP11 http11 = new HTTP11(url, true, callback, userval, 0, 0, false);
		http11.start();
		return http11;
	}

	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return the instance of protocol driver.
	 **/
	public final IDriver connect(final XTSurl url,
			final IConnectCallback callback, final Object userval) {
		HTTP11 http11 = new HTTP11(url, false, callback, userval,
				DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false);
		http11.start();
		return http11;
	}
	
	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @param connTo
	 *            the time to wait for a connection to be established.
	 ** @return the instance of protocol driver.
	 **/
	public final IDriver connect(final XTSurl url,
			final IConnectCallback callback, final Object userval, final int connTo) {
		HTTP11 http11 = new HTTP11(url, false, callback, userval,
				DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false, connTo);
		http11.start();
		return http11;
	}

	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @param retry_interval
	 *            the new retry interval if connection attempt fails.
	 ** @param retry_count
	 *            the number of times to retry to connect. Set this value to 0
	 *            if no retry is needed.
	 ** @param <reconnect set to true if the driver should try to re-establish a
	 *        connection which has been severed.
	 ** @return the instance of protocol driver.
	 **/
	public final IDriver connect(final XTSurl url,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect) {
		HTTP11 http11 = new HTTP11(url, false, callback, userval,
				retry_interval, retry_count, reconnect);
		http11.start();
		return http11;
	}

	protected final Object getToken(final IPtx tx) throws IOException {
		HttpToken t = new HttpToken();
		tx.token = t; // chicken and egg problem

		t.pheadlen = addHeader("POST * HTTP/1.1", t.post_header, t.pheadlen);
		t.pheadlen = addHeader("Content-type: application/octet-stream",
				t.post_header, t.pheadlen);
		t.pheadlen = addHeader("Connection: Keep-Alive", t.post_header,
				t.pheadlen);
		t.pheadlen = addHeader("User-agent: XTS", t.post_header, t.pheadlen);

		t.rheadlen = addHeader("HTTP/1.1 200 OK", t.reply_header, t.rheadlen);
		t.rheadlen = addHeader("Content-type: application/octet-stream",
				t.reply_header, t.rheadlen);
		t.rheadlen = addHeader("Server: XTS", t.reply_header, t.rheadlen);
		t.rheadlen = addHeader("Content-length: ", t.reply_header, t.rheadlen) - 2;

		if (!listener) {// can't be via proxy if listener

			String host = url.getValue("host");
			if (host != null) {
				String port = url.getValue("port");
				if (port != null) {
					host += ":" + port;
				}
				t.pheadlen = addHeader("Host: " + host, t.post_header,
						t.pheadlen);
			}
		}

		t.pheadlen = addHeader("Content-length: ", t.post_header, t.pheadlen) - 2;

		return t;
	}

	protected final void transmit(final Message p, final IPtx tx)
			throws IOException {
		byte[] l = { 0, 0, 0, 0, 0, 0x0d, 0x0a, 0x0d, 0x0a };

		HttpToken t = (HttpToken) tx.token;

		// if(p.target>0)
		if (p.target > 0 || p.target == XTS.RESOLVE_TARGET_NAME) {
			XTStrace.dump("HTTP Outgoing Header", "transmit", t.post_header, t.pheadlen, true);
			tx.dos.write(t.post_header, 0, t.pheadlen);
		} else {
			XTStrace.dump("HTTP Outgoing Header", "transmit", t.reply_header, t.rheadlen, true);
			tx.dos.write(t.reply_header, 0, t.rheadlen);
		}

		int i = 4;
		int j = p.length + 16;
		for (; j > 0; i--) {
			l[i] = (byte) ((j % 10) + 0x30);
			j /= 10;
		}
		tx.dos.write(l, i + 1, 8 - i);

		p.putMessage(tx.dos);
	}

	// -----------------------------------------------------------------------
	// add to header
	// -----------------------------------------------------------------------
	private static final int addHeader(final String s, final byte[] b, int position) {
		int pos = position;
		int len = s.length();
		try {
			System.arraycopy(s.getBytes("UTF8"), 0, b, pos, len);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(); // [0002]
		}
		pos += len;
		b[pos++] = 13;
		b[pos++] = 10;
		return pos;
	}

	protected final Message receive(final IPrx rx) throws IOException, XTSException, Exception {
		while (rx.twin.token == null) {
			Thread.currentThread();
			Thread.yield();
		}
		readHeader(rx);
		return Message.getMessage(rx.dis);
	}

	// -----------------------------------------------------------------------
	// Read the header
	// -----------------------------------------------------------------------
	private final void readHeader(final IPrx rx) throws IOException {
		rx.dis.mark(1024);
		HttpToken t = (HttpToken) rx.twin.token;
		rx.dis.read(t.headread);
		if (!(t.eq(head1, 0) || t.eq(head2, 0))) {
			throw new RuntimeException("HTTP11 readHeader: Bad http header in stream");
		}

		int i = head1.length;
		XTStrace.dump("HTTP Incoming Header", "readHeader", t.headread, t.headread.length, true);
		int hl = 0;
		for (;; i++) {
			if (t.headread[i] == 0x0d) {
				if (t.headread[++i] == 0x0a) {
					i++;
				}
				t.headlines[hl++] = i;
				if (t.headread[i] == 0x0d) {
					if (t.headread[++i] == 0x0a) {
						i++;
					}
					t.headlines[hl++] = i;
					break;
				}
			}
		}

		for (i = 0;; i++) {
			if (i == hl) {
				throw new RuntimeException("HTTP11 readHeader: No user-agent or server in message");
			}
			if (t.eq(server, t.headlines[i])) {
				break;
			}
			if (t.eq(client, t.headlines[i])) {
				break;
			}
		}

		rx.dis.reset();
		rx.dis.readFully(t.headread, 0, t.headlines[hl - 1]);

	}

	protected final void shut(final IPtx tx) throws IOException {
	}

	static class HttpToken {
		byte[] post_header = new byte[256]; // post header
		int pheadlen = 0; // post header length
		byte[] reply_header = new byte[256]; // reply header
		int rheadlen = 0; // post header length
		byte[] headread = new byte[1024];
		int[] headlines = new int[64];

		protected final boolean eq(final byte[] b, final int pos) {
			int i = 0;
			int j = pos;
			try {
				for (; b[i++] == headread[j++];) {
					;
				}
				return false;
			} catch (ArrayIndexOutOfBoundsException ai) {
			}
			return true;
		}

	}

}
