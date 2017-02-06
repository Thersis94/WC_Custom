package com.biomed.smarttrak.security;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//WC_Custom libs
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;

//SMTBaseLibs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

//WebCrescendo libs
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.DBLoginModule;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKLoginModule</p>
 <p><b>Description: </b>Custom login module for SmartTRAK user login.  Authenticates 
 user against WebCrescendo core, then retrieves SmartTRAK user data and teams 
 membership.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Jan 03, 2017
 <b>Changes:</b>
 ***************************************************************************/
public class SmartTRAKLoginModule extends DBLoginModule {

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

		// 1. Authenticate against WC core and load WC profile
		UserDataVO wcUser = authenticateUser(userNm, pwd);
		
		// 2. If authenticated, load full profile and user registration, otherwise reject login.
		if (wcUser.isAuthenticated()) {
			retrieveFullUserProfile(wcUser);
			retrieveUserRegistration(wcUser);
		} else if (wcUser.isLockedOut()) {
			throw new AuthenticationException(ErrorCodes.ERR_ACCOUNT_LOCKOUT);
		} else {
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		}

		// 3. create/populate SmarttrakUserVO based on the WC user data
		UserVO tkUser = initializeSmartTRAKUser(wcUser, new UserVO());

		// 4. Retrieve SmartTRAK-specific user data
		Connection conn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
		retrieveBaseUser(conn, tkUser);
		log.debug("Retrieved SmartTRAK user and user-teams data...");

		return tkUser;
	}

	/**
	 * Populates the SmartTRAK user object with WebCrescendo user data.
	 * @param wcUser
	 * @param tkUser
	 * @return
	 */
	private UserVO initializeSmartTRAKUser(UserDataVO wcUser, UserVO tkUser) {
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
	 * the user ID field or the profile ID field populated
	 * WC profile ID and populates the ST user bean with that data.
	 * @param conn
	 * @param tkUser
	 * @return
	 * @throws AuthenticationException
	 */
	private void retrieveBaseUser(Connection conn, 
			UserVO tkUser) throws AuthenticationException {
		// use profile ID as that is all we have at the moment.
		StringBuilder sql = new StringBuilder(410);
		sql.append("select u.user_id, u.profile_id, u.account_id, u.register_submittal_id, ");
		sql.append("u.create_dt, u.update_dt, 	xr.create_dt as assigned_dt, 	t.team_nm, ");
		sql.append("t.default_flg, t.private_flg from custom.biomedgps_user u ");
		sql.append("left outer join custom.biomedgps_user_team_xr xr on u.user_id = xr.user_id ");
		sql.append("inner join custom.biomedgps_team t on xr.team_id = t.team_id ");
		sql.append("where u.profile_id = ? 	order by t.team_nm");
		log.debug("Retrieve base user w/teams SQL: " + sql.toString());
		log.debug("Using profileId: " + tkUser.getProfileId());
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ps.setString(1, tkUser.getProfileId());
			ResultSet rs = ps.executeQuery();

			UserVO resultUser = null;
			List<TeamVO> teams = new ArrayList<>();
			while (rs.next()) {
				if (resultUser == null) {
					resultUser = new UserVO(rs);
					tkUser.setUserId(resultUser.getUserId());
					tkUser.setAccountId(resultUser.getAccountId());
					tkUser.setRegisterSubmittalId(resultUser.getRegisterSubmittalId());
					tkUser.setCreateDate(resultUser.getCreateDate());
					tkUser.setUpdateDate(resultUser.getUpdateDate());
					log.debug("resultUser userId: " + resultUser.getUserId());
				}
				TeamVO team = new TeamVO(rs);
				teams.add(team);
			}

			if (resultUser == null || resultUser.getUserId() == null) {
				throw new SQLException(ErrorCodes.ERR_INVALID_LOGIN);
			}
		} catch(SQLException ae) {
			throw new AuthenticationException(ae.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#hasUserProfile()
	 */
	@Override
	public Boolean hasUserProfile() {
		return Boolean.TRUE;
	}

}
