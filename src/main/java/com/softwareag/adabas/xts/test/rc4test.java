/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.test;

import java.io.*;

final class rc4test {
	private static int[] esbox = new int[256];
	private static int ei = 0;
	private static int ej = 0;

	public static void main(final String[] arg) throws Exception {
		if (arg.length != 3) {
			System.out.println("usage: java rc4test infile outfile key");
			System.exit(1);
		}

		FileInputStream fi = new FileInputStream(arg[0]);
		FileOutputStream fo = new FileOutputStream(arg[1]);

		getKey(arg[2]);

		byte[] b = new byte[64 * 1024];

		int i = fi.read(b);
		while (i > 0) {
			encrypt(b, i);
			fo.write(b, 0, i);
			i = fi.read(b);
		}

		fi.close();
		fo.close();
	}

	private static void getKey(final String s) {
		for (int i = 0; i < 256; i++) {
			esbox[i] = i;
		}

		long[] k = new long[32];
		int j = 0;
		byte[] b = s.getBytes();
		for (int i = 0; i < b.length; i++) {
			k[j >> 6] <<= 6;
			k[j >> 6] |= b[i];
			j += 6;
		}
		System.out.println("bit length:" + j);
		b = new byte[j >> 3];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) k[i >> 3];
			System.out.print(b[i] + " ");
			k[i >> 3] >>= 8;
		}

		j = 0;
		int temp;
		for (int i = 0; i < 256; i++) {
			j = (j + esbox[i] + (b[i % b.length] & 0xff)) & 0xff;
			temp = esbox[i];
			esbox[i] = esbox[j];
			esbox[j] = temp;
		}
	}

	private static void encrypt(final byte[] b, final int l) {
		int temp;
		for (int i = 0; i < l; i++) {
			ei++;
			ei &= 0xff;
			ej += esbox[ei];
			ej &= 0xff;
			temp = esbox[ei];
			esbox[ei] = esbox[ej];
			esbox[ej] = temp;
			// System.out.println(ei+" "+esbox[ei]+" "+ej+" "+esbox[ej]);
			b[i] ^= esbox[(esbox[ei] + esbox[ej]) & 0xff];
		}
	}
}
