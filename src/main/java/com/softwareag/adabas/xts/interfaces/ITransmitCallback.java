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
 * Transmit callback for protocol drivers.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface ITransmitCallback {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * Method called when transmission completed.
	 ** 
	 * @param conn
	 *            The connection for which transmission completed.
	 ** @param p
	 *            The message that was sent.
	 ** @param uservalue
	 *            The object passed to the send routine.
	 **/
	void transmitted(IConnection conn, Message p, Object uservalue);

	/**
	 * Method called when transmission has failed.
	 ** 
	 * @param conn
	 *            The connection for which transmission failed.
	 ** @param p
	 *            The message that was to be sent.
	 ** @param uservalue
	 *            The object passed to the send routine.
	 **/
	void transmitFailed(IConnection conn, Message p, Object uservalue);
}
