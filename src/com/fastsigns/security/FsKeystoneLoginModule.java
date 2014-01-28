package com.fastsigns.security;

import java.net.URLDecoder;
import java.sql.Connection;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.UserLogin;

/*****************************************************************************
 <p><b>Title</b>: FsKeystoneLoginModule.java</p>
 <p><b>Description: </b>
 	Logs the user into WC using Keystone for authentication and data retrieval.
 	
 <p>Copyright: Copyright (c) 2000 - 2012 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Dec 14, 2012
 ***************************************************************************/

public class FsKeystoneLoginModule extends AbstractLoginModule {

    public FsKeystoneLoginModule() {
	    super();
        super.setUserProfile(Boolean.TRUE);
    }

    /**
     * @param arg0
     */
    public FsKeystoneLoginModule(Map<String, Object> arg0) {
        super(arg0);
        super.setUserProfile(Boolean.TRUE);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     * "Login by cookie" method
     */
    @Override
    public UserDataVO retrieveUserData(String encProfileId) 
    throws AuthenticationException {
		String profileId = this.retrieveAuthIdByCookie(encProfileId);
		log.debug("Starting user login: " + profileId);
		if (profileId == null)
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);

		return loginUserToKeystone(null, null, profileId);
    }
    
    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     * "Login by form (username & password)" method.
     */
    @Override
    public UserDataVO retrieveUserData(String email, String pwd) 
    throws AuthenticationException {
		log.debug("Starting user login: " + email + "/" + pwd);
		if (email == null || pwd == null)
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		
		UserDataVO user = this.loginUserToKeystone(email, pwd, null);
		if (user == null) {
			user = new UserDataVO();
			user.setAuthenticationId(email);
		}
		return user;
    }
    

    /**
     * A single (combined) method that does the legwork of logging the user into Keystone.
     * @param username
     * @param pswd
     * @param profileId
     * @return
     * @throws AuthenticationException
     */
    public UserDataVO loginUserToKeystone(String username, String pswd, String profileId) 
    throws AuthenticationException {
        	
		FastsignsSessVO sessVo = new FastsignsSessVO();
		try {
			// log the user into Keystone
			SMTServletRequest req = (SMTServletRequest) this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
			KeystoneProxy proxy = new KeystoneProxy(initVals);
			proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
			proxy.setModule("user");
			proxy.setAction("eCommLogin");
			proxy.setParserType(KeystoneDataParser.DataParserType.DoNothing);

			// set whether we're logging in by ID, or by user/pswd
			if (profileId == null) {
				proxy.addPostData("username", username);
				proxy.addPostData("password", pswd);
			} else {
				proxy.addPostData("usersId", profileId);
			}

			byte[] byteData = (byte[]) proxy.getData().getActionData();
			JSONObject jsonObject = JSONObject.fromObject(new String(byteData));

			if (jsonObject != null && jsonObject.optBoolean("success")) {
				this.parseKeystoneFranchises(jsonObject.getJSONObject("data"), sessVo);
				this.parseKeystoneProfiles(jsonObject.getJSONObject("data"), sessVo);
			} else {
				throw new AuthenticationException(
						ErrorCodes.ERR_INVALID_LOGIN);
			}

			log.debug("FSSessVO=" + sessVo.toString());
			req.getSession().setAttribute(KeystoneProxy.FRAN_SESS_VO, sessVo);
			// end Keystone integration code

		} catch (Exception e) {
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		}

		// Return the first UserDataVO on the Map.
		// We have no idea which should be the favored 'default'. (nor should
		// it matter)
		for (UserDataVO vo : sessVo.getProfiles().values()) {
			// set certain fields so logging can take place
			vo.setAuthenticationId(username == null ? profileId : username);
			vo.setAuthenticated(true);
			return vo;
		}
		return null;
    }
    
    /**
     * Manages the user information for the authentication system
     */
    public String manageUser(String authId, String userName, String password, Integer resetFlag)  
    throws InvalidDataException {
        if (userName == null || userName.length() == 0 || password == null || password.length() == 0)
        	throw new InvalidDataException();
        
        //TODO interface to Keystone
        
        return null;
    }
    
    /**
     * Retrieves a password based upon the user name.
     */
    public String retrievePassword(String emailAddress)
    throws EmailAddressNotFoundException {
        return "";
    }
    
    public String retrieveAuthIdByCookie(String encProfileId) {
		log.debug("encProfileId=" + encProfileId);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        try {
			encProfileId = URLDecoder.decode(encProfileId, "UTF-8");
			StringEncrypter se = new StringEncrypter(encKey);
			return se.decrypt(encProfileId);
		} catch (Exception e) {
			return null;
		}
    }
    
    /* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java.lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        return ul.getPasswordAge(authenticationId);      
    }

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#deleteUser(java.lang.String)
	 */
	@Override
	public boolean deleteUser(String authId) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveAuthenticationId(java.lang.String)
	 */
	@Override
	public String retrieveAuthenticationId(String userName) {
		return null;
	}

	
	/**
	 * turns the Keystone JSON response into relevant Franchise data for WC to use.
	 * @param data
	 * @return
	 */
	private void parseKeystoneFranchises(JSONObject data, FastsignsSessVO sessVo)
	throws InvalidDataException {
		KeystoneProfileManager pm = new KeystoneProfileManager();
		try {
			JSONArray franchises = data.getJSONArray("franchiseList");
			
			for (int x=0; x < franchises.size(); x++) {
				JSONObject franObj = franchises.getJSONObject(x);
				sessVo.addFranchise(pm.loadFranchiseFromJSON(franObj));
			}
			
		} catch (Exception e) {
			log.error("could not parse Keystone JSON response upon user login", e);
			throw new InvalidDataException(e);
		} finally {
			pm = null;
		}
		
		return;
	}
	
	/**
	 * turns the Keystone JSON response into a valid UserDataVO for WC to use
	 * @param data
	 * @return
	 */
	private void parseKeystoneProfiles(JSONObject data, FastsignsSessVO sessVo) 
	throws InvalidDataException {
		
		KeystoneProfileManager pm = new KeystoneProfileManager();
		try {
			JSONArray accounts = data.getJSONArray("entityList");
		
			for (int cnt=0; cnt < accounts.size(); cnt++) {
				JSONObject entity = accounts.getJSONObject(cnt);
		
				KeystoneUserDataVO user = pm.loadProfileFromEntityJSON(entity);
				user.setWebId(sessVo.lookupWebId(entity.optString("franchise_id")));

				user.setAllowPurchaseOrders(entity.optBoolean("po_approved"));
				sessVo.addProfile(user);
			}
			
		} catch (Exception e) {
			log.error("could not parse Keystone JSON response upon user login", e);
			throw new InvalidDataException(e);
		}
		
		return;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
	 */
	@Override
	public String authenticate(String loginName, String password)
	throws AuthenticationException {
		//this will throw Exceptions long before we risk a null pointer here...
		return this.retrieveUserData(loginName, password).getProfileId();
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String, java.lang.String)
     */
	@Override
	public UserDataVO authenticateUser(String userName, String password)
			throws AuthenticationException {
		return this.retrieveUserData(userName, password);
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		KeystoneProxy proxy = new KeystoneProxy(initVals);
		SMTServletRequest req = (SMTServletRequest) this.initVals.get(GlobalConfig.HTTP_REQUEST);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("userContact");
		proxy.setAction("eCommSetPassword");
		proxy.addPostData("username", user.getEmailAddress());
		proxy.addPostData("password", pwd);
		proxy.setParserType(KeystoneDataParser.DataParserType.DoNothing);
		
		try {
			byte[] byteData = (byte[]) proxy.getData().getActionData();
			JSONObject jsonObject = JSONObject.fromObject(new String(byteData));
			
			if (jsonObject.optBoolean("success")) {
				//reload the user's session data from when they logged in, with what was just acknowledged & saved by Keystone
				//JSONObject entity = jsonObject.getJSONObject("data");
				//user = this.loadProfileFromEntityJSON(entity);
				return true;
				
			} else {
				//msg = jsonObject.getString("responseText");
				//throw new InvalidDataException(msg);
				return false;
			}
		} catch (Exception e) {
			log.error("could not process password reset", e);
		}
		
		return false;
	}

}
