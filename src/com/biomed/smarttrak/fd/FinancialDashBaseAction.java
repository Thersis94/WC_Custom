package com.biomed.smarttrak.fd;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.CompanyAction;
import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.fd.FinancialDashAction.DashType;
import com.biomed.smarttrak.fd.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.biomed.smarttrak.fd.FinancialDashVO.TableType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashBaseAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/

public class FinancialDashBaseAction extends SBActionAdapter {
	
	DBProcessor dbp;
	UserVO user;

	/**
	 * Maximum number of years to query data
	 */
	public static final int MAX_DATA_YEARS = 4;
	
	/**
	 * Column prefixes
	 */
	public static final String CALENDAR_YEAR = "CY";
	public static final String YEAR_TO_DATE = "YTD";
	public static final String QUARTER = "Q";
	
	/**
	 * Quarter prefixes for columns
	 */
	public static final String QUARTER_1 = "Q1";
	public static final String QUARTER_2 = "Q2";
	public static final String QUARTER_3 = "Q3";
	public static final String QUARTER_4 = "Q4";
	
	/**
	 * Number of times section_id appears in the MARKET version of the query
	 */
	public static final int MARKET_QUERY_SECTION_CNT = 14;
	
	public FinancialDashBaseAction() {
		super();
	}

	public FinancialDashBaseAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void setDBConnection(SMTDBConnection dbc) {
		super.setDBConnection(dbc);
		dbp = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
	}
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);
		
		SMTSession ses = req.getSession();
		user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		
		// If section was not passed, set the default
		if (!req.hasParameter("sectionId")) {
			req.setParameter("sectionId", AbstractTreeAction.MASTER_ROOT);
		}
		
		SmarttrakTree sections = getHierarchy(req);
		DashType dashType = (DashType) req.getAttribute(FinancialDashAction.DASH_TYPE);
		
		FinancialDashVO dash = new FinancialDashVO();
		dash.setCurrentQtrYear(dashType, getLatestPublish());
		dash.setData(req, sections);
		
		// Filter out financial data requests (i.e initial page load vs. json call).
		// Financial data is only needed on a json call or report request.
		// Default data/options are required for initial page load.
		if (req.hasParameter("isJson") || req.hasParameter("isReport")) {
			getFinancialData(dash, sections);
		}
		
		if (req.hasParameter("isReport")) {
			processReport(req, dash);
		}
		
		// Gets the company name for page display
		if (!StringUtil.isEmpty(dash.getCompanyId())) {
			String companyName = getCompanyName(dash.getCompanyId());
			dash.setCompanyName(companyName);
		}
		
		this.putModuleData(dash);
	}
	
	/**
	 * Handles generation of the report based on the current selections in the dashboard
	 * 
	 * @param req
	 * @param dash
	 */
	protected void processReport(ActionRequest req, FinancialDashVO dash) {
		FinancialDashReportVO rpt = new FinancialDashReportVO();
		rpt.setData(dash);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
	/**
	 * Returns a section hierarchy action
	 * 
	 * @return
	 */
	protected SectionHierarchyAction getHierarchyAction() {
		SectionHierarchyAction sha = new SectionHierarchyAction(this.actionInit);
		sha.setAttributes(this.attributes);
		sha.setDBConnection(dbConn);
		
		return sha;
	}
	
	/**
	 * Gets the hierarchy starting from the passed sectionId
	 * 
	 * @param sectionId
	 * @return
	 */
	protected SmarttrakTree getHierarchy(String sectionId) {
		SectionHierarchyAction sha = getHierarchyAction();
		return sha.loadTree(sectionId);
	}
	
	/**
	 * Gets the hierarchy for the requested level
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SmarttrakTree getHierarchy(ActionRequest req) {
		return getHierarchy(req.getParameter("sectionId"));
	}
	
	/**
	 * Gets the full hierarchy
	 * 
	 * @return
	 */
	protected SmarttrakTree getFullHierarchy() {
		return getHierarchy((String) null);
	}
	
	/**
	 * Gets the latest system-wide published FD quarter/year
	 * 
	 * @return
	 */
	protected SectionVO getLatestPublish() {
		SectionHierarchyAction sha = getHierarchyAction();
		return sha.getLatestFdPublish();
	}
	
	/**
	 * Returns the company name for the company displayed on the dashboard
	 * 
	 * @param companyId
	 * @return
	 * @throws ActionException
	 */
	protected String getCompanyName(String companyId) throws ActionException {
		CompanyAction compAct = new CompanyAction(this.actionInit);
		compAct.setAttributes(this.attributes);
		compAct.setDBConnection(dbConn);
		
		CompanyVO company = compAct.getCompany(companyId);
		return company.getCompanyName();
	}
	
	/**
	 * Gets the financial data to display in the table and charts
	 * 
	 * @param dash
	 */
	protected void getFinancialData(FinancialDashVO dash, SmarttrakTree sections) {
		String sql = getFinancialDataSql(dash);
		int regionCnt = dash.getCountryTypes().size();
		int sectionCnt = getQuerySectionCnt(dash);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			for (int i = 0; i < sectionCnt; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < 7; i++) {
				ps.setString(++idx, user.getAccountId());
			}
			for (int i = 0; i < 7; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < regionCnt; i++) {
				ps.setString(++idx, dash.getCountryTypes().get(i).name());
			}
			if (!StringUtil.isEmpty(dash.getCompanyId())) {
				ps.setString(++idx, dash.getCompanyId());
			}
			ps.setInt(++idx, dash.getColHeaders().getCalendarYear());
			
			ResultSet rs = ps.executeQuery();
			dash.setData(rs, sections);
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard data", sqle);
		}
	}
	
	/**
	 * Helper to return the count of section id in the query
	 * 
	 * @param dash
	 * @return
	 */
	protected int getQuerySectionCnt(FinancialDashVO dash) {
		TableType tt = dash.getTableType();
		
		int sectionCnt = 0;
		if (TableType.MARKET == tt) {
			sectionCnt = MARKET_QUERY_SECTION_CNT;
		}
		
		return sectionCnt;
	}
	
	/**
	 * Returns the sql for retrieving financial data. 
	 * @return
	 */
	protected String getFinancialDataSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(2600);

		sql.append(getCommonSelectSql(dash));
		sql.append(getSelectSql(dash));
		sql.append(getJoinSql(dash));
		sql.append(getCommonEndSql(dash));

		return sql.toString();
	}
	
	/**
	 * Gets the select part of the query common to the Base and Overlay data.
	 * 
	 * @param dash
	 * @return
	 */
	protected StringBuilder getCommonSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		TableType tt = dash.getTableType();
		
		if (TableType.COMPANY == tt) {
			sql.append("select ").append("r.COMPANY_ID as ROW_ID, c.SHORT_NM_TXT as ROW_NM, ");
		} else { // TableType.MARKET
			
			// When viewing market data for a specific company, we always list/summarize 4 levels down in the heirarchy
			int offset = 0;
			if (!StringUtil.isEmpty(dash.getCompanyId())) {
				offset = 4;
			}
			
			// Group by the appropriate parent in the heirarchy
			sql.append("select CASE ");
			for (int i = 7; i > 0; i--) {
				sql.append("WHEN s").append(i).append(".PARENT_ID = ? THEN s").append(i-offset < 1 ? 1 : i-offset).append(".SECTION_ID ");
			}
			sql.append("END as ROW_ID, ");

			sql.append("CASE ");
			for (int i = 7; i > 0; i--) {
				sql.append("WHEN s").append(i).append(".PARENT_ID = ? THEN s").append(i-offset < 1 ? 1 : i-offset).append(".SECTION_NM ");
			}
			sql.append("END as ROW_NM, ");
		}
		
		sql.append("r.YEAR_NO, ");
		
		return sql;
	}
	
	/**
	 * Gets the select part of the query specific to the Base revenue data.
	 * 
	 * @param dash
	 * @return
	 */
	protected StringBuilder getSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(500);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		sql.append("sum(r.Q1_NO) as Q1_0, sum(r.Q2_NO) as Q2_0, sum(r.Q3_NO) as Q3_0, sum(r.Q4_NO) as Q4_0, ");
		sql.append("sum(r2.Q1_NO) as Q1_1, sum(r2.Q2_NO) as Q2_1, sum(r2.Q3_NO) as Q3_1, sum(r2.Q4_NO) as Q4_1 "); // Needed for all column display types to get percent change from prior year
		
		// Columns needed only for specific display types
		if (dt == DisplayType.YOY || dt == DisplayType.FOURYR || dt == DisplayType.SIXQTR) {
			sql.append(", sum(r3.Q1_NO) as Q1_2, sum(r3.Q2_NO) as Q2_2, sum(r3.Q3_NO) as Q3_2, sum(r3.Q4_NO) as Q4_2 ");
		}
		if (dt == DisplayType.FOURYR) {
			sql.append(", sum(r4.Q1_NO) as Q1_3, sum(r4.Q2_NO) as Q2_3, sum(r4.Q3_NO) as Q3_3, sum(r4.Q4_NO) as Q4_3 ");
			sql.append(", sum(r5.Q1_NO) as Q1_4, sum(r5.Q2_NO) as Q2_4, sum(r5.Q3_NO) as Q3_4, sum(r5.Q4_NO) as Q4_4 "); // Needed to get percent change from prior year in the fourth year
		}
		
		return sql;
	}
	
	/**
	 * Gets the join part of the query specific to the Base revenue data.
	 * 
	 * @param dash
	 * @return
	 */
	protected StringBuilder getJoinSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		sql.append("from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r2 on r.COMPANY_ID = r2.COMPANY_ID and r.REGION_CD = r2.REGION_CD and r.SECTION_ID = r2.SECTION_ID and r.YEAR_NO - 1 = r2.YEAR_NO ");

		// Joins to get columns that are needed only for specific display types
		if (dt == DisplayType.YOY || dt == DisplayType.FOURYR || dt == DisplayType.SIXQTR) {
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r3 on r.COMPANY_ID = r3.COMPANY_ID and r.REGION_CD = r3.REGION_CD and r.SECTION_ID = r3.SECTION_ID and r.YEAR_NO - 2 = r3.YEAR_NO ");
		}
		if (dt == DisplayType.FOURYR) {
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r4 on r.COMPANY_ID = r4.COMPANY_ID and r.REGION_CD = r4.REGION_CD and r.SECTION_ID = r4.SECTION_ID and r.YEAR_NO - 3 = r4.YEAR_NO ");
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r5 on r.COMPANY_ID = r5.COMPANY_ID and r.REGION_CD = r5.REGION_CD and r.SECTION_ID = r5.SECTION_ID and r.YEAR_NO - 4 = r5.YEAR_NO ");
		}

		return sql;
	}

	/**
	 * Gets the end part of the query common to the Base and Overlay data.
	 * 
	 * @param dash
	 * @return
	 */
	protected StringBuilder getCommonEndSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		int regionCnt = dash.getCountryTypes().size();
		TableType tt = dash.getTableType();
		
		sql.append("inner join ").append(custom).append("BIOMEDGPS_COMPANY c on r.COMPANY_ID = c.COMPANY_ID ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_SECTION s1 on r.SECTION_ID = s1.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa1 on s1.SECTION_ID = aa1.SECTION_ID and aa1.FD_NO = 1 and aa1.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s2 on s1.PARENT_ID = s2.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa2 on s2.SECTION_ID = aa2.SECTION_ID and aa2.FD_NO = 1 and aa2.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s3 on s2.PARENT_ID = s3.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa3 on s3.SECTION_ID = aa3.SECTION_ID and aa3.FD_NO = 1 and aa3.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s4 on s3.PARENT_ID = s4.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa4 on s4.SECTION_ID = aa4.SECTION_ID and aa4.FD_NO = 1 and aa4.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s5 on s4.PARENT_ID = s5.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa5 on s5.SECTION_ID = aa5.SECTION_ID and aa5.FD_NO = 1 and aa5.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s6 on s5.PARENT_ID = s6.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa6 on s6.SECTION_ID = aa6.SECTION_ID and aa6.FD_NO = 1 and aa6.ACCOUNT_ID = ? ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s7 on s6.PARENT_ID = s7.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa7 on s7.SECTION_ID = aa7.SECTION_ID and aa7.FD_NO = 1 and aa7.ACCOUNT_ID = ? ");

		sql.append("where (s1.SECTION_ID = ? OR s2.SECTION_ID = ? OR s3.SECTION_ID = ? OR s4.SECTION_ID = ? OR s5.SECTION_ID = ? OR s6.SECTION_ID = ? OR s7.SECTION_ID = ?) ");
		sql.append("and (aa1.SECTION_ID is not null or aa2.SECTION_ID is not null or aa3.SECTION_ID is not null or aa4.SECTION_ID is not null or aa5.SECTION_ID is not null or aa6.SECTION_ID is not null or aa7.SECTION_ID is not null) ");
		
		sql.append("and r.REGION_CD in (");
		for (int i = 1; i <= regionCnt; i++) {
			if (i == 1) {
				sql.append("?");
			} else {
				sql.append(",?");
			}
		}
		sql.append(") ");
		
		if (!StringUtil.isEmpty(dash.getCompanyId())) {
			sql.append("and r.COMPANY_ID = ? ");
		}
		
		sql.append("and r.YEAR_NO = ? ");
		
		sql.append("group by ROW_ID, ROW_NM, r.YEAR_NO ");
		if (dash.getEditMode()) {
			if (TableType.COMPANY == tt) {
				sql.append(", r.COMPANY_ID, r.REGION_CD ");
			} else {
				sql.append(", SECT_ID, r.REGION_CD ");
			}
		}
		
		sql.append("order by ROW_NM ");
		
		return sql;
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		String actionPerform = StringUtil.checkVal(req.getParameter("actionPerform"));
		if ("markCurrent".equals(actionPerform)) {
			markCurrentQuarter(req);
			return;
		}
		
		this.updateData(req);
	}
	
	/**
	 * Adds one or more companies to the FD data for a given section/region/year
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void updateData(ActionRequest req) throws ActionException {
		log.debug("Add companies to FD base data");
		
		// Get applicable data off the request
		FinancialDashRevenueVO revenueVO = new FinancialDashRevenueVO(req);
		FinancialDashVO dashVO = new FinancialDashVO();
		dashVO.addCountryType(StringUtil.checkVal(req.getParameter("regionCd")));
		
		// Get the values to iterate over... we need to add a record for every company selected,
		// and also for every region. If WW was selected, we need to add a separate record for each region.
		List<String> companyIds = Arrays.asList(req.getParameterValues("companyId[]"));
		List<CountryType> countryTypes = dashVO.getCountryTypes();
		
		// Add the records
		for (String companyId : companyIds) {
			revenueVO.setCompanyId(companyId);
			
			for (CountryType countryType : countryTypes) {
				revenueVO.setRegionCd(countryType.toString());
				addRevenueRecord(revenueVO);
			}
		}
	}
	
	/**
	 * Adds a revenue record to the base data
	 * 
	 * @param revenueVO
	 * @throws ActionException 
	 */
	protected void addRevenueRecord(FinancialDashRevenueVO revenueVO) throws ActionException {
		try {
			dbp.save(revenueVO);
		} catch (Exception e) {
			throw new ActionException("Couldn't save new financial dash revenue record.");
		}
	}
	
	/**
	 * Returns a single revenue record.
	 * 
	 * @param revenueId
	 * @return
	 * @throws ActionException
	 */
	protected FinancialDashRevenueVO getRevenueRecord(String revenueId) throws ActionException {
		FinancialDashRevenueVO rvo = new FinancialDashRevenueVO();
		rvo.setRevenueId(revenueId);
		
		try {
			dbp.getByPrimaryKey(rvo);
		} catch (Exception e) {
			throw new ActionException("Couldn't get financial dashboard revenue record.", e);
		}
		
		return rvo;
	}

	/**
	 * Returns a list of revenue records based on the passed ids.
	 * 
	 * @param revenueIds
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<FinancialDashRevenueVO> getRevenueRecords(List<String> revenueIds) {
		List<FinancialDashRevenueVO> rvos = new ArrayList<>();
		
		String sql = getRevenueRecordSql(revenueIds.size());
		List<Object> params = new ArrayList<>();
		params.addAll(revenueIds);
		
		List<?> revRecords = dbp.executeSelect(sql, params, new FinancialDashRevenueVO());
		
		if (!revRecords.isEmpty()) {
			rvos = (List<FinancialDashRevenueVO>) revRecords;
		}
		
		return rvos;
	}

	/**
	 * Returns the sql that retrieves fd revenue records
	 * 
	 * @return
	 */
	private String getRevenueRecordSql(int recordCnt) {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		
		sql.append("select * from ").append(custom).append("BIOMEDGPS_FD_REVENUE ");
		sql.append("where REVENUE_ID in (").append(DBUtil.preparedStatmentQuestion(recordCnt)).append(") ");
		
		return sql.toString();
	}
	
	/**
	 * Gets the existing base revenue data that is related to a specific scenario.
	 * 
	 * @param sectionId
	 * @param countryType
	 * @param year
	 * @param scenarioId
	 * @return
	 */
	protected Map<String, FinancialDashRevenueVO> getBaseData(String sectionId, String countryType, int year, String scenarioId) {
		Map<String, FinancialDashRevenueVO> baseData = new HashMap<>();
		
		String sql = getRevenueSql();
		List<Object> params = new ArrayList<>();
		params.addAll(Arrays.asList(sectionId, year, countryType, scenarioId));
		
		List<?> revenueRecords = dbp.executeSelect(sql, params, new FinancialDashRevenueVO());
		
		for (Object revenueRecord : revenueRecords) {
			FinancialDashRevenueVO rvo = (FinancialDashRevenueVO) revenueRecord;
			baseData.put(rvo.getRevenueId(), rvo);
		}
		
		return baseData;
	}
	
	/**
	 * Returns the sql required for getting all base data associated to a scenario.
	 * 
	 * @return
	 */
	private String getRevenueSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		
		sql.append("select r.* from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY so on r.REVENUE_ID = so.REVENUE_ID ");
		sql.append("where r.SECTION_ID = ? and r.YEAR_NO = ? and r.REGION_CD = ? and so.SCENARIO_ID = ? ");
		
		return sql.toString();
	}
	
	/**
	 * Marks the current quarter for the selected section and all sections
	 * in the tree below it.
	 * 
	 * @param req
	 */
	protected void markCurrentQuarter(ActionRequest req) {
		SectionHierarchyAction sha = getHierarchyAction();

		String sectionId = StringUtil.checkVal(req.getParameter("sectionId"));
		int yr = Convert.formatInteger(req.getParameter("currentYr"));
		int qtr = Convert.formatInteger(req.getParameter("currentQtr"));
		
		log.debug("Marking the current quarter for " + sectionId);
		
		// Gets the tree starting at the selected level
		SmarttrakTree tree = getHierarchy(sectionId);
		Node n = tree.getRootNode();
		
		// Mark sections(s) current
		setSectionCurrent(sha, n, yr, qtr);
	}
	
	/**
	 * Marks a section current to the specified quarter
	 * 
	 * @param sha
	 * @param n
	 * @param yr
	 * @param qtr
	 */
	private void setSectionCurrent(SectionHierarchyAction sha, Node n, int yr, int qtr) {
		// Update the section to the specified quarter
		sha.updateFdPublish(n.getNodeId(), yr, qtr);
		
		// Set children to the current quarter
		for (Node child : n.getChildren()) {
			setSectionCurrent(sha, child, yr, qtr);
		}
	}
}
