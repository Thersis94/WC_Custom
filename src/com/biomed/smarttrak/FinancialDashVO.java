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
	private List<CountryType> countryTypes;
	private TableType tableType;
	private FinancialDashColumnSet colHeaders;
	private List<FinancialDashDataRowVO> rows;
	private String sectionId;
	
	/**
	 * Default table type.
	 */
	public static final String DEFAULT_TABLE_TYPE = "MARKET";
	
	private enum TableType {
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
	
	private enum CountryType {
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
		
		FinancialDashDataRowVO row;
		UUIDGenerator uuidGen = new UUIDGenerator();
		for (int i=0; i < 15; i++) {
			row = new FinancialDashDataRowVO();
			row.setName(tableType.getName() + " " + i);
			row.setPrimaryKey(uuidGen.getUUID());
			for (String key : colHeaders.getColumns().keySet()) {
				row.addColumn(key, rand.nextInt(25000), rand.nextDouble());
			}
			this.addRow(row);
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
	 * @return the countryTypes
	 */
	public List<CountryType> getCountryTypes() {
		return countryTypes;
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
		this.countryTypes.add(countryType);
	}

	/**
	 * @param countryType the countryType to add
	 */
	public void addCountryType(String countryType) {
		this.countryTypes.add(CountryType.valueOf(countryType));
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
}
