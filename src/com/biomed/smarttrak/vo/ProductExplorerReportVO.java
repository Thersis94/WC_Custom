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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.solr.common.SolrDocument;

import com.smt.sitebuilder.search.SearchDocumentHandler;
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
	
	// Member Variables
	Map<String, Object> solrAttribs = new HashMap<>();
	CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); 

	
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
			row.createCell(j++).setCellValue((String)doc.getFieldValue(SearchDocumentHandler.TITLE));
			row.createCell(j++).setCellValue((String)doc.getFieldValue("company_s"));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("sectionname_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("target_market_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("indication_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("classification_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("technology_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("approach_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("uspathnm_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("usstatusnm_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("intregionnm_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("intpathnm_ss")));
			row.createCell(j++).setCellValue(buildContentList(doc.getFieldValues("intstatusnm_ss")));
			row.createCell(j++).setCellValue(buildAllyList(doc));
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
		List<String> headers = new ArrayList<String>();
		headers.add("Product Name");
		headers.add("Company");
		headers.add("Segment");
		headers.add("Target Market");
		headers.add("Indication");
		headers.add("Classification");
		headers.add("Technology");
		headers.add("Approach");
		headers.add("Us Path");
		headers.add("US Status");
		headers.add("International Region");
		headers.add("International Path");
		headers.add("International Status");
		headers.add("Strategic Alliances");
		
		Row row = sheet.createRow(0);
		Cell cell;
		int c = 0;
		
		CellStyle style = wb.createCellStyle();
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderTop(CellStyle.BORDER_MEDIUM);
		style.setBorderBottom(CellStyle.BORDER_MEDIUM);

		//Loop Headers and set cell values.
		for(String n : headers) {
			cell = row.createCell(c++);
			cell.setCellValue(n);
			cell.setCellStyle(style);
		}
	}

	
	/**
	 * Ensure that a list of solr documents was supplied and, if so, set the data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o == null) return;
		if (!(o instanceof Collection)) return;
		data = (Collection<SolrDocument>) o;
	}
	
	
}
