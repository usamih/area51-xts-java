/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.directory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTSException;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;

/**
 * Default Directory service - set by XTS unless changed. This service provides
 * a rudimentary service - the user can call the <i>add</i> method to specify
 * the access point. If the <i>add</i> was not performed, then all requests are
 * forwarded to the Directory Service process for resolution.
 ** 
 * @version 2.1.1.6
 **/
public class DefaultDirectory extends Directory {
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	private static Hashtable<String, Hashtable<String, Vector<XTSurl>>> partTable;
	private static DefaultDirectory lastInstance = null;
	private static boolean serviceSet = false;
	private static boolean useCache = false;
	private static boolean firstAdd = true;
	private static Hashtable<String, Vector<XTSurl>> urls; // current url list
	
	public static String xtsdsurl = null; // Home of the DirServer.

	private int XTSDSport;
	private static String XTSDShost;

	private static final int defaultPort = 4952;
	// -----------------------------------------------------------------------
	// user Defined System Properties.
	// -----------------------------------------------------------------------
	public static final String XTSDSURL = "XTSDSURL"; // Property name

	/**
	 * XTSurl cache used to improve performance by reducing the number of directory
	 * server requests.
	 * The 1st time a target is requested the XTSurl(s) are obtained from the
	 * directory server and stored in this cache. Subsequent calls for the same
	 * target can be resolved from the cache.
	 */
	private static ConcurrentHashMap<String, XTSurl[]> xtsUrlCache = new ConcurrentHashMap<String, XTSurl[]>(100);	

	/** set Partition. **/
	// overrides Directory
	public synchronized void setPartition(final String newPartition) {
		String partition = newPartition;
		if (partition == null) {
			partition = ""; // null = ""
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[SETPARTITION] partition=" + partition);
		super.setPartition(partition); // set the actual var

		if (!serviceSet) {
			return;
		}

		if (partTable == null) {
			partTable = new Hashtable<String, Hashtable<String, Vector<XTSurl>>>();
			// Note add code does not use partTable is serviceSet
			// Question is should it?
		}

		urls = (Hashtable<String, Vector<XTSurl>>) partTable.get(partition);
		if (urls == null) {
			urls = new Hashtable<String, Vector<XTSurl>>();
			partTable.put(partition, urls); // ensure always a partition
		}
	}

	/** set to use xtsdsurl **/
	public void setXtsDsUrl (String xtsdsurl) {
		this.xtsdsurl = xtsdsurl;
	}
	/** set to use local in-memory service **/

	public static void setLocalService(final boolean yesOrNo) {
		serviceSet = yesOrNo;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("setLocalService=" + yesOrNo);
	}

	/** create new instance **/
	public DefaultDirectory() {
		lastInstance = this;
	} // use last instance for setService

	/**
	 * set host name, protocol and port number using a XTSurl
	 ** 
	 * @param url
	 *            the URL where the service access point is. The well-known port
	 *            number is obtained from the URL, if it is present.
	 ** @deprecated - Use the add method.
	 **/
	public static void setService(final XTSurl setUrl) {
		XTSurl url = setUrl;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[SETSERVICE] Target=" + url.getTarget() + " URL=" + url.toString());
		setLocalService(true); // we use local service from now on

		urls = new Hashtable<String, Vector<XTSurl>>();
		url = new XTSurl("*", url);
		try {
			lastInstance.add(XTS.XTSACCESS, url);
			lastInstance.add(XTS.XTSLISTEN, url);
			lastInstance.add(XTS.XTSCONNECT, url);
			lastInstance.delete(XTS.XTSCONNECT, url);
			// zero-length entry
		} catch (Exception ex) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(ex);
		}
		firstAdd = true;
	}
	
	/**
	 * Retrieve a list of URLs from the directory.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target.
	 ** @param hostname
	 *            the hostName:port.
	 ** @return an array of URLs or null.
	 * @throws Exception
	 *             Error retrieving list (Caused by I/O error)
	 **/
	public final XTSurl[] retrieve(final String qualifier, final String target, final String hostName) throws Exception {
		String host;
		int i, port;
		if (XTStrace.bGlobalVerboseEnabled) {
			if (hostName == null)
				XTStrace.verbose("[RETRIEVE] does not use HostName");
			else
				XTStrace.verbose("[RETRIEVE] By HostName=" + hostName);
		}
		XTSurl[] urls = retrieve(qualifier, target);
		if (hostName == null)
			return urls;	
		host = hostName.trim();
		port = 0;
		i = hostName.indexOf(":");
		if (i != -1) {
			try {
				port = Integer.parseInt(host.substring(i + 1));
			} catch (NumberFormatException ne) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Port in hostname= " + hostName + " is not numeric");
				throw new XTSException("Port in hostname= " + hostName + " is not numeric", XTSException.XTS_INVALID_PORT);
			}
			if (port <  0 || port > 65353) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Port value outside of bounds (0-65353) in hostname= " + hostName);
				throw new XTSException("Port value outside of bounds (0-65353) in hostname= " + hostName, XTSException.XTS_INVALID_PORT);
			}								
			host = host.substring(0, i);
		}
		
		if (XTStrace.bGlobalVerboseEnabled) {
			if (port > 0)
			     XTStrace.verbose("[RETRIEVE] by Host=" + host + " Port=" + port);	
			else
			     XTStrace.verbose("[RETRIEVE] by Host=" + host);	
		}					
		if (urls != null) {
			List<XTSurl> urlList = new ArrayList<XTSurl>();
			for (i = 0; i < urls.length; i++) {
				if (host.equalsIgnoreCase(urls[i].getHost())) {
					if (port > 0) {
						if (port == urls[i].getPort()) {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("[RETRIEVE] URL=" + urls[i]);						
							urlList.add(urls[i]);
						}
					}
					else {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("[RETRIEVE] URL=" + urls[i]);						
						urlList.add(urls[i]);
					}	
				}
			}
			urls = null;
			if (!urlList.isEmpty()) {
				urls = new XTSurl[urlList.size()];
				for(i = 0; i <urlList.size(); i++) {
					urls[i] = urlList.get(i);
				}
			}
		}
		return urls;
	}

	/**
	 * Retrieve a list of URLs from the directory.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target.
	 **/
	public final XTSurl[] retrieve(final String qualifier, final String target) throws Exception {

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[RETRIEVE] Partition=" + partition + " Target=" + target + " Qualifier=" + qualifier);
		if (!serviceSet) {			
			XTSurl[] urls = null;
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Send to directory server");		
			urls = sendToDirectoryServer("retrieve", null, qualifier, target, "");
			return urls;
		}

		Vector<XTSurl> result = null;

		if (qualifier.indexOf('*') != -1) {
			char[] c = qualifier.toCharArray(); // make search quicker
			Enumeration<String> e = urls.keys(); // vectors
			while (e.hasMoreElements()) {
				String s = (String) e.nextElement(); // this will be a
				// qualifier
				if (wildMatch(c, s)) {// does the qualifer match?

					XTSurl[] x = retrieve(s, target); // recursive call
					if (x == null) {
						return null;
					}
					if (result == null && x.length > 0) {
						result = new Vector<XTSurl>(); // do proper null
					}
					// processing
					for (int i = 0; i < x.length; i++) {
						result.addElement(x[i]);
					}
				} // elements added to result
			}
		} else {
			Vector<XTSurl> v = urls.get(qualifier);
			if (v != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose(v.size() + " qualifier found with " + qualifier);
				if (result == null) {
					result = new Vector<XTSurl>();
				}
				char[] c = target.toCharArray();
				Enumeration<XTSurl> e = v.elements();
				while (e.hasMoreElements()) {
					XTSurl x = (XTSurl) e.nextElement();
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Got x.target=" + x.target + " " + x.toString());
					if (x.target.equals("*")) {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Add Element with target " + x.toString());
						result.addElement(new XTSurl(target, x));
					} else if (wildMatch(c, x.target)) {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Add Element " + x.toString());
						result.addElement(x);
					}
				}
			}
		}
		if (result == null) {
			return null;
		}
		int rlen = result.size();
		if (rlen == 0) {
			return null; // Return NULL if nothing was found.
		}
		XTSurl[] ur = new XTSurl[rlen];
		result.copyInto(ur); // copy results into the proper array

		return ur;
	}

	/**
	 * Retrieve Adi parameters.
	 ** 
	 **/
	public final XTSurl[] adiquery() throws Exception {

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[QUERY] ADI parameters");
		XTSurl[] urls = null;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Send query to directory server");		
		urls = sendToDirectoryServer("dsquery", null, null, null, "");		
		return urls;
	}

	// match wild card, yes or no

	private static final boolean wildMatch(final char[] c, final String s) {
		return wildMatch(c, 0, s.toCharArray(), 0);
	}

	private static final boolean wildMatch(final char[] c, final int cpos, final char[] x, final int xpos) {

		int i = 0;
		int j = c.length - cpos; // length left in c
		int k = x.length - xpos; // length left in x
		boolean equalLength = j == k; // equal length left
		if (j > k) {
			j = k; // take shorter of the two
		}
		while (i < j) {
			char z = c[cpos + i];
			switch (z) {
			case '*': // wild of any length
				j = xpos + i; // start here in match
				i = cpos + i + 1; // over *
				if (i == c.length) {
					return true; // only '*' left
				}
				while (j < x.length && !wildMatch(c, i, x, j)) {
					j++;
				}
				return j < x.length;
			case '?': // wild, length 1
				i++; // yes, bypass then
				break;
			default:
				if (z != x[xpos + i]) {
					return false;
				}
				i++;
			}
		}
		return equalLength; // it matches on whole length
	}

	private boolean checkQualifier(final String qualifier) {
		boolean rc = true;
		if (qualifier != null) {
			if (qualifier.equalsIgnoreCase(XTS.XTSACCESS)) {
				rc = false;
			}
			if (qualifier.equalsIgnoreCase(XTS.XTSLISTEN)) {
				rc = false;
			}
			if (qualifier.equalsIgnoreCase(XTS.XTSCONNECT)) {
				rc = false;
			}
		}
		return rc;
	}

	/**
	 * Add an entry.
	 ** 
	 ** @param qualifier: a string that qualifies the entry.
	 ** @param url: the url to add.
	 ** @throws NoTargetException, InvalidQualifierException, and NullUrlException.
	 **/
	public final void add (final String qualifier, final XTSurl url) throws Exception {

		if (XTStrace.bGlobalVerboseEnabled) {
			if ((partition == null) || (partition.length() < 1))
				XTStrace.verbose("[ADD] Target=" + url.getTarget() + " Qualifier=" + qualifier + " URL=" + url.toString());
			else
				XTStrace.verbose("[ADD] Partition=" + partition + " Target=" + url.getTarget() + " Qualifier=" + qualifier + " URL=" + url.toString());
		}

		if (checkQualifier(qualifier)) {
			throw new XTSException("Directory invalid qualifier = " + qualifier, XTSException.XTS_DIR_INVALID_QUALIFIER);
		}

		if (url == null) {
			throw new XTSException (XTSException.XTS_NULL_URL);
		}

		if (url.target == null || url.target.length() < 1) {
			throw new XTSException(XTSException.XTS_URL_MISSING_TARGET);
		}
		if (!serviceSet) {			
			/** * Add Url for the partition/target in question. **/
			sendToDirectoryServer("add", null, qualifier, url.getTarget(),	url.toString(true));
                        return;
		}

		if (firstAdd) {// first add after setService?

			partTable = new Hashtable<String, Hashtable<String, Vector<XTSurl>>>();
			urls = new Hashtable<String, Vector<XTSurl>>(); // yes, remove all
			// entries
			firstAdd = false;
		}

		Vector<XTSurl> v = (Vector<XTSurl>) urls.get(qualifier);
		if (v == null) {
			v = new Vector<XTSurl>();
			urls.put(qualifier, v);
		}
		v.addElement(url);
                return;
	}

	/**
	 * Delete an entry.
	 ** 
	 ** @param qualifier: a string that qualifies the entry.
	 ** @throws NoTargetException, InvalidQualifierException, and NullUrlException.
	 **/
	public final void delete(final String qualifier, final XTSurl url) throws Exception {

		if (XTStrace.bGlobalVerboseEnabled)  {
			if (partition == null || partition.length() < 1)
				XTStrace.verbose("[DELETE] Target="	+ url.getTarget() + " Qualifier=" + qualifier + " URL=" + url.toString());
			else
				XTStrace.verbose("[DELETE] Partition=" + partition + " Target="	+ url.getTarget() + " Qualifier=" + qualifier + " URL=" + url.toString());
		}
		if (checkQualifier(qualifier)) {
			if (XTStrace.bGlobalVerboseEnabled)  
				XTStrace.verbose("[DELETE]  qualifier = " + qualifier + " is invalid");
			throw new XTSException("[DELETE]  qualifier = " + qualifier + " is invalid", XTSException.XTS_DIR_INVALID_QUALIFIER);
		}

		if (url == null) {
			if (XTStrace.bGlobalVerboseEnabled)  
				XTStrace.verbose("[DELETE] url is null");
			throw new XTSException ("[DELETE] url is null", XTSException.XTS_NULL_URL);
		}
		if (url.target == null || url.target.length() < 1) {
			if (XTStrace.bGlobalVerboseEnabled)  
				XTStrace.verbose("[DELETE] target is missing");
			throw new XTSException("[DELETE] target is missing", XTSException.XTS_URL_MISSING_TARGET);
		}

		if (!serviceSet) {
			sendToDirectoryServer("delete", null, qualifier, url.getTarget(), url.toString(true));
			if (XTStrace.bGlobalVerboseEnabled)  
				XTStrace.verbose("[DELETE] exit from delete");
			return;
		}

		if (qualifier.indexOf('*') != -1) {// wild card in qualifier
			char[] c = qualifier.toCharArray(); // make search quicker
			Enumeration<String> e = urls.keys(); // vectors
			while (e.hasMoreElements()) {
				String s = (String) e.nextElement(); // this will be a
				// qualifier
				if (wildMatch(c, s)) {
					delete(s, url); // recursive call
				}
			}
		} else {
			Vector<XTSurl> v = (Vector<XTSurl>) urls.get(qualifier);
			if (v == null) {
				return;
			}
			char[] c = url.target.toCharArray();
			char[] u = url.toString(true).toCharArray();
			Enumeration<XTSurl> e = v.elements();
			while (e.hasMoreElements()) {
				XTSurl x = (XTSurl) e.nextElement();
				if (wildMatch(c, x.target) && wildMatch(u, x.toString(true))) {
					v.removeElement(x);
				}
			}
		}
		return;
	}

	/**
	 * Delete an entry.
	 ** 
	 ** @param qualifier: a string that qualifies the entry.
	 ** @throws NoTargetException, InvalidQualifierException, and NullUrlException.
	 **/
	public final void delete_ex(final String qualifier, final String target) throws Exception {

		if (XTStrace.bGlobalVerboseEnabled) {
			if (partition == null || partition.length() < 1)
				XTStrace.verbose("[DELETE] Target="	+ target + " Qualifier=" + qualifier);
			else
				XTStrace.verbose("[DELETE] Partition=" + partition + " Target="	+ target + " Qualifier=" + qualifier);
		}

		if (target == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("[DELETE] Target is null");
			throw new XTSException("[DELETE] Target is null", XTSException.XTS_URL_MISSING_TARGET);
		}
		if (qualifier.charAt(0) != '*') {
			if (checkQualifier(qualifier)) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("[DELETE] Qualifier=" + qualifier + " is invalid");
				throw new XTSException("[DELETE] Qualifier=" + qualifier + " is invalid", XTSException.XTS_DIR_INVALID_QUALIFIER);
			}
		}
		sendToDirectoryServer("delete", null, qualifier, target, "*");
		if (XTStrace.bGlobalVerboseEnabled)  
			XTStrace.verbose("[DELETE] exit from delete_ex");
                return;
	}


	/**
	 * Commit changes.
	 ** 
	 * @param shouldI
	 *            if true then changes are hardened, else changes are discarded.
	 **/
	public final void commit(final boolean shouldI) throws Exception {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[COMMIT] " + shouldI);
		if (serviceSet) {
			return;
		}
		if (shouldI) {
			sendToDirectoryServer("commit", null, "", "", "");
		} else {
			sendToDirectoryServer("backout", null, "", "", "");
		}
	}

	/**
	 * shutdown.
	 **/
	public final void shutdown() throws Exception {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[SHUTDOWN] ");
		sendToDirectoryServer("shutdown", null, "", "", "");
	}

	/**
	 * set parameters.
	 ** 
	 * @param param
	 *            String array containing keyword-value pairs. The following
	 *            values are recognized:
	 *            <ul>
	 **            <li>url - the URL to be used for connection.
	 **            <li>passthru - yes/no.
	 **            <li>xtsdsurl - The resolution of the Directory Server.
	 *            </ul>
	 **/
	public final void setParameters(final String[] param) throws Exception {
		for (int i = 0; i < param.length; i += 2) {
			if (param[i].equalsIgnoreCase("passthru")) {
				if (param[i + 1].equalsIgnoreCase("yes")) {
					setLocalService(false);
				} else if (param[i + 1].equalsIgnoreCase("no")) {
					setLocalService(true);
				} else {
					throw new XTSException("Directory setParameters : Invalid value for parameter " + param[i]
							+ "=" + param[i + 1], XTSException.XTS_DDIR_SETP_INVALID_PARM);
				}
			}
			if (param[i].equalsIgnoreCase("url")) {
				setService(new XTSurl(param[i + 1]));
			}
			if (param[i].equalsIgnoreCase("xtsdsurl")) {
				xtsdsurl = param[i + 1];
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Setting xtsdsurl="+ xtsdsurl);
				// Don't resolve this url here. System Property may superceed.
				// Wait until DirServer is actually required in use.
			}
			// throw new Error("Invalid parameter "+param[i]+" passed");
		}
	}

	// ---------------------------------------------------------------------
	// Send Request to the Directory Server.
	// 1. Check System/Environment variable XTSDSURL for valid tcpip://host:port
	// 2. Did caller setParameter("xtsdsurl=tcpip://host:port")
	// 3. Resolve SAGXTSDSHOST/PORT via the DNS.
	// ---------------------------------------------------------------------
	private XTSurl[] sendToDirectoryServer(final String oper, final String[] param, final String qualifier, final String target, final String value) throws Exception {

		Socket dsSocket = null; // converted to a method variable. [0020]
		XTSurl[] dsUrls = null;
		int i = 0;
		String targetRsp = null;  // target from message response [0003]
		String errorRsp = null;   // error response 
		String messageRsp = null; // message response 

		if (oper == null) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("sendToDirectory operation is null");
			throw new XTSException("sendToDirectory operation is null", XTSException.XTS_DDIR_OPERATION_NULL);
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("<sendToDirectory> operation=" + oper);

		if (XTStrace.bGlobalVerboseEnabled) {
			StringBuffer sb = new StringBuffer();

			sb.append("operation=" + oper);
			if (param != null) {
				for (i = 0; i < param.length; i++) {
					sb.append(" param=" + param[i]);
				}
			}
			sb.append(" partition=" + partition);
			sb.append(" qualifier=" + qualifier);
			sb.append(" target=" + target);
			sb.append(" value=" + value);
			XTStrace.verbose(sb.toString());
			if (xtsdsurl == null) 
				XTStrace.verbose("Get the XTSDSURL variable");
			else
				XTStrace.verbose("XTSDSURL defined=" + xtsdsurl);
		}

		// Enhanced DirServer Host and Port resolution
		// 1. Check System Property xtsdsurl for valid tcpip://host:port
		// 2. Did caller setParameter("xtsdsurl=tcpip://host:port")
		// 3. Resolve SAGXTSDShost/port via the DNS.
		//
		if (xtsdsurl == null) {
			String xtsdsurlsp = null;
			try {
				xtsdsurlsp = System.getenv(XTSDSURL);
				if (xtsdsurlsp == null) {
					xtsdsurlsp = System.getenv(XTSDSURL.toLowerCase());
				}
				if (xtsdsurlsp == null) {
					xtsdsurlsp = System.getProperty(XTSDSURL);
				}
				if (xtsdsurlsp == null) {
					xtsdsurlsp = System.getProperty(XTSDSURL.toLowerCase());
				}
				if (xtsdsurlsp != null) {
					if (XTStrace.bGlobalVerboseEnabled) {
						XTStrace.verbose("XTSDSURL=" + xtsdsurlsp);
					}
				}
			} 
			catch (SecurityException se) {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error("Security Exception occurred while accessing system property XTSDSURL");
				throw new XTSException("Security Exception occurred while accessing system property XTSDSURL", XTSException.XTS_DDIR_DSRV_REQUEST_LERROR);
			}
			if (xtsdsurlsp != null) {
				xtsdsurl = xtsdsurlsp;
			}
		}

		if (xtsdsurl != null) {

			XTSurl xtsdsURL = new XTSurl(xtsdsurl);
			// We ignore the protocol specified and assume tcpip.
			XTSDShost = xtsdsURL.getHost();
			XTSDSport = xtsdsURL.getPort();

		} else {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("XTSDSURL is not defined; checking DNS for SAGXTSDSHOST and SAGXTSDSPORT");
			try{
				byte[] b = InetAddress.getByName("SAGXTSDSPort").getAddress();
				XTSDSport = ((b[0] & 0xff) << 8) | (b[1] & 0xff);
			} 
			catch (UnknownHostException e) {
				XTSDSport = defaultPort;
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("SAGXTSDSPORT not found, using Default port="  + XTSDSport);
			}

			XTSDShost = "SAGXTSDShost";
			try{
				@SuppressWarnings("unused")
				InetAddress inet = InetAddress.getByName(XTSDShost);
			} 
			catch (Exception e) {
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error("TCPIP getByName=" + XTSDShost + " failed, using localhost - Exception=" + e);
				XTSDShost = "localhost";
			}
		}
		if (XTStrace.bGlobalVerboseEnabled) {
			XTStrace.verbose("SAGXTSDSHOST=" + XTSDShost);
			XTStrace.verbose("SAGXTSDSPORT=" + XTSDSport);
		}
		Vector<String> vurls = new Vector<String>(); // relocated here. [0020]

		try {
			dsSocket = new Socket(XTSDShost, XTSDSport);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Socket=" + dsSocket);
			/** * Set timeout to prevent a hang should the other side lose 			*/
			dsSocket.setSoTimeout(15000);

			InputStream is = dsSocket.getInputStream();
			OutputStream os = dsSocket.getOutputStream();

			StringBuffer msgoutBuffer = new StringBuffer();
			msgoutBuffer.append(oper);
			msgoutBuffer.append('\t');
			// String msgout = oper + "\t";
			if (param == null) {
				msgoutBuffer.append(partition);
				msgoutBuffer.append('\t');
				msgoutBuffer.append(qualifier);
				msgoutBuffer.append('\t');
				// msgout += partition + "\t" + qualifier + "\t";
			} else {
				for (i = 0; i < param.length; i++) {
					msgoutBuffer.append(param[i]);
					msgoutBuffer.append('\t');
					// msgout += param[i] + "\t";
				}
			}
			msgoutBuffer.append(target);
			msgoutBuffer.append('\t');
			msgoutBuffer.append(value);
			msgoutBuffer.append('\t');
			msgoutBuffer.append('\n');
			msgoutBuffer.append('\n');
			// msgout += target + "\t" + value + "\n\n";

			// int len = msgout.length() + 2; // inclusive length
			int len = msgoutBuffer.length() + 2; // inclusive length
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Request length=" + len);
			if (len > 65535) {
				terminateRequest (vurls, dsSocket);
				throw new XTSException("Request length=" + len + " invalid; bigger than 65535", XTSException.XTS_DDIR_DSRV_REQUEST_LERROR);
			}
			byte[] blen = new byte[4]; // relocated to here. [0020]
			blen[0] = (byte) (len >>> 8);
			blen[1] = (byte) (len);
			os.write(blen, 0, 2);
			// os.write(msgout.getBytes("UTF8"));
			os.write(msgoutBuffer.toString().getBytes("UTF8"));
			os.flush();
			if (XTStrace.bGlobalDebugEnabled) 
				XTStrace.dump("Request Sent to Directory Server", "sendToDirectoryServer", msgoutBuffer.toString(), true);
			// Read reply
			int mlen = is.read(blen, 0, 2);
			if (mlen != -1) {
				mlen = ((blen[0] & 0xff) << 8 | (blen[1] & 0xff));
				byte[] rbuffer = new byte[mlen];
				int rblen = 0;
				try {
					while (rblen < mlen) {
						// int len=is.read( rbuffer, 0, mlen );
						int j = mlen - rblen;
						len = is.read(rbuffer, rblen, j);
						rblen += len;
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Number of bytes received=" + len);
					}
				} catch (InterruptedIOException ie) {
					if (XTStrace.bGlobalErrorEnabled) 
						XTStrace.error("TCPIP Read Timed out" + len);
					terminateRequest (vurls, dsSocket);
					throw new XTSException("TCPIP Read Timed out", XTSException.XTS_TIMEOUT); // error!
				}	
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Response Message 2-byte length=" + mlen);
				if (rblen > 0) {
					if (XTStrace.bGlobalDebugEnabled) 
						XTStrace.dump("Hex Dump of Response Message", "sendToDirectoryServer", rbuffer, len, true);
				}
				if (rblen != mlen) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Number of bytes received=" + rblen + " doesn't match Message length=" + mlen);
					terminateRequest (vurls, dsSocket);
					throw new XTSException("Number of bytes received doesn't match Message length", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
				}
				if (rblen < 8) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Number of bytes received=" + rblen + " < 8");
					terminateRequest (vurls, dsSocket);
					return (null);
				}
				String s = new String(rbuffer, "UTF8");

				if (s.substring(0, 8).equalsIgnoreCase("response")) {
					if (s.substring(8, 9).equals("\t")) { // int
						int p = s.indexOf("\t", 9); // partition 
						if (p == -1) {
							if (XTStrace.bGlobalErrorEnabled) 
			                              		XTStrace.error("Syntax error: partition field is missing");
							terminateRequest (vurls, dsSocket);
							throw new XTSException("Syntax error: partition field is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
						} else { // if (p == -1)
							int q = s.indexOf("\t", p + 1); // qualifier
							if (q == -1) {
								if (XTStrace.bGlobalErrorEnabled) 
				                               		XTStrace.error("Syntax error: qualifier field is missing");
								terminateRequest (vurls, dsSocket);
								throw new XTSException("Syntax error: qualifier field is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
							} else { // if (q == -1)
								int t = s.indexOf("\t", q + 1); // target 
								if (t == -1) {
									if (XTStrace.bGlobalErrorEnabled) 
				                        			XTStrace.error("Syntax error: target field is missing");
									terminateRequest (vurls, dsSocket);
									throw new XTSException("Syntax error: target field is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
								} else { // if (t == -1)
									targetRsp = s.substring(q + 1, t); // get target
									int u = 0;
									for (i = 0; u != -1; i++) {
										u = s.indexOf("\t", t + 1); // url
										if (u == -1) {
											if (i == 0) {
												if (XTStrace.bGlobalErrorEnabled) 
       			        				                	        	XTStrace.error("Syntax error: url field is missing");
												terminateRequest (vurls, dsSocket);
												throw new XTSException("Syntax error: url field is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error
											}
										} else { // if (u == -1)
											vurls.addElement(s.substring(t + 1, u));
											t = u;
										}
									} // for (i = 0
								} // if (t == -1)
							} // if (q == -1)
						} // if (p == -1)
					} // if (s.substring(8, 9).equals("\t"))
				} else  // if (s.substring(0, 8).equalsIgnoreCase("response"))
				if (s.substring(0, 5).equalsIgnoreCase("error")) {
					if (s.substring(5, 6).equals("\t")) { // int
						int c = s.indexOf("&", 6); // error code
						if (c == -1) {
							if (XTStrace.bGlobalErrorEnabled) 
		                	             		XTStrace.error("Syntax error: & is missing");
							terminateRequest (vurls, dsSocket);
							throw new XTSException("Syntax error: & is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
						} else {
							errorRsp =  s.substring(6, c); // message response 
							int m = s.indexOf("\t", c + 1); // error message
							if (m == -1) {
								if (XTStrace.bGlobalErrorEnabled) 
			                        	       		XTStrace.error("Syntax error: message is missing");
								terminateRequest (vurls, dsSocket);
								throw new XTSException("Syntax error: message is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE); // error!
							} else { // if (m == -1)
								messageRsp =  s.substring(c + 1, m); // message response 
								if (XTStrace.bGlobalDebugEnabled) 
        	 	        	                       		XTStrace.debug("Error reply:" + " errorRsp=" + errorRsp.substring(8) + " messageRsp=" + messageRsp.substring(9));
								if (errorRsp.equalsIgnoreCase("ERRORID=0")) {
								} else {
									terminateRequest (vurls, dsSocket);
									throw new XTSException(1, messageRsp.substring(9));  // error return
								}
							}
						} // if (c == -1
					} else { // if (s.substring(5, 6).equals("\t"))
						if (XTStrace.bGlobalErrorEnabled) 
				             		XTStrace.error("Syntax error: //t delimitator is missing");
						terminateRequest (vurls, dsSocket);
				        	throw new XTSException("Syntax error: //t delimitator is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE);
					}
				} else { // if (s.substring(0, 5).equalsIgnoreCase("error")
					if (XTStrace.bGlobalErrorEnabled) 
			        	     XTStrace.error("Syntax error: message token is missing");
					terminateRequest (vurls, dsSocket);
				        throw new XTSException("Syntax error: message token is missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE);
				}
			
				if ((i = vurls.size()) > 0) {
					dsUrls = new XTSurl[i]; // yes.. 
					/**
					 * If wildcard used, then full directory records are
					 * returned.I.e. partition, qualifier, target, and url.
					 **/
					if (partition.equals("*") == true || (qualifier != null && qualifier.equals("*") == true) || (target != null && target.contains("*") == true)) {
						for (int k = 0; k < i; k++) {
							String ss = (String) vurls.elementAt(k);
							// int iTargPos = 0;

							String part = null;
							int iQualifierStart = ss.indexOf('#'); // Partition start.
								if (iQualifierStart == -1) {
								iQualifierStart = 0;
							} else {
								part = ss.substring(0, iQualifierStart);
								iQualifierStart++;
							}

							int iTargStart = ss.indexOf('.'); // Target Name start.
							int iTargEnd = ss.indexOf('[');   // Target Name end.
							int iURLStart = ss.indexOf('=');  // URL start. 
							String qual = ss.substring(iQualifierStart, iTargStart);
							if (iTargStart == -1) {
								iTargStart = 0;
							} else {
								iTargStart++;
							}
							if (iTargEnd == -1) {
								if (XTStrace.bGlobalDebugEnabled) 
									XTStrace.dump("Dump of directory Server reply - target missing", "sendToDirectoryServer", ss, true);
								terminateRequest (vurls, dsSocket);
								throw new XTSException("Target missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE);
							}
							if (iURLStart == -1) {
								if (XTStrace.bGlobalDebugEnabled) 
									XTStrace.dump("Dump of directory Server reply- URL missing", "sendToDirectoryServer", ss, true);
								terminateRequest (vurls, dsSocket);
								throw new XTSException("URL missing", XTSException.XTS_DIRSRV_INVALID_RESPONSE);
							} else {
								iURLStart++;
							}
							String targ = ss.substring(iTargStart, iTargEnd);
							dsUrls[k] = new XTSurl(part, qual, targ, ss.substring(iURLStart));
						} // for 
					} else {
						for (int k = 0; k < i; k++) {
							// load XTSurl array
							dsUrls[k] = new XTSurl(partition, qualifier, targetRsp, (String) vurls.elementAt(k));				
						}
					}
				} // if ((i = vurls.size()) > 0)
			} // if (mlen != -1)
		} // end try
		catch (UnknownHostException e) {
			// Made corrections to try/catch processing
			// to prevent nullpointer exception and remove redundancy
			// in socket close processing.
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("Host Lookup Failed for XTSDSHost=" + XTSDShost);
			terminateRequest (vurls, dsSocket);
			throw new XTSException("Unknown Host Exception : " + XTSDShost, XTSException.XTS_UNKNOWN_HOST_EXCEPTION);
		} 
		catch (IOException e) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("I/O Error contacting Directory Server at " + XTSDShost + ":" + XTSDSport);
			terminateRequest (vurls, dsSocket);
			throw new XTSException("Directory : I/O Error: " + e.getMessage(), XTSException.XTS_DIR_IO_ERROR);
		} 
		catch (XTSException e) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("Exception from Directory Server at " + XTSDShost + ":" + XTSDSport);
//			XTStrace.verbose(e);
			throw e;
		} 
		return dsUrls;
	}


	private void terminateRequest (Vector<String> vurls, Socket dsSocket) throws Exception  {
		if (vurls.size() > 0) {
			vurls.removeAllElements(); // Clear Vector 
		}
		try {
			if (dsSocket != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Closing: " + dsSocket);
				dsSocket.close();
			}
		}
		catch (IOException ie) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Terminate sendToDirectoryServer; Return to caller from IO exception");
			throw new XTSException("Directory : I/O Error: " + ie.getMessage(), XTSException.XTS_DIR_IO_ERROR);
		}
	}


	@Override
	public final String getUrl() {
		return xtsdsurl;
	}
	public final void clearXtsDsUrl() {
		xtsdsurl = null;
	}
	
	/**
	 * Clear the xtsUrlCache hashmap.
	 ** 
	 * @param target
	 *            
	 **/
	public static final XTSurl[] deleteTargetFromCache(String target) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Delete target " + target + " from xtsUrlCache");
		return xtsUrlCache.remove(target);
	}

	/**
	 * Clear the xtsUrlCache hashmap.
	 ** 
	 * @param target
	 *            
	 **/
	public static final void clearXtsUrlCache() {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("[Clear xtsUrlCache] ");
		xtsUrlCache.clear();
		return;
	}

}
