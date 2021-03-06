package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.UserVO;
// SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 27, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationDailyRollupReportVO extends AbstractSBReportVO {

	private Map<AccountUsersVO, List<UserVO>> accounts;
	private Date dateStart;
	private Date dateEnd;
	private static final String REPORT_TITLE = "Usage Report - Daily Rollup";
	private static final String ACCT_NM = "ACCT_NM";
	private static final String FIRST_NM = "FIRST_NM";
	private static final String LAST_NM = "LAST_NM";
	private static final String EMAIL = "EMAIL";
	private static final String DATE = "DATE";
	private static final String HITS = "HITS";

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;

	/**
	* Constructor
	*/
	public UserUtilizationDailyRollupReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE.replace(' ', '-')+".xls");
        accounts = new HashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(buildReportTitle());

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}
	
	/**
	 * Builds the report title.
	 * @param title
	 * @param suffix
	 * @return
	 */
	protected String buildReportTitle() {
		StringBuilder sb = new StringBuilder(40);
		sb.append(REPORT_TITLE);
		
		if (dateStart != null) {
			sb.append(" (");
			sb.append(Convert.formatDate(dateStart,Convert.DATE_SHORT_MONTH));
			if (dateEnd != null) {
				sb.append(" - ");
				sb.append(Convert.formatDate(dateEnd,Convert.DATE_SHORT_MONTH));
			}
			sb.append(")");
		}
		
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<String,Object> reportData = (Map<String,Object>) o;
		this.accounts =  (Map<AccountUsersVO, List<UserVO>>)reportData.get(UserUtilizationReportAction.KEY_REPORT_DATA);
		dateStart = (Date)reportData.get(UserUtilizationReportAction.KEY_DATE_START);
		dateEnd = (Date)reportData.get(UserUtilizationReportAction.KEY_DATE_END);
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void generateDataRows(List<Map<String, Object>> rows) {
				
		// loop the account map
		for (Map.Entry<AccountUsersVO, List<UserVO>> acct : accounts.entrySet()) {

			AccountUsersVO a = acct.getKey();

			// user vals
			Map<String,Object> row;
			
			// loop account users
			Map<String,Integer> pageCount;
			for (UserVO user : acct.getValue()) {
				pageCount = (Map<String,Integer>)user.getUserExtendedInfo();
				if (pageCount == null) continue;
				for (Map.Entry<String,Integer> count : pageCount.entrySet()) {
					row = new HashMap<>();
					row.put(ACCT_NM, a.getAccountName());
					row.put(FIRST_NM, user.getFirstName());
					row.put(LAST_NM, user.getLastName());
					row.put(EMAIL,user.getEmailAddress());
					row.put(DATE, count.getKey());
					row.put(HITS, count.getValue());
					rows.add(row);
				}
			}
		}

	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(ACCT_NM,"Account");
		headerMap.put(FIRST_NM,"First");
		headerMap.put(LAST_NM,"Last");
		headerMap.put(EMAIL,"Email");
		headerMap.put(DATE,"Date");
		headerMap.put(HITS,"Hits");
		return headerMap;
	} 
	
}