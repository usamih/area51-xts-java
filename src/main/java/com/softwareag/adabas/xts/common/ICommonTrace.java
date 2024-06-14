/*+++*******************************************************************
/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/* Project started at May 2011 (initial by Thorsten Knieling) */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.common;

public interface ICommonTrace {
	public static final int FLAG_PREFIX = (1 << 0);       // applies to log4j2
	public static final int FLAG_FATAL = (1 << 1);        // log4j2
	public static final int FLAG_ERROR = (1 << 2);        // log4j2
	public static final int FLAG_WARN = (1 << 3);         // log4j2
	public static final int FLAG_INFO = (1 << 4);         // log4j2
	public static final int FLAG_DEBUG = (1 << 5);        // log4j2
	public static final int FLAG_VERBOSE = (1 << 6);      // log4j2

	public static final int FLAG_TIMESTAMP = (1 << 7);    // applies to file
	public static final int FLAG_TIME = (1 << 8);         // applies to file
	public static final int FLAG_DATE = (1 << 9);         // applies to file

	public static final char FATAL_LEVEL = 'F';
	public static final char ERROR_LEVEL = 'E';
	public static final char WARN_LEVEL  = 'W';
	public static final char INFO_LEVEL  = 'I';
	public static final char DEBUG_LEVEL = 'D';
	public static final char VERBOSE_LEVEL = 'T';

	public void error(final Object caller, final String msg);
	public void warn(final Object caller, final String msg);
	public void info(final Object caller, final String msg);
	public void debug(final Object caller, final String msg);
	public void debug(final byte[] bytes);
	public void debug(final byte[] bytes, final int offset, final int len);
	public void verbose(final Object caller, final String msg);
	public void verbose(final Object caller, Error e);
	public void dumpBuffer(String headline, byte[] rbuffer, int offset, int len, boolean noLimit);

}
