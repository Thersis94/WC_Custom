package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.codman.cu.tracking.vo.UnitVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: TransIcpAction.java<p/>
 * <b>Description: Handles the processing of ICP unit refurbishment.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 28, 2014
 ****************************************************************************/
public class ICPExpressTransAction extends AbstractTransAction {

	/**
	 * Default Constructor
	 */
	public ICPExpressTransAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ICPExpressTransAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("incomplete-switch")
	public void build( SMTServletRequest req ) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		//Create the transaction 
		TransactionVO trans = new TransactionVO(req);
		
		try {
			switch (trans.getStatus()) {
				case PENDING:
					super.insertTransaction(trans);
					break;

				case COMPLETE:
					changeTransaction(trans);
					// bind the unit(s) to this Transaction
					UnitLedgerAction ula = new UnitLedgerAction(actionInit);
					ula.setAttributes(attributes);
					ula.setDBConnection(dbConn);
					ula.build(req);
					break;
				
				default: //catches all our SVC_REQ_ Status types
					changeTransaction(trans);
			}
		} catch (SQLException se) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error(se);
		}

		//Send necessary Emails
		sendEmail(req, trans);
		
		//if the unit has been successfully returned; unbind it from the user's account and mark it as available.
		//this has to be done after we send the email, which depends on the unit still being attached to the account.
		if (trans.getStatus() == Status.RTRN_REQ_RCVD) {
			UnitReturnAction ura = new UnitReturnAction(actionInit);
			ura.setAttributes(attributes);
			ura.setDBConnection(dbConn);
			try {
				ura.updateUnitLedger(req.getParameter("unitId"));
			} catch (SQLException sqle) {
				log.error("could not unbind unit from account", sqle);
			}
			//update unit stats
			UnitAction ua = new UnitAction(actionInit);
			ua.setAttributes(attributes);
			ua.setDBConnection(dbConn);
			UnitVO unit = ua.retrieveUnit(req.getParameter("unitId"));
			unit.setStatusId(UnitAction.STATUS_AVAILABLE);
			try {
				ua.saveUnit(unit);
			} catch (SQLException sqle) {
				log.error("could not update unit status after refurbishment", sqle);
			}
			ua = null;
		} else if (trans.getStatus() == Status.SVC_REQ_SENT_EDC) {
			//update the unit to "in-use", since service is now complete
			UnitAction ua = new UnitAction(actionInit);
			ua.setAttributes(attributes);
			ua.setDBConnection(dbConn);
			UnitVO unit = ua.retrieveUnit(req.getParameter("unitId"));
			unit.setStatusId(UnitAction.STATUS_IN_USE);
			try {
				ua.saveUnit(unit);
			} catch (SQLException sqle) {
				log.error("could not update unit status after refurbishment", sqle);
			}
		}

		//Redirect 
		setupRedir(req, msg);
	}
	

	/**
	 * Redirects after the build method is completed
	 * @param req
	 */
	private void setupRedir(SMTServletRequest req, Object msg) {
		StringBuilder url = new StringBuilder(100);
		url.append(req.getRequestURI());
		url.append("?type=").append(req.getParameter("type"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		url.append("&msg=").append(msg);
		log.debug("redirUrl = " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

		
	/**
	 * Changes the status of a 
	 * @param vo
	 */
	private void changeTransaction(TransactionVO vo) throws SQLException{
		log.debug("Changing Transaction Status.");

		//build statement
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("CODMAN_CU_TRANSACTION ");
		//The approving party is the person updating the status of the transaction
		sql.append("set STATUS_ID=?, UPDATE_DT=?, APPROVING_PARTY_NM=?, APPROVAL_DT=?");
		if (vo.getNotesText() != null)
			sql.append(", NOTES_TXT=?");
		
		if (StringUtil.checkVal(vo.getCreditText(), null) != null)
			sql.append(",CREDIT_TXT=?");
		
		sql.append(" where TRANSACTION_ID=?");
		log.debug(sql+" | "+vo.getTransactionId());

		PreparedStatement ps =null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++i, vo.getStatusId());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getApprovorName());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (vo.getNotesText() != null) 
				ps.setString(++i, vo.getNotesText());
			if (StringUtil.checkVal( vo.getCreditText(), null) != null) 
				ps.setString(++i, vo.getCreditText());
			ps.setString(++i, vo.getTransactionId());
			ps.executeUpdate();
		} finally {
			DBUtil.close(ps);
		}
	}

	
	/**
	 * Sends notification email to designated recipients.
	 * @param req
	 * @param trans
	 */
	private void sendEmail(SMTServletRequest req, TransactionVO trans) 
			throws ActionException {
		//get admins
		//List<UserDataVO> adminList = super.retrieveAdministrators(req);

		AccountVO acct = super.retrieveRecord(req, trans);
		//replace the transactionVo with one that's been fully populated from the DB
		trans = acct.getTransactionMap().get(trans.getTransactionId());
		
		//setup mailer
		ICPExpressEmailer mailer = new ICPExpressEmailer(this.actionInit);
		mailer.setAttributes(attributes);
		mailer.setDBConnection(dbConn);
		mailer.sendTransactionMessage(req, trans, acct);
	}
}