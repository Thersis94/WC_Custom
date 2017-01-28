package com.biomed.smarttrak;

import java.sql.ResultSet;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashDataColumnVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/

public class FinancialDashDataColumnVO extends SBModuleVO {

	private static final long serialVersionUID = 1L;
	private String colId;
	private int dollarValue;
	private Double pctDiff;
	
	public FinancialDashDataColumnVO() {
	}
	
	public FinancialDashDataColumnVO(ResultSet rs) {
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		
	}

	/**
	 * @return the colId
	 */
	public String getColId() {
		return colId;
	}

	/**
	 * @return the dollarValue
	 */
	public int getDollarValue() {
		return dollarValue;
	}

	/**
	 * @return the pctDiff
	 */
	public Double getPctDiff() {
		return pctDiff;
	}
	
	/**
	 * Since the JSON parser converts null Doubles to zero, this will carry through whether
	 * it was in fact a null value or really is zero.
	 * 
	 * @return
	 */
	public boolean getPctDiffIsNull() {
		return pctDiff == null;
	}

	/**
	 * @param colId the colId to set
	 */
	public void setColId(String colId) {
		this.colId = colId;
	}

	/**
	 * @param dollarValue the dollarValue to set
	 */
	public void setDollarValue(int dollarValue) {
		this.dollarValue = dollarValue;
	}

	/**
	 * @param pctDiff the pctDiff to set
	 */
	public void setPctDiff(Double pctDiff) {
		this.pctDiff = pctDiff;
	}
}
