package com.depuysynthes.srt;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.depuysynthes.huddle.HuddleUtils;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.LoginAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * <b>Title:</b> SRTLoginModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Login Module Using Huddles SAML Login procedure
 * tailored for SRT.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 14, 2018
 ****************************************************************************/
public class SRTLoginModule extends SAMLLoginModule {

	public SRTLoginModule() {
		super();
	}

	public SRTLoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(user, pwd);

		//redirect to the user's personal homepage.
		applyRedirectLogic(userData);

		return loadSRTUser(userData);
	}

	/**
	 * Attempt to match the given wcUser to an SRT Roster Record verifying
	 * their permission to access the system.
	 * @param wcUser
	 * @return
	 * @throws AuthenticationException
	 */
	public SRTRosterVO loadSRTUser(UserDataVO wcUser) throws AuthenticationException {
		Connection dbConn = (Connection) getAttribute(GlobalConfig.KEY_DB_CONN);

		//Attempt to retrieve SRT Roster record from Database.
		List<SRTRosterVO> users = new DBProcessor(dbConn).executeSelect(buildRosterSql(), Arrays.asList(wcUser.getProfileId()), new SRTRosterVO());

		for(SRTRosterVO user : users) {
			//If this user matches the data coming back from SSO.
			if(user.isActive() && user.getWwid().equals(wcUser.getAttribute("wwid"))) {

				//Set Extra ProfileData from original incoming user record
				user.setData(wcUser.getDataMap());

				//Return.
				return user;
			}
		}

		//If no user is matched, throw Exception.
		throw new AuthenticationException("User Not Authorized");
	}

	/**
	 * Build the SRT Roster Lookup Query.
	 * @return
	 */
	private String buildRosterSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("SRT_ROSTER where wwid = ?");

		return sql.toString();
	}

	/**
	 * called via 'remember me' cookie logins:
	 * Note: This is not a real use-case for Huddle b/c there is no login form.
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(encProfileId);

		//redirect to the user's personal homepage.
		applyRedirectLogic(userData);

		return loadSRTUser(userData);
	}


	/**
	 * test for the presents of a specific homepage.  If one exist, set the redirect to go there. 
	 * if not, send them to the initial welcome screen, which will make them set one.
	 * @param userData
	 */
	private void applyRedirectLogic(UserDataVO userData) {
		String homepage = StringUtil.checkVal(userData.getAttribute(SRTUtil.HOMEPAGE_REGISTER_FIELD_ID), null);
		ActionRequest req = (ActionRequest) getAttribute(GlobalConfig.ACTION_REQUEST);
		SMTSession ses = req.getSession();
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
	public boolean canInitiateLogin(ActionRequest req) throws AuthenticationException {
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
	 * path.  If so, the destination page is left untouched.
	 * @param destPg
	 * @return
	 */
	private boolean isAdminToolPath(String destPg) {
		String adminToolPath = StringUtil.checkVal(getAttribute(AdminConstants.ADMIN_TOOL_PATH),null);
		return adminToolPath != null && destPg.contains(adminToolPath);
	}
}