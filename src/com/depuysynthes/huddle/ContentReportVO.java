package com.depuysynthes.huddle;

// JDK 1.8
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.siliconmtn.data.report.ExcelReport;
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
	private static final long serialVersionUID = 187654323987L;

	private List<SolrDocument> data;

	public ContentReportVO() {
		super();
	}


	/**
	 * Build Excel File
	 */
	public byte[] generateReport() {
		HSSFWorkbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet("Huddle Products");
		createHeader(sheet, workbook);
		buildWorkbook(sheet);
		return ExcelReport.getBytes(workbook);
	}


	/**
	 * Build the report's content.  Loop the queries until all rows have been retrieved
	 * @param sheet
	 */
	private void buildWorkbook(Sheet sheet) {
		int rowNo = 0;
		for (SolrDocument doc : data) {
			Row row = sheet.createRow(++rowNo);

			int cellNo = 0;
			row.createCell(cellNo).setCellValue((String)doc.getFieldValue(SearchDocumentHandler.TITLE));

			StringBuilder opCoNms = new StringBuilder(200);
			concatValues(doc.getFieldValues(HuddleUtils.SOLR_OPCO_FIELD), opCoNms);
			row.createCell(++cellNo).setCellValue(opCoNms.toString());

			StringBuilder cList = new StringBuilder(200);
			concatValues(doc.getFieldValues(SearchDocumentHandler.HIERARCHY), cList);
			row.createCell(++cellNo).setCellValue(cList.toString());

			StringBuilder images = new StringBuilder(200);
			StringBuilder documents = new StringBuilder(200);

			// Loop over all fields to ensure all custom attributes are
			// added to the excel file
			for (String name : doc.getFieldNames()) {
				if (name == null || !name.startsWith(HuddleUtils.PROD_ATTR_PREFIX) || doc.get(name) == null) continue;

				if (name.contains(HuddleUtils.PROD_ATTR_IMG_TYPE.toLowerCase())) {
					concatValues(doc.getFieldValues(name), images);

				} else if (name.contains(HuddleUtils.PROD_ATTR_MB_TYPE.toLowerCase())) {
					concatValues(doc.getFieldValues(name), documents);
				}
			}
			String str = images.toString();
			row.createCell(++cellNo).setCellValue(!str.isEmpty() ? str.split(",").length : 0);
			row.createCell(++cellNo).setCellValue(str);
			str = documents.toString();
			row.createCell(++cellNo).setCellValue(!str.isEmpty() ? str.split(",").length : 0);
			row.createCell(++cellNo).setCellValue(str);
		}
	}

	/**
	 * reusable method to turn a list into a delimited string
	 * @param records
	 * @param str
	 */
	private void concatValues(Collection<Object> records, StringBuilder str) {
		if (records == null || str == null || records.isEmpty()) return;
		for (Object o : records) {
			if (str.length() > 0) str.append(", ");
			str.append((String)o);
		}
	}


	/**
	 * creates the header row for Excel
	 * @param sheet
	 * @param wb
	 */
	private void createHeader(Sheet sheet, Workbook wb) {
		List<String> headers = new ArrayList<>();
		headers.add("Product Name");
		headers.add("Speciality");
		headers.add("Categories");
		headers.add("Image Count");
		headers.add("Images");
		headers.add("Document Count");
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
		if (o == null || !(o instanceof List<?>)) return;
		data = (List<SolrDocument>) o;
	}
}