package com.irricurb.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RequestVO.java
 * <b>Project</b>: Sandbox
 * <b>Description: </b> Parses the request data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 11, 2018
 * @updates:
 ****************************************************************************/

public class RequestVO {
	
	// Member Variables
	private String requestUri;
	private String requestUrl;
	private String queryString = "";
	private String webServerAddress;
	private Map<String, List<String>> parameters = new HashMap<>(16);
	protected static final Logger log = Logger.getLogger(RequestVO.class);
	
	/**
	 * 
	 * @param requestUrl
	 */
	public RequestVO(Socket socket) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			webServerAddress = socket.getInetAddress().toString();
			requestUrl = in.readLine();
			
		} catch (IOException e) {
			log.error("Failed respond to client request: ", e);
		}
		
		if (! (requestUrl == null || requestUrl.isEmpty() || requestUrl.indexOf('/') == -1)) {
			requestUrl =  (requestUrl.substring(requestUrl.indexOf(' ') + 1, requestUrl.lastIndexOf(' ')));
			processRequest();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, "<br/>");
	}
	
	/**
	 * 
	 */
	protected void processRequest() {
		// Parse out the uri
		int index = requestUrl.indexOf('?') > -1 ? requestUrl.indexOf('?') : requestUrl.length();
		requestUri = requestUrl.substring(0, index);
		
		// Parse the querystring
		if (requestUrl.indexOf('?') > -1) {
			try {
				queryString = URLDecoder.decode(requestUrl.substring(requestUrl.indexOf('?') + 1), "UTF-8");
			} catch(Exception e) { /* Nothing to do */ }
		}
		
		// Create the parameter map
		parseParameters();
	}

	/**
	 * Converts the query string into a parameter map
	 */
	protected void parseParameters() {
		String[] pairs = queryString.split("&");
		
		for (String pair : pairs) {
			String[] kv = pair.split("=");
			
			if (! parameters.containsKey(kv[0])) {
				List<String> data = null;
				if (kv.length > 1) {
					data = new ArrayList<>();
					data.add(kv[1]);
				}
				
				parameters.put(kv[0], data);
			} else if (kv.length > 1) {
				parameters.get(kv[0]).add(kv[1]);
			}
		}
	}
	
	/**
	 * gets a parameter from the qs
	 * @param key
	 * @return
	 */
	public String getParameter(String key) {
		if (parameters.get(key) != null)
			return parameters.get(key).get(0);
		else return null;
	}

	/**
	 * @return the requestUri
	 */
	public String getRequestUri() {
		return requestUri;
	}

	/**
	 * @return the requestUrl
	 */
	public String getRequestUrl() {
		return requestUrl;
	}

	/**
	 * @return the queryString
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * @return the parameters
	 */
	public Map<String, List<String>> getParameterMap() {
		return parameters;
	}

	/**
	 * @return the webServerAddress
	 */
	public String getWebServerAddress() {
		return webServerAddress;
	}
}

