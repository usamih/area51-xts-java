/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import com.softwareag.adabas.xts.interfaces.IXTSreceiver;

/**
 * The Server class provides 'Chirps' for a server.
 ** 
 * @version 2.1.1.0
 **/
// Maintenance:
// DATE USERID TAG# SAGSIS/VANTIVE# DESCRIPTION
// 7/29/99 rxc [0001] NONE Add DeleteAll method.
// 10/13/99 rxc [0002] NONE Add VERSION string.
// 12/02/99 rxc [0003] NONE Add new chirpUrls vector
// to support chirping.
// 15Mar00 pms [0004] NONE Add Copyright string.
// 22Mar00 pms [0005] Updated to V1118.
// 23May01 rxc [0006] Delete chirper entry when
// deleting the targetid.
// -----------------------------------------------------------------------
public class Server implements Runnable {
	public static final String VERSION = XTSversion.VERSION; // [0002]
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	public int target;
	public String targname;
	public int chirpInterval;
	public long lastChirp;
	public long startTime;
	Thread thread;
	IXTSreceiver rcb;
	Object uservalue;
	boolean isReplicate = false;
	// This vector contains all the urls associated with this server
	// When a chirp is to be issued, these connections are compared
	// to any proxy connections, if they exist. If a match occurs,
	// i.e. this vector has a connection matching a proxy connection
	// then and ONLY then do we send a chirp to that proxy.
	Vector<XTSurl> chirpUrls = new Vector<XTSurl>();

	// Static variables
	private static int nexttarg = 0x7e000001; // next target ID
	private static final Hashtable<String, Server> NameLookup = new Hashtable<String, Server>();
	private static final HashMap<Integer, Server> IDlookup = new HashMap<Integer, Server>();
	private static final Vector<Server> chirpers = new Vector<Server>();

	Server(int target, String targname, int chirpInterval, IXTSreceiver rcb, Object uservalue) {
		this(target, targname, chirpInterval, System.currentTimeMillis(), rcb, uservalue);
	}

	public Server(int target, String targname, int chirpInterval, long startTime, IXTSreceiver rcb, Object uservalue) {
		if (target == 0)
			this.target = getNextTarget();
		else
			this.target = target;
		this.targname = targname;
		this.startTime = startTime;
		this.chirpInterval = chirpInterval;
		this.rcb = rcb;
		this.uservalue = uservalue;

		if (targname != null)
			NameLookup.put(targname, this);
		if (XTStrace.bGlobalDebugEnabled) {
			if (targname != null)
				XTStrace.debug("Create Server=" + this.targname);
			else
				XTStrace.debug("Create Server=" + this.target);
		}
		IDlookup.put(this.target, this);

		lastChirp = System.currentTimeMillis();

		if (rcb != null) {
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
			chirpers.addElement(this);
		}
	}

	public static synchronized final int getNextTarget() {
		return nexttarg++;
	}

	public static final Server getByName(String s) {
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.debug("Get Server by=" + s);
		return (Server) NameLookup.get(s);
	}

	public static final Server getByID(int ID) {
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.debug("Get Server by Id=" + ID);
		return IDlookup.get(ID);
	}

	public static final void delete(String s) {
		Server ps = (Server) NameLookup.remove(s);
		if (ps != null)
			IDlookup.remove(ps.target);
	}

	public static final void delete(int ID) { // All targets are assigned an ID.

		// If the target is removed by ID, then remove the chirper
		// at the same time. Failure to do so will result in multiple
		// chirper elements for the same target.
		Server ps = (Server) IDlookup.get(ID);
		if (ps != null && ps.rcb != null) {
			chirpers.removeElement(ps);
		}
		IDlookup.remove(ID);
	}

	public static final void deleteAll() {
		NameLookup.clear();
		IDlookup.clear();
	}

	public final boolean isChirper() {
		return thread != null;
	}

	public final void run() {
		for (;;) {
			XTS.chirp(this);
			try {
				Thread.sleep(chirpInterval * 1000);
			} catch (InterruptedException ie) {
			}
		}
	}

	// Called for all connection/listens associated with this Server. [0003]
	public final void addChirpUrl(XTSurl x) {
		chirpUrls.addElement(x);
	}

	// Called for all connection/listens associated with this Server. [0003]
	public final Enumeration<XTSurl> getChirpUrls() {
		return chirpUrls.elements();
	}

	public static final void chirp_all() {
		Enumeration<Server> e = chirpers.elements();
		while (e.hasMoreElements())
			((Server) e.nextElement()).interrupt();
	}

	public final void interrupt() {
		thread.interrupt();
	}

	protected final void checkChirpInterval(XTSurl x) {
		String s = x.getValue("chirpinterval");
		try {
			chirpInterval = Integer.parseInt(s);
		} catch (Exception e) {
		}
	}

}
