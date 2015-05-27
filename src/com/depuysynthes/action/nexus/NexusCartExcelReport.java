package com.depuysynthes.action.nexus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.lowagie.text.Font;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: NexusCartExcelReport.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Creates the excel report for all the items in the cart
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since May 20, 2015<p/>
 * @updates:
 ****************************************************************************/

public class NexusCartExcelReport extends AbstractSBReportVO {
	
	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(NexusCartExcelReport.class);

	Map<String, Object> data;
	private HSSFCellStyle header;
	
	
	public NexusCartExcelReport() {
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
		CreationHelper helper = wb.getCreationHelper();
		HSSFSheet s = wb.createSheet();
		setColWidths(s);
		//Build the header section
		int rowCnt = buildFileHeader(s, helper);
		//Get the cart info from the data object and loop over it
		@SuppressWarnings("unchecked")
		Map<String, ShoppingCartItemVO> cart = (Map<String, ShoppingCartItemVO>) data.get("cart");
		buildReportBody(cart, rowCnt, s);
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
	private void buildReportBody(Map<String, ShoppingCartItemVO> cart, int rowCnt, HSSFSheet s) {
		for (String cartKey: cart.keySet()) {
			ProductVO p = cart.get(cartKey).getProduct();
			int cellCnt = 0;
			
			// Create the main data row
			Row r = s.createRow(rowCnt++);
			r.createCell(cellCnt++).setCellValue(p.getProductId());
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("orgName"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("gtin"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("lotNo"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("uom"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("qty"));
			r.createCell(cellCnt++).setCellValue((String)data.get("caseId"));
			r.createCell(cellCnt++).setCellValue((String)data.get("room"));
			r.createCell(cellCnt++).setCellValue((String)data.get("hospital"));
			r.createCell(cellCnt++).setCellValue((String)data.get("surgeon"));
			r.createCell(cellCnt++).setCellValue((String)data.get("time"));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, Object>) o;
		}
	}
	
	
	/**
	 * Widen the columns for easier readability
	 * @param sheet
	 */
	private void setColWidths(HSSFSheet sheet) {
		int colNum = 0;
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 6000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 7000);
	}
	
	
	
	/**
	 * Build the header section of the file
	 * @param s
	 * @param helper
	 * @return
	 */
	private int buildFileHeader(Sheet s, CreationHelper helper) {
		int rowCnt = 0;
		int cellCnt = 0;
		Row r = s.createRow(rowCnt++);
		Cell c = r.createCell(cellCnt++);
		c.setCellValue("Product No.");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Company");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("GTIN");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("LOT No.");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("UOM");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("QTY");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Case Id");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("OR Room");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Hospital name");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Surgeon Name");
		c.setCellStyle(header);
		c = r.createCell(cellCnt++);
		c.setCellValue("Surgery Date");
		c.setCellStyle(header);
		
		return rowCnt;
	}
}
