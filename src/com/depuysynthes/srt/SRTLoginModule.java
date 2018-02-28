package com.depuysynthes.srt;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBLoginModule;

/****************************************************************************
 * <b>Title:</b> SRTLoginModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Login Module Using Huddles SAML Login procedure
 * tailored for SRT.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * TODO - Update to Use Saml once details are finalized.
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 14, 2018
 ****************************************************************************/
public class SRTLoginModule extends DBLoginModule {

	public SRTLoginModule() {
		super();
	}

	public SRTLoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(user, pwd);

		log.debug("USER IS AUTHENTICATED: " + userData.isAuthenticated());

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
		return matchUser(wcUser);
	}

	/**
	 * Match WC Core User to an SRTRosterVO and return it.
	 * @param wcUser
	 * @param users
	 * @return
	 * @throws AuthenticationException - Throw Exception if no match found.
	 */
	private SRTRosterVO matchUser(UserDataVO wcUser) {

		SRTRosterVO roster = new SRTRosterVO();
		roster.setData(wcUser.getDataMap());
		roster.setOpCoId("US_SPINE");

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
		sql.append("SRT_ROSTER where profile_id = ? ");

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
}