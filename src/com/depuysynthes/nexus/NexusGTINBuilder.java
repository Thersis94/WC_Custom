package com.depuysynthes.nexus;

//JDK 1.7.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//Apache HSSF Libs
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

// Solr Libs
import org.apache.solr.common.SolrDocument;

//SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NexusGTINBuilder.java<p/>
 * <b>Description: After the NeXus product files have been imported, this class will
 * build a spreadsheet for each operating company, zip the file and write it to the 
 * file system</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since July 16, 2015
 * @updates 
 * 
 ****************************************************************************/
public class NexusGTINBuilder extends CommandLineUtil {
	
	/**
	 * List of organization files to process
	 */
	public static final List<String> ORGANIZATIONS = new ArrayList<String>(8) {
		private static final long serialVersionUID = 1L; {
			add("ALL");
			add("CMF");
			add("Trauma");
			add("Codman");
			add("Mitek");
			add("Spine");
			add("Orthopaedics");
		}
		
	};
	
	/**
	 * Key for the property for the location of the output excel file
	 */
	public static final String GTIN_FILE_PATH = "gtinFilePath";
	
	/**
	 * Key for the property for the location of the DePuy Synthes logo
	 */
	public static final String LOGO_FILE_PATH = "logoFilePath";
	
	/**
	 * Base name of the exported spreadsheet
	 */
	public static final String FILE_NAME_BASE = "DS_GTIN_";
	
	/**
	 * Spreadsheet extension
	 */
	public static final String FILE_NAME_EXT = ".xlsx";
	
	/**
	 * Name of the solr field to search
	 */
	public static final String SOLR_FIELD_NAME = "organizationName";
	
	/**
	 * Number of rows to retrieve on a given query from solr
	 */
	public static final int NUMBER_SOLR_RESPONSES = 10000;
	
	/**
	 * Row to start diplaying actual data
	 */
	public static final int DATA_START_ROW = 13;
	
	// Member Variables
	Map<String, Object> solrAttribs = new HashMap<>();
	Picture pict = null;
	
	/**
	 * 
	 * @param args
	 */
	public NexusGTINBuilder(String[] args) {
		super(args);
		loadProperties("scripts/Nexus.properties");
		
		// Add the solr info to the map
		solrAttribs.put(Constants.SOLR_BASE_URL, props.get(Constants.SOLR_BASE_URL));
		solrAttribs.put(Constants.SOLR_COLLECTION_NAME, props.get(Constants.SOLR_COLLECTION_NAME));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		NexusGTINBuilder bldr = new NexusGTINBuilder(args);
		bldr.run();
		long end = System.currentTimeMillis();
		
		log.info("Time to build: " + ((end - start) / 1000));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		log.info("Starting Application");
		Map<String, String> messages = new HashMap<>();
		
		for (String org : ORGANIZATIONS) {
			int count = 0;
			
			try {
				count = querySolr(org);
				
				// add a success message
				messages.put(org, "Successfully Processed " + count + " Records");
				
			} catch (Exception e) {
				log.error("Unable to build excel files", e);
				messages.put(org, "Error: " + e.getMessage());
			}
		}
		
		// Send an email report
		try {
			this.sendEmail(getEmailMessage(messages), null, null);
		} catch(Exception e) {
			log.error("Unable to send email report", e);
		}
		
		log.info("Exports Completed");
	}
	
	/**
	 * Queries solr and creates the excel file
	 * @param org
	 * @return Number of records processed
	 * @throws IOException
	 * @throws FileException
	 */
	public int querySolr(String org) throws IOException, FileException {
		log.info("Starting Org: " + org);
		
		//Build Excel File
		Workbook wb = createWorkbook(org);
		Sheet sheet = wb.getSheetAt(0);
		this.createHeader(sheet, wb);
		
		// Loop the queries until all rows have been retrieved
		int start = 0;
		while(true) {
	 		SolrActionVO vo = new SolrActionVO();
			vo.setNumberResponses(NUMBER_SOLR_RESPONSES);
			vo.setStartLocation(start);
			vo.setOrganizationId("DPY_SYN_NEXUS");
			if (! "ALL".equals(org)) 
				vo.addSolrField(new SolrFieldVO(FieldType.SEARCH, SOLR_FIELD_NAME, org, BooleanType.AND));
			
			SolrQueryProcessor sqp = new SolrQueryProcessor(solrAttribs);
			SolrResponseVO res = sqp.processQuery(vo);
			addRow(res.getResultDocuments(), sheet, start);
			
			// See if there are more records to get
			start += NUMBER_SOLR_RESPONSES;
			if (start > res.getTotalResponses()) {
				start = (int)res.getTotalResponses();
				break;
			}
		}
		
		// add the footer label
		//this.addFooter(sheet, wb, start);
		
		// resize the columns
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
		sheet.autoSizeColumn(4);
		
		// Set the image size ** This must be done AFTER the cell resizing
		pict.resize();
		
		// store the file to the file system
		try {
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			wb.write(baos);
			closeExcelFile(baos.toByteArray(), org);
		} catch(Exception e) {
			log.error("Unable to write file", e);
		}
		
		return start;
	}
	
	/**
	 * Adds the rows to the sheet for a given excel file
	 * @param docs
	 * @param sheet
	 * @param start
	 */
	public void addRow(List<SolrDocument> docs, Sheet sheet, int start) {
		int r = start + (DATA_START_ROW + 1);
		for(SolrDocument prod : docs) {
			int c = 0;
			Row row = sheet.createRow(r++);
			
			// get the gtin and uom value (This value may contain multiples.  
			// Need to pull the primary (0 location) first
			Collection<Object> col = prod.getFieldValues("gtin");
			Object gtin = prod.get("gtin");
			if (col != null && col.size() > 0) gtin = col.toArray()[0];
			
			col = prod.getFieldValues("uomLvl");
			Object uom = prod.get("uomLvl");
			if (col != null && col.size() > 0) uom = col.toArray()[0];
			
			// Add cells
			addCell(c++, prod.get("documentId") + "", row);
			addCell(c++, gtin + "", row);
			addCell(c++, prod.get("summary") + "", row);
			addCell(c++, prod.get("organizationName") + "", row);
			addCell(c++, uom + "", row);
		}
	}
	
	/**
	 * Helper method that adds a Cell to the given row.
	 * @param cellPos
	 * @param value
	 * @param r
	 */
	private void addCell(int cellPos, String value, Row r) {
		Cell cell = r.createCell(cellPos);
		cell.setCellValue(value);
	}
	
	/**
	 * Helper method that provides the list of Headers to be written.
	 * @return
	 */
	protected void createHeader(Sheet sheet, Workbook wb) {
		List<String> headers = new ArrayList<String>();
		headers.add("Product #");
		headers.add("GTIN");
		headers.add("Product Desc");
		headers.add("Operating Company");
		headers.add("Unit of Measure");
		
		// Add the date and time
		Row row = sheet.createRow(DATA_START_ROW - 2);
		Cell cell = row.createCell((short)0);
		cell.setCellValue("Date: " + Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
		sheet.addMergedRegion(new CellRangeAddress(DATA_START_ROW - 2,DATA_START_ROW - 2,0,2));
		
		// Create the data header
		row = sheet.createRow(DATA_START_ROW);
		int c = 0;
		
		//Loop Headers and set cell values.
		for(String n : headers) {
			CellStyle style = wb.createCellStyle();
		    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		    style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		    style.setBorderTop(CellStyle.BORDER_MEDIUM);
		    style.setBorderBottom(CellStyle.BORDER_MEDIUM);
		    cell = row.createCell(c++);
			cell.setCellValue(n);
		    cell.setCellStyle(style);
		}
	}

	/**
	 * Adds a footer declaration
	 * @param sheet
	 * @param wb
	 * @param start
	 */
	protected void addFooter(Sheet sheet, Workbook wb, int start) {
		Row row = sheet.createRow(start + 5);
		Cell cell = row.createCell(0);
		cell.setCellValue("This document contains sensitive data that is highly restricted");
		sheet.addMergedRegion(new CellRangeAddress(start + 5,start + 5,0,4));
	}
	
	/**
	 * Adds some labels to the header
	 * @param sheet
	 * @param wb
	 */
	protected void addLabel(Sheet sheet, Workbook wb) {
		// set the font
		Font font = wb.createFont();
	    font.setFontHeightInPoints((short)24);
	    font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
		
		// Add the first label
	    int startRow = DATA_START_ROW - 6;
		Row row = sheet.createRow(startRow);
		Cell cell = row.createCell(0);
		cell.setCellValue("Unique Device Identification");
		CellStyle style = wb.createCellStyle();
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		cell.setCellStyle(style);
		sheet.addMergedRegion(new CellRangeAddress(startRow, startRow,0,4));
		
		// Add the second label
		startRow = DATA_START_ROW - 5;
		row = sheet.createRow(startRow);
		cell = row.createCell(0);
		cell.setCellValue("GTIN Report");
		cell.setCellStyle(style);
		sheet.addMergedRegion(new CellRangeAddress(startRow, startRow,0,4));
	}
	
	/**
	 * 
	 * @param excel
	 * @param org
	 * @throws FileWriterException
	 * @throws InvalidDataException
	 * @throws IOException
	 */
	public void closeExcelFile(byte[] excel, String org) 
	throws FileWriterException, InvalidDataException, IOException {
		// Build the file and path info
		String path = props.getProperty(GTIN_FILE_PATH);
		String fileName = FILE_NAME_BASE + org.toUpperCase() + FILE_NAME_EXT;
		
		// Zip the file contents
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		ZipEntry entry = new ZipEntry(fileName);
		entry.setSize(excel.length);
		zos.putNextEntry(entry);
		zos.write(excel);
		zos.closeEntry();
		zos.close();
		
		// Write the file to the file system
		FileManager fm = new FileManager();
		fm.writeFiles(baos.toByteArray(), path, fileName + ".zip", false, true);
	}

	/**
	 * Initializes the workbook and sheet and adds the depuy logo to the sheet
	 * @return
	 * @throws FileException
	 */
	public Workbook createWorkbook(String org) throws FileException {
		// Create the workbook and initial sheet
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet(org + " Product GTINS");
		
		// Set the default font for the sheet
		Font font = workbook.getFontAt((short)0);
	    font.setFontHeightInPoints((short)12);
	    
		// Grab the depuy logo
		FileManager fm = new FileManager();
		byte[] imageBytes = fm.retrieveFile(props.getProperty(LOGO_FILE_PATH) + "");
		
		// Insert the logo onto the default sheet
		int pictureureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
		CreationHelper helper = workbook.getCreationHelper();
		Drawing drawing = sheet.createDrawingPatriarch();
		ClientAnchor anchor = helper.createClientAnchor();
		anchor.setAnchorType(ClientAnchor.MOVE_DONT_RESIZE);
		
		// Set the cell location
		anchor.setCol1(1);
		anchor.setRow1(0);
		
		// Add the image
		pict = drawing.createPicture(anchor, pictureureIdx);
		
		// Add the labels
		this.addLabel(sheet, workbook);
		return workbook;
	}
	
	/**
	 * Builds the HTML info for the email report.  Each organization will be placed
	 * in the message (inside a table row) with the message (success or failure).
	 * If the org processed successfully, the number of records in the file will be displayed.
	 * If the process errored on the org, the error message will be displayed
	 * @param messages
	 * @return
	 */
	protected StringBuilder getEmailMessage(Map<String, String> messages) {
		StringBuilder sb = new StringBuilder();
		sb.append("<p>The GTIN Download File Process has finished processing data for <b>");
		sb.append(Convert.formatDate(new Date(), Convert.DATE_FULL_MONTH));
		sb.append("</b></p><p>&nbsp;</p>");
		
		// set styles
		sb.append("<style>\n");
		sb.append("table { width: 500px; border-spacing: 0;border-collapse: collapse;} \n");
		sb.append("td { border: solid 1px black; padding: 5px;} \n");
		sb.append(".hdr { background: gray; color:white;text-align:center; } \n");
		sb.append(".err { background: red; } \n");
		sb.append(".normal { background: lightgreen; } \n");
		sb.append("</style> \n");
		
		sb.append("<table><tr><td class='hdr' colspan='2'><b>NeXus GTIN Download Report</b></td></tr>");
		
		for (String  key: messages.keySet()) {
			String message = StringUtil.checkVal(messages.get(key));
			String rowType = "<tr class='normal'>";
			if (message.startsWith("Error:")) rowType = "<tr class='err'>";
			
			sb.append(rowType).append("<td>").append(key).append("</td><td >");
			sb.append(message).append("</td></tr>");
		}
		
		sb.append("</table>");
		return sb;
	}
}
