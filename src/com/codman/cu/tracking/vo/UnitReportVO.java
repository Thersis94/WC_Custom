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

import com.codman.cu.tracking.UnitAction;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: UnitDetailReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		Feb 13, 2012, added state and country to report. - JM
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class UnitReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1407073622024040274L;
	private List<UnitVO> data;
	protected SiteVO siteVo;

	public UnitReportVO(SiteVO site) {
		super();
		this.siteVo = site;
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit Summary Report.xls");
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Unit Report");

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();

		// make title row, its the first row in the sheet (0)
		int rowNo = 0;
		Row r = s.createRow(rowNo++);
		addTitleRow(wb, s, r);
		
		//make column headings row
		r = s.createRow(rowNo++);
		addHeaderRow(wb, s, r);

		//loop the accounts, physians, units, and requests
		for (UnitVO v : data) {
			r = s.createRow(rowNo++); //create a new row
			formatUnit(v, r); //populate the row
		}
		
	    // Auto-size the columns.
		for (int x=0; x < 11; x++)
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
		c.setCellValue("Codman CU Tracking System - Unit Summary");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (all columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,11));
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
		createStringCell(r, "Rep Name", cellCnt++);
		createStringCell(r, "Physician Name", cellCnt++);
		createStringCell(r, "Unit Type", cellCnt++);
		createStringCell(r, "Unit Status", cellCnt++);
		createStringCell(r, "Date Deployed", cellCnt++);
		createStringCell(r, "Serial No.", cellCnt++);
		createStringCell(r, "Software Rev No.", cellCnt++);
		createStringCell(r, "Hardware Rev No.", cellCnt++);
		createStringCell(r, "City", cellCnt++);
		createStringCell(r, "Country", cellCnt++);
		createStringCell(r, "Comments", cellCnt++);
	}


	/**
	 * transforms a UnitVO (piece of data) into a row of data on the report
	 * @param u
	 * @param r
	 */
	protected void formatUnit(UnitVO u, Row r) {
		int cellCnt = 0;
		createStringCell(r, u.getAccountName(), cellCnt++);
		createStringCell(r, u.getRepName(), cellCnt++);
		createStringCell(r, u.getPhysicianName(), cellCnt++);
		String prodNm = (u.getProductType() != null) ? u.getProductType().toString() : "";
		createStringCell(r, prodNm, cellCnt++);
		createStringCell(r, UnitAction.getStatusName(u.getStatusId()), cellCnt++);
		createStringCell(r, this.formatDate(u.getDeployedDate()), cellCnt++);
		createStringCell(r, u.getSerialNo(), cellCnt++);
		createStringCell(r, u.getSoftwareRevNo(), cellCnt++);
		createStringCell(r, u.getHardwareRevNo(), cellCnt++);
		createStringCell(r, u.getAccountCity(), cellCnt++);
		createStringCell(r, u.getAccountCountry(), cellCnt++);
		createStringCell(r, u.getCommentsText(), cellCnt++);
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
		data = (List<UnitVO>) o;
	}
}