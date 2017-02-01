package com.codman.cu.tracking;

import java.sql.PreparedStatement;

import java.sql.SQLException;

import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: TransferFacadeAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 09, 2010
 ****************************************************************************/
public class UnitTransferAction extends SBActionAdapter {
		
	private Object msg = null;
	
	/**
	 * 
	 */
	public UnitTransferAction() {
		super();
	}
	
	/**
	 * 
	 * @param arg0
	 */
	public UnitTransferAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {			
		String fromAccountId = StringUtil.checkVal(req.getParameter("accountId"));
		String toAccountId = StringUtil.checkVal(req.getParameter("toAccountId"));
		String unitId = StringUtil.checkVal(req.getParameter("unitId"));
		
		// get some base values from the request.
		TransactionVO tvo = new TransactionVO(req);
		
		try {
			//write "transfer from" record
			tvo.setAccountId(fromAccountId);
			tvo.getPhysician().setPhysicianId(req.getParameter("origPhysicianId"));
			String fromAcctTransId = this.writeTransaction(req, tvo);
			
			
			//write "transfer to" record
			tvo.setAccountId(toAccountId);
			tvo.getPhysician().setPhysicianId(req.getParameter("physicianId"));
			String toAcctTransId = this.writeTransaction(req, tvo);
			
			
			// set activeRecord=0 for all exisitng ledger entries
			this.updateUnitLedger(unitId);
		
			
			// write a new ledger entries:
			// "transfer from" & "transfer to" associatives to the unit being transferred
			this.writeUnitLedger(unitId, fromAcctTransId, 0);
			this.writeUnitLedger(unitId, toAcctTransId, 1);
			
			
		} catch (SQLException sqle) {
			log.error("could not transfer unit", sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		if (msg == null) msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		mod.setErrorMessage(msg.toString());
		
		//build the redirect.
		String url = req.getRequestURI() + "?msg=" + msg.toString();
	   	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url);
    	log.debug("redirUrl=" + url);
		
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
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Inserts a transaction record for the given accountId
	 * @param req
	 * @param tvo
	 * @param transIds
	 * @param accountId
	 */
	private String writeTransaction(ActionRequest req, TransactionVO tvo) throws SQLException {
		tvo.setTransactionId(new UUIDGenerator().getUUID());
		log.debug("trans pkId=" + tvo.getTransactionId());
		
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(customDb).append("codman_cu_transaction ");
		sql.append("(transaction_type_id, status_id, account_id, approval_dt, approving_party_nm, ");
		sql.append("requesting_party_nm, notes_txt, create_dt, update_dt, physician_id, transaction_id) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		
		log.debug(sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, tvo.getTransactionTypeId());
			ps.setInt(2, tvo.getStatusId());
			ps.setString(3, tvo.getAccountId());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, tvo.getApprovorName());
			ps.setString(6, tvo.getRequestorName());
			ps.setString(7, tvo.getNotesText());
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setTimestamp(9, Convert.getCurrentTimestamp());
			ps.setString(10, tvo.getPhysician().getPhysicianId());
			ps.setString(11, tvo.getTransactionId());
			ps.executeUpdate();
			log.debug("added " + tvo.getAccountId() + "/" + tvo.getTransactionId());
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return tvo.getTransactionId();
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
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("cleared activeRecordFlag for unit " + unitId);
	}
}
