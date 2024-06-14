/* 
 * Copyright (c) 1998-2017 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

//-----------------------------------------------------------------------

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.directory.Directory;
import com.softwareag.adabas.xts.directory.INIdir;
import com.softwareag.adabas.xts.helpers.Status;
import com.softwareag.adabas.xts.helpers.Target;
import com.softwareag.adabas.xts.helpers.Token;
import com.softwareag.adabas.xts.interfaces.IConnectCallback;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.interfaces.IReceiveCallback;
import com.softwareag.adabas.xts.interfaces.ITransmitCallback;
import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.common.CommonTrace;


/**
 * The XTS class implements the XTS API. It enables a client or server to
 * communicate with a partner, possibly through multiple proxies.
 ** 
 * @version 1.4.1.0
 */

public final class XTS implements IConnectCallback, ITransmitCallback, IReceiveCallback, IXTSreceiver, IXTStransmitter {
	public static final int RESOLVE_TARGET_NAME = 0xffff0000;
	public static final int RESOLVE_TARGET_NAME_REPLY = 0xffff0001;
	public static final int CHIRP = 0xffff0002;
	public static final int REGISTER_PROXY = 0xffff0005;
	public static final int CONTROL_MSG = 0xffffff00;
	
    /**
     * RGHADA-3040 
     * Created new target value to indicate the target was unresolved. 
     */
	public static final int UNRESOLVED_TARGET = 0xffffffb9;
	
	public static final int BIND_CLIENT = 0xfffffffa;
	public static final int ROUTE_TIMEOUT = 0xfffffffb;
	public static final int ROUTE_TABLE_OVERFLOW = 0xfffffffc;
	public static final int TTL_EXPIRED = 0xfffffffd;
	public static final int ROUTE_FAILED = 0xfffffffe;
	public static final int ROUTE_RETURN = 0xffffffff;
	public static final int RAW_MESSAGE = 0x7fffffff;
	public static final int ADI_MESSAGE = 0x6fffffff;

	public static final int SERVER_ID_BASE = 0x7E000000;

	// State values
	private static final String SEND = "Send";
	private static final String CONNECT = "Connect";
	private static final String LISTEN = "Listen";
	private static final String CLIENT = "Client Connect";
	private static final String SNDWAIT = "Send and Wait";

	private static final String RXTIMEOUT = "rxtimeout";
	private static AtomicInteger currentSendMessageNo = new AtomicInteger(0);

	/**
	 ** Access qualifier constant Used to qualify the type of URL being added to
	 * directory services. Access URL's specify protocol, host, port required to
	 * reach a target
	 */
	public static final String XTSACCESS = "XTSaccess";
	/**
	 ** Listen qualifier constant Used to qualify the type of URL being added to
	 * directory services. Listen URL's specify protocol, inconsequential host,
	 * port for a target / proxy to listen on
	 */
	public static final String XTSLISTEN = "XTSlisten";
	/**
	 ** Connect qualifier constant Used to qualify the type of URL being added to
	 * directory services. Connect URL's specify protocol, host port for a
	 * target / proxy to connect to.
	 **/
	public static final String XTSCONNECT = "XTSconnect";
		
	public static final XTSurl[] ZERO_LEN_XTSURL_ARRAY = new XTSurl[0];

	// private static final ConcurrentHashMap<String, IConnection> connections =
	// new ConcurrentHashMap<String, IConnection>();
	private static Hashtable<String, ArrayList<IConnection>> urls = new Hashtable<String, ArrayList<IConnection>>();

	private static HashMap<Integer, XTS> replies = new HashMap<Integer, XTS>();
	private static ReentrantLock replyLock = new ReentrantLock();


	/** 
	 * ResolveTargetObj used for Resolve Target Reply synchronization
	 */
	static class ResolveTargetObj {    
		int    Error;    
		Object Obj;
		public ResolveTargetObj (int Error, Object Obj) {    
			this.Error = Error;    
			this.Obj = Obj;    
		}    
	}    

	private static Hashtable<Object, Object> proxies = new Hashtable<Object, Object>();
	private static Hashtable<String, ResolveTargetObj> resname = new Hashtable<String, ResolveTargetObj>();
	private static Vector<IDriver> dconn = new Vector<IDriver>();

	// -----------------------------------------------------------------------
	// INI directory file name for use by development
	// -----------------------------------------------------------------------
	private static final String XTSINI = "sandbox.cfg";
	// -----------------------------------------------------------------------
	// user Defined System Properties.
	// -----------------------------------------------------------------------
	private static final String XTSDIR = "XTSDIR"; // Trace Directory
	// System Property
	private static final String XTSTRACE = "XTSTRACE"; // Trace System
	// Property
	private static String xtsdir = null; // Trace setting

	// services
	static boolean closeTraceFile = true; // Close trace filel indicator
	static boolean traceFileClosed = false; // Indicates the trace file is Closed
	static boolean versionPrinted = false; // Indicates version info was printed
	static boolean firstTimeTrace = true; // Indicates First Line of trace file.

	public static boolean useOnlyOneConnection = false;
	public static boolean closeConnections = false;
	public static boolean connectEachConnection = false;
	public static int forceOpenConnections = 1; // minimal open connections

	private static int nextNumber = 0; // next state number
	/**
	 * The default TIME-TO-LIVE value for each outgoing message. Defaults to 16.
	 **/
	public static byte ttl = 16; // time to live
	/**
	 * The default PRIORITY value for each outgoing message. Defaults to 8.
	 **/
	public static byte priority = 8; // priority
	/**
	 * The period to wait before timing out on a new connection. Defaults to
	 * 120000
	 **/
	public static long sleepTimeout = 120000;      // sleep timeout
	public static long maxmsglength = 1024*1024;   // max received message length

	private boolean freeState = false;
	private static XTS freeXTS = null; // chain of free XTS
	private static boolean isWaiters = false; // are waiters
	private static int waiters = 0; // number of actual waiting th
	private static boolean running = true; // =false during shutdown
	private static Token[] msgQ = new Token[2];

	// private static Directory directory=new DefaultDirectory();
	private static Directory defaultDirectory = new DefaultDirectory();
	private static Vector<IXtsDirectory> directoryVector = new Vector<IXtsDirectory>();
    
	// -----------------------------------------------------------------------
	// Locks
	// -----------------------------------------------------------------------
	private static ReentrantLock allocLock = new ReentrantLock(); // allocation lock
	private static Object connLock = new Object(); // connection lock
	private static Object initListenLock = new Object(); // Initialize a listen lock
	private static Object initConnLock = new Object(); // Initialize a connection lock
	public static Object shutLock = new Object(); // shutdown lock

	// -----------------------------------------------------------------------
	// Instance variables
	// -----------------------------------------------------------------------
	private XTS nextXTS = null; // next on the list
	
	/**
	 *  Next 2 fields are used for XTSException processing.
	 *  In cases where an exception was not thrown, the xtsResponseCode should contain a value
	 *  to be used for the XTSException to be thrown. 
	 */
	private Exception e = null;       // Exception that was thrown  
	private int xtsResponseCode = 0;  // XTS response code
	
	private IXTSreceiver rcb = null; // receive callback
	private Object rxuserval = null; // receive user value
	
	private IXTStransmitter tcb = null; // transmit callback
	protected Object txuserval = null; // transmit user value
	long timeout; // timeout
	private Object waitFor = null; // object on which to wait
	// private int route; // route for replies
	private Object state; // state
	private IConnection connection; // connection on which sent
	private int stateNumber; // state number
	private int index;
	private int clientIndex;
	private boolean noClient = false;
	// private String register = null;
	private static AtomicInteger gIndex = new AtomicInteger(0);
	private static AtomicInteger cIndex = new AtomicInteger(0);
	// -----------------------------------------------------------------------

	// --------------------------------------------------------
	// Static initializer for Directory search chain.
	// If xtsurl.cfg found add it to Directory search chain
	// Always add DefaultDirectory to Directory search chain
	// If XTSDEFAULT is not defined, it checks the system/environment variable XTSDSURL to determine the remote Directory Server
	// If XTSDSURL is not defined, it uses XTSINI=sandbox.cfg file as file directory server
	// If XTSDEFAULT is defined, the user sets the directory server
	// --------------------------------------------------------
	static {
		IXtsDirectory d = null;
		String xtsdefault = System.getProperty("XTSDEFAULT");
		XTStrace.verbose("XTS trace settings have been initialized");
		if (xtsdefault == null) {
			try {			
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Check default directory settings");
				String xtsdsurl = System.getenv(DefaultDirectory.XTSDSURL);
				if (xtsdsurl == null) {
					xtsdsurl = System.getProperty(DefaultDirectory.XTSDSURL);
				} 
				if (xtsdsurl != null) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("XTSDSURL=" + xtsdsurl);
//					System.out.println("XTSDSURL=" + xtsdsurl);
					DefaultDirectory.xtsdsurl =  xtsdsurl;
//					DefaultDirectory.setXtsDsUrl (xtsdsurl);
				} else {
					/* If no directory defined use sandbox.cfg file to access */
					File f = new File(XTSINI);
					if (f.exists()) {
						d = new INIdir(XTSINI);
					} else {
						xtsdir = System.getProperty(XTSDIR);
						if (xtsdir == null) {
							xtsdir = System.getProperty(XTSDIR.toLowerCase());
						}
						if (xtsdir != null) {
							f = new File(xtsdir + XTSINI);
							if (f.exists()) {
								d = new INIdir(xtsdir + XTSINI);
							}
						}
					}
				}
				if (d != null) {
					if (XTStrace.bGlobalVerboseEnabled) {
						XTStrace.verbose("Using default INI file=" + XTSINI);
						System.out.println("Using default INI file=" + XTSINI);
						if (xtsdir != null) 
							XTStrace.verbose("XTSDIR=" + XTSDIR);
					}
					directoryVector.addElement((Directory) d);
				}
			} 
			catch (Throwable th) {
				if (XTStrace.bGlobalErrorEnabled) {
					th.printStackTrace();
				}
			}
		} else {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("XTS default directory settings XTSDEFAULT=" + xtsdefault);
		}	
		if (directoryVector.size() == 0) {
			directoryVector.addElement(defaultDirectory);
		}
	}

	public XTS() {
		incIndex();
		clientIndex = cIndex.incrementAndGet();
	}

	public final String toString() {
		return "[" + clientIndex + "/" + index + "/" + state + ":" + (freeState ? "1" : "0") + "]";
	}

	/**
	 * Set new XTSDSURL for the remote connections to the server
	 * 
	 * @param xtsDsUrl
	 *            New XTSDSURL location
	 * @throws Exception
	 */
	public static IXtsDirectory getDirectory(final String xtsDsUrl) throws Exception {
		return getDirectory(xtsDsUrl, null);
	}

	/**
	 * Set new XTSDSURL for the remote connections to the server
	 * 
	 * @param xtsDsUrl
	 *            New XTSDSURL location
	 * @throws Exception
	 */
	public static IXtsDirectory getDirectory(final String xtsDsUrl,	final String partition) throws Exception {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Calling getDirectory");
		IXtsDirectory directory = null;
		if (xtsDsUrl.startsWith("tcpip")) {
			directory = new DefaultDirectory(); 

			String[] param = new String[4];
			param[0] = "xtsdsurl";
			param[1] = xtsDsUrl;
			param[2] = "passthru";
			param[3] = "yes";
			directory.setParameters(param);
		} else {
			if (xtsDsUrl.startsWith("file")) {
				directory = new INIdir();
				String[] param = new String[2];
				param[0] = "file";
				int b = xtsDsUrl.indexOf(':');
				param[1] = xtsDsUrl.substring(b + 1, xtsDsUrl.length());
				directory.setParameters(param);
			}
		}

		if (directory != null) {
			if (partition != null) {
				directory.setPartition(partition);
			}
			directory.commit(true);
		}
		return directory;
	}

	// -----------------------------------------------------------------------
	/**
	 * Set trace level.
	 * @param level
	 **/
	// -----------------------------------------------------------------------
	public static final void setTrace(final int level) {
		CommonTrace.setGlobalTrace (level);
		if (XTStrace.bGlobalInfoEnabled) 
			XTStrace.info("Set XTS trace to level=" + level);
	}

	// -----------------------------------------------------------------------
	// Allocate an instance
	// -----------------------------------------------------------------------
	private static final XTS newXTS(final Object state, final IXTSreceiver rcb, final Object userval, final Object wait_for) {
		return newXTS(state, null, null, rcb, userval, wait_for);
	}

	private static final XTS newXTS(final Object state, final IXTStransmitter tcb, final Object userval, final Object wait_for) {
		return newXTS(state, tcb, userval, null, null, wait_for);
	}

	private static final XTS newXTS(final Object state, final IXTStransmitter tcb, final Object txuserval, final IXTSreceiver rcb, final Object rxuserval, final Object wait_for) {
		XTS x;
		allocLock.lock();
		try {
			x = freeXTS;
			if (x != null) {
				freeXTS = x.nextXTS;
			} else {
				x = new XTS();
			}
			x.stateNumber = nextNumber++;
		} finally {
			allocLock.unlock();
		}
		x.setWaitFor(wait_for);
		x.nextXTS = null;
		x.state = state;
		x.rcb = rcb;
		x.txuserval = txuserval;
		x.tcb = tcb;
		x.rxuserval = rxuserval;
		x.freeState = false;
		x.incIndex();
		if (XTStrace.bGlobalVerboseEnabled)  {
			XTStrace.verbose("Initialized XTS " + x + " RCB " + rcb + " TCB " + tcb + " wait_for " + wait_for);
			XTStrace.verbose("Allocate State:" + x.stateNumber + " " + (String) state + " tx:" + txuserval + " rx:" + rxuserval + " " + x + " wait_for:" + wait_for);
		}
		return x;
	}

	private void checkSubNode(final Object node) {
		if (node != null) {
			if (node != this) {
				return;
			}

			if (node instanceof XTS) {
				XTS x = (XTS) node;
				if (x.nextXTS != null) {
					x.free();
				}
			}
		}
	}

	// -----------------------------------------------------------------------
	// Free
	// -----------------------------------------------------------------------
	private final Object free() {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Free XTS " + this + " State:" + stateNumber + " RCB " + rcb + " TCB " + tcb);
		waitFor = null;
		checkSubNode(tcb);
		checkSubNode(rcb);
		rcb = null;
		tcb = null;
		e =   null;
		xtsResponseCode = 0;
		txuserval = null;
		state = null;
		if (connection != null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Free connection " + connection);
			connection.setFree();
		}
		connection = null;
		noClient = false;

		allocLock.lock();
		try {
			nextXTS = freeXTS;
			freeXTS = this;
			freeState = true;
			return rxuserval; // return the received userval
		} finally {
			allocLock.unlock();
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Set Directory service.
	 ** 
	 * @param dir
	 *            the directory service to set.
	 ** @return the previous directory service.
	 **/
	// -----------------------------------------------------------------------
	public static final Directory setDirectory(final Directory dir) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Directory " + dir + " set to Directory search chain");
		Directory old = (Directory) directoryVector.elementAt(0);
		// dir_vector.removeElement(old);
		directoryVector.clear();
		directoryVector.addElement(dir);
		// dir_vector.addElement(def_dir); per KT 7/8/99 rxc
		return old;
	}

	/**
	 * Set Directory service.
	 ** 
	 * @param dir
	 *            the directory service to set.
	 ** @return the previous directory service.
	 **/
	// -----------------------------------------------------------------------
	public static final Directory addDirectory(final IXtsDirectory dir) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Directory " + dir + " added to Directory search chain");
		Directory old = (Directory) directoryVector.elementAt(0);
		// dir_vector.removeElement(old);
		directoryVector.addElement(dir);
		// dir_vector.addElement(def_dir); per KT 7/8/99 rxc
		return old;
	}

	// -----------------------------------------------------------------------
	/**
	 * Set Directory service.
	 ** 
	 * @param dir
	 *            the directory service to set.
	 ** @return the previous directory service.
	 **/
	// -----------------------------------------------------------------------
	public static final Directory setDirectory(final IXtsDirectory dir) {
		return setDirectory((Directory) dir);
	}

	// -----------------------------------------------------------------------
	/**
	 * Set Directory service.
	 ** 
	 * @param dir
	 *            the directory service to set.
	 ** @return the previous directory service.
	 * @throws Exception
	 **/
	// -----------------------------------------------------------------------
	public static final Directory setDirectory(final String xtsdsurl) throws Exception {
		return setDirectory(getDirectory(xtsdsurl));
	}

	// -----------------------------------------------------------------------
	/**
	 * Get Directory service.
	 ** 
	 * @return the current directory service.
	 **/
	// -----------------------------------------------------------------------
	public static final Directory getDirectory() {
		return (Directory) directoryVector.elementAt(0);
	}

	// -----------------------------------------------------------------------
	/**
	 * Get Directory service.
	 ** 
	 * @return the current directory service.
	 **/
	// -----------------------------------------------------------------------
	public static final IXtsDirectory[] getDirectories() {
		IXtsDirectory[] dirs = new IXtsDirectory[directoryVector.size()];
		for (int i = 0; i < dirs.length; i++) {
			dirs[i] = directoryVector.elementAt(i);
		}
		return dirs;
	}

	// -----------------------------------------------------------------------
	/**
	 * Register server by target ID. XTS will try to instantiate any LISTENS and
	 * CONNECTS for the server, by obtaining URL information from the directory
	 * and calling the relevant driver.
	 * 
	 * @param targetID
	 *            the numeric target ID to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final int targetID, final IXTSreceiver callback, final Object uservalue) throws XTSException  {
		if (targetID < 0 || targetID > 2113929215) {
			xtsError("Invalid Server Id=" + targetID, XTSException.XTS_INVALID_TARGETID);
			return;
		}
		Server server = Server.getByID(targetID);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Register Server Id=" + targetID);
		if (server == null) {
			register(new Server(targetID, Integer.toString(targetID), 300, callback, uservalue), ZERO_LEN_XTSURL_ARRAY, ZERO_LEN_XTSURL_ARRAY);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Id=" + targetID + " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Register server by target ID. XTS will try to instantiate any LISTENS and
	 * CONNECTS for the server, by obtaining URL information from the directory
	 * and calling the relevant driver.
	 * 
	 * @param targetID
	 *            the numeric target ID to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 * @param listens
	 *            an array of listen urls to be used, bypassing directory
	 *            retrieval.
	 * @param connects
	 *            an array of connection urls to be used, bypassing directory
	 *            retrieval.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final int targetID, final IXTSreceiver callback, final Object uservalue, final XTSurl[] listens, final XTSurl[] connects) throws XTSException {
		if (targetID < 0 || targetID > 2113929215) {
			xtsError("Invalid Server Id=" + targetID, XTSException.XTS_INVALID_TARGETID);
			return;
		}
		Server server = Server.getByID(targetID);
		if (server == null) {
			register(new Server(targetID, Integer.toString(targetID), 300,
					callback, uservalue), listens, connects);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Id=" + targetID + " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Register replicate server by target ID.
	 * 
	 * @param targetID
	 *            the numeric target ID to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 * @param replicateID
	 *            a unique ID representing the replicate set.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final int targetID, final IXTSreceiver callback, final Object uservalue, final long replicateID) throws XTSException {
		if (targetID < 0 || targetID > 2113929215) {
			xtsError("Invalid Server Id=" + targetID, XTSException.XTS_INVALID_TARGETID);
			return;
		}
		Server server = Server.getByID(targetID);
		if (server == null) {
			Server x = new Server(targetID, Integer.toString(targetID), 300, replicateID, callback, uservalue);
			x.isReplicate = true;
			register(x, ZERO_LEN_XTSURL_ARRAY, ZERO_LEN_XTSURL_ARRAY);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Id=" + targetID + " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Register server by target name.
	 * 
	 * @param targetName
	 *            the target Name to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final String targetName, final IXTSreceiver callback, final Object uservalue) throws XTSException {
		Server server = Server.getByName(targetName);
		if (server == null) {
			register(new Server(0, targetName, 300, callback, uservalue), ZERO_LEN_XTSURL_ARRAY, ZERO_LEN_XTSURL_ARRAY);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Name=" + targetName + " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Register server by target name.
	 * 
	 * @param targetName
	 *            the target Name to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 * @param listens
	 *            an array of listen urls to be used, bypassing directory
	 *            retrieval.
	 * @param connects
	 *            an array of connection urls to be used, bypassing directory
	 *            retrieval.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final String targetName, final IXTSreceiver callback, final Object uservalue, final XTSurl[] listens, final XTSurl[] connects) throws XTSException {
		Server server = Server.getByName(targetName);
		if (server == null) {
			register(new Server(0, targetName, 300, callback, uservalue), listens, connects);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Name=" + targetName	+ " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Register replicate server by target name.
	 * 
	 * @param targetName
	 *            the target Name to register.
	 * @param callback
	 *            indicates the method to call back upon receipt of data.
	 * @param uservalue
	 *            an object to be passed to the callback method.
	 * @param replicateID
	 *            a unique ID representing the replicate set.
	 **/
	// -----------------------------------------------------------------------
	public static final void register(final String targetName, final IXTSreceiver callback, final Object uservalue, final long replicateID) throws XTSException {
		Server server = Server.getByName(targetName);
		if (server == null) {
			Server x = new Server(0, targetName, 300, replicateID, callback, uservalue);
			x.isReplicate = true;
			register(x, ZERO_LEN_XTSURL_ARRAY, ZERO_LEN_XTSURL_ARRAY);
		} else {
			if (XTStrace.bGlobalWarnEnabled) 
				XTStrace.warn("Server Name=" + targetName + " has already been Registered, ignoring request");
		}
	}

	// -----------------------------------------------------------------------
	// Do the actual registering
	// -----------------------------------------------------------------------
	private static final void register(final Server ps, final XTSurl[] listenArray, final XTSurl[] connectArray) throws XTSException {
		XTSurl[] listens = null;
		XTSurl[] connects = null;
		IDriver d;

		if (listenArray.length == 0 && connectArray.length == 0) {
			Enumeration<IXtsDirectory> e = directoryVector.elements();
			while (listens == null && e.hasMoreElements()) {
				Directory directory = (Directory) e.nextElement();
				try {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Register Server=" + ps.targname);
					listens = directory.retrieve(XTSLISTEN, ps.targname);
				} catch (Exception ex) {
				}
			}

			e = directoryVector.elements();
			while (connects == null && e.hasMoreElements()) {
				Directory directory = (Directory) e.nextElement();
				try {
					connects = directory.retrieve(XTSCONNECT, ps.targname);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			if (listens == null && connects == null) {
				xtsError("Can't register Server=" + ps.targname + ": No Directory Information", XTSException.XTS_REGISTER_SERVER_NO_DIR);
			}
		} else {
			listens = listenArray;
			connects = connectArray;
		}

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Listen Length=" + ((listens != null) ? listens.length : "null"));
		if (listens != null) {
			for (int i = 0; i < listens.length; i++) {
				try {
					XTS x = newXTS(LISTEN, ps.rcb, ps.uservalue, "Listen " + ps.targname);
					ps.checkChirpInterval(listens[i]);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Server Listen initiated:" + ps.targname + " " + listens[i]);
					// compare IP addresses since a host can be known by [0032]
					// many names. If a listen has already been established that
					// matches this protocol/address/port combination, then, use
					// it [0032]
					// otherwise start a new driver listen. [0032]
					// We need to synchronize listen initialization, to prevent
					// an address in use failure on servers using the same port.
					synchronized (initListenLock) {
						if ((d = existingConnectionListen(listens[i])) == null) {
							dconn.addElement((d = listens[i].driver().listen(listens[i], x, x)));
							ps.addChirpUrl(listens[i]);
						} else
							ps.addChirpUrl(d.getStatus().url);
					}
				} catch (Exception ex) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error(ex);
				}
			}
		}

		if (connects != null) {
			for (int i = 0; i < connects.length; i++) {
				try {
					XTS x = newXTS(CONNECT, ps.rcb, ps.uservalue, "Connect " + ps.targname);
					ps.checkChirpInterval(connects[i]);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Server connect initiated:" + ps.targname + " " + connects[i]);
					// compare IP addresses since a host can be known by many names. If a connection has already been established
					// that matches this protocol/address/port combination, then, use it
					// otherwise start a new driver connection.
					// We need to synchronize connection initialization, to
					// prevent having more than 1 connection to the same destination.
					//
					synchronized (initConnLock) {
						if ((d = existingConnectionListen(connects[i])) == null) {
							dconn.addElement((d = connects[i].driver().connect(connects[i], x, x)));
							ps.addChirpUrl(connects[i]);
						} else {
							ps.addChirpUrl(d.getStatus().url);
							Server.chirp_all();
						}
					}
				} catch (Exception ex) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error(ex);
				}
			}
		}
	}

	/**
	 * Get a connection for the specified URL. The connection is reused if the
	 * line is free. If not a new connection is established and used.
	 * 
	 * @param url
	 * @param state
	 * @param p
	 * @return
	 */
	private static IConnection getConnection(final XTSurl url, final XTS state, final Message p, final int connTo, XTS x) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(">>>>Get connection to " + state);
		IConnection conn = getFreeUrl(url);
		if (conn == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Found no one in the list, create new one");
			// System.out.println("URL:"+url.target+" "+url.port);
			Object th = new Object();
			synchronized (th) {
				if (connTo > 0)
					url.driver().connect(url, state, th, connTo);
				else
					url.driver().connect(url, state, th);
				if (!(state.getWaitFor() instanceof IConnection)) {
					try {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Wait with sleeptimeout=" + sleepTimeout);
						th.wait(sleepTimeout); // wait for connect
					} catch (InterruptedException ie) {
					}
				}
			}
			if (state.getWaitFor() instanceof IConnection) {// success?
				conn = (IConnection) state.getWaitFor();
				// Connection complete - Indicate waiting on Message
				state.setWaitFor(p);
				conn.setLock();
				addUrls(url, conn);
				// urls.put(url[0].toString(),conn); [0033]
				state.setWaitFor(p); // restore message
			} else {
				if (!(state.getWaitFor() instanceof String)) {
					state.setWaitFor("Connection Failed");
				}
				String target = null;
				if (url.partition != null)
					target = url.partition + url.target;
				else
					target = url.target;
				DefaultDirectory.deleteTargetFromCache(target);
				Target.removeByName(target);
				
				state.fail((String) state.getWaitFor(), p);
				return null;
			}
		}
		if (x != null)
	 		x.rxuserval = RXTIMEOUT;
		return conn;
	}

	/**
	 * Search for current active connection one which is defined as free
	 * 
	 * @param url
	 * @return
	 */
	private static IConnection getFreeUrl(final XTSurl url) {
		String location = url.toString(XTSurl.HOST_ADDRESS_FORMAT);

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Get free connection in urlList for location " + location);
		ArrayList<IConnection> urlList = urls.get(location);
		if (urlList != null) {
			for (IConnection c : urlList) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("[CONN] list=" + c);
				if (useOnlyOneConnection) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("[CONN] Return one connection=" + c);
					c.setLock();
					return c;
				}
				if ((c.usage() == 0) || (!connectEachConnection)) {
					if (c.isFree()) {
						c.setLock();
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("[CONN] Found free connection in urlList=" + c);
						return c;
					}
					/*
					 * c.setLock(); return c;
					 */
				}
			}
		}
		return null;
	}

	private static void addUrls(final XTSurl url, final IConnection conn) {
		ArrayList<IConnection> urlList = null;
		String location = "Unknown";
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Add url=" + url + " for connection=" + conn);
		if (url != null) {
			location = url.toString(XTSurl.HOST_ADDRESS_FORMAT);
			urlList = urls.get(location);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("[CONN] location=" + location);
		}
		if (urlList == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("[CONN] Add connection=" + conn + " to location=" + location + " New urlList");
			urlList = new ArrayList<IConnection>();
			urlList.add(conn);
			urls.put(location, urlList);
		} else {
			for (IConnection c : urlList)
				if (c == conn) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("[CONN] Match! conn=" + c);
					return;
				} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("[CONN] Connection=" + c);
				}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("[CONN] Add connection=" + conn + " to existing urlList");
			urlList.add(conn);
		}
	}

	public static void refreshConnection(final Object token) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] RefreshConnection for token=" + token);
		if (token instanceof IConnection) {
			IConnection conn = (IConnection) token;
			int usage = conn.releaseUsage();
			if ((closeConnections) && (usage == 0)) {
				refreshConnection(conn.getUrl(), conn);
			}
		}
	}

	private static void refreshConnection(final XTSurl url,	final IConnection conn) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Refresh connection for url=" + url + " conn=" + conn);
		if ((url == null) || (conn == null)) {
			return;
		}
		if (conn.isClosed()) {
			return;
		}
		synchronized (connLock) {
			int usage = conn.usage();
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("[CONN] Current connection usage=" + usage + " of conn=" + conn + " closed=" + conn.isClosed() + " free=" + conn.isFree());
			if (usage == 0) {
				String location = url.toString(XTSurl.HOST_ADDRESS_FORMAT);
				ArrayList<IConnection> urlList = urls.get(location);
				// Remove unused connection if more than one
				if ((urlList != null) && (urlList.size() > forceOpenConnections)) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("[CONN] Remove and Close connection=" + conn);
					urlList.remove(conn);
					conn.close();
				}
			}
		}
	}

	private static void removeConnection(final XTSurl url, final IConnection conn) {
		conn.setFree();
		// connections.remove(conn.getRegisterName());
		String location = url.toString(XTSurl.HOST_ADDRESS_FORMAT);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Remove location=" + location + " for connection=" + conn);
		urls.remove(location);
	}

	/**
	 * Get all XTSurls from all Directory server
	 * 
	 * @param qualifier
	 * @param target
	 * @return
	 * @throws Exception
	 */
	public static final XTSurl[] retrieveUrls(final String qualifier, final String target) throws Exception {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Search for qualifier=" + qualifier + " target=" + target);
		ArrayList<XTSurl> list = new ArrayList<XTSurl>();
		Iterator<IXtsDirectory> dirIt = directoryVector.iterator();
		while (dirIt.hasNext()) {
			IXtsDirectory directory = dirIt.next();
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("DIR=" + directory);
			XTSurl[] urls = directory.retrieve(qualifier, target);
			if (urls != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("URLS found=" + urls.length);
			}
			if ((urls != null) && (urls.length > 0)) {
				for (XTSurl url : urls)
					list.add(url);
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("List size=" + list.size());
		}
		if (list.size() == 0) {
			return null;
		}
		return list.toArray(new XTSurl[0]);
	}

	/**
	 * Retrieve first XTSurl for a specific target.
	 * 
	 * @param target : Target to search for
	 * @return XTSurl of target or null if not found
	 */
	public static XTSurl retrieveUrl(final Object target) {
		if (target instanceof XTSurl) {
			return (XTSurl) target;
		}
		String targetName = null;
		if (target instanceof String) {
			targetName = (String) target;
		}
		if (target instanceof Integer) {
			int targetID = (Integer) target;
			targetName = Integer.toString(targetID);
		}
		XTSurl[] url = null;
		Enumeration<IXtsDirectory> e = directoryVector.elements();
		if (e == null) {
			return null;
		}
		while (url == null && e.hasMoreElements()) {
			Directory directory = (Directory) e.nextElement();
			try {
				url = directory.retrieve(XTSACCESS, targetName);
			} catch (Exception ee) {
			}
		}
		if ((url != null) && (url.length > 0)) {
			return url[0];
		}
		return null;
	}

	// -----------------------------------------------------------------------
	/**
	 * Send data to a server. =================== send ==========================
	 * 
	 * @param sendParms
	 *            contains all the parameters used to process the send request.
	 **/
	// -----------------------------------------------------------------------
	public static final void send (final XTSSendParameters sendParms) throws XTSException {
		send (sendParms, null);
	}

	public static final void send (final XTSSendParameters sendParms, XTS x) throws XTSException {
		XTSurl remoteUrl = null;
		if (sendParms.targetName == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(">>>>Send Message to " + sendParms.targetId + " " + sendParms.p.toString()
					+ " tx:" + sendParms.txuserval + " rx:" + sendParms.rxuserval + " to:"
					+ sendParms.timeout + " hostname:" +sendParms.hostName );
		}
		else {
			if (sendParms.aliasName == null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(">>>>Send Message to " + sendParms.targetName + " " + sendParms.p.toString()
						+ " tx:" + sendParms.txuserval + " rx:" + sendParms.rxuserval + " to:"
						+ sendParms.timeout  + " hostname:" +sendParms.hostName );
			}
			else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(">>>>Send Message to " + sendParms.targetName + " " + sendParms.aliasName + " " + sendParms.p.toString()
						+ " tx:" + sendParms.txuserval + " rx:" + sendParms.rxuserval + " to:"
						+ sendParms.timeout  + " hostname:" +sendParms.hostName );
			}
		}

		XTS state;
		if (sendParms.targetName == null)  {
			sendParms.p.target = sendParms.targetId; // set in message just in case
			// mihai - to force xts to search for the connection handle 
                        sendParms.targetName = Integer.toString(sendParms.targetId);
			state = newXTS(SEND, sendParms.txcb, sendParms.txuserval, sendParms.rxcb, sendParms.rxuserval, sendParms.p);
			if (sendParms.targetId < 0 || sendParms.targetId > 2113929215) {
				state.e = new XTSException("Invalid Target Id=" + sendParms.targetId, XTSException.XTS_INVALID_TARGETID);
				state.fail("Invalid Target ID=" + sendParms.targetId, sendParms.p);
				return;
			}
		} else {
			state = newXTS(SEND, sendParms.txcb, sendParms.txuserval, sendParms.rxcb, sendParms.rxuserval, sendParms.p);
		}
		if (sendParms.p.length <= 0) {
			state.e = new XTSException(XTSException.XTS_ZERO_LENGTH);
			state.fail("Invalid message length=" + sendParms.p.length, sendParms.p);
			return;
		}
		if (sendParms.aliasName == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Check path to targetName=" + sendParms.targetName);
		}
		else {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Check path to targetName=" + sendParms.targetName + " aliasName=" + sendParms.aliasName);
		}
		IConnection conn = null;
		if (sendParms.p.token instanceof IConnection) {
			conn = (IConnection) sendParms.p.token;
			if (XTStrace.bGlobalVerboseEnabled) 
                    	    XTStrace.verbose("Reuse send message connection token id=" + conn);
		}

		Target pt = null;
		if (conn != null) {
			conn.setLock();
		} else {
			// check for an existing connection 
			if (sendParms.aliasName == null) 
				pt = Target.getByName(sendParms.targetName, sendParms.hostName);
			else
				pt = Target.getByName(sendParms.aliasName, sendParms.hostName);		
			if (pt == null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Opening a new connection ..." );
				synchronized (connLock) {
					if (sendParms.aliasName == null)
						pt = Target.getByName(sendParms.targetName, sendParms.hostName);
					else
						pt = Target.getByName(sendParms.aliasName, sendParms.hostName);		
				}
				if (pt == null) {
					if (sendParms.url == null)  {
						XTSurl[] url = null;
						Enumeration<IXtsDirectory> e = directoryVector.elements();
						if (e == null) {
							state.e = new XTSException("No directory entry found for target=" + sendParms.targetName, XTSException.DS_NO_URL_ENTRIES);
							state.fail("No directory entry found for target=" + sendParms.targetName, sendParms.p);
							return;
						}
						while (url == null && e.hasMoreElements()) {
							Directory directory = (Directory) e.nextElement();
							try {
								if (sendParms.hostName == null)
									url = directory.retrieve(XTSACCESS, sendParms.targetName);
								else
									url = directory.retrieve(XTSACCESS, sendParms.targetName, sendParms.hostName);
							} catch (Exception ee) {
								state.e = ee;
								state.fail("Retrieve failed because of an invalid port specified in hostname:port", sendParms.p);
								return;
							}
						}
						if (url == null) {
							state.e = new XTSException("No directory entry found for target " + sendParms.targetName, XTSException.DS_NO_URL_ENTRIES);
							state.fail("No directory entry found for target=" + sendParms.targetName, sendParms.p);
							return;
						}
						remoteUrl = url[0];
					} else {
						remoteUrl = sendParms.url;			
					}
				}
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Sending to TargetName=" + sendParms.targetName + " TargetAlias=" + sendParms.aliasName + " URL=" + remoteUrl);
				if (sendParms.targetId > 0) // this is when we have a target id that is used for searching a connection based on target name
					sendParms.p.target = sendParms.targetId;
				else
					sendParms.p.target = SERVER_ID_BASE;
			} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Got Target=" + pt.getTargname() + " Alias=" + pt.getAliasname() + " TargetId=0x" + String.format("%x", pt.getTarget()));
				conn = pt.connection;
				remoteUrl = conn.getUrl();
				sendParms.p.token = conn;
				sendParms.p.target = pt.getTarget();
			}
			if (remoteUrl != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Remote URL=" + remoteUrl);
				synchronized (connLock) {
					conn = getConnection(remoteUrl, state, sendParms.p, sendParms.connTo, x);
					if (conn != null) {
						conn.increaseUsage();
					}
				}
				if (pt == null && sendParms.targetName != null && conn != null)  {
					if (sendParms.aliasName == null) {
						if (sendParms.targetId > 0) {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("Avoid resolve to TargetId=" + sendParms.targetId);
							pt = new Target(sendParms.targetId, sendParms.targetName, conn, sendParms.hostName);
						} else {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("Create Target Name =" + sendParms.targetName + " for resolve target");
							pt = new Target(-1, sendParms.targetName, conn, sendParms.hostName);
						}
					}
					else
						pt = new Target(-1, sendParms.targetName, sendParms.aliasName, conn, sendParms.hostName);
				}
			}
			if (conn == null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Found no connection, create new one");
				return;
			}
		}
		//save the connection in the message token
		sendParms.p.token = conn;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Connect to conn=" + conn);
                 int newtargetid = 0;

		 if ((pt != null) && (pt.getTarget() == -1)) { 
		     if (XTStrace.bGlobalVerboseEnabled) 
                    	 XTStrace.verbose("Resolve Target " + pt.getTargname()); 
                     newtargetid = resolveTarget(state, pt, pt.getTargname(), sendParms.p, sendParms.txuserval, conn); 
		     if (newtargetid == 0) {
			     if (XTStrace.bGlobalVerboseEnabled) 
	       	            	 XTStrace.verbose("Resolve Target=" + pt.getTargname() + " failed"); 
			     Target.remove(conn); // remove from named targets
			     removeConnection(conn.getUrl(), conn);
			     return;
		     }		     
                     sendParms.p.target = newtargetid;
		     if (XTStrace.bGlobalVerboseEnabled) 
                     	XTStrace.verbose("Target name resolved to TargetId=0x" + String.format("%x", sendParms.p.target)); 
                } 		 

		sendParms.p.timeout = (int) (sendParms.timeout / 1000); // make seconds
		if (sendParms.rxcb == null) {
			sendParms.p.route = 0;
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug("No route");
		} else {
			sendParms.p.route = currentSendMessageNo.incrementAndGet();
			replyLock.lock();
			try {
				if (XTStrace.bGlobalDebugEnabled) 
					XTStrace.debug("Put reply route=" + sendParms.p.route + " State=" + state);
				state.timeout = System.currentTimeMillis() + sendParms.timeout;
				replies.put(sendParms.p.route, state);
			} finally {
				replyLock.unlock();
			}
			sendParms.rxcb.setConnection(conn);
		}

		sendParms.p.ttl = ttl;
		sendParms.p.priority = priority; // set ttl/priority
		state.connection = conn;
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.debug("Connection=" + conn);
                sendParms.p.putHeader ();
		Message.dump("Send user data ", "send", sendParms.p);
		if (sendParms.p.route == 0) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("No route set " + sendParms.p + " " + sendParms.targetName + " rxcb=" + sendParms.rxcb);
			Thread.dumpStack();
			state.e = new XTSException(XTSException.XTS_NO_ROUTE_SET);
			state.fail("No route set " + sendParms.p + " " + sendParms.targetName, sendParms.p);
			return;
		}
		conn.receive(state, null);
		if (conn.send(sendParms.p, state, state)) {
			replyLock.lock();
			try {
				// synchronized (replyLock) {
				if (XTStrace.bGlobalDebugEnabled) 
					XTStrace.debug("Remove reply " + sendParms.p.route);
				if (sendParms.rxcb != null) {
					replies.remove(sendParms.p.route);
				}
			} finally {
				replyLock.unlock();
			}
			// The urls entry should have been removed during disconnect - rxc
			Target.remove(conn); // remove from named targets

			removeConnection(conn.getUrl(), conn);
			/*
			 * // TODO TKN Remove URL -1 Check that // if (xtsUrltarget != -1)
			 * connections.remove(conn.getRegisterName());
			 * 
			 * if (target instanceof XTSurl) { urls.remove(((XTSurl) target)
			 * .toString(XTSurl.HOST_ADDRESS_FORMAT)); }
			 */
			state.e = new XTSException("Send problem with target " + conn.getUrl().target + " " + conn, XTSException.XTS_SEND_FAILED);
			state.fail("Send problem with target " + conn.getUrl().target + " " + conn, sendParms.p);
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Send finished " + conn + " MsgNo=" + sendParms.p.msgno + " RoutId=" + sendParms.p.route);
	}

	public static final Message sendAndWait (final XTSSendParameters sendParms) throws XTSException {

		if (XTStrace.bGlobalVerboseEnabled) { 
			XTStrace.verbose(">>>>SendAndWait start MsgNo=" + sendParms.p.msgno);
			if (sendParms.targetName == null)
				XTStrace.verbose("New Object SendAndWait to " + sendParms.targetId + " " + sendParms.p.toString() + " timeout:" + sendParms.timeout);
			else
				XTStrace.verbose("New Object SendAndWait to " + sendParms.targetName + " " + sendParms.p.toString() + " timeout:" + sendParms.timeout);	
		}			
		if (XTStrace.bGlobalDebugEnabled) 
			XTStrace.dump("Sending Message:", "sendAndWait", sendParms.p.body, sendParms.p.length, true);
		XTS x = newXTS(SNDWAIT, null, "txtimeout", null, RXTIMEOUT, null);
		x.setWaitFor(x);
		x.rcb = (IXTSreceiver) x;
		x.tcb = (IXTStransmitter) x; // set callback addresses
//		if (XTS.checkMsgFreed) {
//			if (sendParms.p.isFreed()) {
//				if (XTStrace.bGlobalErrorEnabled) 
//					XTStrace.error("XTS: message to be send already freed: Corrupt?");
//				Thread.dumpStack();
//			}
//		}
		x.noClient = false;
		
		/**
		 * The XTS object provides the transmitter/receiver interface methods
		 *  for the SendAndWait method.
		 */
		sendParms.SetTransmitterAndReceiveParameters(x.tcb, x.rcb);
		send(sendParms, x);

		synchronized (x) {
			if (x.rxuserval == RXTIMEOUT) {
				try {
					if (XTStrace.bGlobalVerboseEnabled) {
						XTStrace.verbose("Client wait for request " + x);
						XTStrace.verbose("XTS waits " + (sendParms.timeout + 100));
					}
					x.wait(sendParms.timeout + 100);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Client got request back");
					// reply comes back in rxuserval
				} catch (InterruptedException ie) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("Client wait interrupted");
					x.xtsResponseCode = XTSException.XTS_CLIENT_WAIT_INTERRUPTED;
					xtsError(x, "Client wait interrupted");
				}
			}
		}
		x.noClient = true;
		if (x.rxuserval instanceof Message) {
			Message m = (Message) x.rxuserval;
			if (m.target == UNRESOLVED_TARGET) {
				m.freeMessage("control_msg");
				x.xtsResponseCode = XTSException.XTS_TARGET_CANNOT_BE_RESOLVED;
				if (sendParms.targetName == null)
					xtsError(x, "Target does not exist " + sendParms.targetId);
				else
					xtsError(x, "Target does not exist " + sendParms.targetName);
			} else if (m.target == ROUTE_FAILED) {
				x.xtsResponseCode = XTSException.XTS_ROUTE_FAILED;
				if (sendParms.targetName == null)
					xtsError(x, "Can't reach " + sendParms.targetId);
				else
					xtsError(x, "Can't reach " + sendParms.targetName);
			} else if (m.target == ROUTE_TIMEOUT) {
				x.xtsResponseCode = XTSException.XTS_ROUTE_TIMEOUT;
				if (sendParms.targetName == null)
					xtsError(x, "Route Timeout on Send to" + sendParms.targetId);
				else
					xtsError(x, "Route Timeout on Send to" + sendParms.targetName);
			} else if (m.target == ROUTE_TABLE_OVERFLOW) {
				x.xtsResponseCode = XTSException.XTS_ROUTE_TABLE_OVERFLOW;
				if (sendParms.targetName == null)
					xtsError(x, "Route Table Overflow on Send to" + sendParms.targetId);
				else
					xtsError(x, "Route Table Overflow on Send to" + sendParms.targetName);
			} else if (m.target == TTL_EXPIRED) {
				x.xtsResponseCode = XTSException.XTS_ROUTE_TTL_EXPIRED;
				if (sendParms.targetName == null)
					xtsError(x, "Time to Live Expired on Send to" + sendParms.targetId);
				else
					xtsError(x, "Time to Live Expired on Send to" + sendParms.targetName);
			} else {
				IConnection conn = x.connection;
				m = (Message) x.free();
				if (XTStrace.bGlobalVerboseEnabled) {
					XTStrace.verbose("sendAndWait Conn=" + conn);
					XTStrace.verbose("sendAndWait Message: " + m);
				}
				m.token = conn;
				sendParms.p.token = conn;
				if (XTStrace.bGlobalDebugEnabled) {
					XTStrace.debug("Message received : " + m);
					XTStrace.dump("Message successfully returned", 	"sendAndWait", m.body, m.length, true);
				}
//				if (XTS.checkMsgFreed) {
//					if (m.isFreed()) {
//						if (XTStrace.bGlobalErrorEnabled) 
//							XTStrace.error("XTS: message received already freed: Corrupt?");
//					}
//				}
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(">>>>SendandWait Ending call Msg:"+ sendParms.p.msgno);
				return m;
			}
		}
		if (x.rxuserval instanceof String) {			
			if (x.rxuserval == RXTIMEOUT && x.e == null && x.xtsResponseCode == 0) {
				x.xtsResponseCode = XTSException.XTS_SEND_RECV_TIMEOUT;	
			    
				if (sendParms.targetName == null) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("\nSend/Receive timed out for TargetId=" + sendParms.targetId + " : Timeout setting=" + sendParms.timeout);
					xtsError(x, "Send/Receive timed out for TargetId=" + sendParms.targetId + " : Timeout setting=" + sendParms.timeout);
				}
				else {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("\nSend/Receive timed out for Target Name=" + sendParms.targetName + " : Timeout setting=" + sendParms.timeout);
					xtsError(x, "Send/Receive timed out for Target Name=" + sendParms.targetName + " : Timeout setting=" + sendParms.timeout);
				}
			}
		}
		
		/**
		 * If there is not XTSException or xtsResponseCode
		 *  then indicate it's an invalid return to sendAndWait.
		 */
		if (x.e == null && x.xtsResponseCode == 0) {
//			x.xtsResponseCode = XTSException.XTS_INVALID_RETURN_SENDANDWAIT;
			x.xtsResponseCode = XTSException.XTS_DISCONNECT;
		}

		if (x.e == null) {
			xtsError(x, "Invalid return to sendAndWait()");
		}
		else {
			xtsError(x, x.e.getMessage());
		}		
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(">>>>Send and Wait Error call ");
		return null; // <<--- never get here
	}

//	@SuppressWarnings("unused")
	private static int resolveTarget(final XTS state, final Target target,	final String targetName, final Message p, final Object txuserval, Object conn) throws XTSException {
		String alias = null;
		Target pt = target;
		alias = pt.getAliasname ();
		synchronized (connLock) {
			if (pt.getTarget() == -1) {
				byte[] b = null;
				try {
					b = targetName.getBytes("UTF8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				Message q = Message.newMessage(b.length);
				q.target = RESOLVE_TARGET_NAME;
				System.arraycopy(b, 0, q.body, 0, b.length);
				q.ttl = ttl;
				q.priority = priority;
				q.route = 0;
				Object th = new Object();
				ResolveTargetObj ResObj = new ResolveTargetObj (XTSException.XTS_TIMEOUT, th);
				resname.put(targetName, ResObj); // resolve name list
				if (XTStrace.bGlobalDebugEnabled) {
					XTStrace.debug("Connection used to resolve target=" + " Obj=" + ResObj /*targetName pt.connection*/ );
		                        q.putHeader ();
					Message.dump("Send RESOLVE TARGET", "resolveTarget", q);
				}
				synchronized (th) {
					// must do it synchronized
					if (pt.connection.send(q, null, null)) {
						// .. because of wake-up
						state.e = new XTSException("Cannot resolve target name=" + targetName, XTSException.XTS_RESOLVE_TARGET_FAILED);
						state.fail("Cannot resolve name=" + targetName + " connection failed", p);
						q.freeMessage("resolve");
						resname.remove(targetName);
						return 0;
					}
					if (pt.getTarget() < 1) {
						try {
							// wait for name reply
							th.wait(sleepTimeout);
						} catch (InterruptedException ie) {
							ie.printStackTrace();
							return 0;
						}
						if (XTStrace.bGlobalDebugEnabled) 
							XTStrace.debug("Resolve Target Reply Rc=" + ResObj.Error);
						if (ResObj.Error != 0) {
							state.e = new XTSException("Cannot resolve name=" + targetName + " Rc=" + ResObj.Error, ResObj.Error);
							state.fail("Cannot resolve name=" + targetName + " Rc=" + ResObj.Error, p);
							q.freeMessage("resolve");
							resname.remove(targetName);
							return 0;
						}
					}
				}
				if (alias != null) {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.debug("Get pt by alias=" + alias);
 					pt = Target.getByName(alias);
				} else {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.debug("Get pt by targetName=" + targetName);
					pt = Target.getByName(targetName);
				}
				if (pt == null) {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.debug("TargetName=" + targetName+ " No entry in the target list");
					state.e = new XTSException("No entry in the target list for targetName=" + targetName, XTSException.XTS_TARGET_UNREACHABLE);
					state.fail("No entry in the target list for targetName=" + targetName, p);
					if (state.tcb == null && state.rcb == null) {
						((XTS) txuserval).e = new XTSException("Can't reach " + targetName, XTSException.XTS_TARGET_UNREACHABLE); 
						xtsError((XTS) txuserval, "No entry in the target list for targetName=" + targetName);
					}
					return 0;
				}
				if (XTStrace.bGlobalDebugEnabled) 
					XTStrace.debug("Target list returned TargetName=" + pt.getTargname() + " AliasName=" + pt.getAliasname() + " TargetId=0x" + String.format("%x", pt.getTarget()));
				if (pt.getTarget() < 1) {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.debug("Can't reach TargetName=" + pt.getTargname() + " Invalid TargetId=0x" + String.format("%x", pt.getTarget()));
			     		pt.remove(conn); // remove from named targets
					state.e = new XTSException("Can't reach TargetName=" + pt.getTargname() + " Invalid TargetId=0x" + String.format("%x", pt.getTarget()), XTSException.XTS_TARGET_UNREACHABLE);
					state.fail("Can't reach TargetName=" + pt.getTargname() + " Invalid TargetId=0x" + String.format("%x", pt.getTarget()), p);
					if (state.tcb == null && state.rcb == null) {
						if (txuserval != null) {
							((XTS) txuserval).e = new XTSException("Can't reach TargetName=" + pt.getTargname() + " Invalid TargetId=0x" + String.format("%x", pt.getTarget()), XTSException.XTS_TARGET_UNREACHABLE); 
							xtsError((XTS) txuserval, "Can't reach " + targetName);
						}
					}
					return 0;
				} else if (!pt.clientTarget) {
					IConnection conn1 = (IConnection)conn;
					addUrls(conn1.getUrl(), pt.connection);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Returning target=0x" + String.format("%x", pt.getTarget()));
                                        return pt.getTarget();
				}
			}
		}
	return 0;
	}


	// -----------------------------------------------------------------------
	/**
	 * Send a message via return route.
	 * 
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param token
	 *            the token passed to the Received callback, indicating this
	 *            message. Used to identify where a reply should be sent.
	 * @param callback
	 *            indicates the method to call back upon transmission
	 *            completion.
	 * @param uservalue
	 *            an object to be passed to the transmit complete method.
	 **/
	public static final void sendViaReturn(final Message p, final Token token, final IXTStransmitter callback, final Object uservalue) throws XTSException {
		if (XTStrace.bGlobalVerboseEnabled) 
              		XTStrace.verbose("Processing sendViaReturn Message=" + p + " token=" + token);
		XTS state = newXTS(SEND, callback, uservalue, p);
                if (token == null) {
			state.e = new XTSException(XTSException.XTS_NO_ROUTE_SET);
			state.fail("Send via return fail; route is zero", p);
                        return;
		}
		p.target = ROUTE_RETURN;
		p.ttl = ttl;
		p.priority = priority;
		p.token = token.driver_token;
		p.route = token.route; // use old route
		if (token.connection.send(p, state, state)) {
			state.e = new XTSException(XTSException.XTS_SEND_VIA_RETURN_FAILED);
			state.fail("Send via return fail", p);
		}
		token.free();
	}

// -----------------------------------------------------------------------
//                            final function
// -----------------------------------------------------------------------
	final void fail(final String msg, final Message p) {
		if (XTStrace.bGlobalErrorEnabled) 
			XTStrace.error("Failed: " + msg + " Message: " + ((p==null)?"null":p.toString()));
		/**
		 * If tcb and/or rcb not equal null 
		 * then we copy the XTSException object reference if not null to the base XTS object
		 * and we copy the xtsResponseCode value if not 0 to the base XTS object.  
		 */
		if (tcb != null) {
			if (((XTS)tcb).e == null)
				((XTS)tcb).e = this.e;
			if (((XTS)tcb).xtsResponseCode == 0)
				((XTS)tcb).xtsResponseCode = this.xtsResponseCode;
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("Failed: ((XTS)tcb).xtsResponseCode=" + ((XTS)tcb).xtsResponseCode);
			tcb.sendFailed(p, txuserval);
		}		
		if (rcb != null) {
			if (((XTS)rcb).e == null)
				((XTS)rcb).e = this.e;
			if (((XTS)rcb).xtsResponseCode == 0)
				((XTS)rcb).xtsResponseCode = this.xtsResponseCode;
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("Failed: ((XTS)rcb).xtsResponseCode=" + ((XTS)rcb).xtsResponseCode);
			rcb.receiveFailed(p, rxuserval);
		} else if (p != null) {
			p.freeMessage("fail");
		}
		if (rcb instanceof XTS) {
                    if (msg == null)
			((XTS) rcb).rxuserval = "Disconnection by server";
                    else
			((XTS) rcb).rxuserval = msg;
		if (XTStrace.bGlobalErrorEnabled) 
			XTStrace.error("Failed: ((XTS) rcb).rxuserval=" + ((XTS) rcb).rxuserval);
		}
		free(); // free ourselves
	}

	// -----------------------------------------------------------------------
	/**
	 * Bind client to a target name. The client can thereafter receive
	 * unsolicited messages.
	 * 
	 * @param token
	 *            Is the token received from the client.
	 * @param targetname
	 *            is the name that the client will be known as.
	 **/
	// -----------------------------------------------------------------------
	public static final void bindClient(final Token token,	final String targetname) {
		byte[] targetNameUTF8 = null;
		try {
			targetNameUTF8 = targetname.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}

		// Message p=Message.newMessage(targetname.length()+8);
		Message p = Message.newMessage(targetNameUTF8.length + 8);
		p.putLong(System.currentTimeMillis());
		// p.putBytes(targetname.getBytes());
		p.putBytes(targetNameUTF8);
		p.target = BIND_CLIENT;
		p.ttl = ttl;
		p.priority = priority;
		p.token = token.driver_token;
		p.route = token.route; // use old route
		XTS state = newXTS(SEND, (IXTStransmitter) null, null, p);
		// new Target(-1,targetname,token.connection);
		new Target(-1, targetname, token.connection, true);
		Status connStatus = token.connection.getStatus();
		addUrls(connStatus.url, token.connection);
		Message.dump("BindClient " + targetname + " to " + token.connection,  "bindClient", p);
		if (token.connection.send(p, state, state)) {
			state.e = new XTSException("BindClient failed",	XTSException.XTS_BINDCLIENT_FAILED);
			state.fail("BindClient fail", p);
		}
		token.free();
	}

	// -----------------------------------------------------------------------
	/**
	 * Give a thread to XTS to use for receive callback. This method may be
	 * called more than once to give more than one thread over. Control is
	 * returned to the caller when shutting down.
	 **/
	// -----------------------------------------------------------------------
	public static final void giveThread() {
		giveThread(0);
	}

	// -----------------------------------------------------------------------
	/**
	 * Give a thread to XTS to use for receive callback. This method may be
	 * called more than once to give more than one thread over. Control is
	 * returned to the caller when shutting down or when the sleepTime has
	 * expired.
	 ** 
	 * @param sleepTime
	 *            gives the time in milliseconds that the thread will wait
	 *            without processing any message before returning. A value of
	 *            zero means wait forever.
	 **/
	// -----------------------------------------------------------------------
	public static final void giveThread(final int sleepTime) {
		isWaiters = true;
		Token tok = null;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("giveThread() called by -> " + Thread.currentThread());
		for (;;) {
			synchronized (msgQ) {
				while (running) {
					tok = msgQ[0]; // get headof queue
					if (tok != null) { // something on the Q					
						msgQ[0] = tok.next; // dequeue it
						if (tok.next == null) {
							msgQ[1] = null;// set tail null also
						}
						break;
					} else {
						waiters++;
						if (sleepTime == 0) {
							try {
								msgQ.wait();
							} catch (InterruptedException ie) {
							}
							waiters--;
						} else {
							long nextTime = System.currentTimeMillis()
									+ sleepTime - 5;
							try {
								msgQ.wait(sleepTime);
							} catch (InterruptedException ie) {
							}
							waiters--;
							if (System.currentTimeMillis() > nextTime) {
								return;
							}
						}
					}
				}
			}
			if (tok != null) {
				if (XTStrace.bGlobalVerboseEnabled)  {
					XTStrace.verbose("giveThread() calling -> " + Thread.currentThread());
					XTStrace.verbose("giveThread() token route=" + tok.msg.route);
				}
				if (tok.msg.route != 0) {
					tok.rcb.received(tok.msg, tok.userval, tok);
				} else {
					tok.rcb.received(tok.msg, tok.userval, null);
					tok.free();
				}
			} else {
				break; // can only happen if not running
			}
		}
	}

	// -----------------------------------------------------------------------
	/** Interface method for driver, called by the driver when connected. **/
	// -----------------------------------------------------------------------
	public void connected(final XTSurl partner, final Object uservalue, final IConnection conn) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Connected to:" + partner);
		// if(trace_traffic) conn.getStatus().trace=true;
		if (state == SEND) {
			XTS x = newXTS(CLIENT, rcb, rxuserval, null);
			conn.receive(x, x);
			setWaitFor(conn); // hand him back the connection
			signal(uservalue);
		} else {
			conn.receive(this, this);
		}
	}

	// -----------------------------------------------------------------------
	/** Interface method for driver, called by the driver when disconnected. **/
	// -----------------------------------------------------------------------
	public void disconnected(final XTSurl partner, final Object uservalue) {
		disconnected(partner, uservalue, null, null);
	}

	// -----------------------------------------------------------------------
	/** Interface method for driver, called by the driver when disconnected. **/
	// -----------------------------------------------------------------------
	public void disconnected(final XTSurl partner, final Object uservalue, final Exception e, final IConnection delConn) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Disconnected from partner=" + partner + " delConn=" + delConn);
		String msg = null;
		
		if (e != null) {
			msg = e.getMessage();
			this.e = e;
		}
		// Thread.dumpStack();
		if (state == SEND) {
			setWaitFor("Connect failed:" + msg); // indicate error
			signal(uservalue);
		}
		boolean remove = true;
		// Object conn=urls.remove(partner.toString());
		// clear trace of this
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] Get list for partner=" + partner.toString(XTSurl.HOST_ADDRESS_FORMAT));
		ArrayList<IConnection> connList = urls.get(partner.toString(XTSurl.HOST_ADDRESS_FORMAT));
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[CONN] connList=" + connList);
		if (connList != null) {
			if ((delConn != null) && (delConn.isClosed())) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("[CONN] Delete specific connection " + delConn);
				boolean outstanding = false;
				replyLock.lock();
				try {
					// synchronized (replyLock) {
					if (replies.size() > 0) {
						Object[] keys = replies.keySet().toArray();
						// replies.getkeys();
						for (int i = keys.length - 1; i > -1; i--) {
							XTS x = (XTS) replies.get(keys[i]);
							if (x != null && delConn == x.connection) {
								outstanding = true;
								break;
							}
						}
					}
				} finally {
					replyLock.unlock();
				}
				if (!outstanding) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("[CONN] Remove connection from conn=" + delConn);
					connList.remove(delConn); // Normal close connection
				}
			} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("[CONN] Remove partner=" + partner.toString(XTSurl.HOST_ADDRESS_FORMAT));
				urls.remove(partner.toString(XTSurl.HOST_ADDRESS_FORMAT));
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Delete all connections");
				for (IConnection conn : connList)
					if (conn != null) { // connections.remove(conn);
						proxies.remove(conn);
						replyLock.lock();
						try {
							// synchronized (replyLock) {
							if (!replies.isEmpty()) {
								if (XTStrace.bGlobalVerboseEnabled) 
									XTStrace.verbose("Replies found " + replies.size());
								if (replies.size() > 0) {
									Object[] keys = replies.keySet().toArray();
									// replies.getkeys();
									for (int i = keys.length - 1; i > -1; i--) {
										XTS x = (XTS) replies.get(keys[i]);
										if (x != null && conn == x.connection) {
											replies.remove(keys[i]);
											// We remove the target/conn Only if there were outstanding replies for it. If not, then the
											// next attempt to send will throw and exception thus notifying the client/serverof the connection failure.
											if (remove) {
												remove = false;
												Target.remove(conn); // remove from named targets
												// connections
												// .remove(((IConnection) conn)
												// .getRegisterName());
											}
											if (x.getWaitFor() instanceof Message) {
												String error = "Reply removed by Disconnect";
												error += ":" + msg;
												x.fail(error, (Message) x.getWaitFor());
											} else {
												x.fail(msg, null);
											}
										}
									}
								}
							}
						} finally {
							replyLock.unlock();
						}
					}
			}
		}
		else {
			Target.remove(delConn);
		}
		if (state == CLIENT) {
			free(); // generated by client connect
		}
	}

	// -----------------------------------------------------------------------
	/** Interface method for driver. Called by the driver when disconnected. **/
	// -----------------------------------------------------------------------
	public void connectionLost(final IConnection conn, final Object uservalue) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Connection lost:" + conn);
	}

	public final void incIndex() {
		index = gIndex.incrementAndGet();
	}

	// -----------------------------------------------------------------------
	/** Interface method for driver, called by the driver when data received. **/
	// -----------------------------------------------------------------------
	public void received (final IConnection conn, final Message message, final Object uservalue) {
		IXTSreceiver rcb = null;
		Object rxuserval = null;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(">>>>received: conn " + message.target + " " + this);
		if (message.target >= 0) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Search server " + message.target);
			Server server = Server.getByID(message.target);
			if (server != null) {
				rcb = server.rcb;
				rxuserval = server.uservalue;
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("target=" + server.target + " server name=" + server.targname	+ " server rcb=" + server.rcb);
			} else {
				rcb = this.rcb;
				rxuserval = this.rxuserval;
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("No server found, set " + rxuserval);
			}
		} else {
			rcb = this.rcb;
			rxuserval = this.rxuserval;
		}
		Message.dump("rcb:" + rcb + " isWaiters:" + isWaiters + " Received", "received", message);
		if (message.target < 0) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Route return " + this);
			if (message.target == ROUTE_RETURN) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Calling routeReturn for TargetId=0x" + String.format("%x", message.target));
				routeReturn(conn, message);
			} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Calling controlMsg for TargetId=0x" + String.format("%x", message.target));
				controlMsg(conn, message);
			}
		} else {
			if (message.length == 0) {
				Server.chirp_all(); // forced chirp
			} else {
				if (rcb != null) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("callback for incoming " + rcb);
					Token t = Token.newToken(conn, message.token, message.route);
					if (isWaiters) {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("waiters found " + this);
						t.rcb = rcb;
						t.userval = rxuserval;
						t.msg = message;
						t.next = null;
						synchronized (msgQ) {
							if (msgQ[1] != null) {
								msgQ[1].next = t;
							} else {
								msgQ[0] = t;
							}
							msgQ[1] = t;
							if (waiters > 0) {
								msgQ.notify();
							}
						}
					} else {
						if (XTStrace.bGlobalVerboseEnabled) {
							XTStrace.verbose("no waiters callback receiver called " + message.route + " " + getWaitFor() + " " + this);
							if (rcb instanceof XTS) {
								XTStrace.verbose("rcb=" + rcb);
								XTStrace.verbose("wait_for rcb.wait_for=" + ((XTS) rcb).getWaitFor());
							}
						}
//						if (XTS.checkMsgFreed) {
//							if (message.isFreed()) {
//								if (XTStrace.bGlobalErrorEnabled) 
//									XTStrace.error("Got freed message in received xx");
//							}
//						}
						if (message.route != 0) {
							rcb.received(message, rxuserval, t);
						} else {
							rcb.received(message, rxuserval, null);
							t.free();
						}
					}
				} else {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("no callback for incoming " + getWaitFor());
					message.freeMessage("received no callback");
				}
			}
		}
	}

	// -----------------------------------------------------------------------
	/**                 routeReturn.                                       **/
	// -----------------------------------------------------------------------
	private final void routeReturn (final IConnection conn, final Message message) {
		String targetName = null;
		XTS state = null;
		replyLock.lock();
		try {
			// synchronized (replyLock) {
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.debug("Return reply remove " + message.route);
			state = (XTS) replies.remove(message.route);
		} finally {
			replyLock.unlock();
		}
		if (state == null) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("unmatched reply " + " Message Number="	+ message.msgno + " Route=" + message.route + " " + conn.toString());
			/*
			 * System.err.println("input replies " + repliesb.toString());
			 * StringBuffer sb = XTStrace.dumpToStringBuffer("Unmatched Reply",
			 * "route_return", message.body, message.length, true);
			 * System.err.println(sb.toString());
			 */
			replyLock.lock();
			try {
				// synchronized (replyLock) {
				if (replies.size() == 0) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("No reply entries available");
				} else {
					StringBuffer sb = new StringBuffer();
					sb.append("Reply entries available:");
					for (int r : replies.keySet()) 
						sb.append(r + " ");
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error(sb.toString());
				}
			} finally {
				replyLock.unlock();
			}
			message.freeMessage("route_return state null");
			// throw new Error("unmatched reply");
		} else {
			if (state.getWaitFor() instanceof Message) {
				Message m = (Message) state.getWaitFor();
				synchronized (m) {
					m.freeMessage("route_return message");
				}
			}
			if (state.rcb != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Route callback " + state.rcb);
				if (message.target == BIND_CLIENT) {
					long startTime = message.getLong();
					try {
						targetName = new String(message.body, 8, (message.length - 8), "UTF8");
						// new String(message.getBytes(message.length-8));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					// if (traceMessages)
					// XTStrace.verbose("Received BIND_CLIENT " + targetName);
					new Server(0, targetName, 300, startTime, state.rcb, state.rxuserval);
					XTS x = newXTS(CLIENT, state.rcb, state.rxuserval, null);
					conn.receive(x, x); // set unsolicited address
				}
				state.rcb.received(message, state.rxuserval, null);
			} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("no callback for reply ");
				message.freeMessage("route_return callback");
			}
			/**
			 * We need to synchronize on the XTS object. This same object
			 * will be sync'd on during transmission. On a multiprocessor 
			 * the receive completed and free'd this object before
			 * the send completed all of it's processing. Once freed 
			 * the object could be reallocated. When reallocated 
			 * the rcb field may be initialized to null before the send 
			 * logic checks this field. If the field is null, the send 
			 * logic free's the XTS object. This causes the same object 
			 * to be free'd twice. 
			 */ 
			synchronized (state) {
				state.free();
			}
		}
	}

	// -----------------------------------------------------------------------
	/**                 controlMsg                                         **/
	// -----------------------------------------------------------------------
	private final void controlMsg (final IConnection conn, final Message message) {
		String name = null;
		boolean freeControlMsg = true;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("controlMsg TargetId=0x" + String.format("%x", message.target));
		switch (message.target) {
		case RESOLVE_TARGET_NAME_REPLY:
			/**
			 * RGHADA-3040
			 * Check to see if this is a reply to an outstanding message request
			 * instead of a Control message resolve target request.
			 * If it is, an xts error needs to be returned to the caller indicating
			 * target could not be resolved.
			 * What should happen is the message reply should contain a rsp148 and
			 * some sub-code, but according to Mihai it would be to hard to change
			 * the code because it is embedded in WCP.
			 */
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("controlMsg RESOLVE_TARGET_NAME_REPLY");
			int target = 0;
			XTS state = null;
			replyLock.lock();
			try {
				if (XTStrace.bGlobalDebugEnabled) 
					XTStrace.debug("Return reply route=" + message.route);
				state = (XTS) replies.remove(message.route);
				if (state == null) {
					XTSurl url = conn.getUrl();
					if (url != null) {
						String targ = null;
						if (url.partition != null)
							targ = url.partition + url.target;
						else
							targ = url.target;
						DefaultDirectory.deleteTargetFromCache(targ);
					}
				}


			} finally {
				replyLock.unlock();
			}
			if (state != null) {
				XTSurl url = conn.getUrl();
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("ControlMsg state non null Url=" + url);
				if (url != null) {
					String targ = null;
					if (url.partition != null)
						targ = url.partition + url.target;
					else
						targ = url.target;
					DefaultDirectory.deleteTargetFromCache(targ);
				}
				if (state.getWaitFor() instanceof Message) {
					Message m = (Message) state.getWaitFor();
					synchronized (m) {
						m.freeMessage("route_return message");
					}
				}
				if (state.rcb != null) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Route callback " + state.rcb + " unresolved target");
					/**
					 * Indicate the target was unresolved.
					 * Do not free control message. It will be freed
					 */
					message.target = XTS.UNRESOLVED_TARGET;
					state.rcb.received (message, state.rxuserval, null);
					freeControlMsg = false;
				} else {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("No callback for reply ");
					freeControlMsg = false;
					message.freeMessage("route_return callback");
				}
				/**
				 * We need to synchronize on the XTS object. This same object
				 * will be sync'd on during transmission. On a multiprocessor 
				 * the receive completed and free'd this object before
				 * the send completed all of it's processing. Once freed 
				 * the object could be reallocated. When reallocated 
				 * the rcb field may be initialized to null before the send 
				 * logic checks this field. If the field is null, the send 
				 * logic free's the XTS object. This causes the same object 
				 * to be free'd twice. 
				 */ 
				synchronized (state) {
					state.free();
				}
			} else {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("ControlMsg original logic");
				/**
				 * Original logic before adding the code preceding this.
				 */
				try {
					name = new String(message.body, 4, message.length - 4, "UTF8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				target = message.body[0] << 24
						| ((message.body[1] & 0xff) << 16)
						| ((message.body[2] & 0xff) << 8)
						| (message.body[3] & 0xff);
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("controlMsg RESOLVE_TARGET_NAME_REPLY TargetName=" + name + " TargetId=0x" + String.format("%x", target));
				Target pt = Target.getByConn(conn);
				if (pt != null) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("controlMsg Get target by connection; TargetName=" + pt.getTargname() + " AliasName=" + pt.getAliasname());
					pt.setTarget(target);
				}
				if (name.equalsIgnoreCase(pt.getTargname())) {
					ResolveTargetObj ResObj = resname.remove(name);
					if (ResObj != null) {
						signal(ResObj.Obj);
					} else {
						e.printStackTrace();
						return;
					}
					ResObj.Error = 0;
				} else {
					ResolveTargetObj ResObj = resname.remove(pt.getTargname());
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("controlMsg RESOLVE_TARGET_NAME_REPLY Wrong TargetName=" + name + " reply; expected=" + pt.getTargname());
					message.target = XTS.UNRESOLVED_TARGET;
					ResObj.Error = XTSException.XTS_TARGET_CANNOT_BE_RESOLVED;
					if (ResObj != null) {
						signal(ResObj.Obj);
					} else {
						e.printStackTrace();
						return;
					}
				}
			}
			break;
		case REGISTER_PROXY:
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("controlMsg REGISTER_PROXY");
			proxies.put(conn, conn);
			Server.chirp_all();
			break;
		case RESOLVE_TARGET_NAME:
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("controlMsg RESOLVE_TARGET_NAME");
			// name=new String(message.body,0,message.length);
			try {
				name = new String(message.body, 0, message.length, "UTF8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			Message p = Message.newMessage(message.length + 4);
			p.route = message.route;
			p.ttl = ttl;
			p.priority = priority;
			p.target = RESOLVE_TARGET_NAME_REPLY;
			System.arraycopy(message.body, 0, p.body, 4, message.length);
			Server ps = Server.getByName(name);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Resolve name for " + name + " returns " + ps);
			if (ps != null) {
				target = ps.target;
			} else {
				target = -1;
			}
			p.body[0] = (byte) (target >> 24);
			p.body[1] = (byte) (target >> 16);
			p.body[2] = (byte) (target >> 8);
			p.body[3] = (byte) target;
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Send Resolve target reply " + p.toString());
			Message.dump("Send Resolve target reply " + conn, "control_msg", p);
			conn.send(p, null, null);
			break;
		case CHIRP:
		default:
			Message.dump("Unknown Message", "controlMessage method", message);
		}
		
		if (freeControlMsg)
			message.freeMessage("control_msg");
	}

	static final void chirp(final Server server) {
		Message p;
		byte[] targetNameUTF8 = null;
		if (!proxies.isEmpty()) {
			if (server.targname != null) {
				try {
					targetNameUTF8 = server.targname.getBytes("UTF8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return;
				}
				p = Message.newMessage(18 + targetNameUTF8.length);
			} else {
				p = Message.newMessage(16);
			}

			p.target = CHIRP;
			p.route = server.target;
			p.timeout = server.chirpInterval;
			p.ttl = ttl;
			p.priority = priority;

			p.putLong(server.startTime); // starting time
			p.putInt(server.isReplicate ? 0 : (int) System.currentTimeMillis());
			p.putInt(0); // flags - for future use
			if (server.targname != null) {
				p.putShort(targetNameUTF8.length + 2);
				p.putBytes(targetNameUTF8);
				// { p.putShort(server.targname.length()+2);
				// p.putBytes(server.targname.getBytes());
			}

			Enumeration<Object> e = proxies.elements();
			while (e.hasMoreElements()) {
				IConnection c = (IConnection) e.nextElement();
				// tjv if(traceMessages) trace("Send chirp "+p.toString());
				// Send chirps to proxies that this server connected to [0035]
				// or that connected to a listen port defined for this server.
				Enumeration<?> serverEnum = server.getChirpUrls();
				while (serverEnum.hasMoreElements()) {
					if ((XTSurl) serverEnum.nextElement() == c.getStatus().driverUrl) {
						Message q = p.cloneMessage();
						if (XTStrace.bGlobalDebugEnabled) {
							Message.dump("Send chirp" + c, "chirp", q);
						}
						if (c.send(q, null, null)) {
							q.freeMessage("chirp send");
						}
					}
				}
			}

			p.freeMessage("chirp");
		}
	}

	/**
	 * Check for an existing Connection/Listen with this Protocol/Host/Port
	 * Combination.
	 * 
	 * @param x
	 *            the url used to check for an existing connection/Listen.
	 * @return Driver object if an existing Connection/Listen exists, otherwise
	 *         null.
	 **/
	static final IDriver existingConnectionListen(final XTSurl x) {
		Enumeration<?> dconnEnum = dconn.elements();
		while (dconnEnum.hasMoreElements()) {
			IDriver dr = (IDriver) dconnEnum.nextElement();
			try {
				if (x.getProtocol().equalsIgnoreCase(dr.getStatus().url.getProtocol())) {
					if (InetAddress.getByName(x.getHost()).equals(InetAddress.getByName(dr.getStatus().url.getHost()))) {
						if (x.getPort() == dr.getStatus().url.getPort()) {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("A Connection/Listen exists to/for " + x.toString());
							return dr;
						}
					}
				}
			} catch (UnknownHostException e) {
				if (x.getHost().equals(dr.getStatus().url.getHost())) {
					if (x.getPort() == dr.getStatus().url.getPort()) {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("A Connection/Listen exists to/for " + x.toString());
						return dr;
					}
				}
			}
		}
		return null;
	}

	// --------------------------------transmitted---------------------------------------
	/** Interface method for driver, called by the driver when data transmitted. **/
	// -----------------------------------------------------------------------
	public void transmitted (final IConnection conn, final Message p, final Object uservalue) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("transmitted:Send complete " + p.msgno + " " + toString() + " tcb:" + tcb);
		if (XTStrace.bGlobalVerboseEnabled) {
			if (rcb instanceof XTS) {
				XTStrace.verbose("transmitted:wait_for rcb.wait_for=" + ((XTS) rcb).getWaitFor());
			}
		}
		if (tcb != null) {
			if (tcb.sendComplete(p, txuserval)) {
				conn.close();
			}
		}
		if (XTStrace.bGlobalVerboseEnabled) {
			if (rcb instanceof XTS) {
				XTStrace.verbose("transmitted:wait_for rcb.wait_for=" + ((XTS) rcb).getWaitFor());
			}
		}
		tcb = null;
		if (rcb == null) {
			free(); // no reply expected
		}
	}

	// -----------------------------transmitFailed------------------------------------------
	/**
	 * Interface method for driver, called by the driver when transmission has
	 * failed.
	 **/
	// -----------------------------------------------------------------------
	public void transmitFailed(final IConnection conn, final Message p, final Object uservalue) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("transmitFailed " + p.msgno);
		if (tcb != null) {
			tcb.sendFailed(p, txuserval);
		}
		if (rcb != null) {
			replyLock.lock();
			try {
				// synchronized (replyLock) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Failed transmit remove reply " + p.route);
				if ((replies.remove(p.route)) != null) {
					rcb.receiveFailed(p.cloneMessage(), rxuserval);
					// replies.remove(new Integer(p.route));
				}
			} finally {
				replyLock.unlock();
			}
		} else {
			p.freeMessage("transmitFailed");
		}
	}

	/**                        ========xtsError==========
	 * This method is called when there is no XTS object available 
	 * @param msg
	 * @param xtsResponseCode
	 * @throws XTSException
	 */
	private static final void xtsError(final String msg, final int xtsResponseCode) throws XTSException {

		if (xtsResponseCode == 0) {
			if (msg != null) {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error ("XTS error: " + msg);					
				throw new XTSException(msg, XTSException.XTS_UNKNOWN_EXCEPTION);
			}
			else {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error ("XTS error: " + "Response code = " +Integer.toString(XTSException.XTS_UNKNOWN_EXCEPTION));
				throw new XTSException(XTSException.XTS_UNKNOWN_EXCEPTION);
			}
		}
		throw new XTSException(msg, xtsResponseCode);
	}	

	/**              ==========xtsError=========
	 * This method is called when there is an XTS object available 
	 * @param state
	 * @param msg
	 * @throws XTSException
	 */
	private static final void xtsError (final XTS state, final String msg) throws XTSException {
		
		Exception e = null;
		if (state == null) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("XTS error: XTS state is null");
			return;
		}
		
		int xtsResponseCode = state.xtsResponseCode;
		if (state.e != null) {
			e = state.e;
			state.free();
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("XTS error: " + e.getMessage());
			if (e.getClass().equals(XTSException.class)) {
				throw (XTSException)e; 
			}
			else {
				if (msg != null) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("XTS error: " + msg);					
					throw new XTSException(msg, XTSException.XTS_DISCONNECT);
				}
				else {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("XTS error: " + "Response code = " + Integer.toString(XTSException.XTS_DISCONNECT));
						throw new XTSException(XTSException.XTS_DISCONNECT);
				}
			}
		}
		else {
			state.free();
		}
		if (xtsResponseCode == 0) {
			xtsResponseCode = XTSException.XTS_UNKNOWN_EXCEPTION;
			if (msg != null) {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error("XTS error: " + msg);					
				throw new XTSException(msg, XTSException.XTS_UNKNOWN_EXCEPTION);
			}
			else {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error("XTS error: " + "Response code = " +Integer.toString(XTSException.XTS_UNKNOWN_EXCEPTION));
				throw new XTSException(XTSException.XTS_UNKNOWN_EXCEPTION);
			}
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("XTS error: " + "Response code = " +Integer.toString(xtsResponseCode));
		throw new XTSException(xtsResponseCode); 
	}

	// -----------------------------------------------------------------------
	// XTStransmitter and XTSreceiver interface methods
	// used for sendAndWait() methods
	// -----------------------------------------------------------------------

	// -----------------------------------------------------------------------
	/** Interface method. Called by the XTS when transmission complete. **/
	// -----------------------------------------------------------------------
	public boolean sendComplete(final Message p, final Object uservalue) {
		return false;
	}

	// -----------------------------------------------------------------------
	/** Interface method. Called by the XTS when transmission fails. **/
	// -----------------------------------------------------------------------
	public void sendFailed(final Message p, final Object uservalue) {
		
		rxuserval = "Send failed";
		Object x = getWaitFor();
		checkSignalObject();
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("XTS received failed " + this + " prepare signal "+ x);
		if ((x != null) && (x instanceof XTS)) {
			signal(x);
		} else {
			signal(this);
		}
	}

	private void checkSignalObject() {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("XTS " + this + " send signal " + getWaitFor());
		/*
			* try { int max=10; while (!(getWait_for() instanceof XTS)) {
			* Thread.sleep(100); if (max < 0) break; max--;
			* System.out.println(XTStrace
			* .getDateString()+" "+Thread.currentThread()
			* .getName()+" XTS "+this+" send signal "+getWait_for()); } } catch
			* (InterruptedException e) { }
		*/
	}

	// -----------------------------------------------------------------------
	/** Interface method, called by XTS when data received. **/
	// -----------------------------------------------------------------------
	public void received(final Message p, final Object uservalue, final Token token) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("received " + p + " " + getWaitFor());
		Object x = getWaitFor();
		rxuserval = p;
		
		checkSignalObject();
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("XTS received " + this + " prepare signal " + x);
//		if (XTS.checkMsgFreed) {
//			if (p.isFreed()) {
//				if (XTStrace.bGlobalErrorEnabled) 
//					XTStrace.error("Got freed message in received");
//			}
//		}
		if ((x instanceof XTS) && (noClient)) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("Data received with no client");
			Thread.dumpStack();
		}

		if ((x != null) && (x instanceof XTS)) {
			signal(x);
		} else {
			signal(this);
		}
	}

	// -----------------------------------------------------------------------
	/** Interface method, called by XTS when reception fails. **/
	// -----------------------------------------------------------------------
	public void receiveFailed(final Message p, final Object uservalue) {
		if (XTStrace.bGlobalErrorEnabled) 
			XTStrace.error("received failed " + p);
		rxuserval = "Receive failed";
		Object x = getWaitFor();
		checkSignalObject();
		if (XTStrace.bGlobalErrorEnabled) 
			XTStrace.error("XTS received failed " + this + " prepare signal " + x);
		if ((x != null) && (x instanceof XTS)) {
			signal(x);
		} else {
			signal(this);
		}
	}

	// -----------------------------------------------------------------------
	/** Sets the Close trace file flag and invokes main shutdown method . **/
	// -----------------------------------------------------------------------
	public static final void shutdown(final boolean closeLog) {
		closeTraceFile = closeLog;
		shutdown();
	}

	// -----------------------------------------------------------------------
	/** Shut down XTS. Shuts down all connections and cleans up. **/
	// -----------------------------------------------------------------------
	public static final void shutdown() {
		running = false;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Shutdown Requested");
		synchronized (shutLock) {
			Enumeration<IDriver> de = dconn.elements();
			// Go thru Connections initiated as a server - rxc
			while (de.hasMoreElements()) {
				de.nextElement().stopit();
				try {
					shutLock.wait(2500);
				} catch (InterruptedException ie) {
				}
			}
			// int keys[]=connections.getkeys(); [0036]
			// Go thru Connections initiated as a client - rxc
			// for(int i=0;i<keys.length;i++) [0036]
			// { Connection conn=(Connection)connections.get(keys[i]);
			// if(conn!=null)
			Enumeration<ArrayList<IConnection>> e = urls.elements();
			while (e.hasMoreElements()) {
				ArrayList<IConnection> connList = (ArrayList<IConnection>) e.nextElement();
				for (IConnection conn : connList) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Shutdown..closing connection=" + conn);
					conn.close();
				}
				try {
					shutLock.wait(2500);
				} catch (InterruptedException ie) {
				}
			}
		}
		dconn.removeAllElements();
		// connections.clear();
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Clear urls");
		urls.clear();
		replyLock.lock();
		try {
			// synchronized (replyLock) {
			replies.clear();
		} finally {
			replyLock.unlock();
		}
		proxies.clear();
		resname.clear();
		while (waiters > 0) {
			signal(msgQ);
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Delete all servers");
		Server.deleteAll();
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Delete all targets");
		Target.removeAll();
		closeTraceFile = true; // reset the default to close.
		// System.gc(); // invoke garbage collector - rxc rxc rxc rxc
	}

	// -----------------------------------------------------------------------
	// signal another process
	// -----------------------------------------------------------------------
	private static final void signal(final Object o) {
// mihai reduce trace	XTStrace.verbose("Send signal to " + o);
		synchronized (o) {
			o.notify();
		}
	}

	// -----------------------------------------------------------------------
	// Tracing
	// -----------------------------------------------------------------------
	static Object[] head = { null, null };
	static Object[] tail = head;
	static char[] hextab = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F' };

	/** Return the hex value of an integer. **/
	public static final String hexof(final int v) {
		int x = v;
		char[] c = new char[8];
		for (int i = 7; i > -1; i--) {
			c[i] = hextab[x & 0x0f];
			x >>= 4;
		}
		return new String(c);
	}

	/** Return the hex value of a long. **/
	public static final String hexof(final long v) {
		long x = v;
		char[] c = new char[16];
		for (int i = 15; i > -1; i--) {
			c[i] = hextab[(int) x & 0x0f];
			x >>= 4;
		}
		return new String(c);
	}

	public final Object getWaitFor() {
// mihai reduce trace	XTStrace.verbose("XTS " + this + " get wait_for " + waitFor);
		return waitFor;
	}

	public final void setWaitFor(final Object wait_for) {
// mihai reduce trace		XTStrace.verbose("XTS " + this + " set wait_for " + wait_for);
		this.waitFor = wait_for;
	}

	public final IConnection getConnection() {
		return connection;
	}

	public final void setConnection(final IConnection connection) {
		this.connection = connection;
	}

}
