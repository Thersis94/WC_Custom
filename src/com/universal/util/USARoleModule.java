package com.universal.util;

import java.util.Map;

import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: USARoleModule.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author billy
 * @version 1.0
 * @since Jan 12, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class USARoleModule extends AbstractRoleModule {

	/**
	 * 
	 */
	public USARoleModule() {
		
	}

	/**
	 * @param initVals
	 */
	public USARoleModule(Map<String, Object> initVals) {
		super(initVals);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRole(java.lang.String, java.lang.String)
	 */
	@Override
	public UserRoleVO getUserRole(String profileId, String siteId) {
		SBUserRole role = new SBUserRole();
		role.setProfileId(profileId);
		role.setRoleId("10");
		role.setRoleLevel(10);
		role.setRoleName("Registered User");
		role.setSiteId(siteId);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		return role;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRoles(java.lang.String)
	 */
	@Override
	public UserRoleContainer getUserRoles(String profileId)
	throws AuthorizationException {
		UserRoleVO role = new UserRoleVO();
		role.setProfileId(profileId);
		role.setRoleId("10");
		role.setRoleLevel(10);
		role.setRoleName("Registered User");
		
		UserRoleContainer urc = new UserRoleContainer();
		urc.addRole("USA_SITE_ID", role);
		
		return urc;
	}

}
