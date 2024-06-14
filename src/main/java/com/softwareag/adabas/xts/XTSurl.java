/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import java.lang.Object.*;
import java.lang.reflect.*;

import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.protocol.*;
import com.softwareag.adabas.xts.XTStrace;
/**
 * This class contains the protocol type and the URL defining the remote
 * location of an Net-Work target. A URL has the form:<br>
 ** <i>protocol://host:port?parm=value&parm=value...</i><br>
 ** where <i>parm</i> and <i>value</i> depend on the protocol concerned. The URL
 * is disected and kept in its component parts. The parm values are kept in a
 * Hashtable for easy access through the getValue(key) method.
 ** 
 **/
public class XTSurl implements Serializable {

	private static final long serialVersionUID = -8265476577810257722L;
	public static final short HOST_ADDRESS_FORMAT = 0;
	/**
	 * Qualifier for the connection entry in ADI (XTSaccess or XTSsession).
	 */
	public String qualifier;
	/**
	 * Partition the XTS url is located in.
	 */
	public String partition;
	private String protocol;
	private String host;
	private int port;
	private boolean isIpv6 = false;
	private LinkedHashMap<String, String> values;

	/**
	 * Target name for the remote target (database id or node name).
	 */
	public String target = ""; // for informational purposes
	public String key = "";    // for informational purposes

	private static LinkedHashMap<String, IDriver> drivlist = new LinkedHashMap<String, IDriver>();

	/**
	 * Construct a URL from protocol, host and port.
	 ** 
	 * @param protocol
	 *            the communication protocol. Currently only TCP/IP.
	 ** @param host
	 *            the name or I.P. address of the host.
	 ** @param port
	 *            the well-known port number.
	 **/
	public XTSurl (final String protocol, final String host, final int port) {
// mihai		this.protocol = protocol.trim().toUpperCase();
		this.protocol = protocol.trim();
		this.host = host.trim();
		this.port = port;
	}

	/**
	 * Construct a URL from target, protocol, host and port.
	 ** 
	 * @param target
	 *            the target name. // [0010]
	 ** @param protocol
	 *            the communication protocol. Currently only TCP/IP.
	 ** @param host
	 *            the name or I.P. address of the host.
	 ** @param port
	 *            the well-known port number.
	 **/
	public XTSurl(final String target, final String protocol, final String host, final int port) {
		this(protocol, host, port);
		this.target = target;
	}

	/**
	 * Construct a URL from target, protocol, host and port.
	 ** 
	 * @param targetID
	 *            the target ID number.
	 ** @param protocol
	 *            the communication protocol. Currently only TCP/IP.
	 ** @param host
	 *            the name or I.P. address of the host.
	 ** @param port
	 *            the well-known port number.
	 **/
	public XTSurl(final int targetID, final String protocol, final String host, final int port) {
		this(protocol, host, port);
		this.target = Integer.toString(targetID);
	}

	/**
	 * Construct a URL from a string.
	 ** 
	 * @param inUrl
	 *            the url in form
	 *            "protocol:\\host:port?value=value&value=value".
	 **/
	public XTSurl(final String inUrl) throws XTSException {
		String urlString = inUrl;
		int i = urlString.indexOf("://");
		if (i != -1) {
// mihai			protocol = urlString.substring(0, i).trim().toUpperCase();
			protocol = urlString.substring(0, i).trim();
			urlString = urlString.substring(i + 3);
		} else {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Invalid URL=" + inUrl + " index of : is negative");
			throw new XTSException("Invalid URL=" + inUrl + " index of : is negative", XTSException.XTS_XTSURL_INVALID_URL);
		}

		i = urlString.indexOf("?");
		if (i != -1) {
			values = new LinkedHashMap<String, String>();
			StringTokenizer st = new StringTokenizer(urlString.substring(i + 1), "&");
			urlString = urlString.substring(0, i);
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				i = s.indexOf('=');
				if (i == -1) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Invalid URL=" + inUrl + " index of = is negative");
					throw new XTSException("Invalid URL=" + inUrl + " index of = is negative", XTSException.XTS_XTSURL_INVALID_URL);
				}
// mihai				values.put(s.substring(0, i).toUpperCase(), unescape(s, i + 1));
				values.put(s.substring(0, i), unescape(s, i + 1));
			}
		}

		// Check for IPv6 IP adresses
		i = urlString.indexOf("[");
		if (i != -1) {
			isIpv6 = true;
			int end = urlString.indexOf("]");
			host = urlString.substring(i + 1, end);
			try {
				port = Integer.parseInt(urlString.substring(end + 2));
			} catch (Exception e) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Invalid URL=" + inUrl + " port is not numeric");
				throw new XTSException("Invalid URL=" + inUrl + " port is not numeric", XTSException.XTS_INVALID_PORT);
			}
		} else {
			host = urlString.trim();
			port = 0;
			i = host.indexOf(":");
			if (i != -1) {
				try {
					port = Integer.parseInt(host.substring(i + 1));
					host = host.substring(0, i);
				} catch (Exception e) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Invalid URL=" + inUrl + " port is not numeric");
					throw new XTSException("Invalid URL=" + inUrl + " port is not numeric", XTSException.XTS_INVALID_PORT);
				}
			}
		}
		if (port <  0 || port > 65353) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Invalid URL=" + inUrl + " port value outside of bounds (0-65353)");
			throw new XTSException("Invalid URL=" + inUrl + " port value outside of bounds (0-65353)", XTSException.XTS_INVALID_PORT);
		}								
	}

	private static final int[] revhex = { 0, 10, 11, 12, 13, 14, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

	private static final String unescape(final String s, final int pos) {
		char[] c = s.toCharArray();
		int end = c.length;
		for (int i = pos; i < end; i++) {
			switch (c[i]) {
			case '%':
				c[i] = (char) ((revhex[c[i + 1] & 0x1f] << 4) | revhex[c[i + 2] & 0x1f]);
				System.arraycopy(c, i + 3, c, i + 1, end - i - 3);
				end -= 2;
				break;
			case '+':
				c[i] = ' ';
				break;
			case 13:
			case 10:
				end = i;
				break;
			default:
				break;
			}
		}
		return new String(c, pos, end - pos);
	}

	/**
	 * Construct a URL from a target and URL string.
	 ** 
	 * @param target
	 *            the target name.
	 ** @param url_string
	 *            the url in form
	 *            "protocol:\\host:port?value=value&value=value".
	 **/
	public XTSurl (final String partition, final String qualifier, final String target, final String url_string) throws XTSException {
		this(url_string);
		this.target = target;
		this.qualifier = qualifier;
		this.partition = partition;
		if (XTStrace.bGlobalVerboseEnabled) {
			if (partition != null)
				XTStrace.verbose("Qualifier=" + this.qualifier + " Target=" + this.target + " partition=" + this.partition + " URL=" + url_string);
			else
				XTStrace.verbose("Qualifier=" + this.qualifier + " Target=" + this.target + " URL=" + url_string);
		}
	}

	/**
	 * Construct a URL from a target and URL string.
	 ** 
	 * @param target
	 *            the target name.
	 ** @param url_string
	 *            the url in form
	 *            "protocol:\\host:port?value=value&value=value".
	 **/
	public XTSurl (final String target, final String url_string) throws XTSException {
		this(url_string);
		this.target = target;
		if (XTStrace.bGlobalVerboseEnabled) {
			XTStrace.verbose("Target=" + this.target + " URL=" + url_string);
		}
	}

	/**
	 * Construct a URL from a target and URL string.
	 ** 
	 * @param targetID
	 *            the target ID number.
	 ** @param url_string
	 *            the url in form
	 *            "protocol:\\host:port?value=value&value=value".
	 **/
	public XTSurl(final int targetID, final String url_string) throws XTSException {
		this(url_string);
		this.target = Integer.toString(targetID);
		if (XTStrace.bGlobalVerboseEnabled) {
			XTStrace.verbose("TargetId=" + this.target + " URL=" + url_string);
		}
	}

	/**
	 * Construct a URL object from a target and another URL object.
	 ** 
	 * @param target
	 *            the target name. // [0010]
	 ** @param url
	 *            the URL object from which a new URL object is to be
	 *            constructed.
	 **/
	public XTSurl(final String target, final XTSurl url) {
		this.target = target;
// mihai		protocol = url.protocol.toUpperCase();
		protocol = url.protocol;
		host = url.host;
		port = url.port;
		values = url.values;
	}

	/**
	 * Construct a URL object from a target and another URL object.
	 ** 
	 * @param targetID
	 *            the target ID number.
	 ** @param url
	 *            the URL object from which a new URL object is to be
	 *            constructed.
	 **/
	public XTSurl(final int targetID, final XTSurl url) {
		this.target = Integer.toString(targetID);
// mihai		protocol = url.protocol.toUpperCase();
		protocol = url.protocol;
		host = url.host;
		port = url.port;
		values = url.values;
	}

	/** Get protocol **/
	public final String getProtocol() {
// mihai		return protocol.toUpperCase();
		return protocol;
	}

	/** Get host **/
	public final String getHost() {
		return host;
	}

	/** Get port **/
	public final int getPort() {
		return port;
	}

	/** Get qualifier **/
	public final String getQualifier() {
		return qualifier;
	}

	/** Get partition **/
	public final String getPartition() {
		return partition;
	}

	/** Get a value **/
	public final String getValue(final String key) {
		if (values == null) {
			return null;
		}
// mihai		return (String) values.get(key.toUpperCase());
		return (String) values.get(key);
	}

	/** Put a value **/
	public final void setValue(final String key, final String value) {
		if (values == null) {
			values = new LinkedHashMap<String, String>();
		}
// mihai		values.put(key.toUpperCase(), value.toUpperCase());
		values.put(key.toUpperCase(), value);
	}
	/** Make a string of it. **/
	public final String toString() {
		String s = null;
		if (isIpv6)
			s =  target + "(" + protocol.toUpperCase() + "://[" + host + "]:" + port + ")";
// mihai			s =  target + "(" + protocol + "://[" + host + "]:" + port + ")";
		else
			s =  target + "(" + protocol.toUpperCase() + "://" + host + ":" + port + ")";
// mihai			s =  target + "(" + protocol + "://" + host + ":" + port + ")";
 XTStrace.verbose("Url String=" + s);
		return s;
	}
	/** Make a string of it. **/
	public final String toString(final short stringFormat) {
		String s = null;
		if (stringFormat == HOST_ADDRESS_FORMAT)
			try {
				String hostAddress = InetAddress.getByName(host).getHostAddress();
// mihai				s = "(" + protocol.toUpperCase() + "://" + hostAddress + ":" + port + ")";
				s = "(" + protocol + "://" + hostAddress + ":" + port + ")";
			} catch (UnknownHostException e) {
				s = "(" + protocol + "://" + host + ":" + port + ")";
// mihai				s = "(" + protocol.toUpperCase() + "://" + host + ":" + port + ")";
			}
 XTStrace.verbose("Url String=" + s);
		return s;
	}
	/** Make into a string, with additional parameters shown. **/
	public final String toString(final boolean withValues) {
		StringBuffer sb = new StringBuffer();
// mihai		sb.append(protocol.toUpperCase());
		sb.append(protocol);
		sb.append("://");
		if (isIpv6) sb.append('[');
		sb.append(host);
		if (isIpv6) sb.append(']');
		sb.append(':');
		sb.append(port);
		if (withValues && values != null) {
			char c = '?';
			for (Map.Entry mapElement : values.entrySet()) { 
		            	String s = (String)mapElement.getKey();   
				sb.append(c);
				c = '&';
				sb.append(s);
				sb.append('=');
				sb.append((String) mapElement.getValue());
      			  } 
		}
		return sb.toString();
	}
	/** Get target (if known). **/
	public final String getTarget() {
		return target;
	}
	/** Return a driver instance. **/
	public final IDriver driver() {
// mihai		String s = protocol.toUpperCase();
		String s = protocol;
		if (s.equalsIgnoreCase("TCP/IP")) {
			s = "TCPIP";
		}
		if (s.equalsIgnoreCase("HTTP")) {
			s = "HTTP11";
		}
		if (s.equalsIgnoreCase("TCPMHDR")) {
			s = "MHDR";
		}
		if (s.equalsIgnoreCase("TCPRDA")) {
			s = "RDA";
		}
		synchronized (drivlist) {
			Object new_driver;
			IDriver driver = (IDriver) drivlist.get(s);
			if (driver != null) {
				return driver;
			}
			try {
				Class<?> c = Class.forName("com.softwareag.adabas.xts.protocol." + s);
				Constructor<?> constructor = c.getDeclaredConstructor();
				constructor.setAccessible(true);//ABRACADABRA!
				new_driver = constructor.newInstance();
			} catch (Exception e) {
				if (XTStrace.bGlobalErrorEnabled) {
					e.printStackTrace(System.err);
				}
				throw new RuntimeException("XTSurl driver: unable to create a driver instance for " + s + "Reason: " + e.getMessage());
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Loading the " + s + " protocol");
			drivlist.put(s, (IDriver)new_driver);
			return (IDriver)new_driver;
		}
	}

}
