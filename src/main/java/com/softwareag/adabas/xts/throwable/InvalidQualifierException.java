/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.throwable;
/**
 * Thrown by various class methods, to indicate the qualifier is invalid.  
 *
 * @author  Rich.Cole@sagus.com
 * @version 2.1.1.0
 * @see     XTSDirException
 */
//   Maintenance:
//    DATE    USERID    TAG#   SAGSIS/VANTIVE#  DESCRIPTION
//  10/13/99   rxc     [0001]       NONE        Add VERSION string.
//  15Mar00    pms     [0002]       NONE        Add Copyright string.
//  22Mar00    pms     [0003]                   Updated to V1118.
public class InvalidQualifierException extends Exception {
  /** * 	 */
	private static final long serialVersionUID = -7816221832094682400L;
	public static final String VERSION = "2.1.1.0";                   // [0001]  
	public static final String COPYRIGHT = "Copyright (c) Software AG 1998-2002. All rights reserved."; 
    /**
     * Constructs a <code>InvalidQualifierException</code> with no 
     * detail message. 
     *
     */
	public InvalidQualifierException() {
	super();
    }

    /**
     * Constructs a <code>InvalidQualifierException</code> with 
     * the specified detail message. 
     *
     * @param   s   the detail message.
     */
    public InvalidQualifierException(String s) {
	super(s);
    }

}
