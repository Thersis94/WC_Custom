package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

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
	private List<CountryType> countryTypes;			// used for data aggregation
	private List<CountryType> selectedCountryTypes;	// used for selecting items on a menu
	private TableType tableType;
	private FinancialDashColumnSet colHeaders;
	private List<FinancialDashDataRowVO> rows;
	private String sectionId;
	private String scenarioId;
	
	/**
	 * Provides a logger
	 */
	protected static Logger log;

	/**
	 * Default table type.
	 */
	public static final String DEFAULT_TABLE_TYPE = "MARKET";
	
	protected enum TableType {
		MARKET("Market"), COMPANY("Company / Partner");
		
		private String name;
		
		TableType(String name) {
			this.name= name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	/**
	 * Default country type.
	 */
	public static final String DEFAULT_COUNTRY_TYPE = "US";
	
	protected enum CountryType {
		US("United States"), EU("European Union"), ROW("Rest-of-World"), WW("World-Wide");
		
		private String name;
		
		CountryType(String name) {
			this.name= name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public FinancialDashVO() {
		countryTypes = new ArrayList<>();
		selectedCountryTypes = new ArrayList<>();
		rows = new ArrayList<>();
		log = Logger.getLogger(getClass());
	}

	public FinancialDashVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		FinancialDashDataRowVO row;
		try {
			while (rs.next()) {
				row = new FinancialDashDataRowVO(rs);
				this.addRow(row);
			}
		} catch (SQLException sqle) {
			log.error("Unable to set financial dashboard row data", sqle);
		}
	}
	
	/**
	 * @return the colHeaders
	 */
	public FinancialDashColumnSet getColHeaders() {
		return colHeaders;
	}

	/**
	 * @return the rows
	 */
	public List<FinancialDashDataRowVO> getRows() {
		return rows;
	}

	/**
	 * Country types for data aggregation.
	 * 
	 * @return the countryTypes
	 */
	public List<CountryType> getCountryTypes() {
		return countryTypes;
	}

	/**
	 * Country types for menu display.
	 * Example: WW aggregates US, EU, and ROW, but we only want to show WW on the menu.
	 * 
	 * @return the selectedCountryTypes
	 */
	public List<CountryType> getSelectedCountryTypes() {
		return selectedCountryTypes;
	}

	/**
	 * @return the tableType
	 */
	public TableType getTableType() {
		return tableType;
	}

	/**
	 * @return the tableTypeName
	 */
	public String getTableTypeName() {
		return tableType.getName();
	}

	/**
	 * @return the sectionId
	 */
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the scenarioId
	 */
	public String getScenarioId() {
		return scenarioId;
	}

	/**
	 * @param colHeaders the colHeaders to set
	 */
	public void setColHeaders(FinancialDashColumnSet colHeaders) {
		this.colHeaders = colHeaders;
	}
	
	/**
	 * Sets a list of columns for the dynamically generated tables/columns,
	 * based on the passed display type.
	 * 
	 * @param displayType
	 * @param calendarYear
	 */
	public void setColHeaders(String displayType, Integer calendarYear) {
		this.colHeaders = new FinancialDashColumnSet(displayType, calendarYear);
	}

	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<FinancialDashDataRowVO> rows) {
		this.rows = rows;
	}
	
	/**
	 * @param countryTypes the countryTypes to set
	 */
	public void setCountryTypes(List<CountryType> countryTypes) {
		this.countryTypes = countryTypes;
	}

	/**
	 * @param countryType the countryType to add
	 */
	public void addCountryType(CountryType countryType) {
		if (countryType == CountryType.WW) {
			this.countryTypes.add(CountryType.US);
			this.countryTypes.add(CountryType.EU);
			this.countryTypes.add(CountryType.ROW);
		} else {
			this.countryTypes.add(countryType);
		}

		// Allows for selecting only specific items on the menu.
		// Since WW aggregates the US/EU/ROW data, we only want WW selected on the menu.
		this.selectedCountryTypes.add(countryType);
	}

	/**
	 * @param countryType the countryType to add
	 */
	public void addCountryType(String countryType) {
		this.addCountryType(CountryType.valueOf(countryType));
	}

	/**
	 * @param tableType the tableType to set
	 */
	public void setTableType(TableType tableType) {
		this.tableType = tableType;
	}

	/**
	 * @param tableType the tableType to set
	 */
	public void setTableType(String tableType) {
		this.tableType = TableType.valueOf(tableType);
	}

	/**
	 * Adds a row to the list of rows
	 * 
	 * @param row
	 */
	public void addRow(FinancialDashDataRowVO row) {
		rows.add(row);
	}

	/**
	 * @param sectionId the sectionId to set
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}
	
	/**
	 * @param scenarioId the scenarioId to set
	 */
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}
}
