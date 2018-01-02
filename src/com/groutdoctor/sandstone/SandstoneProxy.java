package com.groutdoctor.sandstone;

//Java 7
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

//SMT app libs
import javax.servlet.http.Cookie;
import org.apache.log4j.Logger;

//SMTBase libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;

//WebCrescendo
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>: SandstoneProxy.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author James McKain
* @version 1.0
* @since Oct 1, 2012
* 
* September 27, 2017: tjohnson: Restored from deleted Fastsigns/Keystone code
* in the github repository, and updated for Grout Doctor/Sandstone needs.
****************************************************************************/
public class SandstoneProxy {
	
	public static final String CFG_SANDSTONE_API_URL = "groutDoctorSandstoneApiUrl";

	// Interaction constants
	private String sandstoneApiUrl = null;
	private String encryptKey = null;
	private int httpConnectionTimeout = 30000; //30 seconds
	protected static Logger log = null;
	
	// Customizable variables
	private Cookie sessionCookie = null;
	private String module = null;
	private String action = null;
	private String accountId = null;
	private String franchiseId = null;
	private String userId = null;
	private String sessionId = null;
	private boolean doJson = true;
	private Map<String, String> postData = null;
	
	/**
	 * @param attribs
	 */
	public SandstoneProxy(Map<String, Object> attribs) {
		log = Logger.getLogger(this.getClass());
		sandstoneApiUrl = StringUtil.checkVal(attribs.get(CFG_SANDSTONE_API_URL));
		encryptKey = StringUtil.checkVal(attribs.get(Constants.ENCRYPT_KEY));
		postData = new HashMap<>();
		
		log.debug(this.getClass() + " created with apiUrl=" + sandstoneApiUrl);
	}
	
	/**
	 * Calling this method actually fires the http call to Sandstone.
	 * Be sure all setters have been done prior to this.
	 * 
	 * @return
	 */
	public void callSandstone() throws InvalidDataException {
		byte[] data = null;
		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			conn.setFollowRedirects(false);
			conn.setConnectionTimeout(httpConnectionTimeout);
			
			if (sessionCookie != null) 
				conn.addCookie(sessionCookie.getName(), sessionCookie.getValue());
			
			data = conn.retrieveDataViaPost(buildUrl(), buildParams());
			
			// Trap all errors generated by Sandstone
			if (200 != conn.getResponseCode())
				throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());
			
		} catch (IOException ioe) {
			throw new InvalidDataException(ioe);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new InvalidDataException(e);
		}
		
		log.debug("retrieved data=" + new String(data));

		// The original code from Fastsigns/Keystone returned parsed data here using the KeystoneDataParser factory.
		// This is not needed for Grout Doctor purposes, so all the related code is not getting restored from the repo at this time.
	}
	
	/**
	 * Builds the url required to contact Sandstone.
	 */
	private String buildUrl() {
		StringBuilder url = new StringBuilder(sandstoneApiUrl);
		url.append("?module=").append(getModule());
		url.append("&action=").append(getAction());
		url.append("&APIKEY=").append(buildApiKey());
		log.debug("url=" + url);
		
		return url.toString();
	}
	
	/**
	 * Encodes all parameters and passes in format expected by the connection manager.
	 * 
	 * @return
	 */
	private Map<String, Object> buildParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("doJson", doJson);
		
		if (accountId != null)
			params.put(getAccountKey(), accountId);
		
		if (franchiseId != null)
			params.put("franchiseId", franchiseId);
		
		if (userId != null)
			params.put("usersId", userId);
		
		// Append any runtime requests of the calling class.  (login would pass username & password here)
		for (Entry<String, String> entry : postData.entrySet()) {
			params.put(entry.getKey(), StringEncoder.urlEncode(entry.getValue()));
		}
		
		log.debug("post data=" + params);
		return params;
	}
	
	/**
	 * Builds the API key required to authenticate against Sandstone.
	 * 'protected' and not 'private' so that actions can access it to pass-along to JSPs  (See MyAssetsAction)
	 * 
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
	 * This method is a work-around for inconsistencies in Sandstone.
	 * The "accountId" request parameter could be "accountId", "accountsId", "entity_id", etc.
	 * depending on which module is being called.
	 * 
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
	 * Sets both of the SMTHTTPConnection's Connect and Read timeouts, in milliseconds
	 * 
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.httpConnectionTimeout = timeout;
	}
	public int getTimeout() { return httpConnectionTimeout; }
	
}