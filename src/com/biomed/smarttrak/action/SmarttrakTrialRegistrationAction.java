package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.biomed.smarttrak.admin.AccountUserAction;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
<p><b>Title</b>: SmarttrakTrialRegistrationAction.java</p>
<p><b>Description: Handle the </b></p>
<p>Copyright: (c) 2000 - 2020 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Eric Damschroder
@version 1.0
@since March 23, 2020
<b>Changes:</b> 
***************************************************************************/

public class SmarttrakTrialRegistrationAction extends SBActionAdapter {
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		RegistrationFacadeAction rfa = ActionControllerFactoryImpl.loadAction(RegistrationFacadeAction.class, this);
		actionInit.setActionId(AdminControllerAction.REGISTRATION_GRP_ID);
		rfa.setActionInit(actionInit);
		rfa.setDBConnection(dbConn);
		rfa.retrieve(req);
	}
	
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (!req.hasParameter("profileId") && (!req.hasParameter("emailAddress") || !checkTrialPermissions(req))) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + 
					(Convert.formatBoolean(req.getParameter("oneFail"))? "?secondFailure=true":"?invalidEmail=true"));
			return;
		}
		
		if (!req.hasParameter("profileId") && checkUserExists(req)) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + "?alreadyExists=true");
			return;
		}

		if (!Convert.formatBoolean(req.getParameter("stepTwo"))) {
			AccountUserAction acc = new AccountUserAction();
			acc.setActionInit(actionInit);
			acc.setAttributes(attributes);
			acc.setDBConnection(dbConn);
			acc.build(req);
			req.setParameter("loginAs", req.getParameter("profileId"));
			acc.retrieve(req);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + "?hideMenu=true");
		} else {
			SmarttrakRegistrationAction reg = new SmarttrakRegistrationAction();
			reg.setActionInit(actionInit);
			reg.setAttributes(attributes);
			reg.setDBConnection(dbConn);
			reg.build(req);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, "/");
		}
	}
	
	
	/**
	 * Check to see if the user already has an account
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private boolean checkUserExists(ActionRequest req) throws ActionException {
		StringBuilder sql =  new StringBuilder(150);
		sql.append("select user_id from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_user ");
		sql.append("where account_id = ? and email_address_txt = ? and active_flg = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, req.getParameter("accountId"));
			ps.setString(2, req.getParameter("emailAddress"));
			ps.setInt(3, 1);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next())
				return true;
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		return false;
	}


	/**
	 * Check to make sure that the user's email address meets requirements for a trial and
	 * load all account specific trial information to the request object.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private boolean checkTrialPermissions (ActionRequest req) throws ActionException {
		String email = req.getParameter("emailAddress");
		if (email.indexOf('@') == -1) return false;
		
		StringBuilder sql = new StringBuilder(300);
		sql.append("select account_id, trial_expiration_dt, campaign_title_txt, type_id from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_account ");
		sql.append("where domains_txt like ? and trial_expiration_dt > current_timestamp ");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "%"+email.substring(email.indexOf('@'))+"%");
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				DateFormat formatter= new SimpleDateFormat(Convert.DATE_SLASH_PATTERN);
				req.setParameter("accountId", rs.getString("account_id"));
				req.setParameter("expirationDate", formatter.format(rs.getDate("trial_expiration_dt").getTime()));
				req.setParameter("entrySource", rs.getString("campaign_title_txt"));
				req.setParameter("statusFlg", "1");
				req.setParameter("licenseType", rs.getInt("type_id") == 3? "U":"K");
				req.setParameter("passProfile", "true");
				return true;
			}
		} catch (SQLException e) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, req.getRequestURI() + "?msg=Failed to load company trial status.");
			throw new ActionException(e);
		}
		
		return false;
	}

}
