package com.biomed.smarttrak.vo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.solr.common.SolrDocument;

import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
* <b>Title</b>: ProductExplorerReportVO.java<p/>
* <b>Description: Build an excel file out of the supplied list
* of solr documents.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2017<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Eric Damschroder
* @version 1.0
* @since Feb 20, 2017
****************************************************************************/
public class ProductExplorerReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1L;
	
	Collection<SolrDocument> data;
	
	private String excludeColumns;
	
	// Member Variables
	Map<String, Object> solrAttribs = new HashMap<>();
	CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); 
	HSSFCellStyle header = null;
	HSSFCellStyle alternate = null;
	HSSFCellStyle green = null;
	HSSFCellStyle headerGreen = null;
	HSSFCellStyle alternateGreen = null;
	HSSFCellStyle blue = null;
	HSSFCellStyle headerBlue = null;
	HSSFCellStyle alternateBlue = null;

	
	public ProductExplorerReportVO() {
		super();
	}
	
	
	
	/**
	 * 
	 */
	public byte[] generateReport() {
		//Build Excel File
		HSSFWorkbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet("Product Set");
		sheet.setDefaultColumnWidth(20);
		buildStyles(workbook);
		this.createHeader(sheet, workbook);
		buildWorkbook(sheet);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			workbook.write(baos);
		} catch (IOException e) {
			log.error("Unable to write file to stream", e);
		}

		return baos.toByteArray();
	}
	
	/**
	 * 
	 */
	protected void buildStyles(HSSFWorkbook wb) {
		header = wb.createCellStyle();
		header.setFillForegroundColor(setColor(wb, (byte) 0xE0,(byte) 0xE0, (byte) 0xE0, (short) 46).getIndex());
		header.setFillPattern(CellStyle.SOLID_FOREGROUND);
		header.setBorderTop(CellStyle.BORDER_MEDIUM);
		header.setBorderBottom(CellStyle.BORDER_MEDIUM);
		
		alternate = wb.createCellStyle();
		alternate.setFillForegroundColor(setColor(wb, (byte) 0xCA,(byte) 0xCA, (byte) 0xCA, (short) 47).getIndex());
		alternate.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		headerGreen = wb.createCellStyle();
		headerGreen.setFillForegroundColor(setColor(wb, (byte) 0xA5,(byte) 0xB0, (byte) 0x86, (short) 48).getIndex());
		headerGreen.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerGreen.setBorderTop(CellStyle.BORDER_MEDIUM);
		headerGreen.setBorderBottom(CellStyle.BORDER_MEDIUM);
		
		green = wb.createCellStyle();
		green.setFillForegroundColor(setColor(wb, (byte) 0xD7,(byte) 0xD9, (byte) 0xCD, (short) 50).getIndex());
		green.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		alternateGreen = wb.createCellStyle();
		alternateGreen.setFillForegroundColor(setColor(wb, (byte) 0xF7,(byte) 0xF9, (byte) 0xED, (short) 49).getIndex());
		alternateGreen.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		headerBlue = wb.createCellStyle();
		headerBlue.setFillForegroundColor(setColor(wb, (byte) 0x84,(byte) 0xA6, (byte) 0xC2, (short) 51).getIndex());
		headerBlue.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerBlue.setBorderTop(CellStyle.BORDER_MEDIUM);
		headerBlue.setBorderBottom(CellStyle.BORDER_MEDIUM);
		
		blue = wb.createCellStyle();
		blue.setFillForegroundColor(setColor(wb, (byte) 0xCC,(byte) 0xD8, (byte) 0xDF, (short) 52).getIndex());
		blue.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		alternateBlue = wb.createCellStyle();
		alternateBlue.setFillForegroundColor(setColor(wb, (byte) 0xEC,(byte) 0xEC, (byte) 0xF8, (short) 53).getIndex());
		alternateBlue.setFillPattern(CellStyle.SOLID_FOREGROUND);
	}
	
	
	public HSSFColor setColor(HSSFWorkbook workbook, byte r,byte g, byte b, short index){
		HSSFPalette palette = workbook.getCustomPalette();
		HSSFColor hssfColor = null;
		try {
			palette.setColorAtIndex(index, r, g,b);
			hssfColor = palette.getColor(index);
		} catch (Exception e) {
			log.error("Failed to set color for styles", e);
		}
		return hssfColor;
	}
	
	/**
	 * Build the report's content.
	 * @param sheet
	 */
	private void buildWorkbook(Sheet sheet) {
		if (data == null) return;
		// Loop the queries until all rows have been retrieved
		int i = 1;
		for (SolrDocument doc : data) {
			Row row = sheet.createRow(i++);
			int j = 0;
			Cell cell = null;
			
			cell = row.createCell(j++);
			cell.setCellValue(StringUtil.checkVal(doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID)).replace("PRODUCT_", ""));
			if (i%2 == 1)cell.setCellStyle(alternate);
			
			if (!excludeColumns.contains("1")) {
				cell = row.createCell(j++);
				cell.setCellValue((String)doc.getFieldValue(SearchDocumentHandler.TITLE));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("2")) {
				cell = row.createCell(j++);
				cell.setCellValue((String)doc.getFieldValue("company_s"));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("b")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("ownership_s")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}


			if (!excludeColumns.contains("3")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("section")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("4")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("target_market_ss")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("5")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("indication_ss")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("6")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("classification_ss")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("7")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("technology_ss")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("8")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("approach_ss")));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("9")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("uspathnm_ss")));
				if (i%2 == 0) {
					cell.setCellStyle(alternateGreen);
				} else {
					cell.setCellStyle(green);
				}
			}

			if (!excludeColumns.contains("e")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("usstatusnm_ss")));
				if (i%2 == 0) {
					cell.setCellStyle(alternateGreen);
				} else {
					cell.setCellStyle(green);
				}
				
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("intregionnm_ss")));
				if (i%2 == 0) {
					cell.setCellStyle(alternateBlue);
				} else {
					cell.setCellStyle(blue);
				}
				
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("intpathnm_ss")));
				if (i%2 == 0) {
					cell.setCellStyle(alternateBlue);
				} else {
					cell.setCellStyle(blue);
				}
				
				cell = row.createCell(j++);
				cell.setCellValue(buildContentList(doc.getFieldValues("intstatusnm_ss")));
				if (i%2 == 0) {
					cell.setCellStyle(alternateBlue);
				} else {
					cell.setCellStyle(blue);
				}
			}
			
			if (!excludeColumns.contains("c")) {
				cell = row.createCell(j++);
				cell.setCellValue((String)doc.getFieldValue(SearchDocumentHandler.COUNTRY));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}
			
			if (!excludeColumns.contains("d")) {
				cell = row.createCell(j++);
				cell.setCellValue((String)doc.getFieldValue(SearchDocumentHandler.STATE));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}

			if (!excludeColumns.contains("a")) {
				cell = row.createCell(j++);
				cell.setCellValue(buildAllyList(doc));
				if (i%2 == 1)cell.setCellStyle(alternate);
			}
		}
	}
	
	/**
	 * Loop over the supplied list of values and combine them into a single string
	 * @param collection
	 * @return
	 */
	protected String buildContentList(Collection<Object> collection) {
		if (collection == null) return "";
		StringBuilder content = new StringBuilder();
		for (Object o : collection) {
			if (content.length() > 0) content.append(",\n");
			content.append(o);
		}
		return content.toString();
	}
	
	
	/**
	 * Loop over the alliance types and the allais to create a full list of alliances
	 * @param doc
	 * @return
	 */
	protected String buildAllyList(SolrDocument doc) {
		List<StringBuilder> alliances = new ArrayList<>();
		
		// Create the first section of the alliance list
		if (doc.getFieldValues("alliance_ss") == null) return ""; 
		
		for (Object o : doc.getFieldValues("alliance_ss")) {
			StringBuilder s = new StringBuilder(50);
			s.append(o).append(" -- ");
			alliances.add(s);
		}
		
		// Add the ally names
		int i = 0;
		for (Object o : doc.getFieldValues("ally_ss")) {
			alliances.get(i++).append(o);
		}

		// Loop over all alliances to create a full list
		StringBuilder complete = new StringBuilder();
		for (StringBuilder s : alliances) {
			complete.append(s).append(",\n");
		}
		
		return complete.toString();
	}
	
	
	/**
	 * Create the header row and set up styling for it.
	 * @param sheet
	 * @param wb
	 */
	private void createHeader(Sheet sheet, Workbook wb) {
		List<String> headers = new ArrayList<>();
		int count = 0;
		int green = 0;
		int blue = 0;
		count++;
		headers.add("Product Id");
		
		if (!excludeColumns.contains("1")){
			count++;
			headers.add("Product Name");
		}
		if (!excludeColumns.contains("2")){
			count++;
			headers.add("Company");
		}
		if (!excludeColumns.contains("b")){
			count++;
			headers.add("Ownership");
		}
		if (!excludeColumns.contains("3")){
			count++;
			headers.add("Segment");
		}
		if (!excludeColumns.contains("4")){
			count++;
			headers.add("Target Market");
		}
		if (!excludeColumns.contains("5")){
			count++;
			headers.add("Indication");
		}
		if (!excludeColumns.contains("6")){
			count++;
			headers.add("Classification");
		}
		if (!excludeColumns.contains("7")){
			count++;
			headers.add("Technology");
		}
		if (!excludeColumns.contains("8")){
			count++;
			headers.add("Approach");
		}
		if (!excludeColumns.contains("9")){
			green = count;
			count += 2;
			headers.add("Us Path");
			headers.add("US Status");
		}
		if (!excludeColumns.contains("e")){
			blue = count;
			count += 3;
			headers.add("International Region");
			headers.add("International Path");
			headers.add("International Status");
		}
		if (!excludeColumns.contains("c")){
			count++;
			headers.add("Country");
		}
		if (!excludeColumns.contains("d")){
			count++;
			headers.add("State");
		}
		if (!excludeColumns.contains("a")){
			count++;
			headers.add("Strategic Alliances");
		}
		
		Row row = sheet.createRow(0);
		Cell cell;
		int c = 0;

		//Loop Headers and set cell values.
		for(String n : headers) {
			cell = row.createCell(c++);
			cell.setCellValue(n);
			cell.setCellStyle(header);
		}

		if(green != 0) {
			row.getCell(green++).setCellStyle(headerGreen);
			row.getCell(green).setCellStyle(headerGreen);
		}

		if(blue != 0) {
			row.getCell(blue++).setCellStyle(headerBlue);
			row.getCell(blue++).setCellStyle(headerBlue);
			row.getCell(blue).setCellStyle(headerBlue);
		}
	}

	
	/**
	 * Ensure that a list of solr documents was supplied and, if so, set the data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o == null) return;
		if (!(o instanceof Map)) return;
		excludeColumns = (String) ((Map<String, Object>)o).get("columns");
		data = (Collection<SolrDocument>) ((Map<String, Object>)o).get("data");
	}
	
	
}
