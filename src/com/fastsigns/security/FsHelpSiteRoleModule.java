package com.fastsigns.security;

import java.util.Map;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

public class FsHelpSiteRoleModule extends AbstractRoleModule {

	public FsHelpSiteRoleModule() {
		super();
	}
	
	public FsHelpSiteRoleModule(Map<String, Object> init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRole(java.lang.String,java.lang.String)
	 */
	public UserRoleVO getUserRole(String profileId, String siteId)
	throws AuthorizationException {
		
		SMTServletRequest req = (SMTServletRequest) super.initVals.get(HTTP_REQUEST);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole vo = new SBUserRole();
		vo.setSiteId(siteId);
		vo.setSiteName(site.getSiteName());
		vo.setOrganizationId(site.getOrganizationId());
		vo.setProfileId(profileId);
		vo.setRoleId(Integer.valueOf(SecurityController.PUBLIC_REGISTERED_LEVEL).toString());
		vo.setRoleName("Registered User");
		vo.setRoleLevel(SecurityController.PUBLIC_REGISTERED_LEVEL);
		vo.setStatusId(SecurityController.STATUS_ACTIVE);
		return vo;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRoles(java.lang.String)
	 */
	@Override
	public UserRoleContainer getUserRoles(String profileId)
			throws AuthorizationException {
		// TODO Do we need to integrate this with Keystone?
		return null;
	}
}
