package com.biomed.smarttrak.fd;

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
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import com.siliconmtn.util.Convert;
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
    private enum CellStyleName {TITLE, HEADER, RIGHT, LEFT, PERCENT_POS, PERCENT_NEG, TOTAL_ROW_POS, TOTAL_ROW_NEG}
    
    private String reportTitle = "SmartTRAK - Financial Dashboard"; 
    private FinancialDashVO dash;
    private HSSFWorkbook wb;
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
		cellStyles.put(CellStyleName.HEADER, setHeaderStyle());
		cellStyles.put(CellStyleName.RIGHT, setRightStyle());
		cellStyles.put(CellStyleName.LEFT, setLeftStyle());
		cellStyles.put(CellStyleName.PERCENT_POS, setPercentStyle(IndexedColors.GREEN.getIndex()));
		cellStyles.put(CellStyleName.PERCENT_NEG, setPercentStyle(IndexedColors.RED.getIndex()));
		cellStyles.put(CellStyleName.TOTAL_ROW_NEG, setTotalNegStyle());
		cellStyles.put(CellStyleName.TOTAL_ROW_POS, setTotalPosStyle());
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
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));
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
		
		int cellCount = 0;		
		for (Map.Entry<String, String> entry : getHeaderData().entrySet()) {
			if (!NAME.equals(entry.getKey())) {
				Cell cell = row.createCell(cellCount++);
				cell.setCellType(Cell.CELL_TYPE_STRING);
				cell.setCellValue(entry.getValue());
				cell.setCellStyle(cellStyles.get(CellStyleName.HEADER));
			} else {
				// Create an empty cell to offset the header row
				Cell cell = row.createCell(cellCount++);
				cell.setCellStyle(cellStyles.get(CellStyleName.HEADER));
			}
		}
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
		
		int i = 0;
		for (FinancialDashDataRowVO fdRow : dash.getRows()) {
			addExcelRowsFromFdRow(fdRow, totals, curFormat, pctFormat, i);
			i++;
		}
		
		// Generate the totals row
		Map<String, Object> totalRow = new HashMap<>();
		totalRow.put(NAME, "Total");
		for (Entry<String, Integer> entry : totals.entrySet()) {
			totalRow.put(entry.getKey(), entry.getValue());
		}
		addDollarRow(totalRow, curFormat, -1);
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
	protected void addExcelRowsFromFdRow(FinancialDashDataRowVO fdRow, Map<String, Integer> totals, NumberFormat curFormat, NumberFormat pctFormat, int rowNum) {
		Map<String, Object> dollarRow = new HashMap<>();
		Map<String, Object> percentRow = new HashMap<>();
		
		dollarRow.put(NAME, fdRow.getName());
		percentRow.put(NAME, "");

		for (Entry<String, FinancialDashDataColumnVO> entry : fdRow.getColumns().entrySet()) {
			dollarRow.put(entry.getKey(), entry.getValue().getDollarValue());
			percentRow.put(entry.getKey(), entry.getValue().getPctDiff());
			totals.put(entry.getKey(), totals.get(entry.getKey()) + entry.getValue().getDollarValue());
		}
	
		addDollarRow(dollarRow, curFormat, rowNum);
		addPercentRow(percentRow, pctFormat, rowNum);
	}
	
	/**
	 * Adds/formats the data in the dollar rows
	 */
	protected void addDollarRow(Map<String, Object> dollarRow, NumberFormat curFormat, int rowNum) {
		row = sheet.createRow(rowCount++);
		
		int cellCount = 0;
		for(String key : getHeaderData().keySet()) {
			Cell cell = row.createCell(cellCount++);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			
			String value;
			if (NAME.equals(key)) {
				value = (String) dollarRow.get(key);
			} else {
				value = curFormat.format((int) dollarRow.get(key));
			}
			
			if (rowNum == -1) {
				cell.setCellStyle(cellStyles.get(Convert.formatInteger(value) < 0? CellStyleName.TOTAL_ROW_NEG : CellStyleName.TOTAL_ROW_POS));
			} else if (!NAME.equals(key)){
				cell.setCellStyle(cellStyles.get(CellStyleName.RIGHT));
			} else {
				cell.setCellStyle(cellStyles.get(CellStyleName.LEFT));
			}
			
			cell.setCellValue(value);
		}
	}
	
	/**
	 * Adds/formats the data in the percentage rows. The percentage rows have special formatting requirements such
	 * that, negative values are red, positive values are green, and zero values are black.
	 */
	protected void addPercentRow(Map<String, Object> percentRow, NumberFormat pctFormat, int rowNum) {
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
		HSSFFont font = getBaseFont(wb, false);
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(true);
		font.setFontHeightInPoints((short)14);

		HSSFCellStyle style = getBaseStyle(wb);
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		HSSFPalette palette = wb.getCustomPalette();
		short backColor = palette.findSimilarColor(208, 208, 208).getIndex();
		style.setFillForegroundColor(backColor);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setIndention((short) 1);
		
		return style;
	}
	
	/**
	 * Creates the Header style
	 * 
	 * @return
	 */
	protected CellStyle setHeaderStyle() {
		HSSFFont font = getBaseFont(wb, false);
		font.setColor(HSSFColor.WHITE.index);
		font.setBold(true);
		HSSFCellStyle style = getBaseStyle(wb);
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		HSSFPalette palette = wb.getCustomPalette();
		short backColor = palette.findSimilarColor(0, 69, 134).getIndex();
		style.setFillForegroundColor(backColor);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setIndention((short) 1);
		
		return style;
	}
	
	/**
	 * Creates the Right Aligned style
	 * 
	 * @return
	 */
	protected CellStyle setRightStyle() {
		HSSFCellStyle style= getBaseStyle(wb);
		
		HSSFFont font = getBaseFont(wb, false);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Creates the Left Aligned style
	 * 
	 * @return
	 */
	protected CellStyle setLeftStyle() {
		HSSFCellStyle style= getBaseStyle(wb);
		style.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		HSSFFont font = getBaseFont(wb, false);
		style.setFont(font);
		
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
	
	/**
	 * Create a style specifically for the totals row when negative
	 * 
	 * @return
	 */
	protected CellStyle setTotalNegStyle() {
		HSSFCellStyle style= getBaseStyle(wb);
		style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderBottom((short)1);
		style.setBorderTop((short)1);
		
		HSSFFont font = getBaseFont(wb, true);
		style.setFont(font);
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(true);
		
		return style;
	}
	
	/**
	 * Create a style specifically for the totals row when positive
	 * 
	 * @return
	 */
	protected CellStyle setTotalPosStyle() {
		HSSFCellStyle style= getBaseStyle(wb);
		style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderBottom((short)1);
		style.setBorderTop((short)1);
		
		HSSFFont font = getBaseFont(wb, false);
		style.setFont(font);
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(true);
		
		return style;
	}

	
	/**
	 * Sets the base cell styles
	 * @param workbook
	 * @return
	 */
	private HSSFCellStyle getBaseStyle(HSSFWorkbook workbook) {
		HSSFCellStyle style = workbook.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		style.setIndention((short) 1);
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		
		return style;
	}
	
	/**
	 * Creates the base fonts
	 * @param workbook
	 * @return
	 */
	private HSSFFont getBaseFont(HSSFWorkbook workbook, boolean neg) {
		HSSFFont font = workbook.createFont();
		font.setFontHeightInPoints((short)10);
		font.setFontName("Arial");
		if (neg) font.setColor(HSSFColor.RED.index);
		else font.setColor(HSSFColor.BLACK.index);
		
		font.setBold(false);
		font.setItalic(false);
		
		return font;
	}
	
}
