/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.helpers;

import java.util.concurrent.atomic.AtomicInteger;

import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;

//----------------------------------------------------------------------
/**
 * Status object. Used by threads of protocol drivers and others.
 ** 
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022
// ----------------------------------------------------------------------
public class Status {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	/** The URL associated with the connection. **/
	public XTSurl url;
	/** The number of messages received since starting. **/
	public int messagesIn = 0;
	/** The number of messages transmitted since starting. **/
	public int messagesOut = 0;
	/** The total number of bytes received. **/
	public long bytesIn = 0;
	/** The total number of bytes sent. **/
	public long bytes_out = 0;
	/** The description of status for reception. **/
	public String expIn = "Initialized";
	/** The description of status for transmission. **/
	public String expOut = expIn;
	// Trace indicator - set to true if tracing must be done. **/
	// public boolean trace;
	/** Anchor for process monitoring threads. **/
	public Object monitor = null; // for monitoring
	/** unique status identifier. **/
	public final int idno = getidno(); // unique id number
	/** Trace identifier. **/
	private String id = ""; // identifier for traces
	/** The URL associated with the driver. **/
	public XTSurl driverUrl; // url from driver object

	/** Default constructor. **/
	public Status() {
	}

	/**
	 * Constructor that sets tracing ID.
	 ** 
	 * @param id
	 *            sets the ID for tracing to the given value.
	 **/
	public Status(final String id) {
		this.id = id + ": ";
	}

	/**
	 * Set the 'in' status value.
	 ** 
	 * @param s
	 *            the new status string.
	 **/
	public final void setStatus(final String s) {
		expIn = s;
		XTStrace.verbose(id + url + " " + s);
	}

	/**
	 * Set the 'out' status value.
	 ** 
	 * @param s
	 *            the new status string.
	 **/
	public final void setStatusOut(final String s) {
		expOut = s;
		XTStrace.verbose(id + url + " " + s);
	}

	private static AtomicInteger nextid = new AtomicInteger(0);

	private static final int getidno() {
		return nextid.getAndIncrement();
	}

}
