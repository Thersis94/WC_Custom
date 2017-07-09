package com.ram.action.products;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// RAM Custom
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.report.vo.KitExcelReport;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;

// WC Libs 3.2
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
	public static final String STATUS = "status";
	
	private enum SearchFields {
		caseId("case_id"),
		surgeryDate("surgery_dt"),
		numProducts("num_prod_case"),
		status("case_status_cd");
		
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
		StringBuilder sql = new StringBuilder(512);
		StringBuilder tSql = new StringBuilder(512);
		tSql.append("select count(*) as key ");
		buildSelectSQL(sql);
		buildKitSearchSQL(req, sql, true);
		buildKitSearchSQL(req, tSql, false);
		
		// Get the hospital
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		params.add(Convert.formatInteger((String)role.getAttribute(0)));

		// Get the search params
		if (req.hasParameter(SEARCH)) params.add("%" + req.getParameter(SEARCH).toLowerCase() + "%");
		if (req.hasParameter(START_DATE)) params.add(Convert.parseDateUnknownPattern(req.getParameter(START_DATE)));
		if (req.hasParameter(END_DATE)) params.add(Convert.parseDateUnknownPattern(req.getParameter(END_DATE)));
		if (! Convert.formatBoolean(req.getParameter("loadAll"))) {
			params.add(Convert.formatInteger(req.getParameter("limit")));
			params.add(Convert.formatInteger(req.getParameter("offset"), 0));
		}
		
		// Query the database
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> kits = dbp.executeSelect(sql.toString(), params, new RAMCaseVO());
		int size = kits.size();
		
		// Add search for overall count
		if (! Convert.formatBoolean(req.getParameter("loadAll"))) {
			params.remove(params.size() - 1);
			params.remove(params.size() - 1);
			List<?> count = dbp.executeSelect(tSql.toString(), params, new GenericVO());
			size = Convert.formatInteger(((GenericVO)count.get(0)).getKey().toString());
		}
		
		putModuleData(kits, size, false);
	}
	
	/**
	 * Separates out the Select form the body so a count can be performed
	 * @param sql
	 */
	private void buildSelectSQL(StringBuilder sql) {
		sql.append("select i.num_prod_case, p.customer_nm || ', ' || or_name as customer_nm, c.hospital_case_id, ");
		sql.append("c.surgery_dt, c.case_status_cd, c.customer_id, c.profile_id, c.case_id ");
	}
	
	/**
	 * Build the kit search sql query
	 * @param req
	 * @return
	 */
	private void buildKitSearchSQL(ActionRequest req, StringBuilder sql, boolean isList) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("from ").append(customDb).append("ram_case c ");
		sql.append("inner join ").append(customDb).append("ram_customer p on c.customer_id = p.customer_id ");
		sql.append("inner join ").append(customDb).append("ram_or_room r on c.or_room_id = r.or_room_id ");
		sql.append("left outer join ( ");
		sql.append("select case_id, cast(sum(qty_no) as int) as num_prod_case ");
		sql.append("from ").append(customDb).append("ram_case_item ");
		sql.append("group by case_id ");
		sql.append(") i on c.case_id = i.case_id  ");
		sql.append("where c.customer_id = cast(? as int) ");
		
		// Add the search params
		if (req.hasParameter(SEARCH)) sql.append("and lower(hospital_case_id) like ? ");
		if (req.hasParameter(START_DATE)) sql.append("and c.surgery_dt > ? ");
		if (req.hasParameter(END_DATE)) sql.append("and c.surgery_dt < ? ");
		if (req.hasParameter(STATUS)) sql.append("and c.case_status_cd in ('").append(req.getParameter(STATUS)).append("') ");
		
		if(isList) {
			sql.append("order by ");
			if (req.hasParameter("sort")) {
				sql.append(SearchFields.valueOf(req.getParameter("sort")).getColumnName());
				sql.append(" ").append(req.getParameter("order")).append(" ");
			} else {
				sql.append("case_status_cd, surgery_dt desc ");
			}
		}
		
		// add the paging
		if (isList && ! Convert.formatBoolean(req.getParameter("loadAll"))) sql.append(" limit ? offset ? ");
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
