package com.depuysynthes.action.nexus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

public class NexusCartReport extends AbstractSBReportVO {
	
	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(NexusCartReport.class);

	Map<String, ShoppingCartItemVO> data;
	
	public NexusCartReport() {
		super();
	}

	@Override
	public byte[] generateReport() {
		//Create Excel Object
		HSSFWorkbook wb = new HSSFWorkbook();
		CreationHelper helper = wb.getCreationHelper();
		HSSFSheet s = wb.createSheet();
		setColWidths(s);
		//Loop and set cell values for the header row.
		int rowCnt = buildFileHeader(s, helper);
		//loop the data rows
		for (String cartKey: data.keySet()) {
			ProductVO p = data.get(cartKey).getProduct();
			Row r = s.createRow(rowCnt++);
			int cellCnt = 0;
			r.createCell(cellCnt++).setCellValue(p.getProductId());
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("orgName"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("gtin"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("lotNo"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("uom"));
			r.createCell(cellCnt++).setCellValue((String)p.getProdAttributes().get("qty"));
			//Create image and put into next cell
			s.addMergedRegion(new CellRangeAddress(r.getRowNum(),r.getRowNum(),6,7));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			wb.write(baos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return baos.toByteArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, ShoppingCartItemVO>) o;
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
		sheet.setColumnWidth(colNum++, 6000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 12000);
	}
	
	
	
	//9 columns
	private int buildFileHeader(Sheet s, CreationHelper helper) {
		int rowCnt = 0;
		Row r = s.createRow(rowCnt++);
		Cell c;
		r.setHeight((short) 1000);
		int x = loadFileIntoWorkbook("http://eric.depuy.siliconmtn.com/binary/themes/CUSTOM/DEPUY/AMBASSADOR/images/mobileLogo.png", s.getWorkbook());
		addImageToSheet(x, 0, r.getRowNum(), s);
		s.addMergedRegion(new CellRangeAddress(0,0,0,2));
		// TODO Get image for Unique Device Identigication image
		//x = loadFileIntoWorkbook("http://eric.depuy.siliconmtn.com/binary/themes/CUSTOM/DEPUY/AMBASSADOR/images/mobileLogo.png", s.getWorkbook());
		//addImageToSheet(x, 6, r.getRowNum(), s);

		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Case Report(ID:" + "" + ")");
		r.createCell(4).setCellValue("Surgery Date and Time:");
		r.createCell(7).setCellValue("");
		
		s.addMergedRegion(new CellRangeAddress(1,3,0,0));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.createCell(4).setCellValue("Surgeon Name:");
		r.createCell(7).setCellValue("");
		s.addMergedRegion(new CellRangeAddress(1,1,4,5));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.createCell(4).setCellValue("Hospital Name:");
		r.createCell(4).setCellValue("");
		s.addMergedRegion(new CellRangeAddress(2,2,4,5));
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.createCell(4).setCellValue("OR Room:");
		r.createCell(7).setCellValue("");
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		r.createCell(4).setCellValue("Case ID:");
		r.createCell(7).setCellValue("");	
		
		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		// TODO Get image for Products
		//x = loadFileIntoWorkbook("http://eric.depuy.siliconmtn.com/binary/themes/CUSTOM/DEPUY/AMBASSADOR/images/mobileLogo.png", s.getWorkbook());
		//addImageToSheet(x, 0, r.getRowNum(), s);

		//Reset and begin the next row
		r = s.createRow(rowCnt++);
		// TODO Get image for UDI
		//x = loadFileIntoWorkbook("http://eric.depuy.siliconmtn.com/binary/themes/CUSTOM/DEPUY/AMBASSADOR/images/mobileLogo.png", s.getWorkbook());
		//addImageToSheet(x, 3, r.getRowNum(), s);
		s.addMergedRegion(new CellRangeAddress(7,7,2,3));
		
		//Reset and begin the next row
		int cellCnt = 0;
		r = s.createRow(rowCnt++);
		c = r.createCell(cellCnt++);
		c.setCellValue("Product No.");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("Company");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("GTIN");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("LOT No.");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("UOM");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("QTY");
		//c.setCellStyle();
		c = r.createCell(cellCnt++);
		c.setCellValue("Barcode");
		//c.setCellStyle();
		
		
		
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
			e.printStackTrace();
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
