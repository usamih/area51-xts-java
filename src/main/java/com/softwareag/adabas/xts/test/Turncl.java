/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This program demonstrates a bindClient() client implementation.
 *
 * usage: java turncl serverID URL
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  11/10/99   rxc     [0001]       NONE        Add support for bindClient 
//                                              reply.
//  26Oct00    usaco   [0002]      202429       Remove DNSdir support  
//
//-----------------------------------------------------------------------

package com.softwareag.adabas.xts.test;

import com.softwareag.adabas.xts.*;
import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.directory.Directory;
import com.softwareag.adabas.xts.directory.INIdir;
import com.softwareag.adabas.xts.helpers.Token;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;

public class Turncl implements IXTSreceiver, IXTStransmitter {
	static IXTSreceiver dummy = new Turncl(); // callback for server msgs
	static IXTStransmitter dummy2 = (IXTStransmitter) dummy; // callback for server msgs

	static String[] serverName;
	static boolean trace = false;
	// static XTSurl url;
	static int[] serverID;
	static long timeout = 20000;
	static int count = 5;
	static long sleepTime = 5000;
	static Directory directory = null;
	private static final int maxMsg = 10000000;
	private static Message rMessage; // [0001]
	private static final String msgString = " Hello, this is your client calling.";
	private static final byte[] REPLY_TO_SERVER = // [0001]
	"Your unsolicited request was received.".getBytes(); // [0001]

	public static final void main(final String[] args) throws Exception {
		int i = 0;
		int k = 0;
		Message q = null;
		Message p = null;
		int mSize = msgString.length();

		System.out.println("turncl - BINDCLIENT client Test Program");
		if (trace) {
			XTStrace.verbose("turncl - BINDCLIENT client Test Program");
		}

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
		if (s != null) {
			// if(s.equalsIgnoreCase("d"))
			// { directory = new DNSdir();
			// System.out.println("Directory: DNS");
			// if(trace)
			// XTStrace.verbose("Using DNS Directory Services");
			//
			// }
			// else if(s.equalsIgnoreCase("i"))
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

		s = Parm.getparm("timeout", args, false);
		if (s != null) {
			timeout = Integer.parseInt(s);
		}
		System.out.println("Timeout: " + timeout);

		if (trace) {
			XTStrace.verbose("Count=" + count);
			XTStrace.verbose("Sleep=" + sleepTime);
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

		// Send special first message to the server saying "please bind me"

		if (serverName != null) {
			for (i = 0; i < serverName.length; i++) {
				try {
					p = Message.newMessage(1);
					p.body[0] = (byte) '%';
				//	XTSSendParameters(serverName[i], p, timeout);
					XTS.send(new XTSSendParameters(serverName[i], p, timeout, dummy2, null, dummy, null));
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
				}
			}
		}

		if (serverID != null) {
			for (i = 0; i < serverID.length; i++) {
				try {
					p = Message.newMessage(1);
					p.body[0] = (byte) '%';
					XTS.send(new XTSSendParameters(serverID[i], p, timeout, dummy2, null, dummy, null));
				} catch (XTSException e) {
					if (trace) {
						XTStrace.verbose(e);
					}
				}
			}
		}

		// Set up message to send to server in the normal way

		p = Message.newMessage(msg.length);
		System.arraycopy(msg, 0, p.body, 0, msg.length);

		// Create a server reply message - should one be expected // [0001]

		rMessage = Message.newMessage(REPLY_TO_SERVER.length); // [0001]
		System.arraycopy(REPLY_TO_SERVER, 0, rMessage.body, 0, // [0001]
				REPLY_TO_SERVER.length); // [0001]

		// Send some messages to the server and wait for replies

		if (serverName != null) {
			for (k = 0; k < serverName.length; k++) {
				for (i = 0; i < count; i++) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException ie) {
					}
					try {
						q = XTS.sendAndWait(new XTSSendParameters(serverName[k], p.cloneMessage(),
								timeout));
						print("Return from server " + serverName[k] + " :"
								+ new String(q.body, 0, q.length));
						if (trace) {
							XTStrace.verbose("Return from server "
									+ serverName[k] + " :"
									+ new String(q.body, 0, q.length));
						}
						q.freeMessage("");
					} catch (XTSException e) {
						i = count;
						if (trace) {
							XTStrace.verbose(e);
						}
					}
				}
			}
		}

		if (serverID != null) {
			for (k = 0; k < serverID.length; k++) {
				for (i = 0; i < count; i++) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException ie) {
					}
					try {
						q = XTS.sendAndWait(new XTSSendParameters(serverID[k], p.cloneMessage(),
								timeout));
						print("Return from server " + serverID[k] + " :"
								+ new String(q.body, 0, q.length));
						if (trace) {
							XTStrace.verbose("Return from server "
									+ serverID[k] + " :"
									+ new String(q.body, 0, q.length));
						}
						q.freeMessage("");
					} catch (XTSException e) {
						i = count;
						if (trace) {
							XTStrace.verbose(e);
						}
					}
				}
			}
		}
		XTS.shutdown();
		System.exit(0);
	}

	public final void received(final Message p, final Object uservalue,
			final Token token) {
		if (p.body[8] == 'T') {
			print("Server has bound us as "
					+ new String(p.body, 8, p.length - 8));
		} else {
			print("Server sent unsolicited:" + new String(p.body, 0, p.length));
		}
		// is the server expecting a reply?
		if (token != null) {
			try {
				XTS.sendViaReturn(rMessage.cloneMessage(), token, dummy2, null);
			} catch (XTSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // [0001]
		}
	}

	public final void receiveFailed(final Message p, final Object uservalue) {
		if (trace) {
			XTStrace.verbose("Received failed");
		}
	}

	public final boolean sendComplete(final Message p, final Object uservalue) // [0001]
	{
		return (false); // [0001]
	} // [0001]

	public final void sendFailed(final Message p, final Object uservalue) // [0001]
	{
		if (trace) {
			XTStrace.verbose("Send failed for message=" + p.toString()); // [0001]
		}
		p.freeMessage(""); // [0001]
	} // [0001]

	private static final synchronized void print(final String s) {
		System.out.println(s);
	}

	@Override
	public void setConnection(final IConnection connection) {
	}

}
