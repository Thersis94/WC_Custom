package com.biomed.smarttrak.admin.report;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.siliconmtn.data.report.ExcelReport;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
<p><b>Title</b>: EmailMetricsReportVO.java</p>
<p><b>Description: builds the report data from the supplied email data</b></p>
<p> 
<p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Eric Damschroder
@version 1.0
@since Mar 23, 2018
<b>Changes:</b> 
***************************************************************************/

public class EmailMetricsReportVO extends AbstractSBReportVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<EmailMetricsVO> emails;
	HSSFWorkbook wb;
	DecimalFormat percentFormat;

	public EmailMetricsReportVO(Map<String, Object> data) {
		setData(data);
		wb = new HSSFWorkbook();
        percentFormat = new DecimalFormat("##.##%");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		HSSFSheet sheet = wb.createSheet();
		int rowNum = buildHeader(sheet);

		addDataRows(sheet, rowNum);

		return ExcelReport.getBytes(wb);
	}


	/**
	 * Build the header row
	 * @param sheet
	 * @return
	 */
	private int buildHeader(HSSFSheet sheet) {
		int rowNum = 0;
		int cellNum = 0;
		HSSFRow row = sheet.createRow(rowNum++);
		row.createCell(cellNum++).setCellValue("Account");
		row.createCell(cellNum++).setCellValue("Campaign");
		row.createCell(cellNum++).setCellValue("Sent To");
		row.createCell(cellNum++).setCellValue("Emails Sent");
		row.createCell(cellNum++).setCellValue("Bounce");
		row.createCell(cellNum++).setCellValue("Reason");
		row.createCell(cellNum++).setCellValue("Opens");
		row.createCell(cellNum).setCellValue("Open Rate");

		return rowNum;
	}

	/**
	 * Loop over the emails to create the body rows of the report
	 * @param sheet
	 */
	protected void addDataRows(HSSFSheet sheet, int rowNum) {
		for (EmailMetricsVO metric : emails) {
			HSSFRow row = sheet.createRow(rowNum++);

			int cellCnt = 0;
			HSSFCell cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getAccountName());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getCampaignName());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getEmailAddress());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getTotal());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getFails() > 0? "Yes":"No");
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getNotesText());
			cell = row.createCell(cellCnt++);
			cell.setCellValue(metric.getOpens());
			cell = row.createCell(cellCnt);
			cell.setCellValue(percentFormat.format((double)metric.getOpens()/metric.getTotal()));
		}
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<String, Object> data = (Map<String, Object>)o;
		emails = (List<EmailMetricsVO>) data.get("emails");
	}
}