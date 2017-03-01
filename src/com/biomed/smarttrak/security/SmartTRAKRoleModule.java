package com.biomed.smarttrak.security;

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
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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
		UserVO user = (UserVO) getAttribute(Constants.USER_DATA);
		ActionRequest req = (ActionRequest) getAttribute(HTTP_REQUEST);

		if (user == null || StringUtil.isEmpty(user.getAccountId()) || req == null)
			throw new AuthorizationException("invalid data, could not load roles");

		if (user.getExpirationDate() != null && user.getExpirationDate().before(Convert.getCurrentTimestamp()))
			throw new AuthorizationException("account is expired");

		req.setParameter(AccountAction.ACCOUNT_ID, user.getAccountId());

		loadAccountPermissions(user, role);
		loadSectionPermissions(req, role);
	}


	/**
	 * calls for the AccountVO to be loaded, so we can see which features are enabled.
	 * @param req
	 * @param role
	 * @throws AuthorizationException
	 */
	protected void loadAccountPermissions(UserVO user, SmarttrakRoleVO role) throws AuthorizationException {
		SMTDBConnection dbConn = (SMTDBConnection)getAttribute(DB_CONN);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select fd_auth_flg, ga_auth_flg, mkt_auth_flg from ").append(schema);
		sql.append("BIOMEDGPS_ACCOUNT where account_id=?");
		log.debug(sql);

		int fdAuth = 0, gaAuth = 0, mktAuth = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getAccountId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				//determine if the user is authorized for each tool, either at the account level or user (personal) level
				fdAuth = rs.getInt("fd_auth_flg");
				gaAuth = rs.getInt("ga_auth_flg");
				mktAuth = rs.getInt("mkt_auth_flg");
			}

		} catch (SQLException sqle) {
			throw new AuthorizationException(sqle);
		}

		//set the 3 section permissions into the role VO
		role.setFdAuthorized(user.getFdAuthFlg(), fdAuth);
		role.setGaAuthorized(user.getGaAuthFlg(), gaAuth);
		role.setMktAuthorized(user.getMktAuthFlg(), mktAuth);
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