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
 * <b>Title</b>: DataExport.java<p/>
 * <b>Description: Exports a suite of data to an Excel report</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class PhysicianReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;

	public PhysicianReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit Physician Export.xls");
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
			//print a row representing each Physician within each Account
			for (PhysicianVO phys : acct.getPhysicians()) {
				r = s.createRow(rowNo++); //create a new row
				formatData(acct.getAccountName(), phys, r); //populate the row
			}
		}
		
	    // Auto-size the columns.
		for (int x=0; x < 10; x++)
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
		c.setCellValue("Codman CU Tracking System - Physician Summary");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (all columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,10));
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
		createStringCell(r, "Name", cellCnt++);
		createStringCell(r, "Email", cellCnt++);
		createStringCell(r, "Phone", cellCnt++);
		createStringCell(r, "Address", cellCnt++);
		createStringCell(r, "Suite/Box", cellCnt++);
		createStringCell(r, "City", cellCnt++);
		createStringCell(r, "State", cellCnt++);
		createStringCell(r, "Zip", cellCnt++);
		createStringCell(r, "Country", cellCnt++);
	}


	/**
	 * transforms a PhysicianVO into a row in the report
	 * @param acctName
	 * @param v
	 * @param r
	 */
	private void formatData(String acctName, PhysicianVO v, Row r) {
		int cellCnt = 0;
		createStringCell(r, acctName, cellCnt++);
		String name = StringUtil.checkVal(v.getFirstName()) + " " + StringUtil.checkVal(v.getLastName());
		createStringCell(r, name, cellCnt++);
		createStringCell(r, v.getEmailAddress(), cellCnt++);
		createStringCell(r, v.getMainPhone(), cellCnt++);
		createStringCell(r, v.getAddress(), cellCnt++);
		createStringCell(r, v.getAddress2(), cellCnt++);
		createStringCell(r, v.getCity(), cellCnt++);
		createStringCell(r, v.getState(), cellCnt++);
		createStringCell(r, v.getZipCode(), cellCnt++);
		createStringCell(r, v.getCountryCode(), cellCnt++);
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