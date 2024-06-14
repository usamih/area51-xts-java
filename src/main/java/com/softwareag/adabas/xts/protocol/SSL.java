/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.protocol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.interfaces.IConnectCallback;
import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.throwable.InvalidQualifierException;
import com.softwareag.adabas.xts.network.IPtransport;

//-----------------------------------------------------------------------
/**
 * This class implements a JSSE/SSL protocol driver for XTS.
 * 
 * @author Jorge Cordovil Lima, (usaco)
 * 
 *          This class needs JSSE1.0.2 or higher. JSSE 1.0.2 requires JavaTM 2
 *          SDK v 1.2.2 or later, or JavaTM 2 Runtime Environment v 1.2.2 or
 *          higher.
 * 
 *          During the installation of JSSE1.0.2, one must add the SunJSSE
 *          provider to the java security file located at:
 *          <java-home>/lib/security/java.security, where <java-home> is the JRE
 *          directory so this may be for example located at
 *          jdk1.3/jre/lib/security/java.security ...
 *          security.provider.1=sun.security.provider.Sun
 *          security.provider.2=com.sun.net.ssl.internal.ssl.Provider ... ... As
 *          an alternative to the above mentioned static provider installation,
 *          a site may chose the dynamic JSSE provider installation by invoking:
 *          ... Security.addProvider(new
 *          com.sun.net.ssl.internal.ssl.Provider()); ... in their code before
 *          dealing with any XTS urls mentioning the SSL protocol.
 * 
 *          Other important points during the JSSE installation are the choice
 *          of intalling JSSE supplied jar files jsse.jar, jnet.jar, and
 *          jcerts.jar in <java-home>/lib/ext or keep them at their original
 *          places and change the classpath.
 * 
 *          Please note that certificates do have an expiration date. The
 *          certificates shipped with Java2 jdk1.3 where expired by the time
 *          JSEE1.0.2 were released. Thus copying the testkeys file delivered
 *          with JSSE over the cacerts, and jssecacerts in
 *          <java-home>/lib/security/ may be a good install/test idea.
 */
// Created: on Sep, 2000
//
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022
// -----------------------------------------------------------------------

public class SSL extends TCPIP {
	public static final String VERSION = XTSversion.VERSION; 
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	public static final String className = "SSL";

	/*
	 * The constants defined below are the XTS way to configure urls to play
	 * under a SSL driven environment. The Attributes/Defaults precedence below
	 * have been sorted in the order of high to low: 1. Pick the Attribute as it
	 * has been coded in a keyword= on the url definition. 2. JAVA defaults if
	 * any, would take place. (see references to JAVA system properties) 3. Take
	 * the XTS default if any.
	 */

	/* VERSION=the SSL context type */
	static final String VERSION_PARM = "VERSION";
	static final String VERSION_0 = "0";
	static final String TLSv1 = "TLSv1"; // VERSION=0 or default
	static final String VERSION_3 = "3";
	static final String SSLv3 = "SSLv3"; // VERSION=3

	// VERIFY=the verification level
	static final String VERIFY = "VERIFY";

	static final String NO_PEER_VERIFICATION = "0"; // VERIFY=0 default
	/*
	 * NO_PEER_VERIFICATION is the default and it means: Server send
	 * certificates but does not request certificates from clients. Client has
	 * the rights of cutting communication
	 */

	static final String LIGHT_PEER_VERIFICATION = "1";
	/*
	 * LIGHT_PEER_VERIFICATION is accepted by this implementation in order to
	 * reduce the differences among the verification options that the C API
	 * exports and the ones exported by the JSSE/SSL Java implementation.
	 * LIGHT_PEER_VERIFICATION is not really supported by XTS JSSE/SSL and will
	 * play as NO_PEER_VERIFICATION. LIGHT_PEER_VERIFICATION in the C API means:
	 * Server send certificates and suggests client to send its certificates.
	 * Server will not cut communications if client did not present its
	 * certificates. Client also has the rights of cutting communication.
	 */

	static final String FORCE_PEER_VERIFICATION = "2";
	/*
	 * FORCE_PEER_VERIFICATION means: Server send certificates and forces client
	 * to send its certificates. Server will cut communications if client did
	 * not present its certificates or if client is not trusted. Client also has
	 * the rights of cutting communication
	 */

	static final String SAME_AS_FORCE_PEER_VERIFICATION = "3";
	/*
	 * SAME_AS_FORCE_PEER_VERIFICATION is accepted by this implementation in
	 * order to reduce the differences among the verification options that the C
	 * API exports and the ones exported by the XTS JSSE/SSL Java
	 * implementation. This option is a blend of option 1 and 2 and shall work
	 * as just the same as option FORCE_PEER_VERIFICATION for XTS JSSE/SSL Java.
	 */

	// KEYSTORE=the keystore
	static final String KEYSTORE = "KEYSTORE";
	/*
	 * The system property javax.net.ssl.keyStore can also set the keystore.
	 */
	static final String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not. JSSE1.0.2 has not preset any
	 * default
	 */

	// KEYSTORE_TYPE=the keystore type
	static final String KEYSTORE_TYPE = "KEYSTORE_TYPE";
	/*
	 * Java default (JSSE1.0.2) is JKS see it in keystore.type= in
	 * <JAVA-HOME>/lib/security/java.security Please note that the system
	 * property javax.net.ssl.keyStoreType is also capable of overriding the
	 * above default.
	 */
	static final String KEYSTORE_TYPE_PROPERTY = "javax.net.ssl.keyStoreType";
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not.
	 */
	static final String JKS_KEYSTORE = "JKS"; // KEYSTORE_TYPE=JKS is the default.
	static final String PKCS12_KEYSTORE = "PKCS12"; // KEYSTORE_TYPE=PKCS12

	// KEYSTORE_PSSWD=the keystore password
	static final String KEYSTORE_PASSWD = "KEYSTORE_PASSWD";
	static final String KEYSTORE_PASSWD_PROPERTY = "javax.net.ssl.keyStorePassword";
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not.
	 */
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not. JSSE1.0.2 has not preset any
	 * default
	 */

	// TRUSTSTORE=a keystore containing certificates
	static final String TRUSTSTORE = "TRUSTSTORE"; // the SunX509 certificates
	/*
	 * The system property javax.net.ssl.trustStore can also set the truststore.
	 */
	static final String TRUSTSTORE_PROPERTY = "javax.net.ssl.trustStore";
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not. If the above system property is
	 * not set, the default will be jssecacerts or cacerts in
	 * <java-home>\lib\security. I mean <java-home> as the jre taken at run
	 * time, please refer to system property java.home) so,
	 * ..../jre/lib/security/jseecacerts or, ..../jre/lib/security/cacerts
	 */

	// TRUSTSTORE_TYPE=is a keystore type
	static final String TRUSTSTORE_TYPE = "TRUSTSTORE_TYPE";
	/*
	 * The default type is JKS The system property javax.net.ssl.trustStoreType
	 * can also set the truststore type.
	 */
	static final String TRUSTSTORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";
	/*
	 * Please note that Sun does not guarantee whether the above property will
	 * be present into further releases or not.
	 */

	// TRUSTSTORE_PSSWD=the truststore password
	static final String TRUSTSTORE_PASSWD = "TRUSTSTORE_PASSWD";
	/*
	 * There is not any default set. The system property
	 * javax.net.ssl.trustStorePassword can set a password This implementation
	 * of XTS JSSE/SSL does NOT need to receive a trustStore password
	 */
	static final String TRUSTSTORE_PASSWD_PROPERTY = "javax.net.ssl.trustStorePassword";

	// JCIPHER=the enabled cipher suite [0001]
	static final String JCIPHER = "JCIPHER";
	/*
	 * The presence of JCIPHER= may enable chipher suites that are present in
	 * this parameter if they were supported. It would also disable any enabled
	 * supported suite that was not present in this parameter. Multiple cipher
	 * suites may be presented by this parameter and they shall be separated by
	 * a semicolumn.
	 * 
	 * JSSE1.0.2 supports the following cipher suites:
	 * SSL_DH_anon_WITH_DES_CBC_SHA SSL_DH_anon_WITH_3DES_EDE_CBC_SHA
	 * SSL_DHE_DSS_WITH_DES_CBC_SHA SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
	 * SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA
	 * SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA SSL_RSA_WITH_RC4_128_MD5
	 * SSL_RSA_WITH_RC4_128_SHA SSL_RSA_WITH_DES_CBC_SHA
	 * SSL_RSA_WITH_3DES_EDE_CBC_SHA SSL_DH_anon_WITH_RC4_128_MD5
	 * SSL_RSA_EXPORT_WITH_RC4_40_MD5 SSL_RSA_WITH_NULL_MD5
	 * SSL_RSA_WITH_NULL_SHA SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
	 * 
	 * Please note that at the client/server handshake time, the server is the
	 * one that would enforce which cipher suite shall be in play. The server
	 * must use a common cipher suite enabled by the server and by the client
	 * sockets, but one has NO control over the one that the server would
	 * select.
	 * 
	 * Of course there may exist cases without any common cipher suite, or with
	 * cipher suites that may not be able to handle the certificates presented
	 * in a trust store. Should these bad cases happen, communication would not
	 * be possible.
	 * 
	 * The property javax.net.debug=ssl,handshake,verbose,session may be a great
	 * helper in order to find why connections could not be done.
	 * 
	 * /*End of parameter configuration constants
	 */

	static final String SUNX509_KEYMANAGER = "SunX509";
	static final String SUNX509_TRUSTMANAGER = "SunX509";
	static final String DEFAULT_PROVIDER_STRING = "SunJSSE";

	private static final int OPTION_KEYSTORE = 1;
	private static final int OPTION_TRUSTSTORE = 2;

	// Constructors follow

	/**
	 * SSL Default constructor, needed at the SSL dynamic class loading time.
	 **/
	SSL() {
		super();
		protocol = "ssl";
	}

	/**
	 * SSL Constructor to be internally used by the SSL class for listening or
	 * connecting
	 * 
	 * @param url
	 *            The URL where it should listen or connect.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 * @param retry_interval
	 *            , the retry interval that should elapse before attempting
	 *            again.
	 * @param retry_count
	 *            the maximum attempting number
	 * @param reconnect
	 *            , (not in use at this time).
	 * @return an instance of this driver.
	 */
	private SSL(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect) {
		super(url, listen, callback, userval, retry_interval, retry_count, reconnect);
		protocol = "ssl";
	}

	/**
	 * SSL Constructor to be internally used by the SSL class for listening or
	 * connecting
	 * 
	 * @param url
	 *            The URL where it should listen or connect.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 * @param retry_interval
	 *            , the retry interval that should elapse before attempting
	 *            again.
	 * @param retry_count
	 *            the maximum attempting number
	 * @param reconnect
	 *            , (not in use at this time).
	 ** @param connTo
	 *            the time to wait for a connection to be established.
	 * @return an instance of this driver.
	 */
	private SSL(final XTSurl url, final boolean listen,
			final IConnectCallback callback, final Object userval,
			final long retry_interval, final int retry_count,
			final boolean reconnect, final int connTo) {
		super(url, listen, callback, userval, retry_interval, retry_count, reconnect, connTo);
		protocol = "ssl";
	}

	// Overriden Methods follow

	/**
	 * Listen for an incoming connection overriding the listen method in TCPIP.
	 * 
	 * @param url
	 *            is the listen URL.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 * @return an instance of this driver.
	 */
	public final IDriver listen(final XTSurl url, final IConnectCallback callback, final Object userval) {

		SSL ssl = new SSL(url, true, callback, userval, 0, 0, false);
		ssl.start();
		return ssl;
	}

	/**
	 * Establish a connection overriding the connect method in TCPIP.
	 * 
	 * @param url
	 *            is the connect URL.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 ** @param connTo
	 *            the time to wait for a connection to be established.
	 * @return an instance of this driver.
	 */
	public IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval, final int connTo) {

		SSL ssl = new SSL(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false, connTo);
		ssl.start();
		return ssl;
	}
	
	/**
	 * Establish a connection overriding the connect method in TCPIP.
	 * 
	 * @param url
	 *            is the connect URL.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 * @return an instance of this driver.
	 */
	public IDriver connect(final XTSurl url, final IConnectCallback callback, final Object userval) {
		SSL ssl = new SSL(url, false, callback, userval, DEFAULT_RETRY_INTERVAL, DEFAULT_RETRY_COUNT, false);
		ssl.start();
		return ssl;
	}

	/**
	 * Establish a connection overriding the connect method in TCPIP.
	 * 
	 * @param url
	 *            is the connect URL.
	 * @param callback
	 *            indicates where to callback when connection established.
	 * @param userval
	 *            the user value to be passed to the callback method.
	 * @param retry_interval
	 *            the new retry interval if connection attempt fails.
	 * @param retry_count
	 *            the number of times to retry to connect. Set this value to 0
	 *            if no retry is needed.
	 * @param reconnect
	 *            set to true if the driver should try to re-establish a
	 *            connection which has been severed.
	 * @return an instance of this driver.
	 */
	public IDriver connect(final XTSurl url, final IConnectCallback callback,
			final Object userval, final long retry_interval,
			final int retry_count, final boolean reconnect) {
		SSL ssl = new SSL(url, false, callback, userval, retry_interval, retry_count, reconnect);
		ssl.start();
		return ssl;
	}

	/**
	 * Create a SSL Server Socket by overriding the IPTransport method. The SSL
	 * configuration parameters will be read from the url.
	 * 
	 * @return a ServerSocket handle.
	 * @exception IOException
	 *                if a ServerSocket could not be established
	 */
	protected ServerSocket createServerSocket() throws IOException {
		String fingerprint = className + ", CreateServerSocket";
		String fileName = null;
		SSLServerSocket serverSocket = null;
		// Socket clientSocket = null;
		char[] keyStorePasswd = null;
		char[] trustStorePasswd = null;
		SSLContext ctx = null;
		KeyManagerFactory kmf = null;
		KeyManager[] keyManagers = null;
		TrustManagerFactory tmf = null;
		TrustManager[] trustManagers = null;
		KeyStore keyStore = null;
		KeyStore trustStore = null;
		String[] cipherSuites = null;
		String verify = null;
		int i = 0;

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(fingerprint + " " + url.toString(true));
		try {
			keyStore = getKeyStoreInstance(OPTION_KEYSTORE);
			keyStorePasswd = getPasswd(KEYSTORE_PASSWD);
			fileName = loadKeyStore(keyStore, keyStorePasswd);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint + " loaded keystore " + fileName);

			kmf = KeyManagerFactory.getInstance(SUNX509_KEYMANAGER, DEFAULT_PROVIDER_STRING);
			kmf.init(keyStore, keyStorePasswd);
			keyManagers = kmf.getKeyManagers();

			trustStore = getKeyStoreInstance(OPTION_TRUSTSTORE);
			trustStorePasswd = getPasswd(TRUSTSTORE_PASSWD);
			fileName = loadTrustStore(trustStore, trustStorePasswd);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint + " loaded truststore " + fileName);

			tmf = TrustManagerFactory.getInstance(SUNX509_TRUSTMANAGER, DEFAULT_PROVIDER_STRING);
			tmf.init(trustStore);
			trustManagers = tmf.getTrustManagers();

			ctx = getSSLContext();
			ctx.init(keyManagers, trustManagers, null);

			SSLServerSocketFactory ssf = ctx.getServerSocketFactory();

			serverSocket = (SSLServerSocket) ssf.createServerSocket(url.getPort());
			if (XTStrace.bGlobalVerboseEnabled) {
				XTStrace.verbose(fingerprint + " SSLServerSocket has been created as " + serverSocket);
				cipherSuites = serverSocket.getSupportedCipherSuites();
				for (i = 0; i < cipherSuites.length; i++) {
					XTStrace.verbose(fingerprint + " " + cipherSuites[i] + " is supported");
				}
			}

			setCipherSuite(serverSocket, null);

			if (XTStrace.bGlobalVerboseEnabled) {
				cipherSuites = serverSocket.getEnabledCipherSuites();
				for (i = 0; i < cipherSuites.length; i++) {
					XTStrace.verbose(fingerprint + " " + cipherSuites[i] + " is enabled");
				}
			}

			verify = url.getValue(VERIFY);
			if (verify != null) {
				if (0 == FORCE_PEER_VERIFICATION.compareTo(verify) || 0 == SAME_AS_FORCE_PEER_VERIFICATION.compareTo(verify)) {
					serverSocket.setNeedClientAuth(true);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose(fingerprint + " NeedClientAuth");
				} else if (0 != NO_PEER_VERIFICATION.compareTo(verify) && 0 != LIGHT_PEER_VERIFICATION.compareTo(verify)) {
					throw (new InvalidQualifierException("VERIFY= unsupported as it is in url " + url.toString(true)));
				}
			}

			return serverSocket;
		} catch (Exception e) {
			if (XTStrace.bGlobalErrorEnabled) {
				System.err.println(fingerprint + ", " + e.getMessage()); 
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint, e);
			throw new IOException(fingerprint + " failed");
		}

	}

	/**
	 * Create a SSL Client Socket overriding the method in IPTransport! The SSL
	 * configuration parameters will be read from the url.
	 * 
	 * @return a Socket handle.
	 * @exception IOException
	 *                if connection could not be established
	 */
	protected Socket createClientSocket() throws IOException {
		String fingerprint = className + ", CreateClientSocket";
		String fileName = null;
		SSLSocket clientSocket = null;
		char[] keyStorePasswd = null;
		char[] trustStorePasswd = null;
		SSLContext ctx = null;
		KeyManagerFactory kmf = null;
		KeyManager[] keyManagers = null;
		TrustManagerFactory tmf = null;
		TrustManager[] trustManagers = null;
		KeyStore keyStore = null;
		KeyStore trustStore = null;
		String[] cipherSuites = null;
		int i = 0;

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(fingerprint + " " + url.toString(true));
		try {
			keyStore = getKeyStoreInstance(OPTION_KEYSTORE);
			keyStorePasswd = getPasswd(KEYSTORE_PASSWD);
			fileName = loadKeyStore(keyStore, keyStorePasswd);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint + " loaded keystore " + fileName);

			kmf = KeyManagerFactory.getInstance(SUNX509_KEYMANAGER);
			kmf.init(keyStore, keyStorePasswd);
			keyManagers = kmf.getKeyManagers();

			trustStore = getKeyStoreInstance(OPTION_TRUSTSTORE);
			trustStorePasswd = getPasswd(TRUSTSTORE_PASSWD);
			fileName = loadTrustStore(trustStore, trustStorePasswd);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint + " loaded truststore " + fileName);

			tmf = TrustManagerFactory.getInstance(SUNX509_TRUSTMANAGER);
			tmf.init(trustStore);
			trustManagers = tmf.getTrustManagers();

			ctx = getSSLContext();
			ctx.init(keyManagers, trustManagers, null);

			SSLSocketFactory ssf = ctx.getSocketFactory();

			clientSocket = (SSLSocket) ssf.createSocket(url.getHost(), url.getPort());
			if (XTStrace.bGlobalVerboseEnabled) {
				XTStrace.verbose(fingerprint + " SSLClientSocket has been created as " + clientSocket);
				cipherSuites = clientSocket.getSupportedCipherSuites();
				for (i = 0; i < cipherSuites.length; i++) {
					XTStrace.verbose(fingerprint + " " + cipherSuites[i] + " is supported");
				}
			}

			setCipherSuite(null, clientSocket);

			if (XTStrace.bGlobalVerboseEnabled) {
				cipherSuites = clientSocket.getEnabledCipherSuites();
				for (i = 0; i < cipherSuites.length; i++) {
					XTStrace.verbose(fingerprint + " " + cipherSuites[i] + " is enabled");
				}
			}

			clientSocket.startHandshake();
			if (XTStrace.bGlobalVerboseEnabled) {
				SSLSession sslSession = clientSocket.getSession();
				XTStrace.verbose(fingerprint + " " + sslSession.getCipherSuite() + " has been imposed by Server");
			}

			return clientSocket;
		} catch (Exception e) {
			if (XTStrace.bGlobalErrorEnabled) {
				System.err.println(fingerprint + ", " + e.getMessage()); // [0003]
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose(fingerprint, e);
			throw new IOException(fingerprint + " failed");
		}

	}

	/**
	 * This method should only be entered if SSLServerSocket is not null, XOR
	 * the SSLSocket is not null. Thus this call must be for a serverSocket or a
	 * clientSocket, but NOT for both at the same time. This method retrieves
	 * the XTS SSL JCIPHER= parameter from the url, enable the presented cipher
	 * suite(s) and disable all the ones that have not been mentioned.
	 * 
	 * @param serverSocket
	 * @param clientSocket
	 * @return void.
	 * @exception InvalidQualifierException
	 *                thrown if any one of the named CIPHER suites happened to
	 *                be unsupported.
	 */
	private void setCipherSuite(final SSLServerSocket serverSocket, final SSLSocket clientSocket) throws InvalidQualifierException {
		String[] supportedCipherSuite = null; // Currently supported by default
		String[] wantToEnableCipherSuite = null; // The Cipher Suites that one may want to enable
		// int wantToEnableCipherSuiteLength = 0; //The taken from
		// supportedCipherSuite.length
		String[] enableCipherSuite = null; // What will really be enabled
		String requestCipher = null; // What has been request via XTSssl or url
		String workingString = null;
		final int SEPARATOR = (int) ';'; // The Cipher Suite separator
		int beginAt = 0;
		int endAt = 0;
		int i = 0;
		int j = 0;
		boolean isCipherSuiteSupported = false;

		if (null == serverSocket && null == clientSocket || null != serverSocket && null != clientSocket) {
			throw (new InvalidQualifierException("XTS Internal error in setCipherSuite due to Bad Parameters combination"));
		}

		// Get xxx from JCIPHER=xxx if JCIPHER= is present in the url.
		requestCipher = url.getValue(JCIPHER); // [0001]

		// Default cipher suite?
		if (null == requestCipher) {
			return; // Yes, we are done
		}
		// We have some cipher suite request, but how many? Take the maximum
		// possible
		if (null != clientSocket) {
			supportedCipherSuite = clientSocket.getSupportedCipherSuites();
		} else {
			supportedCipherSuite = serverSocket.getSupportedCipherSuites();
		}
		wantToEnableCipherSuite = new String[supportedCipherSuite.length];

		// Multiple Cipher Suites request or only one?
		do {
			// Parse one CipherSuite at each lap of this loop and if supported,
			// add it to the
			// wantToEnableCipherSuite
			beginAt = endAt;
			isCipherSuiteSupported = false;
			endAt = requestCipher.indexOf(SEPARATOR, beginAt);

			// Did it find any SEPARATOR?
			if (0 < endAt) {
				// over the SEPARATOR
				workingString = requestCipher.substring(beginAt, endAt++);
			} else {
				// No, this is the last one
				workingString = requestCipher.substring(beginAt);
			}

			for (i = 0; i < supportedCipherSuite.length; i++) {
				if (0 == supportedCipherSuite[i].compareTo(workingString)) {
					isCipherSuiteSupported = true;
					wantToEnableCipherSuite[j++] = workingString;
					break;
				}
			}

			if (!isCipherSuiteSupported && null != workingString) {
				throw (new InvalidQualifierException("JCIPHER=" + workingString	+ " is unsupported as in url " + url.toString(true)));
			}

		} while (0 < endAt);

		if (0 < j) {
			enableCipherSuite = new String[j];

			/*
			 * We need to have enableCipherSuite without nulls, because a null
			 * means accept system defaults in a setEnabledCipherSuites( )
			 * function call
			 */
			for (i = 0; i < j; i++) {
				enableCipherSuite[i] = wantToEnableCipherSuite[i];
			}

			if (null != clientSocket) {
				clientSocket.setEnabledCipherSuites(enableCipherSuite);
			} else {
				serverSocket.setEnabledCipherSuites(enableCipherSuite);
			}
		}
	}

	/**
	 * This method attempts to retrive a XTS SSL keyword matching
	 * KEYSTORE_PASSWD or TRUSTSTORE_PASSWD parameter from the url. If this
	 * failed then it attempts to get the related default password set for the
	 * passed parameter keyword.
	 * 
	 * @param keyword
	 *            shall be KEYSTORE_PASSWD or TRUSTSTORE_PASSWD
	 * @return a char[] handle for password.
	 * @exception InvalidQualifierException
	 *                thrown if getPasswd could not retrieve anything for the
	 *                named keyword, and the keyword was not KEYSTORE_PASSWD or
	 *                TRUSTSTORE_PASSWD.
	 */
	private char[] getPasswd(final String keyword) throws InvalidQualifierException {
		String password = null;
		char[] charPasswd = null;
		/*
		 * Get password from url, or from System Property, or leave it as null.
		 * JSSE SSL verifies the integrity of any keystore opened with a
		 * password.
		 */
		password = url.getValue(keyword);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("keyword=" + keyword + " password=" + password);
		if (password == null) {
			if (keyword.compareTo(KEYSTORE_PASSWD) == 0) {
				password = System.getProperty(KEYSTORE_PASSWD_PROPERTY);
			} else if (keyword.compareTo(TRUSTSTORE_PASSWD) == 0) {
				password = System.getProperty(TRUSTSTORE_PASSWD_PROPERTY);
			} else {
				throw (new InvalidQualifierException("XTS Internal error in getPasswd(" + keyword + ")"));
			}
		}
		if (password != null) {
			charPasswd = password.toCharArray();
		}
		return charPasswd;
	}

	/**
	 * This method retrieves the XTS SSL KEYSTORE_TYPE, TRUSTSTORE_TYPE url
	 * parameters, or a system property default depending on the passed option,
	 * and gets a KeyStore instance of the selected type.
	 * 
	 * @param option
	 *            shall be OPTION_KEYSTORE, or OPTION_TRUSTSTORE
	 * @return a KeyStore handle.
	 * @exception InvalidQualifierException
	 *                if bad option.
	 * @exception KeyStoreException
	 *                if bad type
	 * @exception NoSuchProviderException
	 *                if the provider default is wrong.
	 */
	private KeyStore getKeyStoreInstance(final int option) throws InvalidQualifierException, KeyStoreException, NoSuchProviderException {
		String keyStoreType = null;

		if (option == OPTION_KEYSTORE) {
			keyStoreType = url.getValue(KEYSTORE_TYPE);
		} else if (option == OPTION_TRUSTSTORE) {
			keyStoreType = url.getValue(TRUSTSTORE_TYPE);
		} else {
			throw (new InvalidQualifierException("XTS Internal error in getKeyStore(" + option + ") for url " + url.toString(true)));
		}

		if (keyStoreType == null) {
			if (OPTION_KEYSTORE == option) {
				keyStoreType = System.getProperty(KEYSTORE_TYPE_PROPERTY);
			} else if (OPTION_TRUSTSTORE == option) {
				keyStoreType = System.getProperty(TRUSTSTORE_TYPE_PROPERTY);
			}

			if (keyStoreType == null) {
				keyStoreType = KeyStore.getDefaultType();
			}

		} else if (JKS_KEYSTORE.compareToIgnoreCase(keyStoreType) != 0 && PKCS12_KEYSTORE.compareToIgnoreCase(keyStoreType) != 0) {
			String type = null;

			if (option == 1) {
				type = KEYSTORE_TYPE;
			} else {
				type = TRUSTSTORE_TYPE;
			}
			throw (new InvalidQualifierException(type + "= unsupported as it is in url " + url.toString(true)));
		}

		return KeyStore.getInstance(keyStoreType);

	}

	/**
	 * This method retrieves the XTS SSL KEYSTORE parameter information from a
	 * url, or its related system property information and loads the file
	 * containing the keystore object information in the passed KeyStore handle.
	 * 
	 * @param ks
	 *            is the KeyStore handle passed by the caller.
	 * @param passwd
	 *            is the KeyStore password
	 * @return a handle to a String containing the name of the keystore file.
	 * @exception FileNotFoundException
	 *                thrown if the keystore file could not be found.
	 * @exception SecurityException
	 *                thrown if the keystore file could not be read.
	 * @exception IOException
	 *                thrown if there is an I/O or format problem with the
	 *                keystore data.
	 * @exception NoSuchAlgorithmException
	 *                thrown if the algorithm used to check the integrity of the
	 *                keystore cannot be found.
	 * @exception CertificateException
	 *                thrown if any of the certificates in the keystore could
	 *                not be loaded.
	 */
	private String loadKeyStore(final KeyStore ks, final char[] passwd) throws FileNotFoundException, SecurityException, IOException, NoSuchAlgorithmException, CertificateException {
		String keyStoreFile = null;
		FileInputStream ksFileInputStream = null;

		keyStoreFile = url.getValue(KEYSTORE);
		if (keyStoreFile == null) {
			keyStoreFile = System.getProperty(KEYSTORE_PROPERTY);
		}

		if (keyStoreFile != null) {
			// May throw FileNotFounfException, SecurityException
			ksFileInputStream = new FileInputStream(keyStoreFile);
		}

		// May throw IOException, NoSuchAlgorithmException, CertificateException
		ks.load(ksFileInputStream, passwd);
		if (ksFileInputStream != null) {
			ksFileInputStream.close();
		}
		return keyStoreFile;
	}

	/**
	 * This method retrieves the XTS SSL TRUSTSTORE parameter information from a
	 * url, or its related system property information and loads the file
	 * containing the keystore object information in the passed KeyStore handle.
	 * Please note that a TrustStore is a KeyStore Object! However a TrustStore
	 * objects only cares about Certificates, although that a KeyStore object
	 * may contain Keywords and Certificates.
	 * 
	 * @param ks
	 *            is the KeyStore handle passed by the caller.
	 * @param passwd
	 *            is the KeyStore password
	 * @return a handle to a String containing the name of the truststore file.
	 * @exception FileNotFoundException
	 *                thrown if the keystore file could not be found.
	 * @exception SecurityException
	 *                thrown if the keystore file could not be read.
	 * @exception IOException
	 *                thrown if there is an I/O or format problem with the
	 *                keystore data.
	 * @exception NoSuchAlgorithmException
	 *                thrown if the algorithm used to check the integrity of the
	 *                keystore cannot be found.
	 * @exception CertificateException
	 *                thrown if any of the certificates in the keystore could
	 *                not be loaded.
	 */
	private String loadTrustStore(final KeyStore ks, final char[] passwd) throws FileNotFoundException, SecurityException, IOException, NoSuchAlgorithmException, CertificateException {
		String trustStoreFile = null;
		FileInputStream ksFileInputStream = null;
		String javaHome = null;
		String fileSeparator = null;
		String jreLibSecurity = null;
		String defaultTrustStore = null;

		/* Frist attempt using XTS TRUSTSTORE= in XTSurl */
		trustStoreFile = url.getValue(TRUSTSTORE);

		/* If the above failed, then attempt the java property */
		if (trustStoreFile == null) {
			trustStoreFile = System.getProperty(TRUSTSTORE_PROPERTY);
		}

		/* If the above failed, then attempt java-home/lib/security/jssecacerts */
		if (trustStoreFile == null) {
			javaHome = System.getProperty("java.home");
			if (null != javaHome) {
				fileSeparator = System.getProperty("file.separator");
				if (null != fileSeparator) {
					jreLibSecurity = javaHome + fileSeparator + "lib" + fileSeparator + "security" + fileSeparator;
					defaultTrustStore = jreLibSecurity + "jssecacerts";
					try {
						ksFileInputStream = new FileInputStream(defaultTrustStore);
						trustStoreFile = defaultTrustStore;
					} catch (FileNotFoundException e) {
						/* Attempt java-home/lib/security/cacerts */
						defaultTrustStore = jreLibSecurity + "cacerts";
						try {
							ksFileInputStream = new FileInputStream(defaultTrustStore);
							trustStoreFile = defaultTrustStore;
						} catch (FileNotFoundException e2) { /* No possible default */
						}
					}
				}
			}
		}

		if (null != trustStoreFile) {
			// May throw FileNotFounfException, SecurityException
			ksFileInputStream = new FileInputStream(trustStoreFile);
		}
		// May throw IOException, NoSuchAlgorithmException, CertificateException
		ks.load(ksFileInputStream, passwd);

		return trustStoreFile;
	}

	/**
	 * This method retrieves the XTS SSL VERSION parameter information from a
	 * url, converts is information to the related SSL version and obtain an
	 * SSLContext instance.
	 * 
	 * @return a handle to a SSLContext.
	 * @exception InvalidQualifierException
	 *                thrown if version= names an unsupported XTS SSL version .
	 * @exception NoSuchAlgorithmException
	 *                thrown if the specified protocol is not available from the
	 *                specified provider.
	 * @exception NoSuchProviderException
	 *                thrown if the specified provider has not been configured
	 */
	private SSLContext getSSLContext() throws InvalidQualifierException, NoSuchAlgorithmException, NoSuchProviderException {
		String version = null;
		String protocol = null;

		version = url.getValue(VERSION_PARM);
		if (version == null) {
			protocol = TLSv1;
		} else if (VERSION_0.compareTo(version) == 0) {
			protocol = TLSv1;
// no other version is supported anymore
//		} else if (VERSION_3.compareTo(version) == 0) {
//			protocol = SSLv3;
		} else {
			throw (new InvalidQualifierException("VERSION= unsupported as it is in url " + url.toString(true)));
		}

		return SSLContext.getInstance(protocol, DEFAULT_PROVIDER_STRING);
	}

}
