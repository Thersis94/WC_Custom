package com.mts.security;

import java.sql.Connection;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.admin.action.RoleAction;
import com.smt.sitebuilder.admin.action.data.RoleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SAMLRoleModule;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <p><b>Title:</b> MTSRoleModule.java</p>
 * <p><b>Description:</b> Overload the createRole method to create the MTS 
 * role rather than WC default role.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 31, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class MTSRoleModule extends SAMLRoleModule {

	public MTSRoleModule() {
		super();
	}

	public MTSRoleModule(Map<String, Object> init) {
		super(init);
	}

	/**
	 * Create the user's role using the roleId forwarded to us by the login module.
	 * (Since the login module had it loaded, we just forwarded it along rather than doing a 2nd lookup.)
	 */
	@Override
	protected SBUserRole createRegisteredUserRole(UserDataVO user, String siteId) 
			throws AuthorizationException {
		ActionRequest req = (ActionRequest) initVals.get(DBRoleModule.HTTP_REQUEST);
		SSOProviderVO provider = (SSOProviderVO) req.getAttribute("MTS-SSO-Provider");

		//proceed to default role creation if we don't have the needed override
		if (provider == null || StringUtil.isEmpty(provider.getRoleId())) {
			log.debug("MTS provider not passed");
			return super.createRegisteredUserRole(user, siteId);
		}

		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		Connection conn = (Connection) initVals.get(DBRoleModule.DB_CONN);
		RoleAction ra = new RoleAction(new SMTDBConnection(conn), initVals);

		RoleVO role = ra.getRole(provider.getRoleId());
		SBUserRole sRole = new SBUserRole();
		sRole.setProfileId(user.getProfileId());
		sRole.setSiteId(siteId);
		sRole.setOrganizationId(site.getOrganizationId());
		sRole.setRoleId(role.getRoleId());
		sRole.setRoleLevel(role.getRoleLevel());
		sRole.setRoleName(role.getRoleName());
		sRole.setStatusId(SecurityController.STATUS_ACTIVE);
		return saveRole(sRole);
	}
}
