package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.depuy.sitebuilder.datafeed.RegistrationDataReport.RegistrationDataVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ResistrationDataReport.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Registration Data Report report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 22, 2016<p/>
 * @updates:
 ****************************************************************************/
public class RegistrationDataReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private String startDate=null;
	private String endDate=null;
	Map<String, Object> retVal = new HashMap<>();

	public RegistrationDataReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("RegistrationDataReport.xls");
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

		sb.append("Registration Data Report From ").append(this.startDate).append(" to ").append(endDate);

		rpt.setTitleCell(sb.toString());
		rows = generateKeyRow(rows);
		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generates the addtional row that shows the data key
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> generateKeyRow(
			List<Map<String, Object>> rows) {
		Map<String, String> questions = new LinkedHashMap<>();
		Map<String, Object> row = new HashMap<>();

		if (this.retVal.containsKey("questions")){
			questions = (LinkedHashMap<String, String>)this.retVal.get("questions");
		}

		for (Entry<String, String> entry : questions.entrySet() ){
			row.put(entry.getKey(), entry.getKey());
		}

		rows.add(row);
		return rows;
	}

	/**
	 * generates the header rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		Map<String, String> questions = new LinkedHashMap<>();

		if (this.retVal.containsKey("questions")){
			questions = (LinkedHashMap<String, String>)this.retVal.get("questions");
		}

		for (Entry<String, String> entry : questions.entrySet() ){
			headerMap.put(entry.getKey(), entry.getValue());
		}

		return headerMap;
	}

	/**
	 * generates the data row
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		List<RegistrationDataVO> data = new ArrayList<>();
		Map<String, String> questions = new LinkedHashMap<>();

		if (this.retVal.containsKey("data")){
			data = (ArrayList<RegistrationDataVO>)this.retVal.get("data");
		}

		if (this.retVal.containsKey("questions")){
			questions = (LinkedHashMap<String, String>)this.retVal.get("questions");
		}

		for (RegistrationDataVO vo: data){
			Map<String, Object> row = new HashMap<>();

			row.put("callSource",vo.getCallSourceCode());
			row.put("channel",vo.getChannelCode());
			row.put("scriptType",vo.getScriptTypeCode());
			row.put("leadType",vo.getLeadType());
			row.put("attemptDate",vo.getAttemptDate());
			row.put("prefix",vo.getPrefixName());
			row.put("gender",vo.getGenderCode());
			row.put("birthYear",vo.getBirthYear());
			row.put("cityName",vo.getCityName());
			row.put("stateCode",vo.getStateCode());
			row.put("priProduct",vo.getProductCode());
			row.put("secProduct",vo.getSecProductCode());
			row.put("callTarget",vo.getCallTargetCode());
			row.put("callReason",vo.getCallReasonCode());
			row.put("referringPath", vo.getReferringPath());
			row.put("referringSite", vo.getReferringSite());

			//use already existing means to get question response
			for (String key : questions.keySet()){
				if (!row.containsKey(key)){
				vo.setQuestionCode(key);
				row.put(key, vo.getQuestionResponse());
				}
			}
			
			rows.add(row);
		}

		return rows;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<?,?> retVal = (Map<?, ?> ) o;
		this.retVal = (Map<String, Object>) retVal;

	}

}
