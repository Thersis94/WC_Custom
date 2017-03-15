package com.biomed.smarttrak.admin;

//JDK 1.8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
public class AuditLogAction extends SBActionAdapter {
	
	// Ensures that what is passed in, will either map to something known, or else we will use a default
	private Map<String, String> sortFields;
	private Map<String, String> sortOrders;
	
	/**
	 * Audit statuses
	 */
	public enum AuditStatus {
		N("Not Started"), P("In Progress"), D("Company Done"),
		F("Company and Products Done"), C("Canceled");
		
		private String title;
		
		AuditStatus(String title) {
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
	}
	
	/**
	 * Set the list of fields that can be sorted on
	 */
	public AuditLogAction() {
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
	public AuditLogAction(ActionInitVO actionInit) {
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
			getAuditData(req, custom);
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
		log.debug("Adding/Updating Audit Log Record");
		
		AuditLogVO auditLogRecord = new AuditLogVO(req);
		
		if (!req.hasParameter("auditLogId")) {
			// When adding a new record, set the required defaults
			auditLogRecord.setStatusCd(AuditStatus.N);
			auditLogRecord.setAuditorProfileId("");
			
		} else if (AuditStatus.F == auditLogRecord.getStatusCd()) {
			// If setting to the completed state, track the date
			auditLogRecord.setCompleteDt(new Date());
		}
		
		// Save the new or updated record
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		DBProcessor dbp = new DBProcessor(dbConn, custom);
		try {
			dbp.save(auditLogRecord);
		} catch (Exception e) {
			throw new ActionException("Couldn't save audit log record.", e);
		}
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
	public void getAuditData(ActionRequest req, String custom) {
		log.debug("Getting Audit Log Records");
		
		List<Object> data = new ArrayList<>();
		int recordCount = 0;
		
		try {
			// Get the data
			data = getAuditLogRecords(req, custom);
			
			// Get the count for the pagination
			recordCount = getAuditLogRecordCount(req, custom);
			
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
		DBProcessor dbp = new DBProcessor(dbConn, custom);
		return dbp.executeSelect(sql, params, new AuditLogVO());
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
		StringBuilder sql = new StringBuilder(600);
		
		sql.append("select c.company_nm, c.company_id, c.status_no, c.update_dt as company_update_dt, coalesce(al.audit_log_id, newid()) as audit_log_id, ");
		sql.append("al.auditor_profile_id, al.status_cd, al.start_dt, al.complete_dt, al.update_dt ");
		sql.append("from ").append(custom).append("biomedgps_company c ");
		
		// This join ensures we only get the most recent record from the log 
		sql.append("left join (select company_id, max(start_dt) as max_start_dt ");
		sql.append("from ").append(custom).append("biomedgps_audit_log group by company_id) alm on c.company_id = alm.company_id ");
		
		// Get the entirety of the data associated to the most recent audit log record 
		sql.append("left join ").append(custom).append("biomedgps_audit_log al on alm.company_id = al.company_id and alm.max_start_dt = al.start_dt ");
		
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
		int recordCount = 0;
		
		// Search is the only thing that could change the overall number of results
		String searchTerm = StringUtil.checkVal(req.getParameter("search")).toUpperCase();
		boolean searchFlg = searchTerm.length() > 0;
		
		// Since the main query uses left joins, we are only concerned about companies
		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*) from ").append(custom).append("biomedgps_company ");
		
		if (searchFlg)
			sql.append("where upper(company_nm) like ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (searchFlg) {
				ps.setString(1, "%" + searchTerm + "%");
			}
			
			ResultSet rs = ps.executeQuery();
			rs.next();
			recordCount = rs.getInt(1);
		}
		
		return recordCount;
	}

}

