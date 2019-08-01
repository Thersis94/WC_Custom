package com.biomed.smarttrak.admin.report;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

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

	private enum FormatColor {G,Y,R,B,D,W};
	public RedYellowGreenReportVO() {
		super();
		super.setFileName("RedGreenReport.xls");
	}
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private List<RedYellowGreenVO> data;
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
		c = r.createCell(1);
		c.setCellFormula(String.format("SUM(B8:B%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.G, false, false, true));
		c = r.createCell(2);
		c.setCellFormula(String.format("SUM(C8:C%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, false, true));
		c = r.createCell(3);
		c.setCellFormula(String.format("SUM(D8:D%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, false, true));
		c = r.createCell(4);
		c.setCellFormula(String.format("SUM(E8:E%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.Y, false, false, true));
		c = r.createCell(5);
		c.setCellFormula(String.format("SUM(F8:F%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, false, false));
		c = r.createCell(6);
		c.setCellFormula(String.format("(D%d + C%d) / F%d", rowCnt, rowCnt, rowCnt));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(7);
		c.setCellFormula(String.format("E%d / F%d", rowCnt, rowCnt));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(8);
		c.setCellFormula(String.format("B%d / F%d", rowCnt, rowCnt));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(9);
		c.setCellFormula(String.format("SUM(J8:J%d)", rowCnt - 1));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, false, true));
	}


	/**
	 * Build a Data Row
	 * @param sheet
	 * @param workbook 
	 * @param vo
	 * @param rowCnt
	 */
	private void buildRow(HSSFSheet sheet, HSSFWorkbook workbook, RedYellowGreenVO vo, int rowCnt) {
		int rowIndex = rowCnt + 1;
		Row r = sheet.createRow(rowCnt);
		Cell c = r.createCell(0);
		c.setCellValue(vo.getAcct().getAccountName());
		c = r.createCell(1);
		c.setCellValue(vo.getGreenActivityCnt());
		c.setCellStyle(getCellFormat(workbook, FormatColor.G, false, false, true));
		c = r.createCell(2);
		c.setCellValue(vo.getNoActivityCnt());
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, false, true));
		c = r.createCell(3);
		c.setCellValue(vo.getRedActivityCnt());
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, false, true));
		c = r.createCell(4);
		c.setCellValue(vo.getYellowActivityCnt());
		c.setCellStyle(getCellFormat(workbook, FormatColor.Y, false, false, true));
		c = r.createCell(5);
		c.setCellFormula(String.format("SUM(B%d:E%d)", rowIndex, rowIndex));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, false, false));
		c = r.createCell(6);
		c.setCellFormula(String.format("J%d / F%d", rowIndex, rowIndex));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(7);
		c.setCellFormula(String.format("E%d / F%d", rowIndex, rowIndex));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(8);
		c.setCellFormula(String.format("B%d / F%d", rowIndex, rowIndex));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, true, false, true));
		c = r.createCell(9);
		c.setCellFormula(String.format("C%d + D%d", rowIndex, rowIndex));
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, false, true));
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
		c.setCellValue("From Date");
		c = r.createCell(1);
		c.setCellValue("mm/dd/yyyy");
		c = r.createCell(2);
		c.setCellValue("To Date");
		c = r.createCell(3);
		c.setCellValue("mm/dd/yyyy");

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Count of Email Address");
		c.setCellStyle(getCellFormat(workbook, FormatColor.W, false, true, false));
		c = r.createCell(1);
		c.setCellValue("User Status");
		c.setCellStyle(getCellFormat(workbook, FormatColor.G, false, true, true));
		c = r.createCell(2);
		c.setCellValue("Login Activity Flag");
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, true, false));
		c = r.createCell(3);
		c.setCellValue("");
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, true, false));
		c = r.createCell(4);
		c.setCellValue("");
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, true, false));
		c = r.createCell(5);
		c.setCellValue("");
		c.setCellStyle(getCellFormat(workbook, FormatColor.W, false, true, false));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("");		
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, true, false)));
		c = r.createCell(1);
		c.setCellValue("Active");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.G, false, true, true)));
		c = r.createCell(2);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.R, false, true, false)));
		c = r.createCell(3);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.D, false, true, false)));
		c = r.createCell(4);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.D, false, true, false)));
		c = r.createCell(5);
		c.setCellValue("Active total");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, true, false)));
		c = r.createCell(6);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, false, false)));
		c = r.createCell(7);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, false, false)));
		c = r.createCell(8);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, false, false)));
		c = r.createCell(9);
		c.setCellValue("");
		c.setCellStyle(setBorder(getCellFormat(workbook, FormatColor.W, false, false, false)));

		r = sheet.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellValue("Account Name");
		c.setCellStyle(getCellFormat(workbook, FormatColor.W, false, true, false));
		c = r.createCell(1);
		c.setCellValue("Green");
		c.setCellStyle(getCellFormat(workbook, FormatColor.G, false, true, true));
		c = r.createCell(2);
		c.setCellValue("No Activity");
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, true, true));
		c = r.createCell(3);
		c.setCellValue("Red");
		c.setCellStyle(getCellFormat(workbook, FormatColor.R, false, true, true));
		c = r.createCell(4);
		c.setCellValue("Yellow");
		c.setCellStyle(getCellFormat(workbook, FormatColor.Y, false, true, true));
		c = r.createCell(5);
		c.setCellValue("");
		c.setCellStyle(getCellFormat(workbook, FormatColor.D, false, true, false));
		c = r.createCell(6);
		c.setCellValue("% Red");
		c = r.createCell(7);
		c.setCellValue("% Yellow");
		c = r.createCell(8);
		c.setCellValue("% Green");
		c = r.createCell(9);
		c.setCellValue("# Red/No Utiliz");
		return rowCnt;
	}

	/**
	 * Add Border Style to given style.
	 * @param style
	 * @return
	 */
	private CellStyle setBorder(CellStyle style) {
		style.setBorderBottom(CellStyle.BORDER_MEDIUM);
		style.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
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