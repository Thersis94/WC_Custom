package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.admin.AccountUserAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
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
public class SmarttrakRegistrationAction extends RegistrationFacadeAction {

	private static final String ORIGINAL_PASS_CONFIRM = "originalPassConfirm";
	private static final String EMAIL_FIELD_ID = "EMAIL_ADDRESS_TXT";
	public static final String SKIPPED_MARKETS = "skippedMarkets";

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		RegistrationFacadeAction rfa = ActionControllerFactoryImpl.loadAction(RegistrationFacadeAction.class, this);
		actionInit.setActionId(AdminControllerAction.REGISTRATION_GRP_ID);
		rfa.setActionInit(actionInit);
		rfa.retrieve(req);

		AccountPermissionAction apa = ActionControllerFactoryImpl.loadAction(AccountPermissionAction.class, this);
		SmarttrakTree t = apa.getAccountPermissionTree(req);
		req.setAttribute("permissionTree", t);

		UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		req.setAttribute(SKIPPED_MARKETS, loadSkippedMarkets(user));
	}

	/**
	 * Load a Users Skipped Markets.
	 * @param user
	 * @return
	 */
	private Set<String> loadSkippedMarkets(UserVO user) {
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
		if(!validateRequest(req)) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + "?msg=Current+Password+is+incorrect.++Please+try+again.");
			return;
		}
		super.build(req);
		if(req.hasParameter("skippedMarkets")) {
			List<String> skippedMarkets = Arrays.asList(req.getParameterValues(SKIPPED_MARKETS));
			UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
	
			processSkipMarketPreferences(user, skippedMarkets);
		}
	}

	/**
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
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	private boolean validateRequest(ActionRequest req) throws ActionException {
		boolean isValid = true;
		String username = "";
		for(Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			if(StringUtil.checkVal(e.getKey()).contains(EMAIL_FIELD_ID) && EMAIL_FIELD_ID.equals(e.getKey().split("\\|")[1])) {
				username = e.getValue()[0];
				break;
			}
		}

		String password = StringUtil.checkVal(req.getParameter(ORIGINAL_PASS_CONFIRM));

		//Check if we have enoug information to verify a password change?
		if(!StringUtil.isEmpty(username) && !StringUtil.isEmpty(password)) {
			UserLogin ul = new UserLogin(dbConn, attributes);
			try {
				String authId = ul.checkCredentials(username, password);

				isValid = authId.equals(req.getParameter("authenticationId"));
			} catch (NotAuthorizedException e) {
				log.error("User did not provide valid Current Password", e);
				isValid = false;
			}
		}
		return isValid;
	}
}