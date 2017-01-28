package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.db.DBUtil;
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
		
		this.setName(util.getStringVal("COMPANY_NM", rs));
		this.setPrimaryKey(util.getStringVal("COMPANY_ID", rs));
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
			// just need the last two digits of the year for the column id
			int year = util.getIntVal("YEAR_NO", rs) % 100;
			int total = 0, pyTotal = 0;
			
			ResultSetMetaData rsmd;
			rsmd = rs.getMetaData();
			
			int colCount = rsmd.getColumnCount();
			for (int i = 1; i <= colCount; i++) {
				String colName = rsmd.getColumnName(i); 
				switch (colName) {
					case "q1_y1":
					case "q2_y1":
					case "q3_y1":
					case "q4_y1":
						String quarter = colName.substring(0,2);
						
						int dollarValue = util.getIntVal(colName, rs);
						total += dollarValue;
						
						int pyDollarValue = util.getIntVal(quarter + "_y2", rs);
						pyTotal += pyDollarValue;

						Double pctChange = null;
						if (pyDollarValue > 0) {
							pctChange = (double) (dollarValue - pyDollarValue) / pyDollarValue;
						}
						
						this.addColumn(quarter + year, dollarValue, pctChange);
						break;
				}
			}
			
			Double pctChange = null;
			if (pyTotal > 0) {
				pctChange = (double) (total - pyTotal) / pyTotal;
			}
			this.addColumn("cy" + year, total, pctChange);
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

}
