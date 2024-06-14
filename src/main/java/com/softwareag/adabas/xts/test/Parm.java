/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This class is used by the java test programs for parms processing.
 *
 */

package com.softwareag.adabas.xts.test;

import java.util.*;

public class Parm {

	public static String getparm(final String s, final String[] args, final boolean must) {
		int j = s.length();
		for (int i = args.length - 1; i > -1; i--) {
			if (args[i].length() > j && args[i].charAt(j) == '=') {
				if (s.equalsIgnoreCase(args[i].substring(0, j))) {
					return args[i].substring(j + 1);
				}
			}
		}
		if (must) {
			System.out.println("Required parameter missing...." + s);
			KeywordMsg.print();
		}
		return null;
	}

	public static String[] getStringArray(final String s) {
		int j;
		int i;
		String s2;
		Vector<String> v = new Vector<String>();
		for (i = 0; i < s.length();) {
			if ((j = s.indexOf(",", i)) == -1) {
				j = s.length();
			}
			s2 = s.substring(i, j);
			i = j + 1;
			v.addElement(s2);
		}
		String[] sa = new String[v.size()];
		v.copyInto(sa);
		return sa;
	}

	public static int[] getIntArray(final String s) {
		int j;
		int i;
		int k;
		Integer in;
		String s2;
		Vector<Integer> v = new Vector<Integer>();
		for (i = 0; i < s.length();) {
			if ((j = s.indexOf(",", i)) == -1) {
				j = s.length();
			}
			s2 = s.substring(i, j);
			i = j + 1;
			k = Integer.parseInt(s2);
			in = Integer.valueOf(k);
			v.addElement(in);
		}
		int[] n = new int[v.size()];
		Enumeration<Integer> e = v.elements();
		for (i = 0; e.hasMoreElements(); i++) {
			in = (Integer) e.nextElement();
			n[i] = in.intValue();
		}
		return n;
	}
}
