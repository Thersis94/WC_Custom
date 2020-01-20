package com.biomed.smarttrak.fd;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.fd.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FinancialDashDataRowVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/
public class FinancialDashDataRowVO implements Serializable {

	private static final long serialVersionUID = -1858035677710604733L;
	private String name;
	private String primaryKey;
	private String companyId;
	private String sectionId;
	private String regionCd;
	private String graphColor;
	private boolean inactiveFlg;
	private int activeCnt; // internal value used to calculate overall inactivity
	private Map<String, FinancialDashDataColumnVO> columns;

	/**
	 * Provides a logger
	 */
	protected static Logger log = Logger.getLogger(FinancialDashDataRowVO.class);

	public FinancialDashDataRowVO() {
		super();
		columns = new HashMap<>();
	}

	public FinancialDashDataRowVO(ResultSet rs, FinancialDashVO dashboard) throws SQLException {
		this();
		setData(rs, dashboard);
	}

	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs, FinancialDashVO dashboard) throws SQLException {
		DBUtil util = new DBUtil();

		setName(util.getStringVal("ROW_NM", rs));
		setPrimaryKey(util.getStringVal("ROW_ID", rs));

		// These only come from the edit version of the query
		setCompanyId(util.getStringVal("COMPANY_ID", rs));
		setSectionId(util.getStringVal("SECT_ID", rs));
		setRegionCd(util.getStringVal("REGION_CD", rs));
		setGraphColor(util.getStringVal("GRAPH_COLOR", rs));

		setColumns(util, rs, dashboard);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the columns
	 */
	public Map<String, FinancialDashDataColumnVO> getColumns() {
		return columns;
	}

	/**
	 * @return the primaryKey
	 */
	public String getPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the sectionId
	 */
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the regionCd
	 */
	public String getRegionCd() {
		return regionCd;
	}

	public String getGraphColor() {
		return graphColor;
	}

	public void setGraphColor(String graphColor) {
		this.graphColor = graphColor;
	}

	/**
	 * @return the inactiveFlg
	 */
	public boolean isInactive() {
		return inactiveFlg;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param columns the columns to set
	 */
	public void setColumns(Map<String, FinancialDashDataColumnVO> columns) {
		this.columns = columns;
	}

	/**
	 *  Sets all financial data columns found in the result set
	 * 
	 * @param util
	 * @param rs
	 * @throws SQLException 
	 */
	public void setColumns(DBUtil util, ResultSet rs, FinancialDashVO dashboard) throws SQLException {
		int maxYear = util.getIntVal("YEAR_NO", rs);
		int year = dashboard.getCurrentYear();

		ResultSetMetaData rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		int pubQtr = dashboard.isSimulatedQuarter()? dashboard.getPublishedQtr() : rs.getInt("fd_pub_qtr");
		for (int i=1; i <= colCount; i++) {
			String colName = rsmd.getColumnName(i).toUpperCase();
			String qtr = colName.substring(0,2);
			int currQtr = Convert.formatInteger(qtr.substring(qtr.length() - 1, qtr.length()));

			if (FinancialDashBaseAction.QTR_PATTERN.matcher(qtr).matches()) {
				int yearIdx = Convert.formatInteger(colName.substring(colName.length() - 1, colName.length()));
				// If we are in the current year always compar the the current year
				// so that unreported quarters don't get used for the year to date comparison.
				boolean addPrevious = (year != maxYear || yearIdx != 0 || currQtr <= pubQtr) && (dashboard.getColHeaders().getDisplayType().getShowAll() || currQtr <= pubQtr);
				boolean addSummation = (!dashboard.getColHeaders().getDisplayType().getShowAll() && currQtr <= pubQtr) ||
						(dashboard.getColHeaders().getDisplayType().getShowAll() && (year != maxYear || yearIdx != 0 || currQtr <= pubQtr));
				addColumn(qtr, yearIdx, maxYear, util, rs, addPrevious, true);

				addSummaryColumn(util, qtr, maxYear, yearIdx, rs, FinancialDashBaseAction.CALENDAR_YEAR, addPrevious, addSummation);
				addSummaryColumn(util, qtr, maxYear, yearIdx, rs, FinancialDashBaseAction.YEAR_TO_DATE, addPrevious, addSummation);
				calculateInactivity(qtr, yearIdx, util, rs, dashboard.getColHeaders(), qtr + "-" + (maxYear-yearIdx), dashboard.showEmpty());
			}
		}
	}
	
	/**
	 * Create or update the requested summary column with the current row data
	 * @param util
	 * @param qtr
	 * @param maxYear
	 * @param yearIdx
	 * @param rs
	 * @param columnPrefix
	 * @param adjust
	 */
	private void addSummaryColumn(DBUtil util, String qtr, int maxYear,  int yearIdx, ResultSet rs, String columnPrefix, boolean addPrevious, boolean addCurrent) {
		int dollarValue = util.getIntVal(qtr + "_" + yearIdx, rs);
		int pyDollarValue = util.getIntVal(qtr + "_" + (yearIdx + 1), rs);
		addColumn(columnPrefix + "-" + (maxYear - yearIdx), dollarValue, pyDollarValue, util.getStringVal("REVENUE_ID_" + yearIdx, rs), addPrevious, addCurrent);
	}

	/**
	 * @param primaryKey the primaryKey to set
	 */
	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param sectionId the sectionId to set
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param regionCd the regionCd to set
	 */
	public void setRegionCd(String regionCd) {
		this.regionCd = regionCd;
	}

	/**
	 * Per the defined business rules:
	 * Otherwise if a Section is behind the current published level, apply labeling to those only.
	 * If any revenue data exists, then the term "Reporting" is displayed.
	 * If there is no revenue for the current quarter, then the term "Pending" is displayed.
	 *
	 * @param tree
	 * @param reportedQtr
	 * @param currentYear
	 */
	protected void setReportingPending(SmarttrakTree tree, int reportedQtr, int currentQtr, int currentYear) {
		Node node = tree.findNode(primaryKey);

		// If node isn't found, this is a company row, and the value will be displayed
		if (node != null) {
			SectionVO section = (SectionVO) node.getUserObject();
			/*
			 * If the current quarter is less than or equal to the latest quarter
			 * with reporter sales or the current section's published year is earlier
			 * the currently published year mark the column as reporting or pending
			 */
			log.debug(currentQtr+":|"+reportedQtr);
			if (currentQtr < reportedQtr || section.getFdPubYr() < currentYear) {
				markColumnReportingPending(reportedQtr, currentYear);
			}
		}
	}

	/**
	 * Marks the column associated to the current qtr/year as reporting or pending
	 * 
	 * @param currentQtr
	 * @param currentYear
	 */
	protected void markColumnReportingPending(int currentQtr, int currentYear) {
		// Find the column in the map that matches the current year/qtr
		FinancialDashDataColumnVO currentCol = columns.get(FinancialDashBaseAction.QUARTER + currentQtr + "-" + currentYear);

		// May be null here if we are currently viewing a different time period
		if (currentCol != null)
			currentCol.setValueDisplay();
	}

	/**
	 * @param inactiveFlg the inactiveFlg to set
	 */
	public void setInactive(boolean inactiveFlg) {
		this.inactiveFlg = inactiveFlg;
	}

	/**
	 * Adds a column to the map of columns
	 * 
	 * @param colId
	 * @param pk
	 * @param val
	 */
	public void addColumn(String colId, int val, int pVal, String revenueId, boolean addPrevious, boolean addCurrent) {
		FinancialDashDataColumnVO col;
		if (columns.containsKey(colId)) {
			col =columns.get(colId);
			if (addCurrent) col.setDollarValue(col.getDollarValue() + val);
			if (addPrevious) col.setPDollarValue(col.getPDollarValue() +pVal);
		} else {
			col = new FinancialDashDataColumnVO();
			if (addCurrent) col.setDollarValue(val);
			if (addPrevious) col.setPDollarValue(pVal);
			col.setColId(colId);
			col.setRevenueId(revenueId);
		}
		addColumn(colId, col);
	}

	/**
	 * Adds a column to the map of columns
	 * 
	 * @param colId
	 * @param col
	 */
	public void addColumn(String colId, FinancialDashDataColumnVO col) {
		columns.put(colId, col);
	}

	/**
	 * Adds a column to the map of columns while adding to the total year value
	 * 
	 * @param qtr
	 * @param yearIdx - each increment of the year index represents an earlier year in the query results
	 * @param maxYear - the most recent year in the query
	 * @param util
	 * @param rs
	 * @param b 
	 */
	private void addColumn(String qtr, int yearIdx, int maxYear, DBUtil util, ResultSet rs, boolean addPrevious, boolean addCurrent) {
		int dollarValue = util.getIntVal(qtr + "_" + yearIdx, rs);
		int pyDollarValue = util.getIntVal(qtr + "_" + (yearIdx + 1), rs);

		// Subtracting the year index from the most recent year in the query,
		// gives the year for that column. One row in the returned data could
		// represent data from more than one year.
		String columnId = qtr + "-" + (maxYear - yearIdx);
		addColumn(columnId, dollarValue, pyDollarValue, util.getStringVal("REVENUE_ID_" + yearIdx, rs), addPrevious, addCurrent);

		// Checks for potential delta between overlay and base data 
		checkOverlayDelta(columnId, qtr, yearIdx, rs);
	}

	/**
	 * Checks for deltas between base and scenario overlay data.
	 * 
	 * @param id
	 * @param qtr
	 * @param yearIdx
	 * @param rs
	 */
	protected void checkOverlayDelta(String id, String qtr, int yearIdx, ResultSet rs) {
		try {
			int baseValue = rs.getInt(FinancialDashScenarioOverlayAction.BASE_PREFIX + qtr + "_" + yearIdx);
			int overlayValue = rs.getInt(qtr + "_" + yearIdx);

			FinancialDashDataColumnVO currentCol = columns.get(id);
			if (baseValue != overlayValue)
				currentCol.setDelta(true);

		} catch (Exception e) {
			// base value not found in the result set
			// intentionally buried... if we make it here, then we aren't looking at a scenario, there is no delta
		}
	}

	/**
	 * Makes determination as to whether the company is inactive. When inactive,
	 * the row should not be returned back to the client.
	 * 
	 * @param qtr
	 * @param yearIdx
	 * @param showEmpty 
	 * @param dollarValue
	 * @throws SQLException 
	 */
	private void calculateInactivity(String qtr, int yearIdx, DBUtil util, ResultSet rs, 
			FinancialDashColumnSet headers, String displayColNm, boolean showEmpty) throws SQLException {
		if (showEmpty) {
			setInactive(false);
			return;
		}
		// Inactivity only applies to company rows, not market rows
		// Inactivity is onlycalcuated against columns contains Quarterly FD data
		if (StringUtil.isEmpty(getCompanyId()) || !FinancialDashBaseAction.QTR_PATTERN.matcher(qtr).matches())
			return;

		int dollarValue = util.getIntVal(qtr + "_" + yearIdx, rs);

		// Check for difference between a new company record (with no previous years), and actual zero values
		if (rs.wasNull())
			return;
		
		boolean needCol = headers.getColumns().containsKey(displayColNm) || DisplayType.FOURYR == headers.getDisplayType();
		if (needCol && dollarValue > 0)
			++activeCnt;

		// If all of the displayable quarters are zero, this company is inactive
		setInactive(activeCnt == 0);
	}

	/**
	 * Increments the totals for the summary YTD/CY columns.
	 * 
	 * @param totals
	 * @param yearIdx
	 * @param dollarValue
	 * @param curYrColId - passed when you want to adjust totals for previous years based on current year
	 */
	protected void incrementTotal(Map<Integer, Integer> totals, int yearIdx, int dollarValue, String curYrColId) {
		if (totals.get(yearIdx) == null)
			totals.put(yearIdx, 0);

		boolean adjustForIncompleteYear = curYrColId != null;
		// Run through a series of checks to see if the current 
		// quarter should be added to the totals. This prevents fewer than
		// four quarters of sales in the current year from being compared
		// to a previous year's full compliment of profits.
		int addDollarValue = dollarValue;
		if (adjustForIncompleteYear && columns.get(curYrColId).getDollarValue() == 0) {
			addDollarValue = 0;
		}

		totals.put(yearIdx, totals.get(yearIdx) + addDollarValue);
	}
}
