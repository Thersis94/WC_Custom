package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.action.data.ORKitVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.report.vo.KitExcelReport;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>CaseSearchAction.java<p/>
 * <b>Description: Handles case search functionality for the ram site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 * 		June 30, 2017 - Refactored, pulled out of ProductCartAction
 ****************************************************************************/

public class CaseSearchAction extends SimpleActionAdapter {

	// Names for the request parameters related to this action
	public static final String SEARCH = "search";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String FINALIZED = "finalized";
	
	private enum SearchFields {
		repId("REP_ID"),
		caseId("CASE_ID"),
		surgeryDate("SURGERY_DT"),
		numProducts("COUNT(k.RAM_CASE_INFO_ID)"),
		finalized("FINALIZED_FLG");
		
		private String cloumnNm;
		
		SearchFields(String columnNm) {
			this.cloumnNm = columnNm;
		}
		
		public String getColumnName (){
			return cloumnNm;
		}
	}

	public CaseSearchAction() {
		super();
	}

	/**
	 * @param avo
	 */
	public CaseSearchAction(ActionInitVO avo) {
		super(avo);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// not implemented
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json"))
			return;
		
		if (req.hasParameter("exportKits")) {
			buildKitSummaryReport(req);
		} else {
			loadKits(req);
		}
	}
	
	/**
	 * Load all kits, potentially filtering them down based on supplied search criteria
	 * @param req
	 * @param finalized
	 */
	protected void loadKits(ActionRequest req) throws ActionException {
		List<Object> params = new ArrayList<>();
		String sql = buildKitSearchSQL(req);
		
		// Get the hospital
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		params.add(role.getAttribute(0));

		// Get the search params
		if (req.hasParameter(SEARCH)) {
			params.add("%" + req.getParameter(SEARCH).toLowerCase() + "%");
			params.add("%" + req.getParameter(SEARCH).toLowerCase() + "%");
		}
		
		if (req.hasParameter(START_DATE)) params.add(Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter(START_DATE)), false));
		if (req.hasParameter(END_DATE)) params.add(Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter(END_DATE)), false));
		
		int page = Convert.formatInteger(req.getParameter("offset"), 0);
		int rpp = Convert.formatInteger(req.getParameter("limit"));
		rpp = rpp == 0 ? 10 : rpp;
		int start = page * rpp;
		int end = rpp * (page + 1);
		
		log.info(params);
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> kits = null;
		if ( Convert.formatBoolean(req.getParameter("loadAll")))
			 kits = dbp.executeSelect(sql.toString(), params, new RAMCaseVO());
		else
			 kits = dbp.executeSelect(sql.toString(), params, new RAMCaseVO(), null, start, end);
		
		putModuleData(kits, kits.size(), false);
	}
	
	
	/**
	 * Build the kit search sql query
	 * @param req
	 * @return
	 */
	private String buildKitSearchSQL(ActionRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("select i.prod_total, p.customer_nm, c.hospital_case_id, c.surgery_dt, c.case_status_cd, c.customer_id, c.profile_id ");
		sql.append("from custom.ram_case c ");
		sql.append("inner join ram_customer p on c.customer_id = p.customer_id ");
		sql.append("left outer join ( ");
		sql.append("select case_id, sum(qty_no) as prod_total ");
		sql.append("from custom.ram_case_item ");
		sql.append("group by case_id ");
		sql.append(") i on c.case_id = i.case_id  ");
		sql.append("where c.customer_id = cast(? as int) ");
		
		if (req.hasParameter(SEARCH)) {
			sql.append("and (lower(rep_id)").append(" like ? ");
			sql.append("or lower(case_id)").append(" like ?) ");
		}
		if (req.hasParameter(START_DATE)) sql.append("and k.surgery_dt > ? ");
		if (req.hasParameter(END_DATE)) sql.append("and k.surgery_dt < ? ");
		
		sql.append("order by ");
		
		if (req.hasParameter("sort")) {
			sql.append(SearchFields.valueOf(req.getParameter("sort")).getColumnName());
			
			String order = StringUtil.checkVal(req.getParameter("order"));
			if ("desc".equalsIgnoreCase(order)) {
				sql.append(" DESC ");
			} else {
				sql.append(" ASC ");
			}
		} else {
			sql.append("case_status_cd, SURGERY_DT DESC");
		}
		log.info(sql);
		return sql.toString();
	}
	
	/**
	 * Build an excel file with the all kits from the
	 * current search and add it to the request
	 * @param req
	 */
	private void buildKitSummaryReport(ActionRequest req) throws ActionException {
		loadKits(req);
		AbstractSBReportVO report = new KitExcelReport();
		report.setData(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData());
		report.setFileName("kit_summary_report.xls");
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}
}
