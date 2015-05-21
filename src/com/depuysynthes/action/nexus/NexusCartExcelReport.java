package com.depuysynthes.action.nexus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.lowagie.text.Font;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.constants.Constants;

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
	private HSSFCellStyle leftBorder;
	private HSSFCellStyle header;
	private HSSFCellStyle seperator;
	private HSSFCellStyle caseId;
	private HSSFCellStyle rightAlign;
	
	
	public NexusCartExcelReport() {
		super();
	}
	
	/**
	 * Set up the styles used in this workbook
	 * @param wb
	 */
	private void generateStyles(HSSFWorkbook wb) {
		leftBorder = wb.createCellStyle();
		leftBorder.setBorderLeft(CellStyle.BORDER_THIN);
		leftBorder.setIndention((short)10);
		
		HSSFFont headerFont = wb.createFont();
		headerFont.setBoldweight((short) Font.BOLD);
		header = wb.createCellStyle();
		header.setBorderBottom(CellStyle.BORDER_THICK);
		header.setFont(headerFont);
		
		HSSFFont caseFont = wb.createFont();
		caseFont.setFontHeightInPoints((short)20);
		caseFont.setColor(HSSFColor.GREY_80_PERCENT.index);
		caseId = wb.createCellStyle();
		caseId.setFont(caseFont);

		seperator = wb.createCellStyle();
		seperator.setBorderBottom(CellStyle.BORDER_THIN);
		
		rightAlign = wb.createCellStyle();
		rightAlign.setAlignment(CellStyle.ALIGN_RIGHT);
		rightAlign.setVerticalAlignment(CellStyle.VERTICAL_TOP);
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
			Cell c;
			int cellCnt = 0;
			
			// Create the main data row
			Row r = s.createRow(rowCnt++);
			r.setHeight((short)500);
			r.createCell(cellCnt++).setCellValue(p.getProductId());
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("orgName"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("gtin"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("lotNo"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("uom"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("qty"));
			r.createCell(cellCnt++).setCellValue("GTIN");
			// Build the barcode for the GTIN
			int x = loadFileIntoWorkbook("http://"+data.get("baseDomain")+"/"+Constants.BARCODE_GENERATOR_URL+"?height=25&barcodeData="+(String)p.getProdAttributes().get("gtin"), s.getWorkbook());
			addImageToSheet(x, cellCnt++, r.getRowNum(), s);
			

			// Create the description row
			r = s.createRow(rowCnt++);
			r.setHeight((short)500);
			c = r.createCell(0);
			c.setCellValue(p.getShortDesc());
			c.setCellStyle(seperator);
			s.addMergedRegion(new CellRangeAddress(r.getRowNum(),r.getRowNum(),0,5));
			c = r.createCell(6);
			c.setCellValue("Lot NO");
			c.setCellStyle(seperator);
			r.createCell(7).setCellStyle(seperator);
			// Create the barcode for the lot number
			x = loadFileIntoWorkbook("http://"+data.get("baseDomain")+"/"+Constants.BARCODE_GENERATOR_URL+"?height=25&barcodeData="+(String)p.getProdAttributes().get("lotNo"), s.getWorkbook());
			addImageToSheet(x, 7, r.getRowNum(), s);
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
		sheet.setColumnWidth(colNum++, 12000);
	}
	
	
	
	/**
	 * Build the header section of the file
	 * @param s
	 * @param helper
	 * @return
	 */
	private int buildFileHeader(Sheet s, CreationHelper helper) {
		int rowCnt = 0;
		Row r = s.createRow(rowCnt++);
		Cell c;
		r.setHeight((short) 1000);
		int x = loadFileIntoWorkbook("http://"+data.get("baseDomain")+"/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/logo.jpg", s.getWorkbook());
		addImageToSheet(x, 0, r.getRowNum(), s);
		s.addMergedRegion(new CellRangeAddress(0,0,0,2));

		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Case Report(ID:" + data.get("caseId") + ")");
		c.setCellStyle(caseId);
		c = r.createCell(4);
		c.setCellValue("Surgery Date and Time:");
		c.setCellStyle(leftBorder);
		r.createCell(7).setCellValue((String)data.get("time"));
		s.addMergedRegion(new CellRangeAddress(rowCnt-1, rowCnt+2, 0, 1));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(4);
		c.setCellValue("Surgeon Name:");
		c.setCellStyle(leftBorder);
		r.createCell(7).setCellValue((String)data.get("surgeon"));
		s.addMergedRegion(new CellRangeAddress(1,1,4,5));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(4);
		c.setCellValue("Hospital Name:");
		c.setCellStyle(leftBorder);
		r.createCell(7).setCellValue((String)data.get("hospital"));
		s.addMergedRegion(new CellRangeAddress(2,2,4,5));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(4);
		c.setCellValue("OR Room:");
		c.setCellStyle(leftBorder);
		r.createCell(7).setCellValue((String)data.get("room"));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(4);
		c.setCellValue("Case ID:");
		c.setCellStyle(leftBorder);
		r.createCell(7).setCellValue((String)data.get("caseId"));
		
		// Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.createCell(0).setCellValue("Products");

		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.setHeight((short)400);
		c = r.createCell(2);
		c.setCellValue("UDI");
		c.setCellStyle(rightAlign);
		x = loadFileIntoWorkbook("http://"+data.get("baseDomain")+"/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/padded-line-before.jpg", s.getWorkbook());
		addImageToSheet(x, 2, r.getRowNum(), s);
		x = loadFileIntoWorkbook("http://"+data.get("baseDomain")+"/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/padded-line-after.jpg", s.getWorkbook());
		addImageToSheet(x, 3, r.getRowNum(), s);
		
		//Reset and begin the next row
		int cellCnt = 0;
		r = s.createRow(rowCnt++);
		c = r.createCell(cellCnt++);
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
		c.setCellValue("Barcode");
		c.setCellStyle(header);
		r.createCell(cellCnt++).setCellStyle(header);
		
		return rowCnt;
	}
	
	
	/**
	 * Load the supplied file, add it to the workbook, 
	 * and return the image'sid
	 * @param file
	 * @param workbook
	 * @return
	 */
	private int loadFileIntoWorkbook(String file, Workbook workbook) {
		//add picture data to this workbook.
		int pictureId = 0;
		try (InputStream is = new URL(file).openStream()){
			byte[] bytes = IOUtils.toByteArray(is);
			pictureId = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
		} catch (IOException e) {
			log.error("Unable to load file " + file, e);
		}
		return pictureId;
	}
	
	
	/**
	 * Add the image indicated by the imageId to the position 
	 * defined by the row and col variables.
	 * @param imageId
	 * @param colStart
	 * @param colEnd
	 * @param rowStart
	 * @param rowEnd
	 * @param s
	 */
	private void addImageToSheet(int imageId, int col, int row, Sheet s) {
		// Create the drawing patriarch.  This is the top level container for all shapes. 
		Drawing drawing = s.createDrawingPatriarch();
		//add a picture shape
		ClientAnchor anchor = s.getWorkbook().getCreationHelper().createClientAnchor();
		//set top-left corner of the picture,
		//subsequent call of Picture#resize() will operate relative to it
		anchor.setCol1(col);
		anchor.setRow1(row);
		Picture pict = drawing.createPicture(anchor, imageId);
		//auto-size picture relative to its top-left corner
		pict.resize();
	}
}
