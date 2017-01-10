package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

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
	private String name = "";
	private Map<String, FinancialDashDataColumnVO> columns;
	
	public FinancialDashDataRowVO() {
		columns = new HashMap<>();
	}
	
	public FinancialDashDataRowVO(ResultSet rs) {
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		
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
	 * Adds a column to the list of columns
	 * 
	 * @param colId
	 * @param pk
	 * @param val
	 * @param pctDiff
	 */
	public void addColumn(String colId, String pk, Integer val, Double pctDiff) {
		FinancialDashDataColumnVO col = new FinancialDashDataColumnVO();
		col.setPrimaryKey(pk);
		col.setDollarValue(val);
		col.setPctDiff(pctDiff);
		
		columns.put(colId, col);
	}

}
