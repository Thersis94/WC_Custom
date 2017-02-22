package com.biomed.smarttrak.security;

//JDK
import java.util.Map;

// SMTBaseLibs 2.0
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

// WebCrescendo 3.0
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRole;

//WC Custom
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKRoleModule</p>
 <p><b>Description: Extends Core functionality to include loading the Smartrak Section permissions, from the account level.</b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Jan 03, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKRoleModule extends DBRoleModule {

	public SmartTRAKRoleModule() {
		super();
	}

	/**
	 * @param init - Map of init objects used by the class.
	 */
	public SmartTRAKRoleModule(Map<String, Object> init) {
		super(init);
	}


	/**
	 * overrides the WC Core functionality to include loading the Smartrak Section permissions, at the account level.
	 */
	@Override
	public SBUserRole getUserRole(String profileId, String siteId) throws AuthorizationException {
		SmarttrakRoleVO role = new SmarttrakRoleVO(super.getUserRole(profileId, siteId));
		loadSmarttrakAccountPermissions(role);
		return role;
	}


	/**
	 * @param role
	 * @throws AuthorizationException 
	 */
	protected void loadSmarttrakAccountPermissions(SmarttrakRoleVO role) throws AuthorizationException {
		UserVO user = (UserVO) getAttribute(Constants.USER_DATA);
		ActionRequest req = (ActionRequest) getAttribute(HTTP_REQUEST);

		if (user == null || StringUtil.isEmpty(user.getAccountId()) || req == null)
			throw new AuthorizationException("invalid data, could not run");

		req.setParameter(AccountAction.ACCOUNT_ID, user.getAccountId());

		AccountPermissionAction apa = new AccountPermissionAction();
		apa.setDBConnection((SMTDBConnection)getAttribute(DB_CONN));
		apa.setAttributes(getInitVals());
		try {
			apa.retrieve(req);
			ModuleVO mod = (ModuleVO) apa.getAttribute(Constants.MODULE_DATA);
			Tree perms = (Tree) mod.getActionData();
			role.setAccountRoles(perms.preorderList());
		} catch (Exception e) {
			throw new AuthorizationException("could not load Smartrak permissions", e);
		}
	}
}