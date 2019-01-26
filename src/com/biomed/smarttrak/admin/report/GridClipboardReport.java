package com.biomed.smarttrak.admin.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/********************************************************************
 * <b>Title: </b>GridClipboardReport.java<br/>
 * <b>Description: </b>Convert the supplied string into an excel file.<br/>
 * <b>Copyright: </b>Copyright (c) 2018<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author Eric Damschroder
 * @since Nov 9, 2018
 * Last Updated:
 * 	
 *******************************************************************/

public class GridClipboardReport extends AbstractSBReportVO {
	
	private static final long serialVersionUID = -3974294098434905114L;
	String tableData;

	@Override
	public byte[] generateReport() {

		String[] rowData = tableData.split("`", -1);
		
		Map<String, String> header = getHeaderRow(rowData);
		List<Map<String, Object>> rows = getDataRows(rowData);
		
		ExcelReport rpt = new ExcelReport(header);
		rpt.setData(rows);
		
		return rpt.generateReport();
	}
	
	
	/**
	 * Parse out column headers from the supplied data
	 * @param rowData
	 * @return
	 */
	private Map<String, String> getHeaderRow(String[] rowData) {
		Map<String, String> header = new LinkedHashMap<>();
		String[] headerData = rowData[0].split("\\|", -1);
		for (int i = 0; i < headerData.length; i++) {
			header.put("col_" + i, headerData[i].trim());
		}
		
		return header;
	}
	
	
	/**
	 * Parse out the data rows from the supplied data
	 * @param rowData
	 * @return
	 */
	private List<Map<String, Object>> getDataRows (String[] rowData) {
		List<Map<String, Object>> rows = new ArrayList<>();
		
		for (int i = 1; i < rowData.length; i++) {
			Map<String, Object> row = new LinkedHashMap<>();
			String[] singleRowData = rowData[i].split("\\|", -1);
			for (int j = 0; j < singleRowData.length; j++) {
				row.put("col_" + j, singleRowData[j].trim());
			}
			rows.add(row);
		}
		
		return rows;
	}

	@Override
	public void setData(Object o) {
		tableData = StringUtil.checkVal(o);
	}

}
