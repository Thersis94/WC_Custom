package com.biomed.smarttrak.fd;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.fd.FinancialDashAction.DashType;
import com.biomed.smarttrak.fd.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.fd.FinancialDashVO.TableType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.SectionVO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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
	
	/**
	 * Column prefix used for base data 
	 */
	public static final String BASE_PREFIX = "REV_";
	
	/**
	 * Param used for the json data with the overlay edits
	 */
	public static final String OVERLAY_DATA = "overlayData";
	
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
	 * @param sections
	 */
	@Override
	protected void getFinancialData(FinancialDashVO dash, SmarttrakTree sections, DashType dashType) {
		String sql = getFinancialDataSql(dash);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		int regionCnt = dash.getCountryTypes().size();
		int sectionCnt = getQuerySectionCnt(dash);
		int scenarioJoins = getDataYears(dt, dash.getCurrentYear());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			for (int i = 0; i < sectionCnt; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			for (int i = 0; i < scenarioJoins; i++) {
				ps.setString(++idx, dash.getScenarioId());
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
	 * Returns the sql for retrieving financial data. 
	 * @return
	 */
	@Override
	protected String getFinancialDataSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(2600);

		if (dash.getEditMode()) {
			sql.append(getEditSelectSql(dash));
		} else {
			sql.append(getCommonSelectSql(dash));
		}
		
		sql.append(getSelectSql(dash));
		sql.append(getJoinSql(dash));
		sql.append(getCommonEndSql(dash));

		log.debug("Financial Data SQL: " + sql.toString());
		
		return sql.toString();
	}
	
	/**
	 * Gets the sql required for the overlay data edit mode
	 * 
	 * @param dash
	 * @return
	 */
	protected StringBuilder getEditSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(700);
		TableType tt = dash.getTableType();
		
		sql.append("select r.REVENUE_ID as ROW_ID, ");
		
		DisplayType dt = dash.getColHeaders().getDisplayType();
		for (int i = 1; i <= getDataYears(dt, dash.getCurrentYear()); i++) {
			sql.append("r").append(i>1 ? i : "").append(".REVENUE_ID as REVENUE_ID_").append(i-1).append(", ");
		}
		
		if (TableType.COMPANY == tt) {
			sql.append("c.SHORT_NM_TXT as ROW_NM, r.COMPANY_ID, ");
		} else {
			// When editing market data for a specific company, we always list 4 levels down in the heirarchy
			int offset = 4;
			
			// Use the appropriate parent in the heirarchy
			sql.append("CASE ");
			for (int i = 7; i > 0; i--) {
				sql.append("WHEN s").append(i).append(".PARENT_ID = ? THEN s").append(i-offset < 1 ? 1 : i-offset).append(".SECTION_NM ");
			}
			sql.append("END as ROW_NM, ");

			sql.append("CASE ");
			for (int i = 7; i > 0; i--) {
				sql.append("WHEN s").append(i).append(".PARENT_ID = ? THEN s").append(i-offset < 1 ? 1 : i-offset).append(".SECTION_ID ");
			}
			sql.append("END as SECT_ID, ");
		}
		
		sql.append("r.REGION_CD, r.YEAR_NO, ");
		
		return sql;
	}
	
	/**
	 * Gets the select part of the query specific to Scenario Overlay data.
	 * This gets both the overlay data for display, and the base data to check for deltas.
	 * 
	 * @param dash
	 * @return
	 */
	@Override
	protected StringBuilder getSelectSql(FinancialDashVO dash) {
		StringBuilder sql = new StringBuilder(1200);
		DisplayType dt = dash.getColHeaders().getDisplayType();
		
		// Gets the sql for selecting the base data in addition to the overlay data
		String superSelect = super.getSelectSql(dash).toString();
		sql.append(StringUtil.replace(superSelect, "as Q", "as " + BASE_PREFIX + "Q")).append(", ");
		
		// Usinig coalesce here to "prefer" the overlay data over the standard data where applicable
		sql.append("sum(coalesce(o.Q1_NO, r.Q1_NO)) as Q1_0, sum(coalesce(o.Q2_NO, r.Q2_NO)) as Q2_0, sum(coalesce(o.Q3_NO, r.Q3_NO)) as Q3_0, sum(coalesce(o.Q4_NO, r.Q4_NO)) as Q4_0, ");
		// Market summation needs to handle skipping empty data from the current quarter in the current year
		// in the comparison year so as to prevent the appearance of large losses in areas that have not yet been reported
		if (TableType.MARKET == dash.getTableType() && dash.getColHeaders().getCalendarYear() == dash.getCurrentYear()) {
			for (int i = 1; i <= 4; i++) {
				if (i == dash.getCurrentQtr()) continue;
				sql.append("sum(coalesce(o2.Q").append(i).append("_NO, r2.Q").append(i).append("_NO)) as Q").append(i).append("_1, ");
			}
			sql.append("sum(case when coalesce(o.Q").append(dash.getCurrentQtr()).append("_NO, r.Q").append(dash.getCurrentQtr()).append("_NO) > 0 then coalesce(o2.Q");
			sql.append(dash.getCurrentQtr()).append("_NO, r2.Q").append(dash.getCurrentQtr()).append("_NO) else 0 end) as Q").append(dash.getCurrentQtr()).append("_1 ");
		} else {
			sql.append("sum(coalesce(o2.Q1_NO, r2.Q1_NO)) as Q1_1, sum(coalesce(o2.Q2_NO, r2.Q2_NO)) as Q2_1, sum(coalesce(o2.Q3_NO, r2.Q3_NO)) as Q3_1, sum(coalesce(o2.Q4_NO, r2.Q4_NO)) as Q4_1 "); // Needed for all column display types to get percent change from prior year
		}
		
		// Add in additional years of data as required by the FD display type
		int dataYears = getDataYears(dt, dash.getCurrentYear());
		for (int yr = 3; yr <= dataYears; yr++) {
			sql.append(", sum(coalesce(o").append(yr).append(".Q1_NO, r").append(yr).append(".Q1_NO)) as Q1_").append(yr-1);
			sql.append(", sum(coalesce(o").append(yr).append(".Q2_NO, r").append(yr).append(".Q2_NO)) as Q2_").append(yr-1);
			sql.append(", sum(coalesce(o").append(yr).append(".Q3_NO, r").append(yr).append(".Q3_NO)) as Q3_").append(yr-1);
			sql.append(", sum(coalesce(o").append(yr).append(".Q4_NO, r").append(yr).append(".Q4_NO)) as Q4_").append(yr-1).append(" ");
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

		// Add in additional years of data as required by the FD display type
		int dataYears = getDataYears(dt, dash.getCurrentYear());
		for (int yr = 3; yr <= dataYears; yr++) {
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r").append(yr).append(" ");
			sql.append("on r.COMPANY_ID = r").append(yr).append(".COMPANY_ID ");
			sql.append("and r.REGION_CD = r").append(yr).append(".REGION_CD ");
			sql.append("and r.SECTION_ID = r").append(yr).append(".SECTION_ID ");
			sql.append("and r.YEAR_NO - ").append(yr-1).append(" = r").append(yr).append(".YEAR_NO ");
			
			sql.append("left join ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY o").append(yr).append(" ");
			sql.append("on r").append(yr).append(".REVENUE_ID = o").append(yr).append(".REVENUE_ID ");
			sql.append("and o").append(yr).append(".SCENARIO_ID = ? ");
		}
		
		return sql;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.fd.FinancialDashBaseAction#updateData(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	protected void updateData(ActionRequest req) throws ActionException {
		String actionPerform = StringUtil.checkVal(req.getParameter("actionPerform"));
		if ("publish".equals(actionPerform)) {
			publishScenario(req);
			return;
		}
		
		log.debug("Updating Scenario Overlay Data Records");
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		JsonObject json = getJsonOverlayData(req.getParameter(OVERLAY_DATA));
		
		// Get list of revenueIds from the json data to create/update overlay data with
		List<String> revenueIds = new ArrayList<>();
		Set<Map.Entry<String, JsonElement>> entries = json.entrySet();
		for (Map.Entry<String, JsonElement> entry: entries) {
			revenueIds.add(entry.getKey());
		}

		// Get any existing scenario overlay records 
		List<FinancialDashScenarioOverlayVO> sovos = getOverlayRecords(revenueIds, scenarioId);
		
		// Remove existing ids with an overlay from the list so we know which revenue records we need
		for (FinancialDashScenarioOverlayVO sovo : sovos) {
			revenueIds.remove(sovo.getRevenueId());
		}
		
		// Get the remaining data from the revenue records to create new overlay(s) from
		if (!revenueIds.isEmpty()) {
			List<FinancialDashRevenueVO> rvos = getRevenueRecords(revenueIds);
			
			// Create scenario overlay vo's from revenue vo's
			for (FinancialDashRevenueVO rvo : rvos) {
				FinancialDashScenarioOverlayVO sovo = new FinancialDashScenarioOverlayVO(rvo);
				sovo.setScenarioId(scenarioId);
				sovos.add(sovo);
			}
		}
		
		// Set changed data in each vo
		setUpdatedOverlayData(sovos, json);
		
		// Save the new & updated overlay data
		saveOverlayData(sovos);
	}
	
	/**
	 * Parses the submitted overlay json data
	 * 
	 * @param overlayData
	 * @return
	 */
	protected JsonObject getJsonOverlayData(String overlayData) {
		JsonObject obj = null;
		
		try {
			JsonElement element = new JsonParser().parse(overlayData);
			obj = element.getAsJsonObject();
		} catch(Exception e) {
			log.error("Problem parsing scenario overlay json data.", e);
		}
		
		return obj;
	}
	
	/**
	 * Updates list of existing vo data with the submitted updates
	 * 
	 * @param sovos
	 * @param json
	 * @throws ActionException
	 */
	protected void setUpdatedOverlayData(List<FinancialDashScenarioOverlayVO> sovos, JsonObject json) throws ActionException {
		for (FinancialDashScenarioOverlayVO sovo : sovos) {
			JsonObject revenueData = json.get(sovo.getRevenueId()).getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> entries = revenueData.entrySet();

			for (Map.Entry<String, JsonElement> entry: entries) {
				if (entry.getKey().startsWith(FinancialDashBaseAction.YEAR_TO_DATE)) continue;
				String quarter = getQuarterFromField(entry.getKey());
				long value = entry.getValue().getAsLong();
				
				try {
					// Dynamically set the specific quarter being updated
					Method method = sovo.getClass().getMethod("set" + quarter + "No", long.class);
					method.invoke(sovo, value);				
				} catch (Exception e) {
					throw new ActionException("Couldn't set updated financial dashboard quarter data to scenario overlay vo.", e);
				}
			}
		}
	}
	
	/**
	 * Saves the new or updated overlay records to the database
	 * 
	 * @param sovos
	 * @throws ActionException
	 */
	protected void saveOverlayData(List<FinancialDashScenarioOverlayVO> sovos) throws ActionException {
		// Create batch data to save
		Map<String, List<Object>> insertValues = new HashMap<>();
		Map<String, List<Object>> updateValues = new HashMap<>();
		
		// Loop through the overlay data and add to the batch insert or update data as appropriate
		for (FinancialDashScenarioOverlayVO sovo : sovos) {
			if (StringUtil.isEmpty(sovo.getOverlayId())) {
				String overlayId = new UUIDGenerator().getUUID();
				
				List<Object> insertData = new ArrayList<>();
				insertData.addAll(Arrays.asList(overlayId, sovo.getCompanyId(), sovo.getScenarioId(), sovo.getRevenueId()));		
				insertData.addAll(Arrays.asList(sovo.getYearNo(), sovo.getQ1No(), sovo.getQ2No(), sovo.getQ3No(), sovo.getQ4No()));	
				insertData.add(Convert.getCurrentTimestamp());
				
				insertValues.put(overlayId, insertData);
			} else {
				List<Object> updateData = new ArrayList<>();
				updateData.addAll(Arrays.asList(sovo.getQ1No(), sovo.getQ2No(), sovo.getQ3No(), sovo.getQ4No()));	
				updateData.addAll(Arrays.asList(Convert.getCurrentTimestamp(), sovo.getOverlayId()));
				
				updateValues.put(sovo.getOverlayId(), updateData);
			}
		}
		
		// Save new/updated records
		try {
			dbp.executeBatch(getOverlayInsertSql(), insertValues);
			dbp.executeBatch(getOverlayUpdateSql(), updateValues);
		} catch (Exception e) {
			throw new ActionException("Couldn't save updated overlay data.", e);
		}
	}
	
	/**
	 * Returns the overlay insert sql for the batch update
	 * 
	 * @return
	 */
	private String getOverlayInsertSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(225);
		
		sql.append("insert into ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY (overlay_id, company_id, ");
		sql.append("scenario_id, revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt) values (?,?,?,?,?,?,?,?,?,?)");
		
		return sql.toString();
	}

	/**
	 * Returns the overlay update sql for the batch update
	 * 
	 * @return
	 */
	private String getOverlayUpdateSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("update ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY ");
		sql.append("set q1_no = ?, q2_no = ?, q3_no = ?, q4_no = ?, update_dt = ? ");
		sql.append("where overlay_id = ? ");
		
		return sql.toString();
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
		List<FinancialDashScenarioOverlayVO> sovos = getOverlayRecords(Arrays.asList(revenueId), scenarioId);

		if (!sovos.isEmpty()) {
			// For a given revenueId & scenarioId, there will only be one record if it exists
			sovo = sovos.get(0);
		}
		
		return sovo;
	}

	/**
	 * Returns a list of scenario overlay records.
	 * 
	 * @param revenueIds
	 * @param scenarioId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<FinancialDashScenarioOverlayVO> getOverlayRecords(List<String> revenueIds, String scenarioId) {
		List<FinancialDashScenarioOverlayVO> sovos = new ArrayList<>();
		
		String sql = getOverlayRecordSql(revenueIds.size());
		List<Object> params = new ArrayList<>();
		params.addAll(revenueIds);
		params.add(scenarioId);
		
		List<?> overlays = dbp.executeSelect(sql, params, new FinancialDashScenarioOverlayVO());
		
		if (!overlays.isEmpty()) {
			sovos = (List<FinancialDashScenarioOverlayVO>) overlays;
		}
		
		return sovos;
	}
	
	/**
	 * Returns the sql necessary for retrieving overlay records
	 * 
	 * @return
	 */
	private String getOverlayRecordSql(int recordCnt) {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		
		sql.append("select * from ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY ");
		sql.append("where REVENUE_ID in (").append(DBUtil.preparedStatmentQuestion(recordCnt)).append(") and SCENARIO_ID = ? ");
		
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
	 * Publishes data from a selected scenario/section/year/region/company/quarter combination.
	 * Only updated data that is currently shown in the dashboard is published. i.e. Any quarter
	 * that is not displayed, does not get published.
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void publishScenario(ActionRequest req) throws ActionException {
		log.debug("Publishing Scenario Overlay Records");

		// Get the relevant options selected on the dashboard
		FinancialDashVO dashVO = new FinancialDashVO();
		dashVO.setSectionId(StringUtil.checkVal(req.getParameter("sectionId")));
		dashVO.setScenarioId(StringUtil.checkVal(req.getParameter("scenarioId")));
		dashVO.addCountryType(StringUtil.checkVal(req.getParameter("pubCountryType")));
		dashVO.setCompanyId(StringUtil.checkVal(req.getParameter("companyId")));
		
		// Get information about the columns to be published
		String displayType = StringUtil.checkVal(req.getParameter("colDisplayType"));
		int calendarYear = Convert.formatInteger(req.getParameter("colCalendarYear"));
		int currentQtr = Convert.formatInteger(req.getParameter("colCurrentQtr"));
		FinancialDashColumnSet columnSet = new FinancialDashColumnSet(displayType, calendarYear, currentQtr);
		dashVO.setColHeaders(columnSet);
		int yearCnt = getDataYears(columnSet.getDisplayType(), columnSet.getCalendarYear()) - 1; // Displayed data is 1 year less than what the dashboard uses for calculations
		
		// We pass scenarioId to the base data for efficiency, to get only the related records
		// that are also in the scenario, rather than getting every record.
		Map<String, FinancialDashRevenueVO> baseData = getBaseData(dashVO, yearCnt);
		Map<String, FinancialDashScenarioOverlayVO> overlayData = getScenarioData(dashVO.getScenarioId(), dashVO, yearCnt);
		
		updateAllScenarios(baseData, overlayData, dashVO, yearCnt);
		updateBaseData(baseData, overlayData, dashVO.getColHeaders());
	}
	
	/**
	 * Gets the existing overlay revenue data for a specific scenario.
	 * 
	 * @param dashVO
	 * @return
	 */
	protected Map<String, FinancialDashScenarioOverlayVO> getScenarioData(String sectionId, FinancialDashVO dashVO, int yearCnt) {
		Map<String, FinancialDashScenarioOverlayVO> overlayData = new HashMap<>();
		
		List<Object> params = new ArrayList<>();
		String sql = getSectionOverlaySql(sectionId, dashVO, yearCnt, params);
		
		List<Object> overlays = dbp.executeSelect(sql, params, new FinancialDashScenarioOverlayVO());
		
		for (Object overlayObj : overlays) {
			FinancialDashScenarioOverlayVO overlay = (FinancialDashScenarioOverlayVO) overlayObj;
			overlayData.put(overlay.getRevenueId(), overlay);
		}
		
		return overlayData;
	}
	
	/**
	 * Returns the sql necessary for retrieving all overlay records
	 * for a given scenario.
	 * 
	 * @param scenarioId - that is being requested, can be different from what is on the dashVO
	 * @param dashVO - dashboard options to use in the record selection
	 * @param params - params for db processor
	 * @return
	 */
	private String getSectionOverlaySql(String scenarioId, FinancialDashVO dashVO, int yearCnt, List<Object> params) {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		
		sql.append("select so.* from ").append(custom).append("BIOMEDGPS_FD_SCENARIO_OVERLAY so ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_FD_REVENUE r on so.REVENUE_ID = r.REVENUE_ID ");
		sql.append("where so.SCENARIO_ID = ? ");
		params.add(scenarioId);
		
		// Make sure to only get the years shown in the dashboard
		sql.append("and ").append(getYearSql(dashVO, yearCnt, params, "so"));
		
		// Filter by company id when requested, otherwise by section id
		if (!StringUtil.isEmpty(dashVO.getCompanyId())) {
			sql.append("and so.company_id = ? ");
			params.add(dashVO.getCompanyId());
		} else {
			sql.append("and r.section_id = ? ");
			params.add(dashVO.getSectionId());
		}
		
		sql.append("and ").append(getRegionSql(dashVO, params));
		
		return sql.toString();
	}
	
	/**
	 * Brings the base data current with the published scenario data.
	 * 
	 * @param baseData
	 * @param overlayData
	 * @throws ActionException 
	 */
	protected void updateBaseData(Map<String, FinancialDashRevenueVO> baseData, Map<String, FinancialDashScenarioOverlayVO> overlayData,
			FinancialDashColumnSet columnSet) throws ActionException {
		
		for (Entry<String, FinancialDashScenarioOverlayVO> entry : overlayData.entrySet()) {
			FinancialDashRevenueVO baseRecord = baseData.get(entry.getKey());
			FinancialDashScenarioOverlayVO overlayRecord = overlayData.get(entry.getKey());
			
			updateBaseDataRecord(baseRecord, overlayRecord, columnSet);
		}
	}
	
	/**
	 * Handles updates of a base data record from an overlay record
	 * 
	 * @param baseRecord
	 * @param overlayRecord
	 * @param columns
	 * @throws ActionException 
	 */
	protected void updateBaseDataRecord(FinancialDashRevenueVO baseRecord, FinancialDashScenarioOverlayVO overlayRecord,
			FinancialDashColumnSet columnSet) throws ActionException {

		// Quarters to update in the base data
		List<String> quarters = new ArrayList<>();
		quarters.addAll(Arrays.asList(FinancialDashBaseAction.QUARTER_1, FinancialDashBaseAction.QUARTER_2, FinancialDashBaseAction.QUARTER_3, FinancialDashBaseAction.QUARTER_4));
		
		// Only save records where data has changed
		boolean saveRecord = false;
		
		try {
			int year = baseRecord.getYearNo();
			Map<String, String> columns = columnSet.getColumns();

			// Update the corresponding base record quarter values with data from the overlay record
			// Overlay records always tie back to an original base record
			for (String quarter : quarters) {
				long baseQtrVal = (long) baseRecord.getClass().getMethod("get" + quarter + "No").invoke(baseRecord);
				long overlayQtrVal = (long) overlayRecord.getClass().getMethod("get" + quarter + "No").invoke(overlayRecord);

				if (baseQtrVal != overlayQtrVal && columns.containsKey(quarter + "-" + year)) {
					Method baseSet = baseRecord.getClass().getMethod("set" + quarter + "No", long.class);
					baseSet.invoke(baseRecord, overlayQtrVal);
					saveRecord = true;
				}
			}
			
			if (saveRecord) {
				dbp.save(baseRecord);
			}
		} catch (Exception e) {
			throw new ActionException("Couldn't save updated base data from overlay.", e);
		}
	}

	/**
	 * Brings all existing scenarios up-to-date with the published scenario data.
	 * 
	 * @param baseData
	 * @param overlayData
	 * @param dashVO
	 * @throws ActionException
	 */
	protected void updateAllScenarios(Map<String, FinancialDashRevenueVO> baseData, Map<String, FinancialDashScenarioOverlayVO> overlayData, FinancialDashVO dashVO, int yearCnt) throws ActionException {
		// Get the overlay's scenario id so we can exclude it. It doesn't need to be updated
		// since this is the one we're updating from.
		Entry<String, FinancialDashScenarioOverlayVO> entry = overlayData.entrySet().iterator().next();
		String overlayScenarioId = entry.getValue().getScenarioId();
		
		// Get the list of scenarios that will need to be updated
		FinancialDashScenarioAction sa = new FinancialDashScenarioAction(this.actionInit);
		sa.setAttributes(this.attributes);
		sa.setDBConnection(dbConn);
		List<FinancialDashScenarioVO> scenarios = sa.getScenarios();
		
		// Loop through each scenario to update
		for (FinancialDashScenarioVO scenario : scenarios) {
			if (scenario.getScenarioId().equals(overlayScenarioId)) {
				continue;
			}
			
			Map<String, FinancialDashScenarioOverlayVO> scenarioData = getScenarioData(scenario.getScenarioId(), dashVO, yearCnt);
			if (!scenarioData.isEmpty()) {
				updateScenario(baseData, overlayData, scenarioData, dashVO.getColHeaders());	
			}
		}
	}

	/**
	 * Updates a scenario based on the new overlay data. If the scenario data has changed as compared
	 * to the base data, we leave it alone per the requirements. If it has not changed, we update it with
	 * the new overlay data.
	 * 
	 * @param baseData
	 * @param overlayData
	 * @param scenarioData
	 * @param scenarioId
	 * @throws ActionException 
	 */
	private void updateScenario(Map<String, FinancialDashRevenueVO> baseData, Map<String, FinancialDashScenarioOverlayVO> overlayData,
			Map<String, FinancialDashScenarioOverlayVO> scenarioData, FinancialDashColumnSet columnSet) throws ActionException {

		// Loop through all the records available in the overlay data
		// and add/update the scenario data as necessary
		for (Entry<String, FinancialDashScenarioOverlayVO> entry : overlayData.entrySet()) {
			FinancialDashRevenueVO baseRecord = baseData.get(entry.getKey());
			FinancialDashScenarioOverlayVO overlayRecord = overlayData.get(entry.getKey());
			FinancialDashScenarioOverlayVO scenarioRecord = scenarioData.get(entry.getKey());
			
			if (scenarioRecord != null) {
				updateScenarioRecord(baseRecord, overlayRecord, scenarioRecord, columnSet);
			}
		}
	}
	
	/**
	 * If the scenario data has changed as compared to the base data, we leave it alone per the requirements.
	 * If it has not changed, we update it with the new overlay data.
	 * 
	 * @param baseRecord
	 * @param overlayRecord
	 * @param scenarioRecord
	 * @param quarters
	 * @throws ActionException 
	 */
	private void updateScenarioRecord(FinancialDashRevenueVO baseRecord, FinancialDashScenarioOverlayVO overlayRecord,
			FinancialDashScenarioOverlayVO scenarioRecord, FinancialDashColumnSet columnSet) throws ActionException {
		
		// Quarters to check for changes
		List<String> quarters = new ArrayList<>();
		quarters.addAll(Arrays.asList(FinancialDashBaseAction.QUARTER_1, FinancialDashBaseAction.QUARTER_2, FinancialDashBaseAction.QUARTER_3, FinancialDashBaseAction.QUARTER_4));
		
		// Only save record if a change was made
		boolean saveRecord = false;
		
		try {
			for (String quarter : quarters) {
				// Dynamically check values for each quarter
				Method rvoGet = baseRecord.getClass().getMethod("get" + quarter + "No");
				long baseVal = (long) rvoGet.invoke(baseRecord);

				Method sovoGet = scenarioRecord.getClass().getMethod("get" + quarter + "No");
				long scenarioVal = (long) sovoGet.invoke(scenarioRecord);
				long overlayVal = (long) sovoGet.invoke(overlayRecord);

				// if base record quarter value is same as scenario record quarter value
				// then replace with overlay record quarter value if it is different and a quarter being updated
				if (baseVal == scenarioVal && scenarioVal != overlayVal && columnSet.getColumns().containsKey(quarter + "-" + scenarioRecord.getYearNo())) {
					Method sovoSet = scenarioRecord.getClass().getMethod("set" + quarter + "No", long.class);
					sovoSet.invoke(scenarioRecord, overlayVal);
					saveRecord = true;
				}
			}

			if (saveRecord) {
				dbp.save(scenarioRecord);
			}
		} catch (Exception e) {
			throw new ActionException("Couldn't save updated scenario data from overlay.", e);
		}
	}
	
	/**
	 * Sets a section in the hierarchy current to the specified quarter. If all siblings are marked at
	 * the same quarter, then the parent get's marked current as well. And this repeats up the tree until
	 * either the top gets marked current, or a level is reached where not all nodes match.
	 * 
	 * @param req
	 */
	protected void setCurrentQtr(ActionRequest req) {
		String sectionId = StringUtil.checkVal(req.getParameter("sectionId"));
		int year = Convert.formatInteger(req.getParameter("currentYear"));
		int qtr = Convert.formatInteger(req.getParameter("currentQtr"));
		
		log.debug("Setting Current Quarter: " + year + "-" + qtr);
		
		// Gets the tree info
		SectionHierarchyAction sha = getHierarchyAction();
		SmarttrakTree tree = getFullHierarchy();
		Node currentNode = tree.findNode(sectionId);
		String updateNodeId;
		
		// Assume equal at the beginning so we can enter the loop
		boolean nodesEqual = true;
		
		// Break out of the loop when we reach the top of the tree,
		// or when not all siblings are equal to the new values.
		while (currentNode != null && nodesEqual) {
			// Update current node
			updateNodeId = currentNode.getNodeId();
			sha.updateFdPublish(updateNodeId, year, qtr);
			
			// Get parent node
			currentNode = tree.findNode(currentNode.getParentId());
			
			// Nothing left to process if null
			if (currentNode == null)
				continue;

			// Get the parent node's children
			List<Node> children = currentNode.getChildren();
			
			// Determine if parent node's children are equal to the new values
			nodesEqual = childrenQtrEqual(children, qtr, year, updateNodeId);
		}
	}
	
	/**
	 * Helper that determines whether current published quarters are all equal to what is being set
	 * 
	 * @param children
	 * @param qtr
	 * @param year
	 * @param updateNodeId
	 * @return
	 */
	private boolean childrenQtrEqual(List<Node> children, int qtr, int year, String updateNodeId) {
		boolean nodesEqual = true;

		for (Node child : children) {
			SectionVO section = (SectionVO) child.getUserObject();

			// If child is not equal to what we're updating (and not the node selected for update),
			// then the nodes as a group are not equal.
			if ((section.getFdPubQtr() != qtr || section.getFdPubYr() != year) && !updateNodeId.equals(child.getNodeId())) {
				nodesEqual = false;
			}
		}
		
		return nodesEqual;
	}
}
