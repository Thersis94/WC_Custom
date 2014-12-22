package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AbstractTransactionAction.java<p/>
 * <b>Description: Action that manages a unit transaction. 
 * Some of the original CU transaction methods can be used in
 * other unit types. Those were pulled out and put here to avoid repetition
 * in those other classes. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Oct 29, 2014
 ****************************************************************************/
public abstract class AbstractTransAction extends SBActionAdapter {

	/**
	 * Default Constructor
	 */
	public AbstractTransAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AbstractTransAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Possible statuses for an ICP transaction.
	 */
	public static enum Status {
		//for ICP refurbishments
		SVC_REQ(210,"Return for Service", false, true),
		SVC_REQ_RCVD(220,"Received for Service", false, true),
		SVC_REQ_COMPL(230,"Service Complete", false, true),
		SVC_REQ_SENT_EDC(240,"Sent Back to EDC", false, true),
		SVC_REQ_SENT_REP(250,"Sent Back to Rep", false, false),
		//for ICP returns
		RTRN_REQ(260, "Return for Refurbishment", false, true),
		RTRN_REQ_RCVD(270, "Refurbishment Complete", false, false),
		
		//new unit transactions & Medstream transfers
		PENDING(10, "Pending", true, true),
		APPROVED(20, "Approved", false, true),
		COMPLETE(30, "Completed", false, false),
		DECLINED(50, "Declined", false, false);
		
		private final int code;
		private final String name;
		private boolean reqApproval, isActionable;
		Status(int code, String name, boolean reqApproval, boolean isActionable) {
			this.code = code;
			this.name = name;
			this.reqApproval = reqApproval;
			this.isActionable = isActionable;
		}
		public String getStatusName() { return name; }
		public int getStatusCode() { return code; }
		public boolean reqApproval() { return reqApproval; }
		public boolean isActionable() { return isActionable; }
	}
	
	
	/**
	 * Legacy helper method - may only be used in RequestSearchVO -JM 11.17.14
	 * @param id
	 * @return
	 */
	public static String getStatusName(int id) {
		for (Status s : Status.values()) {
			if (s.getStatusCode() == id) return s.getStatusName();
		}
		return null;
	}
	
	/**
	 * Retrieves admin 
	 * @param req
	 * @throws ActionException
	 */
	protected List<UserDataVO> retrieveAdministrators(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserAction ua = new UserAction(this.actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		List<UserDataVO> admins = ua.loadUserList(SecurityController.ADMIN_ROLE_LEVEL, site.getOrganizationId());
		ua = null;
		return admins;
	}
	
	/**
	 * Retrieves all the users with the given roleLevel
	 * @param req
	 * @param roleLevel The role level requested.
	 * @return
	 * @throws ActionException
	 */
	protected List<UserDataVO> retrieveUsers(SMTServletRequest req, Integer roleLevel) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserAction ua = new UserAction(this.actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		List<UserDataVO> users = ua.loadUserList(roleLevel, site.getOrganizationId());
		ua = null;
		return users;
	}
	
	/**
	 * retrieves the entire account record, filtered to this transaction, rep, and physician
	 * @param req
	 * @param trans
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected AccountVO retrieveRecord(SMTServletRequest req, TransactionVO trans)
	 throws ActionException {
		
		log.debug("trans=" + trans.toString());
		req.setParameter("transactionId", trans.getTransactionId());
		req.setParameter("physicianId", trans.getPhysician().getPhysicianId());
		req.setParameter("accountId", trans.getAccountId());
		req.setParameter("unfiltered", "true"); //this bypasses all search filtering that normally would apply from the SearchVO stored on session
		
		SMTActionInterface ai = new AccountFacadeAction(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		
		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<AccountVO> accounts = (List<AccountVO>) modVo.getActionData();
		AccountVO vo = null;
		try {
			vo = accounts.get(0);
		} catch (Exception e) {
			log.error("could not retrieve transaction for emailing", e);
			vo = new AccountVO();
		}
		
		return vo;
	}
	
	
	/**
	 * Inserts an entry into the transaction table for this unit
	 * @param vo
	 */
	protected void insertTransaction( TransactionVO vo ) throws SQLException{
		log.debug("Creating new transaction.");
		
		if (vo.getCreateDate() == null)
			vo.setCreateDate(Convert.getCurrentTimestamp());

		//generate the insert statement
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("codman_cu_transaction ");
		sql.append("(transaction_type_id, status_id, account_id, physician_id, ");
		sql.append("unit_cnt_no, dropship_flg, address_txt, address2_txt, ");
		sql.append("city_nm, state_cd, zip_cd, country_cd, requesting_party_nm, ");
		sql.append("ship_to_nm, notes_txt, create_dt, transaction_id, ");
		sql.append("approving_party_nm, credit_txt, PRODUCT_CD) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		
		vo.setTransactionId(new UUIDGenerator().getUUID());
		log.debug(sql+"|"+vo.getTransactionId());
		log.debug(vo);
		log.debug(vo.getProductType());

		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++i, vo.getTransactionTypeId());
			ps.setString(++i, String.valueOf(vo.getStatusId()));
			ps.setString(++i, vo.getAccountId());
			ps.setString(++i, vo.getPhysician().getPhysicianId());
			ps.setInt(++i, vo.getUnitCount());
			ps.setInt(++i, vo.getDropShipFlag());
			ps.setString(++i, vo.getDropShipAddress().getAddress());
			ps.setString(++i, vo.getDropShipAddress().getAddress2());
			ps.setString(++i, vo.getDropShipAddress().getCity());
			ps.setString(++i, vo.getDropShipAddress().getState());
			ps.setString(++i, vo.getDropShipAddress().getZipCode());
			ps.setString(++i, vo.getDropShipAddress().getCountry());
			ps.setString(++i, vo.getRequestorName());
			ps.setString(++i, vo.getShipToName());
			ps.setString(++i, vo.getNotesText());
			ps.setTimestamp(++i, Convert.formatTimestamp(vo.getCreateDate()));
			ps.setString(++i, vo.getTransactionId());
			ps.setString(++i, vo.getApprovorName());
			ps.setString(++i, vo.getCreditText());
			ps.setString(++i, vo.getProductType().name());
			ps.executeUpdate();
		} finally {
			DBUtil.close(ps);
		}
	}
}