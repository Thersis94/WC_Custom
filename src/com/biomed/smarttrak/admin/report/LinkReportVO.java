package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;

//WC custom
import com.biomed.smarttrak.vo.LinkVO;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory;
import com.siliconmtn.data.report.ExcelStyleFactory.Styles;
import com.siliconmtn.data.report.ExcelStyleInterface;
import com.siliconmtn.util.Convert;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: LinkReportVO.java</p>
 <p><b>Description: </b>Creates the Broken Links Excel report.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Apr 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class LinkReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 98123765765432882L;

	private transient List<LinkVO> links;
	protected transient CellStyle headerStyle;
	protected transient CellStyle titleStyle;
	protected transient CellStyle bodyStyle;
	protected transient HSSFWorkbook wb;


	/**
	 * Constructor
	 */
	public LinkReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("SmartTRAK Broken Links.xls");
		wb = new HSSFWorkbook();
		ExcelStyleInterface style = ExcelStyleFactory.getExcelStyle(Styles.Standard);
		headerStyle = style.getHeadingStyle(wb);
		titleStyle = style.getTitleStyle(wb);
		bodyStyle =   style.getBodyStyle(wb);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");
		int rowCnt = -1;

		//Create Excel Sheet inside the Workbook
		HSSFSheet sheet = wb.createSheet();
		HSSFRow row = sheet.createRow(++rowCnt);
		addTitleRow(sheet, row, "SmartTRAK Broken Links Report - " + Convert.formatDate(Calendar.getInstance().getTime(),  Convert.DATE_LONG));

		//Loop and set cell values for the header row.
		row = sheet.createRow(++rowCnt);
		int cellCnt = -1;
		for (Map.Entry<String, String> entry : getHeader().entrySet()) {
			HSSFCell c = row.createCell(++cellCnt);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(entry.getValue());
			c.setCellStyle(headerStyle);
		}

		addDataRows(sheet, rowCnt);

		//resize the columns
		for (Cell cell : row)
			sheet.autoSizeColumn(cell.getColumnIndex());

		return ExcelReport.getBytes(wb);
	}


	/**
	 * @param wb
	 * @param string
	 */
	protected void addTitleRow(HSSFSheet sheet, HSSFRow row, String title) {
		//fill it with the title string.
		Cell c = row.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);

		c.setCellValue(title);
		c.setCellStyle(titleStyle);
		//merge it the length of the report.
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));

		//set the row height to auto - accounts for multi-line titles
		row.setHeight((short)0);
	}


	/**
	 * @param rows
	 */
	protected void addDataRows(HSSFSheet sheet, int rowCnt) {
		int rowNo = rowCnt;
		for (LinkVO vo : links) {
			HSSFRow row = sheet.createRow(++rowNo);

			int cellCnt = -1;
			HSSFCell cell = row.createCell(++cellCnt);
			cell.setCellValue(vo.getSection());
			cell = row.createCell(++cellCnt);
			cell.setCellValue(vo.getPageNm());
			cell = row.createCell(++cellCnt);
			cell.setCellValue(Convert.formatDate(vo.getLastChecked(), Convert.DATE_SLASH_PATTERN));
			cell = row.createCell(++cellCnt);
			cell.setCellValue(Integer.toString(vo.getOutcome()));

			//link to public page
			HSSFHyperlink linkCell = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
			linkCell.setAddress(vo.getPublicUrl());
			cell = row.createCell(++cellCnt);
			cell.setCellValue("View Page");
			cell.setHyperlink(linkCell);

			//link to admin page
			linkCell = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
			linkCell.setAddress(vo.getAdminUrl());
			cell = row.createCell(++cellCnt);
			cell.setCellValue("Edit");
			cell.setHyperlink(linkCell);

			//the broken link (make it clickable, they can test it for themself!)
			linkCell = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
			linkCell.setAddress(vo.getUrl());
			cell = row.createCell(++cellCnt);
			cell.setCellValue(vo.getUrl());
			cell.setHyperlink(linkCell);
		}
	}


	/**
	 * @return
	 */
	protected Map<String, String> getHeader() {
		Map<String, String> hdr = new LinkedHashMap<>();
		hdr.put("SECTION","Section");
		hdr.put("PAGE_NM","Page Name");
		hdr.put("DATE","Date checked");
		hdr.put("CODE","Response Code");
		hdr.put("PAGE","Affected Page");
		hdr.put("EDIT","Admin Page (Edit)");
		hdr.put("URL","Broken Link");
		return hdr;
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.links = (List<LinkVO>)o;
	}
}