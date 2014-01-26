package com.fastsigns.product.keystone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;

import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.product.keystone.parser.KeystoneDataParser.DataParserType;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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
public class KeystoneProxy {
	public static final String FRAN_SESS_VO = "franchiseSessVo";
	
	//interaction constants
	private String keystoneApiUrl = null;
	private String encryptKey = null;
	private SMTHttpConnectionManager conn = null;
	private int httpConnectionTimeout = 30000; //30 seconds
	protected static Logger log = null;
	
	//customizable variables
	private Cookie sessionCookie = null;
	private String module = null;
	private String action = null;
	private String accountId = null;
	private String franchiseId = null; //Fastsigns' WebId
	private String userId = null;
	private String sessionId = null;
	private boolean doJson = true;
	private Map<String, String> postData = null;
	
	
	/**
	 * DataParserType defines which class is responsible for parsing the response data for the calling action.
	 * the action will set this variable, so tell us how to parse the data, so we can return it cleanly as a ModuleVO
	 * ModuleVO is then able to be cached by WC, without WC having to re-process the entire byte[] from it's raw form. 
	 */
	DataParserType parserType = null;
	
	
	public KeystoneProxy(Map<String, Object> attribs) {
		log = Logger.getLogger(this.getClass());
		keystoneApiUrl = (String) attribs.get("keystoneApiUrl");
		encryptKey = (String) attribs.get(Constants.ENCRYPT_KEY);
		postData = new HashMap<String, String>();
		
		log.info(this.getClass() + " created with apiUrl=" + keystoneApiUrl);
	}
	
	/**
	 * static class loader for KeystoneProxy; facades cacheable config.
	 * This method will favor a caching proxy based on appConfig, so don't
	 * call it if you specifically need a non-caching version.
	 * @param attribs
	 * @return
	 */
	public static KeystoneProxy newInstance(Map<String, Object> attribs) {
		Integer cacheTimeout = Convert.formatInteger((String)attribs.get("keystoneApiCacheTimeout"), 0);
		if (cacheTimeout > 0) {
			return new CachingKeystoneProxy(attribs, cacheTimeout);
		} else {
			return new KeystoneProxy(attribs);
		}
	}
	
	/**
	 * assuming cache is used (defined in WC config as >0), this method allows the invoking class to alter the cache timeout.
	 * This is practice for things like MyAssets, which we want to keep around for ~10mins while the user is active.
	 * @param attribs
	 * @param timeoutOverride
	 * @return
	 */
	public static KeystoneProxy newInstance(Map<String, Object> attribs, int timeoutOverrideMins) {
		KeystoneProxy proxy = newInstance(attribs);
		if (proxy instanceof CachingKeystoneProxy)
			((CachingKeystoneProxy)proxy).setOsCacheTimeout(timeoutOverrideMins);
		
		return proxy;
	}
	
	
	/**
	 * calling this method actually fires the http call to Keystone.
	 * Be sure all setters have been done prior to this.
	 * @return
	 */
	public ModuleVO getData() throws InvalidDataException {
		byte[] data = null;
		try {
			conn = new SMTHttpConnectionManager();
			conn.setFollowRedirects(false);
			conn.setConnectionTimeout(httpConnectionTimeout);
			
			if (sessionCookie != null) 
				conn.addCookie(sessionCookie.getName(), sessionCookie.getValue());
			
			
			data = conn.retrieveDataViaPost(buildUrl(), buildParams());
			
			//trap all errors generated by Keystone
			if (200 != conn.getResponseCode())
				throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());
			
		} catch (IOException ioe) {
			log.error(ioe.getMessage(), ioe);
			throw new InvalidDataException(ioe);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new InvalidDataException(e);
		}
		
		log.debug("retrieved data=" + new String(data));
		return KeystoneDataParser.newInstance(parserType).formatData(data);
	}
	
	
	private String buildUrl() {
		StringBuilder url = new StringBuilder(keystoneApiUrl);
		url.append("?module=").append(getModule());
		url.append("&action=").append(getAction());
		log.debug("url=" + url);
		return url.toString();
	}
	
	
	private String buildParams() {
		StringBuilder params = new StringBuilder("doJson=").append(doJson);
		
		if (accountId != null)
			params.append("&").append(getAccountKey()).append("=").append(accountId);
		
		if (franchiseId != null)
			params.append("&franchiseId").append("=").append(franchiseId);
		
		if (userId != null)
			params.append("&usersId").append("=").append(userId);
		
		//add the apiKey
		params.append("&APIKEY=").append(buildApiKey());
		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		for (String p : postData.keySet()) {
			params.append("&").append(p).append("=").append(postData.get(p));
		}
		
		log.debug("post data=" + params);
		return params.toString();
	}
	
	/**
	 * builds the API key required to authenticate against Keystone.
	 * 'protected' and not 'private' so that actions can access it to pass-along to JSPs  (See MyAssetsAction)
	 * @return
	 */
	protected String buildApiKey() {
		String apiKey = null;
		try {
			StringEncrypter se = new StringEncrypter(encryptKey);
			apiKey = getModule() + ":" + getAction() + ":" + System.currentTimeMillis();
			apiKey = se.encrypt(apiKey);
			apiKey = URLEncoder.encode(apiKey, "UTF-8");
			
		} catch (EncryptionException ee) {
			log.error("could not encrypt payload: ", ee);
		} catch (UnsupportedEncodingException use) {
			log.error("could not encode payload: ", use);
		} catch (Exception e) {
			log.error("unexpected exception " + e.getMessage(), e);
		}
		return apiKey;
	}
	
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public boolean getDoJson() {
		return doJson;
	}
	public void setDoJson(boolean doJson) {
		this.doJson = doJson;
	}

	public String getFranchiseId() {
		return franchiseId;
	}

	public void setFranchiseId(String franchiseId) {
		this.franchiseId = franchiseId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Cookie getSessionCookie() {
		return sessionCookie;
	}

	public void setSessionCookie(Cookie sessionCookie) {
		this.sessionCookie = sessionCookie;
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

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	/**
	 * this method is a work-around for inconsistencies in Keystone.
	 * the "accountId" request parameter could be "accountId", "accountsId", "entity_id", etc.
	 * depending on which module is being called.
	 * @return
	 */
	private final String getAccountKey() {
		if ("invoicesAccounts".equals(module)) {
			return "entity_id";
			
		} else if ("accounts".equals(module) || "products".equals(module) 
				|| "pricingManager".equals(module) || "jobs".equals(module)) {
			return "accountsId";
			
		} else if ("contact".equals(module)) {
			return "contact_data_id";
			
		} else {
			return "account_id";
		}
	}


	/** 
	 * sets both of the SMTHTTPConnection's Connect and Read timeouts, in milliseconds
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.httpConnectionTimeout = timeout;
	}
	public int getTimeout() { return httpConnectionTimeout; }

	
	public DataParserType getParserType() {
		return parserType;
	}

	public void setParserType(DataParserType parserType) {
		this.parserType = parserType;
	}
	
}
