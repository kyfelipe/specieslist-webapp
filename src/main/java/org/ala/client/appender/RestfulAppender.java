/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.client.appender;

import org.ala.client.model.LogEventVO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Log4J appender for JSON based REST Web Service.
 * 
 * @author MOK011
 *
 */
public class RestfulAppender extends AppenderSkeleton {
	
	private String urlTemplate;
	private String username;
	private String password;
	
	private MultiThreadedHttpConnectionManager connManager = new MultiThreadedHttpConnectionManager();

	public void setUrlTemplate(String urlTemplate) {
		this.urlTemplate = urlTemplate;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	protected void append(LoggingEvent event) {
		if (!checkEntryConditions()) {
			return;
		}

		if (!isAsSevereAsThreshold(event.getLevel())){
			return;
		}

		int statusCode = sendRestRequest(event);
		if(statusCode != 200){
			errorHandler.error("Could not send message from RestfulAppender [" + name + "]. Status Code: " + statusCode);
		}
	}

	private boolean checkEntryConditions() {
		if (urlTemplate == null) {
			errorHandler.error("No 'urlTemplate' for [" + name + "]");
			return false;						
		}				
		return true;
	}
	
	
	private int sendRestRequest(LoggingEvent event) {
		PostMethod post = null;
		int statusCode = 0;
		String message = null;
		ObjectMapper mapper = new ObjectMapper();
		
        try {
        	Object object = event.getMessage();
        	if(object instanceof LogEventVO){
        		mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        		
        		//convert to JSON
        		message = mapper.writeValueAsString(object); 
        	}
        	else if(event.getMessage() instanceof String){
        		message = (String)object;
        		//validate json string
        		mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        		mapper.readValue(message, LogEventVO.class);        		
        	}
        	else{
        		errorHandler.error("Could not send message from RestfulAppender [" + name + "]", new Exception("Invalid json format or logEvent object"), ErrorCode.GENERIC_FAILURE);
        		return HttpStatus.SC_NOT_ACCEPTABLE;
        	}
        	
	        //create the client to call the logger REST api
	        HttpClient client = new HttpClient(connManager);
	        post = new PostMethod(urlTemplate);

	        RequestEntity entity = new StringRequestEntity(message, "application/json", "utf-8"); 
	        post.setRequestEntity(entity); 
        
        	statusCode = client.executeMethod(post);
        } catch(Exception e) {
        	statusCode = HttpStatus.SC_NOT_ACCEPTABLE;
	        errorHandler.error("Could not send message from RestfulAppender [" + name + "]", e, ErrorCode.GENERIC_FAILURE);
        } finally {
        	if(post != null){
        		post.releaseConnection();
        	}
        }
        return statusCode;
	}	

	public void close() {
		
	}

	public boolean requiresLayout() {
		return false;
	}
	
}