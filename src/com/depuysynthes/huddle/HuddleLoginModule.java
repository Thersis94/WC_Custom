package com.depuysynthes.huddle;

import java.util.Map;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * <b>Title</b>: HuddleLoginModule.java<p/>
 * <b>Description: Extends SSO login with redirection to the user's desired homepage.
 * Also captures WWID to the database after first login; which gets uses for the 
 * Huddle Mobile App synchronization.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 29, 2015
 ****************************************************************************/
public class HuddleLoginModule extends SAMLLoginModule {

	/**
	 * 
	 */
	public HuddleLoginModule() {
	}

	/**
	 * @param config
	 */
	public HuddleLoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO retrieveUserData(String user, String pwd)
			throws AuthenticationException {
		UserDataVO userData = super.retrieveUserData(user, pwd);

		//test for the presents of a specific homepage.  If one exist, set the redirect to go there.
		//if not, send them to the initial welcome screen, which will make them set one.
		
		
		return userData;
	}
	
	
	/**
	 * called via 'remember me' cookie logins:
	 */
	@Override
	public UserDataVO retrieveUserData(String encProfileId) throws AuthenticationException {
		UserDataVO userData = super.retrieveUserData(encProfileId);
		
		//redirect to the user's personal homepage.
		
		return userData;
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#initiateLogin()
	 */
	@Override
	public boolean canInitiateLogin(SMTServletRequest req) throws AuthenticationException {
		//set a parameter to invoke SSO and leverage the superclass implementation
		req.setParameter("initiateSSO", "true");
		return super.canInitiateLogin(req);
	}
	
}