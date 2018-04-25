package com.biomed.smarttrak.admin.report;

//Java 8
import java.util.Map;

//Apache POI 3.13
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory.Styles;

/****************************************************************************
 * Title: SmarttrakExcelReport.java <p/>
 * Project: WC_Custom <p/>
 * Description: Custom monthly utilization report that handles individual excel cell styling<p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Apr 4, 2018
 ****************************************************************************/

public class SmarttrakExcelReport extends ExcelReport {
	private static final long serialVersionUID = 1L;
	//constants to related to last login date
	public static final String LAST_LOGIN_DT = "LAST_LOGIN_DT";
	public static final String LAST_LOGIN_AGE = "LAST_LOGIN_AGE";
	public static final String NO_ACTIVITY = "No Activity";
	
	/**
	 * Constructor
	 * @param headerMap
	 */
	public SmarttrakExcelReport(Map<String, String> headerMap) {
		super(headerMap);
	}
	
	/**
	 * @param headerMap
	 * @param s
	 */
	public SmarttrakExcelReport(Map<String, String> headerMap, Styles s) {
		super(headerMap, s);
	}
	
	
	/**
	 * Overridden here to prevent expanding the title style out to lenght of report
	 */
	@Override
	protected void addTitleCell(Sheet s,String title, int headerMapSize) {
		Row r = s.createRow(s.getPhysicalNumberOfRows());
		
		//fill it with the title string.
		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);

		c.setCellValue(title);
		c.setCellStyle(this.titleStyle);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.ExcelReport#setBodyCellStyle(org.apache.poi.ss.usermodel.Cell, java.util.Map, java.lang.String)
	 */
	@Override
	protected void setBodyCellStyle(Cell c, Map<String, Object> rowData, String columnNm) {
		String loginDt = (String)rowData.get(LAST_LOGIN_DT);
		if(LAST_LOGIN_DT.equals(columnNm) && !NO_ACTIVITY.equals(loginDt) && rowData.get(LAST_LOGIN_AGE) != null) {
			
			//set the background-color highlighting based on loginAge(similar to User list page legend)
			CellStyle lastLoginStyle = wb.createCellStyle();
			lastLoginStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			int loginAge = (int) rowData.get(LAST_LOGIN_AGE);
			
			if (loginAge <= 30) {
				lastLoginStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
			} else if (loginAge <= 60) {
				lastLoginStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
			} else if (loginAge >= 90)  {
				lastLoginStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
			}
			c.setCellStyle(lastLoginStyle);

		}else {//default to whatever the normal styling is
			c.setCellStyle(bodyStyle);
		}
	}

}
