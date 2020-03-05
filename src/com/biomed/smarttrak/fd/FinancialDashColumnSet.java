package com.biomed.smarttrak.fd;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

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

public class FinancialDashColumnSet implements Serializable {

	private static final long serialVersionUID = -7706158396915419770L;
	
	/**
	 * First year of history
	 */
	public static final int BEGINNING_YEAR = 2012;

	private Integer calendarYear;
	private int currentQtr;
	private DisplayType displayType;
	private Map<String, String> columns;
	
	/**
	 * Default display type.
	 */
	public static final String DEFAULT_DISPLAY_TYPE = "CURYR";
	
	protected enum DisplayType {
		CURYR("Current Year", 2, true), SIXQTR("Six Quarter Running", 4, true), FOURYR("Four-Year Comparison", 6, true),
		YOY("Year-Over-Year", 3, false), CALYR("Calendar Year", 2, true), EIGHTQTR("Eight Quarter Running", 4, true), ALL("All History", 0, true);
		
		private String name;
		private boolean showAll;
		private int dataYears;
		
		DisplayType(String name, int dataYears, boolean showAll) {
			this.name= name;
			this.dataYears = dataYears;
			this.showAll = showAll;
		}
		
		public String getName() {
			return name;
		}
		
		public int getDataYears() {
			return dataYears;
		}
		
		public boolean getShowAll() {
			return showAll;
		}
	}

	/**
	 * New column set from request
	 * 
	 * @param req
	 */
	public FinancialDashColumnSet(ActionRequest req) {
		this.displayType = DisplayType.valueOf(req.getParameter("displayType"));
		this.calendarYear = Convert.formatInteger(req.getParameter("calendarYear"));
		this.currentQtr = Convert.formatInteger(req.getParameter("currentQtr"));
		this.columns = new LinkedHashMap<>();
		setColumns();
	}
	
	/**
	 * New column set from required values
	 * 
	 * @param displayType
	 * @param calendarYear
	 * @param currentQtr
	 */
	public FinancialDashColumnSet(String displayType, Integer calendarYear, int currentQtr) {
		this.displayType = DisplayType.valueOf(displayType);
		this.calendarYear = calendarYear;
		this.currentQtr = currentQtr;
		this.columns = new LinkedHashMap<>();
		setColumns();
	}
	
	/**
	 * Sets the list of columns
	 */
	private void setColumns() {
		switch(displayType) {
			case SIXQTR:
				this.addQuarterRunningColumns(6);
				break;
			case ALL:
				this.addPreviousAndCurrentYearColumns(calendarYear - BEGINNING_YEAR);
				break;
			case EIGHTQTR:
				this.addQuarterRunningColumns(8);
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
	}
	
	/**
	 * @return the columns
	 */
	public Map<String, String> getColumns() {		
		return columns;
	}

	/**
	 * Adds all columns for a quarter running display type.
	 */
	private void addQuarterRunningColumns(int numColumns) {
		// This represents the earliest possible in a six quarter running
		int quarter = 1;
		int year = calendarYear - 2;
		
		boolean isCurrentQtr = false;
		while (!isCurrentQtr) {
			// Increment the year and quarter
			quarter += 1;
			if (quarter == 5) {
				quarter = 1;
				year += 1;
			}

			// Adds the column to the map
			int twoDigitYr = year % 100;
			this.addColumn(FinancialDashBaseAction.QUARTER + quarter + "-" + year, FinancialDashBaseAction.QUARTER + quarter + twoDigitYr);
			
			// If this is the current quarter, we're done
			if (quarter == currentQtr && year == calendarYear)
				isCurrentQtr = true;

			// If there are too many columns, remove the first one
			if (columns.size() > numColumns)
				columns.remove(columns.entrySet().iterator().next().getKey());
		}
	}
	
	/**
	 * Adds all columns for current year and all years previous,
	 * based on number of years back needed.
	 * 
	 * @param yearsBack
	 */
	private void addPreviousAndCurrentYearColumns(int yearsBack) {
		int quarter = 1;
		int year = calendarYear - yearsBack;
		
		for (int i = 1; i <= 4 * (yearsBack + 1); i++) {
			// Add the quarter column
			int twoDigitYr = year % 100;
			this.addColumn(FinancialDashBaseAction.QUARTER + quarter + "-" + year, FinancialDashBaseAction.QUARTER + quarter + twoDigitYr);
			
			quarter += 1;
			if (quarter == 5) {
				// Add the appropriate year column
				String yearPrefix = year != calendarYear ? FinancialDashBaseAction.CALENDAR_YEAR : FinancialDashBaseAction.YEAR_TO_DATE;
				this.addColumn(yearPrefix + "-" + year, yearPrefix + year);
				
				// Increment to the next quarter
				quarter = 1;
				year += 1;
			}
		}
	}

	/**
	 * Adds all columns for a four-year comparison display type.
	 * Only display complete years. If the current quarter is not 4 the current year
	 * is incomplete and should not be included in the comparison
	 */
	private void addFourYearColumns() {
		if (currentQtr != 4)
			this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 4), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 4));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 3), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 3));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 2), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 2));
		this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + (calendarYear - 1), FinancialDashBaseAction.CALENDAR_YEAR + (calendarYear - 1));
		if (currentQtr == 4) {
			this.addColumn(FinancialDashBaseAction.CALENDAR_YEAR + "-" + calendarYear, FinancialDashBaseAction.CALENDAR_YEAR + calendarYear);
		} else {
			calendarYear -= 1;
		}
	}

	/**
	 * Adds all columns for a year-over-year display type.
	 */
	private void addYearOverYearColumns() {
		Integer twoDigitYr = calendarYear % 100;
		
		this.addColumn(FinancialDashBaseAction.QUARTER + currentQtr + "-" + (calendarYear - 1), FinancialDashBaseAction.QUARTER + currentQtr + (twoDigitYr - 1));
		this.addColumn(FinancialDashBaseAction.QUARTER + currentQtr + "-" + calendarYear, FinancialDashBaseAction.QUARTER + currentQtr + twoDigitYr);
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
	 * @return the currentQtr
	 */
	public int getCurrentQtr() {
		return currentQtr;
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
	 * @param currentQtr the currentQtr to set
	 */
	public void setCurrentQtr(int currentQtr) {
		this.currentQtr = currentQtr;
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
