/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
package com.softwareag.adabas.xts.protocol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.interfaces.IConnectCallback;
import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.network.IPtransport;

//-----------------------------------------------------------------------
/**
 * This class implements a Network MHDR protocol driver for XTS.
 ** 
 **/
// -----------------------------------------------------------------------
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022
// -------------------------------------------------------------------------
class MHDR extends IPtransport {
	public static final String VERSION = XTSversion.VERSION; // [0014]
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	// PL4 revert of simple / update change
	private static final Vector<Thread> tvec = new Vector<Thread>(); // list of threads in this prot
	// static final com.softwareag.javabas.translationTables.Cp037 
	// $Cp037=new com.softwareag.javabas.translationTables.Cp037(); 
	static final com.softwareag.adabas.xts.helpers.Cp037 $Cp037 = new com.softwareag.adabas.xts.helpers.Cp037();

	static final int CHIRP = 0x7fffffff; // chirp time
	static final int NOCHIRP = 0x1; // bring it down
	static final byte[] chirpBody = new byte[16];
	// static final byte[] EYECATCHER = {(byte)'A',(byte)'1'}; 
	// static final byte[] ESTABLISH_CONTEXT_REPLY =
	// {(byte)'A',(byte)'1',-1,(byte)0x81}; //[0004]
	// static final byte[] BAD_PASSWORD = {(byte)'A',(byte)'1',-1,(byte)0xc1};
	// //[0004]
	static final byte[] EYECATCHER = { (byte) 0x41, (byte) 0x31 };
	//
	static final byte[] ESTABLISH_CONTEXT_REPLY = { (byte) 0x41, (byte) 0x31, (byte) 0xff, (byte) 0x81 };
	static final byte[] BAD_PASSWORD = { (byte) 0x41, (byte) 0x31, (byte) 0xff, (byte) 0xc1 };
	static final int HDR_LEN = 12;
	static final int UBBIN = 2; // offset of ubbin
	static final int UBBOUT = 3; // offset of ubbin
	// static final int CTX = 4; // offset of context //[0006]
	public static final int CTX = 4; // offset of context //[0006]
	public static final int CTXVER = 8; // offset of context verifyier //[0006]
	//
	static String nodeName = null; // Node Name set by caller [0003]
	static {
		long x = System.currentTimeMillis();
		for (int i = 0; i < 8; i++) {
			chirpBody[i] = (byte) (x >> (56 - (i * 8)));
		}
	}

	private boolean isEBCDIC = false; // from URL

	/** Default constructor. **/
	public MHDR() {
		super(tvec);
	}

	private MHDR(final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect);
		protocol = "mhdr";
		String s = url.getValue("charset"); 
		if (s != null) {
			isEBCDIC = s.equalsIgnoreCase("ebcdic"); 
		}
	}
	
	private MHDR(final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect, final int connTo) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect, connTo);
		protocol = "mhdr";
		String s = url.getValue("charset"); 
		if (s != null) {
			isEBCDIC = s.equalsIgnoreCase("ebcdic"); 
		}
	}


	/**
	 * Listen for an incoming connection.
	 ** 
	 * @param url
	 *            The URL of the connection to listen on.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return an instance of the driver.
	 **/
	public final IDriver listen(final XTSurl url, final IConnectCallback callback, final Object userval) {
		MHDR mhdr = new MHDR(url, true, callback, userval, 0, 0, false);
		mhdr.start();
		return mhdr;
	}

	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @param connTo
	 *            the time to wait for a connection to be established.
	 ** @return an instance of the driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final int connTo) {
		MHDR mhdr = new MHDR(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false, connTo);
		mhdr.start();
		return mhdr;
	}
	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @return an instance of the driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval) {
		MHDR mhdr = new MHDR(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false);
		mhdr.start();
		return mhdr;
	}

	/**
	 * Establish a connection.
	 ** 
	 * @param url
	 *            The URL to connect to.
	 ** @param callback
	 *            indicates where to callback when connection established.
	 ** @param userval
	 *            the user value to be passed to the callback method.
	 ** @param retry_interval
	 *            the new retry interval if connection attempt fails.
	 ** @param retry_count
	 *            the number of times to retry to connect. Set this value to 0
	 *            if no retry is needed.
	 ** @param <reconnect set to true if the driver should try to re-establish a
	 *        connection which has been severed.
	 ** @return an instance of the driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect) {
		MHDR mhdr = new MHDR(url, false, callback, userval, retry_interval, retry_count, reconnect);
		mhdr.start();
		return mhdr;
	}

	protected final Object getToken(final IPtx tx) throws IOException {
		MHDRtoken t = new MHDRtoken();
		t.isEBCDIC = isEBCDIC;
		// tx.token=t; move to synchronized
		String st = url.getValue("node");
		// if(st!=null) try
		// { t.node=Short.parseShort(st); }
		if (st != null) {
			try {
				t.node = Integer.parseInt(st); // [0014]
				if (t.node > 65535)
					t.node = 65535; // [0014]
			} catch (Exception e) {
			}
		}

		st = url.getValue("nodename");

		if (st != null) {
			t.nodename = $Cp037.getBytes((st.toUpperCase() + "        ").substring(0, 8));
			// else t.nodename=$Cp037.getBytes("XTSNODE ");
		} else {
			if (nodeName == null) {
				t.nodename = $Cp037.getBytes("XTSNODE ");

			} else {

				t.nodename = $Cp037.getBytes(nodeName);
			}
		}

		st = url.getValue("security");
		if (st != null) {
			FileReader f = null;
			BufferedReader br = null;
			try {
				t.security = new Hashtable<String, String>();
				f = new FileReader(st);
				br = new BufferedReader(f);
				st = br.readLine();
				while (st != null) {
					t.security.put(st, st);
					st = br.readLine();
				}
			} catch (Exception ex) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(ex);
			} finally {
				if (br != null) {
					br.close();
				}
				if (f != null) {
					f.close();
				}
			}
		}

		synchronized (tx) {
			tx.token = t; // chicken and egg problem
			if (listener) {
				tx.doze("wait conreq"); // wait for connect request
				tx.dos.writeLong(0x01080000L); // connect response
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose ("conrsp");
				tx.doze("wait MHDR req");
				tx.dos.writeLong(0x00800000010e0000L);
				// tx.dos.writeLong(0x4501020100000004L);
				tx.dos.writeInt(0x45010201); // [0017]
				tx.dos.writeInt(0x00000004 | (t.node << 16)); // [0017]
				tx.dos.writeLong(0x50584c20d4c8c4d9L);
				// tx.dos.writeLong(0x54502d57524b3120L);
				// tx.dos.writeLong(0x4e4554574b524832L);
				tx.dos.write(t.nodename);
				tx.dos.writeLong(0x2020202020202020L);
				tx.dos.writeLong(0x00000001c9c2d461L);
				tx.dos.writeLong(0xf3f9f04000000000L);
				tx.dos.writeLong(0x0000ffcc00000080L);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose ("MHDR connection rsp");
				tx.doze("wait H1"); // wait for H1
				sendhs(tx, 0xc8f2); // send H2
				tx.doze("wait B2"); // wait for B2
			} else {
				tx.dos.writeLong(0x01090000L); // connect request
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose ("MHDR conreq");
				tx.doze("wait conresp"); // wait for connect response
				tx.dos.writeLong(0x00800000010e0000L);
				// tx.dos.writeLong(0x4301020100000004L | (t.node<<16)); // RDA
				// neg
				tx.dos.writeInt(0x43010201); // [0017]
				tx.dos.writeInt(0x00000004 | (t.node << 16)); // [0017]
				tx.dos.writeLong(0x50584c2078787878L);
				// tx.dos.writeLong(0x4e4554574b524832L);
				// tx.dos.writeLong(0x54502d57524b3120L);
				tx.dos.writeLong(0x2020202020202020L);
				tx.dos.write(t.nodename);
				tx.dos.writeLong(0x00000000c9c2d461L);
				tx.dos.writeLong(0xf3f9f04000000000L);
				tx.dos.writeLong(0x0000ffcc00000080L);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				XTStrace.verbose ("MHDR connection req");
				tx.doze("wait MHDR rsp");
				sendhs(tx, 0xc8f1); // send H1
				tx.doze("wait H2"); // wait for H2
				sendhs(tx, 0xc2f2); // send B2
			}
		}
		tx.status.setStatus("rcvwait");
		t.mhdr[16] = 0; // reset mhdr bytes
		t.mhdr[17] = 0;
		return t;
	}

	protected final void transmit(final Message p, final IPtx tx) throws IOException {
		if (p.target < 0) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("MHDR return: do not transmit XTS ADMIN message");// rxc
			return;
		} // rxc
		if (p.length == 0) {// admin message
			do_chirp(tx.twin, (byte) (p.target >> 8), (byte) p.target, CHIRP);
			return;
		}

		MHDRtoken t = (MHDRtoken) tx.token;
		if (p.body[UBBIN] == -1) {// control message

			if (tx.twin.callback != null) {
				switch (p.body[UBBOUT]) {
				case 1: // establish context
					Message q = checkSecurity(t, p);
					// q.trace("Receive");
					tx.twin.callback.received(tx, q, tx.twin.userval);
					p.length = 0;
					break;
				case 2: // destroy context
					// q=Message.newMessage(0); //[0015]
					// q.trace("Receive"); //[0015]
					q = p.cloneMessage(); // Create a copy of message //[0015]
					q.msgno = p.msgno; // [0015]
					q.reset(); // [0015]
					q.skip(3); // point to flags byte //[0015]
					q.put((byte) 0x82); // Set Reply destroy context //[0015]
					q.target = XTS.ROUTE_RETURN; // via route return //[0015]
					q.route = p.route; // Send message back. //[0015]
					q.ttl = 16; // [0002] //[0015]
					tx.twin.callback.received(tx, q, tx.twin.userval);
					p.length = 0;
					break;
				default:
					throw new RuntimeException("MHDR transmit: Huh?? strange Adabas msg " + p.body[UBBOUT]);
				}
			}
			return;
		}

		boolean bigend = (p.body[CTX + 3] & 0x01) == 0; // low-order bit in
		// context
		boolean ebcdic = (p.body[CTX + 3] & 0x02) == 0; // EBCDIC
		byte[] ctxSave = new byte[8];
		System.arraycopy(p.body, CTX, ctxSave, 0, 8);
		t.ctxInfo.put(Integer.valueOf(p.route), ctxSave);

		int tlen = p.length + 0xc0 - HDR_LEN; // amount of data
		t.mhdr[0] = (byte) (tlen >> 8); // set length
		t.mhdr[1] = (byte) tlen; // set length
		t.mhdr[14] = t.mhdr[0];
		t.mhdr[15] = t.mhdr[1];
		t.mhdr[24] = (byte) 0xf2; // [0007] [0008] per Mike Dibacco

		tx.dos.write(t.mhdr); // write it out

		tx.dos.writeShort(t.pnode); // partner node
		tx.dos.writeShort(p.target); // message target
		tx.dos.writeInt(p.route); // handle
		tx.dos.writeLong(0);
		tx.dos.writeLong(0x4000080002L);
		tx.dos.writeLong(((long) t.node) << 48); // our node
		tx.dos.writeShort(0); // 1st two UB bytes
		tx.dos.write(p.body, UBBIN, 2); // UBBIN/UBBOUT

		// tx.dos.writeLong(0x04010000L); [0005] Before
		tx.dos.writeLong(0x01000000L); // Per MDB

		tx.dos.writeInt(p.target << 16);
		int tbuf = 0x6c; // overhead, pal+acb
		byte b = (byte) (p.body[UBBIN] | p.body[UBBOUT]); // buffers out or in
		if (bigend) {
			if ((b & 0x10) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x18] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x19] & 0xff);
			}
			if ((b & 0x08) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1a] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1b] & 0xff);
			}
			if ((b & 0x04) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1c] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1d] & 0xff);
			}
			if ((b & 0x02) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1e] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1f] & 0xff);
			}
			if ((b & 0x01) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x20] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x21] & 0xff);
			}
		} else {
			if ((b & 0x10) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x19] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x18] & 0xff);
			}
			if ((b & 0x08) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1b] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1a] & 0xff);
			}
			if ((b & 0x04) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1d] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1c] & 0xff);
			}
			if ((b & 0x02) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1f] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1e] & 0xff);
			}
			if ((b & 0x01) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x21] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x20] & 0xff);
			}
		}
		tx.dos.writeInt(0xcd000000 | tbuf);
		tx.dos.writeLong(0x64L); // offset UPL
		tx.dos.writeInt(0);
		tx.dos.write(t.nodename);

		// tx.dos.writeLong(0x1020000000000000L); priority/flags1 [0006 per MDB]
		tx.dos.writeLong(0x1000000000000000L); // priority/flags1 [0006 per MDB]

		tx.dos.writeLong(0x4800000000L); // offset (UID)
		tx.dos.writeLong(0);
		tx.dos.writeLong(0); // end of UB
		tx.dos.writeShort((ebcdic ? 0x0104 : 0x0120) | (bigend ? 0 : 1)); // 1st
		// part
		// of
		// user
		// ID
		//
		tx.dos.writeShort(0); // ditto
		tx.dos.writeInt(0); // ditto
		tx.dos.write(t.nodename); // 2nd part of user ID
		// tx.dos.writeInt(t.node); // 3rd part of user ID
		// tbuf=((p.body[CTX+2]&0xff)<<8)+(p.body[CTX+3]&0xff)+100000;
		// tx.dos.write($Cp037.getBytes("US"+Integer.toString(tbuf)));
		if (bigend) {
			tx.dos.write(p.body, CTX, 4); // store context ID in RDA PID [0007]
			//
		} else {// store Context ID in RDA PID

			tx.dos.write(p.body, CTX + 3, 1); // byte swapped format [0008]
			//
			tx.dos.write(p.body, CTX + 2, 1); // byte swapped format [0008]
			//
			tx.dos.write(p.body, CTX + 1, 1); // byte swapped format [0008]
			//
			tx.dos.write(p.body, CTX, 1); // byte swapped format [0008]
		}
		String s = Integer.toString(t.node);
		// tx.dos.write((s+"        ").substring(0,8).getBytes());
		tx.dos.write($Cp037.getBytes((s + "        ").substring(0, 8)));
		tx.dos.writeInt(0x80); // pal - offset(acb)
		tx.dos.writeInt(0xd0); // pal - offset(fb)
		tbuf = 0xd0; // first buffer
		b = p.body[UBBIN];
		if (bigend) {
			if ((b & 0x10) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x18] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x19] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(rb)
			if ((b & 0x08) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1a] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1b] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(sb)
			if ((b & 0x04) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1c] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1d] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(vb)
			if ((b & 0x02) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1e] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1f] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(ib)
		} else {
			if ((b & 0x10) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x19] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x18] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(rb)
			if ((b & 0x08) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1b] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1a] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(sb)
			if ((b & 0x04) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1d] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1c] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(vb)
			if ((b & 0x02) != 0) {
				tbuf += ((p.body[HDR_LEN + 0x1f] & 0xff) << 8)
						+ (p.body[HDR_LEN + 0x1e] & 0xff);
			}
			tx.dos.writeInt(tbuf); // pal - offset(ib)
		}
		tx.dos.writeInt(0); // pal - return rbl/ibl
		tx.dos.write(p.body, HDR_LEN, p.length - HDR_LEN);// write rest of
		// buffer
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose ("MHDR payload");
	}

	private final Message checkSecurity(final MHDRtoken t, final Message p) {
		Message q;
		if (t.security == null || t.security.get(p.from) != null) {
			p.reset();
			p.getBytes(HDR_LEN); // over type/ctx id
			boolean bigend = true;
			boolean ebcdic = t.isEBCDIC; // take default
			while (p.lengthLeft() > 8) {
				String s = new String(p.getBytes(8)).trim().toLowerCase();
				String v = new String(p.getBytes(p.getByte() - 1)).trim();
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(s + ":" + v);
				if (s.equalsIgnoreCase("endian")) {
					bigend = v.equalsIgnoreCase("big");
				} else if (s.equalsIgnoreCase("charset")) {
					ebcdic = v.equalsIgnoreCase("ebcdic");
				}
			}
			// q=Message.newMessage(HDR_LEN);
			// q.putBytes(ESTABLISH_CONTEXT_REPLY);
			// q.putInt(t.nextid++); // give him a new context
			// q.putBytes(p.body,8,4); // include his verifier
			q = Message.newMessage(256);
			q.putBytes(ESTABLISH_CONTEXT_REPLY);
			q.putInt(t.nextid | (bigend ? 0 : 1) | (ebcdic ? 0 : 2));
			q.putBytes(p.body, CTXVER, 4); // give back his original
			t.nextid += 4;
			q.length = HDR_LEN;

			byte[] x;
			if (ebcdic) {
				x = "CHARSET  EBCDIC".getBytes();
			} else {

				x = "CHARSET  ASCII".getBytes();
			}
			x[8] = (byte) (x.length - 8);
			q.putBytes(x);
			q.length += x.length;

			if (bigend) {
				x = "ENDIAN   BIG".getBytes();
			} else {

				x = "ENDIAN   LITTLE".getBytes();
			}
			x[8] = (byte) (x.length - 8);
			q.putBytes(x);
			q.length += x.length;

			if (ebcdic) {
				x = "FPFORMAT IBM370".getBytes();
			} else {

				x = "FPFORMAT IEEE".getBytes();
			}
			x[8] = (byte) (x.length - 8);
			q.putBytes(x);
			q.length += x.length;
		} else {
			byte[] error = null;
			try {
				error = ("Context for " + p.from + " refused by security").getBytes("8859_1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
			// { byte[]
			// error=("Context for "+p.from+" refused by security").getBytes();
			//
			q = Message.newMessage(HDR_LEN + 4 + error.length);
			q.putBytes(BAD_PASSWORD);
			q.putInt(0); // no context
			q.putBytes(p.body, 8, 4); // include his verifier
			q.putShort(1); // response code
			q.putShort(0); // response subcode
			q.putBytes(error);
		}
		q.target = XTS.ROUTE_RETURN; // via route return
		q.route = p.route;
		q.msgno = p.msgno;
		return q;
	}

	private final void sendhs(final IPtx tx, final int type) throws IOException {
		MHDRtoken t = (MHDRtoken) tx.token;
		t.mhdr[1] = 0x59; // protocol header
		t.mhdr[15] = 0x59; // set length of HS/BC
		tx.dos.write(t.mhdr);
		tx.dos.writeShort(type); // set type
		tx.dos.writeShort(t.node); // our node ID
		tx.dos.writeLong(0x0000005900000000L); // version and offset
		tx.dos.writeLong(0x0000000000000034L);
		tx.dos.write(t.nodename);
		tx.dos.writeLong(0x4040404040404040L); // VM id
		tx.dos.writeLong(0x4040404040400000L); // EBCDIC "Software"
		tx.dos.writeShort(t.node);
		tx.dos.write(t.mhdr, 8, 6); // binary 0 filler
		tx.dos.write(0x68); // cqe flag
		tx.dos.writeShort(t.node);
		tx.dos.writeShort(-1); // terminator
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose ("MHDR handshake");
	}

	protected final Message receive(final IPrx rx) throws IOException {
		byte[] ph = new byte[8];
		int rsp; // rsp 207,209 check
		byte[] ctxSave = new byte[8];
		MHDRtoken t = null;
		for (;;) {
			t = (MHDRtoken) rx.twin.token;
			if (t != null) {
				break;
			}
			try {
				sleep(100);
			} catch (InterruptedException ie) {
			}
		}
		forloop: for (;;) {
			rx.dis.readFully(ph); // get protocol header
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.dump("Received header", "receive", ph, 8, true);
			int len = ((ph[0] & 0xff) << 8) | (ph[1] & 0xff);
			if (len == 0) {
				switch (ph[5]) { // header type

				case 8: // connect reply
				case 9:
					rx.rouse();
					continue forloop; // connect request
				case 10:
					break forloop; // disconnect reply
				case 11:
					((MHDRtoken) rx.twin.token).snddrsp = true; // dis request
					rx.twin.running = false;
					rx.rouse(); // rouse the twin
					break forloop;
				case 14:
					break; // data received
				default:
					continue; // unknown
				}
				return null;
			}

			Message p = Message.newMessage(len);
			rx.dis.readFully(p.body, 0, len); // get whole lot
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.dump("MHDR payload", "receive", p.body, len, true);
			// KT for MDB handle probe control message [0009]
			if ((p.body[9] & 0x40) != 0) {
				rx.rouse();
				p.freeMessage("");
				continue forloop;
			}

			if ((p.body[9] & 0x08) != 0) {
				if (rx.callback != null) {
					mhdr_control(rx, p);
				} else if (XTStrace.isGlobalErrorEnabled()) {
					System.err.println("Control message lost");
				}
				rx.rouse(); // rouse twin
				p.freeMessage("");
				continue forloop;
			}

			p.target = XTS.ROUTE_RETURN;

			boolean bigend = (p.body[0x89] & 0x01) == 0;
			p.route = getint(p.body, 0x24);
			p.ttl = 16; // [0002]

			p.body[0] = EYECATCHER[0];
			p.body[1] = EYECATCHER[1];
			p.body[UBBIN] = p.body[0x42]; // UBBIN
			p.body[UBBOUT] = p.body[0x43]; // UBBOUT

			int i = getint(p.body, 0x30); // offset(msg)
			int j = i + getint(p.body, i + 0x18); // offset(plist)
			int rbl_ibl = getint(p.body, j + 24); // rbl-ibl before destruction
			int rbloff = getint(p.body, j + 8); // offset(RB)
			int ibloff = getint(p.body, j + 20); // offset(IB)

			int k = i + getint(p.body, j); // offset(ACB)

			// if(p.body[k+0x0b]!=0) // ubbout valid? [0010]
			// p.body[UBBOUT]=0; // no

			// Extract rsp code to do adaesi 207,209 check [0005]
			if (bigend) {
				rsp = ((p.body[k + 0x0a] & 0xff) << 8 | (p.body[k + 0x0b] & 0xff));
			} else {
				rsp = ((p.body[k + 0x0b] & 0xff) << 8 | (p.body[k + 0x0a] & 0xff));
			}

			// Don't mug rbl/ibl if adaesi rsp 207,209 [0005]
			if (rsp != 207 & rsp != 209) {
				if (bigend) {
					p.body[k + 0x1a] = p.body[j + 24]; // mug RBL
					p.body[k + 0x1b] = p.body[j + 25]; // mug RBL
					p.body[k + 0x20] = p.body[j + 26]; // mug IBL
					p.body[k + 0x21] = p.body[j + 27]; // mug IBL
				} else {
					p.body[k + 0x1b] = p.body[j + 24]; // mug RBL
					p.body[k + 0x1a] = p.body[j + 25]; // mug RBL
					p.body[k + 0x21] = p.body[j + 26]; // mug IBL
					p.body[k + 0x20] = p.body[j + 27]; // mug IBL
				}
			}

			if (rsp != 0) {
				p.body[UBBOUT] = 0;
			}

			// System.arraycopy(p.body,0x24,p.body,CTX,8);
			ctxSave = (byte[]) t.ctxInfo.remove(Integer.valueOf(p.route));
			if (ctxSave != null) {
				System.arraycopy(ctxSave, 0, p.body, CTX, 8);
			}

			System.arraycopy(p.body, k, p.body, HDR_LEN, 80);
			int l = 80 + HDR_LEN; // offset where to copy
			if ((p.body[UBBOUT] & 0x08) != 0) { // rb back?
				System.arraycopy(p.body, i + rbloff, p.body, l, rbl_ibl >>> 16);
				l += (rbl_ibl >>> 16);
			}

			if ((p.body[UBBOUT] & 0x01) != 0) { // ib back?
				System.arraycopy(p.body, i + ibloff, p.body, l, rbl_ibl & 0xffff);
				l += (rbl_ibl & 0xffff);
			}

			p.length = l;
			Message.dump("MHDR passed on", "receive", p);
			return p;
		}
		return null;
	}

	static class MHDRtoken {
		boolean snddrsp = false; // send disconnect response

		byte[] mhdr = { 0, 0, 0, 0, 1, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xd4,
				(byte) 0xc8, (byte) 0xc4, (byte) 0xd9, 0, 0, 0, 0 };
		int node = 8765; // our node [0011]
		byte[] nodename;
		int pnode; // partner node
		// byte[] pnodename = new byte[8]; // partner node name
		int nextid = 4; // next context number [0006]
		// int nextid=1234; // next context number
		Hashtable<String, String> security; // security context
		// byte preserve[] = new byte[8]; // preserve
		boolean isEBCDIC = false; // is EBCDIC
		Hashtable<Integer, byte[]> ctxInfo = new Hashtable<Integer, byte[]>(); // create
		// Hashtable for context  save
	}

	private final void mhdr_control(final IPrx rx, final Message p)	throws IOException {
		if (p.body[0] != 0) {
			return; // MHDR message
		}
		MHDRtoken t = (MHDRtoken) rx.twin.token;
		if (p.body[0x20] == (byte) 0xc8) { // H0, H1 or H2 message

			if (p.body[0x21] == (byte) 0xf0) {
				throw new RuntimeException("MHDR mhdr_control: Handshake error " + $Cp037.getString(p.body, 0x34, 8));
			}
			// throw new
			// RuntimeException("Handshake error "+$Cp037.getString(p.body,0x34,8));
			// 
			t.pnode = ((p.body[0x22] & 0xff) << 8) + (p.body[0x23] & 0xff);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Handshake - register proxy " + t.pnode);
			Message q = Message.newMessage(2);
			q.target = XTS.REGISTER_PROXY; // register proxy
			q.body[0] = p.body[0x22];
			q.body[1] = p.body[0x23];
			rx.callback.received(rx.twin, q, rx.userval); // up to client to
			// free msg
		}

		if (p.body[0x20] == (byte) 0xc2 && p.body[0x21] == (byte) 0xf1) {
			if ((p.body[0x40] & 0x10) != 0) {
				do_chirp(rx, p.body[0x3e], p.body[0x3f], p.body[0x41] == 1 ? CHIRP : NOCHIRP);
			}
		} else {
			int i = 0x34; // start of 1st ID
			while (i < p.length && p.body[i] != -1) { // not end of list
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Node: " + $Cp037.getString(p.body, i, 8));
				i += 32; // over name/weight
				while (i < p.length && p.body[i] != -1) {
					if (XTStrace.bGlobalVerboseEnabled) {
						int targ = ((p.body[i + 1] & 0xff) << 8) + (p.body[i + 2] & 0xff);
						XTStrace.verbose("Target ID: " + targ + " flags:" + p.body[i]);
					}
					if ((p.body[i] & 0x10) != 0) {
						do_chirp(rx, p.body[i + 1], p.body[i + 2], CHIRP);
					}
					i += 3;
				}
				i++;
			}
		}
	}

	private static final void do_chirp(final IPrx rx, final byte b1, final byte b2, final int chirp) {
		rx.do_chirp(((b1 & 0xff) << 8) | (b2 & 0xff), chirp, chirpBody);
	}

	protected final void shut(final IPtx tx) throws IOException {
		MHDRtoken t = (MHDRtoken) tx.token;
		tx.dos.writeLong(t.snddrsp ? 0x10a0000L : 0x10b0000L);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("disconnect");
	}

	public final void setNodeName(final String nodeName) {
		if (nodeName != null) {
			if (nodeName.length() > 8) {
				MHDR.nodeName = nodeName.substring(0, 8).toUpperCase();

			} else {
				if (nodeName.length() == 8) {
					MHDR.nodeName = nodeName.toUpperCase();
				} else {
					int i = 8 - nodeName.length();
					MHDR.nodeName = nodeName.toUpperCase()	+ "       ".substring(0, i);
				}
			}
		}
	}
}
