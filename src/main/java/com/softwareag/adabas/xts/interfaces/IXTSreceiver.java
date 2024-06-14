/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.helpers.Token;

/**
 * The callback interface for XTS user.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IXTSreceiver {

	/**
	 * Called by XTS when a complete message has been received. It is the
	 * responsbility of the called object to free the message.
	 ** 
	 * @param p
	 *            the message that was received.
	 ** @param uservalue
	 *            the object passed to the register() or send() method.
	 ** @param token
	 *            the token representing the other partner.
	 **/
	void received(Message p, Object uservalue, Token token);

	/**
	 * Called by XTS when reception has failed. It is the responsbility of the
	 * called object to free the message.
	 ** 
	 * @param p
	 *            partial received message.
	 ** @param uservalue
	 *            the object passed to the register() or send() method.
	 **/
	void receiveFailed(Message p, Object uservalue);
	public void setConnection(IConnection connection);
	
}
