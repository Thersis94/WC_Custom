package com.biomed.smarttrak.fd;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.biomed.smarttrak.action.CompanyAction;
import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.admin.FinancialDashHierarchyAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.fd.FinancialDashAction.DashType;
import com.biomed.smarttrak.fd.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.biomed.smarttrak.fd.FinancialDashVO.TableType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
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

	/**
	 * Column prefixes
	 */
	public static final String CALENDAR_YEAR = "CY";
	public static final String YEAR_TO_DATE = "YTD";
	public static final String QUARTER = "Q";

	/**
	 * Reused regex pattern used to determine whether the given column (name)
	 * in the ResultSet cotains Quarterly FD
	 */
	protected static final Pattern QTR_PATTERN = Pattern.compile("Q[1-4]");

	/**
	 * Quarter prefixes for columns
	 */
	public static final String QUARTER_1 = "Q1";
	public static final String QUARTER_2 = "Q2";
	public static final String QUARTER_3 = "Q3";
	public static final String QUARTER_4 = "Q4";

	private static final String REQ_SECTION_ID =  "sectionId";

	/**
	 * Number of times section_id appears in the MARKET version of the query
	 */
	public static final int MARKET_QUERY_SECTION_CNT = 14;

	protected DBProcessor dbp;
	protected UserVO user;

	public FinancialDashBaseAction() {
		super();
	}

	public FinancialDashBaseAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void setDBConnection(SMTDBConnection dbc) {
		super.setDBConnection(dbc);
		dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		user = (UserVO)  req.getSession().getAttribute(Constants.USER_DATA);

		// If section was not passed, set the default
		if (!req.hasParameter(REQ_SECTION_ID))
			req.setParameter(REQ_SECTION_ID, AbstractTreeAction.MASTER_ROOT);

		SmarttrakTree sections = getHierarchy(req);
		DashType dashType = (DashType) req.getAttribute(FinancialDashAction.DASH_TYPE);

		FinancialDashVO dash = new FinancialDashVO();
		SectionVO latest = getLatestPublish(req.getParameter(REQ_SECTION_ID));
		dash.setCurrentQtrYear(dashType, latest);
		dash.setReportedQtr(getLastReportedQtr());
		dash.setData(req, sections, dashType);
		dash.setBehindLatest(latest);

		//Load filtered Sections from FinancialDashHierarchyAction
		if (!req.hasParameter("isJson")) {
			FinancialDashHierarchyAction fdha = (FinancialDashHierarchyAction) ActionControllerFactoryImpl.loadAction(FinancialDashHierarchyAction.class.getName(), this);
			fdha.retrieve(req);
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			req.setAttribute("sectionTree", mod.getActionData());
		}

		// Filter out financial data requests (i.e initial page load vs. json call).
		// Financial data is only needed on a json call or report request.
		// Default data/options are required for initial page load.
		if (req.hasParameter("isJson") || req.hasParameter("isReport"))
			getFinancialData(dash, sections, dashType);

		if (req.hasParameter("isReport"))
			processReport(req, dash, sections);

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
	protected void processReport(ActionRequest req, FinancialDashVO dash, SmarttrakTree sections) {
		String sortField = StringUtil.checkVal(req.getParameter("sortField"));
		int sortOrder = Convert.formatInteger(req.getParameter("sortOrder"));

		FinancialDashReportVO rpt = new FinancialDashReportVO();
		rpt.setData(dash);
		rpt.setSortField(sortField);
		rpt.setSortOrder(sortOrder);
		rpt.setSections(sections);

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

	/**
	 * Returns a section hierarchy action
	 * 
	 * @return
	 */
	protected SectionHierarchyAction getHierarchyAction() {
		SectionHierarchyAction sha = new FinancialDashHierarchyAction(getActionInit());
		sha.setAttributes(getAttributes());
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
		return getHierarchy(req.getParameter(REQ_SECTION_ID));
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
	protected SectionVO getLatestPublish(String sectionId) {
		SectionHierarchyAction sha = getHierarchyAction();
		return sha.getLatestFdPublish(sectionId);
	}
	
	/**
	 * Get the latest reported quarter for the most recent year.
	 */
	protected int getLastReportedQtr() {
		SectionHierarchyAction sha = getHierarchyAction();
		return sha.getLatestFdReported();
	}

	/**
	 * Returns the company name for the company displayed on the dashboard
	 * 
	 * @param companyId
	 * @return
	 * @throws ActionException
	 */
	protected String getCompanyName(String companyId) throws ActionException {
		CompanyAction compAct = new CompanyAction(getActionInit());
		compAct.setAttributes(getAttributes());
		compAct.setDBConnection(dbConn);

		CompanyVO company = compAct.getCompany(companyId);
		return company.getCompanyName();
	}

	/**
	 * Gets the financial data to display in the table and charts
	 * 
	 * @param dash
	 */
	protected void getFinancialData(FinancialDashVO dash, SmarttrakTree sections, DashType dashType) {
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
			dash.setData(rs, sections, dashType);
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
		return TableType.MARKET == tt ? MARKET_QUERY_SECTION_CNT : 0;
	}

	/**
	 * Gets the number of years of data required for the display type, to
	 * determine the number of joins in the sql query for previous years of data.
	 * 
	 * @param dt
	 * @param currentYear
	 * @return
	 */
	protected int getDataYears(DisplayType dt, int currentYear) {
		if (DisplayType.ALL == dt) {
			return currentYear - FinancialDashColumnSet.BEGINNING_YEAR + 1;
		} else {
			return dt.getDataYears();
		}
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
		log.debug(sql);
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
			sql.append("select ").append("r.COMPANY_ID as ROW_ID, c.SHORT_NM_TXT as ROW_NM, r.COMPANY_ID, ");
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
		String sumR = ", sum(r";
		DisplayType dt = dash.getColHeaders().getDisplayType();
		StringBuilder sql = new StringBuilder(500);
		sql.append("sum(r.Q1_NO) as Q1_0, sum(r.Q2_NO) as Q2_0, sum(r.Q3_NO) as Q3_0, sum(r.Q4_NO) as Q4_0, ");
		sql.append("sum(r2.Q1_NO) as Q1_1, sum(r2.Q2_NO) as Q2_1, sum(r2.Q3_NO) as Q3_1, sum(r2.Q4_NO) as Q4_1 "); // Needed for all column display types to get percent change from prior year

		// Add in additional years of data as required by the FD display type
		int dataYears = getDataYears(dt, dash.getCurrentYear());
		for (int yr = 3; yr <= dataYears; yr++) {
			sql.append(sumR).append(yr).append(".Q1_NO) as Q1_").append(yr-1);
			sql.append(sumR).append(yr).append(".Q2_NO) as Q2_").append(yr-1);
			sql.append(sumR).append(yr).append(".Q3_NO) as Q3_").append(yr-1);
			sql.append(sumR).append(yr).append(".Q4_NO) as Q4_").append(yr-1).append(" ");
		}
		
		if (TableType.COMPANY == dash.getTableType())
			sql.append(", c.GRAPH_COLOR ");
		
		sql.append(", greatest(s1.fd_pub_qtr, s2.fd_pub_qtr, s3.fd_pub_qtr, s4.fd_pub_qtr, s5.fd_pub_qtr, s6.fd_pub_qtr, s7.fd_pub_qtr) as fd_pub_qtr ");
		
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

		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		DisplayType dt = dash.getColHeaders().getDisplayType();

		sql.append("from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_FD_REVENUE r2 on r.COMPANY_ID = r2.COMPANY_ID and r.REGION_CD = r2.REGION_CD and r.SECTION_ID = r2.SECTION_ID and r.YEAR_NO - 1 = r2.YEAR_NO ");

		// Add in additional years of data as required by the FD display type
		int dataYears = getDataYears(dt, dash.getCurrentYear());
		for (int yr = 3; yr <= dataYears; yr++) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_FD_REVENUE r").append(yr).append(" ");
			sql.append("on r.COMPANY_ID = r").append(yr).append(".COMPANY_ID ");
			sql.append("and r.REGION_CD = r").append(yr).append(".REGION_CD ");
			sql.append("and r.SECTION_ID = r").append(yr).append(".SECTION_ID ");
			sql.append("and r.YEAR_NO - ").append(yr-1).append(" = r").append(yr).append(".YEAR_NO ");
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

		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		int regionCnt = dash.getCountryTypes().size();
		TableType tt = dash.getTableType();
		
		getCommonMidSql(sql, custom);
		


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
		
		sql.append("group by ROW_ID, ROW_NM, r.YEAR_NO, s1.fd_pub_qtr, s2.fd_pub_qtr, s3.fd_pub_qtr, s4.fd_pub_qtr, s5.fd_pub_qtr, s6.fd_pub_qtr, s7.fd_pub_qtr ");

		if (TableType.COMPANY == dash.getTableType()) {
			sql.append(", c.GRAPH_COLOR, r.section_id ");
		} else {
			sql.append(", r.COMPANY_ID ");
		}


		// Handle edit mode specific columns in the group by
		if (dash.getEditMode()) {
			DisplayType dt = dash.getColHeaders().getDisplayType();
			for (int i = 1; i <= getDataYears(dt, dash.getCurrentYear()); i++) {
				sql.append(", REVENUE_ID_").append(i-1).append(" ");
			}

			if (TableType.COMPANY == tt) {
				sql.append(", r.COMPANY_ID, r.REGION_CD ");
			} else {
				sql.append(", SECT_ID, r.REGION_CD ");
			}
		}

		if (!dash.showEmpty()) {
			//the 'having' clause ensures we have financial data in at least one quarter/column
			sql.append("having ");
			sql.append("sum(r.Q1_NO) > 0 or sum(r.Q2_NO) > 0 or sum(r.Q3_NO) > 0 or sum(r.Q4_NO) > 0 ");
			sql.append("or sum(r2.Q1_NO) > 0 or sum(r2.Q2_NO) > 0 or sum(r2.Q3_NO) > 0 or sum(r2.Q4_NO) > 0 ");

			DisplayType dt = dash.getColHeaders().getDisplayType();
			int dataYears = getDataYears(dt, dash.getCurrentYear());
			for (int yr = 3; yr <= dataYears; yr++) {
				sql.append("or sum(r").append(yr).append(".Q1_NO) > 0 or sum(r").append(yr).append(".Q2_NO) > 0 or sum(r").append(yr).append(".Q3_NO) > 0 or sum(r").append(yr).append(".Q4_NO) > 0 ");
			}
		}

		sql.append("order by ROW_NM").append(dash.getEditMode() ? ", CASE r.REGION_CD WHEN 'US' THEN 1 WHEN 'EU' THEN 2 ELSE 3 END" : "");

		return sql;
	}
	
	
	/**
	 * Get the join and standard where clause for the queries.
	 * @param sql
	 * @param custom
	 */
	protected void getCommonMidSql(StringBuilder sql, String custom) {
		sql.append(DBUtil.INNER_JOIN).append(custom).append("BIOMEDGPS_COMPANY c on r.COMPANY_ID = c.COMPANY_ID ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("BIOMEDGPS_SECTION s1 on r.SECTION_ID = s1.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa1 on s1.SECTION_ID = aa1.SECTION_ID and aa1.FD_NO = 1 and aa1.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s2 on s1.PARENT_ID = s2.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa2 on s2.SECTION_ID = aa2.SECTION_ID and aa2.FD_NO = 1 and aa2.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s3 on s2.PARENT_ID = s3.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa3 on s3.SECTION_ID = aa3.SECTION_ID and aa3.FD_NO = 1 and aa3.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s4 on s3.PARENT_ID = s4.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa4 on s4.SECTION_ID = aa4.SECTION_ID and aa4.FD_NO = 1 and aa4.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s5 on s4.PARENT_ID = s5.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa5 on s5.SECTION_ID = aa5.SECTION_ID and aa5.FD_NO = 1 and aa5.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s6 on s5.PARENT_ID = s6.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa6 on s6.SECTION_ID = aa6.SECTION_ID and aa6.FD_NO = 1 and aa6.ACCOUNT_ID = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_SECTION s7 on s6.PARENT_ID = s7.SECTION_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("BIOMEDGPS_ACCOUNT_ACL aa7 on s7.SECTION_ID = aa7.SECTION_ID and aa7.FD_NO = 1 and aa7.ACCOUNT_ID = ? ");

		sql.append("where (s1.SECTION_ID = ? OR s2.SECTION_ID = ? OR s3.SECTION_ID = ? OR s4.SECTION_ID = ? OR s5.SECTION_ID = ? OR s6.SECTION_ID = ? OR s7.SECTION_ID = ?) ");
		sql.append("and (aa1.SECTION_ID is not null or aa2.SECTION_ID is not null or aa3.SECTION_ID is not null or aa4.SECTION_ID is not null or aa5.SECTION_ID is not null or aa6.SECTION_ID is not null or aa7.SECTION_ID is not null) ");
	}

	
	@Override
	public void build(ActionRequest req) throws ActionException {
		String actionPerform = StringUtil.checkVal(req.getParameter("actionPerform"));
		if ("markCurrent".equals(actionPerform)) {
			markCurrentQuarter(req);
			return;
		} else if ("export".equals(actionPerform)) {
			FinancialDashImportAction export = new FinancialDashImportAction();
			export.setDBConnection(dbConn);
			export.setAttributes(attributes);
			FinancialDashImportReportVO report = export.buildReport(req);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			return;
		} else if ("import".equals(actionPerform)) {
			FinancialDashImportAction importer = new FinancialDashImportAction();
			importer.setDBConnection(dbConn);
			importer.setAttributes(attributes);
			importer.importChanges(req);
			return;
		}

		this.updateData(req);
	}

	/**
	 * Adds one or more companies to the FD data for a given section and region.
	 * Adds Revenue Records
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void updateData(ActionRequest req) throws ActionException {
		log.debug("Add companies to FD base data");
		boolean recordAdded = false;

		//Load all Writable Years
		List<Integer> years = loadYears();

		//Create List of Writable CountryTypes.
		List<CountryType> countryTypes = Arrays.asList(CountryType.EU, CountryType.ROW, CountryType.US);

		//Convert CompanyIds from req to List.
		List<String> companyIds = Arrays.asList(req.getParameterValues("companyId[]"));

		//Load Data for Existing RevenueRecords.
		Map<String, EnumMap<CountryType, List<Integer>>> existingRecords = loadExistingRevenues(req.getParameter(REQ_SECTION_ID), companyIds);

		// Get applicable data off the request
		FinancialDashRevenueVO revenueVO = new FinancialDashRevenueVO(req);

		// Add the records for every company selected
		for (String companyId : companyIds) {
			revenueVO.setCompanyId(companyId);

			//Get years for a company.
			EnumMap<CountryType, List<Integer>> regionYears = existingRecords.get(companyId);

			// Add for every region selected
			for (CountryType countryType : countryTypes) {
				revenueVO.setRegionCd(countryType.toString());
				List<Integer> existingYears = null;

				//Attempt to get Years for a Region.  If absent, company doesn't exist.
				if(regionYears != null)
					existingYears = regionYears.get(countryType);

				// Iterate all possible years.
				for(int yr : years) {

					//Check the Existing Years and add a Revenue Record if it doesn't exist in the existingYears.
					if(existingYears == null || !existingYears.contains(yr)) {
						revenueVO.setYearNo(yr);
						revenueVO.setRegionCd(countryType.toString());
						addRevenueRecord(revenueVO);
						revenueVO.setRevenueId(null);
						recordAdded = true;
					}
				}
			}
		}

		//If no records were added, all passed companies were already added.
		if(!recordAdded) {
			putModuleData("No Records Added");
		}
	}

	/**
	 * Load all years from Financial Dashboard Revenues.
	 * @return
	 */
	private List<Integer> loadYears() {
		List<Integer> years = new ArrayList<>();

		//Build Lookup Sql
		StringBuilder sql = new StringBuilder(150);
		sql.append("select distinct yrs.year_no from custom.biomedgps_fd_revenue yrs order by year_no;");

		//Query for all Years in the Revenue Table.
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				years.add(rs.getInt("year_no"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return years;
	}

	/**
	 * Load any existing Records in the system for the given Section and companies.
	 * 
	 * @param regionCd
	 * @param sectionId
	 * @param companyIds
	 * @return - Map of <CompanyId, List<YearsCompanyIsRegisteredFor>>
	 */
	private Map<String, EnumMap<CountryType, List<Integer>>> loadExistingRevenues(String sectionId, List<String> companyIds) {
		Map<String, EnumMap<CountryType, List<Integer>>> existingRevenues = new HashMap<>();

		//Build Lookup Sql
		StringBuilder sql = new StringBuilder(300);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("biomedgps_fd_revenue ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" section_id = ? and company_id in (");
		DBUtil.preparedStatmentQuestion(companyIds.size(), sql);
		sql.append(")");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, sectionId);
			for(String companyId : companyIds) {
				ps.setString(i++, companyId);
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				String companyId = rs.getString("company_id");
				CountryType regionCd = EnumUtil.safeValueOf(CountryType.class, rs.getString("region_cd"));

				EnumMap<CountryType, List<Integer>> regionYears;
				List<Integer> years;

				//Get the regionYears Map
				if(existingRevenues.containsKey(companyId)) {
					regionYears = existingRevenues.get(companyId);
				} else {
					regionYears = new EnumMap<>(CountryType.class);
				}

				//Load Years List
				if(regionYears.containsKey(regionCd)) {
					years = regionYears.get(regionCd);
				} else {
					years = new ArrayList<>();
				}

				//Populate List and update Maps.
				years.add(rs.getInt("year_no"));
				regionYears.put(regionCd, years);
				existingRevenues.put(companyId, regionYears);
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return existingRevenues;
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
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);

		sql.append("select * from ").append(custom).append("BIOMEDGPS_FD_REVENUE ");
		sql.append("where REVENUE_ID in (").append(DBUtil.preparedStatmentQuestion(recordCnt)).append(") ");

		return sql.toString();
	}

	/**
	 * Gets the existing base revenue data that is related to a specific scenario.
	 * 
	 * @param dashVO
	 * @return
	 */
	protected Map<String, FinancialDashRevenueVO> getBaseData(FinancialDashVO dashVO, int yearCnt) {
		Map<String, FinancialDashRevenueVO> baseData = new HashMap<>();

		List<Object> params = new ArrayList<>();
		String sql = getRevenueSql(dashVO, yearCnt, params);

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
	 * @param dashVO - dashboard options to use in the record selection
	 * @param params - params for db processor
	 * @return
	 */
	private String getRevenueSql(FinancialDashVO dashVO, int yearCnt, List<Object> params) {
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select r.* from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY so on r.REVENUE_ID = so.REVENUE_ID ");
		sql.append("where so.SCENARIO_ID = ? ");
		params.add(dashVO.getScenarioId());

		// Make sure to only get the years shown in the dashboard
		sql.append("and ").append(getYearSql(dashVO, yearCnt, params, "r"));

		// If company id is passed, user is looking at a company, not a section
		if (!StringUtil.isEmpty(dashVO.getCompanyId())) {
			sql.append("and r.COMPANY_ID = ? ");
			params.add(dashVO.getCompanyId());
		} else {
			sql.append("and r.SECTION_ID = ? ");
			params.add(dashVO.getSectionId());
		}

		sql.append("and ").append(getRegionSql(dashVO, params));

		return sql.toString();
	}

	/**
	 * Returns sql to filter by the selected year(s)
	 * 
	 * @param dashVO
	 * @param yearCnt
	 * @param params
	 * @param alias
	 * @return
	 */
	protected String getYearSql(FinancialDashVO dashVO, int yearCnt, List<Object> params, String alias) {
		StringBuilder sql = new StringBuilder(25);

		int year = dashVO.getColHeaders().getCalendarYear();
		sql.append(alias).append(".YEAR_NO in (").append(DBUtil.preparedStatmentQuestion(yearCnt)).append(") ");
		for (int i = 0; i < yearCnt; i++) {
			params.add(year--);
		}

		return sql.toString();
	}

	/**
	 * Returns sql to filter by the selected region(s)
	 * 
	 * @param dashVO
	 * @param params
	 * @return
	 */
	protected String getRegionSql(FinancialDashVO dashVO, List<Object> params) {
		StringBuilder sql = new StringBuilder(25);

		// In the case of WW, user is looking at multiple regions
		List<CountryType> regions = dashVO.getCountryTypes();
		sql.append("r.REGION_CD in (").append(DBUtil.preparedStatmentQuestion(regions.size())).append(") ");
		for (CountryType region : regions) {
			params.add(region.toString());
		}

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

		String sectionId = StringUtil.checkVal(req.getParameter(REQ_SECTION_ID));
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
