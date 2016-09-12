package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.sitebuilder.datafeed.TransactionReport.ReportData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: TransactionReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Transaction report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 22, 2016<p/>
 * @updates:
 ****************************************************************************/
public class TransactionReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private List<ReportData> data = new ArrayList<>();
	
	public TransactionReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("TransactionReport.xls");
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(SMTServletRequest req) {
		//this report needs not additional data from the request object
		//intentionally left blank
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader(), ExcelStyleFactory.Styles.Standard);

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
		
			for (ReportData rd : data){
				Map<String, Object> row = new HashMap<>();
				
				row.put("TRANSACTION_DATE",rd.getTransactionDate());
				row.put("TRANSACTION_NAME",rd.getSourceName());
				row.put("ENTRIES_SENT",rd.getNumberSent());
				row.put("ENTRIES_PROCESSED",rd.getNumberStored());	
				
				rows.add(row);
			}
		
		return rows;
	}

	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		
		headerMap.put("TRANSACTION_DATE","Transaction Date");
		headerMap.put("TRANSACTION_NAME","Transaction Name");
		headerMap.put("ENTRIES_SENT","Entries Sent");
		headerMap.put("ENTRIES_PROCESSED","Entries Processed");	
		
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
