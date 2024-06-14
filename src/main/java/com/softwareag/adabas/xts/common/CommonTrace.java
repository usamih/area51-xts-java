/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/* Project started at May 2006 (initial by Thorsten Knieling) */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.common;
import com.softwareag.adabas.xts.XTSversion;
import com.softwareag.adabas.xts.XTS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.config.*;
import java.nio.charset.Charset;

/**
 * Internal Trace extension class providing base features
 * for Trace facilities.
 *  
 * @author tkn, usamih
 * System or environment variable:
 * XTSTRACE - 65535 or 0xffff
 * XTSDIR - where the trace goes. If not defined, it goes locally ("./")
 * XTS_ENABLE_LOG4J - log4j parameters; If not defined or NO, trace goes to XTSDIR; 
 *                    Other values: ALL, DEBUG, INFO, WARN, ERROR, FATAL, OFF, and TRACE
 * MAXSIZE - max trace file size
 * If only XTSTRACE is defined, trace goes to console
 * If XTSTRACE and XTSDIR are defined, trace goes to XTSDIR
 * If XTS_ENABLE_LOG4J and XTSTRACE are defined, trace goes to log4j
 * If none is defined, trace goes to log4j
 *
 */
public class CommonTrace implements ICommonTrace {

	private static final int MAX_LINES = 20;
	private static final String VERSION = XTSversion.VERSION ; 

	protected Class<ICommonTrace> referenceClass = null;

	protected String[] instance;
	protected boolean initDone = false;

	protected static ReentrantLock lock = new ReentrantLock();
	protected static String location;

	protected static int globalTrace = 0;
	protected static boolean XtsTraceSet = false;
	private File traceFile = null;
	private PrintStream traceOut = null;

	protected int currentInstance;
	private boolean traceInitialized = false;

	private static boolean log4jAvail = false;
	private static volatile Logger Logger = null;
	protected static Level level;

	private static long log_max_size = -1;
	private File[] refFile = { null, null };

	public static boolean bGlobalFatalEnabled = false;
	public static boolean bGlobalErrorEnabled = false;
	public static boolean bGlobalWarnEnabled = false;
	public static boolean bGlobalInfoEnabled = false;
	public static boolean bGlobalDebugEnabled = false;
	public static boolean bGlobalVerboseEnabled = false;

//	initEnvironment("XTSDIR", "XTSTRACE", "XTS_ENABLE_LOG4J", "LOGLEVEL");

	protected static void initEnvironment(final String dirName, final String traceEnv, final String log4jEnv, final String loglevel) {
		lock.lock();
		try {
			String xtstrace = System.getenv(traceEnv);
			if (xtstrace == null) {
				xtstrace = System.getProperty(traceEnv);
			}
			if (xtstrace != null) {
				xtstrace = xtstrace.trim();
				xtstrace = xtstrace.toLowerCase();
				try {
					if ((xtstrace.length() > 2) && (xtstrace.charAt(0) == '0') && (xtstrace.charAt(1) == 'x')) {
						globalTrace = Integer.parseInt(xtstrace.substring(2), 16);
					} else {
						globalTrace = Integer.parseInt(xtstrace, 10);
					}
					if (globalTrace != 0) {
						System.out.println(traceEnv + "=" + globalTrace);
					}
				} catch (NumberFormatException ne) {
					System.out.println("XTSTRACE variable not accepted=" + xtstrace);
					globalTrace = 0;
				}
//				XtsTraceSet = true;
			}

			if (globalTrace != 0) {
				location = System.getenv(dirName);
				if (location == null) {
					location = System.getProperty(dirName);
				}
				XtsTraceSet = true;
			}
//                      Do not define a  default location, so trace can go to statndard output if location is null
//			if (location == null) 
//				location = "./";
			if (globalTrace != 0)
				System.out.println("XTSDIR=" + location);
			String limitLevel = System.getenv(("MAXSIZE"));
			if (limitLevel == null) {
				limitLevel = System.getProperty("MAXSIZE");
			}
			if (limitLevel != null) {
				try {
					log_max_size = Integer.valueOf(limitLevel);
				} catch (NumberFormatException ne) {
					System.out.println("MAXSIZE variable not accepted=" + limitLevel);
					log_max_size = -1;
				}
			}
			limitLevel = System.getenv(("MAXBUFFERSIZE"));
			if (limitLevel == null) {
				limitLevel = System.getProperty("MAXBUFFERSIZE");
			}
			if (limitLevel != null) {
				try {
					XTS.maxmsglength = Integer.valueOf(limitLevel);
				} catch (NumberFormatException ne) {
					System.out.println("MAXBUFFERSIZE variable not accepted=" + limitLevel);
					XTS.maxmsglength = 1024*1024;
				}
			}
			if (XtsTraceSet == true) {
				// XTSTRACE variables was set, no chackeing of the LOG4J settings
				System.out.println("XTSTRACE set; not checking log4j2");
				bGlobalFatalEnabled = isGlobalFatalEnabled();
				bGlobalErrorEnabled = isGlobalErrorEnabled();
				bGlobalWarnEnabled = isGlobalWarnEnabled();
				bGlobalInfoEnabled = isGlobalInfoEnabled();
				bGlobalDebugEnabled = isGlobalDebugEnabled();
				bGlobalVerboseEnabled = isGlobalVerboseEnabled();
				return;
			}
			String jdcLevel = System.getenv(log4jEnv);
			if (jdcLevel == null) {
				// check if XTS_ENABLE_LOG4J is defined
				jdcLevel = System.getProperty(log4jEnv);
			}
			if (jdcLevel == null) {
				// if not, check if LOGLEVEL is defined
				jdcLevel = System.getProperty(loglevel);
//				if (jdcLevel != null)
//					System.out.println("XTS " + loglevel + "=" + jdcLevel);
			} else {
//				System.out.println("XTS " + log4jEnv + "=" + jdcLevel);
			}
			if (jdcLevel == null) {
				// default to info
				level = Level.INFO;
			} else {
				switch (jdcLevel.toUpperCase()) {
					case "ALL":
						level = Level.ALL;
						break;
					case "OFF":
						level = Level.OFF;
						break;
					case "INFO":
						level = Level.INFO;
						break;
					case "WARN":
						level = Level.WARN;
						break;
					case "FATAL":
						level = Level.FATAL;
						break;
					case "TRACE":
						level = Level.TRACE;
						break;
					case "DEBUG":
						level = Level.DEBUG;
						break;
					case "ERROR":
						level = Level.ERROR;
						break;
					default:
						level = Level.INFO;
						break;
					}
			}
			log4jAvail = true;
			// is log4j xml defined?
			String log4jfile = System.getProperty ("log4j2.configurationFile");
			if (log4jfile == null) {
				SetDefaultLog4j (level);
				System.out.println("XTS " + "No log4j2 configuration file; set default" + " LOGLEVEL=" + level.name());
			} else {
				Logger = LogManager.getLogger("com.softwareag.adabas.xts");
				level = Logger.getLevel();
				System.out.println("XTS " + "log4j2 configuration file=" + log4jfile + " LOGLEVEL=" + level.name());
			}
			Logger.trace("XTS LOGLEVEL=" + level.name());
			switch (level.name()) {
				case "ALL":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR | FLAG_WARN | FLAG_INFO | FLAG_DEBUG | FLAG_VERBOSE;
					break;
				case "OFF":
					globalTrace = 0;
					break;
				case "FATAL":
					globalTrace = FLAG_PREFIX | FLAG_FATAL;
					break;
				case "ERROR":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR;
					break;
				case "WARN":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR | FLAG_WARN;
					break;
				case "INFO":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR | FLAG_WARN | FLAG_INFO;
					break;
				case "DEBUG":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR | FLAG_WARN | FLAG_INFO | FLAG_DEBUG;
					break;
				case "TRACE":
					globalTrace = FLAG_PREFIX | FLAG_FATAL | FLAG_ERROR | FLAG_WARN | FLAG_INFO | FLAG_DEBUG | FLAG_VERBOSE;
					break;
				default:
					globalTrace = FLAG_VERBOSE | FLAG_TIME | FLAG_DATE | FLAG_TIMESTAMP;
					break;
			}
			Level.toLevel(null, level);
			Logger.trace ("XTS " + "Version=" + VERSION);
			bGlobalFatalEnabled = isGlobalFatalEnabled();
			bGlobalErrorEnabled = isGlobalErrorEnabled();
			bGlobalWarnEnabled = isGlobalWarnEnabled();
			bGlobalInfoEnabled = isGlobalInfoEnabled();
			bGlobalDebugEnabled = isGlobalDebugEnabled();
			bGlobalVerboseEnabled = isGlobalVerboseEnabled();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

/**
 * Set dafault trace to console
 *
 */
	protected static void SetDefaultLog4j (Level level)  {
		LoggerContext context = (LoggerContext) LogManager.getContext();
		Configuration config = context.getConfiguration();
//		PatternLayout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} %2T %-5level [%M():%L]: %m%n ", null, null, Charset.defaultCharset(),false,false,null,null);
		PatternLayout layout = PatternLayout.createDefaultLayout();
		Appender appender=ConsoleAppender.createAppender(layout, null, null, "CONSOLE_APPENDER", null, null);
		appender.start();
		AppenderRef ref = AppenderRef.createAppenderRef("CONSOLE_APPENDER",null,null);
		AppenderRef[] refs = new AppenderRef[] {ref};
		LoggerConfig loggerConfig =  LoggerConfig.createLogger("false", level, "CONSOLE_LOGGER", "", refs, null, config, null);
		loggerConfig.addAppender(appender,null,null);
		config.addAppender(appender);
		config.addLogger("com", loggerConfig);
		context.updateLoggers(config);
		Logger = LogManager.getContext().getLogger("com");
	}

	protected static Logger getLogger() {
		return Logger;
	}
	
	protected static boolean isLog4jAvailable() {
		return log4jAvail;
	}
	
	public static boolean isTraceSet() {
		if (globalTrace > 0)
			return (true);
		return (false);
	}

	// *** Flag verification routines ***
	public static boolean isGlobalFatalEnabled() {
		if ((globalTrace & FLAG_FATAL) == FLAG_FATAL) 
			return true;
		return false;
	}
	public static boolean isGlobalErrorEnabled() {
		if ((globalTrace & FLAG_ERROR) == FLAG_ERROR) 
			return true;
		return false;
	}
	public static boolean isGlobalWarnEnabled() {
		if ((globalTrace & FLAG_WARN) == FLAG_WARN) 
			return true;
		return false;
	}
	public static boolean isGlobalInfoEnabled() {
		if ((globalTrace & FLAG_INFO) == FLAG_INFO) 
			return true;
		return false;
	}
	public static boolean isGlobalDebugEnabled() {
		if ((globalTrace & FLAG_DEBUG) == FLAG_DEBUG) 
			return true;
		return false;
	}
	public static boolean isGlobalVerboseEnabled() {
		if ((globalTrace & FLAG_VERBOSE) == FLAG_VERBOSE) 
			return true;
		return false;
	}


	/**
	 * Verbose trace of exception
	 * 
	 * @param e
	 *            Exception to verbose print
	 */
	public final void error(final Object caller, final String msg) {
//		System.err.println(msg);
		log (ERROR_LEVEL, msg);
	}

	public final void warn(final Object caller, final String msg) {
		log (WARN_LEVEL, msg);
	}

	public final void info(final Object caller, final String msg) {
		log (INFO_LEVEL, msg);
	}

	public final void debug(final Object caller, final String msg) {
		log (DEBUG_LEVEL, msg);
	}

	public final void debug(final byte[] bytes) {
		debug(bytes, 0, bytes.length);
	}

	public final void debug(final byte[] bytes, final int offset, final int len) {
		StringBuffer buffer = new StringBuffer();
		for (int i = offset; i < offset + len; i++) {
			buffer.append(String.format("%x ", bytes[i]));
		}
		log(DEBUG_LEVEL, buffer.toString());
	}

	public final void verbose(final Object caller, final String msg) {
		log (VERBOSE_LEVEL, msg);
	}

	public void verbose(final Object caller, final Error e) {
		printError(caller, e);
	}

	/* (non-Javadoc)
	 * @see com.softwareag.adabas.xts.common.ICommonTrace#dumpBuffer(java.lang.String, byte[], int, int, boolean)
	 */
	public final void dumpBuffer(final String headline, final byte[] b, final int offset, final int len, final boolean noLimit) {

		if (((globalTrace & FLAG_DEBUG) != FLAG_DEBUG) || (b == null) ) 
			return;
		String fct = getFunctionName(this.getClass().getName());
		StringBuffer sBuf = dumpToStringBuffer(headline, fct, b, len, noLimit);
		log(DEBUG_LEVEL, sBuf.toString());
	}

	//   ************ log routine ***************
	private void log(final char level, final String msg) {
		StackTraceElement stack = getStackElement(this.getClass().getName());
		String message = "";

 		if (Logger != null) {
 			try {
 				String cm = stack.getClassName();
 				int j = cm.lastIndexOf(".") + 1;
  				message = cm.substring(j, cm.length()) + "." + stack.getMethodName() + ":" + stack.getLineNumber() + " ";
 				message += msg;

 				switch (level) {
 				case FATAL_LEVEL:
 					Logger.fatal(message);
 					break;
 				case ERROR_LEVEL:
 					Logger.error(message);
 					break;
 				case WARN_LEVEL:
 					Logger.warn(message);
 					break;
 				case INFO_LEVEL:
 					Logger.info(message);
 					break;
 				case DEBUG_LEVEL:
 					Logger.debug(message);
 					break;
 				case VERBOSE_LEVEL:
 					Logger.trace(message);
 					break;
				default:
	 				Logger.log(this.level, message);
 					break;
 				}
 				ThreadContext.clearMap();
 			} catch (IllegalArgumentException e) {
 				e.printStackTrace();
 			}
 			return;
 		}

		if (!initDone) {
			//  select where to log, file our console
			initOutput();
//			return;
		}

		message = "";
		if ((globalTrace & FLAG_TIMESTAMP) != 0) {
			message += System.currentTimeMillis();
		} else 
		if ((globalTrace & (FLAG_DATE | FLAG_TIME)) != 0) {
			Date date = new Date();
			String dateOutput = "";
			if ((globalTrace & FLAG_DATE) != 0) {
				dateOutput += "MMddyy-";
			}
			if ((globalTrace & FLAG_TIME) != 0) {
				dateOutput += "HHmmssz";
			}
			DateFormat df = new SimpleDateFormat(dateOutput, Locale.US);
			message += df.format(date);
		}

		message += " [" + level + "] ";
		if ((globalTrace & FLAG_PREFIX) != 0) {
			String cn = stack.getClassName();
			int i = cn.lastIndexOf(".") + 1;
			message += cn.substring(i, cn.length()) + "."	+ stack.getMethodName() + ":" + stack.getLineNumber() + " ";
		}
		message += msg;
		lock.lock();
		try {
			PrintStream out = traceOut;
			checkOutput();
			if (out != null) {
				out.println(message);
			}
		} finally {
			lock.unlock();
		}
	}

	private void printError(final Object caller, final Error e) {
		if (traceOut != null) {
			traceOut.println(e.getMessage());
			traceOut.println(e.toString());
		}
	}

	/**
	 * Get current Thread Name for debugging output
	 * 
	 * 
	 */
	protected static String getName() {
		Thread current = Thread.currentThread();
		String me = current.getName();
		return (me);
	}

	private static String doubleNumber(final String input) {
		int len = input.length();
		if (len == 1) {
			String output = "0" + input;
			return (output);
		}
		return (input);
	}

	public static String getDateString() {
		Calendar rightNow = Calendar.getInstance();
		String dateString = doubleNumber(Integer.toString(rightNow.get(Calendar.DAY_OF_MONTH)));
		dateString = dateString	+ "-" + doubleNumber(Integer.toString(rightNow.get(Calendar.MONTH) + 1));
		dateString = dateString + "-" + doubleNumber(Integer.toString(rightNow.get(Calendar.YEAR)));
		dateString = dateString	+ "-" + doubleNumber(Integer.toString(rightNow.get(Calendar.HOUR_OF_DAY)));
		dateString = dateString + "-" + doubleNumber(Integer.toString(rightNow.get(Calendar.MINUTE)));
		dateString = dateString + "-" + doubleNumber(Integer.toString(rightNow.get(Calendar.SECOND)));
		return (dateString);
	}

	public final void initOutput() {
		PrintStream out = null;
		lock.lock();
		try {
			if ((initDone) || (globalTrace == 0)) {
				return;
			}
			if (location != null) {
				traceFile = findNextFree();
				out = new PrintStream(traceFile);
			} else {
				out = System.out;
			}
			// traceInit = true;
			out.println(instance[1] + "\nTrace activation with " + globalTrace + " at " + getDateString());
			initDone = true;
		} catch (IOException e) {
			traceOut = null;
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		traceOut = out;

	}

	private File findNextFree() {
		File traceFile = null;
		File locFile = new File(location);
		String filename = instance[0] + "." + System.getProperty("user.name") + ".";
		filename += Thread.currentThread().getName().trim().replace(' ', '_').replaceAll("/", "").replaceAll(":", "_");
		for (int nr = 0; nr < 1000; nr++) {
			traceFile = new File(locFile, filename + "." + nr + ".log");
			if (!traceFile.exists()) {
				break;
			}
		}
		return traceFile;
	}

	private void checkOutput() {
		if ((log_max_size < 0) || (traceFile == null) || (traceFile.length() < log_max_size)) {
			return;
		}
		try {
			File nFreeFile = findNextFree();
			PrintStream out = new PrintStream(nFreeFile);
			out.println(instance[1]	+ "\nNext log opened with Trace activation with " + globalTrace + " at " + getDateString());
			File sFile = traceFile;
			PrintStream sout = traceOut;
			traceFile = nFreeFile;
			traceOut = out;
			sout.close();
			if (refFile[0] != null) {
				refFile[0].delete();
			}
			refFile[0] = refFile[1];
			refFile[1] = sFile;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Search stack Trace element for a specific name. 
	 * 
	 * @param name class to search for
	 * @return Stack element
	 */
	private static StackTraceElement getStackElement(final String name) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (name != null) {
			boolean found = false;
			for (StackTraceElement s : stack) {
				if ((s.getClassName().equals(name)) || (s.getClassName().equals(CommonTrace.class.getName()))) {
					found = true;
				} else {
					if (found) {
						return s;
					}
				}
			}
		}
		System.err.println("Stack request string not found");
		Thread.dumpStack();
		return stack[0];
	}

	public static byte convertToCharset(final String val, final String charset) {
		try {
			byte[] ebcdic = val.getBytes(charset);
			// System.out.println(String.format("%x",ebcdic[0]));
			return (byte) ebcdic[0];
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return (byte) val.charAt(0);

	}

	public static byte convertToAscii(final byte a, final boolean isEbcdic) {
		if (isEbcdic) {
			try {
				// System.out.print(String.format("%x->",a));
				byte[] ebcdic = new byte[1];
				ebcdic[0] = a;
				String test = new String(ebcdic, "Cp037");
				// System.out.println("Test "+test.charAt(0));
				return convertToCharset(test, "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return a;
	}

	public static String getBytesFormated(final boolean isEbcdic, final byte[] buffer) {
		try {
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < buffer.length; i++) {
				if (i % 16 == 0) {
					for (int j = 0; j < 16; j++) {
						if ((j + i) < buffer.length) {
							result.append(String.format("%02x ", buffer[j + i]));
						} else {
							result.append("   ");
						}

					}
					result.append("  >");
				}
				if (isEbcdic) {
					int t = 255 + (int) buffer[i] + 1;
					if (t > 255) {
						t -= 256;
					}
					// System.out.println(t+" "+0xd7+" "+String.format("%02x ",
					// buffer[i]));
					if (t < 0x40) {
						result.append(".");
					} else {
						String s = new String(buffer, i, 1, "cp037");
						result.append(s);
					}
				} else {
					if (buffer[i] < 32) {
						result.append(".");
					} else {
						result.append((char) buffer[i]);
					}
				}
				if (i % 16 == 15) {
					result.append("<\n");
				}
			}
			result.append("<\n");
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "Unknown charset";
	}

	/**
	 * Dump given byte array with corresponding String information into
	 * a StringBuffer.
	 * 
	 * @param headline headline prepand to the StringBuffer
	 * @param fct Function name added to the StringBuffer
	 * @param b byte array dumped in the StringBuffer
	 * @param len length of byte array to be dumped
	 * @param noLimit If false, a maximum number of lines is only dumped
	 * @return StringBuffer representation of the byte array.
	 */
	public static StringBuffer dumpToStringBuffer (final String headline, final String fct, final byte[] b, final int len, final boolean noLimit) {
		int nRows = 16;
		StringBuffer sBuf = new StringBuffer();
		sBuf.append(headline + /* "\n" +*/ String.format(" Dump size=%d(0x%x) Max Dumped=%d(0x%x)", b.length, b.length, len, len));

		byte[][] x = new byte[nRows][2];
		int r = 0;
		int newLine = 0;
		int nrLine = 0;
		int rlen = (b.length > len) ? len : b.length;

		for (int i = 0; i < rlen; nrLine++) {
			int fi = i;
			int ti = i + nRows;
			// if (ti >= rlen) {
			// ti = rlen - 1;
			// }
			boolean isEqual = false;
			if (nrLine > 0) {
				isEqual = true;
				for (int j = 0; j < nRows; j++, i++) {
					if (i < rlen) {
						x[j][r] = b[i];
						if (x[j][r] != x[j][(r + 1) % 2]) {
							isEqual = false;
						}
					} else {
						x[j][r] = 0;
					}
				}
			}
			if (isEqual) {
				newLine++;
			} else {
				newLine = 0;
			}

			if (ti >= rlen) {
				newLine = 0;
			// System.out.println("NEWLINE:" + newLine + " " + ti + " " + rlen
			// + " " + nrLine);
			}

			if ((nrLine > 0) && (newLine > 1)) {
				// traceOut.println(fi + ":" + ti + " rlen=" + rlen);
				if (newLine == 1) {
					sBuf.append("\n");
					sBuf.append(String.format("%04x ... ", fi));
				}
			} else {
				i = fi;
				for (int j = 0; j < nRows; j++, i++) {
					if (i % nRows == 0) {
						sBuf.append("\n");
						sBuf.append(String.format("%04x   %02x", i, b[i]));
					} else {
						if (i < rlen) {
							sBuf.append(String.format("%02x", b[i]));
						} else {
							sBuf.append(String.format("  "));
						}
					}
					if ((i - 1) % 2 == 0) {
						sBuf.append(" ");
					}
				}
				i = fi;
				// i = i - nRows;
				sBuf.append(" ");
				for (int j = 0; j < nRows; j++, i++) {
					if (i < rlen) {
						if (b[i] > 31) {
							sBuf.append((char) b[i]);
						} else {
							sBuf.append(".");
						}
					} else {
						sBuf.append(" ");
					}
				}
				r = (r + 1) % 2;
			}
			i = ti;
			if ((!noLimit) && (nrLine > MAX_LINES)) {
				sBuf.append("\n------- rest skipped  -----");
				break;
			}
		}
//		sBuf.append("\n");
		return sBuf;
	}

	/**
	 * Get String containing class name and method of a given class name.
	 * 
	 * @param name class name
	 * @return class name + method
	 */
	private static String getFunctionName(final String name) {
		// return stack[deep].getClassName() + "." + stack[deep].getMethodName()
		// + ":" + stack[deep].getLineNumber();
		StackTraceElement stack = getStackElement(name);
		String className = stack.getClassName();
		className = className.substring(className.lastIndexOf('.') + 1);
		return className + "." + stack.getMethodName();
		// + ":" + stack[deep].getLineNumber();
	}

	public static void setGlobalTrace(final int globalTrace) {
		CommonTrace.globalTrace = globalTrace;
	}

	@Override
	protected final void finalize() throws Throwable {
		super.finalize();
		if (traceOut != null) {
			traceOut.println("Close Trace at " + getDateString());
			traceOut.close();
		}
	}

	protected final void initLogger(final String name) {
 		lock.lock();
		try {
 			if (Logger == null) {
 				String cl = name;
 				Logger = LogManager.getLogger(cl);
				Configurator.setLevel(cl, level);
 				if (Logger != null) 
					Logger.log(this.level, "XTS " + "Version=" + XTSversion.VERSION);
 			}
 		} catch (IllegalArgumentException e) {
 			e.printStackTrace();
 		} finally {
 			lock.unlock();
 		}

	}


}
