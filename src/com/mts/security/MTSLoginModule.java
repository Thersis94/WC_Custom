package com.mts.security;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
// JDK 1.8.x
import java.util.Map;

import com.mts.subscriber.data.MTSUserVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;
// WC Lobs
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
		loadSubscribers(authUser);
		
		return super.authenticateUser(encProfileId);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO authUser = super.authenticateUser(user, pwd);
		loadSubscribers(authUser);
		ActionRequest req = (ActionRequest) getAttribute(AbstractRoleModule.HTTP_REQUEST);
		log.info("Address: " + req.getRemoteAddr());
		log.info("MTS DB Login: " + authUser.getUserExtendedInfo());
		
		return authUser;
	}
	
	/**
	 * Gets the extended user and subscriber info
	 * @param authUser
	 */
	protected void loadSubscribers(UserDataVO authUser) {
		// Set the config info
		Connection conn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<Object>vals = new ArrayList<>();
		vals.add(authUser.getProfileId());
		
		// Build the sql
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("mts_user a "); 
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("mts_subscription_publication_xr b on a.user_id = b.user_id ");
		sql.append("where profile_id = ? ");
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		// Get the user extended info and assign it to the user object  
		DBProcessor db = new DBProcessor(conn);
		List<MTSUserVO> userPubs = db.executeSelect(sql.toString(), vals, new MTSUserVO());
		if (! userPubs.isEmpty())
			authUser.setUserExtendedInfo(userPubs.get(0));
	}

}

