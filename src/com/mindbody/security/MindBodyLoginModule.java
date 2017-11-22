package com.mindbody.security;

import java.sql.Connection;
import java.util.Map;

import com.mindbody.util.ClientApiUtil;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.DBLoginModule;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title:</b> MindBodyLoginModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Login Module for MindBody Powered Sites.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 16, 2017
 ****************************************************************************/
public class MindBodyLoginModule extends DBLoginModule {

	/**
	 * 
	 */
	public MindBodyLoginModule() {
		super();
	}

	/**
	 * @param config
	 */
	public MindBodyLoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO authenticateUser(String username, String password) throws AuthenticationException {
		if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password))
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		log.debug("Starting authenticateUser: " + username + "/" + password);

		// call UserLogin to load the authentication record
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		UserLogin ul = new UserLogin(dbConn, getAttributes());
		UserDataVO authUser = ul.getAuthRecord(null, username);
		authUser.setEmailAddress(username);

		//getAuthRecord never returns null.  Test the VO for authenticationId, throw if not found
		if (StringUtil.isEmpty(authUser.getAuthenticationId()))
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);

		// Return user authenticated user, after loading their Mindbody data
		return loadMindBodyUser(loadUserData(null, authUser.getAuthenticationId()));
	}

	/**
	 * Create new MindBodyUserVO from existing UserDataVO and then populate
	 * with custom data from Mindbody system.
	 * @param loadUserData
	 * @return
	 * @throws AuthenticationException 
	 */
	private UserDataVO loadMindBodyUser(UserDataVO userData) throws AuthenticationException {
		userData = validateMindBodyUser(userData); //this will throw if the password can't be verified

		MindBodyUserVO mbUser = new MindBodyUserVO();
		mbUser.setData(userData.getDataMap());
		mbUser.setAttributes(userData.getAttributes());
		mbUser.setAuthenticated(userData.isAuthenticated());
		loadCustomData(mbUser);
		return mbUser;
	}

	/**
	 * Load User Data from Mindbody.
	 * @param stUser
	 */
	private void loadCustomData(MindBodyUserVO mbUser) {
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA);
		ClientApiUtil util = new ClientApiUtil(site.getSiteConfig());

		util.reloadUserData(mbUser);
	}

	/**
	 * Validate the User against the Mindbody system.  If we can't verify
	 * a record then they can't login.
	 * @param ul
	 * @param authUser
	 * @param password
	 * @return
	 * @throws AuthenticationException 
	 */
	private UserDataVO validateMindBodyUser(UserDataVO authUser) throws AuthenticationException {
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA);
		ClientApiUtil util = new ClientApiUtil(site.getSiteConfig());

		MindBodyResponseVO resp = util.validateClient(authUser);

		if(resp.isValid()) {
			//Response is a MindBody Client VO
			authUser.addAttribute(MindBodyUtil.MINDBODY_CLIENT_ID, ((UserDataVO)resp.getResults().get(0)).getProfileId());
			return authUser;
		} else {
			//User Data isn't available from MindBody.
			throw new AuthenticationException("User Could not be verified in the MindBody System");
		}
	}
}
