package com.biomed.smarttrak.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//JDK
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 3.0
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.action.user.LoginAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;

//WC Custom
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.UserVO;

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

	/**
	 * Smarttrak licenseTypes levels not permitted to login.
	 */
	protected static final List<String> blockedLicenses = new ArrayList<>(Arrays.asList("I"));
	/**
	 * Smarttrak licenseTypes only permitted login to view updates
	 */
	protected static final List<String> updatesOnlyLicenses = new ArrayList<>(Arrays.asList("U", "T", "4")); //4 comes from account->TypeID
	
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
		loadSmarttrakRoles(role);
		return role;
	}


	/**
	 * facades the calls we need to make to load account, user, and ACL permissions
	 * @param role
	 * @throws AuthorizationException 
	 */
	protected void loadSmarttrakRoles(SmarttrakRoleVO role) throws AuthorizationException {
		UserVO user;
		/* 2017-06-21 DBargerhuff: In case of 'forgot password' request, the user data on the
		 * attributes map will be a UserDataVO instead of a SmartTRAK UserVO.  We try/catch
		 * here to handle the CCE that results from the former case. */
		try {
			user = (UserVO) getAttribute(Constants.USER_DATA);
		} catch (ClassCastException cce) {
			// logging not required.
			return;
		}
		ActionRequest req = (ActionRequest) getAttribute(HTTP_REQUEST);

		if (user == null || StringUtil.isEmpty(user.getAccountId()) || req == null)
			throw new AuthorizationException("invalid data, could not load roles");

		if (user.getExpirationDate() != null && user.getExpirationDate().before(Convert.getCurrentTimestamp()))
			throw new AuthorizationException("account is expired");

		//if status is EU Reports, redirect them to the markets page
		if ("M".equals(user.getLicenseType())) {
			req.getSession().setAttribute(LoginAction.DESTN_URL, Section.MARKET.getPageURL());
		} else if (updatesOnlyLicenses.contains(user.getLicenseType())) {
			//limit the user to updates if their account is limited to updates
			if ("4".equals(user.getLicenseType())) {
				role.setRoleId(AdminControllerAction.UPDATES_ROLE_ID);
				role.setRoleLevel(AdminControllerAction.UPDATES_ROLE_LVL);
			}
			req.getSession().setAttribute(LoginAction.DESTN_URL, Section.UPDATES_EDITION.getPageURL());
		}else if (blockedLicenses.contains(user.getLicenseType())) {
			throw new AuthorizationException("user not authorized to login according to status");
		}

		req.setParameter(AccountAction.ACCOUNT_ID, user.getAccountId());

		loadAccountPermissions(user, role);
		loadSectionPermissions(req, role);
	}


	/**
	 * Check for SECTION authorization for our tools.  If no sections are defined, the sections should be considered 'not authorized'
	 * @param req
	 * @param role
	 * @throws AuthorizationException
	 */
	protected void loadAccountPermissions(UserVO user, SmarttrakRoleVO role) throws AuthorizationException {
		SMTDBConnection dbConn = (SMTDBConnection)getAttribute(DB_CONN);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select sum(fd_no) as fd, sum(ga_no) as ga, sum(pe_no) as pe, sum(an_no) as an from ").append(schema);
		sql.append("BIOMEDGPS_ACCOUNT_ACL where account_id=?");
		log.debug(sql);

		int fdAuth = 0;
		int gaAuth = 0;
		int anAuth = 0;
		int peAuth = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getAccountId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				//determine if the user is authorized for each tool, either at the account level or user (personal) level
				//we need a 1/0 (Yes/No) - don't really care how many sections they're authorized for, only that some exist.
				fdAuth = rs.getInt("fd") > 0 ? 1 : 0;
				gaAuth = rs.getInt("ga") > 0 ? 1 : 0;
				peAuth = rs.getInt("pe") > 0 ? 1 : 0;
				anAuth = rs.getInt("an") > 0 ? 1 : 0;
			}

		} catch (SQLException sqle) {
			throw new AuthorizationException(sqle);
		}

		//set the 3 section permissions into the role VO
		role.setFdAuthorized(user.getFdAuthFlg(), fdAuth);
		role.setGaAuthorized(user.getGaAuthFlg(), gaAuth);
		role.setPeAuthorized(0, peAuth); //not overrideable at the user level
		role.setAnAuthorized(0, anAuth); //not overrideable at the user level
		role.setAccountOwner(user.getAcctOwnerFlg());
	}


	/**
	 * loads the permission matrix (based on hierarchy sections) for the user's account
	 * @param req
	 * @param role
	 * @throws AuthorizationException
	 */
	protected void loadSectionPermissions(ActionRequest req, SmarttrakRoleVO role) throws AuthorizationException {
		AccountPermissionAction apa = new AccountPermissionAction();
		apa.setDBConnection((SMTDBConnection)getAttribute(DB_CONN));
		apa.setAttributes(getInitVals());
		try {
			//retrieve the permission tree for this account
			apa.retrieve(req);
			ModuleVO mod = (ModuleVO) apa.getAttribute(Constants.MODULE_DATA);
			SmarttrakTree t = (SmarttrakTree) mod.getActionData();

			//iterate the nodes and attach parent level tokens to each, at each level.  spine.  spine~bone.  spine~bone~fragment.  etc.
			t.buildNodePaths();

			//attach the list of permissions to the user's role object
			role.setAccountRoles(t.preorderList());

		} catch (Exception e) {
			throw new AuthorizationException("could not load Smartrak permissions", e);
		}
		log.debug("loaded " + role.getAccountRoles().size() + " account permissions into the roleVO");
	}
}