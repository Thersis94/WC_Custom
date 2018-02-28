package com.biomed.smarttrak.fd;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashImportAction.java<p/>
 * <b>Description: Handles the process of importing and exporting financial dashboard data</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/

public class FinancialDashImportAction extends FinancialDashBaseAction {
	
	public static final String COMPANY_ID = "companyId";
	public static final String SCENARIO_ID = "scenarioId";
	
	public FinancialDashImportAction() {
		super();
	}

	public FinancialDashImportAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Parse the supplied file and apply the changes to the applicable scenario.
	 * @param req
	 * @throws ActionException
	 */
	public void importChanges(ActionRequest req) throws ActionException {
		StringBuilder redirectUrl = new StringBuilder(100);
		redirectUrl.append("manage?actionType=").append(req.getParameter("actionType")).append("&msg=");
		String hash = req.getParameter("returnHash");
		
		try {
			List<FinancialDashRevenueDataRowVO> data = buildFromFile(req);
			List<FinancialDashRevenueDataRowVO> insertList = new ArrayList<>();
			List<FinancialDashRevenueDataRowVO> updateList = new ArrayList<>();
			
			for (FinancialDashRevenueDataRowVO row : data) {
				if (CountryType.WW.toString().equals(row.getRegionCode()) || 
						StringUtil.isEmpty(row.getRevenueId())) continue;
				// Every valid row should have a scenario id.  If not this isn't a scenario save and should be discarded.
				if (StringUtil.isEmpty(row.getScenarioId())) throw new ActionException("Missing Scenario Id. Canceling data import");
				
				if (StringUtil.isEmpty(row.getOverlayId())) {
					insertList.add(row);
				} else {
					updateList.add(row);
				}
			}
			
			DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
			
			if (!insertList.isEmpty()) db.executeBatch(insertList);
			if (!updateList.isEmpty()) db.executeBatch(updateList);

		} catch (Exception e) {
			redirectUrl.append(e.getMessage()).append(hash);
			sendRedirect(redirectUrl.toString(), "", req);
			return;
		}

		redirectUrl.append("Changes sucessfully imported.").append(hash);
		sendRedirect(redirectUrl.toString(), "", req);
	}
	
	
	/**
	 * Build a list of VOs from a supplied file
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private List<FinancialDashRevenueDataRowVO> buildFromFile(ActionRequest req) throws ActionException {
		List<FinancialDashRevenueDataRowVO> items = new ArrayList<>();
		try {
			//Get the file from the byte array
			Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(req.getFile("fdImport").getFileData()));
			//get the first sheet in the workbook
			Sheet sheet = wb.getSheetAt(0);
			FormulaEvaluator objFormulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
			DataFormatter objDefaultFormat = new DataFormatter();
			
			//Used to iterate over each row in the spreadsheet
			Iterator<Row> rowSet = sheet.rowIterator();
			Row cursor = null;
			//Skip the first row, since it just has the header names
			rowSet.next();

			while (rowSet.hasNext()) {
				cursor = rowSet.next();
				int end = cursor.getLastCellNum();
				int cur = FinancialDashImportReportVO.DATA_START_NO;
				
				while (cur < end) {
					FinancialDashRevenueDataRowVO dataRow = new FinancialDashRevenueDataRowVO();
					dataRow.setCompanyId(getCellValue(cursor.getCell(FinancialDashImportReportVO.COMPANY_ID_COL), objDefaultFormat, objFormulaEvaluator));
					dataRow.setRegionCode(getCellValue(cursor.getCell(FinancialDashImportReportVO.REGION_COL), objDefaultFormat, objFormulaEvaluator));
					dataRow.setScenarioId(getCellValue(cursor.getCell(FinancialDashImportReportVO.SCENARIO_ID_COL), objDefaultFormat, objFormulaEvaluator));
					dataRow.setRevenueId(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator));
					dataRow.setOverlayId(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator));
					dataRow.setYearNo(Convert.formatInteger(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator)));
					dataRow.setQ1No(Convert.formatInteger(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator)));
					dataRow.setQ2No(Convert.formatInteger(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator)));
					dataRow.setQ3No(Convert.formatInteger(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator)));
					dataRow.setQ4No(Convert.formatInteger(getCellValue(cursor.getCell(cur++), objDefaultFormat, objFormulaEvaluator)));
					
					items.add(dataRow);
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return items;
	}
	
	
	/**
	 * Get the string value from the supplied cell while accounting for formulae in the cell
	 * @param cell
	 * @param objDefaultFormat
	 * @param objFormulaEvaluator
	 * @return
	 */
	private String getCellValue(Cell cell, DataFormatter objDefaultFormat, FormulaEvaluator objFormulaEvaluator) {
		objFormulaEvaluator.evaluateFormulaCell(cell);
		return objDefaultFormat.formatCellValue(cell, objFormulaEvaluator);
	}
	


	/**
	 * Build the data used to create the export file.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public FinancialDashImportReportVO buildReport(ActionRequest req) throws ActionException {
		FinancialDashImportReportVO report = new FinancialDashImportReportVO();
		String sql = getExportSql(Convert.formatBoolean(req.getParameter("isCompany")), req.hasParameter(COMPANY_ID));
		FinancialDashVO dash = new FinancialDashVO();
		SmarttrakTree sections = getHierarchy(req);
		dash.setData(req, sections);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			int idx = 0;
			log.debug(sql+"|"+req.getParameter(SCENARIO_ID)+"|"+user.getAccountId()+"|"+dash.getSectionId()+"|"+req.getParameter(COMPANY_ID));
			ps.setString(++idx, req.getParameter(SCENARIO_ID));
			ps.setString(++idx, req.getParameter(SCENARIO_ID));
			ps.setString(++idx, req.getParameter(SCENARIO_ID));
			for (int i = 0; i < 7; i++) ps.setString(++idx, user.getAccountId());
			for (int i = 0; i < 7; i++) ps.setString(++idx, dash.getSectionId());
			if (req.hasParameter(COMPANY_ID)) ps.setString(++idx, req.getParameter(COMPANY_ID));

			buildData(ps.executeQuery(), report);
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		return report;
	}
	
	
	/**
	 * Build the sql query used to generate the export report
	 * @param isCompany
	 * @param fullCompany
	 * @return
	 */
	private String getExportSql(boolean isCompany, boolean fullCompany) {
		StringBuilder sql = new StringBuilder(1000);
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("select s2.section_nm as parent_nm, s1.section_nm, c.company_nm, c.company_id, r.year_no, r.region_cd, s1.section_id + c.company_id + region_cd as GROUP_ID, ");
		sql.append("o.overlay_id, r.revenue_id, ? as SCENARIO_ID, scenario_nm, coalesce(o.Q1_NO, r.Q1_NO) as Q1_NO, coalesce(o.Q2_NO, r.Q2_NO) as Q2_NO, ");
		sql.append("coalesce(o.Q3_NO, r.Q3_NO) as Q3_NO, coalesce(o.Q4_NO, r.Q4_NO) as Q4_NO ");
		
		sql.append("from custom.BIOMEDGPS_FD_REVENUE r ");
		sql.append("left join custom.biomedgps_fd_scenario_overlay o on o.revenue_id = r.revenue_id and o.scenario_id = ? ");
		sql.append("left join custom.biomedgps_fd_scenario s on s.scenario_id = ? ");
		getCommonMidSql(sql, custom);
		if (fullCompany) {
			sql.append("and c.company_id = ? ");
		}
		if (isCompany) {
			sql.append("order by company_nm, s1.section_nm, region_cd, year_no");
		} else {
			sql.append("order by s1.section_nm, s1.section_id, c.company_nm, region_cd, year_no");
		}
		
		return sql.toString();		
	}

	
	/**
	 * Turn the result set for the export into usable data.
	 * @param rs
	 * @param report 
	 * @return
	 * @throws ActionException 
	 */
	private void buildData(ResultSet rs, FinancialDashImportReportVO report) throws ActionException {
		String groupId = "";
		List<Map<Integer, List<FinancialDashRevenueDataRowVO>>> data = new ArrayList<>();
		Map<Integer, List<FinancialDashRevenueDataRowVO>> group = new HashMap<>();
		Map<String, Object> reportData = new HashMap<>();
		int minYear = 10000;
		int maxYear = 0;
		DBProcessor db = new DBProcessor(dbConn);
		
		try {
			while (rs.next()) {
				if (!groupId.equals(rs.getString("GROUP_ID"))) {
					groupId = rs.getString("GROUP_ID");
					addData(data, group);
					group = new HashMap<>();
				}
				FinancialDashRevenueDataRowVO row = new FinancialDashRevenueDataRowVO();
				db.executePopulate(row, rs);
				if (minYear > row.getYearNo()) minYear = row.getYearNo();
				if (maxYear < row.getYearNo()) maxYear = row.getYearNo();
				addExportYear(group, row);
			}
			addData(data, group);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		reportData.put("data", data);
		reportData.put("minYear", minYear);
		reportData.put("maxYear", maxYear);
		report.setData(reportData);
	}

	
	/**
	 * Add the data for this year to the data collection
	 * @param group
	 * @param row
	 */
	private void addExportYear(Map<Integer, List<FinancialDashRevenueDataRowVO>> group,
			FinancialDashRevenueDataRowVO row) {
		if (!group.containsKey(row.getYearNo()))
			group.put(row.getYearNo(), new ArrayList<>());
		
		group.get(row.getYearNo()).add(row);
	}

	/**
	 * Add the completed item to the list
	 * @param data
	 * @param group
	 */
	private void addData(List<Map<Integer, List<FinancialDashRevenueDataRowVO>>> data,
			Map<Integer, List<FinancialDashRevenueDataRowVO>> group) {
		if (group.isEmpty()) return;
		data.add(group);
	}
	
}
