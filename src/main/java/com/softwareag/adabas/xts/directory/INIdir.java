/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts.directory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.softwareag.adabas.xts.XTS;
import com.softwareag.adabas.xts.XTSException;
import com.softwareag.adabas.xts.XTStrace;
import com.softwareag.adabas.xts.XTSurl;

/**
 * Directory server. This class has an INI file from which a list of entries are
 * loaded.
 ** 
 * @version 5.1.1.1
 **/
public class INIdir extends Directory {

	private ReentrantLock lock = new ReentrantLock();
	private Hashtable<String, String> ini;
	private String filename = "xtsurl.cfg";
	private File configFile = null;
	private boolean updated = false;
	private boolean loaded = false;
	private long lastModify = 0;

	/** default constructor. **/
	public INIdir() {
		this("xtsurl.cfg");
	}

	/**
	 * Constructor that sets the file name.
	 * 
	 * @param filename
	 *            File name containing XTS ADI targets
	 **/
	public INIdir(final String filename) {

		if (filename.startsWith("file:")) {
			this.filename = filename.substring(5);
		} else {
			this.filename = filename;
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Init INIdir " + this.filename + " out of " + filename);
		configFile = new File(this.filename);
		lastModify = configFile.lastModified();
	}

	/**
	 * Retrieve a list of URLs from the directory.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target.
	 ** @param hostname
	 *            the hostName.
	 ** @return an array of URLs or null.
	 * @throws XTSException
	 *             Error retrieving list (Caused by I/O error)
	 **/
	public final XTSurl[] retrieve(final String qualifier, final String target, final String hostName) throws XTSException {
        
		XTSurl[] urls = retrieve(qualifier, target);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir: RETRIEVE function called, filtered by hostname=" + hostName);
		if (urls != null) {
			List<XTSurl> urlList = new ArrayList<XTSurl>();
			for (int i = 0; i < urls.length; i++) {
				if (hostName.equalsIgnoreCase(urls[i].getHost())) {
					urlList.add(urls[i]);
				}
			}
			urls = null;
			if (!urlList.isEmpty()) {
				urls = new XTSurl[urlList.size()];
				for( int i = 0; i <urlList.size(); i++) {
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
	 ** @return an array of URLs or null.
	 * @throws XTSException
	 *             Error retrieving list (Caused by I/O error)
	 **/
	public final synchronized XTSurl[] retrieve(final String qualifier, final String target) throws XTSException {

		String s = getString(qualifier, target);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir: ` called, KEY=" + s + " from file=" + filename);
		lock.lock();
		try {
			long lastModify = configFile.lastModified();
			if (this.lastModify != lastModify) {
				ini = loadHashTable("=");
			}
			if (ini == null) {
// MIHAI this creates a crash	return new XTSurl[0];
				return null;
			}

			Enumeration<String> e = ini.keys();
			XTSurl[] x = new XTSurl[ini.size()];
			for (int i = s.length() - 1; i >= 0; i--) {
				if (s.charAt(i) == '.') {
					s = s.substring(0, i) + "\\." + s.substring(i + 1);
				}
				if (s.charAt(i) == '[') {
					s = s.substring(0, i) + "\\[.*";
				}
				if (s.charAt(i) == '*') {
					if (i > 0) {
						s = s.substring(0, i) + ".*" + s.substring(i + 1);
					} else {
						s = ".*" + s.substring(i + 1);
					}
				}
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("INIdir: RETRIEVE function called, NEW KEY=" + s);
			int i = 0;
			while (e.hasMoreElements()) {
				String t = (String) e.nextElement();
				if (t.matches(s)) { 
					String u = (String) ini.get(t); 
					if (u != null) {
						try {
							if (XTStrace.bGlobalVerboseEnabled) 
								XTStrace.verbose("INIdir: RETRIEVE KEY=" + t + " URL=" + u);
							x[i] = new XTSurl(u);
						} catch (Exception ee) {
							XTStrace.verbose("URL=" + u + " Ignored");
							continue;
						}
						x[i].key = t;
						int qualIndex = t.indexOf('#');
						if (qualIndex == -1) {
							qualIndex = 0;
						} else {
							x[i].partition = t.substring(0, qualIndex);
							qualIndex++;
						}
						int targIndex = t.indexOf('.');
						x[i].qualifier = t.substring(qualIndex, targIndex);
						x[i].target = t.substring(targIndex + 1, t.lastIndexOf('['));
						i++;
					}
				} 
			}
			if (i == 0) {
// MIHAI this creates a crash	return new XTSurl[0];
				return null;
			}
			if (i == x.length) {
				return x;
			}
			XTSurl[] y = new XTSurl[i];
			System.arraycopy(x, 0, y, 0, i);
			return y;			
		} catch (IOException ie) {
			throw new XTSException("Directory : I/O Error: " + ie.getMessage(), XTSException.XTS_DIR_IO_ERROR);
//		} catch (Exception ee) {
//			throw new XTSException(ee.getMessage(), XTSException.XTS_XTSURL_INVALID_URL);
//			XTStrace.verbose(ee.getMessage());
//			return null;
//			continue;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Retrieve ADI parameters.
	 ** 
	 ** @return an array of URLs or null.
	 * @throws XTSException
	 *             Error retrieving list (Caused by I/O error)
	 **/
	public final XTSurl[] adiquery() throws XTSException {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir: QUERY ADI parameters");
      		return null;
	}

	/**
	 * Add an entry.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param url
	 *            the url to add.
	 ** @throws XTSException.
	 **/
	public final synchronized void add(final String qualifier, final XTSurl url) throws IOException, XTSException, Exception {

		String s = partition + qualifier + url.target;
		if ((s.indexOf('.') != -1) || (s.indexOf('#') != -1) || (s.indexOf('[') != -1) || (s.indexOf(']') != -1) || (s.indexOf('\\') != -1) || (s.indexOf('=') != -1)) {
			throw new XTSException("Directory add : Invalid parameters = " + partition + " " + qualifier + " " + url.target, XTSException.XTS_IDIR_ADD_INVALID_PARMS);
		}

		s = getString(qualifier, url.target);
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir add function called, KEY=" + s + " URL=" + url.toString(true) + " file=" + filename);
		int i = 0;
		String existingUrl = null;
		lock.lock();
		try {
			if (ini == null) {
				throw new XTSException(XTSException.XTS_DIR_CONFIG_ERROR);
			}
			while ((existingUrl = (String) ini.get(s + i + ']')) != null) {
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("INIdir: ADD Entry Next=" + existingUrl);
				if (existingUrl.equalsIgnoreCase(url.toString(true))) {
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("INIdir: ADD Entry Duplicate=" + existingUrl);
					throw new XTSException(XTSException.DS_ALREADY_EXISTS);
				}
				i++;
			}
			ini.put(s + i + ']', url.toString(true));
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("INIdir: ADD Entry " + s + i + "]" + "=" + url.toString(true) + " successful");
			updated = true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Delete an entry.
	 ** 
	 * @param qualifier
	 *            a string that qualifies the entry.
	 ** @param url
	 *            the url that uniquely identifies the entry.
	 **/
	public final synchronized void delete(final String qualifier, final XTSurl url) throws Exception {

		boolean ispartition = false;
		String s = getString(qualifier, url.target);
		if ((partition != null) && (partition.length() != 0)) {
			ispartition = true;
		}
		if (XTStrace.bGlobalVerboseEnabled) {
			if (ispartition)
				XTStrace.verbose("INIdir: DELETE function called, KEY=" + s + " URL=" + url.toString() + " partition=" + partition);
			else
				XTStrace.verbose("INIdir: DELETE function called, KEY=" + s + " URL=" + url.toString());
		}
		String u = url.toString(true);
		lock.lock();
		try {
			Enumeration<String> e = ini.keys();
			while (e.hasMoreElements()) {
				String t = (String) e.nextElement();
				String o = t;
				String uu = (String) ini.get(t); 
				if (ispartition) {
					if (partition.equals("*")) {
						int i = t.indexOf('#');
						if (i >= 0) 
							t = t.substring(i+1);
					}
				}
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("INIdir: DELETE Entry Next=" + t + " URL=" + uu);
				if (t.startsWith(s) && u.equalsIgnoreCase((String) ini.get(o))) {
					ini.remove(o);
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("INIdir: DELETE Entry Deleted");
					updated = true;
					// delete only the first instance of the URL, not all, si ne oprim aici
					break;
				}
			}
			if (!updated)
				throw new XTSException(XTSException.DS_NO_ENTRIES);

		} finally {
			lock.unlock();
		}
	}

	/**
	 * Delete an entry ex.
	 ** 
	 *  @param qualifier
	 *            a string that qualifies the entry.
	 ** @param target
	 *            the target id of the entry.
	 **/
	public final synchronized void delete_ex(final String qualifier, final String target) throws Exception {

		boolean ispartition = false;
		String s = getString(qualifier, target);
		if ((partition != null) && (partition.length() != 0)) {
			ispartition = true;
		}
		if (XTStrace.bGlobalVerboseEnabled) {
			if (ispartition)
				XTStrace.verbose("INIdir: DELETE_EX function called, KEY=" + s + " partition=" + partition);
			else
				XTStrace.verbose("INIdir: DELETE_EX function called, KEY=" + s);
		}
	        if (qualifier.equals("*") || target.equals("*")) {
			for (int i = s.length() - 1; i >= 0; i--) {
				if (s.charAt(i) == '.') {
					s = s.substring(0, i) + "\\." + s.substring(i + 1);
				}
				if (s.charAt(i) == '[') {
					s = s.substring(0, i) + "\\[.*";
				}
				if (s.charAt(i) == '*') {
					if (i > 0) {
						s = s.substring(0, i) + ".*" + s.substring(i + 1);
					} else {
						s = ".*" + s.substring(i + 1);
					}
				}
			}
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("INIdir: DELETE_EX function called, NEW KEY=" + s);
			lock.lock();
			Enumeration<String> e = ini.keys();
			while (e.hasMoreElements()) {
				String t = (String) e.nextElement();
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("INIdir: DELETE_EX Entry Next=" + t + " URL=" + (String) ini.get(t));
				if (t.matches(s)) { 
					String u = (String) ini.get(t); 
					if (u != null) {
						if (!ispartition) {
							// no partition, ignore the entries with partition
							int k = t.indexOf('#');
							if (k >= 0) {
								continue;
							}
						} 
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("INIdir: DELETE_EX Entry Deleted");
						ini.remove(t);
						updated = true;
					}
				} 
			}
			lock.unlock();
			if (!updated)
				throw new XTSException(XTSException.DS_NO_ENTRIES);
		} else {
			lock.lock();
			try {
				Enumeration<String> e = ini.keys();
				while (e.hasMoreElements()) {
					String t = (String) e.nextElement();
					String o = t;
					String uu = (String) ini.get(t); 
					if (ispartition) {
						if (partition.equals("*")) {
							int i = t.indexOf('#');
							if (i >= 0) 
								t = t.substring(i+1);
						}
					}
					if (XTStrace.bGlobalVerboseEnabled) 
						XTStrace.verbose("INIdir: DELETE_EX Entry Next=" + t + " URL=" + uu);
					if (t.startsWith(s) ) {
						ini.remove(o);
						if (XTStrace.bGlobalVerboseEnabled) 
							XTStrace.verbose("INIdir: DELETE_EX Entry=" + o + " URL=" + uu);
						updated = true;
						// delete only the first instance of the URL, not all
						break;
					}
				}
				if (!updated)
					throw new XTSException(XTSException.DS_NO_ENTRIES);
			} finally {
				lock.unlock();
			}
		}
	}


	/**
	 * Commit the changes.
	 ** 
	 * @param shouldI
	 *            if true then changes are hardened, else changes are backed
	 *            out.
	 ** @exception IOException
	 *                if the file has been deleted or can occur during file
	 *                processing.
	 **/
	public final synchronized void commit(final boolean shouldI) throws Exception {

		if (!updated) {
			return;
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir: COMMIT function called Parm=" + shouldI);
		if (shouldI) {
			saveHashTable();
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.verbose("INIdir: Changes SAVED in file=" + filename);
		} else {
			Hashtable<String, String> table = loadHashTable("="); 
			lock.lock();
			try {
				ini = table;
				if (XTStrace.bGlobalVerboseEnabled) 
					XTStrace.verbose("INIdir: Changes BACKED OUT of file=" + filename);
			} finally {
				lock.unlock();
			}
		}
		updated = false;
	}

	public final synchronized void shutdown() throws IOException {
	}

	private final String getString(final String qualifier, final String target) throws XTSException {

		if (!loaded) {
			String[] p = {};
			setParameters(p);
		}
		if ((partition == null) || (partition.length() == 0)) {
			if (target.equals("*")) {
				return qualifier + '.' + target;
			} else {
				return qualifier + '.' + target + '[';
			}
		}
		if (partition.equals("*")) {
			if (target.equals("*")) {
				return qualifier + '.' + target;
			} else {
				return qualifier + '.' + target + '[';
			}
		}
		if (target.equals("*")) {
			return partition + '#' + qualifier + '.' + target;
		} else {
			return partition + '#' + qualifier + '.' + target + '[';
		}
	}

	/**
	 * set parameters.
	 ** 
	 * @param param
	 *            String array containing keyword-value pairs. The following
	 *            values are recognized:
	 *            <ul>
	 **            <li>file - the file to load the directory info from.
	 *            </ul>
	 **/
	public final void setParameters(final String[] param) throws XTSException{

		for (int i = 0; i < param.length; i += 2) {
			if (!param[i].equalsIgnoreCase("file")) {
				throw new XTSException("INIdir setParameters: Invalid parameter " + param[i] + " passed");
			}
			filename = param[i + 1];
			configFile = new File(filename);
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("INIdir: Using File=" + filename);
		lock.lock();
		try {
			ini = loadHashTable("=");
			lastModify = configFile.lastModified();
		} catch (FileNotFoundException e) {
			if (XTStrace.bGlobalVerboseEnabled) 
				XTStrace.error("INIdir: " + e + " file=" + filename + " error=" + e);
			throw new XTSException("File " + filename + " not found", XTSException.XTS_FILE_NOT_FOUND); 
		} catch (IOException e) {
			throw new XTSException(e);
		} finally {
			lock.unlock();
		}
		loaded = true;
	}

	private final Hashtable<String, String> loadHashTable(String inKeyDelimiter) throws IOException {

		String keyDelimiter = inKeyDelimiter;
		BufferedReader fr = new BufferedReader(new FileReader(configFile));
		String keyIn, valueIn;
		String valueOut;
		int equalsIndex = 0;
		int escapeIndex = 0;

		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Load the hash table from=" + filename);
		if (keyDelimiter == null || keyDelimiter.length() <= 0) {
			keyDelimiter = "=";
		}

		Hashtable<String, String> ht = new Hashtable<String, String>();

		String lineIn = fr.readLine();
		while (lineIn != null) {
			// Check for comment line
			if (lineIn.startsWith("#")) {
				lineIn = fr.readLine();
				continue;
			}

			// Remove all possible Properties escape sequences 
			escapeIndex = lineIn.indexOf('\\');
			while (escapeIndex != -1) {
				lineIn = lineIn.substring(0, escapeIndex) + lineIn.substring(escapeIndex + 1);
				escapeIndex = lineIn.indexOf('\\', escapeIndex + 1); // +1 for Double, escape, sequence
			}
			equalsIndex = lineIn.indexOf(keyDelimiter);
			if (equalsIndex >= 0) {
				keyIn = lineIn.substring(0, equalsIndex);
				valueIn = lineIn.substring(equalsIndex + 1);
				// ht.put(keyIn, valueIn); 
				// If this is a duplicate record, then the assumption is [0009]
				// the indexs are the same. The index will be incremented [0009]
				// by 1 until the record is successfully added. [0009]
				while ((valueOut = (String) ht.put(keyIn, valueIn)) != null) {
					int idxStart = keyIn.indexOf('[');
					if (idxStart != -1) {
						int idxEnd = keyIn.indexOf(']');
						if (idxEnd != -1) {
							String sidx = keyIn.substring(++idxStart, idxEnd);
							int idx = Integer.parseInt(sidx);
							keyIn = keyIn.substring(0, ++idxStart) + String.valueOf(++idx) + keyIn.substring(idxEnd);
							valueIn = valueOut;
						}
					}
				}
			}

			lineIn = fr.readLine();
		}
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("File=" + filename + " closed");
		fr.close();
		return ht;
	}

	/**
	 * Save Hash table to file.
	 * 
	 * @throws IOException
	 *             Error storing file on disc
	 */
	private final void saveHashTable() throws Exception {
		if (XTStrace.bGlobalVerboseEnabled) 
			XTStrace.verbose("Write the hash table to=" + filename);
		lock.lock();
		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter(configFile));

			List<String> list = new ArrayList<String>(ini.keySet());
			for (String key : list) {
				fw.write(key + "=" + ini.get(key) + "\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("INIdir: " + e + " file=" + filename + " error=" + e);
			throw new XTSException("File " + filename + " not found", XTSException.XTS_FILE_NOT_FOUND); 
		} catch (IOException e) {
			if (XTStrace.bGlobalErrorEnabled) 
				XTStrace.error("INIdir: " + e + " file=" + filename + " error=" + e);
			throw new XTSException(e);
		} finally {
			lock.unlock();
		}
	}


	@Override
	public final String getUrl() {
		return "file:" + filename;
	}
}
