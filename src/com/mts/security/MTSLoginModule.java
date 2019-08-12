package com.mts.security;

// JDK 1.8.x
import java.util.Map;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;

// WC Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBLoginModule;

/****************************************************************************
 * <b>Title</b>: MTSLoginModule.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Overrides the db login module.  Makes call out to get the users
 * subscriber information
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 10, 2019
 * @updates:
 ****************************************************************************/

public class MTSLoginModule extends DBLoginModule {
	/**
	 * Email/User Name that is passed for the IP address validation
	 */
	public static final String CORPORATE_DEFAULT_EMAIL = "ip@mtssecurity.com";

	/**
	 * 
	 */
	public MTSLoginModule() {
		super();
	}

	/**
	 * @param config
	 */
	public MTSLoginModule(Map<String, Object> config) {
		super(config);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO authUser = super.authenticateUser(encProfileId);
		if (authUser != null) {
			Connection conn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
			loadSubscribers(authUser, conn);
		}
		
		return super.authenticateUser(encProfileId);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO authUser = null;
		Connection conn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		
		// Authenticate the user by IP address or form the std db login
		if (CORPORATE_DEFAULT_EMAIL.equalsIgnoreCase(user)) {
			ActionRequest req = (ActionRequest) getAttribute(AbstractRoleModule.HTTP_REQUEST);
			authUser = getUserByIPAddr(req.getRemoteAddr(), conn);
			
		} else {
			authUser = super.authenticateUser(user, pwd);
		}
		
		// If the user was successfully authenticated, get the extended user data
		if (authUser != null)  loadSubscribers(authUser, conn);
		
		return authUser;
	}
	
	/**
	 * Loads the user data vo associated to the ip address
	 * @param ip
	 * @param conn
	 * @return
	 */
	public UserDataVO getUserByIPAddr(String ip, Connection conn) {
		IPSecurityAction isa = new IPSecurityAction(conn, getAttributes());
		String profileId = isa.getProfileIdByIP(ip);
		if (profileId == null) return null;
		
		return loadUserData(profileId, null);
	}
	
	/**
	 * Gets the extended user and subscriber info
	 * @param authUser
	 */
	protected void loadSubscribers(UserDataVO authUser, Connection conn) {
		// Set the config info
		
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<Object>vals = new ArrayList<>();
		vals.add(authUser.getProfileId());
		
		// Build the sql
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("mts_user a "); 
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("mts_subscription_publication_xr b on a.user_id = b.user_id ");
		sql.append("where profile_id = ? and expiration_dt is not null ");
		sql.append("and expiration_dt > now() and active_flg = 1 ");
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		// Get the user extended info and assign it to the user object  
		DBProcessor db = new DBProcessor(conn);
		List<MTSUserVO> userPubs = db.executeSelect(sql.toString(), vals, new MTSUserVO());
		if (! userPubs.isEmpty())
			authUser.setUserExtendedInfo(userPubs.get(0));
	}

}

