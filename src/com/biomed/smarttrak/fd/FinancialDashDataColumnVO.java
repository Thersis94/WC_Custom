package com.biomed.smarttrak.fd;

import java.io.Serializable;

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

public class FinancialDashDataColumnVO implements Serializable {

	private static final long serialVersionUID = 6516888074396410665L;
	private String colId;
	private int dollarValue;
	private Double pctDiff;
	private ValueDisplayType valueDisplay;
	
	public enum ValueDisplayType {
		REPORTING("Reporting"), PENDING("Pending");
		
		private String title;
		
		ValueDisplayType(String title) {
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
	}
	
	public FinancialDashDataColumnVO() {
		super();
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

	/**
	 * @return the valueDisplay
	 */
	public ValueDisplayType getValueDisplay() {
		return valueDisplay;
	}
	
	/**
	 * @param valueDisplay the valueDisplay to set
	 */
	public void setValueDisplay(ValueDisplayType valueDisplay) {
		this.valueDisplay = valueDisplay;
	}
	
	/**
	 * Sets the value display type based on the current data in the bean.
	 * Per business rules, greater than zero is reporting, otherwise pending.
	 */
	public void setValueDisplay() {
		if (getDollarValue() > 0) {
			setValueDisplay(ValueDisplayType.REPORTING);
		} else {
			setValueDisplay(ValueDisplayType.PENDING);
		}
	}
}
