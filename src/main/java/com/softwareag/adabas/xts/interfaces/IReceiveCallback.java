/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTSversion;

/**
 * Receive callback interface for drivers.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IReceiveCallback {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * Method called upon receipt of a message.
	 ** 
	 * @param conn
	 *            The connection for which reception occurred.
	 ** @param message
	 *            The message that was received.
	 ** @param uservalue
	 *            The user object passed to the Receive method.
	 **/
	void received(IConnection conn, Message message, Object uservalue);

	/**
	 * Method called upon disconnection.
	 ** 
	 * @param conn
	 *            The connection for which disconnection occurred.
	 ** @param uservalue
	 *            The user object passed to the Receive method.
	 **/
	void connectionLost(IConnection conn, Object uservalue);
}
