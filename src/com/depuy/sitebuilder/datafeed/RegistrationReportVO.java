package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.depuy.sitebuilder.datafeed.RegistrationReport.RegistrationVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RegistrationReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Registration Report report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 26, 2016<p/>
 * @updates:
 ****************************************************************************/
public class RegistrationReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = -7447814836563970560L;
	Map<String, Object> retVal = new HashMap<>();
	private String startDate=null;
	private String endDate=null;
	private enum column {
		COL_0("Question Text"), COL_1("Question Code"), COL_2("Response Value"), 
		COL_3("# Responses"), COL_4("Channel Code");

		private String header;

		private column(String header) {
			this.header = header;
		}

	};


	public RegistrationReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("RegistrationReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(SMTServletRequest req) {
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader());

		List<Map<String, Object>> rows = new ArrayList<>();

		StringBuilder sb = new StringBuilder(100);

		if (this.endDate == null || this.endDate.isEmpty()) endDate = "Today";

		sb.append("Registration Report");

		
		if (!this.startDate.isEmpty()){
			sb.append(" From ").append(this.startDate);
		}
		
		if (!this.endDate.isEmpty()){
			sb.append(" to ").append(endDate);
		}
		
		rpt.setTitleCell(sb.toString());

		rows = generateQLRows(rows);
		rows = generateSummaryRows(rows);
		rows = generateRawDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * controls the summary rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateSummaryRows(
			List<Map<String, Object>> rows) {

		rows = summaryHeaders(rows);
		rows = summaryData(rows);

		return rows;
	}

	/**
	 * controls the data rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateRawDataRows(
			List<Map<String, Object>> rows) {

		rows = rawHeaders(rows);
		rows = rawData(rows);

		return rows;
	}

	/**
	 * generates the rows of raw data
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> rawData(List<Map<String, Object>> rows) {

		@SuppressWarnings("unchecked")
		List<RegistrationVO> data = (List<RegistrationVO>) retVal.get("data");

		for ( RegistrationVO vo : data ) {

			Map<String, Object> row = new HashMap<>();
			row.put(column.COL_0.name(), vo.getQuestionName());
			row.put(column.COL_1.name(), vo.getQuestionCode());
			row.put(column.COL_2.name(), vo.getResponseText());
			row.put(column.COL_3.name(), vo.getResponseCount());
			row.put(column.COL_4.name(), vo.getChannelCode());

			rows.add(row);
		}

		return rows;
	}

	/**
	 * generates the header rows for the raw data section
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> rawHeaders(List<Map<String, Object>> rows) {

		Map<String, Object> row1 = new HashMap<>();
		row1.put(column.COL_0.name(), "Raw Data ");
		rows.add(row1);

		Map<String, Object> row2 = new HashMap<>();
		row2.put(column.COL_0.name(), column.COL_0.header);
		row2.put(column.COL_1.name(), column.COL_1.header);
		row2.put(column.COL_2.name(), column.COL_2.header);
		row2.put(column.COL_3.name(), column.COL_3.header);
		row2.put(column.COL_4.name(), column.COL_4.header);

		rows.add(row2);

		return rows;
	}

	/**
	 * generates the qualified leads and leads rows at the top of the report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateQLRows(List<Map<String, Object>> rows) {

		Map<String, Object> row1 = new HashMap<>();
		row1.put(column.COL_0.name(), "Qualified Leads: ");
		row1.put(column.COL_1.name(), StringUtil.checkVal(retVal.get("qualLeads")));

		rows.add(row1);
		Map<String, Object> row2 = new HashMap<>();
		row2.put(column.COL_0.name(), "Leads: ");
		row2.put(column.COL_1.name(), StringUtil.checkVal(retVal.get("leads")));

		rows.add(row2);

		Map<String, Object> spacerRow = new HashMap<>();
		spacerRow.put(column.COL_0.name(), " ");
		rows.add(spacerRow);

		return rows;
	}

	/**
	 * generates the summary data rows
	 * generates 
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> summaryData(List<Map<String, Object>> rows) {

		@SuppressWarnings("unchecked")
		Map<String, Integer> stats = (Map<String, Integer>) retVal.get("stats");

		for ( Entry<String, Integer> entry : stats.entrySet() ) {

			List<String> tags = Arrays.asList(entry.getKey().split("[|]"));

			Map<String, Object> row = new HashMap<>();
			row.put(column.COL_0.name(), tags.get(0));
			row.put(column.COL_1.name(), tags.get(1));
			row.put(column.COL_2.name(), entry.getValue());
			row.put(column.COL_3.name(), tags.get(2));
			row.put(column.COL_4.name(), "");

			rows.add(row);
		}

		Map<String, Object> spacerRow = new HashMap<>();
		spacerRow.put(column.COL_0.name(), " ");
		rows.add(spacerRow);

		return rows;
	}

	/**
	 * generates the summary headers
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> summaryHeaders(List<Map<String, Object>> rows) {

		Map<String, Object> row1 = new HashMap<>();
		row1.put(column.COL_0.name(), "Summary ");
		rows.add(row1);

		Map<String, Object> row2 = new HashMap<>();
		row2.put(column.COL_0.name(), column.COL_0.header);
		row2.put(column.COL_1.name(), column.COL_1.header);
		row2.put(column.COL_2.name(), column.COL_3.header);
		row2.put(column.COL_3.name(), column.COL_4.header);
		row2.put(column.COL_4.name(), "");

		rows.add(row2);

		return rows;
	}

	/**
	 * Generates and empty header row
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(column.COL_0.name(),"");
		headerMap.put(column.COL_1.name(),"");
		headerMap.put(column.COL_2.name(),"");
		headerMap.put(column.COL_3.name(),"");
		headerMap.put(column.COL_4.name(),"");
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> methodRetVal = (Map<?, ?> ) o;
		this.retVal = (Map<String, Object>) methodRetVal;

	}

}
