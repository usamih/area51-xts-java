/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This test program implements a rudimentary server that passes thread(s)
 * to XTS for use in callback.
 *
 * usage: java XTSserver server-name URL [number-of-threads]
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  26Oct00    usaco   [0001]      202429       Remove DNSdir support  
//
//------------------------------------------------------------------------

package com.softwareag.adabas.xts.test;

// import java.awt.*;
// import java.awt.event.*;
// import javax.swing.*;
import com.softwareag.adabas.xts.*;
import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.directory.Directory;
import com.softwareag.adabas.xts.directory.INIdir;
import com.softwareag.adabas.xts.helpers.Token;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;

public class XTSserver2 implements IXTSreceiver, IXTStransmitter, Runnable {
	static XTSserver2 dummy = new XTSserver2();
	static boolean first = true;
	static String[] serverName;
	static boolean trace = false;
	// static XTSurl url;
	static int[] serverID;
	static Directory directory = null;
	static int threads = 1;
	private static final int maxMsg = 10000000;
	private static final String msgString = "Hello, your message was received. ";
	private static int mSize = msgString.length();
	private static byte[] msg = null;

	public static final void main(final String[] args) throws Exception {
		int i;

		System.setProperty("XTSDIR", "C:\\Users\\usamih\\workspace\\XTS\\java\\test\\server");
		System.setProperty("XTS_ENABLE_LOG4J", "NO");
		System.out.println("Starting XTSServer2 ...");
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
			XTStrace.verbose("Starting XTSServer2 ...");
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

		s = Parm.getparm("NbrThreads", args, false);
		if (s != null) {
			threads = Integer.parseInt(s);
			if (trace) {
				XTStrace.verbose("Number of threads=" + s);
			}
		}
		System.out.println("NbrThreads: " + threads);

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

		if (serverName != null) {
			for (i = 0; i < serverName.length; i++) {
				try {
					XTS.register(serverName[i], dummy, null);
					System.out.println("Server " + serverName[i] + " Registered");
					if (trace) {
						XTStrace.verbose("Server " + serverName[i] + " Registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server " + serverName[i] + " Registration Failed ");
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
						XTStrace.verbose("Server ID " + serverID[i] + " Registered");
					}
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
					System.out.println("Server ID " + serverID[i] + " Registration Failed");
					System.out.println(e);
				}
			}
		}

		while (threads > 1) {
			XTSserver2 x = new XTSserver2();
			new Thread(x).start();
			threads--;
		}

		XTS.giveThread();
	}

	public final void received(final Message p, final Object uservalue,
			final Token token) {
		Message q = Message.newMessage(msg.length);
		System.arraycopy(msg, 0, q.body, 0, msg.length);
		try {
			XTS.sendViaReturn(q, token, this, null);
		} catch (XTSException e) {
			if (trace) {
				XTStrace.verbose(e);
			}
		}
		// out(new String(q.body,0,q.length));
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

	public final void run() {
		XTS.giveThread();
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
