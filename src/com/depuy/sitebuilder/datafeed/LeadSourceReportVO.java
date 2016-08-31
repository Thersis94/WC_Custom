package com.depuy.sitebuilder.datafeed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.depuy.sitebuilder.datafeed.LeadSourceReport.ReportData;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LeadSourceReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> generates a non html based report to be streamed to the user
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 26, 2016<p/>
 * @updates:
 ****************************************************************************/
public class LeadSourceReportVO extends AbstractDataFeedReportVO {


	private static final long serialVersionUID = -149078367340081688L;

	private Map<Date, ReportData> dataSource;
	private String groupType;
	private String startDate;
	private String endDate;
	private String productCode;
	private Map<String, Integer> reportHeaders;
	private String[] jointTypes;
	private int cellCounter;
	private int rowTotal;
	private enum leadTypes {
		CONTACT("Contact", 0), RESPONSE("Response", 1), LEAD("Lead", 5), QUALIFIED("Qualified", 10), 
		POST_SURGERY("Post-Surgery", 15 );
		String textValue;
		int rank; 

		leadTypes(String textValue, int rank){
			this.textValue = textValue;
			this.rank = rank;
		}
	}
	


	public LeadSourceReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("LeadSourceReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setRequestData(SMTServletRequest req) {
		this.groupType = StringUtil.checkVal(req.getParameter("groupType"));
		this.startDate = StringUtil.checkVal(req.getParameter("startDate"));
		this.endDate = StringUtil.checkVal(req.getParameter("endDate"));
		this.productCode = StringUtil.checkVal(req.getParameter("productCode"));
		this.reportHeaders = (Map<String, Integer>) req.getAttribute("reportHeader");
		this.jointTypes = (String[]) req.getAttribute("jointTypes");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		if (this.dataSource == null) return new byte[0];
		log.debug("starting generateReport()");

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();

		//make a heading font we can use to separate the sections
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		
		// the cell styles to use in different sections of the report
		CellStyle titleStyle = wb.createCellStyle();
		titleStyle.setFont(font);

		CellStyle centerStyle = getCenterStyle(font, wb);
				
		CellStyle greyCellStyle = getGreyStyle(font, wb);
		
		CellStyle borderStyle = getBorderStyle( wb);

		//generate the rows
		addDateRow(s, titleStyle);

		addSourceRow(s, centerStyle);

		addProdRow(s, centerStyle); 

		addLeadTypeRow(s, greyCellStyle);

		addDataRows(s,greyCellStyle, borderStyle);

		addTotalRow(s, centerStyle);

		//stream the WorkBook 
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException ioe) {
			log.error("could not write output stream", ioe);
		} finally {
			try { 
				wb.close(); 
			} catch (Exception e) {
				log.error("could not close ", e );
			}
		}

		return new byte[0];
	}

	/**
	 * returns a cell style thats is just borders
	 * @param font
	 * @param wb
	 * @return
	 */
	private CellStyle getBorderStyle( Workbook wb) {
		CellStyle borderStyle = wb.createCellStyle();
		
		borderStyle.setBorderBottom(CellStyle.BORDER_THIN);
		borderStyle.setBorderTop(CellStyle.BORDER_THIN);
		borderStyle.setBorderRight(CellStyle.BORDER_THIN);
		borderStyle.setBorderLeft(CellStyle.BORDER_THIN);
		
		return borderStyle;
	}

	/**
	 * this generates a cell style that with a grey background and thin borders
	 * @param font
	 * @param wb
	 * @return
	 */
	private CellStyle getGreyStyle(Font font, Workbook wb) {
		CellStyle greyCellStyle = wb.createCellStyle();
		
		greyCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
		greyCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		greyCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		greyCellStyle.setBorderTop(CellStyle.BORDER_THIN);
		greyCellStyle.setBorderRight(CellStyle.BORDER_THIN);
		greyCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		
		greyCellStyle.setFont(font);
		
		return greyCellStyle;
	}

	/**
	 * returns a cell style that centers text, has a border and a grey background with both text
	 * @param font 
	 * @param wb 
	 * @return
	 */
	private CellStyle getCenterStyle(Font font, Workbook wb) {
		CellStyle centerStyle = wb.createCellStyle();
		centerStyle.setAlignment(CellStyle.ALIGN_CENTER);
		
		centerStyle.setBorderBottom(CellStyle.BORDER_THIN);
		centerStyle.setBorderTop(CellStyle.BORDER_THIN);
		centerStyle.setBorderRight(CellStyle.BORDER_THIN);
		centerStyle.setBorderLeft(CellStyle.BORDER_THIN);
		
		centerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
		centerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		centerStyle.setFont(font);
		
		return centerStyle;
	}

	/**
	 * adds the bottom row of the report
	 * @param s
	 * @param centerStyle
	 */
	private void addTotalRow(Sheet s, CellStyle centerStyle) {
		int rowNo = s.getPhysicalNumberOfRows();
		Row r = s.createRow(rowNo);
		int counter = 0;
		int total = 0;
		// the label
		Cell c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue("Total");
		counter++;
		for ( Entry<String, Integer> entry : reportHeaders.entrySet() ){
			// you must merge the cells before setting style
			s.addMergedRegion(new CellRangeAddress(rowNo,rowNo,counter,counter+19));
			c = r.createCell(counter);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellStyle(centerStyle);
			c.setCellValue(entry.getValue());
			total += entry.getValue();
			counter += 20;
		}

		c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue(total);

	}

	/**
	 * adds the data rows
	 * @param s
	 * @param greyCellStyle 
	 * @param borderStyle 
	 * @param headingStyle
	 */
	private void addDataRows(Sheet s, CellStyle greyCellStyle, CellStyle borderStyle) {
		 
		for (Entry<Date, ReportData> entry: dataSource.entrySet()){
			int rowNo = s.getPhysicalNumberOfRows();
			Row r = s.createRow(rowNo);
			//must be set to zero every row
			this.cellCounter = 0;
			this.rowTotal = 0;

			Cell c = r.createCell(this.cellCounter);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellStyle(greyCellStyle);
			c.setCellValue(formatDate(entry.getKey()));
			this.cellCounter++;
			
			
			fillDataCells(r, c, entry, borderStyle);
			
			c = r.createCell(this.cellCounter);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellStyle(greyCellStyle);
			c.setCellValue(this.rowTotal);
		}
	
		//making an empty row
		addEmptyRow(s, greyCellStyle, borderStyle);
	}

	/**
	 * adds an empty row after all the data rows
	 * @param borderStyle 
	 * @param greyCellStyle 
	 * @param s 
	 * 
	 */
	private void addEmptyRow(Sheet s, CellStyle greyCellStyle, CellStyle borderStyle) {
		Row r2 = s.createRow(s.getPhysicalNumberOfRows());
		
		Cell c2 = r2.createCell(0);
		c2.setCellType(Cell.CELL_TYPE_STRING);
		c2.setCellStyle(greyCellStyle);
		c2.setCellValue("");
		
		int colNum = 1+ (reportHeaders.size() * jointTypes.length * leadTypes.values().length);
		
		for (int ct = 1 ; ct< colNum-1 ; ct++){
		c2 = r2.createCell(ct);
		c2.setCellType(Cell.CELL_TYPE_STRING);
		c2.setCellStyle(borderStyle);
		c2.setCellValue("");
		}
		
		c2 = r2.createCell(colNum);
		c2.setCellType(Cell.CELL_TYPE_STRING);
		c2.setCellStyle(greyCellStyle);
		c2.setCellValue("");
	}

	/**
	 * fills in each data cell
	 * @param borderStyle 
	 * @param entry 
	 * @param c 
	 * @param r 
	 * 
	 */
	private void fillDataCells(Row r, Cell c, Entry<Date, ReportData> entry, CellStyle borderStyle) {
		for(String callSrc : reportHeaders.keySet()){
			for(String joint : jointTypes){
				for (leadTypes type : leadTypes.values()){

					int srcCount = entry.getValue().getCount(callSrc, joint, type.rank);

					// you must merge the cells before setting style
					c = r.createCell(this.cellCounter);
					c.setCellType(Cell.CELL_TYPE_STRING);
					c.setCellStyle(borderStyle);
					c.setCellValue(srcCount);

					this.rowTotal += srcCount;
					this.cellCounter++;
				}
			}
		}
	}

	/**
	 * @param key
	 * @return
	 */
	private String formatDate(Date key) {

		if("1".equals(groupType)){
			return Convert.formatDate(key, Convert.DATE_LONG);
		}else if ("2".equals(groupType)){ 
			return Convert.formatDate(key, "MMMM dd, yyyy");
		}else{
			return Convert.formatDate(key, "MMMM, yyyy");
		}

	}

	/**
	 * adds the lead type row
	 * @param s
	 * @param greyCellStyle 
	 */
	private void addLeadTypeRow(Sheet s, CellStyle greyCellStyle) {
		int rowNo = s.getPhysicalNumberOfRows();
		Row r = s.createRow(rowNo);
		int counter = 0;
		// the label
		Cell c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(greyCellStyle);
		c.setCellValue("");
		counter++;
		int loopLimit = reportHeaders.size() * jointTypes.length;
		for(int i=0; i<loopLimit; i++){
			for (int x=0; x<leadTypes.values().length; x++){
				// you must merge the cells before setting style
				c = r.createCell(counter);
				c.setCellType(Cell.CELL_TYPE_STRING);
				c.setCellValue(leadTypes.values()[x].textValue);

				counter++;
			}
		}
		c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(greyCellStyle);
		c.setCellValue("");

	}

	/**
	 * adds the products row to the report
	 * @param s
	 * @param centerStyle 
	 */
	private void addProdRow(Sheet s, CellStyle centerStyle) {
		int rowNo = s.getPhysicalNumberOfRows();
		Row r = s.createRow(rowNo);
		int counter = 0;
		// the label
		Cell c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue("");
		counter++;
		for(int i=0; i<reportHeaders.size(); i++){
			for (int x=0; x<jointTypes.length; x++){
				// you must merge the cells before setting style
				s.addMergedRegion(new CellRangeAddress(rowNo,rowNo,counter,counter+4));
				c = r.createCell(counter);
				c.setCellType(Cell.CELL_TYPE_STRING);
				c.setCellStyle(centerStyle);
				c.setCellValue(jointTypes[x]);

				counter += 5;
			}
		}
		c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue("");

	}

	/**
	 * adds the source row to the report
	 * @param s
	 * @param centerStyle 
	 */
	private void addSourceRow(Sheet s, CellStyle centerStyle) {
		int rowNo = s.getPhysicalNumberOfRows();
		Row r = s.createRow(rowNo);
		int counter = 0;
		// the label
		Cell c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue("Date");
		counter++;
		for (String key : reportHeaders.keySet() ){
			// you must merge the cells before setting style
			s.addMergedRegion(new CellRangeAddress(rowNo,rowNo,counter,counter+19));
			c = r.createCell(counter);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellStyle(centerStyle);
			c.setCellValue(key);

			counter += 20;
		}

		c = r.createCell(counter);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellStyle(centerStyle);
		c.setCellValue("Total");

	}

	/**
	 * @param s
	 * @param headingStyle
	 */
	private void addDateRow(Sheet s, CellStyle headingStyle) {
		Row r = s.createRow(0);
		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);

		StringBuilder sb = new StringBuilder();


		if ("3".equals(groupType)){
			sb.append("Monthly ");
		}else if ("2".equals(groupType)) {
			sb.append("Weekly ");
		}else {
			sb.append("Daily ");
		}

		sb.append("Lead Report ");

		if (productCode != null && !productCode.isEmpty()){
			sb.append("for ").append(productCode).append(" ");
		}

		sb.append("- ").append((startDate != null && !startDate.isEmpty())?  "From " + startDate :""  );
		sb.append((endDate != null && !endDate.isEmpty())?  "To " + endDate : "To Today "  );

		c.setCellValue(sb.toString());

		c.setCellStyle(headingStyle);

		//number of columns
		int colNum = 1+ (reportHeaders.size() * jointTypes.length * leadTypes.values().length);

		s.addMergedRegion(new CellRangeAddress(0,0,0,colNum));

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.dataSource = (Map<Date, ReportData>) o;

	}

}
