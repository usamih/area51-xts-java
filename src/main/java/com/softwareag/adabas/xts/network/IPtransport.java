/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.network;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.softwareag.adabas.xts.Message;
import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTSException;
import com.softwareag.adabas.xts.XTSoutputStream;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.directory.DefaultDirectory;
import com.softwareag.adabas.xts.helpers.Status;
import com.softwareag.adabas.xts.interfaces.IConnectCallback;
import com.softwareag.adabas.xts.interfaces.IConnection;
import com.softwareag.adabas.xts.interfaces.IDriver;
import com.softwareag.adabas.xts.interfaces.IReceiveCallback;
import com.softwareag.adabas.xts.interfaces.ITransmitCallback;

//-----------------------------------------------------------------------
/**
 * This class abstracts for all protocol drivers using TCP/IP.
 ** 
 * @version 2.1.1.2
 **/
public abstract class IPtransport extends Thread implements IDriver {
	public static final String className = "IPTransport";

	/** The default re-connect retry interval **/
	public static final long DEFAULT_RETRY_INTERVAL = 60000;

	/** The default re-connect retry attempt count **/
	public static final int DEFAULT_RETRY_COUNT = 0;

	protected static final IOException stopped = new IOException("stopped");
	protected static final ThreadDeath death = new ThreadDeath();

	protected static int txCnt = 0; // transmit thread counter [0006]
	protected static int rxCnt = 0; // receive thread counter [0006]
	protected static int listenCnt = 0; // listen thread counter [0006]
	protected static int connectCnt = 0; // connect thread counter [0006]

	protected Vector<Thread> threads; // list of threads in this protocol
	protected String protocol = "tcpip"; // protocol name
	protected XTSurl url; // connected to?
	protected boolean listener = false; // true if listening
	protected IConnectCallback callback; // where to call back (connect)
	protected Object userval; // connect cb user value
	protected Status status = new Status(); // the status of this thread
	protected long retry_interval; // retry interval
	protected int retry_count = 0; // retry count
	protected boolean reconnect; // reconnect if disconnect
	protected int xtsConnectTimeout = 0;
	
	protected ServerSocket lsocket; // for server
	protected Vector<Thread> children = new Vector<Thread>(); // list of child
	// threads
	// public boolean trace_all=false; // trace all connections [0003]
	protected byte ttl; // time-to-live
	protected byte priority; // priority
	protected boolean ttl_set; // ttl value given;
	protected boolean priority_set; // priority value given;
	protected int MsgType = 0; // 1 is 'adi' mode, 2 is 'raw' mode
	protected boolean shutdownRequest = false;
	private IConnection connection = null;

	private boolean threadSuspended = false;

	protected IPtransport(final Vector<Thread> threads) {
		this.threads = threads;
	}

	protected IPtransport(final Vector<Thread> threads, final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval,
			final int retry_count, final boolean reconnect, final int connTo) {
	
		this(threads, url, listen, callback, userval, retry_interval, retry_count, reconnect);
		this.xtsConnectTimeout = connTo;
	}
	
	protected IPtransport(final Vector<Thread> threads, final XTSurl url, final boolean listen, final IConnectCallback callback, final Object userval, final long retry_interval,
			final int retry_count, final boolean reconnect) {
		this.url = url;
		this.listener = listen;
		this.callback = callback;
		this.userval = userval;
		this.retry_interval = retry_interval;
		this.retry_count = retry_count;
		this.reconnect = reconnect;
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Init IPtransport " + callback);

		ttl_set = true;
		try {
			ttl = Byte.parseByte(url.getValue("ttl"));
		} catch (Exception e) {
			ttl_set = false;
		}

		priority_set = true;
		try {
			priority = Byte.parseByte(url.getValue("priority"));
		} catch (Exception e) {
			priority_set = false;
		}

		if (url.getValue("adi") != null) {
			MsgType = 1;
		}
		else if (url.getValue("raw") != null) {
			MsgType = 2;
		}

		try {
			this.retry_count = Integer.parseInt(url.getValue("retry"));
		} catch (Exception e) {
		}

		try {
			this.retry_interval = Integer.parseInt(url.getValue("retryint")) * 1000L;
		} catch (Exception e) {
		}

		try {
			this.reconnect = url.getValue("reconnect").equalsIgnoreCase("on");
		} catch (Exception e) {
		}
        
		status.url = url;
		// status.trace=trace_all;

		// try { status.trace=url.getValue("trace").equalsIgnoreCase("on"); }
		// catch(Exception e) {}

		this.threads = threads;
		threads.addElement(this);
		setDaemon(true);
		// start();
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
	public abstract IDriver listen(XTSurl url, IConnectCallback callback, Object userval);

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
	public abstract IDriver connect(XTSurl url, IConnectCallback callback,	Object userval);

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
	 ** @param reconnect
	 *            set to true if the driver should try to re-establish a
	 *            connection which has been severed.
	 ** @return an instance of the protocol driver.
	 **/
	public abstract IDriver connect(XTSurl url, IConnectCallback callback, Object userval, long retry_interval, int retry_count, boolean reconnect);

	/**
	 * Create a Server Socket.
	 ** 
	 ** @return an instance a ServerSocket.
	 **/
	protected ServerSocket createServerSocket() throws IOException {
		String fingerprint = className + " CreateServerSocket";
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(fingerprint);
		return (new ServerSocket(url.getPort()));
	}

	/**
	 * Create a Client Socket.
	 ** 
	 ** @return an instance a ServerSocket.
	 **/
	protected Socket createClientSocket() throws Exception {
		Socket socket = null;
	
		String fingerprint = className + " CreateClientSocket";
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(fingerprint);
		InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
		for (InetAddress inetadr : addresses) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("INET " + inetadr.getCanonicalHostName() + " "	+ inetadr.getClass().getCanonicalName());
		}		
		if (xtsConnectTimeout == 0) {
			socket = new Socket(addresses[0], url.getPort());
		} else
		{
			socket = new Socket();
			socket.connect(new InetSocketAddress(addresses[0], url.getPort()), xtsConnectTimeout);
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(fingerprint + "=" + socket);
		return socket;
	}

	// -----------------------------------------------------------------------
	/**
	 * The actual code executed by the thread. A listener blocks on the accept,
	 * and starts a receive and a transmit thread on successful completion. A
	 * connector connects to the given URL. If successful, it also creates the
	 * two threads, then exits. If unsuccessful, it waits "retry_time" before
	 * trying again. It will try again "retry count" times.
	 **/
	// -----------------------------------------------------------------------
	public final void run() {
		if (listener) { 
//			setName("listen(" + url.toString(XTSurl.HOST_ADDRESS_FORMAT) + ")");
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Run listener-" + (listenCnt+1) + " Url=" + url.toString(XTSurl.HOST_ADDRESS_FORMAT));
			try {
				setName("Listen-" + ++listenCnt);
			} catch (SecurityException se) {
			}
			try { 
				Socket s = null;
				lsocket = createServerSocket();
				status.setStatus("Listening on port=" + url.getPort());
				for (;;) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Listen on port " + url.getPort());
					s = lsocket.accept();
					XTStrace.verbose("Listen event on port=" + url.getPort() + " New Socket=" + s);
					start_threads(s);
				}
			} catch (IOException e) {
				status.setStatus("Listener " + e.toString());
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Listen Failure (" + url.toString() + ") " + e.getMessage());
			} finally {
				try {
					lsocket.close();
				} catch (Exception xx) {
				}
			}

		} else {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Run connect-" + (connectCnt + 1) + " Url=" + url.toString(XTSurl.HOST_ADDRESS_FORMAT));
			try {
//				setName("IPt-connect("+ url.toString(XTSurl.HOST_ADDRESS_FORMAT) + ")-"+ ++connectCnt);
				setName("IPt-connect-" + ++connectCnt);
			} catch (SecurityException se) {
			}
			for (;;) {
				try {
					status.setStatus("Connecting to " + url.getHost() + ":"	+ url.getPort());
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Connecting to " + url);
					start_threads (createClientSocket());
					if (reconnect) {
						status.setStatus("Connector suspended");
						waitThread();
						try {
							sleep(retry_interval);
						} catch (InterruptedException ie) {
						}
						if (listener) {
							break; // must exit
						}
					} else {
						status.setStatus("Connector complete");
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Connector terminating....");
						break;
					}
					/**
					 * Delete the target from the cache
					 * Types of failures: Connection refused
					 */
				} catch (Exception e) {
					Exception ex = null;
					String target = null;
					if (url.partition != null)
						target = url.partition + url.target;
					else
						target = url.target;
					DefaultDirectory.deleteTargetFromCache(target);
					status.setStatus("Connector " + e.toString());
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Connect fail (" + url + ") " + e.getMessage());
					if (retry_count != 0 && !listener) {
						try {
							retry_count--;
							sleep(retry_interval);
						} catch (InterruptedException xx) {
						}
						if (listener) {
							break;
						}
					} else {
						if (callback != null) {
							if (e.getClass().equals(UnknownHostException.class))
								ex = new XTSException("Unknown Host Exception : " + url.getHost(), XTSException.XTS_UNKNOWN_HOST_EXCEPTION);
							else if (e.getClass().equals(ConnectException.class))
								ex = new XTSException(XTSException.XTS_CONNECT_FAILED);
							else if (e.getClass().equals(SocketTimeoutException.class))
								ex = new XTSException(XTSException.XTS_TIMEOUT);
							else
								ex = e;							
							this.callback.disconnected(url, userval, ex, connection);
						}
						break;
					}
				}
			}
		}
		threads.removeElement(this);
		synchronized (XTS.shutLock) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Listen Thread Notify shutlock");
			XTS.shutLock.notify();
		}
		if (XTStrace.bGlobalVerboseEnabled) {
			if (listener)
				XTStrace.verbose("Running listen throw death");
			else
				XTStrace.verbose("Running connect throw death");
		}
		return;
// removed by mihai		throw death;
	}

	// -----------------------------------------------------------------------
	/** Stop the driver entirely. Closes all connections. **/
	// -----------------------------------------------------------------------
	public final void stopAll() {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("stopAll: Stopping all threads");
		synchronized (threads) {
			Enumeration<Thread> e = threads.elements();
			while (e.hasMoreElements()) {
				// stop all parents
				((IPtransport) e.nextElement()).stopit();
			}
			threads.removeAllElements();
		}
	}

	// -----------------------------------------------------------------------
	/** Stop the instance from listening or connecting. **/
	// -----------------------------------------------------------------------
	public final void stopit() {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Stopit listening");
		if (listener) {
			try {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Stopit listening close socket=" + lsocket);
				lsocket.close();
			} catch (Exception ex) {
			}
		} else {
			listener = true; // disallow any restart
		}

		Enumeration<Thread> e = children.elements();
		while (e.hasMoreElements()) {
			IPtx x = (IPtx) e.nextElement();
			// x.stop(stopped);
			// x.twin.stop(stopped);
			x.close();
		}
//	removed by mihai	resumeThread(); // for connectors
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Stopit listening issue interrupt ....");
		interrupt(); // it may be sleeping
	}

	// -----------------------------------------------------------------------
	// Start a transmit thread and a receive thread.
	// -----------------------------------------------------------------------
	private final void start_threads(final Socket s) throws IOException {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Start IPrx and IPtx threads");
		s.setSoLinger(false, 0); 
		s.setTcpNoDelay(true);
		IPtx tx = new IPtx(s);
		children.addElement(tx);
		IPrx rx = new IPrx(s, tx);
		tx.twin = rx;
		// save driver url in tx status object. Used in chirp processing [0012]
		tx.status.driverUrl = this.url; 
		status.messagesIn++;
		if (callback != null) {
			if (XTStrace.bGlobalVerboseEnabled) 
                       		XTStrace.verbose("Calling callback.connected ....");
			XTSurl x = new XTSurl(protocol, s.getInetAddress().getHostName(), s.getPort());
			callback.connected(x, userval, tx);
		}
		rx.start(); // rx thread not started yet
		tx.start(); // tx thread not started yet
		connection = tx;
	}

	// -----------------------------------------------------------------------
	// Remove a child from the list. The threads Vector is used for
	// monitoring of the status of the threads.
	// -----------------------------------------------------------------------
	final void retire(final Thread x) {
		children.removeElement(x);
	}

	final void retire (final IPrx x, final IPtx y) {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Retire IPrx IPtx closing socket=" + x.socket);
		try {
			x.socket.close();
		} catch (Exception e) {
		}
		retire(x);
		retire(y);
		if (!listener && reconnect) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Retire ... reconnecting");
			resumeThread(); 
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Exit Retire IPrx IPtx ");
	}

	private void resumeThread() {
		synchronized (this) {
			threadSuspended = !threadSuspended;
			if (!threadSuspended) {
				notify();
			}
		}
	}

	private void waitThread() {
		try {
			synchronized (this) {
				while (threadSuspended) {
					wait();
				}
			}
		} catch (InterruptedException ie) {
		}
	}

	// -----------------------------------------------------------------------
	/**
	 * Return status of the thread. Used for monitoring.
	 ** 
	 * @return a Status object, representing the status of the thread.
	 **/
	// -----------------------------------------------------------------------
	public final Status getStatus() {
		return status;
	}

	// -----------------------------------------------------------------------
	/**
	 * Return the Enumerator of driver threads, that includes all connector,
	 * listen, transmit and receive threads.
	 **/
	// -----------------------------------------------------------------------
	public final Enumeration<Thread> getThreads() {
		return threads.elements();
	}

	// -----------------------------------------------------------------------
	/** Return a vector of Connections. **/
	// -----------------------------------------------------------------------
	public final Enumeration<Thread> getConnections() {
		return children.elements();
	}

	// -----------------------------------------------------------------------
	/**
	 * Return listener value. Used for monitoring.
	 ** 
	 * @return listener value. True indicates a listen thread, False a connect
	 *         thread.
	 **/
	// -----------------------------------------------------------------------
	public final boolean isListener() {
		return listener;
	} 

	// make a stack trace string
	final String strace(final Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose(sw.toString());
		return sw.toString();
	}

	// retrieve an Int from a byte array
	protected static final int getint(final byte[] b, final int position) {
		int pos = position;
		return ((b[pos++] & 0xff) << 24) | ((b[pos++] & 0xff) << 16) | ((b[pos++] & 0xff) << 8) | (b[pos] & 0xff);
	}

	// ----------------------------------------------------------------------
	/** get a new Object for thread **/
	// ----------------------------------------------------------------------
	protected abstract Object getToken(IPtx tx) throws IOException;

	// ----------------------------------------------------------------------
	/** Transmit a message **/
	// ----------------------------------------------------------------------
	protected abstract void transmit(Message p, IPtx tx) throws IOException;

	// ----------------------------------------------------------------------
	/** Receive a message **/
	// ----------------------------------------------------------------------
	protected abstract Message receive(IPrx rx) throws IOException, XTSException, Exception;

	// ----------------------------------------------------------------------
	/** Close down **/
	// ----------------------------------------------------------------------
	protected abstract void shut(IPtx tx) throws IOException;

	// ----------------------------------------------------------------------
	/** Transmit thread. **/
	// ----------------------------------------------------------------------
	public class IPtx extends Thread implements IConnection {
		public IPrx twin; // receive twin
		Message transmit_head = null; // transmit queue
		Message transmit_tail = null; // transmit queue
		public XTSoutputStream dos; // output stream
		public Status status = new Status(); // status
		boolean connected = true; // connected
		public boolean running = true; // connected
		boolean crashed = true; // only if real exception
		public Object token = null; // protocol info
		String from; // for security
		public Socket socket; // [0005]
		private AtomicBoolean free = new AtomicBoolean(true);
		private AtomicInteger freeCounter = new AtomicInteger(0);
		private AtomicInteger usage = new AtomicInteger(0);
		private boolean closed = false;

		// ----------------------------------------------------------------------
		/** Constructor - allocates output stream and starts the thread. **/
		// ----------------------------------------------------------------------
		IPtx(final Socket s) throws IOException {
			super("Send" + "-" + ++txCnt); 
			dos = new XTSoutputStream(s.getOutputStream(), 4096);
			from = s.getInetAddress().getHostAddress();
			status.url = new XTSurl(url.target, protocol, s.getInetAddress().getHostName(), s.getPort()); 
			setDaemon(true);
			socket = s; // save socket for tracing purposes 
		}

		// ----------------------------------------------------------------------
		/**
		 * Actual transmit thread execution. The thread waits inside a
		 * synchronized block. It will be Notified by another thread when data
		 * is to be sent. It retrieves messages from its queue and sends them
		 * until the queue is empty, then it goes back to sleep. If there is an
		 * error in the thread, then the twin is also stopped and if the parent
		 * was a connector it is resumed so that the connection can be
		 * re-established.
		 **/
		// ----------------------------------------------------------------------
		public final void run() { 
			String oldName = getName();
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Running transmit name=" + oldName);
			try {
//				setName("IPtx-" + status.url.toString(XTSurl.HOST_ADDRESS_FORMAT) + ")-" + oldName);
				setName("IPtx-" + oldName);
			} catch (SecurityException se) {
			}
			Message p = null;
			try {
				token = getToken(this); // get a token for this
				status.setStatusOut("sendwait");
				while (true) {
					synchronized (this) {
						while (transmit_head == null && running) {
							try {
								wait();
							} catch (InterruptedException ie) {
							}
						}
						if (!running) {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("Running Transmit Close connection was called for " + socket);
							break;
						} 
						p = transmit_head;
						transmit_head = p.nextMessage;
						if (p.nextMessage == null) {
							transmit_tail = null;
						}
					}
					/*
					 * It is necessary to synchronize on the message
					 * being transmitted. The reply could come back and
					 * be processed before all of the transmit logic
					 * completes and free the message while the transmit
					 * thread is still using it. This occurs with
					 * SendAndWait, where a reply is expected.
					 */
					synchronized (p) {
						// added to be compatible with C XTS which uses this field for streaming
						p.msgno = 0;
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Running Transmit message " + p.toString() + " p.route=" + p.route);
// reduce trace					if (XTStrace.bGlobalDebugEnabled) 
//							XTStrace.dump("Transmit messge", "run", p.body, p.length, true);
						if (p.callback == null) {
							transmit(p, this);
						} else {
							/*
							 * We need to synchronize on the XTS object.
							 * This same objet [0022] will be sync'd on during
							 * transmission. On a dyadic processor the
							 * receive completed and free'd this object
							 * before the send completed all of it's processing.
							 * Once free'd the object could be
							 * reallocated. When reallocated the rcb
							 * field may be initialized to null before the send
							 * logic checks this field. If the field is
							 * null, the send logic free's the XTS
							 * object. This causes the same object to be
							 * free'd twice.
							 */
							synchronized (p.callback) {
								transmit(p, this);
								p.callback.transmitted(this, p, p.userval);
							}
						}
						status.messagesOut++;
						status.bytes_out += p.length;
						// If route not equal 0(no reply expected),
						// then hold onto the message, so if it's timed out,
						// it can be returned to user
						if (XTStrace.bGlobalVerboseEnabled) 
						if (p.route == 0) {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("Running Transmit message " + p.toString() + " p.route= 0 Delete message set");
							p.freeMessage("Send thread");
						}
					}
					p = null;
				}
				crashed = false;
			} catch (IOException e) {
				status.setStatusOut("Run Transmit " + e.toString());
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Run Transmit " + socket + " " + e);
			} catch (Exception e) {
				status.setStatusOut("Run Transmit " + IPtransport.this.strace(e));
				if (XTStrace.bGlobalErrorEnabled) 
					XTStrace.error(e);
			} finally {
				connected = false;
				if (p != null) {
					if (p.callback != null) {
						p.callback.transmitFailed(this, p, p.userval);
					} 
					else {
						p.freeMessage("run callback null");
					}
				}
				while (transmit_head != null) {
					synchronized (this) {
						p = transmit_head;
						transmit_head = p.nextMessage;
						if (p.nextMessage == null) {
							transmit_tail = null;
						}
					}
					if (p.callback != null) { // [0017]
						p.callback.transmitFailed(this, p, p.userval);
					} 
					else {
						p.freeMessage("run callback null transmit");
					}
				}
				try {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Running Transmit shutdown " + this);
					shut(this);
				} catch (Exception ff) { }
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Running Transmit retire " + this);
				IPtransport.this.retire(twin, this);
				if (crashed) {
					crashed = false;
					twin.interrupt();
					// twin.stop(stopped);
				}
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Running transmit throw death");
			return;
//removed by mihai			throw death;
		}

		// ----------------------------------------------------------------------
		/**
		 * Queue a message for sending. If it is the first message in the queue,
		 * then the transmit thread is Notified.
		 ** 
		 * @param p
		 *            the message to send.
		 ** @param callback
		 *            represents the method to call back on send completion or
		 *            failure. May be set to <i>null</i>, in which case no
		 *            callback occurrs.
		 ** @param userval
		 *            an object to pass to the callback routine.
		 ** @return true if sending not possible.
		 **/
		// ----------------------------------------------------------------------
		public final boolean send(final Message p, final ITransmitCallback callback, final Object userval) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("IPtx send TransmitCallback:" + callback);
			if (!connected) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("IPtx send not connected exiting");
				return true;
			}
			p.callback = callback;
			p.userval = userval;
			p.nextMessage = null;
			if (p.isFresh) { // set TTL and PRIORITY
				if (ttl_set) {
					p.ttl = ttl; // ... if available
				}
				if (priority_set) {
					p.priority = IPtransport.this.priority;
				}
			}

			synchronized (this) {
				if (transmit_head == null) {
					transmit_head = p;
					transmit_tail = p;
					notify();
				} else {
					transmit_tail.nextMessage = p;
					transmit_tail = p;
				}
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("IPtx send exiting");
			return false;
		}

		// -----------------------------------------------------------------------
		/** Return status of the thread. Used for monitoring. **/
		// -----------------------------------------------------------------------
		public final Status getStatus() {
			return status;
		}

		// -----------------------------------------------------------------------
		/**
		 * Return user value (for receiving).
		 ** 
		 * @return the user object passed to the <i>Listen</i> or <i>Receive</i>
		 *         methods.
		 **/
		// -----------------------------------------------------------------------
		public final Object getUserValue() {
			return twin.userval;
		}

		// -----------------------------------------------------------------------
		/**
		 * Set receive parameters.
		 ** 
		 * @param callback
		 *            represents the method to call back on receipt of a
		 *            message. May be set to <i>null</i>, in which case no
		 *            callback occurrs.
		 ** @param uservalue
		 *            an object to present to the callback method.
		 **/
		// -----------------------------------------------------------------------
		public final void receive(final IReceiveCallback callback, final Object uservalue) {
			twin.callback = callback;
			twin.userval = uservalue;
		}

		// -----------------------------------------------------------------------
		/** Close a connection. **/
		// -----------------------------------------------------------------------
		public final void close() {
			running = false;
			closed = true;
			synchronized (this) {
				notify();
			}
		}

		// -----------------------------------------------------------------------
		/** Print a pretty picture of it. **/
		/** Sleep for a bit. **/
		// -----------------------------------------------------------------------
		public final void doze(final String s) {
			status.setStatus(s);
			try {
				wait(60000);	// wait for action
			} catch (InterruptedException ie) {
			} // assume it happened
		}

		@Override
		public final boolean setLock() {
			boolean set = free.weakCompareAndSet(true, false);
			freeCounter.incrementAndGet();
			return set;
		}

		@Override
		public final boolean isFree() {
			return free.get();
		}

		@Override
		public final void setFree() {
			freeCounter.decrementAndGet();
			if (freeCounter.get() < 1) {
				free.set(true);
			}
		}

		public final boolean isClosed() {
			return closed;
		}

		@Override
		public final int increaseUsage() {
			return usage.incrementAndGet();
		}

		@Override
		public final int releaseUsage() {
			return usage.decrementAndGet();
		}

		@Override
		public final int usage() {
			return usage.get();
		}

		public final XTSurl getUrl() {
			return url;
		}

	}

	// ----------------------------------------------------------------------
	/** Receive thread. **/
	public// ----------------------------------------------------------------------
	class IPrx extends Thread {
		public IPtx twin = null; // transmit twin of this
		public DataInputStream dis;
		Socket socket;
		public IReceiveCallback callback = null;
		public Object userval;

		// ----------------------------------------------------------------------
		/** Constructor. Creates an Input stream and starts the thread. **/
		// ----------------------------------------------------------------------
		protected IPrx(final Socket s, final IPtx twin) throws IOException {
			super("Receive" + "-" + ++rxCnt);
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Receive thread constructor rxCnt=" + rxCnt + " Socket=" + s);
			socket = s;
			this.twin = twin;
			dis = new DataInputStream(new BufferedInputStream(s.getInputStream(), 4096));
			setDaemon(true);
		}

		/**
		 * The actual receive thread. It blocks receiving a message. When a
		 * message is received, it tries to route the message to the correct
		 * place. If any error occurrs, the corresponding transmit thread is
		 * stopped also. If the parent task is a connector, then it is resumed
		 * so that the connection can be re-established.
		 */
		public final void run() { 
			try {
			String oldName = getName();
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Running receive thread name=" + oldName);
			try {
//				setName("IPrx-"	+ twin.status.url.toString(XTSurl.HOST_ADDRESS_FORMAT)	+ ")-" + oldName);
				setName("IPrx-" + oldName);
			} catch (SecurityException se) {
			}
			twin.status.setStatus("rcvwait");
			Exception ee = null;
			try {
				for (;;) {
					XTStrace.verbose("Receive thread waiting ....");
					Message p = receive(this);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Receive message " + p.toString()); 
// reduce trace				XTStrace.dump("IPrx messge", "run", p.body, p.length, true);
					p.from = twin.from;
// reduce trace				Message.dump("IPRx Receive", "run", p);
					twin.status.messagesIn++;
					twin.status.bytesIn += p.length;

					if (callback != null) {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Message received callback:" + callback);
						if (callback instanceof XTS) {
							if (XTStrace.bGlobalVerboseEnabled) {
								XTStrace.verbose("callback=" + callback);
								XTStrace.verbose("wait_for callback.wait_for=" + ((XTS) callback).getWaitFor());
							}
						}
						callback.received(twin, p, userval); // up to client to free msg
					} else {
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("Message lost");
						p.freeMessage("Message lost");
					}
				}
			} catch (SocketException e) {
				ee = e;
				twin.status.setStatus("Receive SocketException " + e.getMessage());
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Receive SocketException in IPtransport" + e.getMessage());
			} catch (IOException e) {
				ee = e;
				twin.status.setStatus("Receive " + e.toString());
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Receive IOException in IPtransport " + e);
			} catch (XTSException e) {
				ee = e;
				twin.status.setStatus("Receive " + e.toString());
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Receive XTSException  in IPtransport" + e);
			} catch (Exception e) {
				ee = e;
				twin.status.setStatus("Receive Exception " + IPtransport.this.strace(e));
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("Receive Exception in IPtransport" + e);
			} finally {
				synchronized (XTS.shutLock) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Running receive got shutlock - retiring");
					if (callback != null) {
						callback.connectionLost(twin, userval);
					}
					if (IPtransport.this.callback != null) {
						IPtransport.this.callback.disconnected(twin.status.url,	IPtransport.this.userval, ee, IPtransport.this.connection);
					}
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("Running receive got shutlock - retiring");
					IPtransport.this.retire(this, twin);
					if (twin.crashed) {
						twin.close();
					}
					XTS.shutLock.notify(); 
				} 
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("Running receive throw death");
			return;
//removed by mihai	throw death;
		} catch (Throwable t){
			t.printStackTrace(); // this would require redirect output like java -jar my.jar >> error_log.txt
			// try clean up and exit jvm with System.exit(0);
		}
		}

		// -----------------------------------------------------------------------
		/** Return status of the thread. Used for monitoring. **/
		// -----------------------------------------------------------------------
		public final Status getStatus() {
			return twin.status;
		}

		// -----------------------------------------------------------------------
		/** Wake up the transmit thread. **/
		// -----------------------------------------------------------------------
		public final void rouse() {
			synchronized (twin) {
				twin.notify();
			}
		}

		// -----------------------------------------------------------------------
		/** Return KEEP ALIVE for given DBID **/
		// -----------------------------------------------------------------------
		public final void do_chirp(final int dbid, final int timeout, final byte[] chirp_body) {
			Message p = Message.newMessage(18); // make a "keep alive"
			p.target = XTS.CHIRP;
			p.ttl = 16;
			p.route = dbid;
			p.timeout = timeout; // time to next chirp
			System.arraycopy(chirp_body, 0, p.body, 1, 16);
			p.body[16] = 0;
			p.body[17] = 2; // name length
			p.hdrlen = 28; // set header length
			// if(status.trace)
			if (XTStrace.isGlobalDebugEnabled()) {
				p.putHeader();
				Message.dump("IPtransport", "do_chirp", p);
			}
			callback.received(twin, p, userval);
		}
	}
} 
