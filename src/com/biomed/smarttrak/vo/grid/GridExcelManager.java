package com.biomed.smarttrak.vo.grid;

// JDK 1.8.x
import java.io.ByteArrayOutputStream;
import java.util.List;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// Apache POI
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
			log.info("Title: " + StringUtil.removeNonAlphaNumeric(grid.getTitle(), false));
			HSSFSheet sheet = workbook.createSheet(StringUtil.removeNonAlphaNumeric(grid.getTitle(), false));
			
			List<GridDetailVO> details = grid.getDetails();
			String[] series = grid.getSeries();
			
			int numberCols = grid.getNumberColumns();
			
			// Create the columns first
			int ctr = 0;
			Row row = sheet.createRow(ctr++);
			Cell cell = row.createCell(0);
			cell.setCellValue("");
			cell.setCellStyle(getHeaderStyle(workbook));

			for (int i=0; i < (numberCols - 1); i++) {
				cell = row.createCell(i+1);
				cell.setCellValue(series[i]);
				cell.setCellStyle(getHeaderStyle(workbook));
			}
			
			// Add the rows of data
			for (GridDetailVO detail : details ) {
				row = sheet.createRow(ctr++);
				cell = row.createCell(0);
				cell.setCellValue(detail.getLabel());
				cell.setCellStyle(getRowStyle(workbook, detail.getDetailType()));
				for (int i=0; i < (numberCols - 1); i++) {
					cell = row.createCell(i + 1);
					cell.setCellValue(detail.getValues()[i]);
					cell.setCellStyle(getRowStyle(workbook, detail.getDetailType()));
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
	
	/**
	 * Sets the styles for the header row
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getHeaderStyle(HSSFWorkbook workbook) {
		HSSFFont font = getBaseFont(workbook);
		font.setColor(HSSFColor.WHITE.index);
		font.setBold(true);
		
		HSSFCellStyle style = getBaseStyle(workbook);
		style.setFont(font);
		style.setFillForegroundColor(HSSFColor.DARK_BLUE.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderLeft((short)0);
		style.setBorderRight((short)0);
		
		return style;
	}
	
	/**
	 * Determines the row style type (data, heading total, sub-total) and 
	 * builds the appropriate styles for the sheet 
	 * @param workbook
	 * @param type
	 * @return
	 */
	public HSSFCellStyle getRowStyle(HSSFWorkbook workbook, String type) {
		if (StringUtil.isEmpty(type)) type = "DATA";
		RowStyle rsType = RowStyle.valueOf(type);
		HSSFCellStyle style = null;
		switch(rsType) {
			case HEADING:
				style = getHeadingStyle(workbook);
				break;
			case DATA:
			case UNCHARTED_DATA:
				style = getDataStyle(workbook);
				break;
			case SUB_TOTAL:
				style = getSubTotalStyle(workbook);
				break;
			case TOTAL:
				style = getTotalStyle(workbook);
				break;
		}
		
		return style;
	}
	
	/**
	 * Gets the styles for headings
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getTotalStyle(HSSFWorkbook workbook) {
		HSSFCellStyle style= getBaseStyle(workbook);
		style.setFillForegroundColor(HSSFColor.BLACK.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		HSSFFont font = getBaseFont(workbook);
		style.setFont(font);
		font.setColor(HSSFColor.WHITE.index);
		font.setBold(true);
		
		return style;
	}
	
	/**
	 * Gets the styles for headings
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getSubTotalStyle(HSSFWorkbook workbook) {
		HSSFCellStyle style= getBaseStyle(workbook);
		style.setFillForegroundColor(HSSFColor.GREY_50_PERCENT.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		HSSFFont font = getBaseFont(workbook);
		style.setFont(font);
		
		return style;
	}
	
	/**
	 * Gets the styles for Data elements
	 * @param workbook
	 * @return
	 */
	public HSSFCellStyle getDataStyle(HSSFWorkbook workbook) {
		HSSFCellStyle style= getBaseStyle(workbook);
		
		HSSFFont font = getBaseFont(workbook);
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
		style.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderLeft((short)0);
		style.setBorderRight((short)0);
		
		HSSFFont font = getBaseFont(workbook);
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
		style.setBorderBottom((short)1);
		style.setBorderLeft((short)1);
		style.setBorderRight((short)1);
		style.setBorderTop((short)1);
		return style;
	}
	
	/**
	 * Creates the base fonts
	 * @param workbook
	 * @return
	 */
	private HSSFFont getBaseFont(HSSFWorkbook workbook) {
		HSSFFont font = workbook.createFont();
		font.setFontHeightInPoints((short)12);
		font.setFontName("Arial");
		font.setColor(HSSFColor.BLACK.index);
		font.setBold(false);
		font.setItalic(false);
		
		return font;
	}
}

