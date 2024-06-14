/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program is the client for benchmarking.
 *
 * usage: java XTSclient serverid url
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

public class XTSclient {
	// private static XTSclient dummy=new XTSclient();
	private static String[] serverName;
	private static boolean trace = false;
	// private static XTSurl url;
	private static int[] serverID;
	private static int timeout = 20000;
	private static int count = 1;
	private static int threads = 1;
	private static final int maxMsg = 10000000;
	private static final String msgString = " Hello, this is your client calling.";
	private static Directory directory;

	public static final void main(final String[] args) {
		Message q = null;
		int i = 0;
		int k = 0;
		int j = 0;
		int scnt = 0;
		int total_time = 0;
		int mSize = msgString.length();

		System.setProperty("XTSDIR", "C:\\Users\\usamih\\workspace\\XTS\\java\\test\\client");
		System.setProperty("XTS_ENABLE_LOG4J", "NO");

		System.out.println("XTSclient - SYNCHRONOUS Send Client Test Program");
		try {
			if (args.length > 0 && args[0].equals("?")) {
				KeywordMsg.print();
				return;
			}
			System.out.println("Settings for this Run:");
			String s = Parm.getparm("trace", args, false);
			System.out.println("Trace parm=" + s);
			if (s != null) {
				trace = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on");
			}
			if (trace) {
				System.setProperty("XTSTRACE", "0xffff"); // with XTS trace
				XTS.setTrace(65535);
				System.out.println("TRACE ON");
				XTStrace.verbose("XTSclient SYNCHRONOUS Send Client Test Program");
			} else {
				System.out.println("TRACE OFF");
				XTS.setTrace(0);
			}

			s = Parm.getparm("serverID", args, false);
			System.out.println("serverID parm=" + s);
			if (s != null) {
				serverID = Parm.getIntArray(s);
				for (i = 0; i < serverID.length; i++) {
					if (trace) {
						XTStrace.verbose("ServerID=" + serverID[i]);
					}
					System.out.println("Server ID=" + serverID[i]);
				}
			}

			if (serverID == null) {
				s = Parm.getparm("serverNm", args, true);
				System.out.println("serverNm parm=" + s);
				if (s == null) {
					System.exit(9);
				}
			} else {
				s = Parm.getparm("serverNm", args, false);
				System.out.println("serverNm parm=" + s);
			}
			if (s != null) {
				serverName = Parm.getStringArray(s);
				for (i = 0; i < serverName.length; i++) {
					if (trace) {
						XTStrace.verbose("ServerName=" + serverName[i]);
					}
					System.out.println("Server Name=" + serverName[i]);
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
					directory = new DefaultDirectory();
					System.out.println("Directory: DefaultDirectory");
					if (trace) {
						XTStrace.verbose("Using user provided URL or Directory Server Services");
					}
				}
			} else {
				directory = new DefaultDirectory();
				System.out.println("Directory: DefaultDirectory");
				if (trace) {
					XTStrace.verbose("Using user provided URL or Directory Server Services");
				}
			}
			XTS.setDirectory(directory);

			s = Parm.getparm("dirparms", args, false);
			System.out.println("dirparms parm=" + s);
			if (s != null) {
				directory.setParameters(s);
				System.out.println("DirParms=" + s);
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

			s = Parm.getparm("NbrThreads", args, false);
			if (s != null) {
				threads = Integer.parseInt(s);
			}
			System.out.println("NbrThreads: " + threads);

			if (trace) {
				XTStrace.verbose("Count=" + count);
				XTStrace.verbose("Timeout=" + timeout);
				XTStrace.verbose("Threads=" + threads);
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
			Message p;

			p = Message.newMessage(msg.length);
			System.arraycopy(msg, 0, p.body, 0, msg.length);

			if (serverName != null) {
				scnt += serverName.length;
			}
			if (serverID != null) {
				scnt += serverID.length;
			}

			if (threads > scnt) {
				threads = scnt;
				System.out.println("Thread count > than Server count...set Thread Count to Server count");
				if (trace) {
					XTStrace.verbose("Thread count > than Server count...set Thread Count to Server count");
				}
			}

			if (serverName != null) {
				for (k = 0; k < serverName.length; k++) { // Run the thru the
					try {
						for (j = 0; j < count; j++) {
							long time = System.currentTimeMillis();
							for (i = 0; i < 1000; i++) {
								q = XTS.sendAndWait(new XTSSendParameters(serverName[k], p.cloneMessage(), timeout));
								if (i == 0) {
									System.out.println("Received: "	+ new String(q.body, 0, q.length > 69 ? 69 : q.length));
								}
			                                        if (trace) 
                                                                     XTStrace.verbose("Recv message connection token id " + p.token);
                                                                p.token  = q.token;
								q.freeMessage("client2");
							}
							time = System.currentTimeMillis() - time;
							total_time += time;
							System.out.println("Calls per second for Server " + serverName[k] + ": " + 1000000L / time);
						}
						System.out.println("Average calls per second for Server " + serverName[k] + ": " + (1000000L * count) / total_time);
						total_time = 0;
					} catch (XTSException e) {
						i = 1000; // force end of loop - Process next Server
						j = 2147483646; // *****************
						System.out.println(e);
						if (trace) {
							XTStrace.verbose(e);
						}
					}
				}
			}

			if (serverID != null) {
				for (k = 0; k < serverID.length; k++) {// Run the thru the
					// Timings 1 server at a time
					try {
						for (j = 0; j < count; j++) {
							long time = System.currentTimeMillis();
							for (i = 0; i < 10000; i++) {
			                                        if (trace) 
                                                                     XTStrace.verbose("Send message connection token id " + p.token);
								q = XTS.sendAndWait(new XTSSendParameters(serverID[k], p.cloneMessage(), timeout));
								if (i == 0) {
									System.out.println("Received: "	+ new String(q.body, 0,	q.length > 69 ? 69 : q.length));								}
			                                        if (trace) 
                                                                     XTStrace.verbose("Recv message connection token id " + p.token);
                                                                p.token  = q.token;
								q.free("client2");
							}
							time = System.currentTimeMillis() - time;
							total_time += time;
							System.out.println("Calls per second for Server ID " + serverID[k] + ": " + (10000L *1000)/ time);
						}
						System.out.println("Average calls per second for Server ID " + serverID[k] + ": " + ((10000L * count) *1000)/ total_time);
						total_time = 0;
					} catch (XTSException e) {
						i = 1000; // force end of loop - Process next Server
						j = 2147483646; // *****************
						System.out.println("********** " + e);
						if (trace) {
							XTStrace.verbose(e);
						}
					}
				}
			}
		} catch (Throwable th) {
			th.printStackTrace(System.out);
		}
		XTS.shutdown();
		System.exit(0);
	}
}
