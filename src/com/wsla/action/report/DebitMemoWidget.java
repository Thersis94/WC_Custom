package com.wsla.action.report;

// JDK 1.8.x
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.DebitMemoVO;
import com.wsla.data.ticket.UserVO;

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
		if (req.getBooleanParameter("listcm")) {
			setModuleData(getCreditMemos(req.getParameter("debitMemoId")));
		} else {
			String oemId = req.getParameter("oemId");
			String retailerId = req.getParameter("retailerId");
			String complete = req.getParameter("complete");
			String dmType = req.getParameter("dmType");
			setModuleData(getDebitMemos(new BSTableControlVO(req, DebitMemoVO.class), oemId, retailerId, complete, dmType));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Load the debit memo
		DebitMemoVO dm = new DebitMemoVO(req);
		dm.assignTransferDate();
		dm.assignApprovalDate();
		
		// Save the data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(dm);
			putModuleData(dm);
		} catch (Exception e) {
			log.error("Unable to save Debit Memo", e);
			putModuleData(dm, 1, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * 
	 * @param dmid
	 * @return
	 */
	public List<CreditMemoVO> getCreditMemos(String dmid) {
		List<Object> params = new ArrayList<>();
		params.add(dmid);
		
		StringBuilder sql = new StringBuilder(376);
		sql.append("select d.value_txt as file_path_url, c.ticket_no, a.* ");
		sql.append("from ").append(getCustomSchema()).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_data d on a.asset_id = d.data_entry_id ");
		sql.append("where debit_memo_id = ? ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), params, new CreditMemoVO());
	}
	
	/**
	 * Gets the list of Debit Memos
	 * @param bst
	 * @return
	 */
	public GridDataVO<DebitMemoVO> getDebitMemos(BSTableControlVO bst, String oemId, String retId, String complete, String dmType) {
		List<Object> params = new ArrayList<>();
		if (! StringUtil.isEmpty(bst.getSearch())) bst.setSearch(bst.getSearch().toLowerCase());
		
		StringBuilder sql = new StringBuilder(896);
		sql.append("select coalesce(first_nm || ' ' || last_nm, r.provider_nm) as retailer_nm, ");
		sql.append("o.provider_nm as oem_nm, dm.* from ");
		sql.append(getCustomSchema()).append("wsla_debit_memo dm ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider o on dm.oem_id = o.provider_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider r on dm.retail_id = r.provider_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_user u on dm.user_id = u.user_id ");
		
		sql.append("where 1=1 ");
		// Add the oem Filter
		if (! StringUtil.isEmpty(oemId)) {
			params.add(oemId);
			sql.append("and dm.oem_id = ? ");
		}
		
		// Add the debit memo type Filter
		if (! StringUtil.isEmpty(dmType)) {
			if ("user".equalsIgnoreCase(dmType))
				sql.append("and dm.user_id is not null ");
			if ("retailer".equalsIgnoreCase(dmType))
				sql.append("and dm.user_id is null ");
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
			sql.append("and dm.debit_memo_id in (select dm.debit_memo_id from ");
			sql.append(getCustomSchema()).append("wsla_debit_memo dm ");
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
			sql.append("wsla_credit_memo cm on dm.debit_memo_id = cm.debit_memo_id ");
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
			sql.append("wsla_ticket_ref_rep rr on cm.ticket_ref_rep_id = rr.ticket_ref_rep_id ");
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
			sql.append("wsla_ticket t on rr.ticket_id = t.ticket_id ");
			sql.append("where lower(dm.debit_memo_id) like ? ");
			sql.append("or lower(cm.customer_memo_cd) like ? ");
			sql.append("or lower(dm.customer_memo_cd) like ? ");
			sql.append("or lower(ticket_no) like ? ");
			sql.append("or lower(credit_memo_id) like ? ");
			sql.append("or lower(transfer_no_txt) like ? ");
			sql.append("group by dm.debit_memo_id ) ");
			
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());

		}
		
		sql.append(bst.getSQLOrderBy("create_dt", "desc"));
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new DebitMemoVO(), bst);
	}
}

