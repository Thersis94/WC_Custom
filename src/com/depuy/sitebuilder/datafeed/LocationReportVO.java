/**
 * 
 */
package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
	Map<String, Integer> reportTotal = new TreeMap<>();
	private List<ReportData> data = new ArrayList<>();
	private final String LOCATION = "LOCATION";
	private final String TOTAL_LEADS_LOC = "TOTAL_LEADS_LOCATION";
	
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
		reportTotal = (Map<String, Integer>) req.getAttribute("reportTotal");
		
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
		rows = generateTotalRow(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generates the total row
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateTotalRow(	List<Map<String, Object>> rows ) {
		int total = 0;
		Map<String, Object> row=new HashMap<>();
		row.put(LOCATION, "Total");
		
		for( Entry<String, Integer> entry : reportTotal.entrySet()){
			row.put(entry.getKey(), entry.getValue());
			total += entry.getValue();
		}
		
		row.put(TOTAL_LEADS_LOC,total);
		
		
		rows.add(row);

		return rows;
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
			row.put(LOCATION, rd.getLocation());
			Map<String, Integer> values = rd.getDataSource();
			for(String v : reportHeader){
				if (values.containsKey(v)){
				row.put(v, values.get(v));
				total += values.get(v);
				}else{
					row.put(v, 0);
				}
			}
			
			row.put(TOTAL_LEADS_LOC,total);
			
			
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
		headerMap.put(LOCATION,"LOCATION");
		for (String s :reportHeader ){
			headerMap.put(s,s);
		}
		headerMap.put(TOTAL_LEADS_LOC,"Total Leads/Location");
		
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		List<?> methodData = (List<?> ) o;
		this.data = (List<ReportData>) methodData;
		
	}

}
