package com.ram.action.report.vo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.lowagie.text.Font;
import com.ram.action.data.ORKitVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>KitExcelReport.java<p/>
 * <b>Description: Create an excel document listing basic information on
 * all supplied kits </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 ****************************************************************************/

public class KitExcelReport extends  AbstractSBReportVO {
	
	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(KitExcelReport.class);

	List<ORKitVO> data;
	private HSSFCellStyle header;
	
	
	public KitExcelReport() {
		super();
	}
	
	/**
	 * Set up the styles used in this workbook
	 * @param wb
	 */
	private void generateStyles(HSSFWorkbook wb) {
		HSSFFont headerFont = wb.createFont();
		headerFont.setBoldweight((short) Font.BOLD);
		header = wb.createCellStyle();
		header.setBorderBottom(CellStyle.BORDER_THICK);
		header.setFont(headerFont);
	}

	@Override
	public byte[] generateReport() {
		//Create Excel Object
		HSSFWorkbook wb = new HSSFWorkbook();
		generateStyles(wb);
		HSSFSheet s = wb.createSheet();
		//Build the header section
		int rowCnt = buildFileHeader(s);
		//Get the cart info from the data object and loop over it
		buildReportBody(rowCnt, s);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			wb.write(baos);
		} catch (IOException e) {
			log.error("Unable to write file to stream", e);
		}
		
		return baos.toByteArray();
	}
	
	/**
	 * Loop over the supplied car and build the main body of the report
	 * @param cart
	 * @param rowCnt
	 * @param s
	 */
	private void buildReportBody(int rowCnt, HSSFSheet s) {
		for (ORKitVO kit : data) {
			int cellCnt = 0;
			// Create the main data row
			Row r = s.createRow(rowCnt++);
			r.createCell(cellCnt++).setCellValue(kit.getHospitalName());
			r.createCell(cellCnt++).setCellValue(kit.getOperatingRoom());
			r.createCell(cellCnt++).setCellValue(kit.getSurgeryDt());
			r.createCell(cellCnt++).setCellValue(kit.getSurgeonNm());
			r.createCell(cellCnt++).setCellValue(kit.getCaseId());
			r.createCell(cellCnt++).setCellValue(kit.getResellerNm());
			r.createCell(cellCnt++).setCellValue(kit.getRepId());
			r.createCell(cellCnt++).setCellValue(kit.getOtherId());
			r.createCell(cellCnt).setCellValue(kit.getNumProducts());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof List<?>) {
			data = (List<ORKitVO>) o;
		}
	}
	
	
	/**
	 * Build the header section of the file
	 * @param s
	 * @return
	 */
	private int buildFileHeader(Sheet s) {
		int rowCnt = 0;
		int cellCnt = 0;
		Row r = s.createRow(rowCnt++);
		Cell c = r.createCell(cellCnt++);
		c.setCellValue("Hospital Name");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Operating Room");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Surgery Date");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Surgeon");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Hospital Case Id");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Reseller Name");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Rep Id");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Other Id");
		c.setCellStyle(header);
		c = r.createCell(cellCnt);
		c.setCellValue("Number of Products in Case");
		c.setCellStyle(header);
		
		return rowCnt;
	}
}
