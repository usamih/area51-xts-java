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
 * The callback interface for XTS user.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IXTStransmitter {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * Called by XTS when transmission has been completed. The called object
	 * should not free the message since this will be done by XTS.
	 ** 
	 * @param p
	 *            the message that was sent.
	 ** @param uservalue
	 *            the object passed to the send() method.
	 ** @return true = close connection just sent to
	 **/
	boolean sendComplete(Message p, Object uservalue);

	/**
	 * Called by XTS when transmission has failed. The called object should not
	 * free the message since this will be done by XTS.
	 ** 
	 * @param p
	 *            the message that cannot be delivered.
	 ** @param uservalue
	 *            the object passed to the send() method.
	 **/
	void sendFailed(Message p, Object uservalue);
}
