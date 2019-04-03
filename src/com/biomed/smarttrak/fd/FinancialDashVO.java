package com.biomed.smarttrak.fd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.fd.FinancialDashAction.DashType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
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
	private boolean editMode;
	private String scenarioId;
	private String companyId;
	private String companyName;
	private int publishedQtr;
	private int publishedYear;
	private int currentQtr;
	private int currentYear;
	private boolean behindLatest;
	private boolean showEmpty;

	/**
	 * The month offset from the current date, for the financial dashboard to display as current
	 */
	public static final int CURRENT_DT_MONTH_OFFSET = -3;

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

	public FinancialDashVO(ResultSet rs, SmarttrakTree sections, DashType dashType) {
		this();
		setData(rs, sections, dashType);
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
	public void setData(ResultSet rs, SmarttrakTree sections, DashType dashType) {
		FinancialDashDataRowVO row = null;

		boolean allSameQuarter = checkAllSameQuarter(sections);
		String rowId = "";
		DBUtil util = new DBUtil();
		try {
			while (rs.next()) {
				if (row == null || !rowId.equals(rs.getString("row_id"))) {
					row = new FinancialDashDataRowVO(rs, this);
					rowId = row.getPrimaryKey();
					addRow(row);

					//If this is the Public View, calculate Labels.
					if(DashType.COMMON.equals(dashType)) {
						row.setReportingPending(sections, currentQtr, currentYear, allSameQuarter);
					}
				} else {
					row.setColumns(util, rs, this, true);
				}
			}
		} catch (SQLException sqle) {
			log.error("Unable to set financial dashboard row data", sqle);
		}
	}

	/**
	 * Check if all Sections are in the same Published Quarter.
	 * @param sections
	 * @return
	 */
	public boolean checkAllSameQuarter(SmarttrakTree sections) {
		for(Node n : sections.preorderList()) {
			if(n.getUserObject() != null) {
				SectionVO s = (SectionVO) n.getUserObject();
				if(s.getFdPubQtr() != currentQtr) {
					return false;
				}
			}
		}

		return true;
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
		boolean edit = Convert.formatBoolean(req.getParameter("editMode"));
		String scenId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String compId = StringUtil.checkVal(req.getParameter("companyId"));
		showEmpty = Convert.formatBoolean(req.getParameter("showEmpty"));

		if (0 == getCurrentYear()) setCurrentQtrYear();
		Integer calYr = Convert.formatInteger(req.getParameter("calendarYear"), getCurrentYear());

		// Set the parameters
		setTableType(tblType);
		setColHeaders(dispType, calYr);
		for(String countryType : ctryTypes) {
			addCountryType(countryType);
		}
		setSectionId(sectId);
		setEditMode(edit);
		setScenarioId(scenId);
		setCompanyId(compId);

		// Get the year/quarter of what was most recently published for the section being viewed
		SectionVO section = (SectionVO) sections.getRootNode().getUserObject();
		setPublishedQtr(section.getFdPubQtr());
		setPublishedYear(section.getFdPubYr());
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
	 * @return the editMode
	 */
	public boolean getEditMode() {
		return editMode;
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
	 * @return the companyName
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @return the publishedQtr
	 */
	public int getPublishedQtr() {
		return publishedQtr;
	}

	/**
	 * @return the publishedYear
	 */
	public int getPublishedYear() {
		return publishedYear;
	}

	/**
	 * @return the currentQtr
	 */
	public int getCurrentQtr() {
		return currentQtr;
	}

	/**
	 * @return the currentYear
	 */
	public int getCurrentYear() {
		return currentYear;
	}

	/**
	 * @return the behindLatest
	 */
	public boolean isBehindLatest() {
		return behindLatest;
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
		this.colHeaders = new FinancialDashColumnSet(displayType, calendarYear, getCurrentQtr());
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
	 * @param editMode the editMode to set
	 */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
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
	 * @param companyName the companyName to set
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @param publishedQtr the publishedQtr to set
	 */
	public void setPublishedQtr(int publishedQtr) {
		this.publishedQtr = publishedQtr;
	}

	/**
	 * @param publishedYear the publishedYear to set
	 */
	public void setPublishedYear(int publishedYear) {
		this.publishedYear = publishedYear;
	}

	/**
	 * Sets both the current quarter and the current year for the financial dashboard.
	 * 
	 * Current quarter/year is defined as follows: 
	 *     - Admin: 3 months in the past, from the current date
	 *     - Public: Latest system-wide published FD quarter
	 * 
	 * @param req
	 */
	public void setCurrentQtrYear(DashType dashType, SectionVO data) {
		log.debug("Setting Current Quarter - Dash Type: " + dashType.toString());

		if (DashType.ADMIN == dashType) {
			setCurrentQtrYear();
		} else {
			setCurrentQtrYear(data);
		}
	}

	/**
	 * Sets the current quarter/year for the financial dashboard display,
	 * based on the month offset, from the current date.
	 */
	private void setCurrentQtrYear() {
		Date currentDate = Convert.formatDate(new Date(), Calendar.MONTH, CURRENT_DT_MONTH_OFFSET);

		Calendar calendar = new GregorianCalendar();
		calendar.setTime(currentDate);

		// Set the "current" quarter/year
		int month = calendar.get(Calendar.MONTH);
		setCurrentQtr(month/3 + 1);
		setCurrentYear(calendar.get(Calendar.YEAR));
	}

	/**
	 * Sets the current quarter/year for the financial dashboard display,
	 * based on publish data from a section vo.
	 * 
	 * @param data
	 */
	private void setCurrentQtrYear(SectionVO data) {
		setCurrentQtr(data.getFdPubQtr());
		setCurrentYear(data.getFdPubYr());
	}

	/**
	 * @param currentQtr the currentQtr to set
	 */
	public void setCurrentQtr(int currentQtr) {
		this.currentQtr = currentQtr;
	}

	/**
	 * @param currentYear the currentYear to set
	 */
	public void setCurrentYear(int currentYear) {
		this.currentYear = currentYear;
	}

	/**
	 * @param behindLatest the behindLatest to set
	 */
	public void setBehindLatest(boolean behindLatest) {
		this.behindLatest = behindLatest;
	}

	/**
	 * Sets whether the viewed section's published year/qtr is behind the
	 * latest system-wide published year/qtr across all sections.
	 * 
	 * @param data
	 */
	public void setBehindLatest(SectionVO data) {
		if (getPublishedYear() < data.getFdPubYr() || (getPublishedYear() == data.getFdPubYr() && getPublishedQtr() < data.getFdPubQtr()))
			setBehindLatest(true);
		else
			setBehindLatest(false);
	}

	public boolean showEmpty() {
		return showEmpty;
	}

	public void setShowEmpty(boolean showEmpty) {
		this.showEmpty = showEmpty;
	}
}
