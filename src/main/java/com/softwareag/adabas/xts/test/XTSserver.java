/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program is the server for benchmarking.
 *
 * usage: java XTSserver serverid url
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

// import javax.swing.*;
public class XTSserver implements IXTSreceiver, IXTStransmitter // ,WindowListener
{
	private static XTSserver dummy = new XTSserver();
	private static String[] serverName;
	private static boolean trace = false;
	// private static XTSurl url;
	private static int[] serverID;
	private static Directory directory = null;
	private static final int maxMsg = 10000000;
	private static final String msgString = "Hello, your message was received. ";
	private static int mSize = msgString.length();
	private static byte[] msg = null;

	public static final void main(final String[] args) throws Exception {
		int i;

		System.setProperty("XTSDIR", "C:\\Users\\usamih\\workspace\\XTS\\java\\test\\server");
		System.setProperty("XTS_ENABLE_LOG4J", "NO");
//		System.setProperty("XTSTRACE", "0xffff"); // with XTS trace

		System.out.println("Starting XTSServer ...");
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
			System.out.println("TRACE ON");
			System.setProperty("XTSTRACE", "0xffff"); // with XTS trace
			XTS.setTrace(65535);
			XTStrace.verbose("Starting XTSServer ...");
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
				System.out.println("ServerID=" + serverID[i]);
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
				System.out.println("ServerName=" + serverName[i]);
			}
		}

		s = Parm.getparm("dir", args, false);
		System.out.println("dir parm=" + s);
		if (s != null) { 
			if (s.equalsIgnoreCase("i")) {
				directory = new INIdir("/tmp/xtsurl.cfg");
				System.out.println("Directory: INI");
				if (trace) {
					XTStrace.verbose("Using INI Directory Services");
				}
				System.out.println("Using INI Directory Services");
			} else {
				directory = new DefaultDirectory(); 
				System.out.println("Directory: DefaultDirectory");
				if (trace) {
					XTStrace.verbose("Using user provided URL or Directory Server Services");
				}
				System.out.println("Using user provided URL or Directory Server Services");
			}
		} else {
			directory = new DefaultDirectory(); 
			System.out.println("Directory: DefaultDirectory");
			if (trace) {
				XTStrace.verbose("Using user provided URL or Directory Server Services");
			}
			System.out.println("Using user provided URL or Directory Server Services");
		}
		if (directory != null) {
			XTS.setDirectory(directory);
		}

		s = Parm.getparm("dirparms", args, false);
		System.out.println("dirparms parm=" + s);
		if (s != null) {
			directory.setParameters(s);
			System.out.println("DirParms: " + s);
			if (trace) {
				XTStrace.verbose("Directory Parameters=" + s);
			}
		System.out.println("Directory Parameters=" + s);
		}

		s = Parm.getparm("message-size", args, false);
		System.out.println("message-size parm=" + s);
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
		// create outgoing message
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

		System.out.println("Server Registration... ");
		if (serverName != null) {
			for (i = 0; i < serverName.length; i++) {
				try {
					XTS.register(serverName[i], dummy, null);
					System.out.println("Server " + serverName[i] + " registered");
					if (trace) {
						XTStrace.verbose("Server " + serverName[i] + " registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server " + serverName[i] + " registration Failed ");
					System.out.println(e);
				}
			}
		}
		if (serverID != null) {
			for (i = 0; i < serverID.length; i++) {
				try {
					XTS.register(serverID[i], dummy, null);
					System.out.println("Server ID " + serverID[i] + " registered");
					if (trace) {
						XTStrace.verbose("Server ID " + serverID[i] + " registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server ID " + serverID[i] + " registration Failed");
					System.out.println(e);
				}
			}
		}
		try {
			Thread.sleep(999999999);
		} catch (InterruptedException ie) {
			System.out.println("Program Terminating");
		}
	}

	public final void received(final Message p, final Object uservalue, final Token token) {
		Message q = Message.newMessage(msg.length);
		System.arraycopy(msg, 0, q.body, 0, msg.length);
		try {
			XTS.sendViaReturn(q, token, this, null);
		} catch (XTSException e) {
			if (trace) {
				XTStrace.verbose(e);
			}
		}
		p.freeMessage("");
	}

	public final void receiveFailed(final Message p, final Object uservalue) {
		out("Receive failed");
		p.freeMessage("");
	}

	public final boolean sendComplete(final Message p, final Object uservalue) {
		return false;
	}

	public final void sendFailed(final Message p, final Object uservalue) {
		out("Send failed");
	}

	private static final void out(final String s) {
		System.out.println(s);
	}

	public final String[] getServerNames() {
		return serverName;
	}

	public final int[] getServerIDs() {
		return serverID;
	}

	@Override
	public void setConnection(final IConnection connection) {

	}

}
