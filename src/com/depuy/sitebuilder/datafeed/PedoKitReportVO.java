package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.sitebuilder.datafeed.PedoKitReport.PedoKitVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.ExcelStyleFactory;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: PedoKitReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Pedo kit report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 22, 2016<p/>
 * @updates:
 ****************************************************************************/
public class PedoKitReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private String startDate=null;
	private String endDate=null;
	private String includePII = null;
	private List<PedoKitVO> data = new ArrayList<>();

	public PedoKitReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("PedoKitReport.xls");
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(SMTServletRequest req) {
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));
		includePII = req.getParameter("includePII");

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader(), ExcelStyleFactory.getExcelStyles(ExcelStyleFactory.DEFAULT));

		List<Map<String, Object>> rows = new ArrayList<>();

		StringBuilder sb = new StringBuilder(100);

		sb.append("Pedometer Kit Report From ");

		if (!this.startDate.isEmpty()){
			sb.append(" From ").append(this.startDate);
		}
		
		if (!this.endDate.isEmpty()){
			sb.append(" To ").append(endDate);
		}
		
		rpt.setTitleCell(sb.toString());

		rows = generateHeaderRows(rows);
		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generates the two rowed header for this report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateHeaderRows(
			List<Map<String, Object>> rows) {
		Map<String, Object> row0 = new HashMap<>();
		row0.put("TOUCHPOINT", "Previous Marketing Campaign");
		rows.add(row0);

		Map<String, Object> row1 = new HashMap<>();


		row1.put("CUSTOMER_ID", "CustomerId");
		row1.put("ATTEMPT_DATE", "Attempt Date");
		row1.put("ATTEMPT_WEEK", "Attempt Week");
		row1.put("RECORD_SOURCE", "Record Source");
		row1.put("KIT_MAIL_DATE", "Kit Mail Date");
		row1.put("KIT_FAIL_DATE", "Kit Fail Date");
		row1.put("KIT_FAIL_REASON", "Kit Fail Reason");

		if(this.includePII != null && !this.includePII.isEmpty()){
			row1.put("TITLE", "Title");
			row1.put("FIRST_NAME", "First Name");
			row1.put("LAST_NAME", "Last Name");
			row1.put("PHONE", "Phone");
			row1.put("EMAIL_IMPLIES_OPT_IN)", "Email (implies opt-in)");
			row1.put("ADDRESS", "Address");
			row1.put("SUITE_APT", "Suite/Apt#");
		}

		row1.put("CITY", "City");
		row1.put("STATE", "State");
		row1.put("ZIP", "Zip");
		row1.put("YEAR_OF_BIRTH", "Year of Birth");
		row1.put("GENDER", "Gender");
		row1.put("AFFECTED_JOINT", "Affected Joint");
		row1.put("DAYS_PER_WEEK_IN_PAIN", "Days per week in pain");
		row1.put("PAIN_SEVERITY", "Pain severity (1-5)");
		row1.put("YEARS_IN_PAIN", "Years in Pain");
		row1.put("MORE_REVIEWED_KIT", "More prepared having reviewed kit?");
		row1.put("SURGERY_DATE", "Approx. Surgery Date");
		row1.put("SURGERY_MONTH", "Surgery Month");
		row1.put("TOUCHPOINT", "Touchpoint");
		row1.put("ATTEMPT_DATE2", "Attempt Date");
		row1.put("DAYS_PER_WEEK_IN_PAIN", "Days per week in pain");
		row1.put("PAIN_SEVERITY_2", "Pain severity (1-5)");
		row1.put("TERRITORY", "Territory");
		row1.put("DISTRIBUTOR_NAME", "Distributor Name");
		row1.put("AD", "AD");
		row1.put("REGION", "Region");
		row1.put("AVP", "AVP");
		row1.put("ID_CARD_DATA_SOURCE", "ID Card Data Source");
		row1.put("BEST_TIME_TO_CALL", "Best Time to Call");
		row1.put("TYPE_OF_SURGERY", "Type of Surgery");
		row1.put("SURGEON", "Surgeon");
		row1.put("HOSPITAL", "Hospital");
		row1.put("COLLECTION_STATEMENT", "Collection Statement");
		row1.put("CALL_1", "Call 1");
		row1.put("CALL_2", "Call 2");
		row1.put("LIFE_LIKE_BEFORE_JOINT_REPLACEMENT", "Life Like Before Joint Replacement?");
		row1.put("ACTIVITIES_STOPPED", "Activities Stopped?");
		row1.put("TURNING_POINT", "Turning Point?");
		row1.put("LIFE_NOW", "Life Now?");
		row1.put("ADVICE_FOR_OTHERS", "Advice for Others?");
		row1.put("PARTICIPATE_EDUCATION_EVENT", "Willing to Participate at an Education Event?");
		row1.put("OTHER_COMMENTS", "Other Comments");
		row1.put("PICTURES", "Pictures");
		row1.put("CONSENT_FORM_SENT", "Consent Form Sent");
		row1.put("CONSENT_FORM_RECEIVED", "Consent Form Received");
		row1.put("BACKPACK_PEDOMETER", "Received Backpack/Pedometer?");

		rows.add(row1);

		return rows;
	}

	/**
	 * genereates the data rows for this report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		for (PedoKitVO vo : data){
			Map<String, Object> row = new HashMap<>();

			row.put("CUSTOMER_ID", vo.getCustomerId());
			row.put("ATTEMPT_DATE", Convert.formatDate(vo.getAttemptDt(), "MM/dd/yy"));
			row.put("ATTEMPT_WEEK", Convert.formatDate(vo.getAttemptDt(), "w"));
			row.put("RECORD_SOURCE", vo.getCallSourceCd());
			row.put("KIT_MAIL_DATE", Convert.formatDate(vo.getKitMailDt(), "MM/dd/yy"));
			row.put("KIT_FAIL_DATE", Convert.formatDate(vo.getKitFailDt(), "MM/dd/yy"));
			row.put("KIT_FAIL_REASON", vo.getKitFailTxt());

			UserDataVO profile = vo.getProfile();
			if(profile != null ){

				if(this.includePII != null && !this.includePII.isEmpty()){
					row.put("TITLE", profile.getPrefixName());
					row.put("FIRST_NAME", profile.getFirstName());
					row.put("LAST_NAME", profile.getLastName());
					row.put("PHONE", profile.getMainPhone());
					row.put("EMAIL_IMPLIES_OPT_IN)", profile.getEmailAddress());
					row.put("ADDRESS", profile.getAddress());
					row.put("SUITE_APT", profile.getAddress2());
				}

				row.put("CITY", profile.getCity());
				row.put("STATE", profile.getState());
				row.put("ZIP", profile.getZipCode());
				row.put("YEAR_OF_BIRTH", profile.getBirthYear());
				row.put("GENDER", profile.getGenderCode());
				row.put("BEST_TIME_TO_CALL", profile.getBestTime());
			}
			row.put("AFFECTED_JOINT", vo.getProductCd());

			String resKey2 = "QUAL_"+vo.getProductCd().toUpperCase()+"_02";
			String resKey1 = "QUAL_"+vo.getProductCd().toUpperCase()+"_01";

			row.put("DAYS_PER_WEEK_IN_PAIN", vo.getResponses().get(resKey2));
			row.put("PAIN_SEVERITY", vo.getResponses().get(resKey1));
			row.put("YEARS_IN_PAIN", vo.getResponses().get("YEARS_IN_PAIN"));
			row.put("MORE_REVIEWED_KIT", vo.getResponses().get("MORE_PREPARED"));
			row.put("SURGERY_DATE", vo.getResponses().get("SURGERY_DATE"));

			Date sd = Convert.formatDate("yyyy-MM-dd", vo.getResponses().get("SURGERY_DATE"));
			row.put("SURGERY_MONTH", Convert.formatDate(sd, "MMMM"));

			row.put("PREVIOUS_MARKETING_CAMPAIGN", "");
			row.put("TOUCHPOINT", vo.getLastCampaign());
			row.put("ATTEMPT_DATE2", Convert.formatDate(vo.getAttemptDt(), "MM/dd/yy"));
			row.put("DAYS_PER_WEEK_IN_PAIN_2", vo.getLastQual02());
			row.put("PAIN_SEVERITY_2", vo.getLastQual01());
			row.put("TERRITORY", vo.getTerritory());
			row.put("DISTRIBUTOR_NAME", vo.getDistributor());
			row.put("AD", vo.getAd());
			row.put("REGION", vo.getRegion());
			row.put("AVP", vo.getAvp());
			row.put("ID_CARD_DATA_SOURCE", vo.getIDCardCallSourceCd());

			row.put("TYPE_OF_SURGERY", vo.getIDCardProductCd());
			row.put("SURGEON", vo.getIDCardSurgeonNm());
			row.put("HOSPITAL", vo.getIDCardHospitalNm());
			row.put("COLLECTION_STATEMENT", (vo.getAllowComm() == 1)? "Yes" : "No" );
			row.put("CALL_1", "");
			row.put("CALL_2", "");
			row.put("LIFE_LIKE_BEFORE_JOINT_REPLACEMENT",  "");
			row.put("ACTIVITIES_STOPPED", "");
			row.put("TURNING_POINT", "");
			row.put("LIFE_NOW", "");
			row.put("ADVICE_FOR_OTHERS", "");
			row.put("PARTICIPATE_EDUCATION_EVENT", "");
			row.put("OTHER_COMMENTS", "");
			row.put("PICTURES", "");
			row.put("CONSENT_FORM_SENT", "");
			row.put("CONSENT_FORM_RECEIVED", "");
			row.put("BACKPACK_PEDOMETER", "");

			rows.add(row);
		}
		return rows;
	}

	/**
	 * Generates the headers of the report, right side is a blank string so a more custom header can be created
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();

		headerMap.put("CUSTOMER_ID", "");
		headerMap.put("ATTEMPT_DATE", "");
		headerMap.put("ATTEMPT_WEEK", "");
		headerMap.put("RECORD_SOURCE", "");
		headerMap.put("KIT_MAIL_DATE", "");
		headerMap.put("KIT_FAIL_DATE", "");
		headerMap.put("KIT_FAIL_REASON", "");

		if(this.includePII != null && !this.includePII.isEmpty()){
			headerMap.put("TITLE", "");
			headerMap.put("FIRST_NAME", "");
			headerMap.put("LAST_NAME", "");
			headerMap.put("PHONE", "");
			headerMap.put("EMAIL_IMPLIES_OPT_IN)", "");
			headerMap.put("ADDRESS", "");
			headerMap.put("SUITE_APT", "");
		}

		headerMap.put("CITY", "");
		headerMap.put("STATE", "");
		headerMap.put("ZIP", "");
		headerMap.put("YEAR_OF_BIRTH", "");
		headerMap.put("GENDER", "");
		headerMap.put("AFFECTED_JOINT", "");
		headerMap.put("DAYS_PER_WEEK_IN_PAIN", "");
		headerMap.put("PAIN_SEVERITY", "");
		headerMap.put("YEARS_IN_PAIN", "");
		headerMap.put("MORE_REVIEWED_KIT", "");
		headerMap.put("SURGERY_DATE", "");
		headerMap.put("SURGERY_MONTH", "");

		headerMap.put("TOUCHPOINT", "");
		headerMap.put("ATTEMPT_DATE2", "");
		headerMap.put("DAYS_PER_WEEK_IN_PAIN", "");
		headerMap.put("PAIN_SEVERITY_2", "");
		headerMap.put("TERRITORY", "");
		headerMap.put("DISTRIBUTOR_NAME", "");
		headerMap.put("AD", "");
		headerMap.put("REGION", "");
		headerMap.put("AVP", "");
		headerMap.put("ID_CARD_DATA_SOURCE", "");
		headerMap.put("BEST_TIME_TO_CALL", "");
		headerMap.put("TYPE_OF_SURGERY", "");
		headerMap.put("SURGEON", "");
		headerMap.put("HOSPITAL", "");
		headerMap.put("COLLECTION_STATEMENT", "");
		headerMap.put("CALL_1", "");
		headerMap.put("CALL_2", "");
		headerMap.put("LIFE_LIKE_BEFORE_JOINT_REPLACEMENT", "");
		headerMap.put("ACTIVITIES_STOPPED", "");
		headerMap.put("TURNING_POINT", "");
		headerMap.put("LIFE_NOW", "");
		headerMap.put("ADVICE_FOR_OTHERS", "");
		headerMap.put("PARTICIPATE_EDUCATION_EVENT", "");
		headerMap.put("OTHER_COMMENTS", "");
		headerMap.put("PICTURES", "");
		headerMap.put("CONSENT_FORM_SENT", "");
		headerMap.put("CONSENT_FORM_RECEIVED", "");
		headerMap.put("BACKPACK_PEDOMETER", "");

		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		List<?> externalData = (List<?> ) o;
		this.data = (List<PedoKitVO>) externalData;

	}

}
