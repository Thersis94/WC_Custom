package com.biomed.smarttrak;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: FinancialDashReportVO.java</p>
 <p>Generates a report of data from the financial dashboard.</p>
 <p>Copyright: Copyright (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Feb 20, 2017
 ***************************************************************************/

public class FinancialDashReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    
    private static final String NAME = "NAME";
    private enum CellStyleName {TITLE, HEADER_LEFT, HEADER_RIGHT, RIGHT, PERCENT_POS, PERCENT_NEG}
    
    private String reportTitle = "SmartTRAK - Financial Dashboard";
    private FinancialDashVO dash;
    private Workbook wb;
    private Sheet sheet;
    private Row row;
    private int rowCount;
    private Map<CellStyleName, CellStyle> cellStyles;

    public FinancialDashReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Financial-Dashboard.xls");
    }
    
    /**
     * Assigns the financial dashboard data
     * 
     * @param data (FinancialDashVO)
     */
    @Override
	public void setData(Object data) {
    	dash = (FinancialDashVO) data;
    }
    
	@Override
	public byte[] generateReport() {
		log.debug("Starting FinancialDashReport generateReport()");
		if (dash == null) return new byte[0];

		// Create Excel Object
		wb = new HSSFWorkbook();
		sheet = wb.createSheet();
		setCellStyles();
		
		// Add the rows
		addTitleRow();
		addEmptyRow();
		addHeaderRow();
		addDataRows();

		// Format so everthing can be seen when opened
		for (Cell cell : row)
			sheet.autoSizeColumn(cell.getColumnIndex());

		// Stream the workbook back to the browser
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
	 * Sets the special cell styles used by this report
	 */
	protected void setCellStyles() {
		cellStyles = new EnumMap<>(CellStyleName.class);
		
		cellStyles.put(CellStyleName.TITLE, setTitleStyle());
		cellStyles.put(CellStyleName.HEADER_LEFT, setHeaderLeftStyle());
		cellStyles.put(CellStyleName.HEADER_RIGHT, setHeaderRightStyle());
		cellStyles.put(CellStyleName.RIGHT, setRightStyle());
		cellStyles.put(CellStyleName.PERCENT_POS, setPercentStyle(IndexedColors.GREEN.getIndex()));
		cellStyles.put(CellStyleName.PERCENT_NEG, setPercentStyle(IndexedColors.RED.getIndex()));
	}
	
	/**
	 * Adds the title row to the excel report
	 */
	protected void addTitleRow() {		
		row = sheet.createRow(rowCount++);
		row.setHeightInPoints((short) 24);
		
		Cell cell = row.createCell(0);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue(reportTitle);
		cell.setCellStyle(cellStyles.get(CellStyleName.TITLE));

		// Merge the title cell across additional cells to display full title
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,8));
	}
	
	/**
	 * Builds the header map for the Excel report.
	 * 
	 * @return
	 */
	protected HashMap<String, String> getHeaderData() {
		HashMap<String, String> headers = new LinkedHashMap<>();
		headers.put(NAME, dash.getTableTypeName());
		headers.putAll(dash.getColHeaders().getColumns());
		
		return headers;
	}
	
	/**
	 * Adds the header row to the Excel report
	 */
	protected void addHeaderRow() {
		row = sheet.createRow(rowCount++);
		CellStyle style;
		
		int cellCount = 0;		
		for (Map.Entry<String, String> entry : getHeaderData().entrySet()) {
			style = cellStyles.get(CellStyleName.HEADER_RIGHT);
			if (NAME.equals(entry.getKey())) {
				style = cellStyles.get(CellStyleName.HEADER_LEFT);
			}
			
			Cell cell = row.createCell(cellCount++);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			cell.setCellValue(entry.getValue());
			cell.setCellStyle(style);
		}
	}
	
	/**
	 * Adds an empty row when needed
	 */
	protected void addEmptyRow() {
		row = sheet.createRow(rowCount++);
	}

	/**
	 * Adds the data rows of the Excel report.
	 * 
	 * @param rows
	 * @return
	 */
	protected void addDataRows() {
		// All values are in US dollars
		Locale usLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
		NumberFormat curFormat = NumberFormat.getCurrencyInstance(usLocale);
		curFormat.setMaximumFractionDigits(0);

		NumberFormat pctFormat = NumberFormat.getPercentInstance();
		pctFormat.setMinimumFractionDigits(1);
		
		// Setup to increment totals for the totals row
		Map<String, Integer> totals = initTotals(dash.getRows().get(0));
		
		for (FinancialDashDataRowVO fdRow : dash.getRows()) {
			addExcelRowsFromFdRow(fdRow, totals, curFormat, pctFormat);
		}
		
		// Generate the totals row
		Map<String, Object> totalRow = new HashMap<>();
		totalRow.put(NAME, "Total");
		for (Entry<String, Integer> entry : totals.entrySet()) {
			totalRow.put(entry.getKey(), entry.getValue());
		}
		addDollarRow(totalRow, curFormat);
	}
	
	/**
	 * Initializes a set of totals from data in an existing row.
	 * 
	 * @param fdRow
	 * @return
	 */
	protected Map<String, Integer> initTotals(FinancialDashDataRowVO fdRow) {
		Map<String, Integer> totals = new HashMap<>();
		
		for (Entry<String, FinancialDashDataColumnVO> entry : fdRow.getColumns().entrySet()) {
			totals.put(entry.getKey(), 0);
		}
		
		return totals;
	}
	
	/**
	 * Breaks the single financial dash rows into multiple excel rows (one for dollar, other for percent)
	 * 
	 * @param fdRow
	 * @param totals
	 * @param curFormat
	 * @param pctFormat
	 * @return
	 */
	protected void addExcelRowsFromFdRow(FinancialDashDataRowVO fdRow, Map<String, Integer> totals, NumberFormat curFormat, NumberFormat pctFormat) {
		Map<String, Object> dollarRow = new HashMap<>();
		Map<String, Object> percentRow = new HashMap<>();
		
		dollarRow.put(NAME, fdRow.getName());
		percentRow.put(NAME, "");

		for (Entry<String, FinancialDashDataColumnVO> entry : fdRow.getColumns().entrySet()) {
			dollarRow.put(entry.getKey(), entry.getValue().getDollarValue());
			percentRow.put(entry.getKey(), entry.getValue().getPctDiff());
			totals.put(entry.getKey(), totals.get(entry.getKey()) + entry.getValue().getDollarValue());
		}
	
		addDollarRow(dollarRow, curFormat);
		addPercentRow(percentRow, pctFormat);
	}
	
	/**
	 * Adds/formats the data in the dollar rows
	 */
	protected void addDollarRow(Map<String, Object> dollarRow, NumberFormat curFormat) {
		row = sheet.createRow(rowCount++);
		
		int cellCount = 0;
		for(String key : getHeaderData().keySet()) {
			Cell cell = row.createCell(cellCount++);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			
			String value;
			if (NAME.equals(key)) {
				value = (String) dollarRow.get(key);
			} else {
				cell.setCellStyle(cellStyles.get(CellStyleName.RIGHT));
				value = curFormat.format((int) dollarRow.get(key));
			}
			
			cell.setCellValue(value);
		}
	}
	
	/**
	 * Adds/formats the data in the percentage rows. The percentage rows have special formatting requirements such
	 * that, negative values are red, positive values are green, and zero values are black.
	 */
	protected void addPercentRow(Map<String, Object> percentRow, NumberFormat pctFormat) {
		row = sheet.createRow(rowCount++);
		
		int cellCount = 0;
		for(String key : getHeaderData().keySet()) {
			Cell cell = row.createCell(cellCount++);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			
			String value = "";
			if (percentRow.get(key) != null && !NAME.equals(key)) {
				Double dblValue = (Double) percentRow.get(key);
				value = pctFormat.format(dblValue);
				
				if (dblValue > 0) {
					cell.setCellStyle(cellStyles.get(CellStyleName.PERCENT_POS));
				} else if (dblValue < 0) {
					cell.setCellStyle(cellStyles.get(CellStyleName.PERCENT_NEG));
				}
			}
			
			cell.setCellValue(value);
		}
	}
	
	/**
	 * Creates the Report Title style
	 * 
	 * @return
	 */
	protected CellStyle setTitleStyle() {
		CellStyle style = wb.createCellStyle();
		
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short) 18);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Creates the Left Aligned Header style
	 * 
	 * @return
	 */
	protected CellStyle setHeaderLeftStyle() {
		CellStyle style = wb.createCellStyle();
		
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Creates the Right Aligned Header style
	 * 
	 * @return
	 */
	protected CellStyle setHeaderRightStyle() {
		CellStyle style = setHeaderLeftStyle();
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		
		return style;
	}
	
	/**
	 * Creates the Right Aligned style
	 * 
	 * @return
	 */
	protected CellStyle setRightStyle() {
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		
		return style;
	}
	
	/**
	 * Creates a Percent style with configurable color for positive/negative values
	 * 
	 * @return
	 */
	protected CellStyle setPercentStyle(short color) {
		CellStyle style = setRightStyle();
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(color);
		style.setFont(font);
		
		return style;
	}
}
