package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/

public class FinancialDashVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String nameCol = "";
	private Map<String, String> colHeaders;
	private List<FinancialDashDataRowVO> rows;

	public FinancialDashVO() {
		colHeaders = new LinkedHashMap<>();
		rows = new ArrayList<>();
	}

	public FinancialDashVO(ResultSet rs) {
		super(rs);
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		
	}
	
	// TODO: Remove this after there is real data to work with.
	public void setTempData() {
		Random rand = new Random();
		
		this.setNameCol("Company / Partner");
		this.addColHeader("q116", "Q116");
		this.addColHeader("q216", "Q216");
		this.addColHeader("q316", "Q316");
		this.addColHeader("q416", "Q416");
		this.addColHeader("cy16", "CY2016");
		
		FinancialDashDataRowVO row;
		UUIDGenerator uuidGen = new UUIDGenerator();
		for (int i=0; i < 15; i++) {
			row = new FinancialDashDataRowVO();
			row.setName("Company " + i);
			for (String key : colHeaders.keySet()) {
				row.addColumn(key, uuidGen.getUUID(), rand.nextInt(25000), rand.nextDouble());
			}
			this.addRow(row);
		}
	}

	/**
	 * @return the colHeaders
	 */
	public Map<String, String> getColHeaders() {
		return colHeaders;
	}

	/**
	 * @return the rows
	 */
	public List<FinancialDashDataRowVO> getRows() {
		return rows;
	}

	/**
	 * @return the nameCol
	 */
	public String getNameCol() {
		return nameCol;
	}

	/**
	 * @param colHeaders the colHeaders to set
	 */
	public void setColHeaders(Map<String, String> colHeaders) {
		this.colHeaders = colHeaders;
	}
	
	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<FinancialDashDataRowVO> rows) {
		this.rows = rows;
	}
	
	/**
	 * @param nameCol the nameCol to set
	 */
	public void setNameCol(String nameCol) {
		this.nameCol = nameCol;
	}

	/**
	 * Adds a column header to the header list
	 * 
	 * @param order
	 * @param colId
	 */
	public void addColHeader(String colId, String name) {
		colHeaders.put(colId, name);
	}

	/**
	 * Adds a row to the list of rows
	 * 
	 * @param row
	 */
	public void addRow(FinancialDashDataRowVO row) {
		rows.add(row);
	}
}
