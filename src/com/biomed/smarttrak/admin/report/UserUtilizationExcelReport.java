package com.biomed.smarttrak.admin.report;

//Java 8
import java.util.Date;
import java.util.Map;

//Apache POI 3.13
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory.Styles;

/****************************************************************************
 * Title: UserUtilizationExcelReport.java <p/>
 * Project: WC_Custom <p/>
 * Description: <p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Apr 4, 2018
 ****************************************************************************/

public class UserUtilizationExcelReport extends ExcelReport {
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Constructor
	 * @param headerMap
	 */
	public UserUtilizationExcelReport(Map<String, String> headerMap) {
		super(headerMap);
	}
	
	/**
	 * @param headerMap
	 * @param s
	 */
	public UserUtilizationExcelReport(Map<String, String> headerMap, Styles s) {
		super(headerMap, s);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.ExcelReport#setBodyCellStyle(org.apache.poi.ss.usermodel.Cell, java.util.Map, java.lang.String)
	 */
	@Override
	protected void setBodyCellStyle(Cell c, Map<String, Object> rowData, String code) {
		if(UserUtilizationMonthlyRollupReportVO.LAST_LOGIN_DT.equals(code) && 
				rowData.get(UserUtilizationMonthlyRollupReportVO.LAST_LOGIN_DT) instanceof Date) {
			
			//set the background-color highlighting based on loginAge(similar to User list page legend)
			CellStyle lastLoginStyle = wb.createCellStyle();
			lastLoginStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			int loginAge = (int) rowData.get(UserUtilizationMonthlyRollupReportVO.LAST_LOGIN_AGE);
			
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
