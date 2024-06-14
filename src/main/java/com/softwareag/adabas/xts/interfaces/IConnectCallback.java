/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;

/**
 * Connect callback interface for protocol drivers.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IConnectCallback {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * Method called when a connection has been established.
	 ** 
	 * @param partner
	 *            The url passed to the Connect or Listen methods.
	 ** @param uservalue
	 *            The user object passed to the Receive method.
	 ** @param conn
	 *            The connection. Used to refer to the connection from here on.
	 **/
	void connected(XTSurl partner, Object uservalue, IConnection conn);

	/**
	 * Method called when a connection has been disconnected.
	 ** 
	 * @param partner
	 *            The url passed to the Connect or Listen methods.
	 ** @param uservalue
	 *            The user object passed to the Receive method.
	 **/
	void disconnected(XTSurl partner, Object uservalue);
	
	/**
	 * Method called when a connection has been disconnected.
	 ** 
	 * @param partner
	 *            The url passed to the Connect or Listen methods.
	 ** @param uservalue
	 *            The user object passed to the Receive method.
	 **/
	void disconnected(XTSurl partner, Object uservalue, Exception e, IConnection conn);
}
