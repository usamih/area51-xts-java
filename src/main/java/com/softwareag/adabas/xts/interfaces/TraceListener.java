/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.interfaces;

import com.softwareag.adabas.xts.XTSversion;

/**
 * If a class wishes to receive trace output from XTS then it must implement
 * this interface.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public interface TraceListener {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;

	/**
	 * The method called by XTS when tracing.
	 ** 
	 * @param s
	 *            the trace information.
	 **/
	void trace(String s);
}
