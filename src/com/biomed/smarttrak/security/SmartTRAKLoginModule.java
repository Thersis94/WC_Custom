package com.biomed.smarttrak.security;

// Java 7
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

//WC_Custom libs
import com.biomed.smarttrak.admin.user.TeamManager;
import com.biomed.smarttrak.admin.user.UserManager;
import com.biomed.smarttrak.vo.UserVO;

//SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

//WebCrescendo libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.DBLoginLockoutModule;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKLoginModule</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Jan 03, 2017
 <b>Changes:</b>
 ***************************************************************************/
public class SmartTRAKLoginModule extends DBLoginLockoutModule {

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO retrieveUserData(String userNm, String pwd) 
			throws AuthenticationException {
		// 1. retrieve SmartTRAK-specific acct info
		log.debug("Starting authenticateUser: " + userNm + "/" + pwd);

		if (StringUtil.checkVal(userNm,null) == null || 
				StringUtil.checkVal(pwd,null) == null)
			throw new AuthenticationException("No login information was supplied for authentication.");

		// get a db connection
		Connection conn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);

		// 1. Authenticate against WC core and load WC profile
		UserDataVO wcUser = doAuthentication(conn, userNm, pwd);

		// 2. create/populate SmarttrakUserVO
		UserVO tkUser = populateUser(wcUser, new UserVO());

		// 3. Retrieve SmartTRAK-specific user data using the WC profileId.
		retrieveBaseUser(conn, tkUser);
		retrieveBaseUserTeams(conn, tkUser);
		log.debug("Retrieved SmartTRAK user and user-teams data...");

		return tkUser;
	}

	/**
	 * Populates the SmartTRAK user object with WebCrescendo user data.
	 * @param wcUser
	 * @param tkUser
	 * @return
	 */
	private UserVO populateUser(UserDataVO wcUser, UserVO tkUser) {
		// set fields from the WC UserDataVO data map
		tkUser.setData(wcUser.getDataMap());
		// set attributes
		tkUser.setAttributes(wcUser.getAttributes());
		// set authentication values.
		tkUser.setAuthenticationId(wcUser.getAuthenticationId());
		tkUser.setAuthenticated(wcUser.isAuthenticated());
		tkUser.setLockedOut(wcUser.isLockedOut());
		tkUser.setAuthenticationLogId(wcUser.getAuthenticationLogId());
		tkUser.setPasswordChangeDate(wcUser.getPasswordChangeDate());
		tkUser.setPasswordResetFlag(wcUser.getPasswordResetFlag());
		tkUser.setPasswordHistory(wcUser.getPasswordHistory());
		return tkUser;
	}
	
	/**
	 * Retrieves base SmartTRAK-specific user data for a user based on 
	 * the user ID field or the profile ID field poplated
	 * WC profile ID and populates the ST user bean with that data.
	 * @param conn
	 * @param tkUser
	 * @return
	 * @throws AuthenticationException
	 */
	private void retrieveBaseUser(Connection conn, 
			UserVO tkUser) throws AuthenticationException {
		UserManager um = new UserManager(conn,getInitVals());
		// use profile ID as that is all we have at the moment.
		um.setProfileId(tkUser.getProfileId());
		log.debug("searching for SmartTRAK user using profileId: " + tkUser.getProfileId());

		List<UserVO> users;
		try {
			users = um.retrieveBaseUser();
		} catch(SQLException ae) {
			throw new AuthenticationException(ae.getMessage());
		}

		if (users.isEmpty() || 
				users.size() > 1) {
			log.error("Error retrieving SmartTRAK user; records found: " + users.size());
			throw new AuthenticationException();
		}

		UserVO resultUser = users.get(0);
		tkUser.setUserId(resultUser.getUserId());
		tkUser.setAccountId(resultUser.getAccountId());
		tkUser.setRegisterSubmittalId(resultUser.getRegisterSubmittalId());
		tkUser.setCreateDate(resultUser.getCreateDate());
		tkUser.setUpdateDate(resultUser.getUpdateDate());
	}
	
	/**
	 * Retrieves list of teams to which a user belongs and populates
	 * the user VO with the list of teams.
	 * @param conn
	 * @param tkUser
	 * @throws AuthenticationException
	 */
	private void retrieveBaseUserTeams(Connection conn, 
			UserVO tkUser) throws AuthenticationException {
		TeamManager tm = new TeamManager(conn,getInitVals());
		// retrieve teams based on user ID.
		tm.setUserId(tkUser.getUserId());
		
		try {
			tkUser.setTeams(tm.retrieveTeams());
		} catch (SQLException ae) {
			throw new AuthenticationException(ae.getMessage());
		}

	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginLockoutModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String userNm, String pwd) throws AuthenticationException {
		log.debug("starting SmartTRAK login module's authenticate user, about to call my own retrieveUserData");
		return this.retrieveUserData(userNm, pwd);
	}
	
	/**
	 * Calls base class' authentication method, checks for valid authentication and lockout.  If 
	 * user authenticated and is not locked out, attempt to load profile.
	 * @param conn
	 * @param userNm
	 * @param pwd
	 * @return
	 * @throws AuthenticationException
	 */
	private UserDataVO doAuthentication(Connection conn, String userNm, String pwd) 
			throws AuthenticationException {
		/* Get the user's auth record, check for isAuthenticated and isLockedOut.
		 * Then and only then do we try to retrieve a profile. */
		UserDataVO authUser = super.authenticateUser(userNm,pwd);
		
		if (! authUser.isAuthenticated()) {
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		} else if (authUser.isLockedOut()) {
			throw new AuthenticationException(ErrorCodes.ERR_ACCOUNT_LOCKOUT);
		}
	
		// Obtain the orgId for the profile lookup by auth Id.
		ActionRequest req = (ActionRequest)initVals.get(GlobalConfig.ACTION_REQUEST);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String orgId = site.getOrganizationId();
		
		// Retrieve the full profile by authentication ID lookup
		ProfileManager pm = ProfileManagerFactory.getInstance(initVals);
		UserDataVO wcUser;
		try {
			wcUser = pm.getProfile(authUser.getAuthenticationId(), conn, ProfileManager.AUTH_ID_LOOKUP, orgId);
			// If the lookup failed, bail.
			if (wcUser.getProfileId() == null) 
				throw new AuthenticationException();
			wcUser.setAuthenticated(authUser.isAuthenticated());
			wcUser.setAuthenticationLogId(authUser.getAuthenticationLogId());
			wcUser.setPasswordResetFlag(authUser.getPasswordResetFlag());
			if (authUser.getPasswordResetFlag() == 1) {
				wcUser.setPasswordHistory(authUser.getPasswordHistory());
			}
			// set the 'password changed date' so we can check for expired password.
			wcUser.setPasswordChangeDate(authUser.getPasswordChangeDate());
		} catch (Exception e) {
			log.error("Error attempting to retrieve WebCrescendo profile: " + e.getMessage());
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		}
		
		return wcUser;

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#hasUserProfile()
	 */
	@Override
	public Boolean hasUserProfile() {
		return Boolean.TRUE;
	}

}
