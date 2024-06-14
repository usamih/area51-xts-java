/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.helpers;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;

/**
 * Tokens, passed to XTS client/server upon receipt, used to indicate return
 * path and driver token.
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022
// -----------------------------------------------------------------------
public class Token {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	private static Token free_tokens = null;
	private static Object lock = "lock";

	public Token next;
	public int route;
	public Object driver_token;
	public IConnection connection;
	public Message msg;
	public IXTSreceiver rcb;
	public Object userval;
	boolean isFree;

	public static final Token newToken(IConnection conn, Object token, int route) {
		Token t;
		synchronized (lock) {
			t = free_tokens;
			if (t == null)
				t = new Token();
			else
				free_tokens = t.next;
		}
		t.driver_token = token;
		t.connection = conn;
		t.route = route;
		t.isFree = false;
		return t;
	}

	/**
	 * Override hashCode() in Object - provide same hashCode() as the
	 * connection.
	 **/
	public final int hashCode() {
		return connection.hashCode();
	}

	/**
	 * Override equals() in Object - tokens are equivalent if they have the same
	 * connection.
	 **/
	public final boolean equals(Object x) {
		return (x instanceof Token) && ((Token) x).connection == connection;
	}

	public final void free() {
		synchronized (lock) {
			if (isFree) {
				RuntimeException re = new RuntimeException("Token free: Tried to free already free Token");
				if (XTStrace.bGlobalErrorEnabled) 
					re.printStackTrace(System.err);
				throw re;
			}
			isFree = true;
			next = free_tokens;
			free_tokens = this;
		}
	}

}
