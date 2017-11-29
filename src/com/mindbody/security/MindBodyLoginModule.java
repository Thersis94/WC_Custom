package com.mindbody.security;

import java.util.List;
import java.util.Map;

import com.mindbody.util.ClientApiUtil;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBLoginModule;

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

	public MindBodyLoginModule() {
		super();
	}

	/**
	 * @param config
	 */
	public MindBodyLoginModule(Map<String, Object> config) {
		super(config);
	}


	/*
	 * username/password login - Return user after loading their Mindbody data
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String username, String password) throws AuthenticationException {
		return loadMindBodyUser(super.authenticateUser(username, password));
	}


	/*
	 * cookie login -  Return user after loading their Mindbody data
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		return loadMindBodyUser(super.authenticateUser(encProfileId));
	}


	/**
	 * Create new MindBodyUserVO from existing UserDataVO and then populate
	 * with custom data from Mindbody system.
	 * @param loadUserData
	 * @return
	 * @throws AuthenticationException 
	 */
	private UserDataVO loadMindBodyUser(UserDataVO user) throws AuthenticationException {
		validateMindBodyUser(user); //this will throw if the password can't be verified

		MindBodyUserVO mbUser = new MindBodyUserVO();
		mbUser.setData(user.getDataMap());
		mbUser.setAttributes(user.getAttributes());
		mbUser.setAuthenticated(user.isAuthenticated());
		mbUser.setClientId((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID));
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

		//Load Perkville Data
		loadPerkville(mbUser);
	}

	/**
	 * After making the call to perkville, we need to populate
	 * the users data with the Perkville Points Response.
	 * @param mbUser
	 */
	private void loadPerkville(MindBodyUserVO mbUser) {
		//This should tie into the Perkville API.
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
	private void validateMindBodyUser(UserDataVO authUser) throws AuthenticationException {
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA);
		ClientApiUtil util = new ClientApiUtil(site.getSiteConfig());

		MindBodyResponseVO resp = util.validateClient(authUser);

		if (resp != null && resp.isValid()) {
			//Response is a MindBody Client VO
			List<Object> users = resp.getResults();
			Object user = users.get(0);
			authUser.addAttribute(MindBodyUtil.MINDBODY_CLIENT_ID, ((UserDataVO)user).getProfileId());

		} else {
			throw new AuthenticationException("User Could not be verified in the MindBody System");
		}
	}
}
