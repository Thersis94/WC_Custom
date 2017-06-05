package com.biomed.smarttrak.vo.grid;

// JDK 1.8.x
import java.io.ByteArrayOutputStream;
import java.util.List;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// Apache POI
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

// App Libs
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.admin.vo.GridVO.RowStyle;

// SMT Base Libs
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>GridExcelManager.java<br/>
 * <b>Description: </b>Takes a gridVO and converts it into a style Excel document<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 4, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GridExcelManager {

	private Logger log = Logger.getLogger(GridExcelManager.class);
	
	/**
	 * 
	 */
	public GridExcelManager() {
		super();
	}

	/**
	 * Converts a Grid into an Excel Spreadsheet
	 * @param grid
	 * @return
	 * @throws ApplicationException
	 */
	public byte[] getExcelFile(GridVO grid) throws ApplicationException {
		byte[] data;
		try (HSSFWorkbook workbook = new HSSFWorkbook()) {
			log.debug("Title: " + StringUtil.removeNonAlphaNumeric(grid.getTitle(), false));
			HSSFSheet sheet = workbook.createSheet(StringUtil.removeNonAlphaNumeric(grid.getTitle(), false));
			
			List<GridDetailVO> details = grid.getDetails();
			String[] series = grid.getSeries();
			
			int numberCols = grid.getNumberColumns();
			log.debug("Number of columns: " + numberCols);
			
			// Create the columns first
			int ctr = 1;
			this.addHeadingLabel(workbook, sheet, numberCols, grid.getTitle());
			
			Row row = sheet.createRow(ctr++);
			row.setHeightInPoints((int)(1.5 * sheet.getDefaultRowHeightInPoints()));
			Cell cell = row.createCell(0);
			cell.setCellValue("");
			cell.setCellStyle(getHeaderStyle(workbook));
			
			for (int i=0; i < (numberCols); i++) {
				cell = row.createCell(i+1);
				cell.setCellValue(series[i]);
				cell.setCellStyle(getHeaderStyle(workbook));
			}
			
			// Add the rows of data
			for (GridDetailVO detail : details ) {
				row = sheet.createRow(ctr++);
				row.setHeightInPoints((int)(1.5 * sheet.getDefaultRowHeightInPoints()));
				cell = row.createCell(0);
				cell.setCellValue(detail.getLabel());
				HSSFCellStyle style = getRowStyle(workbook, detail.getDetailType(), false);
				style.setAlignment(HSSFCellStyle.ALIGN_LEFT);
				cell.setCellStyle(style);
				
				for (int i=0; i < (numberCols); i++) {
					cell = row.createCell(i + 1);
					boolean neg = false;
					if (Convert.formatDouble(detail.getValues()[i]) < 0) neg = true;
					if (neg) log.info("Val: " + detail.getDetailType() + "|" + detail.getValues()[i] + "|" + Convert.formatDouble(detail.getValues()[i]));
					
					cell.setCellValue(detail.getValues()[i]);
					cell.setCellStyle(getRowStyle(workbook, detail.getDetailType(), neg));
				}
			}
			
			// resize all of the columns
			for (int i=0; i < numberCols; i++) sheet.autoSizeColumn(i);
			
			// Add the workbook to the stream and store to the byte[]
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				workbook.write(baos);
				data = baos.toByteArray();
			} 
			
		} catch(Exception e) {
			log.error("Unable to create excel file", e);
			throw new ApplicationException("Unable to create excel file", e);
		}
		
		return data;
	}
	
	
	@SuppressWarnings("deprecation")
	public void addHeadingLabel(HSSFWorkbook workbook, HSSFSheet sheet, int numColumns, String name) {
		
		Row row = sheet.createRow(0);
		Cell cell = row.createCell(0);
		CellRangeAddress range = new CellRangeAddress(0, 0, 0, numColumns);
		sheet.addMergedRegion(range);
		cell.setCellValue("SmartTRAK® - " + name);
		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
		cell.setCellStyle(getHeadingLabelStyle(workbook));
	}
	
	/**
	 * Sets the styles for the heading label row
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getHeadingLabelStyle(HSSFWorkbook workbook) {
		HSSFFont font = getBaseFont(workbook, false);
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(true);
		font.setFontHeightInPoints((short)14);

		HSSFCellStyle style = getBaseStyle(workbook);
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		HSSFPalette palette = workbook.getCustomPalette();
		short backColor = palette.findSimilarColor(208, 208, 208).getIndex();
		style.setFillForegroundColor(backColor);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setIndention((short) 1);
		
		return style;
	}
	
	/**
	 * Sets the styles for the header row
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getHeaderStyle(HSSFWorkbook workbook) {
		HSSFFont font = getBaseFont(workbook, false);
		font.setColor(HSSFColor.WHITE.index);
		font.setBold(true);
		HSSFCellStyle style = getBaseStyle(workbook);
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		HSSFPalette palette = workbook.getCustomPalette();
		short backColor = palette.findSimilarColor(0, 69, 134).getIndex();
		style.setFillForegroundColor(backColor);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setIndention((short) 1);
		
		return style;
	}
	
	/**
	 * Determines the row style type (data, heading total, sub-total) and 
	 * builds the appropriate styles for the sheet 
	 * @param workbook
	 * @param type
	 * @return
	 */
	public HSSFCellStyle getRowStyle(HSSFWorkbook workbook, String type, boolean neg) {
		if (StringUtil.isEmpty(type)) type = "DATA";
		RowStyle rsType = RowStyle.valueOf(type);
		HSSFCellStyle style = null;
		switch(rsType) {
			case HEADING:
				style = getHeadingStyle(workbook);
				break;
			case SUB_TOTAL:
				style = getSubTotalStyle(workbook, neg);
				break;
			case TOTAL:
				style = getTotalStyle(workbook, neg);
				break;
			default:
				style = getDataStyle(workbook, neg);
				break;
		}

		return style;
	}
	
	/**
	 * Gets the styles for headings
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getTotalStyle(HSSFWorkbook workbook, boolean neg) {
		HSSFCellStyle style= getBaseStyle(workbook);
		style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderBottom((short)1);
		style.setBorderTop((short)1);
		
		HSSFFont font = getBaseFont(workbook, neg);
		style.setFont(font);
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(true);
		
		return style;
	}
	
	/**
	 * Gets the styles for headings
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getSubTotalStyle(HSSFWorkbook workbook, boolean neg) {
		HSSFCellStyle style= getBaseStyle(workbook);
		style.setFillForegroundColor(HSSFColor.WHITE.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderBottom((short)1);
		style.setBorderTop((short)1);
		
		HSSFFont font = getBaseFont(workbook, neg);
		if (! neg) font.setColor(HSSFColor.GREY_50_PERCENT.index);
		font.setItalic(true);
		font.setBold(true);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Gets the styles for Data elements
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getDataStyle(HSSFWorkbook workbook, boolean neg) {
		HSSFCellStyle style= getBaseStyle(workbook);
		
		HSSFFont font = getBaseFont(workbook, neg);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Gets the styles for headings
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getHeadingStyle(HSSFWorkbook workbook) {
		HSSFCellStyle style= getBaseStyle(workbook);
		HSSFPalette palette = workbook.getCustomPalette();
		short backColor = palette.findSimilarColor(61, 120, 216).getIndex();
		
		style.setFillForegroundColor(backColor);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderLeft((short)0);
		style.setBorderRight((short)0);
		style.setBorderBottom((short)1);
		style.setBorderTop((short)1);
		
		HSSFFont font = getBaseFont(workbook, false);
		font.setColor(HSSFColor.WHITE.index);
		font.setBold(true);
		style.setFont(font);
		
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

