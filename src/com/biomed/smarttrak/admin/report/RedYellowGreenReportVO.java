package com.biomed.smarttrak.admin.report;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title:</b> RedGreenReportVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Smarttrak report for formatting the Account Users login
 * statistics according to the red, yellow, green rules in the User List Report.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 30, 2019
 ****************************************************************************/
public class RedYellowGreenReportVO extends AbstractSBReportVO {

	public enum StyleType {DEFAULT, GREEN_VAL, RED_VAL, YELLOW_VAL, PERCENT, WHITE_HEADER, GREEN_HEADER, RED_HEADER, YELLOW_HEADER, WHITE_BORDER_TOP, GREEN_BORDER_TOP, YELLOW_BORDER_TOP, RED_BORDER_TOP, DEFAULT_BORDER_TOP, DEFAULT_BORDER_HEADER, PERCENT_BORDER_TOP}
	private static final long serialVersionUID = 1L;
	private enum FormatColor {G,Y,R,B,D,W}
	private List<RedYellowGreenVO> data;
	private Map<StyleType, CellStyle> styles;
	public RedYellowGreenReportVO() {
		super();
		super.setFileName("User Login Activity Report.xls");
	}
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		byte[] xcl = null;
		try (HSSFWorkbook workbook = new HSSFWorkbook()) {
			HSSFSheet sheet = workbook.createSheet("Low Usage  Red Yel Green");

			// Add Custom Colors
			HSSFPalette palette = workbook.getCustomPalette();

			// Green
			palette.setColorAtIndex(HSSFColor.GREEN.index, (byte)0, (byte)176, (byte)80);
			// Yellow
			palette.setColorAtIndex(HSSFColor.YELLOW.index, (byte)255, (byte)192, (byte)0);
			// Red
			palette.setColorAtIndex(HSSFColor.RED.index, (byte)255, (byte)4, (byte)0);
			// Blue
			palette.setColorAtIndex(HSSFColor.BLUE.index, (byte)47, (byte)85, (byte)151);

			buildStyles(workbook);

			//Build Header
			int rowCnt = buildHeader(sheet, workbook);

			//Build Data Rows
			for (int i=0; i < data.size(); i++) {
				buildRow(sheet, workbook, data.get(i), rowCnt++);
			}

			//Add Total Row
			buildTotalRow(sheet, workbook, rowCnt);

			//Auto size columns
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);
			sheet.autoSizeColumn(5);
			sheet.autoSizeColumn(6);
			sheet.autoSizeColumn(7);
			sheet.autoSizeColumn(8);
			sheet.autoSizeColumn(9);

			// Add the workbook to the stream and store to the byte[]
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				workbook.write(baos);
				xcl = baos.toByteArray();
			} 

		} catch(Exception e) {
			log.error("Unable to create excel file", e);
		}

		return xcl;
	}

	/**
	 * Prepare the styles ahead of time for use in the report.  Fixes problem
	 * where formatting would randomly fail in the report.
	 * @param workbook
	 * @return
	 */
	private void buildStyles(HSSFWorkbook workbook) {
		styles = new HashMap<>();
		styles.put(StyleType.DEFAULT, getCellFormat(workbook, FormatColor.D, false, false, true));
		styles.put(StyleType.GREEN_VAL, getCellFormat(workbook, FormatColor.G, false, false, true));
		styles.put(StyleType.RED_VAL, getCellFormat(workbook, FormatColor.R, false, false, true));
		styles.put(StyleType.YELLOW_VAL, getCellFormat(workbook, FormatColor.Y, false, false, true));
		styles.put(StyleType.PERCENT, getCellFormat(workbook, FormatColor.D, true, false, true));
		styles.put(StyleType.WHITE_HEADER, getCellFormat(workbook, FormatColor.W, false, true, true));
		styles.put(StyleType.GREEN_HEADER, getCellFormat(workbook, FormatColor.G, false, true, true));
		styles.put(StyleType.RED_HEADER, getCellFormat(workbook, FormatColor.R, false, true, true));
		styles.put(StyleType.YELLOW_HEADER, getCellFormat(workbook, FormatColor.Y, false, true, true));
		styles.put(StyleType.WHITE_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.W, false, true, true)));
		styles.put(StyleType.GREEN_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.G, false, true, true)));
		styles.put(StyleType.YELLOW_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.Y, false, true, true)));
		styles.put(StyleType.RED_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.R, false, true, true)));
		styles.put(StyleType.DEFAULT_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.D, false, false, true)));
		styles.put(StyleType.PERCENT_BORDER_TOP, setBorder(getCellFormat(workbook, FormatColor.D, true, false, true)));
		styles.put(StyleType.DEFAULT_BORDER_HEADER, setBorder(getCellFormat(workbook, FormatColor.D, false, true, true)));
	}

	/**
	 * Build the cell format styles
	 * @param workbook
	 * @param color
	 * @param isPercent
	 * @param isHeader
	 * @param centerText
	 * @return
	 */
	private CellStyle getCellFormat(HSSFWorkbook workbook, FormatColor color, boolean isPercent, boolean isHeader, boolean centerText) {
		CellStyle style = workbook.createCellStyle();
		HSSFFont f = workbook.createFont();
		HSSFPalette palette = workbook.getCustomPalette();
		switch(color) {
			case B:
				f.setColor(palette.getColor(HSSFColor.BLUE.index).getIndex());
				break;
			case Y:
				f.setColor(palette.getColor(HSSFColor.YELLOW.index).getIndex());
				break;
			case R:
				f.setColor(palette.getColor(HSSFColor.RED.index).getIndex());
				break;
			case W:
				f.setColor(HSSFColor.WHITE.index);
				break;
			case G:
				f.setColor(palette.getColor(HSSFColor.GREEN.index).getIndex());
				break;
			case D:
			default:
				f.setColor(HSSFColor.BLACK.index);
				break;
		}
		style.setFont(f);
		if(isPercent)
			style.setDataFormat(workbook.createDataFormat().getFormat("0%"));

		if(isHeader) {
			style.setFillForegroundColor(palette.getColor(HSSFColor.BLUE.index).getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		}

		if(centerText) {
			style.setAlignment(CellStyle.ALIGN_CENTER);
		}
		return style;
	}

	/**
	 * Build the Total Row.
	 * @param sheet
	 * @param workbook 
	 * @param rowCnt
	 */
	private void buildTotalRow(HSSFSheet sheet, HSSFWorkbook workbook, int rowCnt) {
		Row r = sheet.createRow(rowCnt);
		Cell c = r.createCell(0);
		c.setCellValue("Total Result");
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(1);
		c.setCellFormula(String.format("SUM(B8:B%d)", rowCnt - 1));
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(2);
		c.setCellFormula(String.format("SUM(C8:C%d)", rowCnt - 1));
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(3);
		c.setCellFormula(String.format("SUM(D8:D%d)", rowCnt - 1));
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(4);
		c.setCellFormula(String.format("SUM(E8:E%d)", rowCnt - 1));
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(5);
		c.setCellFormula(String.format("SUM(F8:F%d)", rowCnt - 1));
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(6);
		c.setCellFormula(String.format("(D%d + C%d) / F%d", rowCnt, rowCnt, rowCnt));
		c.setCellStyle(styles.get(StyleType.PERCENT_BORDER_TOP));
		c = r.createCell(7);
		c.setCellFormula(String.format("E%d / F%d", rowCnt, rowCnt));
		c.setCellStyle(styles.get(StyleType.PERCENT_BORDER_TOP));
		c = r.createCell(8);
		c.setCellFormula(String.format("B%d / F%d", rowCnt, rowCnt));
		c.setCellStyle(styles.get(StyleType.PERCENT_BORDER_TOP));
	}


	/**
	 * Build a Data Row
	 * @param sheet
	 * @param workbook 
	 * @param vo
	 * @param rowCnt
	 */
	private void buildRow(HSSFSheet sheet, HSSFWorkbook workbook, RedYellowGreenVO vo, int rowCnt) {
		double total = vo.getGreenActivityCnt() + vo.getNoActivityCnt() + vo.getYellowActivityCnt() + vo.getRedActivityCnt();
		Row r = sheet.createRow(rowCnt);

		Cell c = r.createCell(0);
		c.setCellValue(vo.getAcct().getAccountName());

		c = r.createCell(1);
		c.setCellValue(vo.getGreenActivityCnt());
		c.setCellStyle(styles.get(StyleType.GREEN_VAL));

		c = r.createCell(2);
		c.setCellValue(vo.getNoActivityCnt());
		c.setCellStyle(styles.get(StyleType.RED_VAL));

		c = r.createCell(3);
		c.setCellValue(vo.getRedActivityCnt());
		c.setCellStyle(styles.get(StyleType.RED_VAL));

		c = r.createCell(4);
		c.setCellValue(vo.getYellowActivityCnt());
		c.setCellStyle(styles.get(StyleType.YELLOW_VAL));

		c = r.createCell(5);
		c.setCellValue(total);
		c.setCellStyle(styles.get(StyleType.DEFAULT));

		c = r.createCell(6);
		c.setCellValue((vo.getRedActivityCnt() + vo.getNoActivityCnt()) / total);
		c.setCellStyle(styles.get(StyleType.PERCENT));

		c = r.createCell(7);
		c.setCellValue(vo.getYellowActivityCnt() / total);
		c.setCellStyle(styles.get(StyleType.PERCENT));

		c = r.createCell(8);
		c.setCellValue(vo.getGreenActivityCnt() / total);
		c.setCellStyle(styles.get(StyleType.PERCENT));
	}


	/**
	 * Build the header rows
	 * @param sheet
	 * @param workbook
	 * @return
	 */
	private int buildHeader(HSSFSheet sheet, HSSFWorkbook workbook) {
		int rowCnt = 0;
		Row r = sheet.createRow(rowCnt++);
		Cell c = r.createCell(0);
		c.setCellValue("Account Status = Active");

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Account Type = Full Access & Pilot");
		sheet.addMergedRegion(new CellRangeAddress(r.getRowNum(),r.getRowNum(),0,1));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("License Type = SmartTRAK User and SmartTRAK Extra Seat");
		sheet.addMergedRegion(new CellRangeAddress(r.getRowNum(),r.getRowNum(),0,2));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Report Date : " + Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
		sheet.addMergedRegion(new CellRangeAddress(r.getRowNum(),r.getRowNum(),0,2));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Count of Email Address");
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));
		c = r.createCell(1);
		c.setCellValue("User Status");
		c.setCellStyle(styles.get(StyleType.GREEN_HEADER));
		c = r.createCell(2);
		c.setCellValue("Login Activity Flag");
		c.setCellStyle(styles.get(StyleType.RED_HEADER));
		c = r.createCell(3);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.RED_HEADER));
		c = r.createCell(4);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.RED_HEADER));
		c = r.createCell(5);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("");		
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));
		c = r.createCell(1);
		c.setCellValue("Active");
		c.setCellStyle(styles.get(StyleType.GREEN_HEADER));
		c = r.createCell(2);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.RED_HEADER));
		c = r.createCell(3);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));
		c = r.createCell(4);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));
		c = r.createCell(5);
		c.setCellValue("Active total");
		c.setCellStyle(styles.get(StyleType.WHITE_HEADER));
		c = r.createCell(6);
		c.setCellValue("");

		c = r.createCell(7);
		c.setCellValue("");

		c = r.createCell(8);
		c.setCellValue("");


		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Account Name");
		c.setCellStyle(styles.get(StyleType.WHITE_BORDER_TOP));
		c = r.createCell(1);
		c.setCellValue("Green (< 30 Days)");
		c.setCellStyle(styles.get(StyleType.GREEN_BORDER_TOP));
		c = r.createCell(2);
		c.setCellValue("No Activity");
		c.setCellStyle(styles.get(StyleType.RED_BORDER_TOP));
		c = r.createCell(3);
		c.setCellValue("Red (> 90 Days)");
		c.setCellStyle(styles.get(StyleType.RED_BORDER_TOP));
		c = r.createCell(4);
		c.setCellValue("Yellow (< 90 Days)");
		c.setCellStyle(styles.get(StyleType.YELLOW_BORDER_TOP));
		c = r.createCell(5);
		c.setCellValue("");
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_HEADER));
		c = r.createCell(6);
		c.setCellValue("% Red");
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(7);
		c.setCellValue("% Yellow");
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));
		c = r.createCell(8);
		c.setCellValue("% Green");
		c.setCellStyle(styles.get(StyleType.DEFAULT_BORDER_TOP));

		return rowCnt;
	}

	/**
	 * Add Border Style to given style.
	 * @param style
	 * @param borderTop
	 * @return
	 */
	private CellStyle setBorder(CellStyle style) {
		style.setBorderTop(CellStyle.BORDER_MEDIUM);
		style.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
		return style;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object data) {
		if(data instanceof List) {
			this.data = (List<RedYellowGreenVO>) data;
		}
	}
}