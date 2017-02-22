package com.biomed.smarttrak;

import java.util.LinkedHashMap;
import java.util.Map;

import com.smt.sitebuilder.action.SBModuleVO;

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

public class FinancialDashColumnSet extends SBModuleVO {

	private static final long serialVersionUID = -7706158396915419770L;

	private Integer calendarYear;
	private DisplayType displayType;
	private Map<String, String> columns;
	
	/**
	 * Default display type.
	 */
	public static final String DEFAULT_DISPLAY_TYPE = "CURYR";
	
	protected enum DisplayType {
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
		
		this.addColumn(FinancialDashBaseAction.QUARTER_3 + "-" + (calendarYear - 1), FinancialDashBaseAction.QUARTER_3 + lastYrTwoDigit);
		this.addColumn(FinancialDashBaseAction.QUARTER_4 + "-" + (calendarYear - 1), FinancialDashBaseAction.QUARTER_4 + lastYrTwoDigit);
		this.addColumn(FinancialDashBaseAction.QUARTER_1 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_1 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_2 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_2  + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_3 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_3 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_4 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_4 + twoDigitYr);
	}

	/**
	 * TODO: Need the business rules for this display type.
	 * Adds all columns for a four-year comparison display type.
	 */
	private void addFourYearColumns() {
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 3), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 3));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 2), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 2));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 1), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 1));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + calendarYear, FinancialDashBaseAction.CALENDAR_YEAR + calendarYear);
	}

	/**
	 * TODO: Need the business rules for this display type.
	 * Adds all columns for a year-over-year display type.
	 */
	private void addYearOverYearColumns() {
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn(FinancialDashBaseAction.QUARTER_4 + "-" + (calendarYear - 1), FinancialDashBaseAction.QUARTER_4 + (twoDigitYr - 1));
		this.addColumn(FinancialDashBaseAction.QUARTER_4 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_4 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.YEAR_TO_DATE + "-" + (calendarYear - 1), FinancialDashBaseAction.YEAR_TO_DATE + (calendarYear - 1));
		this.addColumn(FinancialDashBaseAction.YEAR_TO_DATE + "-" + calendarYear, FinancialDashBaseAction.YEAR_TO_DATE + calendarYear);
	}

	/**
	 * Adds all columns for a calendar year display type or a current year display type.
	 */
	private void addCalendarYearColumns() {
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn(FinancialDashBaseAction.QUARTER_1 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_1 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_2 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_2 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_3 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_3 + twoDigitYr);
		this.addColumn(FinancialDashBaseAction.QUARTER_4 + "-" + calendarYear, FinancialDashBaseAction.QUARTER_4 + twoDigitYr);
		
		if (this.getDisplayType() == DisplayType.CURYR) {
			this.addColumn(FinancialDashBaseAction.YEAR_TO_DATE + "-" + calendarYear, FinancialDashBaseAction.YEAR_TO_DATE + calendarYear);
		} else {
			this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + calendarYear, FinancialDashBaseAction.CALENDAR_YEAR + calendarYear);
		}
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
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayType.getName();
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

	/**
	 * @param displayType the displayType to set
	 */
	public void setDisplayType(String displayType) {
		this.displayType = DisplayType.valueOf(displayType);
	}
}
