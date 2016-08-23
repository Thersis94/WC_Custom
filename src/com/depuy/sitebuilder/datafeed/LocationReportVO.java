/**
 * 
 */
package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.sitebuilder.datafeed.LocationReport.ReportData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: LocationReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Location Report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 18, 2016<p/>
 * @updates:
 ****************************************************************************/
public class LocationReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private List<String> reportHeader = new ArrayList<>();
	private List<ReportData> data = new ArrayList<>();
	
	public LocationReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("LocationReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVo#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setRequestData(SMTServletRequest req) {
		reportHeader = (List<String>) req.getAttribute("reportHeader");
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader());

		List<Map<String, Object>> rows = new ArrayList<>();

		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		
		for(ReportData rd : data){
			int total = 0;
			Map<String, Object> row=new HashMap<>();
			row.put("LOCATION", rd.getLocation());
			log.debug("# location " + rd.getLocation());
			Map<String, Integer> values = rd.getDataSource();
			for(String v : reportHeader){
				if (values.containsKey(v)){
				log.debug("### KEY to value " + v + " value: " + values.get(v));
				row.put(v, values.get(v));
				total += values.get(v);
				}else{
					row.put(v, 0);
				}
			}
			
			row.put("TOTAL_LEADS_LOCATION",total);
			
			
			rows.add(row);
		}
		
		return rows;
	}

	/**
	 * generates the header row for this report
	 * @return
	 */
	private Map<String, String> getHeader() {
		
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put("LOCATION","LOCATION");
		for (String s :reportHeader ){
			headerMap.put(s,s);
		}
		headerMap.put("TOTAL_LEADS_LOCATION","Total Leads/Location");
		
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		List<?> data = (List<?> ) o;
		this.data = (List<ReportData>) data;
		
	}

}
