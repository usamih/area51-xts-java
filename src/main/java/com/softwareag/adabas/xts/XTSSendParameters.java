/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
//   Maintenance:
//   Reworked by usamih (Michael Chirila) 10/06/2022

package com.softwareag.adabas.xts;

import com.softwareag.adabas.xts.interfaces.IXTSreceiver;
import com.softwareag.adabas.xts.interfaces.IXTStransmitter;

public class XTSSendParameters {
	/**
	 * targetName and targeId are mutually exclusive.
	 * One must be specified to identify the target name or ID being requested.
	 */
	String  targetName = null;
	String  aliasName = null;
	int     targetId = -1;
	
	/**
	 * The message to be sent to the server.
	 */
	Message p = null;
	
	/**
	 * txcb points to an object that implements the IXTStransmitter interface.
	 * txuserval is an object that is to be passed to txcb IXTStransmitter interface
	 * methods.
	 */
	IXTStransmitter txcb = null;
	Object txuserval = null;
	
	/**
	 * rxcb points to an object that implements the IXTSreceiver interface.
	 * rxuserval is an object that is to be passed to rxcb IXTSreceiver interface
	 * methods.
	 */
	IXTSreceiver rxcb = null;
	Object rxuserval = null;
	
	/**
	 * The amount of time to wait for a reply in milliseconds.
	 */
	long timeout = 0;
	
	/**
	 * The amount of time to wait for a connection to be established.
	 */
	int connTo = 0;
		
	/**
	 * There can be multiple entries in the directory server that match the
	 *  targetId/targetName being requested. The hostname value when specified 
	 *  says to use the 1st url returned that who's hostname matches this one. 
	 * 
	 */
	String  hostName = null;                                
	XTSurl  url = null;                                

	/**
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            The message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            The period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 */
	public XTSSendParameters(int targetId, Message p, long timeout) {		
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
	}
	
	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 */
	public XTSSendParameters(String targetName, Message p, long timeout) {
		
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
	}
	
	/**
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            The message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            The period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param hostName           
	 *            The hostname where the server(targetId) is located. 
	 *            
	 */
	public XTSSendParameters(int targetId, Message p, long timeout, String hostName) {		
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.hostName = hostName;
	}
	
	/**
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            The message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            The period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param url           
	 *            The url of the server(targetId). 
	 *            
	 */
	public XTSSendParameters(int targetId, Message p, long timeout, XTSurl url) {		
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.url = url;
	}

	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param hostName           
	 *            The hostname where the server(targetName) is located. 
	 *            
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, String hostName) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.hostName = hostName;
	}

	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param url           
	 *            The url of the server(targetName). 
	 *            
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, XTSurl url) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.url = url;
	}

	public XTSSendParameters(String targetName, String aliasName, Message p, long timeout, XTSurl url) {
		this.targetName = targetName;
		this.aliasName = aliasName;
		this.p = p;
		this.timeout = timeout;
		this.url = url;
	}

	/**
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            The message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            The period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param hostName           
	 *            The hostname where the server(targetId) is located. 
	 * @param connTo           
	 *            The time to wait for connection to be established. 
	 *            
	 */
	public XTSSendParameters(int targetId, Message p, long timeout, String hostName, int connTo) {		
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.hostName = hostName;
		this.connTo = connTo;
	}
	
	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param hostName           
	 *            The hostname where the server(targetName) is located. 
	 * @param connTo           
	 *            The time to wait for connection to be established. 
	 *            
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, String hostName, int connTo) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.hostName = hostName;
		this.connTo = connTo;
	}
	
	
	/**
	 * 
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected.
	 * @param rxuserval
	 *            an object to be passed to the receive complete method.
	 */
	public XTSSendParameters(int targetId, Message p, long timeout,  IXTSreceiver rxcb, Object rxuserval) {
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.rxcb = rxcb;
		this.rxuserval = rxuserval;
	}
	
	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected.
	 * @param rxuserval
	 *            an object to be passed to the receive complete method.
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, IXTSreceiver rxcb, Object rxuserval) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.rxcb = rxcb;
		this.rxuserval = rxuserval;
	}	
	

	/**
	 * 
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param txuserval
	 *            an object to be passed to the transmit complete method.
	 */
	public XTSSendParameters(int targetId, Message p, long timeout, IXTStransmitter txcb, Object txuserval) {
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.txcb = txcb;
		this.txuserval = txuserval;
	}
	
	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param txuserval
	 *            an object to be passed to the transmit complete method.
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, IXTStransmitter txcb, Object txuserval) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.txcb = txcb;
		this.txuserval = txuserval;
	}	
	
	/**
	 * 
	 * @param targetID
	 *            the numeric target ID of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param txuserval
	 *            an object to be passed to the transmit complete method.
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected.
	 * @param rxuserval
	 *            an object to be passed to the receive complete method.
	 */
	public XTSSendParameters(int targetId, Message p, long timeout, IXTStransmitter txcb, Object txuserval, IXTSreceiver rxcb, Object rxuserval) {
		this.targetId = targetId;
		this.p = p;
		this.timeout = timeout;
		this.txcb = txcb;
		this.txuserval = txuserval;
		this.rxcb = rxcb;
		this.rxuserval = rxuserval;
	}
	
	/**
	 * 
	 * @param targetName
	 *            the name of the server.
	 * @param p
	 *            the message to send. The target need not have been set in the
	 *            message.
	 * @param timeout
	 *            the period in milliseconds to wait before assuming that no
	 *            reply will be received. After the specified period, the method
	 *            will return <i>null</i>.
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param txuserval
	 *            an object to be passed to the transmit complete method.
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected.
	 * @param rxuserval
	 *            an object to be passed to the receive complete method.
	 */
	public XTSSendParameters(String targetName, Message p, long timeout, IXTStransmitter txcb, Object txuserval, IXTSreceiver rxcb, Object rxuserval) {
		this.targetName = targetName;
		this.p = p;
		this.timeout = timeout;
		this.txcb = txcb;
		this.txuserval = txuserval;
		this.rxcb = rxcb;
		this.rxuserval = rxuserval;
	}	

	/**
	 * @param hostName
	 *            The hostname where the server(targetId) is located. 
	 */
	public void SetHostNameParameter(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @param url
	 *            The url of the server(targetId). 
	 */
	public void SetUrlParameter(XTSurl url) {
		this.url = url;
	}

	/**
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param txuserval
	 *            an object to be passed to the transmit complete method.
	 */
	public void SetTransmitterParameters(IXTStransmitter txcb, Object txuserval) {
		this.txcb = txcb;
		this.txuserval = txuserval;
	}
	
	/**
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected.
	 * @param rxuserval
	 *            an object to be passed to the receive complete method.
	 */
	public void SetReceiverParameters(IXTSreceiver rxcb, Object rxuserval) {		
		this.rxcb = rxcb;
		this.rxuserval = rxuserval;
	}

    /**
	 * @param txcb
	 *            indicates the method to call back upon transmission
	 *            completion. If provided, then responsibility to free the
	 *            message p rests on this routine, otherwise it is free by XTS.
	 * @param rxcb
	 *            indicates the method to call back upon receipt of a reply. If
	 *            provided, then XTS will assume a reply is expected. 
	 */              
	public void SetTransmitterAndReceiveParameters(IXTStransmitter txcb, IXTSreceiver rxcb) {			
		this.txcb = txcb;
		this.rxcb = rxcb;
	}
	
	/**
	 * @param connTo
	 *            The time to wait for a connection to be established.
	 *            May only be effective if less than the default value used by the OS TCP/IP driver. 
	 *             
	 */
	public void SetConnectTimeoutParameter(int connTo) {
		this.connTo = connTo;
	}


}
