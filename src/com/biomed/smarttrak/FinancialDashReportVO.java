package com.biomed.smarttrak;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import com.smt.sitebuilder.action.AbstractSBReportVO;

import com.siliconmtn.data.report.ExcelReport;

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
    private FinancialDashVO dash;

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
    public void setData(Object data) {
    	dash = (FinancialDashVO) data;
    }
    
	public byte[] generateReport() {
		log.debug("Starting FinancialDashReport generateReport()");
		
		ExcelReport rpt = new ExcelReport(this.getHeader());
		rpt.setTitleCell("SmartTRAK - Financial Dashboard");
		
		List<Map<String, Object>> rows = new ArrayList<>();
		rows = generateDataRows(rows);
		rpt.setData(rows);
		
		return rpt.generateReport();
	}
	
	/**
	 * Builds the header map for the Excel report.
	 * 
	 * @return
	 */
	private HashMap<String, String> getHeader() {
		HashMap<String, String> headers = new LinkedHashMap<>();
		headers.put("NAME", dash.getTableTypeName());
		headers.putAll(dash.getColHeaders().getColumns());
		
		return headers;
	}

	/**
	 * Generates the data rows of the Excel report.
	 * 
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(List<Map<String, Object>> rows) {
		// All values are in US dollars
		Locale usLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
		NumberFormat curFormat = NumberFormat.getCurrencyInstance(usLocale);
		curFormat.setMaximumFractionDigits(0);

		NumberFormat pctFormat = NumberFormat.getPercentInstance();
		pctFormat.setMinimumFractionDigits(1);
		
		// Setup to increment totals for the totals row
		Map<String, Integer> totals = initTotals(dash.getRows().get(0));
		
		for (FinancialDashDataRowVO row : dash.getRows()) {
			rows.addAll(getExcelRowsFromFdRow(row, totals, curFormat, pctFormat));
		}
		
		// Generate the totals row
		Map<String, Object> totalRow = new HashMap<>();
		totalRow.put("NAME", "Total");
		for (Entry<String, Integer> entry : totals.entrySet()) {
			totalRow.put(entry.getKey(), curFormat.format(entry.getValue()));
		}
		rows.add(totalRow);

		return rows;
	}
	
	/**
	 * Initializes a set of totals from data in an existing row.
	 * 
	 * @param row
	 * @return
	 */
	private Map<String, Integer> initTotals(FinancialDashDataRowVO row) {
		Map<String, Integer> totals = new HashMap<>();
		
		for (Entry<String, FinancialDashDataColumnVO> entry : row.getColumns().entrySet()) {
			totals.put(entry.getKey(), 0);
		}
		
		return totals;
	}
	
	/**
	 * Breaks the single financial dash rows into multiple excel rows (one for dollar, other for percent)
	 * 
	 * @param row
	 * @param totals
	 * @param curFormat
	 * @param pctFormat
	 * @return
	 */
	private List<Map<String, Object>> getExcelRowsFromFdRow(FinancialDashDataRowVO row, Map<String, Integer> totals, NumberFormat curFormat, NumberFormat pctFormat) {
		List<Map<String, Object>> excelRows = new ArrayList<>();
		
		Map<String, Object> dollarRow = new HashMap<>();
		Map<String, Object> percentRow = new HashMap<>();
		
		dollarRow.put("NAME", row.getName());
		percentRow.put("NAME", "");

		for (Entry<String, FinancialDashDataColumnVO> entry : row.getColumns().entrySet()) {
			dollarRow.put(entry.getKey(), curFormat.format(entry.getValue().getDollarValue()));
			totals.put(entry.getKey(), totals.get(entry.getKey()) + entry.getValue().getDollarValue());
			
			if (entry.getValue().getPctDiff() == null) {
				percentRow.put(entry.getKey(), "");
			} else {
				percentRow.put(entry.getKey(), pctFormat.format(entry.getValue().getPctDiff()));
			}
		}
	
		excelRows.add(dollarRow);
		excelRows.add(percentRow);
		
		return excelRows;
	}
}
