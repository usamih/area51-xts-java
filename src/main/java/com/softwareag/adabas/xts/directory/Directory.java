/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.directory;

import com.softwareag.adabas.xts.IXtsDirectory;
import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTSException;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;
import com.softwareag.adabas.xts.XTSversion;

/**
 * The Directory defines how a directory technology will be accessed. A
 * "partition" may be specified, that is prepended to the query, effectively
 * enabling multiple logical partitions within the directory. All directory
 * entries are unique. If a list of entries is required, a wild card may be
 * specified as part of the target e.g. "col*" would retrieve URLs for "color"
 * and "colour" and "color4". Wild cards may not appear as part of the
 * qualifier.
 **/
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

public abstract class Directory implements IXtsDirectory {
	
	public static final String VERSION = XTSversion.VERSION;
	public static final String COPYRIGHT = XTSversion.COPYRIGHT;
	/** The partition value. **/
	String partition = "";

	/**
	 * Set partition. This machanism divides a directory into multiple
	 * subdirectories that are mutually exclusive.
	 ** 
	 * @param partition
	 *            the partition to set.
	 **/
	public void setPartition(String partition) {
		this.partition = partition;
	}
	
	/* (non-Javadoc) Return the partition value
	 * This was needed so a caller could save, change, and then restore the value.
	 * @see com.softwareag.adabas.xts.IXtsDirectory#getPartition()
	 */
	public String getPartition() {
		return partition;
	}

	/**
	 * Retrieve a list of URLs from the directory.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target.
	 ** @exception Exception
	 *                thrown if retrieval not possible.
	 **/
	public abstract XTSurl[] retrieve(String qualifier, String target) throws Exception;

	/**
	 * Retrieve a list of URLs from the directory.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target.
	 ** @param hostname
	 *            the hostName.
     
	 ** @exception Exception
	 *                thrown if retrieval not possible.
	 **/
	public abstract XTSurl[] retrieve(String qualifier, String target, String hostName) throws Exception;
	/**
	 * Retrieve ADI parameters.
	 ** 
	 ** @exception Exception
	 *                thrown if retrieval not possible.
	 **/
	public abstract XTSurl[] adiquery() throws Exception;
	/**
	 * Add an entry.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param url
	 *            the url to add. It must contain the target.
	 ** @exception Exception
	 *                thrown if adding not possible.
	 **/
	public abstract void add(String qualifier, XTSurl url) throws Exception;

	/**
	 * Delete an entry.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param url
	 *            the url to delete. It must contain the target.
	 ** @exception Exception
	 *                thrown if deleting not possible.
	 **/
	public abstract void delete(String qualifier, XTSurl url) throws Exception;

	public abstract void delete_ex(String qualifier, String target) throws Exception;

	/**
	 * Commit changes.
	 ** 
	 * @param shouldI
	 *            if true then changes are hardened, else changes are discarded.
	 ** @exception Exception
	 *                thrown if commit not possible.
	 **/
	public abstract void commit(boolean shouldI) throws Exception;

	/**
	 * shutdown.
	 ** 
	 **/
	public abstract void shutdown() throws Exception;

	/**
	 * Set directory parameters.
	 ** 
	 * @param parameters
	 *            an array of strings containing pairs of parameters e.g. it
	 *            must contain an even number of elements and the even-numbered
	 *            elements contain a keyword and the odd-numbered elements
	 *            contain a value. An error is raised if a keyword is not
	 *            recognized or a value is invalid.
	 * @throws Exception 
	 **/
	public abstract void setParameters(String[] parameters) throws Exception;

	/**
	 * Set directory parameters from a string.
	 ** 
	 * @param parameters
	 *            a string of the format <i>keyword=value,keyword=value</i>
	 *            where the value may be enclosed in single or double quotes.
	 **/
	public void setParameters(String parameters) throws Exception {
		String[] p = new String[20]; // max 20 parameters
		char[] c = parameters.toCharArray(); // for speed
		int i = 0;
		int j = 0;
		try {
			for (;;) {
				int k = i++;
				while (c[i] != '=')
					i++;
				p[j++] = new String(c, k, i - k);
				XTStrace.verbose("DIR: PARM=" + p[j - 1]);
				k = ++i;
				if (c[i] == '\'' || c[i] == '"') {
					char x = c[i];
					k = ++i;
					while (c[i] != x)
						i++;
					p[j++] = new String(c, k, i - k);
					XTStrace.verbose("DIR: PARM=" + p[j - 1]);
					i++;
				} else {
					while (i < c.length && c[i] != ',')
						i++;
					p[j++] = new String(c, k, i - k);
					XTStrace.verbose("DIR: PARM=" + p[j - 1]);
				}
				if (i < c.length) {
					if (c[i] != ',')
						throw new ArrayIndexOutOfBoundsException();
					i++;
				} else
					break;
			}
		} catch (ArrayIndexOutOfBoundsException ae) {
			throw new XTSException("Directory setParameters : Syntax error in " + parameters
					+ " position " + i, XTSException.XTS_DIR_SET_PARMETERS_ERROR);
		}

		String[] q = new String[j];
		System.arraycopy(p, 0, q, 0, j);
		setParameters(q);
	}

}
