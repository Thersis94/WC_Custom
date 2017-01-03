package com.biomed.smarttrak.security;

// SMTBaseLibs 2.0
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;

// WebCrescendo 3.0
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRoleContainer;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKRoleModule</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2016 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Dec 20, 2016
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKRoleModule extends DBRoleModule {

	@Override
	public SBUserRoleContainer adminAuthorized(String profileId) throws AuthorizationException {
		return super.adminAuthorized(profileId);
	}

	@Override
	public UserRoleVO getUserRole(String profileId, String siteId) throws AuthorizationException {
		// TODO Add custom role processing (e.g. retrieve one or more subscription roles)
		return super.getUserRole(profileId, siteId);
	}

	@Override
	public UserRoleContainer getUserRoles(String authId) throws AuthorizationException {
		// TODO Add custom role processing (e.g. retrieve subscription roles)
		return super.getUserRoles(authId);
	}

}
