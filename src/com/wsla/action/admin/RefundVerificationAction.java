package com.wsla.action.admin;

// JDk 1.8.x
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.admin.RefundVerificationVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: RefundVerificationAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the list data for the users that have been
 * authorized to receive a refund, but have mot been verified by WSLA that they
 * have actually received the data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 29, 2019
 * @updates:
 ****************************************************************************/
public class RefundVerificationAction extends BaseTransactionAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "refund_ver";
	
	
	public static final String REFUND_ATTR_KEY = "attr_userRefundPaid";
	/**
	 * 
	 */
	public RefundVerificationAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RefundVerificationAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getUnverifiedRefundUsers(new BSTableControlVO(req)));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketDataVO tdvo = new TicketDataVO(req);
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		try {
			// Delete existing ticket data
			if (! StringUtil.isEmpty(req.getParameter("dataEntryId"))) {
				db.delete(tdvo);
			}
			
			// Insert new record
			addLedger(tdvo.getTicketId(), user.getUserId(), null, LedgerSummary.REFUND_VERIFIED.summary, null);
			db.insert(tdvo);
			setModuleData(tdvo);
			
		} catch (Exception e) {
			setModuleData(tdvo, 1, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Retrieves the data for the list of unverified users
	 * @param bst
	 * @return
	 */
	public GridDataVO<RefundVerificationVO> getUnverifiedRefundUsers(BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(864);
		List<Object> vals = new ArrayList<>();
		
		sql.append("select d.ticket_id, ticket_no, first_nm, last_nm, e.main_phone_txt, ");
		sql.append("b.refund_amount_no, b.approval_dt, e.email_address_txt, data_entry_id ");
		sql.append("from ").append(getCustomSchema()).append("wsla_debit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_credit_memo b ");
		sql.append("on a.debit_memo_id = b.debit_memo_id ");
		sql.append("inner join ").append(getCustomSchema()).append("wsla_ticket_ref_rep c ");
		sql.append("on b.ticket_ref_rep_id = c.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket d ");
		sql.append("on c.ticket_id = d.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user e ");
		sql.append("on d.originator_user_id = e.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_ticket_data f  ");
		sql.append("on d.ticket_id = f.ticket_id and f.attribute_cd = '").append(REFUND_ATTR_KEY).append("' ");
		sql.append("where b.approval_dt is not null and (f.value_txt is null or f.value_txt = '0') ");
		
		// Add the search filters
		if (bst.hasSearch()) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ? or lower(ticket_no) like ? ");
			sql.append("or lower(email_address_txt) like ? or main_phone_txt like ? ) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append("order by b.approval_dt desc, last_nm, first_nm ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new RefundVerificationVO(), bst);
	}
}
