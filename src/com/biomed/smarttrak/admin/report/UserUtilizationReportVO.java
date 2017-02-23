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
import com.siliconmtn.util.PhoneNumberFormat;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

import freemarker.template.utility.StringUtil;

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
	private List<String> monthHeaders;
	private static final String REPORT_TITLE = "Utilization Report";
	private static final String NAME = "NAME";
	private static final String TITLE = "TITLE";
	private static final String EMAIL = "EMAIL_ADDRESS";
	private static final String PHONE = "PHONE";
	private static final String UPDATES = "UPDATES";
	private static final String TOTAL = "TOTAL";
	private static final int HEADER_MONTHS_SIZE = 12;

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;
	
	/**
	* Constructor
	*/
	public UserUtilizationReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE.replace(' ', '-')+".xls");
        accounts = new HashMap<>();
        monthHeaders = new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(REPORT_TITLE);

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
		this.accounts =  (Map<AccountVO,List<UserVO>>) o;
		formatMonthHeaders();
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
				
		// loop the account map
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {

			Map<String,Integer> acctTotals = new HashMap<>();
			AccountVO a = acct.getKey();

			// add acct header row(s)
			addAccountHeader(rows,a.getAccountName(),monthHeaders);

			// user vals
			Map<String,Object> row;
			Map<String,Integer> counts;
			int userTotal = 0;

			// loop account users
			for (UserVO user : acct.getValue()) {
				row = new HashMap<>();
				row.put(NAME, user.getFullName());
				row.put(TITLE, user.getTitle());
				row.put(EMAIL,user.getEmailAddress());
				pnf.setPhoneNumber(user.getMainPhone());
				row.put(PHONE, pnf.getFormattedNumber());
				row.put(UPDATES, StringUtil.capitalize(user.getUpdates()));
				
				/* Add monthly counts to user's row. We loop the month headers
				 * List using the values as keys to retrieve a user's counts for a
				 * given month.  If no key/value exists on the user's map, we 
				 * use a count value of zero. */
				counts = (Map<String,Integer>)user.getUserExtendedInfo();
				for (String monthKey : monthHeaders) {
					userTotal += manageTotals(acctTotals,row,counts,monthKey);
				}
				row.put(TOTAL, userTotal);
				rows.add(row);
				userTotal = 0;

			}

			// acct footer row(s)
			addAccountFooter(rows, a.getAccountName(), acctTotals);
		}
		return rows;
	}
	
	/**
	 * Manages the monthly page view totals for an account.
	 * @param acctTotals
	 * @param currRow
	 * @param currIdx
	 * @param currPageCount
	 * @return
	 */
	protected int manageTotals(Map<String,Integer>acctTotals, 
			Map<String,Object> currRow, Map<String,Integer> counts, String monthKey) {
		int mCnt = 0;
		if (counts == null) return mCnt;
		if (counts.get(monthKey) != null) {
			mCnt = counts.get(monthKey);
			updateAccountTotal(acctTotals,monthKey,mCnt);
		}
		currRow.put(monthKey,mCnt);
		return mCnt;
	}

	/**
	 * Updates the page view totals for an account.
	 * @param acctTotals
	 * @param index
	 * @param countVal
	 */
	protected void updateAccountTotal(Map<String,Integer> acctTotals, String monthKey, int countVal) {
		if (acctTotals.get(monthKey) != null) {
			acctTotals.put(monthKey, acctTotals.get(monthKey) + countVal);
		} else {
			acctTotals.put(monthKey, countVal);
		}
	}

	/**
	 * Formats the account's header row.
	 * @param rows
	 * @param acctNm
	 * @param monthHeaders
	 */
	protected void addAccountHeader(List<Map<String,Object>> rows, 
			String acctNm, List<String> monthHeaders) {
		
		// add account name row.
		Map<String,Object> row = new HashMap<>();
		row.put(NAME,acctNm);
		row.put(TITLE,"");
		row.put(EMAIL,"");
		row.put(PHONE,"");
		row.put(UPDATES,"");
		for (String monthKey : monthHeaders) {
			row.put(monthKey, "");
		}
		row.put(TOTAL,"");
		rows.add(row);
	}

	/**
	 * Builds the account's footer row.
	 * @param acctNm
	 * @param acctTotals
	 * @return
	 */
	protected void addAccountFooter(List<Map<String,Object>> rows, String acctNm, 
			Map<String,Integer> acctTotals) {
		Map<String,Object> row = new HashMap<>();
		row.put(NAME,"Total");
		row.put(TITLE,"");
		row.put(EMAIL,"");
		row.put(PHONE,"");
		row.put(UPDATES,"");
		//int acctTotal = 0;
		Integer monthVal;
		int acctTotal = 0;
		int monthTotal = 0;
		for (String monthKey : monthHeaders) {
			monthVal = acctTotals.get(monthKey);
			if (monthVal != null) monthTotal = monthVal;
			row.put(monthKey, monthTotal);
			acctTotal += monthTotal;
			monthTotal = 0;
		}
		row.put(TOTAL, acctTotal);
		rows.add(row);
		
		row = new HashMap<>();
		row.put(NAME,"");
		row.put(TITLE,"");
		row.put(EMAIL,"");
		row.put(PHONE,"");
		row.put(UPDATES,"");
		for (String monthKey : monthHeaders) {
			row.put(monthKey, "");
		}
		row.put(TOTAL,"");
		rows.add(row);
	}

	/**
	 * Formats the month headers for the past 12 months inclusive. Header
	 * format is 'MMM YY' (e.g. Dec 16, Jan 17, etc.). The headers are added
	 * to the List in proper data order (e.g. Dec 16, Jan 17, Feb 17, etc.).
	 * @return
	 */
	protected void formatMonthHeaders() {
		Locale loc = Locale.US;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, 1 - HEADER_MONTHS_SIZE);
		for (int idx = 0; idx < HEADER_MONTHS_SIZE; idx++) {
			monthHeaders.add(cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, loc) + 
					" " + cal.get(Calendar.YEAR)%100);
			cal.add(Calendar.MONTH, 1);
		}
	}
	
	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		// this header is intentionally left blank.
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(NAME,"Name");
		headerMap.put(TITLE,"Title");
		headerMap.put(EMAIL,"Email Address");
		headerMap.put(PHONE,"Phone");
		headerMap.put(UPDATES,"Update Frequency");
		for (String monthKey : monthHeaders) {
			headerMap.put(monthKey, monthKey);
		}
		headerMap.put(TOTAL,"Total");
		return headerMap;
	} 
	
}