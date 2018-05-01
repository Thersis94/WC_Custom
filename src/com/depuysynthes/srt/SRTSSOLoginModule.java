package com.depuysynthes.srt;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import com.depuysynthes.huddle.HuddleUtils;
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
 * <b>Title:</b> SRTSSOLoginModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 27, 2018
 ****************************************************************************/
public class SRTSSOLoginModule extends SAMLLoginModule {

	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(user, pwd);

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
		//Attempt to retrieve SRT Roster record from Database.
		List<SRTRosterVO> users = new DBProcessor((Connection) getAttribute(GlobalConfig.KEY_DB_CONN)).executeSelect(buildRosterSql(), Arrays.asList(wcUser.getProfileId()), new SRTRosterVO());
		log.info(users);
		if(!users.isEmpty()) {
			return matchUser(wcUser, users);
		} else {
			throw new AuthenticationException("User not in Roster Table.");
		}
	}

	/**
	 * Match WC Core User to an SRTRosterVO and return it.
	 * @param wcUser 
	 * @param users
	 * @param users
	 * @return
	 * @throws AuthenticationException - Throw Exception if no match found.
	 */
	private SRTRosterVO matchUser(UserDataVO wcUser, List<SRTRosterVO> users) {
		SRTRosterVO roster = users.get(0);
		roster.setData(wcUser.getDataMap());
		//THIS NEEDS TO BE SET OFF ORIGINAL RECORD!
		roster.setAuthenticated(wcUser.isAuthenticated());
		return roster;
	}

	/**
	 * Build the SRT Roster Lookup Query.
	 * TODO - Update to use WWID if necessary Later On.
	 * @return
	 */
	private String buildRosterSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_SRT_ROSTER where profile_id = ? ");

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

		return loadSRTUser(userData);
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
}