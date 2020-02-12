package com.tricumed.cu.tracking;
//java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;

//SMT Base Lib
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
//WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.tricumed.cu.tracking.vo.TransactionVO;

/****************************************************************************
 * <b>Title</b>: UnitTransferAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Dave Bargerhuff
 * @version 3.0
 * @since Nov 09, 2010 
 * @updates:
 * rjr code clean up May 29, 2017
 ****************************************************************************/

public class UnitTransferAction extends SBActionAdapter {

	private Object msg = null;

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

	/* 
	 * (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
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
			String fromAcctTransId = writeTransaction(tvo);


			//write "transfer to" record
			tvo.setAccountId(toAccountId);
			tvo.getPhysician().setPhysicianId(req.getParameter("physicianId"));
			String toAcctTransId = writeTransaction(tvo);


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
		StringBuilder sql = new StringBuilder(250);
		sql.append("insert into ").append(customDb).append("tricumed_cu_unit_ledger ");
		sql.append("(unit_id, transaction_id, active_record_flg, ");
		sql.append("create_dt, ledger_id) ");
		sql.append("values (?,?,?,?,?) ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
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
		}
	}

	/**
	 * Inserts a transaction record for the given accountId
	 * @param req
	 * @param tvo
	 * @param transIds
	 * @param accountId
	 */
	private String writeTransaction(TransactionVO tvo) throws SQLException {
		tvo.setTransactionId(new UUIDGenerator().getUUID());
		log.debug("trans pkId=" + tvo.getTransactionId());

		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(250);
		sql.append("insert into ").append(customDb).append("tricumed_cu_transaction ");
		sql.append("(transaction_type_id, status_id, account_id, approval_dt, approving_party_nm, ");
		sql.append("requesting_party_nm, notes_txt, create_dt, update_dt, physician_id, transaction_id) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
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
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append(customDb);
		sql.append("tricumed_cu_unit_ledger set active_record_flg=0, update_dt=? ");
		sql.append("where unit_id = ? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, unitId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw new SQLException(sqle);
		}
		log.debug("cleared activeRecordFlag for unit " + unitId);
	}
}
