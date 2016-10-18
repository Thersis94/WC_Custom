package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.sitebuilder.datafeed.QualifiedByCityReport.ReportData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.StandardExcelReport;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: QualifiedByCityReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML qualified by city report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 18, 2016<p/>
 * @updates:
 ****************************************************************************/
public class QualifiedByCityReportVO  extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private List<ReportData> data = new ArrayList<>();
	
	public QualifiedByCityReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("QualifiedByCityReport.xls");
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVo#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(SMTServletRequest req) {
		// Qualified by city report does not need any additional request items
		//    this method left intentionally empty
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new StandardExcelReport(this.getHeader());

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
			Map<String, Object> row=new HashMap<>();
			
			row.put("FIRST_NAME", rd.getFirstName());
			row.put("LAST_NAME", rd.getLastName());
			row.put("EMAIL_ADDRESS", rd.getEmail());
			row.put("ADDRESS", rd.getAddress());
			row.put("CITY", rd.getCity());
			row.put("STATE", rd.getState());
			row.put("ZIP", rd.getZip());
			
			rows.add(row);
		}
		
		return rows;
	}

	/**
	 * generates the header row
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("EMAIL_ADDRESS", "Email Address");
		headerMap.put("ADDRESS", "Address");
		headerMap.put("CITY", "City");
		headerMap.put("STATE", "State");
		headerMap.put("ZIP", "Zip");
	
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		List<?> dataSource = (List<?> ) o;
		this.data = (List<ReportData>) dataSource;
	}
	

}
