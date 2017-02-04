package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.codman.cu.tracking.vo.AccountVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AccountAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 02, 2010
 * @updates
 * James McKain - 03-04-2014 - Added "delete account" support, from build method.
 * 		split build method into separate methods for update and delete.
 ****************************************************************************/
public class AccountAction extends SBActionAdapter {
		
	public AccountAction() {
		super();
	}
	
	public AccountAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		Object msg = null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		AccountVO avo = new AccountVO(req);
		
		if (req.hasParameter("deleteAccount")) {
			msg = deleteAccount(avo, customDb);
			
		} else {
			// get the personId attached to the incoming profileId
			UserAction ua = new UserAction(actionInit);
			ua.setAttributes(attributes);
			ua.setDBConnection(dbConn);
			if (StringUtil.checkVal(avo.getRep().getPersonId()).length() == 0)
				avo.getRep().setPersonId(ua.lookupPersonId(req.getParameter("profileId"), site.getOrganizationId()));
			ua = null;
			
			msg = updateAccount(avo, customDb);
		}
		
		// Setup the redirect
		StringBuilder url = new StringBuilder(50);
		url.append(req.getRequestURI());
		url.append("?type=").append(req.getParameter("type"));
		url.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		log.debug("redirUrl = " + url);
	}
	
	
	
	/**
	 * deletes an account from the system
	 * foreign key constraints cascade this through the DB.
	 * @param avo
	 * @param customDb
	 * @return
	 */
	private Object deleteAccount(AccountVO avo, String customDb) {
		Object msg = null;
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(customDb).append("codman_cu_account ");
		sql.append("where account_id=?");
		log.debug(sql + " " + avo.getAccountId());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, avo.getAccountId());
			int cnt = ps.executeUpdate();

			if (cnt > 0) msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			else msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			
		} catch (SQLException sqle) {
			log.error("could not delete account", sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return msg;
	}
	
	
	/**
	 * updates an account record in the database
	 * @param avo
	 * @param customDb
	 * @return
	 * @throws SQLException
	 */
	private Object updateAccount(AccountVO avo, String customDb) {
		Object msg = null;
		StringBuilder sql = new StringBuilder();
		if (StringUtil.checkVal(avo.getAccountId()).length() == 0) {
			avo.setAccountId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(customDb).append("codman_cu_account ");
			sql.append("(person_id, account_no, account_nm, phone_no_txt, ");
			sql.append("address_txt, address2_txt, city_nm, state_cd, zip_cd, ");
			sql.append("country_cd, create_dt, organization_id, product_cd, account_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(customDb).append("codman_cu_account ");
			sql.append("set person_id = ?, account_no = ?, account_nm = ?, phone_no_txt = ?, ");
			sql.append("address_txt = ?, address2_txt = ?, city_nm = ?, state_cd = ?, zip_cd = ?, ");
			sql.append("country_cd = ?, update_dt = ?, organization_id=?, product_cd=? where account_id = ?");
		}
		log.debug(sql + " " + avo.getAccountId());
		log.debug(StringUtil.getToString(avo));

		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, avo.getRep().getPersonId());
			ps.setString(++i, avo.getAccountNo());
			ps.setString(++i, avo.getAccountName());
			ps.setString(++i, avo.getAccountPhoneNumber());
			ps.setString(++i, avo.getAccountAddress());
			ps.setString(++i, avo.getAccountAddress2());
			ps.setString(++i, avo.getAccountCity());
			ps.setString(++i, avo.getAccountState());
			ps.setString(++i, avo.getAccountZipCode());
			ps.setString(++i, avo.getAccountCountry());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, avo.getOrganizationId());
			ps.setString(++i, avo.getProductCd());
			ps.setString(++i, avo.getAccountId());
			int cnt = ps.executeUpdate();

			if (cnt > 0)
				msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			else
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);

		} catch (SQLException sqle) {
			log.error("error saving Account", sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	
		return msg;
	}
}