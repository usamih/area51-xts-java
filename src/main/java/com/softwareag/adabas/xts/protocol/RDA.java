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
 * This class implements a Network RDA protocol driver for XTS.
 ** 
 **/
// -----------------------------------------------------------------------
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022
// -----------------------------------------------------------------------
class RDA extends IPtransport {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	private static final Vector<Thread> tvec = new Vector<Thread>();
	static final int CHIRP = 0x7fffffff; // chirp time
	static final int NOCHIRP = 0x1; // bring it down
	static final byte[] chirpBody = new byte[16];
	// static final byte[] EYECATCHER = {(byte)'A',(byte)'1'};
	// static final byte[] ESTABLISH_CONTEXT_REPLY = {(byte)'A',
	// (byte)'1',-1,(byte)0x81};
	// static final byte[] BAD_PASSWORD = {(byte)'A',(byte)'1',-1,
	// (byte)0xc1};
	static final byte[] EYECATCHER = { (byte) 0x41, (byte) 0x31 };
	static final byte[] ESTABLISH_CONTEXT_REPLY = { (byte) 0x41, (byte) 0x31, (byte) 0xff, (byte) 0x81 };
	static final byte[] BAD_PASSWORD = { (byte) 0x41, (byte) 0x31, (byte) 0xff, (byte) 0xc1 };
	static final int HDR_LEN = 12;
	static final int UBBIN = 2; // offset of ubbin
	static final int UBBOUT = 3; // offset of ubbout
	// make CTX and CTXVER public, thus allowing use by others [0006]
	public static final int CTX = 4; // offset of context [0006]
	public static final int CTXVER = 8; // offset of context verifier[0006]
	static String nodeName = null; // Node Name set by caller [0003]
	static {
		long x = System.currentTimeMillis();
		for (int i = 0; i < 8; i++) {
			chirpBody[i] = (byte) (x >> (56 - (i * 8)));
		}
	}

	private boolean isEBCDIC = false; // from URL

	/** Default constructor. **/
	public RDA() {
		super(tvec);
	}

	private RDA(final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect);
		protocol = "rda";
		String s = url.getValue("charset"); 
		if (s != null) {
			isEBCDIC = s.equalsIgnoreCase("ebcdic"); 
		}
	}

	private RDA(final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect, final int connTo) {
		super(tvec, url, listen, callback, userval, retry_interval, retry_count, reconnect, connTo);
		protocol = "rda";
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
	 ** @return an instance of the protocol driver.
	 **/
	public final IDriver listen(final XTSurl url, final IConnectCallback callback, final Object userval) {
		RDA rda = new RDA(url, true, callback, userval, 0, 0, false);
		rda.start();
		return rda;
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
	 ** @return an instance of the protocol driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval) {
		RDA rda = new RDA(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false);
		rda.start();
		return rda;
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
	 ** @return an instance of the protocol driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final int connTo) {
		RDA rda = new RDA(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false, connTo);
		rda.start();
		return rda;
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
	 ** @return an instance of the protocol driver.
	 **/
	public final IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final long retry_interval, final int retry_count, final boolean reconnect) {
		RDA rda = new RDA(url, false, callback, userval, retry_interval, retry_count, reconnect);
		rda.start();
		return rda;
	}
	
	private static final int binbout(final byte b) {
		return ((b & 0x10) >> 4) | ((b & 0x08) >> 2) | (b & 0x04) | ((b & 0x02) << 2) | ((b & 0x01) << 4);
	}

	protected final Object getToken(final IPtx tx) throws IOException {
		RDAtoken t = new RDAtoken();
		t.isEBCDIC = isEBCDIC;
		String st = url.getValue("node");
		if (st != null) {
			try {
				// t.node=Short.parseShort(st);
				t.node = Integer.parseInt(st);
				if (t.node > 65535)
					t.node = 65535;
			} catch (Exception e) {
			}
		}
		st = url.getValue("nodename");
		if (st != null) {
			t.nodename = ((st + "        ").toUpperCase().substring(0, 8)).getBytes("8859_1");
		} else {
			try {
				if (nodeName == null) {
					t.nodename = "XTSNODE ".getBytes("8859_1");
				} else {

					t.nodename = nodeName.getBytes("8859_1");
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			XTStrace.verbose("RDA Converted nodename using encode 8859_1"); // rxc
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
				tx.dos.writeLong(0x101080000L); // connect response
				XTStrace.verbose ("conrsp");
				tx.doze("wait RDA req");
				tx.dos.writeLong(0x00800000010e0000L); // 8-byte header
				// [0017] tx.dos.writeLong(0x4501000100000028L | (t.node<<16));
				// // 450102 to 450100
				tx.dos.writeInt(0x45010001);
				tx.dos.writeInt(0x00000028 | (t.node << 16));
				tx.dos.write(t.preserve);
				tx.dos.write(t.nodename);
				tx.dos.writeLong(0x2020202020202020L);
				// tx.dos.writeLong(0x0000000141495820L); // AIX
				tx.dos.writeLong(0x0000000158545320L); // XTS
				tx.dos.writeLong(0x2020202000000000L);
				tx.dos.writeLong(0x0000ffff00000080L);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				XTStrace.verbose ("RDA conrsp");
			} else {
				tx.dos.writeLong(0x01090000L); // connect request
				XTStrace.verbose ("conreq");
				tx.doze("wait conresp"); // wait for connect response
				tx.dos.writeLong(0x00800000010e0000L);
				// [0017] tx.dos.writeLong(0x4301000100000028L | (t.node<<16));
				// // rxc 430102 to 430100 // RDA neg
				tx.dos.writeInt(0x43010001); // [0017]
				tx.dos.writeInt(0x00000028 | (t.node << 16)); // [0017]
				tx.dos.writeLong(0);
				tx.dos.write(t.nodename);
				tx.dos.writeLong(0x2020202020202020L);
				// tx.dos.writeLong(0x0000000141495820L); // AIX
				tx.dos.writeLong(0x0000000158545320L); // XTS
				tx.dos.writeLong(0x2020202000000000L);
				tx.dos.writeLong(0x0000ffff00000080L);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(0);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				tx.dos.writeLong(-1);
				XTStrace.verbose ("RDA conreq");
				tx.doze("wait RDA rsp");
			}
		}
		tx.status.setStatus("rcvwait");
		return t;
	}

	protected final void transmit(final Message p, final IPtx tx) throws IOException {
		if (p.userval == status) {
			tx.dos.writeShort(p.length);
			tx.dos.writeInt(0x0000010e);
			tx.dos.writeShort(0);
			tx.dos.write(p.body, 0, p.length); // protocol message
			XTStrace.verbose ("protmsg");
			return;
		}

		if (p.target < 0) {
			XTStrace.verbose("RDA return: do not transmit XTS ADMIN message");// rxc
			return;
		} // rxc

		if (p.length == 0) { // admin message
			tx.twin.do_chirp(p.target, CHIRP, chirpBody);
			return;
		}

		RDAtoken t = (RDAtoken) tx.token;

		if (p.body[UBBIN] == -1) { // establish context

			if (tx.twin.callback != null) {
				switch (p.body[UBBOUT]) {
				case 1: // establish context
					Message q = checkSecurity(t, p);
					tx.twin.callback.received(tx, q, tx.twin.userval);
					break;

				case 2: // destroy context
					// **********q=Message.newMessage(0);*****************************
					// [0015]
					q = p.cloneMessage(); // Create a copy of message
					q.reset();
					q.skip(3); // point to flags byte
					q.put((byte) 0x82); // Set Reply destroy context
					q.target = XTS.ROUTE_RETURN; // via route return
					q.route = p.route; // Send message back.
					q.ttl = 16;
					tx.twin.callback.received(tx, q, tx.twin.userval);
					break;

				default:
					throw new RuntimeException("RDA transmit: Huh?? strange Adabas msg " + p.body[UBBOUT]);
				}
			}
			return; // establish context
		}

		boolean bigend = (p.body[CTX + 3] & 0x01) == 0; // low-order bit in
		// context
		boolean ebcdic = (p.body[CTX + 3] & 0x02) == 0; // EBCDIC
		// byte[] ctxSave=new byte[8];
		// System.arraycopy(p.body,CTX,ctxSave,0,8);
		// t.ctxInfo.put(new Integer(p.route), ctxSave);
		byte[] SaveCtxUBBinBout = new byte[12];
		System.arraycopy(p.body, CTX, SaveCtxUBBinBout, 0, 8);
		SaveCtxUBBinBout[8] = p.body[UBBIN];
		SaveCtxUBBinBout[9] = p.body[UBBOUT];
		System.arraycopy(p.body, 20, SaveCtxUBBinBout, 10, 2); // SV FNR
		t.ctxInfo.put(Integer.valueOf(p.route), SaveCtxUBBinBout);

		tx.dos.writeShort(p.length - HDR_LEN + 0x40);
		tx.dos.writeLong(0x0000010e00004200L);
		tx.dos.writeShort(0x0041);
		tx.dos.writeShort(p.target);
		tx.dos.writeShort((ebcdic ? 0x0004 : 0x0028) | (bigend ? 0 : 1));
		tx.dos.write(p.body, UBBIN, 4); // CQE
		tx.dos.writeInt(p.route); // SEQNR
		tx.dos.write(t.nodename);
		tx.dos.write(t.pnodename);
		// Make rdas1_prio2 x01 vs x40 to eliminate rsp 146 for adaesi [0019]
		// tx.dos.writeInt(0x10400000 | (binbout(p.body[UBBIN])<<8) |
		// binbout(p.body[UBBOUT]));
		tx.dos.writeInt(0x10010000 | (binbout(p.body[UBBIN]) << 8)
				| binbout(p.body[UBBOUT]));

		// Convert nodeid to string and place it in the rdaTID field [0006]
		String s = Integer.toString(t.node); // [0006]
		// String s=Integer.toString((t.node<<16) [0006]
		// | ((p.body[CTX+2]&0xff)<<8) | (p.body[CTX+3]&0xff)); [0006]
		tx.dos.write((s + "        ").substring(0, 8).getBytes());
		// tx.dos.write(p.body,CTX,8);

		int fbl;
		int rbl;
		int sbl;
		int vbl;
		int ibl;

		int b = p.body[UBBIN];
		if (bigend) {
			fbl = ((b & 0x10) == 0) ? 0 : ((p.body[HDR_LEN + 24] & 0xff) << 8)
					| (p.body[HDR_LEN + 25] & 0xff);
			rbl = ((b & 0x08) == 0) ? 0 : ((p.body[HDR_LEN + 26] & 0xff) << 8)
					| (p.body[HDR_LEN + 27] & 0xff);
			sbl = ((b & 0x04) == 0) ? 0 : ((p.body[HDR_LEN + 28] & 0xff) << 8)
					| (p.body[HDR_LEN + 29] & 0xff);
			vbl = ((b & 0x02) == 0) ? 0 : ((p.body[HDR_LEN + 30] & 0xff) << 8)
					| (p.body[HDR_LEN + 31] & 0xff);
			ibl = ((b & 0x01) == 0) ? 0 : ((p.body[HDR_LEN + 32] & 0xff) << 8)
					| (p.body[HDR_LEN + 33] & 0xff);
		} else {
			fbl = ((b & 0x10) == 0) ? 0 : ((p.body[HDR_LEN + 25] & 0xff) << 8)
					| (p.body[HDR_LEN + 24] & 0xff);
			rbl = ((b & 0x08) == 0) ? 0 : ((p.body[HDR_LEN + 27] & 0xff) << 8)
					| (p.body[HDR_LEN + 26] & 0xff);
			sbl = ((b & 0x04) == 0) ? 0 : ((p.body[HDR_LEN + 29] & 0xff) << 8)
					| (p.body[HDR_LEN + 28] & 0xff);
			vbl = ((b & 0x02) == 0) ? 0 : ((p.body[HDR_LEN + 31] & 0xff) << 8)
					| (p.body[HDR_LEN + 30] & 0xff);
			ibl = ((b & 0x01) == 0) ? 0 : ((p.body[HDR_LEN + 33] & 0xff) << 8)
					| (p.body[HDR_LEN + 32] & 0xff);
		}

		tx.dos.writeInt(fbl + sbl + vbl + 0x40); // offset of acb
		// tx.dos.writeInt(p.body[CTX]); // store context ID in RDA PID [0006]
		if (bigend) {
			tx.dos.write(p.body, CTX, 4); // store context ID in RDA PID [0007]
		} else {// store Context ID in RDA PID [0008]

			tx.dos.write(p.body, CTX + 3, 1); // byte swapped format [0008]
			tx.dos.write(p.body, CTX + 2, 1); // byte swapped format [0008]
			tx.dos.write(p.body, CTX + 1, 1); // byte swapped format [0008]
			tx.dos.write(p.body, CTX, 1); // byte swapped format [0008]
		}
		// tx.dos.writeLong(p.length+0x40-HDR_LEN);// length of message
		// [0006]
		tx.dos.writeInt(p.length + 0x40 - HDR_LEN);// length of message 4 bytes
		// [0006]
		tx.dos.writeLong(0); // spare
		if (fbl > 0) {
			tx.dos.write(p.body, 80 + HDR_LEN, fbl);
		}
		if (sbl > 0) {
			tx.dos.write(p.body, 80 + HDR_LEN + fbl + rbl, sbl);
		}
		if (vbl > 0) {
			tx.dos.write(p.body, 80 + HDR_LEN + fbl + rbl + sbl, vbl);
			// if(p.body[HDR_LEN+(bigend?8:9)]==0) [0006]
			// Insert target/DBID into adabas control block if ascii. [0009]
		}

		// Store the FNR and DBID bytes of the ACB into an integer field [0010]
		int fnr_DBID = 0;
		if (bigend) {
			fnr_DBID = ((p.body[HDR_LEN + 8] & 0xff) << 8)
					| (p.body[HDR_LEN + 9] & 0xff);
		} else {

			fnr_DBID = ((p.body[HDR_LEN + 9] & 0xff) << 8)
					| (p.body[HDR_LEN + 8] & 0xff);
			// If the fnr_DBID field is < 256(no DBID or 2 byte file #)
			// specifed[0010]
			// and the connection to WCP is NOT ebcdic(i.e.it's ascii) [0010]
			// if(fnr_DBID<256 && ((p.body[HDR_LEN] & 0x30) != 0x30) //
			// && !t.isEBCDIC)
		}

		// If going to ascii platform DBID and FNR must be in acb [0021]
		// If going to ebcdic platform && ascii client && large FNR then [0021]
		// x'30' must be set [0021]
		// If going to an ASCII platform [0011]
		// If the target or fnr_DBID is > 255 [0011]
		// set the acb type field to 30(may be set, but make sure) [0011]
		// if sending to a big endian platform [0011]
		// set acb rsp code with last 2 bytes of targetid [0011]
		// else [0011]
		// set acb rsp code with last 2 bytes of targetid swapped [0011]
		// else (targetid and fnr must be less than 256) [0011]
		// if acb type field != X'30' and [0011]
		// set the target id in the acb based on endian format [0011]
		if (!t.isEBCDIC) {
			if (p.target > 255 || fnr_DBID > 255
					|| (p.body[HDR_LEN] & 0x30) == 0x30) {
				p.body[HDR_LEN] |= 0x30;
				if (bigend) {
					p.body[HDR_LEN + 10] = (byte) (p.target >> 8);
					p.body[HDR_LEN + 11] = (byte) p.target;
				} else {
					p.body[HDR_LEN + 10] = (byte) p.target;
					p.body[HDR_LEN + 11] = (byte) (p.target >> 8);
				}
			} else {

				p.body[HDR_LEN + (bigend ? 8 : 9)] = (byte) p.target;
			}
		} else {
			// if ascii client && large fnr to ebcdic platform force x'30' call

			if (!ebcdic && fnr_DBID > 255) {
				p.body[HDR_LEN] |= 0x30;
				if (bigend) {
					p.body[HDR_LEN + 10] = (byte) (p.target >> 8);
					p.body[HDR_LEN + 11] = (byte) p.target;
				} else {
					p.body[HDR_LEN + 10] = (byte) p.target;
					p.body[HDR_LEN + 11] = (byte) (p.target >> 8);
				}
			}
		}

		tx.dos.write(p.body, HDR_LEN, 80); // control block
		if (rbl > 0) {
			tx.dos.write(p.body, 80 + HDR_LEN + fbl, rbl);
		}
		if (ibl > 0) {
			tx.dos.write(p.body, 80 + HDR_LEN + fbl + rbl + sbl + vbl, ibl);
		}
		XTStrace.verbose ("RDA Payload");
	}

	private final Message checkSecurity(final RDAtoken t, final Message p) {
		Message q;
		if (t.security == null || t.security.get(p.from) != null) {
			p.reset();
			p.getBytes(HDR_LEN); // over type/ctx id
			boolean bigend = true;
			boolean ebcdic = t.isEBCDIC; // take default
			while (p.lengthLeft() > 8) {
				String s = new String(p.getBytes(8)).trim().toLowerCase();
				String v = new String(p.getBytes(p.getByte() - 1)).trim();
				XTStrace.verbose(s + ":" + v);
				if (s.equalsIgnoreCase("endian")) {
					bigend = v.equalsIgnoreCase("big");
				} else if (s.equalsIgnoreCase("charset")) {
					ebcdic = v.equalsIgnoreCase("ebcdic");
				}
			}
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
				error = ("Context for " + p.from + " refused by security")
						.getBytes("8859_1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
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

	static final int Rdarnorm = 0x0001; // Normal return
	static final int Rdaract = 0xD803; // Activation successful
	static final int Rdaralr = 0x80DA; // Database already active
	static final int Rdaratt = 0xD813; // Attach successful
	static final int Rdardea = 0xD80B; // Deactivate successful
	static final int Rdardet = 0xD81B; // Detach successful
	static final int Rdardtf = 0xD23A; // Detach/attach/trm failed
	static final int Rdarerr = 0x0002; // Some error
	static final int Rdarnak = 0xDB4A; // Negative acknowledge

	protected final Message receive(final IPrx rx) throws IOException {
		byte[] ph = new byte[8];
		int rsp; // rsp 207,209 check
		// byte[] ctxSave=new byte[8];
		byte[] SaveCtxBinBout = new byte[12];
		RDAtoken t = null;
		for (;;) {
			t = (RDAtoken) rx.twin.token;
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
			XTStrace.dump("Received header", "receive", ph, 8, true);
			int len = ((ph[0] & 0xff) << 8) | (ph[1] & 0xff);
			if (len == 0) {
				switch (ph[5]) {
				case 8: // connect reply
				case 9:
					rx.rouse();
					continue forloop; // connect request
				case 10:
					break forloop; // disconnect reply
				case 11:
					t.snddrsp = true; // dis request
					rx.twin.running = false;
					rx.rouse(); // rouse the twin
					throw new IOException("Disconnected");
				case 14:
					break; // data received
				default:
					continue; // unknown
				}
			}

			Message p = Message.newMessage(len);
			rx.dis.readFully(p.body, 0, len); // get whole lot
			XTStrace.dump("RDA Payload", "receive", p.body, len, true);

			if ((p.body[0] & 0x01) != 0) {
				if (rda_control(p, rx)) {
					rx.rouse();
				}
				p.freeMessage("");
				continue forloop;
			}

			p.target = XTS.ROUTE_RETURN;

			boolean bigend = (p.body[0x07] & 0x01) == 0;
			p.route = getint(p.body, 0x0c);
			p.ttl = 16;

			p.body[0] = EYECATCHER[0];
			p.body[1] = EYECATCHER[1];
			p.body[UBBIN] = (byte) binbout(p.body[0x22]); // UBBIN
			p.body[UBBOUT] = (byte) binbout(p.body[0x23]); // UBBOUT

			if (p.body[0x4a] != 0 || p.body[0x4b] != 0) {
				p.body[UBBOUT] = 0; // yes = no returned buffers
			}

			int rbl = ((p.body[0x30] & 0xff) << 8) | (p.body[0x31] & 0xff);
			int ibl = ((p.body[0x32] & 0xff) << 8) | (p.body[0x33] & 0xff);

			// Extract rsp code to do adaesi 207,209 check [0019]
			if (bigend) {
				rsp = ((p.body[0x4a] & 0xff) << 8 | (p.body[0x4b] & 0xff));
			} else {
				rsp = ((p.body[0x4b] & 0xff) << 8 | (p.body[0x4a] & 0xff));
			}

			// Don't mug rbl/ibl if adaesi rsp 207,209 [0019]
			if (rsp != 207 & rsp != 209) {
				if (bigend) {
					p.body[0x5a] = p.body[0x30]; // mug RBL
					p.body[0x5b] = p.body[0x31]; // mug RBL
					p.body[0x60] = p.body[0x32]; // mug IBL
					p.body[0x61] = p.body[0x33]; // mug IBL
				} else {
					p.body[0x5b] = p.body[0x30]; // mug RBL
					p.body[0x5a] = p.body[0x31]; // mug RBL
					p.body[0x61] = p.body[0x32]; // mug IBL
					p.body[0x60] = p.body[0x33]; // mug IBL
				}
			}

			SaveCtxBinBout = (byte[]) t.ctxInfo
					.remove(Integer.valueOf(p.route));
			if (SaveCtxBinBout != null) {
				System.arraycopy(SaveCtxBinBout, 0, p.body, CTX, 8);
				p.body[UBBIN] = SaveCtxBinBout[8];
				p.body[UBBOUT] = SaveCtxBinBout[9];
				System.arraycopy(SaveCtxBinBout, 10, p.body, 0x48, 2);
				// restore
				// FNR
			}

			// Large FNR rsp 17 fix [0021]
			// Don't bash FNR if x'30' call.
			// if((p.body[0x40] & 0x30)!= 0x30)
			// {
			// p.body[bigend?0x48:0x49]=0;
			// }

			System.arraycopy(p.body, 0x40, p.body, HDR_LEN, 80);
			int l = 80 + HDR_LEN; // offset where to copy

			if ((p.body[UBBOUT] & 0x08) != 0) { // rb back?

				System.arraycopy(p.body, 0x90, p.body, l, rbl);
				l += rbl;
			} else {
				rbl = 0;
			}

			if ((p.body[UBBOUT] & 0x01) != 0) { // ib back?
				System.arraycopy(p.body, 0x90 + rbl, p.body, l, ibl);
				l += ibl;
			}

			p.length = l;

			Message.dump("RDA passed on", "receive", p);
			return p;
		}
		return null;
	}

	static class RDAtoken {
		boolean snddrsp = false; // send disconnect response
		int node = 8765; // our node
		byte[] nodename; // our node name
		int pnode; // partner node
		byte[] pnodename = new byte[8]; // partner node name
		// int nextid=0; // next context number [0006]
		int nextid = 4; // next context number [0006]
		Hashtable<String, String> security; // security context
		byte[] preserve = new byte[8]; // preserve
		boolean isEBCDIC = false; // is EBCDIC
		Hashtable<Integer, byte[]> ctxInfo = new Hashtable<Integer, byte[]>(); // create Hashtable for context save
	}

	private final boolean rda_control(final Message p, final IPrx rx) {
		boolean request = (p.body[0] & 0x02) != 0;
		if (!request) {
			if (p.body[1] != 1 && p.body[1] != 9) {
				throw new RuntimeException("RDA rda_control: Response? Huh?");
			}
		}

		int rsp = ((p.body[32] & 0xff) << 24) | ((p.body[33] & 0xff) << 16)
				| ((p.body[34] & 0xff) << 8) | (p.body[35] & 0xff);

		RDAtoken t = (RDAtoken) rx.twin.token;

		System.arraycopy(p.body, 8, t.preserve, 0, 8);

		int dbid = ((p.body[4] & 0xff) << 8) | (p.body[5] & 0xff);

		switch (p.body[1]) {
		case 0:
			XTStrace.verbose("RDA broadcast");
			respond(p, Rdarnorm, rx); // broadcast, ignore
			return false;

		case 9:
			XTStrace.verbose("RDA Exchange");
		case 1:
			if (rsp == Rdarnak) {
				throw new RuntimeException("RDA rda_control: Connect reject");
			}
			if (p.body[1] == 1) {
				XTStrace.verbose("RDA Connect");
			}
			t.pnode = dbid;
			if (request && t.isEBCDIC) {
				t.isEBCDIC = (p.body[7] & 0x04) != 0;
			}
			System.arraycopy(p.body, 16, t.pnodename, 0, 8);
			dbid = 0;
			for (int i = 64; i < 96; i++) {
				if ((p.body[i] & 0x01) != 0) {
					rx.do_chirp(dbid, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x02) != 0) {
					rx.do_chirp(dbid + 1, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x04) != 0) {
					rx.do_chirp(dbid + 2, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x08) != 0) {
					rx.do_chirp(dbid + 3, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x10) != 0) {
					rx.do_chirp(dbid + 4, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x20) != 0) {
					rx.do_chirp(dbid + 5, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x40) != 0) {
					rx.do_chirp(dbid + 6, CHIRP, chirpBody);
				}
				if ((p.body[i] & 0x80) != 0) {
					rx.do_chirp(dbid + 7, CHIRP, chirpBody);
				}
				p.body[i] = 0;
				dbid += 8;
			}
			if (p.body[1] == 1) {
				return true;
			}
			for (int i = 96; i < 128; i++) {
				p.body[i] = -1;
			}
			respond(p, Rdarnorm, rx);
			return false;

		case 2: // activate db
		case 3:
			XTStrace.verbose(p.body[1] == 2 ? "RDA Activate DB": "RDA Deactivate DB");
			respond(p, Rdardtf, rx); // deactivat db
			throw new RuntimeException("RDA rda_control: activate/inactivate");

		case 4:
			XTStrace.verbose("RDA attach DB");
			rx.do_chirp(dbid, CHIRP, chirpBody); // attach db
			respond(p, Rdaratt, rx); // attach successful
			return false;

		case 5:
			XTStrace.verbose("RDA detach DB");
			rx.do_chirp(dbid, NOCHIRP, chirpBody); // detach db
			respond(p, Rdardet, rx); // detach db
			return false;

		case 10: // prepare to commit
		case 12: // rollback
		case 6:
			XTStrace.verbose(p.body[1] == 6 ? "RDA disconnect" : "RDA prepare commit/rollback");
			respond(p, Rdarnorm, rx); // disconnect
			return false;

		case 7: // timeout
		case 8:
			XTStrace.verbose("RDA timeout/cancel");
			respond(p, -1, rx); // cancel
			return false;

		case 11:
			XTStrace.verbose("RDA commit");
			rx.do_chirp(dbid, CHIRP, chirpBody); // commit
			respond(p, Rdarnorm, rx);
			return false;

		case 13:
			XTStrace.verbose("RDA terminate DB");
			rx.do_chirp(dbid, NOCHIRP, chirpBody); // end it
			respond(p, Rdardea, rx); // terminate db
			return false;

		case 14: // activate CSCI server
		case 15: // deactivate CSCI server
		case 16: // probe
		case 17:
			XTStrace.verbose(p.body[1] == 16 ? "RDA probe" : "RDA actCSCI/deactCSCI/negComp");
			respond(p, Rdarnorm, rx); // negotiate compression
			return false;
		default:
			throw new RuntimeException("RDA rda_control: Unknown Admin msg " + p.body[1]);
		}
	}

	private final void respond(final Message p, final int rsp, final IPrx rx) {
		Message q = p.cloneMessage();
		System.arraycopy(p.body, 0, q.body, 0, p.length);
		System.arraycopy(p.body, 24, q.body, 16, 8); // swap from-to
		System.arraycopy(p.body, 16, q.body, 24, 8);
		q.body[0] = (byte) ((p.body[0] & 0xfd) | 0x04); // make response
		q.body[32] = (byte) (rsp >>> 24);
		q.body[33] = (byte) (rsp >>> 16);
		q.body[34] = (byte) (rsp >>> 8);
		q.body[35] = (byte) rsp;
		rx.twin.send(q, null, status); // send it
	}

	protected final void shut(final IPtx tx) throws IOException {
		RDAtoken t = (RDAtoken) tx.token;
		tx.dos.writeLong(t.snddrsp ? 0x10a0000L : 0x10b0000L);
		XTStrace.verbose ("disconnect");
	}

	public final void setNodeName(final String nodeName) {
		if (nodeName != null) {
			if (nodeName.length() > 8) {
				RDA.nodeName = nodeName.substring(0, 8).toUpperCase();
			} else {
				if (nodeName.length() == 8) {
					RDA.nodeName = nodeName.toUpperCase();
				} else {
					int i = 8 - nodeName.length();
					RDA.nodeName = nodeName.toUpperCase() + "       ".substring(0, i);
				}
			}
		}
	}
}
