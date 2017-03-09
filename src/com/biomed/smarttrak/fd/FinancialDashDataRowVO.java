package com.biomed.smarttrak.fd;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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
	private String regionCd;
	private boolean inactiveFlg;
	private int inactiveCnt; // internal value used to calculate overall inactivity
	private Map<String, FinancialDashDataColumnVO> columns;
	private int reportingQtr;
	private int reportingYr;
	
	/**
	 * Provides a logger
	 */
	protected static Logger log;
	
	public FinancialDashDataRowVO() {
		super();
		columns = new HashMap<>();
		log = Logger.getLogger(getClass());
	}
	
	public FinancialDashDataRowVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		
		setName(util.getStringVal("ROW_NM", rs));
		setPrimaryKey(util.getStringVal("ROW_ID", rs));
		setCompanyId(util.getStringVal("COMPANY_ID", rs));
		setRegionCd(util.getStringVal("REGION_CD", rs));
		setColumns(util, rs);
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
	 * @return the regionCd
	 */
	public String getRegionCd() {
		return regionCd;
	}

	/**
	 * @return the reportingQtr
	 */
	public int getReportingQtr() {
		return reportingQtr;
	}

	/**
	 * @return the reportingYr
	 */
	public int getReportingYr() {
		return reportingYr;
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
	 */
	public void setColumns(DBUtil util, ResultSet rs) {
		
		try {
			int maxYear = util.getIntVal("YEAR_NO", rs);
			Map<Integer, Integer> totals = new HashMap<>();
			
			ResultSetMetaData rsmd;
			rsmd = rs.getMetaData();
			
			int colCount = rsmd.getColumnCount();
			for (int i = 1; i <= colCount; i++) {
				String colName = rsmd.getColumnName(i).toUpperCase();
				String qtr = colName.substring(0,2);
				int yearIdx = Convert.formatInteger(colName.substring(colName.length() - 1, colName.length()));

				switch (qtr) {
					case FinancialDashBaseAction.QUARTER_1:
					case FinancialDashBaseAction.QUARTER_2:
					case FinancialDashBaseAction.QUARTER_3:
					case FinancialDashBaseAction.QUARTER_4:
						addColumn(qtr, yearIdx, maxYear, util, rs);
						incrementTotal(totals, yearIdx, util.getIntVal(colName, rs));
						calculateInactivity(qtr, yearIdx, util, rs);
						break;
					default:
				}
			}
			
			this.addSummaryColumns(totals, maxYear);
		} catch (SQLException sqle) {
			log.error("Unable to set financial dashboard row data columns", sqle);
		}
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
	 * @param regionCd the regionCd to set
	 */
	public void setRegionCd(String regionCd) {
		this.regionCd = regionCd;
	}

	/**
	 * @param reportingQtr the reportingQtr to set
	 */
	public void setReportingQtr(int reportingQtr) {
		this.reportingQtr = reportingQtr;
	}

	/**
	 * @param reportingYr the reportingYr to set
	 */
	public void setReportingYr(int reportingYr) {
		this.reportingYr = reportingYr;
	}

	/**
	 * Sets the reporting year/quarter based on other values in the tree
	 * 
	 * @param tree
	 */
	protected void setReporting(SmarttrakTree tree) {
		Node parentNode = tree.getRootNode();
		List<Node> childNodes = parentNode.getChildren();
		
		SectionVO parent = (SectionVO) parentNode.getUserObject();
		int parentQtr = parent.getFdPubQtr();
		int parentYr = parent.getFdPubYr();
		
		for (Node node : childNodes) {
			SectionVO child = (SectionVO) node.getUserObject();
			int childQtr = child.getFdPubQtr();
			int childYr = child.getFdPubYr();
			
			if (childYr > parentYr || (childYr == parentYr && childQtr > parentQtr)) {
				setReportingQtr(childQtr);
				setReportingYr(childYr);
				break;
			}
		}
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
	 * @param pctDiff
	 */
	public void addColumn(String colId, int val, Double pctDiff) {
		FinancialDashDataColumnVO col = new FinancialDashDataColumnVO();
		col.setDollarValue(val);
		col.setPctDiff(pctDiff);
		col.setColId(colId);
		
		this.addColumn(colId, col);
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
	 */
	private void addColumn(String qtr, int yearIdx, int maxYear, DBUtil util, ResultSet rs) {
		if (yearIdx >= FinancialDashBaseAction.MAX_DATA_YEARS) {
			return;
		}
		
		int dollarValue = util.getIntVal(qtr + "_" + yearIdx, rs);
		int pyDollarValue = util.getIntVal(qtr + "_" + (yearIdx + 1), rs);

		Double pctChange = null;
		if (pyDollarValue > 0) {
			pctChange = (double) (dollarValue - pyDollarValue) / pyDollarValue;
		}
		
		// Subtracting the year index from the most recent year in the query,
		// gives the year for that column. One row in the returned data could
		// represent data from more than one year.
		this.addColumn(qtr + "-" + (maxYear - yearIdx), dollarValue, pctChange);
	}
	
	/**
	 * Makes determination as to whether the company is inactive. When inactive,
	 * the row should not be returned back to the client.
	 * 
	 * @param qtr
	 * @param yearIdx
	 * @param dollarValue
	 */
	private void calculateInactivity(String qtr, int yearIdx, DBUtil util, ResultSet rs) {
		// Inactivity only applies to company rows, not market rows
		// Inactivity is only determined from the first two years of data
		if (StringUtil.isEmpty(getCompanyId()) || yearIdx > 1)
			return;
		
		int dollarValue = util.getIntVal(qtr + "_" + yearIdx, rs);
		
		try {
			// Check for difference between a new record, and actual zero values
			if (rs.wasNull() && yearIdx == 1) {
				return;
			}
		} catch (SQLException sqle) {
			log.error("Unable to calculate company inactivity.", sqle);
		}
		
		checkInactive(qtr, yearIdx, dollarValue);
	}
	
	/**
	 * Checks for company inactivity for the passed quarter
	 * 
	 * @param qtr
	 * @param yearIdx
	 * @param dollarValue
	 */
	private void checkInactive(String qtr, int yearIdx, int dollarValue) {
		switch (qtr) {
			case FinancialDashBaseAction.QUARTER_1:
			case FinancialDashBaseAction.QUARTER_2:
				if (yearIdx == 1)
					break;
			case FinancialDashBaseAction.QUARTER_3:
			case FinancialDashBaseAction.QUARTER_4:
				if (dollarValue == 0)
					inactiveCnt += 1;
				break;
			default:
		}
		
		// If all 6 of the past quarters are zero, this company is inactive
		if (inactiveCnt == 6) {
			setInactive(true);
		}
	}
	
	/**
	 * Creates the summary YTD/CY columns.
	 * 
	 * @param totals
	 * @param maxYear - the most recent year from the query
	 */
	private void addSummaryColumns(Map<Integer, Integer> totals, int maxYear) {
		for (int i = 0; i < totals.size() - 1; i++) {
			Integer cyTotal = totals.get(i);
			Integer pyTotal = totals.get(i + 1);
			
			Double pctChange = null;
			if (pyTotal > 0) {
				pctChange = (double) (cyTotal - pyTotal) / pyTotal;
			}
			
			// Each iteration signifies one year earlier
			this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (maxYear - i), cyTotal, pctChange);
			this.addColumn(FinancialDashBaseAction.YEAR_TO_DATE + "-" + (maxYear - i), cyTotal, pctChange);
		}
	}

	/**
	 * Increments the totals for the summary YTD/CY columns.
	 * 
	 * @param totals
	 * @param key
	 * @param dollarValue
	 */
	private void incrementTotal(Map<Integer, Integer> totals, int key, int dollarValue) {
		if (totals.get(key) == null) {
			totals.put(key, 0);
		}
		
		totals.put(key, totals.get(key) + dollarValue);
	}
}
