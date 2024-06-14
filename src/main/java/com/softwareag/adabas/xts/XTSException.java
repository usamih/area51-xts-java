/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */
package com.softwareag.adabas.xts;
import com.softwareag.adabas.xts.XTStrace;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
/**
 * @author usarc
 * Reworked by usamih (Michael Chirila) 10/06/2022
 */
public class XTSException extends Exception {	

	private static final long serialVersionUID = 173507381415216642L;  // Eclipse generated

	public int 	xtsResponseCode = XTS_UNKNOWN_ERROR;
	public String	xtsMessage;
	public String	xtsException;
	
	/**
	 * The following return codes are used by the C version of XTS
	 */
	public final static int XTS_SEMAPHORE_FAILURE = 50;
	public final static int XTS_MEMORY_FAILURE = 51;
	public final static int XTS_SERVER_ALREADY_REGISTERED = 52;
	public final static int XTS_WRONG_URL_INFO = 53;
	public final static int XTS_PROTOCOL_LOAD_FAILED = 54;
	public final static int XTS_PROTOCOL_INIT_FAILED = 55;
	public final static int XTS_INVALID_PROTOCOL_TYPE = 56;
	public final static int XTS_LISTEN_FAILED = 57;
	public final static int XTS_ACCEPT_FAILED = 58;
	public final static int XTS_CONNECT_FAILED = 59;
	public final static int XTS_NO_DIRECTORY_INFO = 60;
	public final static int XTS_THREAD_FAILURE = 61;
	public final static int XTS_TIMEOUT = 62;
	public final static int XTS_SEND_FAILED = 63;
	public final static int XTS_INVALID_REFERENCE = 64;
	public final static int XTS_SEND_INCOMPLETE = 65;
	public final static int XTS_NO_SUCH_SERVER = 66;
	public final static int XTS_NO_MORE_COOKIES = 68;
	public final static int XTS_RESOLVE_TARGET_FAILED = 69;
	public final static int XTS_RESOLVE_TARGET_REPLY_FAILED = 70;
	public final static int XTS_TARGET_CANNOT_BE_RESOLVED = 71;              // Used by XTS JAVA also
	public final static int XTS_NOT_INITIALIZED = 72;
	public final static int XTS_BINDCLIENT_FAILED = 73;
	public final static int XTS_TOO_MANY_THREADS = 74;
	public final static int XTS_CREATE_THREAD_FAILED = 75;
	public final static int XTS_WRONG_IP_ADDRESS = 76;
	public final static int XTS_CONVERSION_FAILED = 77;
	public final static int XTS_NO_TARGET_NAME = 78;
	public final static int XTS_NO_CLIENT_NAME = 79;
	public final static int XTS_GETHOSTBYNAME_FAILED = 80;
	public final static int XTS_GETHOSTBYADDR_FAILED = 81;
	public final static int XTS_ZERO_LENGTH = 82;
	public final static int XTS_INVALID_TARGETID = 83;
	public final static int XTS_CHANNEL_INACTIVE = 84;
	public final static int XTS_DISCONNECT = 85;
	public final static int XTS_HASHTABLE_ERROR = 86;
	public final static int XTS_KEY_FAILURE = 87;
	public final static int XTS_NO_URL_LINKS = 88;
	public final static int XTS_PROTOCOL_NOT_SUPPORTED = 89;
	public final static int XTS_SINGLE_THREAD_NOT_SUPPORTED = 90;
	public final static int XTS_NO_ACTIVE_SERVER = 91;
	public final static int XTS_NO_MATCHING_COOKIE = 92;
	public final static int XTS_INVALID_PORT = 93;
	public final static int XTS_ZERO_POINTER = 94;
	public final static int XTS_HASH_TABLE_FULL = 95;
	public final static int XTS_UXSEND_FAILED = 96;
	public final static int XTS_UXRECV_FAILED = 97;
	public final static int XTS_UX_LOAD_FAILED = 98;
	public final static int XTS_MESSAGE_REJECTED = 99;
	public final static int XTS_SHUTDOWN_IN_PROCESS = 100;
	public final static int XTS_ROUTE_TIMEOUT = 101;                         // Used by XTS JAVA also                
	public final static int XTS_ROUTE_TABLE_OVERFLOW = 102;                  // Used by XTS JAVA also
	public final static int XTS_ROUTE_TTL_EXPIRED = 103;                     // Used by XTS JAVA also
	public final static int XTS_ROUTE_FAILED = 104;                          // Used by XTS JAVA also
	public final static int XTS_NULL_PARAMETER = 105;
	public final static int XTS_CONNECTION_REJECTED = 106;
	public final static int XTS_REPLICA_REDIRECT = 107;
	public final static int XTS_TIMEOUT_CONNECT_PENDING = 108;
	public final static int XTS_INVALID_SESSION = 109;
	public final static int XTS_INVALID_KEY = 110;
	public final static int XTS_COUNT_CONVERTERS_FAILED = 111;
	public final static int XTS_CONVERT_ENUM_FAILED = 112;
	public final static int XTS_BUFFER_NOT_BIG_ENOUGH = 113;
	public final static int XTS_NO_PORTS_AVAILABLE = 114;
	public final static int XTS_TXT_CONVERTER_LOAD_FAILED = 115;
	public final static int XTS_TXT_CONVERTER_RESOLVE_FAILED = 116;
	public final static int XTS_NAME_TO_EDDKEY_ERR = 117;
	public final static int XTS_XDS_LIBRARY_LOAD_FAILED = 118;
	public final static int XTS_DS_TIMEOUT = 119;
	public final static int XTS_ECS_ENVIRONMENT_FAILED = 120;
	public final static int XTS_SET_UDP_PORT_FAILED = 121;
	public final static int XTS_SYNC_ASYNC_NOT_ALLOWED = 122;
	public final static int XTS_UNKNOWN_BUFFER = 123;
	public final static int XTS_FUNCTION_RETIRED = 124;
	public final static int XTS_INVALID_PARAMETER = 125;
	public final static int XTS_INVALID_DUPLICATE_SERVER = 126;
	public final static int XTS_FATAL_ERROR = 127;
	public final static int XTS_LOAD_LIB_FAILED = 128;
	public final static int XTS_KEY_NOT_FOUND = 129;
	public final static int XTS_CONFIG_KEY_NOT_FOUND = 130;
	public final static int XTS_PROFILE_NO_DIRECTORY_INFO = 131;
	public final static int XTS_LOAD_FUNCTION_FAILED = 132;
	public final static int XTS_STREAMING_NOT_ALLOWED = 133;

	public final static int XTS_SSL_INIT_FAILED = 150;
	public final static int XTS_SSL_WRONG_CA_LOCATIONS = 151;
	public final static int XTS_SSL_NO_CERTIFICATE = 152;
	public final static int XTS_SSL_INVALID_CERTIFICATE = 153;
	public final static int XTS_SSL_INVALID_KEYFILE = 154;
	public final static int XTS_SSL_INVALID_KEY = 155;
	public final static int XTS_SSL_CONNECT_FAILED = 156;
	public final static int XTS_SSL_ACCEPT_FAILED = 157;
	public final static int XTS_SSL_READ_FAILED = 158;
	public final static int XTS_SSL_WRITE_FAILED = 159;
	public final static int XTS_SSL_NULL_PARAMETER = 160;
	public final static int XTS_SSL_INVALID_PARAMETER = 161;
	public final static int XTS_SSL_INVALID_VALUE = 162;
	public final static int XTS_SSL_LOAD_LIB_FAILED = 163;
	public final static int XTS_SSL_NO_SESSION = 164;
	public final static int XTS_SSL_RENEGOTIATION_FAILED = 165;
	public final static int XTS_SSL_NO_RANDOM_FILE_ERROR = 166;
	public final static int XTS_SSL_ILLEGAL_HOSTNAME = 167;
	public final static int XTS_SSL_NO_LOCAL_CERT = 168;
	public final static int XTS_SSL_NO_REMOTE_CERT = 169;
	public final static int XTS_SSL_INSUFFICIENT_MEMORY = 170;
	public final static int XTS_SSL_CERT_REJECTED = 171;
	public final static int XTS_SSL_ZERO_RETURN = 172;
	public final static int XTS_SSL_ERROR_WANT_READ = 173;
	public final static int XTS_SSL_ERROR_WANT_WRITE = 174;
	
	public final static int DS_OPENFILE_FAILED = 200;
	public final static int DS_INCOMPLETE_ENTRY = 201;
	public final static int DS_WRONG_PARAMETER = 202;
	public final static int DS_SERVICE_NOT_PROVIDED = 203;
	public final static int DS_NULL_PARAM = 204;
	public final static int DS_URL_INCOMPLETE_ENTRY = 205;
	public final static int DS_URL_TOPIC_NOT_DEFINED = 206;
	public final static int DS_UNKNOWN_REQUEST = 207;
	public final static int DS_NO_ENTRIES = 208;
	public final static int DS_DELETE_FAILED = 209;
	public final static int DS_CONNECTION_FAILED = 210;
	public final static int DS_BIND_FAILED = 211;
	public final static int DS_SEARCH_FAILED = 212;
	public final static int DS_ADD_FAILED = 213;
	public final static int DS_ALREADY_EXISTS = 214;
	public final static int DS_LOAD_FAILED = 215;
	public final static int DS_WRONG_DIRECTORY_PARAM = 216;
	public final static int DS_NOT_INITIALIZED = 217;
	public final static int DS_INVALID_TARGET_NAME = 218;
	public final static int DS_INCOMPLETE_REQUEST = 219;
	public final static int DS_NO_URL_ENTRIES = 220;
	public final static int DS_XDS_ERROR = 221;
	public final static int DS_NO_ROLLBACK = 222;
	public final static int DS_OVO_ERROR = 223;
	public final static int DS_ONETIME_SET_VIOLATION = 224;
	public final static int DS_ENCODING_CONFLICT = 225;
	public final static int DS_CREATEFILE_FAILED = 226;
	public final static int DS_IOFILE_FAILED = 227;
	public final static int DS_WRONG_FILE_FORMAT = 228;
	public final static int DS_SEND_BUFFER_TOO_BIG = 229;
	/**
	 * XTS JAVA response codes
	 */
	public final static int XTS_UNKNOWN_EXCEPTION              = 512;
	public final static int XTS_UNKNOWN_HOST_EXCEPTION         = 513;	
	public final static int XTS_SECURITY_EXCEPTION             = 514;
	public final static int XTS_IO_EXCEPTION                   = 515;
	public final static int XTS_ILLEGAL_ARGUMENT_EXCEPTION     = 516;
	public final static int XTS_INVALID_RETURN_SENDANDWAIT     = 517;
	public final static int XTS_REGISTER_SERVER_NO_DIR         = 518;
	public final static int XTS_TARGET_UNREACHABLE             = 519;
	public final static int XTS_CLIENT_WAIT_INTERRUPTED        = 520;
	public final static int XTS_RECEIVE_FAILURE                = 521;
	public final static int XTS_INVALID_A1_A2_HEADER           = 522;
	public final static int XTS_SOCKET_EXCEPTION               = 523;
	public final static int XTS_SEND_RECV_TIMEOUT              = 524;
	public final static int XTS_IDIR_ADD_INVALID_PARMS         = 525;
	public final static int XTS_DIR_ADD_DUPLICATE_URL          = 526;
	public final static int XTS_FILE_NOT_FOUND                 = 527;
	public final static int XTS_DIR_CONFIG_ERROR               = 528;
	public final static int XTS_DIR_SET_PARMETERS_ERROR        = 529;
	public final static int XTS_NULL_URL                       = 530;     
	public final static int XTS_URL_MISSING_TARGET             = 531;
	public final static int XTS_DIRSRV_INVALID_RESPONSE        = 532;
	public final static int XTS_DIR_INVALID_QUALIFIER          = 533;
	public final static int XTS_DDIR_SETP_INVALID_PARM         = 534;
	public final static int XTS_DDIR_DSRV_CONTACT_ERROR        = 535;
	public final static int XTS_DDIR_OPERATION_NULL            = 536;
	public final static int XTS_DDIR_DSRV_REQUEST_LERROR       = 537;
	public final static int XTS_DIR_IO_ERROR                   = 538;
	public final static int XTS_XTSURL_INVALID_URL             = 539;
	public final static int XTS_NO_ROUTE_SET                   = 540; 
	public final static int XTS_SEND_VIA_RETURN_FAILED         = 541; 
	public final static int XTS_INVALID_GETMSG_LEN             = 542; 
	public final static int XTS_INVALID_GETMSG_HDR             = 543;

   	public final static int XTS_UNKNOWN_ERROR                  = 1000;
   	
 	/**
	 * Constructor with no arguments is invalid.
	 * 
	 * @throws Exception
	 */
	public XTSException () throws Exception {
		throw new Exception("Unknown XTS Exception - no message or response code!");
	}
	
	/**
	 * Constructor with only a message argument.
	 * 
	 * @param message	Exception message.
	 */
	public XTSException (Exception ex) {
		
		this.xtsMessage = ex.getMessage();
			
		XTStrace.error("XTSException(): " + xtsMessage);        
		/**
		 * The following Exceptions can happen when attempting to establish a socket connection
		 * An IO_EXCEPTION can also occur when receiving or sending on a socket 
		 */
		if (ex.getClass().equals(UnknownHostException.class)) {
			this.xtsResponseCode = XTS_UNKNOWN_HOST_EXCEPTION;
			this.xtsException = "UnknownHostException";
		}
		else if (ex.getClass().equals(SecurityException.class)) {
			this.xtsResponseCode = XTS_SECURITY_EXCEPTION;
			this.xtsException = "SecurityException";
		}
		else if (ex.getClass().equals(IOException.class)) {
			this.xtsResponseCode = XTS_IO_EXCEPTION;
			this.xtsException = "IOException";
		}
		else if (ex.getClass().equals(IllegalArgumentException.class)) {
			this.xtsResponseCode = XTS_ILLEGAL_ARGUMENT_EXCEPTION;
			this.xtsException = "IllegalArgumentException";
		}
		else if (ex.getClass().equals(SocketException.class)) {
			this.xtsResponseCode = XTS_SOCKET_EXCEPTION;
			this.xtsException = "SocketException";
		}
	}
		
	/**
	 * Constructor with only a message argument.
	 * 
	 * @param message	Exception message.
	 */
	public XTSException (String message) {		
		this.xtsMessage= message;			
		XTStrace.error("XTSException(): Message=" + xtsMessage);
	}
	
	public XTSException (int traceFlag, String message) {		
		this.xtsMessage= message;			
		XTStrace.debug("XTSException(): Message=" + xtsMessage);
	}
	/**
	 * Constructor with only a message argument.
	 * 
	 * @param message	Exception message.
	 */
	public XTSException (int rc) {		
		this.xtsResponseCode = rc;
		this.xtsMessage = getRCText(rc);
		XTStrace.error("XTSException(): Rc=" + rc + " XtsMessage=" + xtsMessage);
	}
	
	/**
	 * Constructor with a DBID and response code.
	 * 
	 * @param dbid Database ID.
	 * @param rc   Response code.
	 */
	public XTSException(String message, int rc) {		
		this.xtsResponseCode 	= rc;
		this.xtsMessage		= message;		
		XTStrace.error("XTSException(): Rc=" + rc + " Message=" + xtsMessage);
	}

	@Override
	/**
	 * Get message.
	 * 
	 * Override for superclass method to return Adabas specific exception method.
	 */
	public String getMessage () {		
		return xtsMessage;
	}
	
	/**
	 * Get response code.
	 * 
	 * @return Adabas response code causing exception.
	 */
	public int getResponseCode() {		
		return this.xtsResponseCode;
	}
		
	/**
	 * Response Code Enumeration.
	 * 
	 * It is here that response codes are associated with text 
	 *  
	 * The response code constructor is of the form:
	 * 
	 * Name (ResponseCode,	ResponseText)
	 */
	protected enum RC {		
	    XTS_SUCCESS					((int) 0, 	"Success"),
	    XTS_SEMAPHORE_FAILURE 			((int) 50, 	"Semaphore error"),
	    XTS_MEMORY_FAILURE				((int) 51, 	"Memory allocation Error"),
	    XTS_SERVER_ALREADY_REGISTERED		((int) 52, 	"Server already registered"),
	    XTS_WRONG_URL_INFO				((int) 53, 	"Incorrect URL"),
	    XTS_PROTOCOL_LOAD_FAILED			((int) 54, 	"Failure loading protocol handler"),
	    XTS_PROTOCOL_INIT_FAILED			((int) 55,	"Protocol init failed"),
	    XTS_INVALID_PROTOCOL_TYPE 			((int) 56, 	"Invalid protocol type"),
	    XTS_LISTEN_FAILED				((int) 57,	"Listen failed"),
	    XTS_ACCEPT_FAILED				((int) 58, 	"Accept failed"),
	    XTS_CONNECT_FAILED				((int) 59,	"Connect failed"),
	    XTS_NO_DIRECTORY_INFO 			((int) 60, 	"No directory information"),
	    XTS_THREAD_FAILURE				((int) 61, 	"Failure to create a thread"),
	    XTS_TIMEOUT					((int) 62, 	"Timeout error"),
	    XTS_SEND_FAILED				((int) 63, 	"Send failed"),
	    XTS_INVALID_REFERENCE			((int) 64, 	"Invalid reference"),
	    XTS_SEND_INCOMPLETE 			((int) 65,	"Send incomplete"),
	    XTS_NO_SUCH_SERVER				((int) 66, 	"No such server"),
	    XTS_NO_MORE_COOKIES				((int) 68,	"No more cookies"),
	    XTS_RESOLVE_TARGET_FAILED			((int) 69,	"Resolve target failed"),
	    XTS_RESOLVE_TARGET_REPLY_FAILED		((int) 70, 	"Resolve target reply failed"),
	    XTS_TARGET_CANNOT_BE_RESOLVED		((int) 71, 	"Target cannot be resolved"),
	    XTS_NOT_INITIALIZED				((int) 72, 	"Not initialized"),
	    XTS_BINDCLIENT_FAILED			((int) 73, 	"Bind Client failed"),
	    XTS_TOO_MANY_THREADS			((int) 74, 	"Too many user threads"),
	    XTS_CREATE_THREAD_FAILED			((int) 75,	"Created thread failed"),
	    XTS_WRONG_IP_ADDRESS			((int) 76, 	"Wrong IP address"),
	    XTS_CONVERSION_FAILED			((int) 77,	"Conversion failed"),
	    XTS_NO_TARGET_NAME 				((int) 78, 	"No target name"),
	    XTS_NO_CLIENT_NAME				((int) 79,	"No client Name"),
	    XTS_GETHOSTBYNAME_FAILED			((int) 80, 	"GETHOSTBYNAME failed"),
	    XTS_GETHOSTBYADDR_FAILED			((int) 81, 	"GETHOSTBYADDR failed"),
	    XTS_ZERO_LENGTH				((int) 82, 	"Zero length not accepted"),
	    XTS_INVALID_TARGETID			((int) 83, 	"Target ID out of range"),
	    XTS_CHANNEL_INACTIVE			((int) 84, 	"Channel inactive"),
	    XTS_DISCONNECT				((int) 85,	"Disconnect message"),
	    XTS_HASHTABLE_ERROR				((int) 86, 	"Hash table error"),
	    XTS_KEY_FAILURE				((int) 87,	"Thread key error"),
	    XTS_NO_URL_LINKS				((int) 88, 	"No CONNECT or LISTEN has been issued"),
	    XTS_PROTOCOL_NOT_SUPPORTED			((int) 89,	"Protocol not supported"),
	    XTS_SINGLE_THREAD_NOT_SUPPORTED 		((int) 90, 	"Single thread not supported"),
	    XTS_NO_ACTIVE_SERVER			((int) 91, 	"No active server"),
	    XTS_NO_MATCHING_COOKIE			((int) 92, 	"No matching cookie"),
	    XTS_INVALID_PORT				((int) 93, 	"Invalid port"),
	    XTS_ZERO_POINTER 				((int) 94, 	"Zero pointer"),
	    XTS_HASH_TABLE_FULL 			((int) 95, 	"Hash table full"),
	    XTS_UXSEND_FAILED				((int) 96,	"Send user exit failed"),
	    XTS_UXRECV_FAILED				((int) 97, 	"Recv user exit failed"),
	    XTS_UX_LOAD_FAILED				((int) 98, 	"Load user exit failed"),
	    XTS_MESSAGE_REJECTED			((int) 99,	"Message rejected"),
	    XTS_SHUTDOWN_IN_PROCESS			((int) 100, "Shutdown in process"),
	    XTS_ROUTE_TIMEOUT				((int) 101, "Route timeout"),
	    XTS_ROUTE_TABLE_OVERFLOW			((int) 102, "Route table overflow"),
	    XTS_ROUTE_TTL_EXPIRED			((int) 103, "Route TTL expired"),
	    XTS_ROUTE_FAILED				((int) 104, "Route failed"),
	    XTS_NULL_PARAMETER				((int) 105, "Null parameter"),
	    XTS_CONNECTION_REJECTED			((int) 106, "Connection rejected"),
	    XTS_REPLICA_REDIRECT 			((int) 107, "Replica redirected"),
	    XTS_TIMEOUT_CONNECT_PENDING			((int) 108, "Timeout - connect in progress"),
	    XTS_INVALID_SESSION				((int) 109, "Invalid session"),
	    XTS_INVALID_KEY				((int) 110, "Invalid key"),
	    XTS_COUNT_CONVERTERS_FAILED			((int) 111, "Count converters failed"),
	    XTS_CONVERT_ENUM_FAILED			((int) 112, "Convert enum failed"),
	    XTS_BUFFER_NOT_BIG_ENOUGH 			((int) 113, "Buffer too small"),
	    XTS_NO_PORTS_AVAILABLE			((int) 114, "No more ports available"),
	    XTS_TXT_CONVERTER_LOAD_FAILED 		((int) 115, "Text converter library failed"),
	    XTS_TXT_CONVERTER_RESOLVE_FAILED 		((int) 116, "Text converter resolve failed"),
	    XTS_NAME_TO_EDDKEY_ERR 			((int) 117, "Name to EDDkey map failed"),
	    XTS_XDS_LIBRARY_LOAD_FAILED			((int) 118, "XDS library loading failed"),
	    XTS_DS_TIMEOUT				((int) 119, "DS access time out error"),
	    XTS_ECS_ENVIRONMENT_FAILED			((int) 120, "Failed to set ECS environment"),
	    XTS_SET_UDP_PORT_FAILED			((int) 121, "Set UDP port failed"),
	    XTS_SYNC_ASYNC_NOT_ALLOWED 			((int) 122, "Sync and async send in raw mode not allowed"),
	    XTS_UNKNOWN_BUFFER				((int) 123, "Unknown received buffer"),
	    XTS_FUNCTION_RETIRED 			((int) 124, "Function retired"),
	    XTS_INVALID_PARAMETER			((int) 125, "Invalid parameter"),
	    XTS_INVALID_DUPLICATE_SERVER		((int) 126, "invalid duplicate server"),
	    XTS_FATAL_ERROR 				((int) 127, "Fatal error"),
	    XTS_LOAD_LIB_FAILED 			((int) 128, "Load library failed"),
	    XTS_KEY_NOT_FOUND				((int) 129, "Key not found"),
	    XTS_CONFIG_KEY_NOT_FOUND 			((int) 130, "Xts.Config Key not found"),
	    XTS_PROFILE_NO_DIRECTORY_INFO 		((int) 131, "No directory information for XTS profile"),
	    XTS_LOAD_FUNCTION_FAILED			((int) 132, "Load function failed"),
	    XTS_STREAMING_NOT_ALLOWED 			((int) 133, "Streaming not allowed"),
	    XTS_SSL_INIT_FAILED				((int) 150, "SSL Init failed"),
	    XTS_SSL_WRONG_CA_LOCATIONS			((int) 151, "SSL Verify CA locations failed"),
	    XTS_SSL_NO_CERTIFICATE 			((int) 152, "SSL Certificate file not specified"),
	    XTS_SSL_INVALID_CERTIFICATE			((int) 153, "SSL Invalid certificate"),
	    XTS_SSL_INVALID_KEYFILE			((int) 154, "SSL Invalid key file"),
	    XTS_SSL_INVALID_KEY				((int) 155, "SSL Invalid key"),
	    XTS_SSL_CONNECT_FAILED			((int) 156, "SSL Connect failed"),
	    XTS_SSL_ACCEPT_FAILED			((int) 157, "SSL Accept failed"),
	    XTS_SSL_READ_FAILED 			((int) 158, "SSL Read failed"),
	    XTS_SSL_WRITE_FAILED			((int) 159, "SSL Write failed"),
	    XTS_SSL_NULL_PARAMETER 			((int) 160, "SSL Null parameter"),
	    XTS_SSL_INVALID_PARAMETER 			((int) 161, "SSL Invalid parameter"),
	    XTS_SSL_INVALID_VALUE 			((int) 162, "SSL Invalid value"),
	    XTS_SSL_LOAD_LIB_FAILED			((int) 163, "SSL Failed loading library"),
	    XTS_SSL_NO_SESSION 				((int) 164, "SSL No session"),
	    XTS_SSL_RENEGOTIATION_FAILED 		((int) 165, "SSL Renegotiation failed"),
	    XTS_SSL_NO_RANDOM_FILE_ERROR		((int) 166, "SSL No random file error"),
	    XTS_SSL_ILLEGAL_HOSTNAME			((int) 167, "SSL Illegal host name"),
	    XTS_SSL_NO_LOCAL_CERT			((int) 168, "SSL No locale certificate"),
	    XTS_SSL_NO_REMOTE_CERT			((int) 169, "SSL No remote certificate"),
	    XTS_SSL_INSUFFICIENT_MEMORY 		((int) 170, "SSL Insufficient user memory"),
	    XTS_SSL_CERT_REJECTED 			((int) 171, "SSL Certificate rejected"),
	    XTS_SSL_ZERO_RETURN				((int) 172, "SSL Zero length message returned"),
	    XTS_SSL_ERROR_WANT_READ 			((int) 173, "SSL Want read error"),
	    XTS_SSL_ERROR_WANT_WRITE 			((int) 174, "SSL Want write error"),
	    DS_OPENFILE_FAILED  			((int) 200, "DS Open file failed"),
	    DS_INCOMPLETE_ENTRY   			((int) 201, "DS Incomplete entry"),
	    DS_WRONG_PARAMETER   			((int) 202, "DS Wrong parameter"),
	    DS_SERVICE_NOT_PROVIDED 			((int) 203, "DS Service not provided"),
	    DS_NULL_PARAM	    			((int) 204, "DS Null parameter"),
	    DS_URL_INCOMPLETE_ENTRY			((int) 205, "DS Incomplete entry"),
	    DS_URL_TOPIC_NOT_DEFINED			((int) 206, "DS URL topic not defined"),
	    DS_UNKNOWN_REQUEST   			((int) 207, "DS Unknown request"),
	    DS_NO_ENTRIES       			((int) 208, "DS No entries"),
	    DS_DELETE_FAILED    			((int) 209, "DS Delete failed"),
	    DS_CONNECTION_FAILED   			((int) 210, "DS Connection failed"),
	    DS_BIND_FAILED		   		((int) 211, "DS Bind failed"),
	    DS_SEARCH_FAILED	   			((int) 212, "DS Search failed"),
	    DS_ADD_FAILED				((int) 213, "DS Add failed"),
	    DS_ALREADY_EXISTS    			((int) 214, "DS Already exists"),
	    DS_LOAD_FAILED				((int) 215, "DS Failure loading service handler"),
	    DS_WRONG_DIRECTORY_PARAM			((int) 216, "DS Wrong directory parameter"),
	    DS_NOT_INITIALIZED    			((int) 217, "DS Not initialized"),
	    DS_INVALID_TARGET_NAME 			((int) 218, "DS Invalid target Name"),
	    DS_INCOMPLETE_REQUEST  			((int) 219, "DS Incomplete request"),
	    DS_NO_URL_ENTRIES    			((int) 220, "No directory entry found"),
	    DS_XDS_ERROR 		   		((int) 221, "DS XDS error"),
	    DS_NO_ROLLBACK  	   			((int) 222, "DS Rollback is not implemented"),
	    DS_OVO_ERROR			 	((int) 223, "DS OVO error"),
	    DS_ONETIME_SET_VIOLATION	    		((int) 224, "DS One time set violation"),
	    DS_ENCODING_CONFLICT			((int) 225, "DS Encoding conflict"),
	    DS_CREATEFILE_FAILED    			((int) 226, "DS Create file failed"),
	    DS_IOFILE_FAILED     			((int) 227, "DS IO file failed"),
	    DS_WRONG_FILE_FORMAT 			((int) 228, "DS Wrong file format"),
	    DS_SEND_BUFFER_TOO_BIG 			((int) 229, "DS Send buffer bigger than maximum allowed"),
	    
	    /**
	     *     JAVA XTS specific exceptions/errors
	     **/
	    XTS_UNKNOWN_EXCEPTION           ((int) 512, "Unknown Exception"), 
	    XTS_UNKNOWN_HOST_EXCEPTION      ((int) 513, "Unknown Host Exception : hostname"),  
	    XTS_SECURITY_EXCEPTION          ((int) 514, "Security Exception"),
	    XTS_IO_EXCEPTION                ((int) 515, "IO Exception"),
	    XTS_ILLEGAL_ARGUMENT_EXCEPTION  ((int) 516, "Illegal Argument Exception"),
	    XTS_INVALID_RETURN_SENDANDWAIT  ((int) 517, "Invalid return to sendAndWait"),
	    XTS_REGISTER_SERVER_NO_DIR      ((int) 518, "Can't register the server=x... : No Directory Information"),
	    XTS_TARGET_UNREACHABLE          ((int) 519, "Can't reach target"),
	    XTS_CLIENT_WAIT_INTERRUPTED     ((int) 520, "Client wait interrupted"),
	    XTS_RECEIVE_FAILURE             ((int) 521, "Receive Failed to complete"),
	    XTS_INVALID_A1_A2_HEADER        ((int) 522, "Invalid A1/A2 Header"),
	    XTS_SOCKET_EXCEPTION            ((int) 523, "Socket Exception"),
	    XTS_SEND_RECV_TIMEOUT           ((int) 524, "Send/Receive timeout"),
	    XTS_IDIR_ADD_INVALID_PARMS      ((int) 525, "Directory add method : Invalid Parameters = ..."),
	    XTS_DIR_ADD_DUPLICATE_URL       ((int) 526, "Directory add method : Duplicate URL"),
	    XTS_FILE_NOT_FOUND              ((int) 527, "File filename Not Found"),
	    XTS_DIR_CONFIG_ERROR            ((int) 528, "Directory configuration error"),
	    XTS_DIR_SET_PARMETERS_ERROR     ((int) 529, "Directory setParameters : Syntax error in parameters ... position nnnn"),
	    XTS_NULL_URL                    ((int) 530, "Directory null URL"),
	    XTS_URL_MISSING_TARGET          ((int) 531, "Directory URL does not contain a target"),
	    XTS_DIRSRV_INVALID_RESPONSE     ((int) 532, "Directory invalid response received from the Directory Server"),
	    XTS_DIR_INVALID_QUALIFIER       ((int) 533, "Directory invalid qualifier = ..."),
	    XTS_DDIR_SETP_INVALID_PARM      ((int) 534, "Directory setParameters : Invalid value for parameter parm=value"),
	    XTS_DDIR_DSRV_CONTACT_ERROR     ((int) 535, "Directory : Directory Server contact error ..."),
	    XTS_DDIR_OPERATION_NULL         ((int) 536, "Directory : No Operation specified for Directory Server Request"),
	    XTS_DDIR_DSRV_REQUEST_LERROR    ((int) 537, "Directory : Directory Server request exceeds 65535 bytes"),
	    XTS_DIR_IO_ERROR                ((int) 538, "Directory : I/O Error: ...."),   
	    XTS_XTSURL_INVALID_URL          ((int) 539, "Invalid URL = ..."),
	    XTS_NO_ROUTE_SET                ((int) 540, "No route set in message"),
	    XTS_SEND_VIA_RETURN_FAILED      ((int) 541, "Send via return failed"),
	    XTS_INVALID_GETMSG_LEN          ((int) 542, "Message getMessage : Invalid message length received=nnn"),
            XTS_INVALID_GETMSG_HDR          ((int) 543, "Message getMessage : Invalid message header received" ),  	    
	    XTS_UNKNOWN_ERROR               ((int) 1000,  "unknown error=(code)"),		
	    XTS_DEFAULT_RESPONSE_CODE       ((int) 99999, "Response Code has not been assigned");
		
	
		protected int		rc;                     // decimal response code
		protected String	rcText;			// response code error text		
		/**
		 * Constructor taking response code and meaning.
		 * 
		 * @param rc	Response code.
		 * @param text	Meaning.
		 */
		private RC(int rc, String text) {
			
			this.rc 	= rc;
			this.rcText	= text;
		}
	}

	/**
	 * Get response code text from enumeration using only rc.
	 * Returns 1st match on rc only.
	 *  
	 * @param rc Response code.
	 * @return   Error text.
	 */
	public static String getRCText(int rc) {		
		for (RC responseCode:RC.values()) {				 		
			if (responseCode.rc == rc) {						
				return responseCode.rcText;							
			}
		}	
		return "Unknown";											
	}
	
	/**
	 * Print all response code texts.
	 * 
	 * Used for documentation feeds.
	 */
	
	public static void printAllRCTexts() {
		for (RC responseCode : RC.values()) {						// loop thru enum values
			System.out.println(responseCode.rcText);				// print response text
		}
		System.out.println(String.format("Total number of ResponseCodes = %d", RC.values().length));
	}
	
}
