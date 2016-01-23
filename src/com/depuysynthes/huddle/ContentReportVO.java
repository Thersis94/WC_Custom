package com.depuysynthes.huddle;

// JDK 1.6.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;








// Apache POI Office API
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.solr.common.SolrDocument;

import com.depuy.datafeed.AbstractBaseReport;
import com.smt.sitebuilder.action.AbstractSBReportVO;
// SMT Base Libs
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>:RegistrationReport.java<p/>
 * <b>Description: Returns grouped registration data (and summaries) collected 
 * from the call sources.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Oct 30, 2008
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
	
	
	private void buildWorkbook(Sheet sheet) {
		
		// Loop the queries until all rows have been retrieved
		int i = 0;
		for (SolrDocument doc : data) {
			Row row = sheet.createRow(i++);
			
			int j = 0;
			row.createCell(i++).setCellValue((String)doc.getFieldValue("title"));
			row.createCell(j++).setCellValue((String)doc.getFieldValue("opco_ss"));
		}
	}
	
	
	private void createHeader(Sheet sheet, Workbook wb) {
		List<String> headers = new ArrayList<String>();
		headers.add("Product");
		headers.add("Speciality");
		
		// Add the date and time
		Row row = sheet.createRow(1);
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

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o == null) return;
		if (!(o instanceof List)) return;
		if (((List<?>)o).isEmpty() || !(((List<?>)o).get(0) instanceof SolrDocument)) return;
		data = (List<SolrDocument>) o;
	}
	
	
}
