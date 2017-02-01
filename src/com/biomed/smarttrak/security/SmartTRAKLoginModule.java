package com.biomed.smarttrak.security;

// Java 7
import java.sql.Connection;

//WC_Custom libs
import com.biomed.smarttrak.admin.user.TeamManagerAction;
import com.biomed.smarttrak.admin.user.UserManagerAction;
import com.biomed.smarttrak.vo.SmarttrakUserVO;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
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
		SmarttrakUserVO tkUser = new SmarttrakUserVO();
		tkUser.setData(wcUser.getDataMap());
		
		// 3. Retrieve SmartTRAK-specific user data
		Connection conn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
		retrieveBaseUser(conn, tkUser);
		retrieveBaseUserTeams(conn, tkUser);
		
		return tkUser;
	}
	
	/**
	 * Retrieves base user data for the user represented by the
	 * WC profile ID and populates the ST user bean with that data.
	 * @param conn
	 * @param tkUser
	 * @return
	 * @throws AuthenticationException
	 */
	private SmarttrakUserVO retrieveBaseUser(Connection conn, 
			SmarttrakUserVO tkUser) throws AuthenticationException {
		UserManagerAction uma = new UserManagerAction();
		uma.setDBConnection(new SMTDBConnection(conn));
		uma.setAttributes(getInitVals());
		try {
			uma.retrieveBaseUser(tkUser);
		} catch(ActionException ae) {
			throw new AuthenticationException(ae.getMessage());
		}
		return tkUser;
	}
	
	/**
	 * Retrieves list of teams to which a user belongs and populates
	 * the user VO with the list of teams.
	 * @param conn
	 * @param tkUser
	 * @throws AuthenticationException
	 */
	private void retrieveBaseUserTeams(Connection conn, 
			SmarttrakUserVO tkUser) throws AuthenticationException {
		TeamManagerAction tma = new TeamManagerAction();
		tma.setDBConnection(new SMTDBConnection(conn));
		tma.setAttributes(getInitVals());
		try {
			tma.retrieveUserTeams(tkUser);
		} catch (ActionException ae) {
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
