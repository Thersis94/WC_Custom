package com.fastsigns.product.keystone.checkout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import org.apache.log4j.Logger;


import com.fastsigns.product.keystone.KeystoneProxy;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: KeystoneProxy.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2012
 ****************************************************************************/
public class CheckoutProxy {
	public static final String FRAN_SESS_VO = KeystoneProxy.FRAN_SESS_VO;
	
	//interaction constants
	private String smtProxyUrl = null;
	private SMTHttpConnectionManager conn = null;
	private int httpConnectionTimeout = 30000; //30 seconds
	protected static Logger log = null;
	
	//customizable variables
	//private String servletPath = "/";
	private boolean doJson = true;
	private Map<String, String> postData = null;
	
	public CheckoutProxy(String servletPath) {
		log = Logger.getLogger(this.getClass());
		smtProxyUrl = servletPath;
		postData = new HashMap<String, String>();
		//this.servletPath = servletPath;
		log.info(this.getClass() + " created with proxyUrl=" + smtProxyUrl);
	}
	
	
	/**
	 * calling this method actually fires the http call to Keystone.
	 * Be sure all setters have been done prior to this.
	 * @return
	 */
	public byte[] getData() {
		byte[] data = null;
		try {
			conn = new SMTHttpConnectionManager();
			conn.setFollowRedirects(false);
			conn.setConnectionTimeout(httpConnectionTimeout);
						
			data = conn.retrieveDataViaPost(buildUrl(), buildParams());
		} catch (IOException ioe) {
			log.error("IOException " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			log.error("unexpected exception: " + e.getMessage(), e);
		}
		
		log.debug("retrieved data=" + new String(data));
		return data;
	}
	
	
	private String buildUrl() {
		StringBuilder url = new StringBuilder(smtProxyUrl);
		//url.append(servletPath);
		log.debug("url=" + url);
		return url.toString();
	}
	
	
	private String buildParams() {
		StringBuilder params = new StringBuilder("doJson=").append(doJson);
		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		for (String p : postData.keySet()) {
			params.append("&").append(p).append("=").append(postData.get(p));
		}
		
		log.debug("post data=" + params);
		return params.toString();
	}
	
	public boolean getDoJson() {
		return doJson;
	}
	public void setDoJson(boolean doJson) {
		this.doJson = doJson;
	}
	public Map<String, String> getPostData() {
		return postData;
	}

	public void setPostData(Map<String, String> postData) {
		this.postData = postData;
	}
	public void addPostData(String k, String v) {
		postData.put(k, v);
	}

	/** 
	 * sets both of the SMTHTTPConnection's Connect and Read timeouts, in milliseconds
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.httpConnectionTimeout = timeout;
	}
	public int getTimeout() { return httpConnectionTimeout; }
	
}
