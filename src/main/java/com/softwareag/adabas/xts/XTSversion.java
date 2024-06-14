/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/**
 ** This program displays version information for all XTS class files.
 **/
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  10/18/99   rxc     [0001]       NONE        Add VERSION string.
//  15Mar00    pms     [0002]       NONE        Add Copyright string.
//  22Mar00    pms     [0003]                   Updated to V1118.
//  Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipFile;

public class XTSversion {
	public static final String VERSION = "6.1.0.0"; 
//	public static final String VERSION = XTSDCVersion.getCompleteVersion(); 
	public static final String COPYRIGHT = "Copyright (c) 1995-2024 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.";
	private static Class<?> c = null;
	private static ZipFile z = null;
	private static String s = null;

	public static final void main(String[] args) {
		try {
			if (args.length > 0)
				z = new ZipFile(args[0]);
			else
				z = new ZipFile("xts.jar");
			Enumeration<?> e = z.entries();
			while (e.hasMoreElements()) {
				try {
					s = (String) e.nextElement().toString();
					s = s.replace('/', '.');
					int i = s.lastIndexOf(".");
					s = s.substring(0, i);
					i = s.lastIndexOf(".");
					if (i != -1) {
						if (s.substring(0, i).equalsIgnoreCase(
								"com.softwareag.adabas.xts")) {
							c = Class.forName(s);
							int idx = s.indexOf('$');
							if (idx != -1)
								System.out.println(" version=NONE     " + s + " -> INNER CLASS");
							else {
								try {
									if (c.isInterface()) {
										System.out.print(" version=" + (String) c.getField("VERSION").get(c) + "  "
												+ c.getName());
										System.out.println(" -> INTERFACE");
									} else
										System.out.println(" version=" + (String) c.getField("VERSION").get(c) + "  " + c.getName());
								} catch (NoSuchFieldException ex) {
									System.out.println(" version=NONE     " + s);
								} catch (IllegalAccessException ex) {
									System.out.println(" version=NONE     "	+ s + " is a private class that is not part of this package");
								}
							}
						}
					}
				} catch (IllegalArgumentException ex) {
				} catch (NoClassDefFoundError er) {
					System.out.println(" NOT FOUND      " + s);
				} catch (ClassNotFoundException ex) {
					System.out.println(" NOT FOUND      " + s);
				} catch (UnsatisfiedLinkError ex) {
				}
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}
