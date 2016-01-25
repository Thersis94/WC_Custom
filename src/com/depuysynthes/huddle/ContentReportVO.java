package com.depuysynthes.huddle;

// JDK 1.6.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache POI Office API
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.solr.common.SolrDocument;

// SMT Base Libs
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: ContentReportVO.java<p/>
 * <b>Description: Build an excel file out of the supplied list
 * of solr documents.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 25, 2016
 ****************************************************************************/
public class ContentReportVO extends AbstractSBReportVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<SolrDocument> data = new ArrayList<SolrDocument>();
	
	// Member Variables
	Map<String, Object> solrAttribs = new HashMap<>();
	CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); 

	
	public ContentReportVO() {
		super();
	}
	
	
	
	/**
	 * 
	 */
	public byte[] generateReport() {
		
		//Build Excel File
		HSSFWorkbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet("Huddle Products");
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
		// Loop the queries until all rows have been retrieved
		int i = 1;
		for (SolrDocument doc : data) {
			Row row = sheet.createRow(i++);
			
			int j = 0;
			row.createCell(j++).setCellValue((String)doc.getFieldValue(SearchDocumentHandler.TITLE));
			row.createCell(j++).setCellValue((String)doc.getFieldValue(HuddleUtils.SOLR_OPCO_FIELD));
			
			Collection<Object> categories = doc.getFieldValues(SearchDocumentHandler.HIERARCHY);
			StringBuilder cList = new StringBuilder(100);
			
			// Iterate over the hierarchies and add to the report
			if (categories != null) {
				for (Object o : categories) {
					if (o == null) continue;
					if (cList.length() > 0) cList.append(", ");
					cList.append((String)o);
				}
				
			}
			row.createCell(j++).setCellValue(cList.toString());
			
			StringBuilder images = new StringBuilder(100);
			StringBuilder documents = new StringBuilder(100);
			
			// Loop over all fields to ensure all custom attributes are
			// added to the excel file
			for (String name : doc.getFieldNames()) {
				if (name == null || !name.startsWith(HuddleUtils.PROD_ATTR_PREFIX)
						|| doc.get(name) == null) continue;
				if (name.contains(HuddleUtils.PROD_ATTR_IMG_TYPE.toLowerCase())) {
					for (Object o : doc.getFieldValues(name)) {
						if (images.length() > 0) images.append(", ");
						images.append((String)o);
					}
				} else if (name.contains(HuddleUtils.PROD_ATTR_MB_TYPE.toLowerCase())) {
					for (Object o : doc.getFieldValues(name)) {
						if (documents.length() > 0) documents.append(", ");
						documents.append((String)o);
					}
				}
			}
			row.createCell(j++).setCellValue(images.toString());
			row.createCell(j++).setCellValue(documents.toString());
		}
	}
	
	
	private void createHeader(Sheet sheet, Workbook wb) {
		List<String> headers = new ArrayList<String>();
		headers.add("Product Name");
		headers.add("Speciality");
		headers.add("Categories");
		headers.add("Images");
		headers.add("Documents");
		
		// Add the date and time
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
		
		// Set the column sizes.
		sheet.setColumnWidth(0, 8000);
		sheet.setColumnWidth(1, 5000);
		sheet.setColumnWidth(2, 12000);
		sheet.setColumnWidth(3, 12000);
		sheet.setColumnWidth(4, 12000);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o == null) return;
		if (!(o instanceof List)) return;
		if (((List<?>)o).isEmpty() || !(((List<?>)o).get(0) instanceof SolrDocument)) return;
		data = (List<SolrDocument>) o;
	}
	
	
}
