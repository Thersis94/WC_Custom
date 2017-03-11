package com.biomed.smarttrak.admin;

// JDK 1.8
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.AuditLogVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/********************************************************************
 * <b>Title: </b>ProfileAuditAction.java<br/>
 * <b>Description: </b>Manages company profile auditing.<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author Tim Johnson
 * @since Mar 10, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class ProfileAuditAction extends SBActionAdapter {
	
	// Ensures what is passed in, either maps to something known, or else we will use a default
	private Map<String, String> sortFields;
	private Map<String, String> sortOrders;
	
	/**
	 * N = "Not Started", P = "In Progress", D = "Company Done",
	 * F = "Company and Products Done", C = "Canceled"
	 */
	private enum AuditStatus {N, P, D, F, C}
	
	/**
	 * Set the list of fields that can be sorted on
	 */
	public ProfileAuditAction() {
		super();
		sortFields = new HashMap<>();
		sortFields.put("companyNm", "company_nm");
		sortFields.put("companyStatusNo", "status_no");
		sortFields.put("companyUpdateDt", "company_update_dt");
		sortFields.put("startDt", "start_dt");
		sortFields.put("completeDt", "complete_dt");
		sortFields.put("statusCd", "status_cd");
		sortFields.put("auditorProfileId", "auditor_profile_id");
		
		sortOrders = new HashMap<>();
		sortOrders.put("asc", "asc");
		sortOrders.put("desc", "desc");
	}

	/**
	 * @param actionInit
	 */
	public ProfileAuditAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		if (req.hasParameter("json")) {
			getData(req, custom);
		} else {
			getManagers(req, custom);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
	}
	
	/**
	 * Gets the list of managers for the dropdowns in the table
	 * 
	 * @param req
	 * @param custom
	 * @throws ActionException
	 */
	private void getManagers(ActionRequest req, String custom) throws ActionException {
		AccountAction acctAct = new AccountAction(this.actionInit);
		acctAct.setAttributes(this.attributes);
		acctAct.setDBConnection(dbConn);
		acctAct.loadManagerList(req, custom);
	}
	
	/**
	 * Gets the audit log data, including pagination count.
	 * 
	 * @param req
	 * @param custom
	 */
	public void getData(ActionRequest req, String custom) {
		List<Object> data = new ArrayList<>();
		int recordCount = 0;
		
		try {
			data = getAuditLogRecords(req, custom);
			
			// Gets the count for the pagination
			//recordCount = getAuditLogRecordCount(req, custom);
			
		} catch(Exception e) {
			log.error("Unable to get audit log records.", e);
		}
		
		// Return the data
		putModuleData(data, recordCount, false);
	}
	
	/**
	 * Gets the audit log records.
	 * 
	 * @param req
	 * @param custom
	 * @return
	 * @throws SQLException
	 */
	protected List<Object> getAuditLogRecords(ActionRequest req, String custom) throws SQLException {
		// Pagination/search fields
		String searchTerm = StringUtil.checkVal(req.getParameter("search")).toUpperCase();
		String sortField = StringUtil.checkVal(sortFields.get(req.getParameter("sort")), "company_nm");
		String order = StringUtil.checkVal(sortOrders.get(req.getParameter("order")), "asc");
		int start = Convert.formatInteger(req.getParameter("offset"), 0);
		int rpp = Convert.formatInteger(req.getParameter("limit"), 10);
		
		// Get the sql
		boolean searchFlg = searchTerm.length() > 0;
		String sql = getAuditLogSql(custom, searchFlg, sortField, order);
		
		// Set the parameters
		List<Object> params = new ArrayList<>();
		if (searchFlg) {
			params.add("%" + searchTerm + "%");
		}
		params.add(rpp);
		params.add(start);
		
		// Get the audit records
		DBProcessor dbp = new DBProcessor(dbConn);
		List<Object> data = dbp.executeSelect(sql, params, new AuditLogVO());
		
		return data;
	}
	
	/**
	 * Creates the sql for getting the audit log data
	 * 
	 * @param custom
	 * @param searchFlg
	 * @param sortField
	 * @param order
	 * @return
	 */
	protected String getAuditLogSql(String custom, boolean searchFlg, String sortField, String order) {
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("select c.company_nm, c.company_id, c.status_no, c.update_dt as company_update_dt, coalesce(al.audit_log_id, newid()) as audit_log_id, ");
		sql.append("al.auditor_profile_id, al.status_cd, al.start_dt, al.complete_dt, al.update_dt ");
		sql.append("from ").append(custom).append("biomedgps_company c ");
		sql.append("left join ").append(custom).append("biomedgps_audit_log al on c.company_id = al.company_id ");
		
		if (searchFlg)
			sql.append("where upper(company_nm) like ? ");
		
		sql.append("order by ").append(sortField).append(" ").append(order).append(" ");
		sql.append("limit ? offset ? ");		
		
		log.debug(sql.toString());
		
		return sql.toString();
	}
	
	/**
	 * Gets the record count for pagination
	 * 
	 * @param req
	 * @param custom
	 * @return
	 * @throws SQLException
	 */
	protected int getAuditLogRecordCount(ActionRequest req, String custom) throws SQLException {
		int count = 0;
		return count;
	}

}

