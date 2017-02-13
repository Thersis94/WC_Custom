package com.biomed.smarttrak;

import java.lang.reflect.Method;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashScenarioOverlayAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 06, 2017
 ****************************************************************************/

public class FinancialDashScenarioOverlayAction extends FinancialDashBaseAction {
	
	public FinancialDashScenarioOverlayAction() {
		super();
	}

	public FinancialDashScenarioOverlayAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Gets the financial data to display in the table and charts
	 * 
	 * @param dash
	 */
	@Override
	protected void getFinancialData(FinancialDashVO dash) {
		String sql = getFinancialDataSql(dash);
		TableType tt = dash.getTableType();
		DisplayType dt = dash.getColHeaders().getDisplayType();
		int regionCnt = dash.getCountryTypes().size();
		
		int sectionCnt = 0;
		if (tt == TableType.MARKET) {
			sectionCnt = 14;
		}
		
		int scenarioJoins = 2;
		if (dt == DisplayType.YOY || dt == DisplayType.SIXQTR) {
			scenarioJoins = 3;
		} else if (dt == DisplayType.FOURYR) {
			scenarioJoins = 5;
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			for (int i = 0; i < sectionCnt; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < scenarioJoins; i++) {
				ps.setString(++idx, dash.getScenarioId());
			}
			for (int i = 0; i < 7; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < regionCnt; i++) {
				ps.setString(++idx, dash.getCountryTypes().get(i).name());
			}
			ps.setInt(++idx, dash.getColHeaders().getCalendarYear());
			
			ResultSet rs = ps.executeQuery();
			dash.setData(rs);
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard data", sqle);
		}
	}
	
	/**
	 * Gets the select part of the query specific to the Scenario Overlay data.
	 * 
	 * @param dash
	 * @return
	 */
	@Override
	protected StringBuilder getSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		// Usinig coalesce here to "prefer" the overlay data over the standard data where applicable
		sql.append("r.YEAR_NO, sum(coalesce(o.Q1_NO, r.Q1_NO)) as Q1_0, sum(coalesce(o.Q2_NO, r.Q2_NO)) as Q2_0, sum(coalesce(o.Q3_NO, r.Q3_NO)) as Q3_0, sum(coalesce(o.Q4_NO, r.Q4_NO)) as Q4_0, ");
		sql.append("sum(coalesce(o2.Q1_NO, r2.Q1_NO)) as Q1_1, sum(coalesce(o2.Q2_NO, r2.Q2_NO)) as Q2_1, sum(coalesce(o2.Q3_NO, r2.Q3_NO)) as Q3_1, sum(coalesce(o2.Q4_NO, r2.Q4_NO)) as Q4_1 "); // Needed for all column display types to get percent change from prior year
		
		// Columns needed only for specific display types
		if (dt == DisplayType.YOY || dt == DisplayType.FOURYR || dt == DisplayType.SIXQTR) {
			sql.append(", sum(coalesce(o3.Q1_NO, r3.Q1_NO)) as Q1_2, sum(coalesce(o3.Q2_NO, r3.Q2_NO)) as Q2_2, sum(coalesce(o3.Q3_NO, r3.Q3_NO)) as Q3_2, sum(coalesce(o3.Q4_NO, r3.Q4_NO)) as Q4_2 ");
		}
		if (dt == DisplayType.FOURYR) {
			sql.append(", sum(coalesce(o4.Q1_NO, r4.Q1_NO)) as Q1_3, sum(coalesce(o4.Q2_NO, r4.Q2_NO)) as Q2_3, sum(coalesce(o4.Q3_NO, r4.Q3_NO)) as Q3_3, sum(coalesce(o4.Q4_NO, r4.Q4_NO)) as Q4_3 ");
			sql.append(", sum(coalesce(o5.Q1_NO, r5.Q1_NO)) as Q1_4, sum(coalesce(o5.Q2_NO, r5.Q2_NO)) as Q2_4, sum(coalesce(o5.Q3_NO, r5.Q3_NO)) as Q3_4, sum(coalesce(o5.Q4_NO, r5.Q4_NO)) as Q4_4 "); // Needed to get percent change from prior year in the fourth year
		}
		
		return sql;
	}
	
	/**
	 * Gets the join part of the query specific to the Scenario Overlay data.
	 * 
	 * @param dash
	 * @return
	 */
	@Override
	protected StringBuilder getJoinSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder();
		
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		sql.append("from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o on r.REVENUE_ID = o.REVENUE_ID and o.SCENARIO_ID = ? ");

		sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r2 on r.COMPANY_ID = r2.COMPANY_ID and r.REGION_CD = r2.REGION_CD and r.SECTION_ID = r2.SECTION_ID and r.YEAR_NO - 1 = r2.YEAR_NO ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o2 on r2.REVENUE_ID = o2.REVENUE_ID and o2.SCENARIO_ID = ? ");

		// Joins to get columns that are needed only for specific display types
		if (dt == DisplayType.YOY || dt == DisplayType.FOURYR || dt == DisplayType.SIXQTR) {
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r3 on r.COMPANY_ID = r3.COMPANY_ID and r.REGION_CD = r3.REGION_CD and r.SECTION_ID = r3.SECTION_ID and r.YEAR_NO - 2 = r3.YEAR_NO ");
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o3 on r3.REVENUE_ID = o3.REVENUE_ID and o3.SCENARIO_ID = ? ");
		}
		if (dt == DisplayType.FOURYR) {
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r4 on r.COMPANY_ID = r4.COMPANY_ID and r.REGION_CD = r4.REGION_CD and r.SECTION_ID = r4.SECTION_ID and r.YEAR_NO - 3 = r4.YEAR_NO ");
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o4 on r4.REVENUE_ID = o4.REVENUE_ID and o4.SCENARIO_ID = ? ");

			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r5 on r.COMPANY_ID = r5.COMPANY_ID and r.REGION_CD = r5.REGION_CD and r.SECTION_ID = r5.SECTION_ID and r.YEAR_NO - 4 = r5.YEAR_NO ");
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o5 on r5.REVENUE_ID = o5.REVENUE_ID and o5.SCENARIO_ID = ? ");
		}
		
		return sql;
	}

	@Override
	protected void updateData(ActionRequest req) throws ActionException {
		String actionPerform = StringUtil.checkVal(req.getParameter("actionPerform"));
		if ("publish".equals(actionPerform)) {
			publishScenario(req);
			return;
		}
		
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String revenueId = StringUtil.checkVal(req.getParameter("pk"));
		String quarter = getQuarterFromField(StringUtil.checkVal(req.getParameter("name")));
		long value = Convert.formatLong(StringUtil.checkVal(req.getParameter("value")));
		
		try {
			// Get the complete current overlay data if it exists
			FinancialDashScenarioOverlayVO sovo = getOverlayRecord(revenueId, scenarioId);
			
			// If an overlay record doesn't exist, get the current revenue data to create an overlay
			if (sovo == null) {
				FinancialDashRevenueVO rvo = getRevenueRecord(revenueId);
				sovo = new FinancialDashScenarioOverlayVO(rvo);
				sovo.setScenarioId(scenarioId);
			}
			
			// Dynamically set the specific quarter being updated
			Method method = sovo.getClass().getMethod("set" + quarter + "No", long.class);
			method.invoke(sovo, value);

			// Update or insert the record as applicable
			dbp.save(sovo);
		} catch (Exception e) {
			throw new ActionException("Couldn't save updated financial dashboard quarter data to database.", e);
		}
	}
	
	/**
	 * Returns a single scenario overlay record.
	 * May return null if the record doesn't exist.
	 * 
	 * @param revenueId
	 * @param scenarioId
	 * @return
	 */
	protected FinancialDashScenarioOverlayVO getOverlayRecord(String revenueId, String scenarioId) {
		FinancialDashScenarioOverlayVO sovo = null;
		
		String sql = getOverlayRecordSql();
		List<Object> params = new ArrayList<>();
		params.addAll(Arrays.asList(revenueId, scenarioId));
		
		List<Object> overlay = dbp.executeSelect(sql, params, new FinancialDashScenarioOverlayVO());
		
		if (!overlay.isEmpty()) {
			// For a given revenueId & scenarioId, there will only be one record if it exists
			sovo = (FinancialDashScenarioOverlayVO) overlay.get(0);
		}
		
		return sovo;
	}

	/**
	 * Returns the sql necessary for retrieving a single overlay record
	 * 
	 * @return
	 */
	protected String getOverlayRecordSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		
		sql.append("select * from ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY ");
		sql.append("where REVENUE_ID = ? and SCENARIO_ID = ? ");
		
		return sql.toString();
	}
	
	/**
	 * A row of data in the financial dashboard may have data for more than one year depending
	 * on the chosen column display set, so this helper method gets the field's quarter from the
	 * bootstrap-table-editable's field name parameter.
	 * 
	 * The format for the field name parameter is "qtr-year", for example: Q1-2016.
	 * 
	 * @param fieldName
	 * @return
	 * @throws ActionException
	 */
	protected String getQuarterFromField(String fieldName) throws ActionException {
		String[] parts = fieldName.split("-");
		String qtrString = parts[0];
		
		// Check to make sure the quarter is valid
		switch(qtrString) {
			case QUARTER_1:
			case QUARTER_2:
			case QUARTER_3:
			case QUARTER_4:
				break;
			default:
				throw new ActionException("Invalid quarter on financial data save.");
		}
		
		return qtrString;
	}
	
	/**
	 * Publishes data from a selected scenario/section/year/region combination.
	 * 
	 * @param req
	 */
	// TODO: Finish out all methods related to scenario publishing 
	protected void publishScenario(ActionRequest req) {
		String sectionId = StringUtil.checkVal(req.getParameter("sectionId"));
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String countryType = StringUtil.checkVal(req.getParameter("countryType"));
		int year = Convert.formatInteger(StringUtil.checkVal(req.getParameter("year")));
		
		// We pass scenarioId to the base data for efficiency, to get only the related records
		// that are also in the scenario, rather than getting every record.
		Map<String, FinancialDashRevenueVO> baseData = getBaseData(sectionId, countryType, year, scenarioId);
		Map<String, FinancialDashScenarioOverlayVO> overlayData = getScenarioData(sectionId, countryType, year, scenarioId);
		
		updateAllScenarios(baseData, overlayData, sectionId, countryType, year);
		updateBaseData(baseData, overlayData);
	}
	
	/**
	 * Gets the existing overlay revenue data for a specific scenario.
	 * 
	 * @param sectionId
	 * @param countryType
	 * @param year
	 * @param scenarioId
	 * @return
	 */
	protected Map<String, FinancialDashScenarioOverlayVO> getScenarioData(String sectionId, String countryType, int year, String scenarioId) {
		Map<String, FinancialDashScenarioOverlayVO> overlayData = new HashMap<>();
		
		// TODO: get the data
		
		return overlayData;
	}
	
	/**
	 * Brings the base data current with the published scenario data.
	 * 
	 * @param baseData
	 * @param overlayData
	 */
	protected void updateBaseData(Map<String, FinancialDashRevenueVO> baseData, Map<String, FinancialDashScenarioOverlayVO> overlayData) {
		for (String revenueId : overlayData.keySet()) {
			FinancialDashRevenueVO baseRecord = baseData.get(revenueId);
			if (baseRecord == null) {
				// create the record
			} else {
				// update the record
			}
			// save record to database
		}
	}

	/**
	 * Brings all existing scenarios up-to-date with the published scenario data.
	 * 
	 * @param baseData
	 * @param overlayData
	 */
	protected void updateAllScenarios(Map<String, FinancialDashRevenueVO> baseData, Map<String, FinancialDashScenarioOverlayVO> overlayData, String sectionId, String countryType, int year) {
		// Get the list of scenarios that will need to be updated
		FinancialDashScenarioAction sa = new FinancialDashScenarioAction(this.actionInit);
		sa.setAttributes(this.attributes);
		sa.setDBConnection(dbConn);
		List<FinancialDashScenarioVO> scenarios = sa.getScenarios();
		
		// Quarters to check for changes
		List<String> quarters = new ArrayList<>();
		quarters.addAll(Arrays.asList(FinancialDashBaseAction.QUARTER_1, FinancialDashBaseAction.QUARTER_2, FinancialDashBaseAction.QUARTER_3, FinancialDashBaseAction.QUARTER_4));
		
		// Loop through each scenario
		for (FinancialDashScenarioVO scenario : scenarios) {
			Map<String, FinancialDashScenarioOverlayVO> scenarioData = getScenarioData(sectionId, countryType, year, scenario.getScenarioId());
			
			// Loop through all the records in the overlay data
			for (String revenueId : overlayData.keySet()) {
				FinancialDashScenarioOverlayVO scenarioRecord = scenarioData.get(revenueId);
				if (scenarioRecord == null) {
					// create the record
				} else {
					FinancialDashRevenueVO baseRecord = baseData.get(revenueId);
					for (String quarter : quarters) {
						// if base record quarter value is same as scenario record quarter value
						// then replace with overlay record quarter value
					}
				}
				// save record to database
			}
		}
	}
}
