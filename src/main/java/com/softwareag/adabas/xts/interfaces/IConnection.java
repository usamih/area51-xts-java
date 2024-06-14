/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.helpers.Status;

/**
 * An object representing a connection.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IConnection {

	/**
	 * Send a message to a connection.
	 ** 
	 * @param p
	 *            the message to send.
	 ** @param callback
	 *            indicates where to call back upon send completion or failure.
	 *            May be set to <i>null</i> in which case no callback occurrs.
	 ** @param uservalue
	 *            an object to be passed to the callback method.
	 ** @return a flag indicating failure.
	 **/
	boolean send(Message p, ITransmitCallback callback, Object uservalue);

	/**
	 * Set receive parameters.
	 ** 
	 * @param callback
	 *            indicates where to call back upon receipt of a message. May be
	 *            set to <i>null</i> in which case no callback occurrs.
	 ** @param uservalue
	 *            an object to be passed to the callback method.
	 **/
	void receive(IReceiveCallback callback, Object uservalue);

	/**
	 * Get the object passed to the <i>Receive</i> method.
	 ** 
	 * @return the object (uservalue) originally passed to the <i>Receive</i>
	 *         method.
	 **/
	Object getUserValue();

	/** Close the connection. **/
	void close();
	boolean isClosed();

	/** Get status of the connection. **/
	Status getStatus();

	/** Lock connection usage */
	boolean setLock();
	boolean isFree();
	void setFree();
	public int usage();
	int increaseUsage();
	int releaseUsage();
	
	public XTSurl getUrl();
}
