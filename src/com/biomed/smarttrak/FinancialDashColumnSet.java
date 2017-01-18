package com.biomed.smarttrak;

import java.util.LinkedHashMap;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: FinancialDashColumnSet.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 17, 2017
 ****************************************************************************/

public class FinancialDashColumnSet {

	private Integer calendarYear;
	private DisplayType displayType;
	private Map<String, String> columns;
	
	/**
	 * Default display type.
	 */
	public static final String DEFAULT_DISPLAY = "CURYR";
	
	private enum DisplayType {
		CURYR("Current Year"), SIXQTR("Six Quarter Running"), FOURYR("Four-Year Comparison"),
		YOY("Year-Over-Year"), CALYR("Calendar Year");
		
		private String name;
		
		DisplayType(String name) {
			this.name= name;
		}
		
		public String getName() {
			return name;
		}
	}

	public FinancialDashColumnSet(String displayType, Integer calendarYear) {
		this.displayType = DisplayType.valueOf(displayType);
		this.calendarYear = calendarYear;
		this.columns = new LinkedHashMap<>();
	}
	
	public Map<String, String> getColumns() {
		switch(displayType) {
			case SIXQTR:
				this.addSixQuarterColumns();
				break;
			case FOURYR:
				this.addFourYearColumns();
				break;
			case YOY:
				this.addYearOverYearColumns();
				break;
			case CURYR:
			case CALYR:
				this.addCalendarYearColumns();
				break;
		}
		
		return columns;
	}

	/**
	 * TODO: Need the business rules for this display type.
	 * Adds all columns for a six quarter running display type.
	 */
	private void addSixQuarterColumns() {
		Integer lastYrTwoDigit = (calendarYear - 1) % 100;
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn("q3" + lastYrTwoDigit, "Q3" + lastYrTwoDigit);
		this.addColumn("q4" + lastYrTwoDigit, "Q4" + lastYrTwoDigit);
		this.addColumn("q1" + twoDigitYr, "Q1" + twoDigitYr);
		this.addColumn("q2" + twoDigitYr, "Q2" + twoDigitYr);
		this.addColumn("q3" + twoDigitYr, "Q3" + twoDigitYr);
		this.addColumn("q4" + twoDigitYr, "Q4" + twoDigitYr);
	}

	/**
	 * TODO: Need the business rules for this display type.
	 * Adds all columns for a four-year comparison display type.
	 */
	private void addFourYearColumns() {
		this.addColumn("cy" + (calendarYear - 4), "CY" + (calendarYear - 4));
		this.addColumn("cy" + (calendarYear - 3), "CY" + (calendarYear - 3));
		this.addColumn("cy" + (calendarYear - 2), "CY" + (calendarYear - 2));
		this.addColumn("cy" + (calendarYear - 1), "CY" + (calendarYear - 1));
	}

	/**
	 * TODO: Need the business rules for this display type.
	 * Adds all columns for a year-over-year display type.
	 */
	private void addYearOverYearColumns() {
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn("q4" + (twoDigitYr - 1), "Q4" + (twoDigitYr - 1));
		this.addColumn("q4" + twoDigitYr, "Q4" + twoDigitYr);
		this.addColumn("ytd" + (twoDigitYr - 1), "YTD" + (calendarYear - 1));
		this.addColumn("ytd" + twoDigitYr, "YTD" + calendarYear);
	}

	/**
	 * Adds all columns for a calendar year display type.
	 */
	private void addCalendarYearColumns() {
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn("q1" + twoDigitYr, "Q1" + twoDigitYr);
		this.addColumn("q2" + twoDigitYr, "Q2" + twoDigitYr);
		this.addColumn("q3" + twoDigitYr, "Q3" + twoDigitYr);
		this.addColumn("q4" + twoDigitYr, "Q4" + twoDigitYr);
		this.addColumn("cy" + twoDigitYr, "CY" + calendarYear);
	}

	/**
	 * Adds a column to the column set.
	 * 
	 * @param colId
	 * @param name
	 */
	private void addColumn(String colId, String name) {
		columns.put(colId, name);
	}

	/**
	 * @return the calendarYear
	 */
	public Integer getCalendarYear() {
		return calendarYear;
	}

	/**
	 * @return the displayType
	 */
	public DisplayType getDisplayType() {
		return displayType;
	}

	/**
	 * @param calendarYear the calendarYear to set
	 */
	public void setCalendarYear(Integer calendarYear) {
		this.calendarYear = calendarYear;
	}

	/**
	 * @param displayType the displayType to set
	 */
	public void setDisplayType(DisplayType displayType) {
		this.displayType = displayType;
	}
}
