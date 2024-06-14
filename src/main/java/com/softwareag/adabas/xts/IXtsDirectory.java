/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts;


public interface IXtsDirectory {

	public void setParameters(String[] param) throws Exception;

	public void setPartition(String partition);
	
	public String getPartition();

	public void commit(boolean b) throws Exception;

	public void shutdown() throws Exception;

	public void add(String qualifier, XTSurl url) throws Exception;

	public void delete(String qualifier, XTSurl url) throws Exception;

	public void delete_ex (String qualifier, String target) throws Exception;

	public XTSurl[] retrieve(String qualifier, String target) throws Exception;
	
	public XTSurl[] retrieve(String qualifier, String target, String hostName) throws Exception;
	
	public XTSurl[] adiquery ()throws Exception;
	
	public String getUrl();

}
