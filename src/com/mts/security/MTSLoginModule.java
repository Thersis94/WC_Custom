package com.mts.security;

//JDK 1.8.x
import java.sql.Connection;
import java.util.Map;

import com.mts.admin.action.SSOProviderAction;
import com.mts.admin.action.UserAction;
import com.mts.subscriber.action.SubscriptionAction;
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserDataVO.AuthenticationType;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.admin.action.SiteAuthManageAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SAMLLoginModule;

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
 * 		Refactored to support SSO (superclass) and move subscriptions to proper home.  -JM- 08.28.19
 ****************************************************************************/
public class MTSLoginModule extends SAMLLoginModule {
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
		if (authUser != null && authUser.isAuthenticated()) {
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
		if (authUser != null && authUser.isAuthenticated())  loadSubscriptions(authUser, conn);

		// ensure users created through SAML in the past aren't trying to use a password, which circumvents their corp agreement
		MTSUserVO mtsUser = authUser != null ? (MTSUserVO)authUser.getUserExtendedInfo() : null;
		if (mtsUser != null && AuthenticationType.SAML != authUser.getAuthType() && !StringUtil.isEmpty(mtsUser.getSsoId()))
			throw new AuthenticationException("Please use Corporate Sign-in");

		return authUser;
	}


	/*
	 * This hook is only called during SAML logins.  We want to make sure the mts_user exists, or create them, 
	 * so login can complete (through custom user/subscription lookup)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.SAMLLoginModule#manageProfile(java.sql.Connection, com.siliconmtn.security.UserDataVO)
	 */
	@Override
	protected void manageProfile(Connection conn, UserDataVO origUser) throws AuthenticationException {
		//leverage upstream code to create the WC profile, profile_role, org_profile_comm, etc.
		log.debug("mts manageProfile");
		super.manageProfile(conn, origUser);

		SMTDBConnection dbConn = new SMTDBConnection(conn);

		//get the SSO config for this vendor
		SSOProviderAction ssoPa = new SSOProviderAction(dbConn, getAttributes());
		SSOProviderVO provider = ssoPa.getProviderById((String)getAttribute(SiteAuthManageAction.LOGIN_MODULE_XR_ID));
		if (provider == null || StringUtil.isEmpty(provider.getSsoId()))
			throw new AuthenticationException("could not load provider for spEntityId=" + getSPEntityId());

		//create the MTS user
		MTSUserVO mtsUser = new MTSUserVO(origUser);
		mtsUser.setSubscriptionType(SubscriptionType.SSO);
		mtsUser.setRoleId(provider.getRoleId());
		mtsUser.setExpirationDate(provider.getExpirationDate());
		mtsUser.setSsoId(provider.getSsoId());
		mtsUser.setRoleId(provider.getRoleId());
		mtsUser.setActiveFlag(1);

		boolean isUserInsert = true;
		UserAction ua = new UserAction(dbConn, getAttributes());
		try {
			log.debug("checking for user " + mtsUser.getProfileId());
			//check to see if this user already exists (by profileId)
			mtsUser.setUserId(ua.getUserIdByProfileId(mtsUser.getProfileId()));
			isUserInsert = StringUtil.isEmpty(mtsUser.getUserId());

			log.debug("saving user " + mtsUser);
			ua.updateUser(mtsUser, ua.getSSOColumns());
		} catch (Exception e) {
			log.error("could not create mts user account", e);
		}

		//bind the user's subscriptions - we only need to run this if we created the user
		///if, by chance, this runs for an existing user their subscriptions are purged and replaced; which is okay.
		if (isUserInsert) {
			log.debug("creating user subscriptions");
			SubscriptionAction sa = new SubscriptionAction(dbConn, getAttributes());
			try {
				sa.assignSubscriptions(mtsUser.getUserId(), provider.getPublications());
			} catch (Exception e) {
				log.error("could not create mts user subscriptions", e);
			}
		}

		//remove the SSO response from the request, which causes SecurityController to stop iterating the SAML modules (for other companies)
		ActionRequest req = (ActionRequest) getAttribute(AbstractRoleModule.HTTP_REQUEST);
		req.setParameter(Constants.SSO_SAML_RESPONSE, null);
		//pass the provider along to the role module
		req.setAttribute("MTS-SSO-Provider", provider);
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
