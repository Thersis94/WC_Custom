package com.codman.cu.tracking.vo;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: AccountReportVO<p/>
 * <b>Description: Exports the CU Account data an Excel report</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class AccountReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;

	public AccountReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("CU Accounts.xls");
	}
	

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Report");

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();

		// make title row, its the first row in the sheet (0)
		int rowNo = 0;
		Row r = s.createRow(rowNo++);
		addTitleRow(wb, s, r);

		//make the column headings row
		r = s.createRow(rowNo++);
		addHeaderRow(wb, s, r);

		//loop the accounts, physians, units, and requests
		for (AccountVO acct : data) {
			r = s.createRow(rowNo++); //create a new row
			formatData(acct, r); //populate the row
		}
		
	    // Auto-size the columns.
		for (int x=0; x < 15; x++)
			s.autoSizeColumn(x);

		//lastly, stream the WorkBook back to the browser
		return ExcelReport.getBytes(wb);
	}


	/**
	 * creates the initial title (header) row #1.
	 * @param wb
	 * @param s
	 * @param r
	 */
	protected void addTitleRow(Workbook wb, Sheet s, Row r) {
		r.setHeight((short)(r.getHeight()*2));
		
		//make a heading font for the title to be large and bold
		CellStyle headingStyle = wb.createCellStyle();
		Font font = wb.createFont();
		font.setFontHeightInPoints((short)16);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headingStyle.setFont(font);

		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue("MedStream CU Tracking System - Account Summary");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (all columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,15));
	}


	/**
	 * adds row #2, the column headings
	 * @param wb
	 * @param s
	 * @param r
	 */
	protected void addHeaderRow(Workbook wb, Sheet s, Row r) {
		int cellCnt = 0;
		createStringCell(r, "Rep Name", cellCnt++);
		createStringCell(r, "Territory", cellCnt++);
		createStringCell(r, "Sample Acct. No.", cellCnt++);
		createStringCell(r, "Account Name", cellCnt++);
		createStringCell(r, "Account No.", cellCnt++);
		createStringCell(r, "Physicians", cellCnt++);
		createStringCell(r, "Units", cellCnt++);
		createStringCell(r, "Requests", cellCnt++);
		createStringCell(r, "Phone", cellCnt++);
		createStringCell(r, "Address", cellCnt++);
		createStringCell(r, "Address2", cellCnt++);
		createStringCell(r, "City", cellCnt++);
		createStringCell(r, "State", cellCnt++);
		createStringCell(r, "Zip", cellCnt++);
		createStringCell(r, "Country", cellCnt++);
	}


	/**
	 * transforms a data VO into a row in the Excel report
	 * @param acct
	 * @param r
	 */
	private void formatData(AccountVO acct, Row r) {
		int cellCnt = 0;
		String name = StringUtil.checkVal(acct.getRep().getFirstName()) + " " + StringUtil.checkVal(acct.getRep().getLastName());
		createStringCell(r, name, cellCnt++);
		createStringCell(r, acct.getRep().getTerritoryId(), cellCnt++);
		createStringCell(r, acct.getRep().getSampleAccountNo(), cellCnt++);
		createStringCell(r, acct.getAccountName(), cellCnt++);
		createStringCell(r, acct.getAccountNo(), cellCnt++);
		createStringCell(r, acct.getPhysicians().size(), cellCnt++);
		createStringCell(r, acct.getUnitCount(), cellCnt++);

		//iterated the transactions and classify by status
		int pending = 0, approved = 0, complete = 0, denied = 0;
		for (TransactionVO v : acct.getTransactions()) {
			switch (v.getStatus()) {
				case PENDING:
					++pending;
					break;
				case APPROVED:
					++approved;
					break;
				case COMPLETE:
					++complete;
					break;
				case DECLINED:
					++denied;
					break;
				default:
					break;
			}
		}
		StringBuilder sts = new StringBuilder(100);
		if (pending > 0) sts.append(pending).append(" Pending<br/>\r");
		if (approved > 0) sts.append(approved).append(" Approved<br/>\r");
		if (complete > 0) sts.append(complete).append(" Completed<br/>\r");
		if (denied > 0) sts.append(denied).append(" Denied");
		createStringCell(r, sts.toString(), cellCnt++);

		createStringCell(r, acct.getAccountPhoneNumber(), cellCnt++);
		createStringCell(r, acct.getAccountAddress(), cellCnt++);
		createStringCell(r, acct.getAccountAddress2(), cellCnt++);
		createStringCell(r, acct.getAccountCity(), cellCnt++);
		createStringCell(r, acct.getAccountState(), cellCnt++);
		createStringCell(r, acct.getAccountZipCode(), cellCnt++);
		createStringCell(r, acct.getAccountCountry(), cellCnt++);
	}


	/**
	 * populates and returns a single String cell on the given row.
	 * @param r
	 * @param value
	 * @param cellNo
	 * @return
	 */
	protected Cell createStringCell(Row r, Object value, int cellNo) {
		Cell c = r.createCell(cellNo);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue(StringUtil.checkVal(value));
		return c;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<AccountVO>) o;
	}
}