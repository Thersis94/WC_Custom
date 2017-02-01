package com.biomed.smarttrak.security;

// Java 7
import java.sql.Connection;

// WC Custom libs
import com.biomed.smarttrak.vo.SmartTRAKUserVO;

// SMTBaseLibs
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

		// 2. Retrieve SmartTRAK-specific user data
		Connection conn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);

		SmartTRAKUserAction sta = new SmartTRAKUserAction();
		sta.setDBConnection(conn);
		sta.setAttributes(getInitVals());

		SmartTRAKUserVO tkUser = sta.retrieveUserData(wcUser.getProfileId());
		tkUser.setData(wcUser.getDataMap());

		return tkUser;
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
