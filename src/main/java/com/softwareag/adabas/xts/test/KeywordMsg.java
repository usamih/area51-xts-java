/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

/*
 * This class contains a method to display a list of possible keyword values used by  
 * the test programs.
 *
 */
//-----------------------------------------------------------------------
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  11/10/99   rxc     [0001]       NONE        Add datagram option that indicates 
//                                              bindclient processing. Yes..no reply
//                                              expected, no..reply expected.
//  26Oct00    usaco   [0002]      202429       Remove DNSdir support  
//
//-----------------------------------------------------------------------
package com.softwareag.adabas.xts.test;

public class KeywordMsg {

	public static final void print() {
		System.out.print("\n\n");
		System.out
				.println(" **************** LIST OF KEYWORD PARAMETERS [note 1] ****************");
		System.out.println(" dir=x, Possible values for x:");
		// System.out.println("  x=d [DNSdir]"); // [0002]
		System.out.println("  x=i [INIdir]");
		// System.out.println("  x=l [LDAPdir]");
		System.out
				.println("    * Defaults to user provided URL..i.e.dirparms=url=tcpip://.....");
		System.out
				.println("      or Directory Server Lookup if no URL is provided.");
		System.out
				.println(" ---------------------------------------------------------------------");
		System.out.println(" dirparms=parms, Possible directory parm values:");
		System.out.println("  INI     directory: file=myfile.ini");
		// System.out.println("  LDAP    directory: url=tcpip://host:port,basedn=xxxxxx,authdn=xxxxxx,");
		// System.out.println("                     authpass=xxxxxx,host=hostname          [note 5]");
		System.out
				.println("  Default directory: url=protocol://host:port?parm=value&parm=value...");
		System.out
				.println(" ---------------------------------------------------------------------");
		System.out
				.println(" serverNm=ssssssss,...,    e.g. serverName=XTSserver,srv32  [note 3,4]");
		System.out
				.println(" serverID=nnnnnnnn,...,    e.g. serverID=124,9898,4444      [note 3,4]");
		System.out.println(" trace={on|off},           Defaults to trace=off");
		System.out
				.println(" count=nnnnnnnnnn,         Number of Iterations             [note 2,6,7]");
		System.out
				.println(" NbrThreads=nnnnnnnnnn,    Number of Threads                [note 2,6]");
		System.out
				.println(" sleep=nnnnnnnnnnnnnnnnnn, Time in milliseconds             [note 2,6]");
		System.out
				.println(" timeout=nnnnnnnnnnnnnnnn, Time in milliseconds             [note 2,6]");
		System.out
				.println(" message-size=nnnnnnnn,    Size of message to send          [note 8]");
		System.out
				.println(" datagram=(YES|no)         bindClient send/send and reply   [note 2,9]\n\n");// [0001]
		System.out.println("NOTE 1 - Keywords are case INSENSITIVE");
		System.out
				.println("NOTE 2 - Keyword not used by all applications and may be ignored");
		System.out.println("Note 3 - Keyword REQUIRED");
		System.out
				.println("Note 4 - At least 1 serverNm/serverID must be specified");
		System.out
				.println("Note 5 - Type 1 or more of these Dirparm Keywords separated by commas");
		System.out.println("Note 6 - Defaults depend on application ");
		System.out
				.println("Note 7 - Count is the number of times 1000 calls will be issued. 1000 was");
		System.out
				.println("         chosen for the formula for calculating calls per second");
		System.out.println("Note 8 - minimun size is 1 max is 10000000");
		System.out.println("Note 9 - Upper case value is the default"); // [0001]
		return;
	}
}
