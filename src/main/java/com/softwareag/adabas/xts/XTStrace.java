/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/* Project started at May 2006 (initial by Thorsten Knieling) */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.softwareag.adabas.xts.common.CommonTrace;
import com.softwareag.adabas.xts.common.ICommonTrace;

/**
 * Handle trace output. Handle internal trace or external log4j usage.
 * 
 * @author tkn, usamih
 * System or environment variable:
 * XTSTRACE - 65535 or 0xffff
 * XTSDIR - where the trace goes. If not defined, it goes locally ("./")
 * XTS_ENABLE_LOG4J - log4j parameters; If not defined or NO, trace goes to XTSDIR; 
 *                    Other values: ALL, DEBUG, INFO, WARN, ERROR, FATAL, OFF, and TRACE
 * MAXSIZE - max trace file size
 * 
 */
public final class XTStrace extends CommonTrace {

	private static ConcurrentHashMap<String, ICommonTrace> traceHash = new ConcurrentHashMap<String, ICommonTrace>();

	// private volatile boolean traceInit = false;
	private static AtomicInteger instanceNr = new AtomicInteger(1);

	private static 	ICommonTrace tracehandle = null;

	static {
		initEnvironment("XTSDIR", "XTSTRACE", "XTS_ENABLE_LOG4J", "LOGLEVEL");
	}

	public XTStrace(final String typeName) {
		super();
		StringBuffer prefix = new StringBuffer();

                // If Log4j is available, do not create the XTS trace;
		if ((isLog4jAvailable()) && (getLogger() != null)) {
                    return;
		}
		if ((isLog4jAvailable()) && (getLogger() == null)) {
			initLogger(getClass().getName());
	                return;
		}
		prefix.append("jaix");
		if (typeName != null) {
			prefix.append('-');
			prefix.append(typeName);
		}
		instance = new String[] { prefix.toString(), "XTS " + " v" + XTSversion.VERSION };
		initOutput();
		currentInstance = instanceNr.getAndIncrement();
	}

	/**
	 * Get thread-specific trace instance. If trace instance is not available,
	 * create new one. Store instances in HashMap.
	 * 
	 * @return AdabasTrace instance
	 */
	public static ICommonTrace getTracer() {
		return initTracer(null);
	}

	/**
	 * Get thread-specific trace instance. If trace instance is not available,
	 * create new one. Store instances in HashMap.
	 * 
	 * @return AdabasTrace instance
	 */
	public static ICommonTrace initTracer(final String typeName) {
		String threadName = Thread.currentThread().getName();

		ICommonTrace xt = traceHash.get(threadName);
		if (xt == null) {
			xt = new XTStrace(typeName);
			traceHash.put(threadName, xt);
		}
		return xt;
	}

	public static void fatal(final String msg) {
		if (bGlobalFatalEnabled) {
			getTracer().debug(null, msg);
		}
	}               
	public static void error(final String msg) {
		if (bGlobalErrorEnabled) {
			getTracer().error(null, msg);
		} 
	}
	public static final void error(final Throwable x) {
		if (bGlobalErrorEnabled) {
			StringWriter sw = new StringWriter();
			x.printStackTrace(new PrintWriter(sw));
			String s = sw.toString();
			getTracer().error(null, s);
		}
	}
	public static void warn(final String msg) {
		if (bGlobalWarnEnabled) {
			getTracer().warn(null, msg);
		}
	}               
	public static void info(final String msg) {
		if (bGlobalInfoEnabled) {
			getTracer().info(null, msg);
		}
	}               
	public static void debug(final String msg) {
		if (bGlobalDebugEnabled) {
			getTracer().debug(null, msg);
		}
	}               

	public static void dump(final String headline, final String fct, final byte[] b, final int len,	final boolean noLimit) {
		if (bGlobalDebugEnabled) {
			StringBuffer stringBuffer = dumpToStringBuffer(headline, fct, b, len, noLimit);
			getTracer().debug(null, stringBuffer.toString());
		}
	}

	public static void dump(final String headline, final String fct, final String s, final boolean noLimit) {
		if (bGlobalDebugEnabled) {
                        byte[] b = s.getBytes();
			StringBuffer stringBuffer = dumpToStringBuffer(headline, fct, b, b.length, noLimit);
			getTracer().debug(null, stringBuffer.toString());
		}
	}

	public static final void verbose(final Throwable x) {
		if (bGlobalVerboseEnabled) {
			StringWriter sw = new StringWriter();
			x.printStackTrace(new PrintWriter(sw));
			String s = sw.toString();
			getTracer().verbose(null, s);
		}
	}

	public static void verbose(final String msg, final Throwable e) {
		if (bGlobalVerboseEnabled) {
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			final String traceMessage = msg + " has caught " + stringWriter.toString();
			getTracer().verbose(null, traceMessage);
		}
	}

	public static void verbose(final String msg) {
		if (bGlobalVerboseEnabled) {
			getTracer().verbose(null, msg);
		}
	}



}
