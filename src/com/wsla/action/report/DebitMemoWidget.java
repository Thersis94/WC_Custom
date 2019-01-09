package com.wsla.action.report;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.DebitMemoVO;

/****************************************************************************
 * <b>Title</b>: DebitMemoWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Debit Memo Report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 8, 2019
 * @updates:
 ****************************************************************************/

public class DebitMemoWidget extends SBActionAdapter {
	/**
	 * Ajax key for the main controller to use to call this action
	 */
	public static final String AJAX_KEY = "debitMemo";
	
	/**
	 * 
	 */
	public DebitMemoWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DebitMemoWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String oemId = req.getParameter("oemId");
		String retailerId = req.getParameter("retailerId");
		String complete = req.getParameter("complete");
		setModuleData(getDebitMemos(new BSTableControlVO(req, DebitMemoVO.class), oemId, retailerId, complete));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Saving .... ");
		
		DebitMemoVO dm = new DebitMemoVO(req);
		log.info("Approval Date: " + dm.getApprovalDate());
		// Set the approval date if empty and approved by has been passed
		if (dm.getApprovalDate() == null && ! StringUtil.isEmpty(dm.getApprovedBy())) {
			dm.setApprovalDate(new Date());
		}
	}
	
	/**
	 * Gets the list of Debit Memos
	 * @param bst
	 * @return
	 */
	public GridDataVO<DebitMemoVO> getDebitMemos(BSTableControlVO bst, String oemId, String retId, String complete) {
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(896);
		sql.append("select retailer_nm, provider_nm as oem_nm, cm.*, dm.* from ");
		sql.append(getCustomSchema()).append("wsla_debit_memo dm ");
		sql.append(DBUtil.INNER_JOIN).append(" ( select debit_memo_id, ");
		sql.append("sum(a.refund_amount_no) as total_credit_memo, ");
		sql.append("count(*) as num_credit_memos, oem_id, retailer_id ");
		sql.append("from ").append(getCustomSchema()).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append("group by debit_memo_id, oem_id, retailer_id ");
		sql.append(") as cm on dm.debit_memo_id = cm.debit_memo_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider p on cm.oem_id = p.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append("( select location_id, ");
		sql.append("provider_nm as retailer_nm from ");
		sql.append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider_location b on a.provider_id = b.provider_id ");
		sql.append(") as r on cm.retailer_id = r.location_id where 1=1 ");
		
		// Add the oem Filter
		if (! StringUtil.isEmpty(oemId)) {
			params.add(oemId);
			sql.append("and dm.oem_id = ? ");
		}
		
		// Add the Retailer Filter
		if (! StringUtil.isEmpty(retId)) {
			params.add(retId);
			sql.append("and dm.retail_id = ? ");
		}
		
		// Add the Retailer Filter
		if (! StringUtil.isEmpty(complete)) {
			if (Convert.formatBoolean(complete))
				sql.append("and transfer_dt is not null ");
			else 
				sql.append("and transfer_dt is null ");
		}

		// Add the text search
		if (bst.hasSearch()) {
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			sql.append("and (lower(dm.debit_memo_id) like ? ");
			sql.append(" or lower(customer_memo_cd) like ? ");
			sql.append(" or lower(transfer_no_txt) like ?) ");
		}
		
		sql.append(bst.getSQLOrderBy("create_dt", "desc"));
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new DebitMemoVO(), bst);
	}
}

