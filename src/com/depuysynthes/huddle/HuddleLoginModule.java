package com.depuysynthes.huddle;

import java.util.Map;

import javax.servlet.http.HttpSession;

import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.LoginAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
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
		super();
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

		//redirect to the user's personal homepage.
		applyRedirectLogic(userData);

		return userData;
	}


	/**
	 * called via 'remember me' cookie logins:
	 * Note: This is not a real use-case for Huddle b/c there is no login form.
	 */
	@Override
	public UserDataVO retrieveUserData(String encProfileId) throws AuthenticationException {
		UserDataVO userData = super.retrieveUserData(encProfileId);

		//redirect to the user's personal homepage.
		applyRedirectLogic(userData);

		return userData;
	}


	/**
	 * test for the presents of a specific homepage.  If one exist, set the redirect to go there. 
	 * if not, send them to the initial welcome screen, which will make them set one.
	 * @param userData
	 */
	private void applyRedirectLogic(UserDataVO userData) {
		String homepage = StringUtil.checkVal(userData.getAttribute(HuddleUtils.HOMEPAGE_REGISTER_FIELD_ID), null);
		SMTServletRequest req = (SMTServletRequest) initVals.get(GlobalConfig.HTTP_REQUEST);
		HttpSession ses = req.getSession();
		String destPg = StringUtil.checkVal(ses.getAttribute(LoginAction.DESTN_URL));

		// if this is an admintool login, preserver the destination page.
		if (isAdminToolPath(destPg)) return;
		
		if (homepage == null) {
			//no homepage, this is a first-time login.
			//send them to our registration page to setup their account
			ses.setAttribute(LoginAction.DESTN_URL, "/?firstVisit=true");
			homepage = "/";
		} else if (destPg.endsWith("/") || destPg.length() == 0) { //homepage or /context/
			//send the user to their homepage if they're not deep linking somewhere specific.
			ses.setAttribute(LoginAction.DESTN_URL, homepage);
		}
		ses.setAttribute(HuddleUtils.MY_HOMEPAGE, homepage);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#initiateLogin()
	 */
	@Override
	public boolean canInitiateLogin(SMTServletRequest req) throws AuthenticationException {
		//only initiate logic if the session is new.
		//This traps an infinite redirect loop where something goes wrong on WC 
		//but the user successfully authenticates to SSO. (go there, come back, fail, redir to homepage, go there, come back, fail, ...con't.)
		if (!req.getSession().isNew() && !req.hasParameter(Constants.SSO_INITIATE))
			return false;

		//set a parameter to invoke SSO and leverage the superclass implementation
		req.setParameter(Constants.SSO_INITIATE, "true");
		return super.canInitiateLogin(req);
	}
	
	/**
	 * Helper method used to determine if the destination page is an admin tool 
	 * path.  If so, the destintation page is left untouched.
	 * @param destPg
	 * @return
	 */
	private boolean isAdminToolPath(String destPg) {
		String adminToolPath = StringUtil.checkVal(initVals.get(AdminConstants.ADMIN_TOOL_PATH),null);
		if (adminToolPath != null && destPg.contains(adminToolPath)) return true;
		return false;
	}

}