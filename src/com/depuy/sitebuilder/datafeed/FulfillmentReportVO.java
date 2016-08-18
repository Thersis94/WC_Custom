package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.depuy.sitebuilder.datafeed.FulfillmentReport.SummaryData;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
public class FulfillmentReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private Map<String, Date> unconfirmedList = new LinkedHashMap<>();
	private Map<String, SummaryData> data =  new TreeMap<>();
	private int notStarted = 0;
	private Boolean showCodes = null;

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
		notStarted = Convert.formatInteger(StringUtil.checkVal(req.getAttribute("notStarted")));
		showCodes = Convert.formatBoolean((Boolean)req.getAttribute("showCodes"));

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
	 * controls the flow of rows as they are created
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
	 * generates rows related to awaiting fulfillment.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateAwaitingFFRows(
			List<Map<String, Object>> rows) {

		if(!unconfirmedList.isEmpty()){
			//adds a mid report title row
			Map<String, Object> row = new HashMap<>();
			row.put("COL_0", "Awaiting Fulfillment");
			rows.add(row);

			//mid report header row
			row=new HashMap<>();

			row.put("COL_0", "Fulfillment ID");
			row.put("COL_1", "Date Sent for Fulfillment");

			rows.add(row);

			for (Entry<String, Date> entry : unconfirmedList.entrySet()){
				row=new HashMap<>();
				row.put("COL_0", entry.getKey());
				row.put("COL_1", entry.getValue());
				rows.add(row);
			}
		}
		return rows;
	}

	/**
	 * adds the two blank rows between sections of the report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> addBlankrows(
			List<Map<String, Object>> rows) {

		Map<String, Object> row = new HashMap<>();
		row.put("COL_0", "");
		rows.add(row);

		row=new HashMap<>();
		row.put("COL_0", "");
		rows.add(row);

		return rows;
	}

	/**
	 * generates unconfirmed data rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateConfirmedFFRows(
			List<Map<String, Object>> rows) {
		if(!data.isEmpty()){

			//adds a mid report title row
			Map<String, Object> row = new HashMap<>();
			row.put("COL_0", "Confirmed Fulfillments");
			rows.add(row);

			//mid report header row
			rows = addConfirmedFFHeader(rows);

			rows = addDataConfirmedFF(rows);

			rows= addBlankrows(rows);

		}
		return rows;
	}

	/**
	 * adds the data rows to the confirmed fulfillment section
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> addDataConfirmedFF(
			List<Map<String, Object>> rows) {
		//add the data from the collection
		for (Entry<String, SummaryData> entry : data.entrySet()){
			Map<String, Object> row=new HashMap<>();
			if(showCodes){
				row.put("COL_0", entry.getValue().getCallSourceCode());
				row.put("COL_1", entry.getValue().getProcessDate());
				row.put("COL_2", entry.getValue().getSuccess());
				row.put("COL_3", entry.getValue().getUnconfirmed());
			}else{
				row.put("COL_0", entry.getValue().getProcessDate());
				row.put("COL_1", entry.getValue().getSuccess());
				row.put("COL_2", entry.getValue().getUnconfirmed());
			}
			rows.add(row);
		}
		return rows;
	}

	/**
	 * adds the header to the confirmed fulfillment section of the report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> addConfirmedFFHeader(
			List<Map<String, Object>> rows) {
		Map<String, Object> row = new HashMap<>();

		if(showCodes){
			row.put("COL_0", "Call Source Code");
			row.put("COL_1", "Transaction Date");
			row.put("COL_2", "Completed Successfully");
			row.put("COL_3", "Unconfirmed");
		}else{	
			row.put("COL_0", "Transaction Date");
			row.put("COL_1", "Completed Successfully");
			row.put("COL_2", "Unconfirmed");
		}
		rows.add(row);
		return rows;
	}

	/**
	 * generates rows related to awaiting processing
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateAwaitingProcessingRows(
			List<Map<String, Object>> rows) {

		Map<String, Object> row = new HashMap<>();
		row.put("COL_0", "Awaiting Processing");
		rows.add(row);

		row=new HashMap<>();
		row.put("COL_0", notStarted);
		rows.add(row);

		rows=addBlankrows(rows);
		return rows;
	}

	/**
	 * makes the header row, in this report the header row is blank the keys 
	 * will be used to assign values to columns
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put("COL_0","");
		headerMap.put("COL_1","");
		headerMap.put("COL_2","");
		headerMap.put("COL_3","");
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
