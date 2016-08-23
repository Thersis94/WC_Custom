package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.depuy.sitebuilder.datafeed.DailySourceReport.ReportData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DailySourceReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non html daily source report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 16, 2016<p/>
 * @updates:
 ****************************************************************************/
public class DailySourceReportVO extends AbstractDataFeedReportVO {
	private Map<Date, ReportData> dataSource = new TreeMap<>();
	private String startDate=null;
	private String endDate=null;
	private String joint=null;
	private Map<String, Integer> headers = new TreeMap<>();

	private static final long serialVersionUID = 1L;

	public DailySourceReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("DailySourceReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVo#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	
	@SuppressWarnings("unchecked")
	@Override
	public void setRequestData(SMTServletRequest req) {
		this.headers = (Map<String, Integer>) req.getAttribute("reportHeader");
		this.joint = StringUtil.checkVal(req.getParameter("productCode"));
		this.startDate = StringUtil.checkVal(req.getParameter("startDate"));
		this.endDate = StringUtil.checkVal(req.getParameter("endDate"));
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		Map<String, String> headerMap = this.getHeader();

		ExcelReport rpt = new ExcelReport(headerMap);

		List<Map<String, Object>> rows = new ArrayList<>();

		StringBuilder sb = new StringBuilder(100);

		sb.append("Daily Source Report for ").append(joint).append(" - From ").append(this.startDate).append(" to ").append(endDate);

		rpt.setTitleCell(sb.toString());

		rows = generateDataRows(rows,headerMap);

		rows = generateLastRow(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateLastRow(
			List<Map<String, Object>> rows) {
		Map<String, Object> row = new HashMap<>();

		row.put("DATE","TOTAL");
		int total = 0;
		for( Entry<String, Integer> entry : headers.entrySet()){
			
			row.put(entry.getKey(), entry.getValue());
			total += entry.getValue();
		}

		row.put("TOTAL_LEADS_STATE",total);
		rows.add(row);
		return rows;
	}

	/**
	 * generates the rows of data
	 * @param rows
	 * @param headerMap 
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows, Map<String, String> headerMap) {
		int total = 0;
		for (Entry<Date, ReportData> entry : dataSource.entrySet()){
			Map<String, Object> row = new HashMap<>();
			ReportData dateData = entry.getValue();
			row.put("DATE",Convert.formatDate(entry.getKey(), "MM/dd/yy"));
			for(String key : headerMap.keySet()){

				if (dateData.getDataSource().containsKey(key) ){
					row.put(key,dateData.getDataSource().get(key));
					total += dateData.getDataSource().get(key);
				}else{
					row = zeroCell(row, key);
				}

			}

			row.put("TOTAL_LEADS_STATE", total);
			total = 0;
			rows.add(row);
		}
		return rows;
	}

	/**
	 * if not the date sets the value to zero
	 * @param row
	 * @param key 
	 * @return
	 */
	private Map<String, Object> zeroCell(Map<String, Object> row, String key) {
		if (!"DATE".equals(key))
			row.put(key,0);
		return row;
	}

	/**
	 * generates the map of headers
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();

		headerMap.put("DATE", "DATE");

		for (String key : headers.keySet()) {
			headerMap.put(key.toUpperCase(), key.toUpperCase());
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
		this.dataSource = (Map<Date, ReportData>) dataSource;
	}

}
