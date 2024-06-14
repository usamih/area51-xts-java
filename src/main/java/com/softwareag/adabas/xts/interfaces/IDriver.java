/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import java.util.Enumeration;

import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.helpers.Status;

/**
 * Interface to be implemented by all protocol drivers.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface IDriver {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * Listen for an incoming connection.
	 ** 
	 * @param url
	 *            The URL of the connection to listen on.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return an intance of the protocol driver for the URL.
	 **/
	IDriver listen(XTSurl url, IConnectCallback callback, Object userval);

	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return an intance of the protocol driver for the URL.
	 **/
	IDriver connect(XTSurl url, IConnectCallback callback, Object userval);

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
	 ** @return an intance of the protocol driver for the URL.
	 **/
	IDriver connect(XTSurl url, IConnectCallback callback, Object userval, int connTo);

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
	 ** @param reconnect set to true if the driver should try to re-establish a
	 *        connection which has been severed.
	 ** @return an intance of the protocol driver for the URL.
	 **/
	IDriver connect(XTSurl url, IConnectCallback callback, Object userval,
			long retry_interval, int retry_count, boolean reconnect);

	/** Stop the instance from listening or connecting. **/
	void stopit();

	/** Stop all threads. **/
	void stopAll();

	/** Get top-level thread list. **/
	Enumeration<Thread> getThreads();

	/** Get Connection list. **/
	Enumeration<Thread> getConnections();

	/**
	 * Get status of thread.
	 ** 
	 * @return the Status object for the thread.
	 **/
	Status getStatus();

	/** Get Thread type. Listen or Connect. **/
	boolean isListener();

}
