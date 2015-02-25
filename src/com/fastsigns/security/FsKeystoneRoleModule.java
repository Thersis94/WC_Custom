package com.fastsigns.security;

//JDK
import java.util.Map;


//SMT Base Libs
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;

//WC libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: FsKeystoneDBRoleModule.java<p/>
 * <b>Description: proxies WC user login as well as a call to Keystone for user 
 * 		authentication.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sept 24, 2012
 ****************************************************************************/
public class FsKeystoneRoleModule extends AbstractRoleModule {
	
	public FsKeystoneRoleModule() {
		super();
	}
	
	public FsKeystoneRoleModule(Map<String, Object> init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRole(java.lang.String,java.lang.String)
	 */
	public UserRoleVO getUserRole(String profileId, String siteId)
	throws AuthorizationException {
		
		/*  TODO tweak this code to use role_id and role_permission_id from user's attributes.
			(They were put there for this reason via the loginModule, but Keystone does not yet populate them)
			JM 01-10-13
			
			You'll need to pass UserDataVO on the attributes map, from SecurityController.
		*/ 
		
		
		UserDataVO user = (UserDataVO) initVals.get(Constants.USER_DATA);
		if (!Convert.formatBoolean(user.getAttributes().get("isKeystone"))) {
			throw new AuthorizationException(ErrorCodes.ERR_NOT_AUTHORIZED);
		}

		SMTServletRequest req = (SMTServletRequest) super.initVals.get(HTTP_REQUEST);
		//UserDataVO user = (UserDataVO) initVals.get(Constants.USER_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole vo = new SBUserRole();
		vo.setSiteId(siteId);
		vo.setSiteName(site.getSiteName());
		vo.setOrganizationId(site.getOrganizationId());
		vo.setProfileId(profileId);
		vo.setRoleId(Integer.valueOf(SecurityController.PUBLIC_REGISTERED_LEVEL).toString());
		vo.setRoleName("Registered User");
		vo.setRoleLevel(SecurityController.PUBLIC_REGISTERED_LEVEL);
		//if (Convert.formatBoolean(user.getAttributes().get("active")))
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
