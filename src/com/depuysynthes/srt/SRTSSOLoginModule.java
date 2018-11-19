package com.depuysynthes.srt;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.saml.SAMLV2Constants;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * <b>Title:</b> SRTSSOLoginModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> SSO Implementation of the SRT Login Module.
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

		//Convert UserDataVO to SRTRosterVO
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
		Connection conn = (Connection) getAttribute(GlobalConfig.KEY_DB_CONN);
		List<Object> vals = Arrays.asList(wcUser.getAttributes().get(SAMLV2Constants.NAME_ID_ELE));
		List<SRTRosterVO> users = new DBProcessor(conn).executeSelect(buildRosterSql(), vals, new SRTRosterVO());
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
		if(!StringUtil.isEmpty(wcUser.getEmailAddress())) {
			roster.setAuthenticated(wcUser.isAuthenticated());
		}
		return roster;
	}

	/**
	 * Build the SRT Roster Lookup Query.
	 * Ensure we only get matches on active roster records.
	 * @return
	 */
	private String buildRosterSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_SRT_ROSTER where wwid = ? and is_active = 1");

		return sql.toString();
	}

	/**
	 * called via 'remember me' cookie logins:
	 * Note: This is not a real use-case for Huddle b/c there is no login form.
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(encProfileId);

		//Convert UserDataVO to SRTRosterVO
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

	/**
	 * Manages the retrieval or creation of a user's authentication record
	 * @param conn
	 * @param user
	 * @param site
	 * @param pwd
	 * @throws AuthenticationException
	 */
	@Override
	public void manageAuthenticationRecord(Connection conn, UserDataVO user, SiteVO site, String pwd) throws AuthenticationException {

		//If user doesn't have an email address, attempt to retrieve it.
		if(StringUtil.isEmpty(user.getEmailAddress())) {
			//Load the Roster User using WWID on user Attributes Map.
			SRTRosterVO r = loadSRTUser(user);

			//Copy data back to original User.  This primarily gets us ProfileId.
			user.setData(r.getDataMap());

			//Attempt to populate User with Profile Data so we can Authenticate.
			matchEmailAddress(conn, user);
		}

		//Proceed with normal Authentication.
		super.manageAuthenticationRecord(conn, user, site, pwd);
	}


	/**
	 * Attempt to load ProfileData for the given user.  We are only passed
	 * WWID from SSO so we need to get the EmailAddress for the user in
	 * order to proceed with login.
	 * @param user
	 */
	private void matchEmailAddress(Connection conn, UserDataVO user) {
		try {
			ProfileManagerFactory.getInstance(getAttributes()).populateRecords(conn, Arrays.asList(user));
		} catch (DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}
}