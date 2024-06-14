/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/* Project started at May 2006  (initial by Thorsten Knieling) */

package com.softwareag.adabas.xts;

public final class XTSDCVersion  {

	public static String getCompleteVersion() {
		return new XTSDCVersion().getClass().getPackage().getImplementationVersion();
	}

	public static long[] getVersion() {
		String v = new XTSDCVersion().getClass().getPackage().getImplementationVersion();
		String[] s = v.split("\\.");
		long[] vi = new long[4];
		for(int i=0;i<4;i++) {
			String n = s[i];			
			if( (i==3) && (n.charAt(0)=='v'))
				n = n.substring(1);
			vi[i] = Long.valueOf(n);
		}
		return vi;
	}

	public void output() {
		System.out.println("Adabas JAI API version is " + getCompleteVersion());
	}

	public static void main(String[] argv) {
		XTSDCVersion adcv = new XTSDCVersion();

		if (argv.length > 0) {
			if (argv[0].equals("int")) {
				for(long i:getVersion())
					System.out.print(i+" ");
				System.out.println();
			} else
			System.out.println(XTSDCVersion.getCompleteVersion());
		} else
			adcv.output();
	}
}
