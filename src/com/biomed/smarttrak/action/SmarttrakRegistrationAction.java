package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.admin.AccountUserAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.UserLogin;
/****************************************************************************
 * <b>Title:</b> SmarttrakRegistrationAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Smarttrak Custom Registration Implementation.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 5, 2018
 ****************************************************************************/
public class SmarttrakRegistrationAction extends SimpleActionAdapter {

	private static final String ORIGINAL_PASS_CONFIRM = "originalPassConfirm";
	private static final String EMAIL_FIELD_ID = "EMAIL_ADDRESS_TXT";
	private static final String PASSWORD_FIELD_ID = "PASSWORD_TXT";
	public static final String SKIPPED_MARKETS = "skippedMarkets";

	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		//Load the Registration data.
		loadRegistration().retrieve(req);

		loadMarketsInformation(req);

		//Check if we need to load any extra Account data.
		AccountVO acct = (AccountVO)req.getSession().getAttribute(AccountAction.SESS_ACCOUNT);
		if(StringUtil.isEmpty(acct.getLeadEmail())) {
			loadAccountRepOwnerInfo(acct);
		}
	}

	/**
	 * Helper method for loading Markets Info for a User.
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	public void loadMarketsInformation(ActionRequest req) {
		AccountPermissionAction apa = ActionControllerFactoryImpl.loadAction(AccountPermissionAction.class, this);
		SmarttrakTree t = apa.getAccountPermissionTree(req);
		req.setAttribute("permissionTree", t);

		UserVO user;
		if(req.hasParameter("userId") && !"ADD".equalsIgnoreCase(req.getParameter("userId")) && attributes.get(Constants.MODULE_DATA) != null) {
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			user = ((List<UserVO>)mod.getActionData()).get(0);
		} else {
			user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		}
		req.setAttribute(SKIPPED_MARKETS, loadSkippedMarkets(user));
	}

	/**
	 * Loads the Registration action used for managing user data.
	 * @return
	 */
	public RegistrationFacadeAction loadRegistration() {
		RegistrationFacadeAction rfa = ActionControllerFactoryImpl.loadAction(RegistrationFacadeAction.class, this);
		actionInit.setActionId(AdminControllerAction.REGISTRATION_GRP_ID);
		rfa.setActionInit(actionInit);
		return rfa;
	}

	/**
	 * Load Account Lead information.  Select first available if there are
	 * multiple possibilities.
	 * @param acct 
	 */
	private void loadAccountRepOwnerInfo(AccountVO acct) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select first_nm, last_nm, email_address_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("biomedgps_user ").append(DBUtil.WHERE_CLAUSE).append("account_id = ? and acct_owner_flg = 1 ");
		sql.append(DBUtil.ORDER_BY).append("last_nm limit 1 ");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, acct.getAccountId());
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				acct.setLeadEmail(rs.getString("email_address_txt"));
				acct.setLeadFirstName(rs.getString("first_nm"));
				acct.setLeadLastName(rs.getString("last_nm"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Load a Users Skipped Markets.
	 * @param user
	 * @return
	 */
	public Set<String> loadSkippedMarkets(UserVO user) {
		Set<String> skippedMarkets = new HashSet<>();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select section_id from ").append(getCustomSchema());
		sql.append("biomedgps_user_updates_skip where user_id = ?");
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getUserId());
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				skippedMarkets.add(rs.getString("SECTION_ID"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return skippedMarkets;
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		//Verify they've provided the correct current password.
		if(!validateRequest(req)) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + "?msg=Current Password is incorrect.  Please try again.");
			return;
		}

		//Process users Registration.
		loadRegistration().build(req);

		UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		
		if (Convert.formatBoolean(req.getParameter("partialUpdate"))) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.FALSE);
			user.setUpdateDate(new Date());
			return;
		}

		//Process their Markets Selections.
		List<String> skippedMarkets = req.hasParameter(SKIPPED_MARKETS) ? Arrays.asList(req.getParameterValues(SKIPPED_MARKETS)) : new ArrayList<>();
	
		processSkipMarketPreferences(user, skippedMarkets);
	}

	/**
	 * Call to AccountUserAction to process the skipped Markets.
	 * @param user
	 * @param skippedMarkets
	 * @throws ActionException 
	 */
	private void processSkipMarketPreferences(UserVO user, List<String> skippedMarkets) throws ActionException {
		AccountUserAction aua = ActionControllerFactoryImpl.loadAction(AccountUserAction.class, this);
		user.setSkippedMarkets(skippedMarkets);
		aua.addSkippedMarkets(user);
	}

	/**
	 * Check that if the user is attempting to modify their password, they've 
	 * provided the correct current password.  If they haven't return false.
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	private boolean validateRequest(ActionRequest req) {
		boolean isValid = true;
		String username = "";
		boolean hasNewPassword = false;

		/*
		 * Look for Email on address and capture if they're trying to change
		 * their password.  We should only block if they are and need to properly
		 * handle if they are and don't provide current.
		 */
		for(Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			if(StringUtil.checkVal(e.getKey()).contains(EMAIL_FIELD_ID) && EMAIL_FIELD_ID.equals(e.getKey().split("\\|")[1])) {
				username = e.getValue()[0];
			} else if(StringUtil.checkVal(e.getKey()).contains(PASSWORD_FIELD_ID) && PASSWORD_FIELD_ID.equals(e.getKey().split("\\|")[1])) {
				hasNewPassword = !UserLogin.DUMMY_PSWD.equals(e.getValue()[0]);
			}
		}

		//Get the Current Password
		String password = StringUtil.checkVal(req.getParameter(ORIGINAL_PASS_CONFIRM));

		//Check if we have enough information to verify a password change?
		if(hasNewPassword && !StringUtil.isEmpty(username) && !StringUtil.isEmpty(password)) {
			UserLogin ul = new UserLogin(dbConn, attributes);
			try {
				String authId = ul.checkCredentials(username, password);

				isValid = authId.equals(req.getParameter("authenticationId"));
			} catch (NotAuthorizedException e) {
				log.error("User did not provide valid Current Password", e);
				isValid = false;
			}
		} else if(hasNewPassword) {
			isValid = false;
			log.debug("User attempted to change password without providing original!");
		}
		return isValid;
	}
}