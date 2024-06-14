/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program is the client for benchmarking using asynchronous
 * calls.
 *
 * usage: java XTSclient2 serverid url
 * Change History
 * KT.1  Add DefaultDirectory allocation if no/invalid dir parm
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  26Oct00    usaco   [0001]      202429       Remove DNSdir support  
//
//------------------------------------------------------------------------

package com.softwareag.adabas.xts.test;

import com.softwareag.adabas.xts.*;
import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.directory.Directory;
import com.softwareag.adabas.xts.directory.INIdir;
import com.softwareag.adabas.xts.helpers.Token;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;

public class XTSclient2 implements IXTSreceiver, IXTStransmitter {
	private static IXTSreceiver dummy = new XTSclient2();
	private static IXTStransmitter dummy2 = (IXTStransmitter) dummy;
	private static int received;
	private static String[] serverName;
	private static boolean trace = false;
	// private static XTSurl url;
	private static int[] serverID;
	private static int timeout = 20000;
	private static int count = 1;
	private static Directory directory;
	private static int i;
	private static int j;
	private static int k;
	private static final int maxMsg = 10000000;
	private static final String msgString = " Hello, this is your client calling.";

	public static final void main(final String[] args) throws Exception {
		Message q = null;
		int total_time = 0;
		int mSize = msgString.length();

		System.setProperty("XTSDIR", "C:\\Users\\usamih\\workspace\\XTS\\java\\test\\client");
		System.setProperty("XTS_ENABLE_LOG4J", "NO");
		System.out.println("XTSclient2 - ASYNCHRONOUS Send Client Test Program");

		if (args.length > 0 && args[0].equals("?")) {
			KeywordMsg.print();
			return;
		}
		System.out.println("Settings for this Run:");

		String s = Parm.getparm("trace", args, false);
		if (s != null) {
			trace = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on");
		}
		if (trace) {
			System.setProperty("XTSTRACE", "0xffff"); // with XTS trace
			XTS.setTrace(65535);
			System.out.println("TRACE ON");
			XTStrace.verbose("XTSclient2 - ASYNCHRONOUS Send Client Test Program");
		} else {
			System.out.println("TRACE OFF");
			XTS.setTrace(0);
		}

		s = Parm.getparm("serverID", args, false);
		if (s != null) {
			serverID = Parm.getIntArray(s);
			for (i = 0; i < serverID.length; i++) {
				if (trace) {
					XTStrace.verbose("ServerID=" + serverID[i]);
				}
				System.out.println("Server ID: " + serverID[i]);
			}
		}

		if (serverID == null) {
			s = Parm.getparm("serverNm", args, true);
			if (s == null) {
				System.exit(9);
			}
		} else {
			s = Parm.getparm("serverNm", args, false);
		}
		if (s != null) {
			serverName = Parm.getStringArray(s);
			for (i = 0; i < serverName.length; i++) {
				if (trace) {
					XTStrace.verbose("ServerName=" + serverName[i]);
				}
				System.out.println("Server Name: " + serverName[i]);
			}
		}

		s = Parm.getparm("dir", args, false);
		if (s != null) { 
			if (s.equalsIgnoreCase("i")) {
				directory = new INIdir();
				System.out.println("Directory: INI");
				if (trace) {
					XTStrace.verbose("Using INI Directory Services");
				}
			} else {
				directory = new DefaultDirectory(); // KT.1
				System.out.println("Directory: DefaultDirectory");
				if (trace) {
					XTStrace.verbose("Using user provided URL or Directory Server Services");
				}
			}
		} else {
			directory = new DefaultDirectory(); // KT.1
			System.out.println("Directory: DefaultDirectory");
			if (trace) {
				XTStrace.verbose("Using user provided URL or Directory Server Services");
			}
		}
		XTS.setDirectory(directory); // KT.1

		s = Parm.getparm("dirparms", args, false);
		if (s != null) {
			directory.setParameters(s);
			System.out.println("DirParms: " + s);
			if (trace) {
				XTStrace.verbose("Directory Parameters=" + s);
			}
		}

		s = Parm.getparm("count", args, false);
		if (s != null) {
			count = Integer.parseInt(s);
		}
		System.out.println("Count: " + count);

		s = Parm.getparm("timeout", args, false);
		if (s != null) {
			timeout = Integer.parseInt(s);
		}
		System.out.println("Timeout: " + timeout);

		if (trace) {
			XTStrace.verbose("Count=" + count);
			XTStrace.verbose("Timeout=" + timeout);
		}

		s = Parm.getparm("message-size", args, false);
		if (s != null) {
			mSize = Integer.parseInt(s);
			if (mSize < 1) {
				mSize = msgString.length();
			} else if (mSize > maxMsg) {
				mSize = maxMsg;
			}
		}
		System.out.println("Message Size: " + mSize);
		if (trace) {
			XTStrace.verbose("Message Size=" + mSize);
		}
		String sMsg = "";
		for (i = mSize; i > 0;) {
			if (i < msgString.length()) {
				sMsg += msgString.substring(0, i);
			} else {
				sMsg += msgString;
			}
			i -= msgString.length();
		}
		byte[] msg = sMsg.getBytes();

		Message p = Message.newMessage(msg.length);
		System.arraycopy(msg, 0, p.body, 0, msg.length);

		// Thread th=Thread.currentThread();

		if (serverName != null) {
			for (k = 0; k < serverName.length; k++) {// Run the thru the Timings
				//1 server at a time

				try {
					// send_msg:
					for (j = 0; j < count; j++) {
						long time = System.currentTimeMillis();
						for (i = 0; i < 1000; i++) {
							XTS.send(new XTSSendParameters(serverName[k], p.cloneMessage(),	timeout, dummy2, null, dummy, null));
						}
						while (received != 1000) {
							Thread.yield();
						}
						received = 0; // reset received counter
						time = System.currentTimeMillis() - time;
						total_time += time;
						System.out.println("Calls per second for Server " + serverName[k] + ": " + 1000000L / time);
					}
					System.out.println("Average calls per second for Server " + serverName[k] + ": " + (1000000L * count) / total_time);
					total_time = 0;
				} catch (XTSException e) {
					i = 1000; // force end of loop - Process next Server
					j = 2147483646; // *****************
					received = 1000; // *****************
					if (trace) {
						XTStrace.verbose(e);
					}
				}
			}
		}

		if (serverID != null) {
			for (k = 0; k < serverID.length; k++) {// Run the thru the Timings 1
													// server at a time

				try {
					for (j = 0; j < count; j++) {// received=0;
						long time = System.currentTimeMillis();
						for (i = 0; i < 1000; i++) {
							XTS.send(new XTSSendParameters(serverID[k], p.cloneMessage(), timeout, dummy, null));
						}
						while (received != 1000) {
							Thread.yield();
						}
						received = 0; // reset received counter
						time = System.currentTimeMillis() - time;
						total_time += time;
						System.out.println("Calls per second for Server " + serverID[k] + ": " + 1000000L / time);
					}
					System.out.println("Average calls per second for Server " + serverID[k] + ": " + (1000000L * count) / total_time);
					total_time = 0;
				} catch (XTSException e) {
					i = 1000; // force end of loop - Process next Server
					j = 2147483646; // *****************
					received = 1000; // *****************
					if (trace) {
						XTStrace.verbose(e);
					}
				}
			}
		}
		XTS.shutdown();
		return;
	}

	public final void received(final Message p, final Object uservalue,
			final Token token) {
		received++;
		p.freeMessage("");
	}

	public final void receiveFailed(final Message p, final Object uservalue) {// received++;
		p.freeMessage("");
		if (trace) {
			XTStrace.verbose("XTSclient2 Receive FAILED! " + p.toString());
		}
		i = 1000; // force end of loop - Process next Server
		j = 2147483646; // *****************
		received = 1000; // *****************
	}

	public final boolean sendComplete(final Message p, final Object uservalue) {
		if (trace) {
			XTStrace.verbose("XTSclient2 Send Completed " + p.toString());
		}

		return (false);
	}

	public final void sendFailed(final Message p, final Object uservalue) {
		if (trace) {
			XTStrace.verbose("XTSclient2 Send FAILED! " + p.toString());
		}
		// p.freeMessage();
		i = 1000; // force end of loop - Process next Server
		j = 2147483646; // *****************
		received = 1000; // *****************
	}

	@Override
	public void setConnection(final IConnection connection) {
	}
}
