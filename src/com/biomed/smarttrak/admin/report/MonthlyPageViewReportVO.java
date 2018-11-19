package com.biomed.smarttrak.admin.report;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title:</b> MonthlyPageViewReport.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Formats the Monthly Pageview Report
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2018
 ****************************************************************************/
public class MonthlyPageViewReportVO extends AbstractSBReportVO {

	/**
	 *
	 */
	private static final long serialVersionUID = -9199238750969348996L;
	public static final String DATE_HEADER_KEY = "dateHeaderKey";
	public static final String PAGE_VIEW_DATA_KEY = "pageViewDataKey";
	public static final String START_DT = "startDt";
	public static final String END_DT = "endDt";

	private Collection<MonthlyPageViewVO> pageViews;
	private Set<String> dateHeaders;
	private Date startDt;
	private Date endDt;
	private HSSFWorkbook wb;

	public MonthlyPageViewReportVO() {
		super();
		wb = new HSSFWorkbook();
		setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("MonthlyPageViewReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		//Update the File Name.
		updateName();

		//Instantiate the Workbook and build headers.
		HSSFSheet sheet = wb.createSheet();
		int rowNum = buildHeader(sheet);

		//Build Body Rows
		addDataRows(sheet, rowNum);

		//Return Report.
		return ExcelReport.getBytes(wb);
	}

	/**
	 * Helper method that updates the Report name now that we have data to
	 * piece it together.
	 */
	private void updateName() {
		StringBuilder name = new StringBuilder(200);
		name.append("MonthlyPageViewReport from ");
		name.append(Convert.formatDate(startDt));
		name.append(" - ").append(Convert.formatDate(endDt));
		name.append(".xls");

		setFileName(name.toString());
	}

	/**
	 * Iterate Data rows and write values.
	 * @param sheet
	 * @param rowNum
	 */
	private void addDataRows(HSSFSheet sheet, int rowNum) {

		//Prep style for Url Links.
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle linkStyle = wb.createCellStyle();
        Font linkFont = wb.createFont();
        linkFont.setUnderline(Font.U_SINGLE);
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        linkStyle.setFont(linkFont);

		for (MonthlyPageViewVO pv : pageViews) {
			HSSFRow row = sheet.createRow(rowNum++);
			int total = 0;
			int cellCnt = 0;
			HSSFCell cell = row.createCell(cellCnt++);
			cell.setCellValue(pv.getSectionName());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(pv.getPageTitle());

			//Build Url Link with style
			cell = row.createCell(cellCnt++);
			cell.setCellValue(pv.getRequestUri());
			HSSFHyperlink link = (HSSFHyperlink) createHelper.createHyperlink(Hyperlink.LINK_URL);
			link.setAddress(pv.getRequestUri());
			cell.setHyperlink((HSSFHyperlink)link);
			cell.setCellStyle(linkStyle);

			cell = row.createCell(cellCnt++);
			cell.setCellValue(pv.getPageName());

			//Iterate dynamic headers for each month.
			for(String h : dateHeaders) {
				int val = pv.getPageCounts(h);
				total += val;
				cell = row.createCell(cellCnt++);
				cell.setCellValue(val);
			}

			cell = row.createCell(cellCnt);
			cell.setCellValue(total);
		}
	}

	/**
	 * Build Header Row.
	 * @param sheet
	 * @return
	 */
	private int buildHeader(HSSFSheet sheet) {
		int rowNum = 0;
		int cellNum = 0;
		HSSFRow row = sheet.createRow(rowNum++);
		row.createCell(cellNum++).setCellValue("Site Section");
		row.createCell(cellNum++).setCellValue("Page Title");
		row.createCell(cellNum++).setCellValue("Page URL");
		row.createCell(cellNum++).setCellValue("Name");
		for(String h : dateHeaders) {
			row.createCell(cellNum++).setCellValue(h);
		}

		row.createCell(cellNum).setCellValue("Total");

		return rowNum;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setData(Object o) {

		//Check if this is a map and if so, pull off expected data.
		if(o instanceof Map) {
			pageViews = (Collection<MonthlyPageViewVO>) ((Map)o).get(PAGE_VIEW_DATA_KEY);
			dateHeaders = (Set<String>) ((Map)o).get(DATE_HEADER_KEY);
			startDt = (Date)((Map)o).get(START_DT);
			endDt = (Date)((Map)o).get(END_DT);
		}
	}
}