package com.telovations.security;

import java.util.Map;

import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: TeloRoleModule.java <p/>
 * <b>Project</b>: WC_Misc <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 15, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class TeloRoleModule extends AbstractRoleModule {

	/**
	 * 
	 */
	public TeloRoleModule() {
	}

	/**
	 * @param initVals
	 */
	public TeloRoleModule(Map<String, Object> initVals) {
		super(initVals);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRole(java.lang.String, java.lang.String)
	 */
	@Override
	public SBUserRole getUserRole(String profileId, String siteId)
	throws AuthorizationException {
		SBUserRole role = new SBUserRole();
		role.setProfileId(profileId);
		role.setRoleId("100");
		role.setRoleLevel(100);
		role.setRoleName("Site Administrator");
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
		role.setRoleId("100");
		role.setRoleLevel(100);
		role.setRoleName("Site Administrator");
		
		UserRoleContainer urc = new UserRoleContainer();
		urc.addRole("TELO_SITE_ID", role);
		
		return urc;
	}

}
