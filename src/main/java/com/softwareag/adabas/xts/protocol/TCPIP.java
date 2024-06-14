/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.protocol;

import java.io.*;
import java.util.Vector;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.Server;
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
 * This class implements a TCP/IP protocol driver for XTS.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public class TCPIP extends IPtransport {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	static final Vector<Thread> tvec = new Vector<Thread>(); // contains connections

	public TCPIP() {
		super(tvec);
	}

	protected TCPIP(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect, final int connTo) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect, connTo);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Init TCPIP " + callback);
	}
	
	protected TCPIP(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Init TCPIP " + callback);
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
	 ** @return an instance of this driver.
	 **/
	public IDriver listen(final XTSurl url, final IConnectCallback callback, final Object userval) {
		TCPIP tcpip = new TCPIP(url, true, callback, userval, 0, 0, false);
		tcpip.start();
		return tcpip;
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
	 ** @return an instance of this driver.
	 **/
	public IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval) {
		TCPIP tcpip = new TCPIP(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Connect TCPIP " + callback);
		tcpip.start();
		return tcpip;
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
 	 ** @return an instance of this driver.
	 **/
	public IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final int connTo) {
		TCPIP tcpip = new TCPIP(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false, connTo);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Connect TCPIP " + callback);
		tcpip.start();
		return tcpip;
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
	 ** @param reconnect
	 *            set to true if the driver should try to re-establish a
	 *            connection which has been severed.
	 ** @return an instance of this driver.
	 **/
	public IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect) {
		TCPIP tcpip = new TCPIP(url, false, callback, userval, retry_interval, retry_count, reconnect);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Connect TCPIP " + callback);
		tcpip.start();
		return tcpip;
	}

	protected final Object getToken(final IPtx tx) {
		return null;
	}

	protected final void transmit(final Message p, final IPtx tx) throws IOException {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Transmit length=" + p.length + " Message=" + p);
		if (MsgType == 0) {		// XTS type 
			p.putMessage(tx.dos);
		} else if (MsgType == 1) {	// ADI type
			if (p.length > 0) {
				tx.dos.write(p.body, 0, p.length);
				tx.dos.flush();
			}

		} else if (MsgType == 2) {	// RAW type
			if (p.length > 0) {
				tx.dos.write(p.body, 0, p.length);
				tx.dos.flush();
			}
		}		
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("User data Sent to " + tx.socket);
		
//
//		if (raw) {
//			if (p.length > 0) {
//				if (p.target < 0) {
//					if (p.target < XTS.CONTROL_MSG) {// via route return?
//
//						if (p.target == XTS.RESOLVE_TARGET_NAME) {
//							XTStrace.verbose("Transmit resolve target name lenght=" + p.length);
//							Message q = Message.newMessage(p.length + 4);
//							q.target = XTS.RESOLVE_TARGET_NAME_REPLY;
//							q.route = p.route;
//							q.ttl = p.ttl;
//							q.putInt(Server.getNextTarget());
//							System.arraycopy(p.body, 0, q.body, 4, p.length);
//							tx.twin.callback.received(tx, q, tx.twin.userval);
//						}
//						return;
//					}
//				}
//				XTStrace.dump("Dump of RAW data Sent to " + tx.socket, "transmit", p.body, p.length, true);
//				tx.dos.write(p.body, 0, p.length);
//				tx.dos.flush();
//			}
//		} else {
//			p.putMessage(tx.dos);
//		}

	}

	protected final Message receive(final IPrx rx) throws IOException, XTSException, Exception {
		Message p;

		if (MsgType == 0) {		// XTS type 
			p = Message.getMessage(rx.dis);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Received lenght=" + p.length + " Message=" + p);
			return p;
		} else if (MsgType == 1) {	// ADI type
			short len;
			len = rx.dis.readShort();
			if (XTStrace.bGlobalVerboseEnabled) {
				XTStrace.verbose("ADI message length:" + len);
			}
			if (len < 2) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("ADI Invalid message length:" + len + " " + XTS.hexof(len));
				throw new XTSException("ADI Message getMessage : Invalid message length received=" + len, XTSException.XTS_INVALID_GETMSG_LEN);
			}
			p = Message.newMessage(len);
			p.target = XTS.ADI_MESSAGE;
			p.route = XTS.ADI_MESSAGE;
			rx.dis.readFully(p.body, 2, len - 2);
			p.body[0] = (byte) (len >> 8);
			p.body[1] = (byte) len;
			return p;
		} else if (MsgType == 2) {	// RAW type
			byte[] b = new byte[4096];
			int i;
			for (;;) {
				i = rx.dis.read(b);
				if (i < 0) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.error("RAW Received error=" + i);
					throw new EOFException("Ended");
				}
				if (i > 0) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("RAW Received lenght=" + i);
					break;
				}
			}
			p = Message.newMessage(i);
			p.target = XTS.RAW_MESSAGE;
			p.route = XTS.RAW_MESSAGE;
			System.arraycopy(b, 0, p.body, 0, i);
			return p;
		}		
		p = Message.getMessage(rx.dis);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Received lenght=" + p.length);
		return p;
	}

	protected final void shut(final IPtx tx) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("shut " + callback);
	}

}
