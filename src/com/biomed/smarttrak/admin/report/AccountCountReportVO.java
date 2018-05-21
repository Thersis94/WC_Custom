package com.biomed.smarttrak.admin.report;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.data.Node;
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
	private Map<AccountUsersVO, Map<String, Integer>> accounts;
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
		for (Entry<AccountUsersVO, Map<String, Integer>> e : accounts.entrySet()) {
			HSSFRow row = sheet.createRow(rowCnt++);
			int cellCnt = 0;
			int activeUsers = getSeatCount(e.getValue(), "AA");
			int openUsers = getSeatCount(e.getValue(), "OA");
			int activeExtra = getSeatCount(e.getValue(), "AE");
			int openExtra = getSeatCount(e.getValue(), "OE");
			int activeTrial = getSeatCount(e.getValue(), "AK");
			int openTrial = getSeatCount(e.getValue(), "OK");
			int activeComp = getSeatCount(e.getValue(), "AC");
			int openComp = getSeatCount(e.getValue(), "OC");
			int activeUpdates = getSeatCount(e.getValue(), "AU");
			int openUpdates = getSeatCount(e.getValue(), "OU");
			int filled = activeUsers + activeExtra;
			int open = openUsers + openExtra;
			int total = filled + open;
			row.createCell(cellCnt++).setCellValue(e.getKey().getAccountName());
			row.createCell(cellCnt++).setCellValue(e.getKey().getAccountId());
			row.createCell(cellCnt++).setCellValue(df.format(e.getKey().getStartDate()));
			row.createCell(cellCnt++).setCellValue(e.getKey().getExpirationDate() == null? "None": df.format(e.getKey().getExpirationDate()));
			row.createCell(cellCnt++).setCellValue(activeUsers);
			row.createCell(cellCnt++).setCellValue(openUsers);
			row.createCell(cellCnt++).setCellValue(activeExtra);
			row.createCell(cellCnt++).setCellValue(openExtra);
			row.createCell(cellCnt++).setCellValue(activeTrial);
			row.createCell(cellCnt++).setCellValue(openTrial);
			row.createCell(cellCnt++).setCellValue(activeComp);
			row.createCell(cellCnt++).setCellValue(openComp);
			row.createCell(cellCnt++).setCellValue(activeUpdates);
			row.createCell(cellCnt++).setCellValue(openUpdates);
			row.createCell(cellCnt++).setCellValue(filled);
			row.createCell(cellCnt++).setCellValue(open);
			row.createCell(cellCnt++).setCellValue(total);
			row.createCell(cellCnt++).setCellValue(formatOpenPercent(open, total));
			row.createCell(cellCnt++).setCellValue(e.getKey().getClassificationName());
			row.createCell(cellCnt++).setCellValue(buildPermissions(e.getKey().getPermissions(), true));
			row.createCell(cellCnt).setCellValue(buildPermissions(e.getKey().getPermissions(), false));
		}
	}

	/**
	 * Format the percent of open seats
	 * @param open
	 * @param total
	 * @return
	 */
	private String formatOpenPercent(int open, int total) {
		if (open == 0) return "0%";
		double percent = (double)open/(double)total;
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		return percentFormat.format(percent);
	}


	/**
	 * Build the permissions string
	 * @param permissions
	 * @param isGA
	 * @return
	 */
	private String buildPermissions(SmarttrakTree permissions, boolean isGA) {
		StringBuilder permissionText = new StringBuilder(5000);
		
		for (Node n : permissions.preorderList()) {
			PermissionVO p = (PermissionVO) n.getUserObject();
			if (isGA && p.isGaAuth()) {
				if (permissionText.length() > 0) permissionText.append(", ");
				permissionText.append(p.getSectionNm());
			} else if (p.isFdAuth()) {
				if (permissionText.length() > 0) permissionText.append(", ");
				permissionText.append(p.getSectionNm());
			}
		}
		return permissionText.toString();
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
		row.createCell(cellCnt++).setCellValue("Account Id");
		row.createCell(cellCnt++).setCellValue("Start Date");
		row.createCell(cellCnt++).setCellValue("Expiration Date");
		row.createCell(cellCnt++).setCellValue("Active ST User");
		row.createCell(cellCnt++).setCellValue("Open ST User");
		row.createCell(cellCnt++).setCellValue("Active Extra Seat");
		row.createCell(cellCnt++).setCellValue("Open Extra Seat");
		row.createCell(cellCnt++).setCellValue("Active Trial Seat");
		row.createCell(cellCnt++).setCellValue("Open Trial Seat");
		row.createCell(cellCnt++).setCellValue("Active Complimentary Seat");
		row.createCell(cellCnt++).setCellValue("Open Complimentary Seat");
		row.createCell(cellCnt++).setCellValue("Active Updates Only");
		row.createCell(cellCnt++).setCellValue("Open Updates Only");
		row.createCell(cellCnt++).setCellValue("Total Active Seats");
		row.createCell(cellCnt++).setCellValue("Total Open Seats");
		row.createCell(cellCnt++).setCellValue("Total Paid Seats");
		row.createCell(cellCnt++).setCellValue("Percent Open");
		row.createCell(cellCnt++).setCellValue("Classification");
		row.createCell(cellCnt++).setCellValue("GA Modules");
		row.createCell(cellCnt).setCellValue("FD Modules");
	}


	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		accounts = (Map<AccountUsersVO, Map<String, Integer>>)o;
	}

}
