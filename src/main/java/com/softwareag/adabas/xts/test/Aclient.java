package com.softwareag.adabas.xts.test;

/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program is the client for testing asynchronous calls.
 *
 * usage: java Aclient serverid url
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  26Oct00    usaco   [0001]      202429       Remove DNSdir support  
//
//-----------------------------------------------------------------------
import com.softwareag.adabas.xts.*;
import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.directory.Directory;
import com.softwareag.adabas.xts.directory.INIdir;
import com.softwareag.adabas.xts.helpers.Token;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;

public class Aclient implements IXTSreceiver, IXTStransmitter {
	private static IXTSreceiver dummy = new Aclient();
	private static IXTStransmitter dummy2 = (IXTStransmitter) dummy;
	private static int receiveCnt = 0;
	private static int receiveFailedCnt = 0;
	private static int sendCnt = 0;
	private static int sendFailedCnt = 0;
	private static int sentCnt = 0;
	private static String[] serverName;
	private static boolean trace = false;
	// private static XTSurl url;
	private static int[] serverID;
	private static int timeout = 20000;
	private static int count = 1;
	private static int countTotal = 0;
	private static Directory directory;
	private static int i;
	private static int j;
	private static int k;
	private static Object waitLock = new Object();
	private static final int maxMsg = 10000000;
	private static final String msgString = " Hello, this is Aclient calling.";

	public static final void main(final String[] args) throws Exception {
		// Message q = null;
		// int total_time = 0;
		int mSize = msgString.length();

		System.out.println("Aclient - Client ASYNCHRONOUS Send Test Program");

		if (args.length > 0 && args[0].equals("?")) {
			KeywordMsg.print();
			return;
		}
		System.out.println("Aclient - Settings for this Run:");

		String s = Parm.getparm("trace", args, false);
		if (s != null) {
			trace = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on");
		}
		if (trace) {
			XTS.setTrace(65535);
			System.out.println("Aclient - TRACE ON");
			XTStrace.verbose("Aclient - Client ASYNCHRONOUS Send Test Program");
		} else {
			System.out.println("TRACE OFF");
		}

		s = Parm.getparm("serverID", args, false);
		if (s != null) {
			serverID = Parm.getIntArray(s);
			for (i = 0; i < serverID.length; i++) {
				if (trace) {
					XTStrace.verbose("Aclient - ServerID=" + serverID[i]);
				}
				System.out.println("Aclient - Server ID: " + serverID[i]);
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
					XTStrace.verbose("Aclient - ServerName=" + serverName[i]);
				}
				System.out.println("Aclient - Server Name: " + serverName[i]);
			}
		}

		s = Parm.getparm("dir", args, false);
		if (s != null) { 
			if (s.equalsIgnoreCase("i")) {
				directory = new INIdir();
				System.out.println("Aclient - Directory: INI");
				if (trace) {
					XTStrace.verbose("Aclient - Using INI Directory Services");
				}
			} else {
				directory = new DefaultDirectory(); // KT.1
				System.out.println("Aclient - Directory: DefaultDirectory");
				if (trace) {
					XTStrace.verbose("Aclient - Using user provided URL or Directory Server Services");
				}
			}
		} else {
			directory = new DefaultDirectory(); // KT.1
			System.out.println("Aclient - Directory: DefaultDirectory");
			if (trace) {
				XTStrace.verbose("Aclient - Using user provided URL or Directory Server Services");
			}
		}
		XTS.setDirectory(directory); // KT.1

		s = Parm.getparm("dirparms", args, false);
		if (s != null) {
			directory.setParameters(s);
			System.out.println("Aclient - DirParms: " + s);
			if (trace) {
				XTStrace.verbose("Aclient - Directory Parameters=" + s);
			}
		}

		s = Parm.getparm("count", args, false);
		if (s != null) {
			count = Integer.parseInt(s);
			if (count < 1) {
				count = 1;
			}
		}
		System.out.println("Aclient - Count: " + count);

		s = Parm.getparm("timeout", args, false);
		if (s != null) {
			timeout = Integer.parseInt(s);
		}
		System.out.println("Aclient - Timeout: " + timeout);

		if (trace) {
			XTStrace.verbose("Aclient - Count=" + count);
			XTStrace.verbose("Aclient - Timeout=" + timeout);
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
		System.out.println("Aclient - Message Size: " + mSize);
		if (trace) {
			XTStrace.verbose("Aclient - Message Size=" + mSize);
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

		// Thread th = Thread.currentThread();

		if (serverName != null && serverName.length > 0) {
			countTotal = count * serverName.length;
		}
		if (serverID != null && serverID.length > 0) {
			countTotal += count * serverID.length;
		}
		if (trace) {
			XTStrace.verbose("Aclient - number of servers * count = total messages to send=" + countTotal);
		}
		System.out.println("Aclient - number of servers * count = total messages to send=" + countTotal);
		if (serverName != null) {
			for (k = 0; k < serverName.length; k++) {
				if (trace) {
					XTStrace.verbose("Aclient - Sending messages to serverID=" + serverName[k]);
				}
				System.out.println("Aclient - Sending messages to serverID="
						+ serverName[k]);
				try {
					for (j = 0; j < count; j++) {
						XTS.send(new XTSSendParameters(serverName[k], p.cloneMessage(),timeout, dummy2, null, dummy, null));
						sentCnt++;
						Thread.yield();
					}
				} catch (XTSException e) {
					countTotal -= k + 1;
					countTotal += count;
					System.out.println("Aclient - Error=" + e);
					System.out.println("Aclient - new total message count="	+ countTotal);
					if (trace) {
						XTStrace.verbose("Aclient - Error=" + e);
						XTStrace.verbose("Aclient - new total message count=" + countTotal);
					}
				}
			}
		}

		if (serverID != null) {
			for (k = 0; k < serverID.length; k++) {
				if (trace) {
					XTStrace.verbose("Aclient - Sending messages to serverID=" + serverID[k]);
				}
				System.out.println("Aclient - Sending messages to serverID="+ serverID[k]);
				try {
					for (j = 0; j < count; j++) {
						XTS.send(new XTSSendParameters(serverID[k], p.cloneMessage(), timeout, dummy, null));
						sentCnt++;
						Thread.yield();
					}
				} catch (XTSException e) {
					countTotal -= k + 1;
					countTotal += count;
					System.out.println("Aclient - Error=" + e);
					System.out.println("Aclient - new total message count="	+ countTotal);
					if (trace) {
						XTStrace.verbose("Aclient - Error=" + e);
						XTStrace.verbose("Aclient - new total message count=" + countTotal);
					}
				}
			}
		}
		synchronized (waitLock) {
			if (receiveCnt + receiveFailedCnt < sentCnt) {
				try {
					waitLock.wait();
				} catch (InterruptedException ie) {
				}
			}
		}
		System.out.println("Aclient - Send Completed Count=" + sendCnt);
		if (sendFailedCnt > 0) {
			System.out.println("Aclient - Send FAILED Count=" + sendFailedCnt);
		}
		System.out.println("Aclient - Total messages received=" + receiveCnt);
		if (receiveFailedCnt > 0) {
			System.out.println("Aclient - Receive FAILED Count=" + receiveFailedCnt);
		}
		if (trace) {
			XTStrace.verbose("Aclient - Send Completed Count=" + sendCnt);
			if (sendFailedCnt > 0) {
				XTStrace.verbose("Aclient - Send FAILED Count=" + sendFailedCnt);
			}
			XTStrace.verbose("Aclient - Total messages received=" + receiveCnt);
			if (receiveFailedCnt > 0) {
				XTStrace.verbose("Aclient - Receive FAILED Count=" + receiveFailedCnt);
			}
		}
		XTS.shutdown();
		return;
	}

	public final void received(final Message p, final Object uservalue, final Token token) {
		receiveCnt++;
		System.out.println("Aclient - received message=" + p.body.toString());
		if (trace) {
			XTStrace.verbose("Aclient - received message=" + p.body.toString());
		}
		p.freeMessage("received");
		checkNotify();
	}

	public final void receiveFailed(final Message p, final Object uservalue) {
		receiveFailedCnt++;
		p.freeMessage("receiveFailed");
		System.out.println("Aclient - Receive FAILED! " + p.toString());
		if (trace) {
			XTStrace.verbose("Aclient - Receive FAILED! " + p.toString());
		}
		checkNotify();
	}

	public final boolean sendComplete(final Message p, final Object uservalue) {
		sendCnt++;
		System.out.println("Aclient - Send Completed " + p.toString());
		if (trace) {
			XTStrace.verbose("Aclient - Send Completed " + p.toString());
		}
		return (false);
	}

	public final void sendFailed(final Message p, final Object uservalue) {
		sendFailedCnt++;
		p.freeMessage("sendFailed");
		System.out.println("Aclient - Send FAILED! " + p.toString());
		if (trace) {
			XTStrace.verbose("Aclient - Send FAILED! " + p.toString());
		}
	}

	public static final void checkNotify() {
		synchronized (waitLock) {
			if ((receiveCnt + receiveFailedCnt) == countTotal) {
				waitLock.notify();
			}
		}
	}

	@Override
	public void setConnection(final IConnection connection) {
	}
}
