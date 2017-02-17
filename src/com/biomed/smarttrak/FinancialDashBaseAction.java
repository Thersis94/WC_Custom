package com.biomed.smarttrak;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.FinancialDashVO.TableType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
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

	public static final int MAX_DATA_YEARS = 4;
	
	public static final String CALENDAR_YEAR = "CY";
	public static final String YEAR_TO_DATE = "YTD";
	
	public static final String QUARTER_1 = "Q1";
	public static final String QUARTER_2 = "Q2";
	public static final String QUARTER_3 = "Q3";
	public static final String QUARTER_4 = "Q4";

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
		
		// Filter out invalid requests (i.e initial page load vs. json call)
		if (!req.hasParameter("tableType")) {
			return;
		}
		
		// Get the paramters required to generate the requested table
		String displayType = StringUtil.checkVal(req.getParameter("displayType"), FinancialDashColumnSet.DEFAULT_DISPLAY_TYPE);
		Integer calendarYear = Convert.formatInteger(req.getParameter("calendarYear"), Convert.getCurrentYear());
		String tableType = StringUtil.checkVal(req.getParameter("tableType"), FinancialDashVO.DEFAULT_TABLE_TYPE);
		String[] countryTypes = req.getParameterValues("countryTypes[]") == null ? new String[]{FinancialDashVO.DEFAULT_COUNTRY_TYPE} : req.getParameterValues("countryTypes[]");
		String sectionId = StringUtil.checkVal(req.getParameter("sectionId"), "MASTER_ROOT");
		boolean leafMode = Convert.formatBoolean(req.getParameter("leafMode"));
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String companyId = StringUtil.checkVal(req.getParameter("companyId"));
		
		// Set the parameters so they can be used to generate the query/table
		FinancialDashVO dash = new FinancialDashVO();
		dash.setTableType(tableType);
		dash.setColHeaders(displayType, calendarYear);
		for(String countryType : countryTypes) {
			dash.addCountryType(countryType);
		}
		dash.setSectionId(sectionId);
		dash.setLeafMode(leafMode);
		dash.setScenarioId(scenarioId);
		dash.setCompanyId(companyId);
		
		// Get the data for the table/chart and return it
		this.getFinancialData(dash);
		this.putModuleData(dash);
	}
	
	/**
	 * Gets the financial data to display in the table and charts
	 * 
	 * @param dash
	 */
	protected void getFinancialData(FinancialDashVO dash) {
		String sql = getFinancialDataSql(dash);
		TableType tt = dash.getTableType();
		int regionCnt = dash.getCountryTypes().size();
		
		int sectionCnt = 7;
		if (tt == TableType.MARKET) {
			sectionCnt = 21;
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			for (int i = 0; i < sectionCnt; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < regionCnt; i++) {
				ps.setString(++idx, dash.getCountryTypes().get(i).name());
			}
			if (!"".equals(dash.getCompanyId())) {
				ps.setString(++idx, dash.getCompanyId());
			}
			ps.setInt(++idx, dash.getColHeaders().getCalendarYear());
			
			ResultSet rs = ps.executeQuery();
			dash.setData(rs);
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard data", sqle);
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

		return sql.toString();
	}
	
	/**
	 * Gets the select part of the query common to the Base and Overlay data.
	 * 
	 * @param dash
	 * @return
	 */
	private StringBuilder getCommonSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		TableType tt = dash.getTableType();
		
		if (tt == TableType.COMPANY) {
			sql.append("select ").append(dash.getLeafMode() ? "r.REVENUE_ID " : "r.COMPANY_ID ").append("as ROW_ID, c.COMPANY_NM as ROW_NM, r.COMPANY_ID, ");
		} else { // TableType.MARKET
			
			// When viewing market data for a specific company, we always list/summarize 3 levels lower in the heirarchy
			int offset = 0;
			if (!"".equals(dash.getCompanyId())) {
				offset = 3;
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
		
		sql.append("r.YEAR_NO, sum(r.Q1_NO) as Q1_0, sum(r.Q2_NO) as Q2_0, sum(r.Q3_NO) as Q3_0, sum(r.Q4_NO) as Q4_0, ");
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
	private StringBuilder getCommonEndSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		int regionCnt = dash.getCountryTypes().size();
		TableType tt = dash.getTableType();
		
		sql.append("inner join ").append(custom).append("BIOMEDGPS_COMPANY c on r.COMPANY_ID = c.COMPANY_ID ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_SECTION s1 on r.SECTION_ID = s1.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s2 on s1.PARENT_ID = s2.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s3 on s2.PARENT_ID = s3.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s4 on s3.PARENT_ID = s4.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s5 on s4.PARENT_ID = s5.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s6 on s5.PARENT_ID = s6.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s7 on s6.PARENT_ID = s7.SECTION_ID ");
		sql.append("where (s1.SECTION_ID = ? OR s2.SECTION_ID = ? OR s3.SECTION_ID = ? OR s4.SECTION_ID = ? OR s5.SECTION_ID = ? OR s6.SECTION_ID = ? OR s7.SECTION_ID = ?) ");
		
		sql.append("and r.REGION_CD in (");
		for (int i = 1; i <= regionCnt; i++) {
			if (i == 1) {
				sql.append("?");
			} else {
				sql.append(",?");
			}
		}
		sql.append(") ");
		
		if (!"".equals(dash.getCompanyId())) {
			sql.append("and r.COMPANY_ID = ? ");
		}
		
		sql.append("and r.YEAR_NO = ? ");
		
		sql.append("group by ROW_ID, ROW_NM, r.YEAR_NO ");
		if (tt == TableType.COMPANY) {
			sql.append(", r.COMPANY_ID ");
		}
		
		sql.append("order by ROW_NM ");
		
		return sql;
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		this.updateData(req);
	}
	
	/**
	 * TODO: Not sure this is going to be needed any more. Data is published to the base from scenarios.
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void updateData(ActionRequest req) throws ActionException {
		log.debug("Update SmartTRAK Base Data");
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
}
