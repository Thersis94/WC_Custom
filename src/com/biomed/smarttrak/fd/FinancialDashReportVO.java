package com.biomed.smarttrak.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
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
import org.apache.poi.ss.util.CellUtil;

import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.siliconmtn.data.Node;
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
    private enum CellStyleName {TITLE, HEADER, GROUP_TITLE, PARENT_TITLE, RIGHT, LEFT, CURRENCY, PERCENT, PERCENT_POS, PERCENT_NEG}
    private enum DataRowType {STANDARD, TOTAL, GROUP_TOTAL}
    private enum DataType {CURRENCY, PERCENT}
    
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
		for (Cell cell : row) {
			// Auto size to the non-formatted data
			sheet.autoSizeColumn(cell.getColumnIndex(), false);
			
			// Add width for numerical cell formatting characters: $ % , .
			// 256 equals a character's maximum width for the font
			int curWidth = sheet.getColumnWidth(cell.getColumnIndex());
			sheet.setColumnWidth(cell.getColumnIndex(), curWidth + 512);
		}

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
		cellStyles.put(CellStyleName.GROUP_TITLE, setGroupingTitleStyle((short) 12, (short) 0));
		cellStyles.put(CellStyleName.PARENT_TITLE, setGroupingTitleStyle((short) 11, (short) 1));
		cellStyles.put(CellStyleName.RIGHT, setRightStyle());
		cellStyles.put(CellStyleName.LEFT, setLeftStyle());
		cellStyles.put(CellStyleName.CURRENCY, setCurrencyStyle());
		cellStyles.put(CellStyleName.PERCENT, setPercentStyle(IndexedColors.BLACK.getIndex()));
		cellStyles.put(CellStyleName.PERCENT_POS, setPercentStyle(IndexedColors.GREEN.getIndex()));
		cellStyles.put(CellStyleName.PERCENT_NEG, setPercentStyle(IndexedColors.RED.getIndex()));
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
		int range = dash.getColHeaders().getColumns().size();
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
	 * Adds the group/parent title rows to the excel report
	 * 
	 * @param marketName
	 * @param isGroup
	 * @param spacerRow
	 */
	protected void addGroupTitleRow(String marketName, boolean isOuterLevel, boolean blankRow) {
		// Number of additional cells to merge across
		int range = dash.getColHeaders().getColumns().size();

		if (blankRow) {
			row = sheet.createRow(rowCount++);
			sheet.addMergedRegion(new CellRangeAddress(rowCount-1,rowCount-1,0,range));
		}
		
		row = sheet.createRow(rowCount++);
		sheet.addMergedRegion(new CellRangeAddress(rowCount-1,rowCount-1,0,range));
		
		Cell cell = row.createCell(0);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue(marketName);
		cell.setCellStyle(cellStyles.get(isOuterLevel ? CellStyleName.GROUP_TITLE : CellStyleName.PARENT_TITLE));
		
		// Set row height from the font height
		HSSFCellStyle style = (HSSFCellStyle) cell.getCellStyle();
		row.setHeightInPoints(style.getFont(wb).getFontHeightInPoints() + (float) 4);
	}
	
	/**
	 * Adds the data rows of the Excel report.
	 * 
	 * @param dataRows
	 * @return
	 */
	protected FinancialDashDataRowVO addDataRows(List<FinancialDashDataRowVO> dataRows) {
		// Adds the header for the rows in this group
		addHeaderRow();
		
		// Setup to increment totals for the totals row
		FinancialDashDataRowVO totals = initTotals(null);
		
		// Sort according to specified sort order
		FinancialDashDataRowComparator comparator = new FinancialDashDataRowComparator(sortField, sortOrder, sections, dash.getTableType());
		Collections.sort(dataRows, comparator);
		
		// Add the data rows to the report
		for (FinancialDashDataRowVO fdRow : dataRows) {
			addExcelRowsFromFdRow(fdRow, totals);
		}
		
		// Generate the totals row
		addTotalRow(totals, DataRowType.TOTAL);
		
		// Return the totals when needed for group summaries
		return totals;
	}
	
	/**
	 * Adds a totals row to the report
	 * 
	 * @param totals
	 * @param rowType
	 */
	protected void addTotalRow(FinancialDashDataRowVO totals, DataRowType rowType) {
		for (Entry<String, FinancialDashDataColumnVO> entry : totals.getColumns().entrySet()) {
			int cyValue = entry.getValue().getDollarValue();
			int pyValue = entry.getValue().getPDollarValue();
			if (pyValue > 0) {
				entry.getValue().setPctDiff((double) (cyValue - pyValue) / pyValue);
			}
		}
		
		addDollarRow(totals, rowType);
		addPercentRow(totals, rowType);
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
			Node groupNode = sections.findNode(entry.getKey());
			FinancialDashDataRowVO groupTotals = initTotals(groupNode.getNodeName());
			addGroupTitleRow(groupNode.getNodeName(), true, true);
			
			// Add every parent market
			int index = 0;
			for (Node parentNode : entry.getValue()) {
				addGroupTitleRow(parentNode.getNodeName(), false, index++ > 0);
				
				// Add the rows & and add their totals to the group totals
				List<FinancialDashDataRowVO> rows = parentRows.get(parentNode.getNodeId());
				FinancialDashDataRowVO parentTotals = addDataRows(rows);
				sumColumns(parentTotals, groupTotals);
			}
			
			// Generate the totals row for the upper level grouping
			addTotalRow(groupTotals, DataRowType.GROUP_TOTAL);
		}
	}
	
	/**
	 * Groups the raw rows of data into rows by market, and further by an upper (third) level market
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
	 * @param groupTitle
	 * @return
	 */
	protected FinancialDashDataRowVO initTotals(String groupTitle) {
		FinancialDashDataRowVO totals = new FinancialDashDataRowVO();
		totals.setName("Total " + StringUtil.checkVal(groupTitle));
		
		// Use the columns in the first row of data to initialize the totals row
		// The totals row will share all of the same columns
		for (Entry<String, FinancialDashDataColumnVO> entry : dash.getRows().get(0).getColumns().entrySet()) {
			totals.addColumn(entry.getKey(), new FinancialDashDataColumnVO());
		}
		
		return totals;
	}
	
	/**
	 * Adds data from a row to the running totals in all columns
	 * 
	 * @param newData
	 * @param runningTotals
	 */
	protected void sumColumns(FinancialDashDataRowVO newData, FinancialDashDataRowVO runningTotals) {
		for (Entry<String, FinancialDashDataColumnVO> entry : newData.getColumns().entrySet()) {
			FinancialDashDataColumnVO columnTotal = runningTotals.getColumns().get(entry.getKey());
			columnTotal.setDollarValue(columnTotal.getDollarValue() + entry.getValue().getDollarValue());
			columnTotal.setPDollarValue(columnTotal.getPDollarValue() + entry.getValue().getPDollarValue());
		}
	}
	
	/**
	 * Breaks the single financial dash rows into multiple excel rows (one for dollar, other for percent)
	 * 
	 * @param fdRow
	 * @param totals
	 * @return
	 */
	protected void addExcelRowsFromFdRow(FinancialDashDataRowVO fdRow, FinancialDashDataRowVO totals) {
		sumColumns(fdRow, totals);
		addDollarRow(fdRow, DataRowType.STANDARD);
		addPercentRow(fdRow, DataRowType.STANDARD);
	}
	
	/**
	 * Adds/formats the data in the dollar rows
	 * 
	 * @param fdRow
	 * @param rowType
	 */
	protected void addDollarRow(FinancialDashDataRowVO fdRow, DataRowType rowType) {
		row = sheet.createRow(rowCount++);
		
		int cellCount = 0;
		for(String key : getHeaderData().keySet()) {
			Cell cell = row.createCell(cellCount++);
			
			if (NAME.equals(key)) {
				cell.setCellValue(fdRow.getName());
				cell.setCellType(Cell.CELL_TYPE_STRING);
			} else {
				cell.setCellValue(fdRow.getColumns().get(key).getDollarValue());
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			}
			
			// Apply cell style, and additional styles
			cell.setCellStyle(cellStyles.get(NAME.equals(key) ? CellStyleName.LEFT : CellStyleName.CURRENCY));
			applyRowTypeStyle(cell, rowType, DataType.CURRENCY);
		}
	}
	
	/**
	 * Adds/formats the data in the percentage rows. The percentage rows have special formatting requirements such
	 * that, negative values are red, positive values are green, and zero values are black.
	 * 
	 * @param fdRow
	 * @param rowType
	 */
	protected void addPercentRow(FinancialDashDataRowVO fdRow, DataRowType rowType) {
		row = sheet.createRow(rowCount++);
		
		int cellCount = 0;
		for(String key : getHeaderData().keySet()) {
			Cell cell = row.createCell(cellCount++);
			
			if (!NAME.equals(key) && fdRow.getColumns().get(key).getPctDiff() != null) {
				Double value = fdRow.getColumns().get(key).getPctDiff();
				
				// setCellValue is type dependant, ensure that the proper one is called here
				if (value == -1) {
					cell.setCellValue("-");
				} else {
					cell.setCellValue(value);
					cell.setCellType(Cell.CELL_TYPE_NUMERIC);
				}
				
				if (value == 0 || value == -1) {
					cell.setCellStyle(cellStyles.get(CellStyleName.PERCENT));
				} else {
					cell.setCellStyle(cellStyles.get(value > 0 ? CellStyleName.PERCENT_POS : CellStyleName.PERCENT_NEG));
				}
			} else {
				cell.setCellValue("");
				cell.setCellType(Cell.CELL_TYPE_STRING);
			}
			
			// Apply additional styling to the cell
			applyRowTypeStyle(cell, rowType, DataType.PERCENT);
		}
	}
	
	/**
	 * Creates the Report Title style
	 * 
	 * @return
	 */
	protected CellStyle setTitleStyle() {
		HSSFFont font = getBaseFont(wb);
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
		HSSFFont font = getBaseFont(wb);
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
	 * Creates the group style
	 * 
	 * @return
	 */
	protected CellStyle setGroupingTitleStyle(short fontHeight, short indentation) {
		HSSFFont font = getBaseFont(wb);
		font.setFontHeightInPoints(fontHeight);
		font.setBold(true);
		
		HSSFCellStyle style = getBaseStyle(wb);
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		style.setIndention(indentation);
		
		return style;
	}
	
	/**
	 * Creates the Right Aligned style
	 * 
	 * @return
	 */
	protected CellStyle setRightStyle() {
		HSSFCellStyle style= getBaseStyle(wb);
		
		HSSFFont font = getBaseFont(wb);
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
		HSSFFont font = getBaseFont(wb);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Creates a currency style
	 * 
	 * @return
	 */
	protected CellStyle setCurrencyStyle() {
		CellStyle style = setRightStyle();
		style.setDataFormat((short) 6);
		
		return style;
	}
	
	/**
	 * Creates a Percent style with configurable color for positive/negative values
	 * 
	 * @return
	 */
	protected CellStyle setPercentStyle(short color) {
		CellStyle style = setRightStyle();
		
		HSSFDataFormat df = wb.createDataFormat();
		style.setDataFormat(df.getFormat("#,##0.0%"));
		
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		font.setColor(color);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Applies the specified row style to the cell
	 * 
	 * @param cell
	 * @param rowType
	 */
	protected void applyRowTypeStyle(Cell cell, DataRowType rowType, DataType dataType) {
		switch (rowType) {
			case TOTAL:
				applyTotalRowStyle(cell, dataType);
				break;
			case GROUP_TOTAL:
				applyGroupTotalRowStyle(cell);
				break;
			case STANDARD:
				break;
		}
	}
	
	/**
	 * Applies additional cell formatting required by a totals row
	 * 
	 * @param cell
	 */
	protected void applyTotalRowStyle(Cell cell, DataType dataType) {
		// Create a bold font using the original font's color
		HSSFCellStyle style = (HSSFCellStyle) cell.getCellStyle();
		HSSFFont oldFont = style.getFont(wb);
		HSSFFont newFont = getBaseFont(wb);
		newFont.setColor(oldFont.getColor());
		newFont.setBold(true);

		// Apply the new properties
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.FILL_FOREGROUND_COLOR, HSSFColor.GREY_25_PERCENT.index);
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.FILL_PATTERN, CellStyle.SOLID_FOREGROUND);
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.BORDER_BOTTOM, dataType == DataType.PERCENT ? (short) 1 : (short) 0);
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.BORDER_TOP, dataType == DataType.CURRENCY ? (short) 1 : (short) 0);
		CellUtil.setFont(cell, wb, newFont);
	}
	
	/**
	 * Applies additional celll formatting required by a group total row
	 * 
	 * @param cell
	 */
	protected void applyGroupTotalRowStyle(Cell cell) {
		// Create a bold font using the original font's color
		// If color was black, reverse out to white
		HSSFCellStyle style = (HSSFCellStyle) cell.getCellStyle();
		HSSFFont oldFont = style.getFont(wb);
		HSSFFont newFont = getBaseFont(wb);
		newFont.setColor(oldFont.getColor() == HSSFColor.BLACK.index ? HSSFColor.WHITE.index : oldFont.getColor());
		newFont.setBold(true);

		// Apply the new properties
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.FILL_FOREGROUND_COLOR, HSSFColor.BLACK.index);
		CellUtil.setCellStyleProperty(cell, wb, CellUtil.FILL_PATTERN, CellStyle.SOLID_FOREGROUND);
		CellUtil.setFont(cell, wb, newFont);
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
	private HSSFFont getBaseFont(HSSFWorkbook workbook) {
		HSSFFont font = workbook.createFont();
		font.setFontHeightInPoints((short)10);
		font.setFontName("Arial");
		font.setBold(false);
		font.setItalic(false);
		font.setColor(HSSFColor.BLACK.index);
		
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
