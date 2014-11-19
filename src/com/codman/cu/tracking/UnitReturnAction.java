package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.codman.cu.tracking.vo.UnitVO;
import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UnitReturnAction.java<p/>
 * <b>Description: facilitates field reps returning unused/old ICP devices for credits, or to be refurbished.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 09, 2014
 ****************************************************************************/
public class UnitReturnAction extends AbstractTransAction {

	public UnitReturnAction() {
		super();
	}

	/**
	 * 
	 * @param arg0
	 */
	public UnitReturnAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {	
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String fromAccountId = StringUtil.checkVal(req.getParameter("accountId"));
		String unitId = StringUtil.checkVal(req.getParameter("unitId"));

		// get some base values from the request.
		TransactionVO tvo = new TransactionVO(req);
		
		//check for unit returns, rather that refurbishments
		if (Convert.formatBoolean(req.getParameter("returnUnit"))) {
			tvo.setStatus(Status.RTRN_REQ);
			tvo.setTransactionTypeId(2); //type=return, instead of refurbish
		}

		try {
			//write "transfer from" record
			tvo.setAccountId(fromAccountId);
			tvo.getPhysician().setPhysicianId(req.getParameter("origPhysicianId"));
			insertTransaction(tvo); 
			
			//update unit stats
			UnitAction ua = new UnitAction(actionInit);
			ua.setAttributes(attributes);
			ua.setDBConnection(dbConn);
			UnitVO unit = ua.retrieveUnit(unitId);
			unit.setStatusId(UnitAction.STATUS_BEING_SERVICED);
			ua.saveUnit(unit);
			ua = null;

			// set activeRecord=0 for all exisitng ledger entries
			this.updateUnitLedger(unitId);

			// write a new ledger entries:
			this.writeUnitLedger(unitId, tvo.getTransactionId(), 1);
			
			//send notification emails
			this.sendNotifications(req, tvo);


		} catch (SQLException sqle) {
			log.error("could not return unit", sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		//build the redirect.
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String url = page.getRequestURI() + "?type=unit&accountId=" + fromAccountId + "&msg=" + msg.toString();
		log.debug("redirUrl=" + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url);

	}


	private void writeUnitLedger(String unitId, String transactionId, int activeFlg) 
			throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(customDb).append("codman_cu_unit_ledger ");
		sql.append("(unit_id, transaction_id, active_record_flg, ");
		sql.append("create_dt, ledger_id) ");
		sql.append("values (?,?,?,?,?) ");
		log.debug(sql);

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, unitId);
			ps.setString(2, transactionId);
			ps.setInt(3, activeFlg);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, new UUIDGenerator().getUUID());
			ps.executeUpdate();
			log.debug("added " + unitId + "/" + transactionId + "/" + activeFlg);
		} catch (SQLException sqle) {
			throw(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}


	/**
	 * Sets all active record flags for a unit to 0.
	 * @param unitId
	 * @throws SQLException
	 */
	private void updateUnitLedger(String unitId) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("update ").append(customDb);
		sql.append("codman_cu_unit_ledger set active_record_flg=0, update_dt=? ");
		sql.append("where unit_id = ? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, unitId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("cleared activeRecordFlag for unit " + unitId);
	}
	
	private void sendNotifications(SMTServletRequest req, TransactionVO tvo) 
			throws ActionException {
		//send a notification email for ICP products only
		if (tvo.getProductType() != ProdType.ICP_EXPRESS) return;
		
		AccountVO acct = super.retrieveRecord(req, tvo);
		//replace the transactionVo with one that's been fully populated from the DB
		tvo = acct.getTransactionMap().get(tvo.getTransactionId());
		
		//get admins
		List<UserDataVO> adminList = super.retrieveAdministrators(req);

		//setup mailer
		ICPExpressEmailer mailer = new ICPExpressEmailer(this.actionInit);
		mailer.setAttributes(attributes);
		mailer.setDBConnection(dbConn);
		mailer.sendTransactionMessage(req, adminList, tvo, acct);
	}
}
