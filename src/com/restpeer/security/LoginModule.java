package com.restpeer.security;

// JDK 1.8.x
import java.util.Map;

// RestPeer Custom
import com.restpeer.action.admin.UserAction;
import com.restpeer.data.RPUserVO;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;

// WC3
import com.smt.sitebuilder.security.DBLoginModule;

/****************************************************************************
 * <b>Title</b>: LoginModule.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom DB Login module for RestPeer that stores the
 * RPUserVO from the custom project as extended data.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since June 13, 2019
 ****************************************************************************/
public class LoginModule extends DBLoginModule {

	public LoginModule() {
		super();
	}

	public LoginModule(Map<String, Object> config) {
		super(config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#loadUserData(java.lang.String, java.lang.String)
	 */
	@Override
	protected UserDataVO loadUserData(String profileId, String authenticationId) {
		UserDataVO user = super.loadUserData(profileId, authenticationId);
		if (user == null) return null; //same logic as superclass
		
		// Get the extended user data
		RPUserVO rpUser = getRPUser(user.getProfileId());
		if (rpUser.getUserId() == null) return user;
		user.setUserExtendedInfo(rpUser);
		
		return user;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO uvo = super.authenticateUser(user, pwd);
		checkExtendedData(uvo);
		
		return uvo;
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO uvo = super.authenticateUser(encProfileId);
		checkExtendedData(uvo);

		return uvo;
	}
	
	/**
	 * Catches the edge case of a user in WC3, but not a registered user of RestPeer.
	 * 
	 * @param uvo
	 * @throws AuthenticationException
	 */
	protected void checkExtendedData(UserDataVO uvo) throws AuthenticationException {
		if (uvo.getUserExtendedInfo() == null)
			throw new AuthenticationException("User is a user of the site, but not registered as a RestPeer user.");
	}

	/**
	 * Gets the RestPeer user data.
	 * 
	 * @param profileId
	 * @return
	 */
	protected RPUserVO getRPUser(String profileId) {
		SMTDBConnection dbConn = (SMTDBConnection) getAttribute(GlobalConfig.KEY_DB_CONN);
		
		UserAction ua = new UserAction(dbConn, getAttributes());
		return ua.getUserByProfileId(profileId);
	}
}