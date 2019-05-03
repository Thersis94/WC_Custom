package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.UserActivityVO;
// SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 27, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4515509174093168302L;
	
	private Map<String, UserActivityVO> activity;
	private static final String REPORT_TITLE = "Activity Report";
	private static final String NAME = "NAME";
	private static final String PAGE = "PAGE";
	private static final String PAGE_TITLE = "PAGE_TITLE";
	private static final String VISIT_DATE = "VISIT_DATE";
	private static final String URI = "URI";
	private static final String EMAIL_ADDRESS = "EMAIL_ADDRESS";
	private static final String ACCOUNT_NM = "ACCOUNT_NAME";
	private static final String CLASSIFICATION = "CLASSIFICATION";
	private static final String LICENSE_TYPE = "LICENSE_TYPE";
	private static final String STATUS = "SUBSCRIBER_STATUS";

	/**
	* Constructor
	*/
	public UserActivityReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE.replace(' ', '-')+".xls");
        activity = new LinkedHashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(REPORT_TITLE);

		List<Map<String, Object>> rows = new ArrayList<>(activity.size());
		generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.activity =  (Map<String,UserActivityVO>) o;
	}

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(List<Map<String, Object>> rows) {
		// loop the account map
		Map<String,Object> row;
		for (Map.Entry<String, UserActivityVO> users : activity.entrySet()) {
			UserActivityVO user = users.getValue();
			// loop users
			String userName = StringUtil.join(user.getFirstName() + " " + user.getLastName());
			for (PageViewVO page : user.getPageViews()) {
				row = new HashMap<>();
				row.put(NAME, userName);
				row.put(PAGE, page.getPageDisplayName());
				row.put(PAGE_TITLE, page.getPageTitleName());
				row.put(URI, page.getRequestUri());
				row.put(VISIT_DATE, page.getVisitDate());
				row.put(EMAIL_ADDRESS, user.getEmailAddressTxt());
				row.put(ACCOUNT_NM, user.getAccountNm());
				row.put(CLASSIFICATION, user.getClassification());
				row.put(LICENSE_TYPE, user.getLicenseType());
				row.put(STATUS, user.getUserStatus());
				rows.add(row);
			}
		}

	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(NAME,"Name");
		headerMap.put(PAGE,"Page");
		headerMap.put(PAGE_TITLE, "Page Title");
		headerMap.put(URI,"Page Address");
		headerMap.put(VISIT_DATE,"Visit Date");
		headerMap.put(EMAIL_ADDRESS,"Subscriber Email Address");
		headerMap.put(ACCOUNT_NM, "Account Name");
		headerMap.put(CLASSIFICATION, "Classification");
		headerMap.put(LICENSE_TYPE, "License Type");
		headerMap.put(STATUS, "Subscriber status");
		return headerMap;
	} 
	
}