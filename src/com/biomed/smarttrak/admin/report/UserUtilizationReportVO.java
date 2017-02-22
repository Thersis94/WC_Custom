package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;

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
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportVO extends AbstractSBReportVO {

	private Map<AccountVO, List<UserVO>> accounts;
	private final String reportTitle = "Activity Rollup Report";
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;
	
	/**
	* Constructor
	*/
	public UserUtilizationReportVO() {
		/* Sorted by company, showing:
		 * User Name
		 * User Title
		 * User Phone
		 * User Email
		 * User Update Email Preferences
		 * Monthly page views for last 12 months
		 * Summary at bottom of totals.
		 */
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(reportTitle);

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		rows = generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<AccountVO,List<UserVO>> accounts = (Map<AccountVO,List<UserVO>>) o;
		this.accounts = accounts;
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		List<String> monthHeaders = formatMonthHeaders();
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {
			Map<Integer,Integer> acctTotals = new HashMap<>();
			AccountVO a = acct.getKey();
			// add acct header row
			rows.add(formatAccountHeader(a.getAccountName(),monthHeaders));

			// user vals
			Map<String,Object> row = null;
			Map<Integer,Integer> counts = null;
			int userTotal = 0;
			//int finalTotal = 0;
			for (UserVO user : acct.getValue()) {
				row = new HashMap<>();
				row.put("NAME", user.getFullName());
				row.put("TITLE", user.getTitle());
				row.put("EMAIL_ADDRESS",user.getEmailAddress());
				row.put("PHONE", user.getMainPhone());
				row.put("UPDATES", user.getUpdates());

				counts = (Map<Integer,Integer>)user.getUserExtendedInfo();
				for (int i = 0; i < counts.size(); i++) {
					row.put("MONTH"+i, counts.get(i));
					if (acctTotals.get(i) != null) {
						acctTotals.put(i, acctTotals.get(i) + counts.get(i));
					} else {
						acctTotals.put(i, counts.get(i));
					}
					userTotal += counts.get(i);
				}
				row.put("TOTAL", userTotal);

				// add row to list
				rows.add(row);

				//finalTotal += userTotal;
				userTotal = 0;
			}

			// acct footer row
			rows.add(formatAccountFooter(a.getAccountName(), acctTotals));
		}
		return rows;
	}
	
	private Map<String,Object> formatAccountFooter(String acctNm, Map<Integer,Integer> acctTotals) {
		Map<String,Object> row = new HashMap<>();
		row.put("NAME","Total for " + acctNm);
		row.put("TITLE","");
		row.put("EMAIL_ADDRESS","");
		row.put("PHONE","");
		row.put("UPDATES","");
		for (int i = 0; i < acctTotals.size(); i++) {
			row.put("MONTH"+i, acctTotals.get(i));
		}
		row.put("TOTAL","Total");
		return row;
	}
	
	private Map<String,Object> formatAccountHeader(String acctNm, List<String> monthHeaders) {
		Map<String,Object> row = new HashMap<>();
		row.put("NAME",acctNm);
		row.put("TITLE","");
		row.put("EMAIL_ADDRESS","");
		row.put("PHONE","");
		row.put("UPDATES","");
		for (int i = 0; i < monthHeaders.size(); i++) {
			row.put("MONTH"+i, monthHeaders.get(i));
		}
		row.put("TOTAL","Total");
		return row;
	}
	
	/**
	 * Formats the month headers for the past 12 months inclusive.
	 * @return
	 */
	private List<String> formatMonthHeaders() {
		List<String> monthHeaders = new ArrayList<>();
		Locale loc = Locale.US;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -11);
		for (int i = 0; i < 12; i++) {
			monthHeaders.add(cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, loc) + 
					" " + cal.get(Calendar.YEAR)%100);
			cal.add(Calendar.MONTH, 1);
		}
		return monthHeaders;
	}
	
	/**
	 * builds the header map for the excel report
	 * @return
	 */
	private HashMap<String, String> getHeader() {

		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("NAME","");
		headerMap.put("TITLE","");
		headerMap.put("EMAIL_ADDRESS","");
		headerMap.put("PHONE","");
		headerMap.put("UPDATES","");
		headerMap.put("MONTH0","");
		headerMap.put("MONTH1","");
		headerMap.put("MONTH2","");
		headerMap.put("MONTH3","");
		headerMap.put("MONTH4","");
		headerMap.put("MONTH5","");
		headerMap.put("MONTH6","");
		headerMap.put("MONTH7","");
		headerMap.put("MONTH8","");
		headerMap.put("MONTH9","");
		headerMap.put("MONTH10","");
		headerMap.put("MONTH11","");
		headerMap.put("TOTAL","");
		return headerMap;
	} 
	
}
