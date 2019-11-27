package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.List;
// JDK 1.8.x
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.WarrantyBillableVO;

/****************************************************************************
 * <b>Title</b>: WarrantyBillableAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> WebCrescendo Action managing the association of billable
 * activities to the specific warranties
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 7, 2018
 * @updates:
 ****************************************************************************/
public class WarrantyBillableAction extends SBActionAdapter {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "warrantyBillable";
	
	/**
	 * 
	 */
	public WarrantyBillableAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public WarrantyBillableAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public WarrantyBillableAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getBillableActivities(req.getParameter("warrantyId")));
	}
	
	/**
	 * Gets the list of billable activities and their costs for a given warranty
	 * @param warrantyId
	 * @return
	 */
	public List<WarrantyBillableVO> getBillableActivities(String warrantyId) {
		if (StringUtil.isEmpty(warrantyId)) return new ArrayList<>();
		
		List<Object> vals = new ArrayList<>();
		vals.add(warrantyId);
		
		StringBuilder sql = new StringBuilder(360);
		sql.append("select a.*, b.cost_no, b.invoice_amount_no, warranty_id, ");
		sql.append("coalesce(warranty_billable_id, replace(newid(), '-', '')) as warranty_billable_id ");
		sql.append("from ").append(getCustomSchema()).append("wsla_billable_activity a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_warranty_billable_xr b "); 
		sql.append("on a.billable_activity_cd = b.billable_activity_cd and warranty_id = ? ");
		sql.append("where active_flg = 1 ");
		sql.append("order by activity_nm ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new WarrantyBillableVO());
	}
	
	/**
	 * 
	 * @param warrantyId
	 * @param activityCode
	 * @return
	 */
	public WarrantyBillableVO getBillableActivity (String warrantyId, String activityCode) {
		if (StringUtil.isEmpty(warrantyId) || StringUtil.isEmpty(activityCode) ) return new WarrantyBillableVO();
		
		List<Object> vals = new ArrayList<>();
		vals.add(activityCode);
		vals.add(warrantyId);
		
		StringBuilder sql = new StringBuilder(154);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_warranty_billable_xr where billable_activity_cd = ? and warranty_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<WarrantyBillableVO> data = db.executeSelect(sql.toString(), vals, new WarrantyBillableVO());
		
		if (data != null && !data.isEmpty() ){
			return data.get(0);
		}else {
			return new WarrantyBillableVO();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		WarrantyBillableVO wbvo = new WarrantyBillableVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Delete the record and then insert.  This is easier than determining the 
		// If the record exists
		try {
			delete(req);
			db.insert(wbvo);
		} catch (Exception e) {
			log.error("Unable to process warranty billiable update", e);
			setModuleData(wbvo, 0, e.getLocalizedMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		WarrantyBillableVO wbvo = new WarrantyBillableVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.delete(wbvo);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException("Unable to delete row", e);
		}
		
	}
}
