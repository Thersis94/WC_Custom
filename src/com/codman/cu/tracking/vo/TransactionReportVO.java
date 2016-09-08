package com.codman.cu.tracking.vo;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: TransactionReportVO<p/>
 * <b>Description: Exports the Transaction data to an Excel report</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class TransactionReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;
	protected SiteVO siteVo;

	public TransactionReportVO(SiteVO site) {
		super();
		this.siteVo = site;
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit Transaction Report.xls");
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Physician Report");

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
			for (TransactionVO trans : acct.getTransactions()) {
				r = s.createRow(rowNo++); //create a new row
				formatData(acct.getAccountName(), trans, r); //populate the row
			}
		}
		
	    // Auto-size the columns.
		for (int x=0; x < 17; x++)
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
		c.setCellValue("Codman CU Tracking System - Request Summary");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (all columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,17));
	}


	/**
	 * adds row #2, the column headings
	 * @param wb
	 * @param s
	 * @param r
	 */
	protected void addHeaderRow(Workbook wb, Sheet s, Row r) {
		int cellCnt = 0;
		createStringCell(r, "Account", cellCnt++);
		createStringCell(r, "Request Submitted By", cellCnt++);
		createStringCell(r, "Intended User (Physician)", cellCnt++);
		createStringCell(r, "Request Status", cellCnt++);
		createStringCell(r, "Date Submitted", cellCnt++);
		createStringCell(r, "Request Overviewed By", cellCnt++);
		createStringCell(r, "Date Completed", cellCnt++);
		createStringCell(r, "Request No.", cellCnt++);
		createStringCell(r, "Request Type", cellCnt++);
		createStringCell(r, "Unit Type", cellCnt++);
		createStringCell(r, "Units Requested", cellCnt++);
		createStringCell(r, "Units Sent", cellCnt++);
		createStringCell(r, "Unit Serial No.(s)", cellCnt++);
		createStringCell(r, "Shipped To", cellCnt++);
		createStringCell(r, "Transaction Assignee", cellCnt++);
		createStringCell(r, "Transaction Date", cellCnt++);
		createStringCell(r, "Comments", cellCnt++);
	}


	/**
	 * formats the TransactionVO into a row on the Excel report
	 * @param acctName
	 * @param t
	 * @param r
	 */
	private void formatData(String acctName, TransactionVO t, Row r) {
		int cellCnt = 0;
		createStringCell(r, acctName, cellCnt++);
		createStringCell(r, t.getRequestorName(), cellCnt++);
		String physNm = StringUtil.checkVal(t.getPhysician().getFirstName()) + " " + StringUtil.checkVal(t.getPhysician().getLastName());
		createStringCell(r, physNm, cellCnt++);
		createStringCell(r, t.getStatusStr(), cellCnt++);
		createStringCell(r, this.formatDate(t.getCreateDate()), cellCnt++);
		createStringCell(r, t.getApprovorName(), cellCnt++);
		createStringCell(r, this.formatDate(t.getCompletedDate()), cellCnt++);
		createStringCell(r, t.getRequestNo(), cellCnt++);
		//request type
		String type;
		if (2 == t.getTransactionTypeId() && ProdType.ICP_EXPRESS == t.getProductType()) {
			type = "Return";
		} else if (2 == t.getTransactionTypeId()) {
			type = "Transfer";
		} else if (3 == t.getTransactionTypeId()) {
			type = "Refurbish";
		} else {
			type = "New Request";
		}
		createStringCell(r, type, cellCnt++);
		createStringCell(r, t.getProductTypeStr(), cellCnt++);
		createStringCell(r, t.getUnitCount(), cellCnt++);
		createStringCell(r, t.getUnits().size(), cellCnt++);
		createStringCell(r, t.getUnitSerialNos(), cellCnt++);
		String shipTo = StringUtil.checkVal(t.getShipToName());
		if (t.getShippingAddress() != null) shipTo += "\n" + t.getShippingAddress().getFormattedLocation();
		createStringCell(r, shipTo, cellCnt++);
		createStringCell(r, t.getRequestorName(), cellCnt++);
		createStringCell(r, this.formatDate(t.getCreateDate()), cellCnt++);
		createStringCell(r, t.getNotesText(), cellCnt++);
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


	/**
	 * date formatting helper - leverages the site's Locale for Intl localization
	 * @param d
	 * @return
	 */
	protected String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, siteVo.getLocale());
		return (d != null) ? df.format(d) : "";
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