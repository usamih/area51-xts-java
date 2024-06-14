/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program demonstrates bindClient() functionality.
 * On the first receive, it binds the client.
 *
 * usage: java turnsv sererID URL
 *
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  11/10/99   rxc     [0001]       NONE        Add datagram option that indicates 
//                                              bindclient processing. Yes..no reply
//                                              expected, no..reply expected.
//  26Oct00    usaco   [0002]      202429       Remove DNSdir support  
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

import java.util.*;

public class Turnsv implements IXTSreceiver, IXTStransmitter {
	static IXTSreceiver dummy = new Turnsv(); // callback for server msgs
	static IXTStransmitter dummy2 = (IXTStransmitter) dummy; // callback for server msgs

	static String[] serverName;
	static boolean trace = false;
	static boolean datagram = true;
	// static XTSurl url;
	static int[] serverID;
	static int count = 5;
	static long sleepTime = 2000;
	static Directory directory;
	static final byte[] msg2 = ("This is a server unsolicited message")
			.getBytes();
	static Thread thread;
	static Vector<String> clients = new Vector<String>(); // keeps a list of
															// clients
	static boolean busy = true; // keeps us busy
	private static final int maxMsg = 10000000;
	private static final String msgString = "Hello, your message was received. ";
	private static int mSize = msgString.length();
	private static byte[] msg = null;

	public static final void main(final String[] args) throws Exception {
		int i;

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
			XTS.setTrace(255);
			System.out.println("TRACE ON");
			XTStrace.verbose("TRACING REQUESTED");
		} else {
			System.out.println("TRACE OFF");
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
		if (s != null) { // if(s.equalsIgnoreCase("d")) // [0002]
							// { directory = new DNSdir(); // [0002]
							// System.out.println("Directory: DNS"); // [0002]
							// if(trace) // [0002]
							// XTStrace.verbose("Using DNS Directory Services");
							// // [0002]
							// } // [0002]
							// else if(s.equalsIgnoreCase("i")) // [0002]
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
		if (directory != null) {
			XTS.setDirectory(directory);
		}

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

		s = Parm.getparm("sleep", args, false);
		if (s != null) {
			sleepTime = Integer.parseInt(s);
		}
		System.out.println("Sleep: " + sleepTime);

		s = Parm.getparm("datagram", args, false);
		if (s != null) {
			if (s.equalsIgnoreCase("NO")) {
				datagram = false;
			}
		}
		if (datagram) {
			System.out.println("Datagram=yes");
		} else {

			System.out.println("Datagram=no");
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
			XTStrace.verbose("Count=" + count);
			XTStrace.verbose("Sleep=" + sleepTime);
			XTStrace.verbose("Message Size=" + mSize);
			if (datagram) {
				XTStrace.verbose("Datagram=yes");
			} else {

				XTStrace.verbose("Datagram=no");
			}
		}
		// Create outgoing message
		String sMsg = "";
		for (i = mSize; i > 0;) {
			if (i < msgString.length()) {
				sMsg += msgString.substring(0, i);
			} else {
				sMsg += msgString;
			}
			i -= msgString.length();
		}
		msg = sMsg.getBytes();

		thread = Thread.currentThread();
		Message q = Message.newMessage(msg2.length);
		System.arraycopy(msg2, 0, q.body, 0, msg2.length);

		if (serverName != null) {
			for (i = 0; i < serverName.length; i++) {
				try {
					XTS.register(serverName[i], dummy, null);
					System.out.println("Server " + serverName[i]
							+ " Registered");
					if (trace) {
						XTStrace.verbose("Server " + serverName[i]
								+ " Registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server " + serverName[i]
							+ " Registration Failed ");
					System.out.println(e);
				}
			}
		}
		if (serverID != null) {
			for (i = 0; i < serverID.length; i++) {
				try {
					XTS.register(serverID[i], dummy, null);
					System.out.println("Server ID " + serverID[i]
							+ " Registered");
					if (trace) {
						XTStrace.verbose("Server ID " + serverID[i]
								+ " Registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server ID " + serverID[i]
							+ " Registration Failed");
					System.out.println(e);
				}
			}
		}

		// Send an unsolicited message to all clients every 2 seconds

		while (busy) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ie) {
			}
			Enumeration<String> e = clients.elements();
			s = null;
			while (e.hasMoreElements()) {
				s = (String) e.nextElement();
				if (datagram) {
					try {
						XTS.send(new XTSSendParameters(s, q.cloneMessage(), 15000, dummy, s));
					} catch (XTSException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {

					try {
						XTS.send(new XTSSendParameters(s, q.cloneMessage(), 15000, dummy2, s, dummy, s));
					} catch (XTSException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}

	}

	public final void received(final Message p, final Object uservalue,
			final Token token) {
		print("Received length:" + p.length + ":"
				+ new String(p.body, 0, p.length));

		// Check if our special message

		if (p.length == 1 && p.body[0] == '%') {
			String s = "Turn" + System.currentTimeMillis(); // generate client
															// name
			if (trace) {
				XTStrace.verbose(s);
			}
			XTS.bindClient(token, s);
			clients.addElement(s);
			// if(--count<1)
			// busy=false;
		} else if (token != null) {
			// otherwise send a mundane reply
			Message q = Message.newMessage(msg.length);
			System.arraycopy(msg, 0, q.body, 0, msg.length);
			try {
				XTS.sendViaReturn(q, token, this, null);
			} catch (XTSException e) {
				if (trace) {
					XTStrace.verbose(e);
				}
			}
		}
		p.freeMessage("");
	}

	public final void receiveFailed(final Message p, final Object uservalue) {
		print("Receive failed");
		p.freeMessage("");
	}

	public final boolean sendComplete(final Message p, final Object uservalue) {
		return false;
	}

	// If sending fails then the client has gone

	public final void sendFailed(final Message p, final Object uservalue) {
		print("Send failed to client " + uservalue);
		clients.removeElement(uservalue);
		// busy=false;
	}

	private static final synchronized void print(final String s) {
		System.out.println(s);
	}

	@Override
	public void setConnection(final IConnection connection) {
	}

}
