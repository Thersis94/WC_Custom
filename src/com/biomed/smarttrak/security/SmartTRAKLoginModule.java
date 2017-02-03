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
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WebCrescendo libs
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

		// 1. Authenticate against WC core
		UserDataVO wcUser = super.authenticateUser(userNm,pwd);

		// 2. create/populate SmarttrakUserVO
		UserVO tkUser = new UserVO();
		tkUser.setData(wcUser.getDataMap());
		
		// 3. Retrieve SmartTRAK-specific user data using the WC profileId.
		Connection conn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
		retrieveBaseUser(conn, tkUser);
		retrieveBaseUserTeams(conn, tkUser);
		
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
		
		List<UserVO> users;
		try {
			users = um.retrieveBaseUser();
		} catch(SQLException ae) {
			throw new AuthenticationException(ae.getMessage());
		}

		if (users.isEmpty()) throw new AuthenticationException("SmartTRAK user does not exist.");
		
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
		return this.retrieveUserData(userNm, pwd);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#hasUserProfile()
	 */
	@Override
	public Boolean hasUserProfile() {
		return Boolean.TRUE;
	}

}
