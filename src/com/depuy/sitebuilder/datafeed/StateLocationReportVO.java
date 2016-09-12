
package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.depuy.sitebuilder.datafeed.StateLocationReport.ReportData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: StateLocationReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML State Location report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 18, 2016<p/>
 * @updates:
 ****************************************************************************/
public class StateLocationReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private String startDate=null;
	private String endDate=null;
	private String productCode=null;
	private Map<String, Integer> reportHeader = new TreeMap<>();
	private Map<String, ReportData> dataSource = new TreeMap<>();

	public StateLocationReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("StateLocationReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */

	@SuppressWarnings("unchecked")
	@Override
	public void setRequestData(SMTServletRequest req) {
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));
		productCode = StringUtil.checkVal(req.getParameter("productCode"));
		reportHeader = (Map<String, Integer>) req.getAttribute("reportHeader");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader(), ExcelStyleFactory.Styles.Standard);

		List<Map<String, Object>> rows = new ArrayList<>();

		StringBuilder sb = new StringBuilder(100);

		if (this.endDate == null || this.endDate.isEmpty()) endDate = "Today";

		sb.append("State Location Source Report for ").append(this.productCode);
		
		
		if (!this.startDate.isEmpty()){
			sb.append(" - From ").append(this.startDate);
		}
		
		if (!this.endDate.isEmpty()){
			sb.append(" to ").append(endDate);
		}
		
		rpt.setTitleCell(sb.toString());

		rows = generateDataRows(rows);
		rows = generateTotalRow(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generates the last line of data with totals for each column
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateTotalRow(
			List<Map<String, Object>> rows) {

		Map<String, Object> row = new HashMap<>();
		row.put("STATE", "TOTAL");
		int total = 0;
		for(Entry<String, Integer> entry : reportHeader.entrySet()){
			row.put(entry.getKey(),entry.getValue());
			total += entry.getValue();
		}

		row.put("TOTAL_LEADS_STATE", total);
		rows.add(row);

		return rows;
	}

	/**
	 * generates the data rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		for(Entry<String, ReportData> entry : dataSource.entrySet()){
			int total = 0;
			Map<String, Object> row = new HashMap<>();
			row.put("STATE", entry.getKey());

			Map<String, Integer> stateEntry = entry.getValue().getDataSource();
			
			for(String key : reportHeader.keySet() ){
				if (stateEntry.containsKey(key)) {
					row.put(key, stateEntry.get(key));
					total += stateEntry.get(key);
				}else{
					row.put(key, 0);
				}
			}
			
			row.put("TOTAL_LEADS_STATE", total);
			rows.add(row);

		}

		return rows;
	}

	/**
	 * generates the report  header
	 * @return
	 */
	private Map<String, String> getHeader() {

		HashMap<String, String> headerMap = new LinkedHashMap<>();

		headerMap.put("STATE", "STATE");

		for (String key : reportHeader.keySet()){
			headerMap.put(key, key);
		}

		headerMap.put("TOTAL_LEADS_STATE", "Total Leads/State");

		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> dataSource = (Map<?, ?> ) o;
		this.dataSource = (Map<String, ReportData>) dataSource;

	}

}
