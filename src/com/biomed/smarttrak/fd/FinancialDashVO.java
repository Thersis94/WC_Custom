package com.biomed.smarttrak.fd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
	private boolean leafMode;
	private String scenarioId;
	private String companyId;
	private int quarter;
	
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

	public FinancialDashVO(ResultSet rs, SmarttrakTree sections) {
		this();
		setData(rs, sections);
	}
	
	public FinancialDashVO(ActionRequest req, SmarttrakTree sections) {
		this();
		setData(req, sections);
	}
	
	/**
	 * Sets applicable data coming from a ResultSet
	 * 
	 * @param rs
	 */
	public void setData(ResultSet rs, SmarttrakTree sections) {
		FinancialDashDataRowVO row;
		
		try {
			while (rs.next()) {
				row = new FinancialDashDataRowVO(rs);

				if (!row.isInactive()) {
					row.setReporting(sections);
					addRow(row);
				}
			}
		} catch (SQLException sqle) {
			log.error("Unable to set financial dashboard row data", sqle);
		}
	}
	
	/**
	 * Processes the request's options needed for generating the
	 * requested table, chart, or report. Sets defaults where needed.
	 * 
	 * @param req
	 * @param sections
	 */
	public void setData(ActionRequest req, SmarttrakTree sections) {
		// Get the paramters off the request, set defaults where required
		String dispType = StringUtil.checkVal(req.getParameter("displayType"), FinancialDashColumnSet.DEFAULT_DISPLAY_TYPE);
		String tblType = StringUtil.checkVal(req.getParameter("tableType"), FinancialDashVO.DEFAULT_TABLE_TYPE);
		String[] ctryTypes = req.getParameterValues("countryTypes[]") == null ? new String[]{FinancialDashVO.DEFAULT_COUNTRY_TYPE} : req.getParameterValues("countryTypes[]");
		String sectId = StringUtil.checkVal(req.getParameter("sectionId"));
		boolean leafMd = Convert.formatBoolean(req.getParameter("leafMode"));
		String scenId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String compId = StringUtil.checkVal(req.getParameter("companyId"));
		
		// Default year & quarter require knowledge of what was most recently published for the section being viewed
		SectionVO section = (SectionVO) sections.getRootNode().getUserObject();
		Integer calYr = Convert.formatInteger(req.getParameter("calendarYear"), section.getFdPubYr());

		// Set the parameters
		setTableType(tblType);
		setColHeaders(dispType, calYr);
		for(String countryType : ctryTypes) {
			addCountryType(countryType);
		}
		setSectionId(sectId);
		setLeafMode(leafMd);
		setScenarioId(scenId);
		setCompanyId(compId);
		setQuarter(section.getFdPubQtr());
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
	 * @return the leafMode
	 */
	public boolean getLeafMode() {
		return leafMode;
	}

	/**
	 * @return the scenarioId
	 */
	public String getScenarioId() {
		return scenarioId;
	}

	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the quarter
	 */
	public int getQuarter() {
		return quarter;
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
	 * @param leafMode the leafMode to set
	 */
	public void setLeafMode(boolean leafMode) {
		this.leafMode = leafMode;
	}

	/**
	 * @param scenarioId the scenarioId to set
	 */
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}
	
	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param quarter the quarter to set
	 */
	public void setQuarter(int quarter) {
		this.quarter = quarter;
	}
}
