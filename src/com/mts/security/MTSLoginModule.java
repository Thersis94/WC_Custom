package com.mts.security;

//JDK 1.8.x
import java.sql.Connection;
import java.util.Map;

import com.mts.subscriber.action.SubscriptionAction;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
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

	public MTSLoginModule() {
		super();
	}

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
			loadSubscriptions(authUser, conn);
		}
		return authUser;
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String email, String pwd) throws AuthenticationException {
		UserDataVO authUser = null;
		Connection conn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);

		// Authenticate the user by IP address or follow std db login
		if (CORPORATE_DEFAULT_EMAIL.equalsIgnoreCase(email)) {
			ActionRequest req = (ActionRequest) getAttribute(AbstractRoleModule.HTTP_REQUEST);
			log.debug(String.format("Login by IP: %s | %s", req.getRemoteAddr(), req.getParameter("clientIpAddress")));

			authUser = getUserByIPAddr(req.getParameter("clientIpAddress"), conn);

		} else {
			authUser = super.authenticateUser(email, pwd);
		}

		// If the user was successfully authenticated, get the extended user data
		if (authUser != null)  loadSubscriptions(authUser, conn);

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
		return !StringUtil.isEmpty(profileId) ? loadUserData(profileId, null) : null;
	}


	/**
	 * Ask SubscriptionAction to populate the user vo with their subscription(s)
	 * @param authUser
	 * @param conn
	 */
	private void loadSubscriptions(UserDataVO authUser, Connection conn) {
		SubscriptionAction sa = new SubscriptionAction(new SMTDBConnection(conn), getAttributes());
		sa.loadSubscriptions(authUser);
	}
}
