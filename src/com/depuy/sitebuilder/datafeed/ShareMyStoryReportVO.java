package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.StandardExcelReport;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ShareMyStoryReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML share my story report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 22, 2016<p/>
 * @updates:
 ****************************************************************************/
public class ShareMyStoryReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private Map<String, Integer> retVal = new HashMap<>();
	private String startDate=null;
	private String endDate=null;
	
	//holds the key and the readable value on the data sheet.
	private enum myStoryType {
		VALID_EMAIL("Candidates with a valid email: "),
		INVALID_EMAIL("Candidates with an invalid email: "),
		SENT("Emails Sent: "),
		OPEN("Emails Opened: "),
		REDIRECT("Click-throughs to data form (from the email):"),
		FORMS("Data-Form Submissions:");
		private String fieldText;

		private myStoryType(String fieldText) {
			this.fieldText = fieldText;
		}

		public String getFieldText(){
			return fieldText;
		}
	}

	public ShareMyStoryReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("ShareMyStoryReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(ActionRequest req) {
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));


	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new StandardExcelReport(this.getHeader());

		List<Map<String, Object>> rows = new ArrayList<>();

		StringBuilder sb = new StringBuilder(100);

		sb.append("Patient Stories Report");

		
		if (!this.startDate.isEmpty()){
			sb.append(" From ").append(this.startDate);
		}
		
		if (!this.endDate.isEmpty()){
			sb.append(" To ").append(this.endDate);
		}
		
		rpt.setTitleCell(sb.toString());

		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generates the data rows
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		for(myStoryType type : myStoryType.values()){
			Map<String, Object> row = new HashMap<>();
			row.put("COL_0",type.getFieldText());
			row.put("COL_1",retVal.get(type.name()));
			rows.add(row);
		}

		return rows;
	}

	/**
	 * generates the header map, these have intentionally blank values
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();

		headerMap.put("COL_0", "");
		headerMap.put("COL_1", "");

		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> retVal = (Map<?, ?> ) o;
		this.retVal = (Map<String, Integer>) retVal;

	}

}
