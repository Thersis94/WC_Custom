package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.depuy.sitebuilder.datafeed.FulfillmentReport.SummaryData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: FulfillmentReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Fulfillment report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 17, 2016<p/>
 * @updates:
 ****************************************************************************/
public class FulfillmentReportVO extends AbstractDataFeedReportVo {

	private static final long serialVersionUID = 1L;
	Map<String, Date> unconfirmedList = new LinkedHashMap<>();
	Map<String, SummaryData> data =  new TreeMap<>();

	public FulfillmentReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("FulfillmentReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVo#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setRequestData(SMTServletRequest req) {
		unconfirmedList  =  (Map<String, Date>) req.getAttribute("unconfirmedList");

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
	 * controls the flow of which rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		rows = generateAwaitingProcessingRows(rows);
		rows = generateConfirmedFFRows(rows);
		rows = generateAwaitingFFRows(rows);		
		return rows;
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateAwaitingFFRows(
			List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub
		return rows;
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateConfirmedFFRows(
			List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub
		return rows;
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateAwaitingProcessingRows(
			List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub
		return rows;
	}

	/**
	 * makes the header row, in this report the header row is blank the keys 
	 * will be used to assign values to columns
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put("CALL_SOURCE_CODE","");
		headerMap.put("TRANSACTION_DATE","");
		headerMap.put("COMPLETED","");
		headerMap.put("UNCOMFIRMED","");
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> dataSource = (Map<?, ?> ) o;
		this.data = (Map<String, SummaryData>) dataSource;

	}

}
