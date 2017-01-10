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
	private String primaryKey;
	private Integer dollarValue;
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
	 * @return the primaryKey
	 */
	public String getPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @return the dollarValue
	 */
	public Integer getDollarValue() {
		return dollarValue;
	}

	/**
	 * @return the pctDiff
	 */
	public Double getPctDiff() {
		return pctDiff;
	}

	/**
	 * @param primaryKey the primaryKey to set
	 */
	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * @param dollarValue the dollarValue to set
	 */
	public void setDollarValue(Integer dollarValue) {
		this.dollarValue = dollarValue;
	}

	/**
	 * @param pctDiff the pctDiff to set
	 */
	public void setPctDiff(Double pctDiff) {
		this.pctDiff = pctDiff;
	}
}
