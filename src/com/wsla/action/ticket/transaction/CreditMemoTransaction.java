package com.wsla.action.ticket.transaction;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.CreditMemoVO;

/****************************************************************************
 * <b>Title:</b> CreditMemoTransaction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since May 30 2019
 * @updates:
 ****************************************************************************/

public class CreditMemoTransaction extends BaseTransactionAction {

	public CreditMemoTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public CreditMemoTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public CreditMemoTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	/**
	 * Returns whether there are open credit memos for a given ticket
	 * 
	 * @param ticketId
	 * @return
	 */
	public boolean hasUnapprovedCreditMemos(String ticketId) {
		String schema = getCustomSchema();
		
		StringBuilder sql = new StringBuilder(250);
		sql.append(DBUtil.SELECT_CLAUSE).append("trr.ticket_id as key, count(*) as value");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket_ref_rep trr");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_credit_memo cm on trr.ticket_ref_rep_id = cm.ticket_ref_rep_id");
		sql.append(DBUtil.WHERE_CLAUSE).append("trr.ticket_id = ? and cm.approval_dt is null");
		sql.append(DBUtil.GROUP_BY).append("trr.ticket_id");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(ticketId), new GenericVO());
		
		return !data.isEmpty() && (Long) data.get(0).getValue() > 0;
	}
	
	/**
	 * Updates the credit memo after it is approved.
	 * @param cmvo
	 */
	public void saveCreditMemoApproval(CreditMemoVO cmvo) {
		cmvo.setApprovalDate(new Date());
		cmvo.setUpdateDate(new Date());
		
		List<String> fields = Arrays.asList("end_user_refund_flg", "asset_id", "refund_amount_no", "customer_assisted_cd" , "approved_by_txt", "bank_nm", "account_no", "transfer_cd", "approval_dt", "customer_memo_cd", "authorization_dt", "credit_memo_id");
		
		StringBuilder sql = new StringBuilder(93);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_credit_memo ");
		sql.append("set end_user_refund_flg = ?, asset_id = ?, refund_amount_no = ?,customer_assisted_cd = ?, approved_by_txt = ?, bank_nm = ?, account_no = ?, transfer_cd = ?, approval_dt = ?, customer_memo_cd = ?, authorization_dt = ?");
		sql.append(DBUtil.WHERE_CLAUSE).append("credit_memo_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.executeSqlUpdate(sql.toString(), cmvo, fields);
		} catch (DatabaseException e) {
			log.error("could not save credit memo approval",e);
			putModuleData(cmvo, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * saves just the banking info for the credit memo
	 * @param cmvo
	 */
	public void saveBankInfo(CreditMemoVO cmvo) {

		List<String> fields = Arrays.asList("bank_nm", "account_no", "transfer_cd", "end_user_refund_flg", "credit_memo_id");
		
		StringBuilder sql = new StringBuilder(93);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_credit_memo ");
		sql.append("set bank_nm = ?, account_no = ?, transfer_cd = ?, end_user_refund_flg = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("credit_memo_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.executeSqlUpdate(sql.toString(), cmvo, fields);
		} catch (DatabaseException e) {
			log.error("could not save credit memo approval",e);
			putModuleData(cmvo, 0, false, e.getLocalizedMessage(), true);
		}
		
	}
	
	
}
