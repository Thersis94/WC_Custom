package com.biomed.smarttrak.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

import com.biomed.smarttrak.fd.FinancialDashColumnSet.DisplayType;
import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.siliconmtn.data.Node;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
    private String sortField;
    private int sortOrder;
    private SmarttrakTree sections;

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
		
		if (!StringUtil.isEmpty(dash.getCompanyId())) {
			// Company view groups data by parent markets
			addDataRowGroups();
		} else {
			// Market view lists sub-markets or companies
			addDataRows(dash.getRows());
		}
		
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
		List<CountryType> countryTypes = dash.getSelectedCountryTypes();
		
		//construct the title
		StringBuilder title = new StringBuilder(50);
		title.append(reportTitle);
		if(!countryTypes.isEmpty()){
			String region = countryTypes.get(0).toString();
			title.append(" - Region : ").append(region);
		}
		
		Cell cell = row.createCell(0);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue(title.toString());
		cell.setCellStyle(cellStyles.get(CellStyleName.TITLE));

		// Merge the title cell across additional cells to display full title
		int range = 5;
		if(dash.getColHeaders().getDisplayType() == DisplayType.ALL) { range++; }
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,range));
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
	 * Adds the group title rows to the excel report
	 */
	protected void addGroupTitleRow(String marketName) {
		row = sheet.createRow(rowCount++);
		row.setHeightInPoints((short) 20);
		
		Cell cell = row.createCell(0);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue(marketName);
		cell.setCellStyle(cellStyles.get(CellStyleName.TITLE));

		// Merge across additional cells
		int range = 5;
		if(dash.getColHeaders().getDisplayType() == DisplayType.ALL) { range++; }
		sheet.addMergedRegion(new CellRangeAddress(rowCount-1,rowCount-1,0,range));
	}
	
	/**
	 * Returns the currency format used by the report
	 * 
	 * @return
	 */
	protected NumberFormat getCurrencyFormat() {
		// All values are in US dollars
		Locale usLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
		NumberFormat curFormat = NumberFormat.getCurrencyInstance(usLocale);
		curFormat.setMaximumFractionDigits(0);
		
		return curFormat;
	}
	
	/**
	 * Returns the percentage format used by the report
	 * 
	 * @return
	 */
	protected NumberFormat getPercentFormat() {
		NumberFormat pctFormat = NumberFormat.getPercentInstance();
		pctFormat.setMinimumFractionDigits(1);
		
		return pctFormat;
	}
	
	/**
	 * Adds the data rows of the Excel report.
	 * 
	 * @param dataRows
	 * @return
	 */
	protected Map<String, Integer> addDataRows(List<FinancialDashDataRowVO> dataRows) {
		// Adds the header for the rows in this group
		addHeaderRow();
		
		// Get the required formatters
		NumberFormat curFormat = getCurrencyFormat();
		NumberFormat pctFormat = getPercentFormat();
		
		// Setup to increment totals for the totals row
		Map<String, Integer> totals = initTotals(dataRows.get(0));
		
		// Sort according to specified sort order
		FinancialDashDataRowComparator comparator = new FinancialDashDataRowComparator(sortField, sortOrder, sections, dash.getTableType());
		Collections.sort(dataRows, comparator);
		
		int i = 0;
		for (FinancialDashDataRowVO fdRow : dataRows) {
			addExcelRowsFromFdRow(fdRow, totals, curFormat, pctFormat, i);
			i++;
		}
		
		// Generate the totals row
		addTotalRow(totals);
		
		// Return the totals when needed for group summaries
		return totals;
	}
	
	/**
	 * Adds a totals row to the report
	 * 
	 * @param totals
	 */
	protected void addTotalRow(Map<String, Integer> totals) {
		Map<String, Object> totalRow = new HashMap<>();
		totalRow.put(NAME, "Total");
		
		for (Entry<String, Integer> entry : totals.entrySet()) {
			totalRow.put(entry.getKey(), entry.getValue());
		}
		
		addDollarRow(totalRow, getCurrencyFormat(), -1);
	}
	
	/**
	 * Groups data rows in the report based on market/section hierarchy
	 */
	protected void addDataRowGroups() {
		// Process the groups of data for the report
		Map<String, List<FinancialDashDataRowVO>> parentRows = new LinkedHashMap<>();
		Map<String, List<Node>> parentGroups = new LinkedHashMap<>();
		setupReportGroups(parentRows, parentGroups);
		
		// Add each upper level group of data to the report
		for (Map.Entry<String, List<Node>> entry : parentGroups.entrySet()) {
			Map<String, Integer> groupTotals = new LinkedHashMap<>();
			Node groupNode = sections.findNode(entry.getKey());
			addGroupTitleRow(groupNode.getNodeName());
			
			// Add every parent market
			for (Node parentNode : entry.getValue()) {
				addGroupTitleRow(parentNode.getNodeName());
				
				// Add the rows & generate the group totals
				List<FinancialDashDataRowVO> rows = parentRows.get(parentNode.getNodeId());
				Map<String, Integer> parentTotals = addDataRows(rows);
				for (Entry<String, Integer> total : parentTotals.entrySet()) {
					int groupTotal = Convert.formatInteger(groupTotals.get(total.getKey()));
					groupTotals.put(total.getKey(), groupTotal + total.getValue());
				}
			}
			
			// Generate the totals row
			addTotalRow(groupTotals);
		}
	}
	
	/**
	 * Groups the raw rows of data into rows by market and further by an uppper level market
	 * 
	 * @param parentRows
	 * @param parentGroups
	 */
	private void setupReportGroups(Map<String, List<FinancialDashDataRowVO>> parentRows, Map<String, List<Node>> parentGroups) {
		// Group all rows by their parent in the hierarchy
		for (FinancialDashDataRowVO fdRow : dash.getRows()) {
			Node node = sections.findNode(fdRow.getPrimaryKey());
			
			List<FinancialDashDataRowVO> rows = parentRows.get(node.getParentId());
			if (rows == null) {
				rows = new ArrayList<>();
				parentRows.put(node.getParentId(), rows);
			}
			
			rows.add(fdRow);
		}
		
		// Group all found parents by their corresponding third level in the hierarchy,
		// which is where all parents are summarized in the report.
		for (Map.Entry<String, List<FinancialDashDataRowVO>> entry : parentRows.entrySet()) {
			Node parentNode = sections.findNode(entry.getKey());
			
			Node groupNode = sections.findNode(parentNode.getNodeId());
			while (groupNode.getDepthLevel() > 3) {
				groupNode = sections.findNode(groupNode.getParentId());
			}
			
			List<Node> nodes = parentGroups.get(groupNode.getNodeId());
			if (nodes == null) {
				nodes = new ArrayList<>();
				parentGroups.put(groupNode.getNodeId(), nodes);
			}
			
			nodes.add(parentNode);
		}
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

	/**
	 * @return the sortField
	 */
	public String getSortField() {
		return sortField;
	}

	/**
	 * @param sortField the sortField to set
	 */
	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	/**
	 * @return the sortOrder
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * @param sortOrder the sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * @return the sections
	 */
	public SmarttrakTree getSections() {
		return sections;
	}

	/**
	 * @param sections the sections to set
	 */
	public void setSections(SmarttrakTree sections) {
		this.sections = sections;
	}
	
}
