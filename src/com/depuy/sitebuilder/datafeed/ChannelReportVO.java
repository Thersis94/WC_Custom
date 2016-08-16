package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ChannelReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Put Something Here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 16, 2016<p/>
 * @updates:
 ****************************************************************************/
public class ChannelReportVO extends AbstractDataFeedReportVo  {
	
	private static final long serialVersionUID = 1L;
	private Map<String, Object> data = new LinkedHashMap<>();
	private String startDate=null;
	private String endDate=null;
	
	
	public ChannelReportVO() {
	    super();
	    setContentType("application/vnd.ms-excel");
	    isHeaderAttachment(Boolean.TRUE);
	    setFileName("RSVP-Summary.xls");
		}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		
		ExcelReport rpt = new ExcelReport(this.getHeader());
		
		List<Map<String, Object>> rows = new ArrayList<>();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Channel Report From ").append(this.startDate).append(" to ").append(endDate);
		
		rpt.setTitleCell(sb.toString());
		
	    rows = generateDataRows(rows);
	    
	    rows = generateTotalRow(rows);
		
		rpt.setData(rows);
		
		return rpt.generateReport();
	}

	/**
	 * adds the final total row
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateTotalRow(
			List<Map<String, Object>> rows) {
		int count = 0;	
		
		for (Map<String, Object> map : rows){
			for(String key :map.keySet()){
				//log.debug("adding: " + map.get(key).toString() + " for key: " + key);
				if("TOTAL".equals(key)){
				count +=  Convert.formatInteger(map.get(key).toString());
				}
			}
		}
		
		Map<String, Object> row = new HashMap<String, Object>();
		
		row.put("CHANNEL_CODE", "Total: ");
		row.put("TOTAL", count);
		rows.add(row);
		
		return rows;
	}

	/**
	 * Generates the rows of data
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		
		for (String key: data.keySet()) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("CHANNEL_CODE", key);
			row.put("TOTAL", data.get(key));
			rows.add(row);
		}
		
		return rows;
	}

	/**
	 * generates the header row
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("CHANNEL_CODE","Channel Code");
		headerMap.put("TOTAL","Total");
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> data = (Map<?, ?> ) o;
		this.data = (Map<String, Object>) data;
		
	}
	
	/*
	 * pulls any needed data off the request and stores it in the report
	 */
	public void setRequestData(SMTServletRequest req){
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));
		//log.debug("start date: " + startDate + " end date: " + endDate);
		
	}

}
