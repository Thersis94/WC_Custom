package com.biomed.smarttrak.admin.report;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import com.biomed.smarttrak.vo.AccountVO;
import com.siliconmtn.data.report.ExcelReport;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: AccountCountReportVO.java</p>
 <p><b>Description: </b>Create an excel document listing accounts and
 the number users in each seat type.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Eric Damschroder
 @version 1.0
 @since Apr 23, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountCountReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = -7988553133841657758L;
	private Map<AccountVO, Map<String, Integer>> accounts;
	private static final String REPORT_TITLE = "Account User Counts.xls";
	protected transient HSSFWorkbook wb;

	/**
	 * Constructor
	 */
	public AccountCountReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName(REPORT_TITLE);
		accounts = new LinkedHashMap<>();
		wb = new HSSFWorkbook();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		int rowCnt = 0;

		//Create Excel Sheet inside the Workbook
		HSSFSheet sheet = wb.createSheet();
		HSSFRow row = sheet.createRow(rowCnt++);
		addTitleRow(row);

		addDataRows(sheet, rowCnt);

		//resize the columns
		for (Cell cell : row)
			sheet.autoSizeColumn(cell.getColumnIndex());

		return ExcelReport.getBytes(wb);
	}

	/**
	 * Loop over the data and add it to the report.
	 * @param sheet
	 * @param rowCnt
	 */
	private void addDataRows(HSSFSheet sheet, int rowCnt) {
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		for (Entry<AccountVO, Map<String, Integer>> e : accounts.entrySet()) {
			HSSFRow row = sheet.createRow(rowCnt++);
			int cellCnt = 0;
			row.createCell(cellCnt++).setCellValue(e.getKey().getAccountName());
			row.createCell(cellCnt++).setCellValue(df.format(e.getKey().getStartDate()));
			row.createCell(cellCnt++).setCellValue(e.getKey().getExpirationDate() == null? "None": df.format(e.getKey().getExpirationDate()));
			row.createCell(cellCnt++).setCellValue(getSeatCount(e.getValue(), "A"));
			row.createCell(cellCnt++).setCellValue(getSeatCount(e.getValue(), "O"));
			row.createCell(cellCnt++).setCellValue(getSeatCount(e.getValue(), "C"));
			row.createCell(cellCnt++).setCellValue(getSeatCount(e.getValue(), "E"));
			row.createCell(cellCnt).setCellValue(getSeatCount(e.getValue(), "U"));
		}
	}

	/**
	 * Check to see if there is seat data for the supplied type.
	 * If it is missing return 0.
	 * @param seats
	 * @param key
	 * @return
	 */
	private int getSeatCount(Map<String, Integer> seats, String key) {
		if (seats == null || !seats.containsKey(key)) return 0;
		return seats.get(key);
	}


	/**
	 * Add the title row
	 * @param sheet
	 * @param row
	 */
	private void addTitleRow(HSSFRow row) {
		int cellCnt = 0;
		row.createCell(cellCnt++).setCellValue("Account");
		row.createCell(cellCnt++).setCellValue("Start Date");
		row.createCell(cellCnt++).setCellValue("Expiration Date");
		row.createCell(cellCnt++).setCellValue("Paid Seats");
		row.createCell(cellCnt++).setCellValue("Open Seats");
		row.createCell(cellCnt++).setCellValue("Complimentary Seats");
		row.createCell(cellCnt++).setCellValue("Extra Seats");
		row.createCell(cellCnt).setCellValue("Updates Only");
	}


	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		accounts = (Map<AccountVO, Map<String, Integer>>)o;
	}

}
