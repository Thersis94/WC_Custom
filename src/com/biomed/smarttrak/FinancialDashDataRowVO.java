package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

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

public class FinancialDashDataRowVO extends SBModuleVO {

	private static final long serialVersionUID = 1L;
	private String name;
	private String primaryKey;
	private Map<String, FinancialDashDataColumnVO> columns;
	
	/**
	 * Provides a logger
	 */
	protected static Logger log;
	
	public FinancialDashDataRowVO() {
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
		
		this.setName(util.getStringVal("ROW_NM", rs));
		this.setPrimaryKey(util.getStringVal("ROW_ID", rs));
		this.setColumns(util, rs);
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
					case FinancialDashAction.QUARTER_1:
					case FinancialDashAction.QUARTER_2:
					case FinancialDashAction.QUARTER_3:
					case FinancialDashAction.QUARTER_4:
						this.addColumn(qtr, yearIdx, maxYear, util, rs);
						this.incrementTotal(totals, yearIdx, util.getIntVal(colName, rs));
						break;
					default:
						break;
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
		if (yearIdx >= FinancialDashAction.MAX_DATA_YEARS) {
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
			this.addColumn(FinancialDashAction.CALENDAR_YEAR + "-" + (maxYear - i), cyTotal, pctChange);
			this.addColumn(FinancialDashAction.YEAR_TO_DATE + "-" + (maxYear - i), cyTotal, pctChange);
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
		};
		
		totals.put(key, totals.get(key) + dollarValue);
	}
}
