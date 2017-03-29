package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

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
public class UserUtilizationMonthlyRollupReportVO extends AbstractSBReportVO {

	private Map<AccountVO, List<UserVO>> accounts;
	private Date dateStart;
	private Date dateEnd;
	private List<String> monthHeaders;
	private static final String REPORT_TITLE = "Utilization Report - Monthly Rollup";
	private static final String NAME = "NAME";
	private static final String TITLE = "TITLE";
	private static final String EMAIL = "EMAIL_ADDRESS";
	private static final String PHONE = "PHONE";
	private static final String UPDATES = "UPDATES";
	private static final String TOTAL = "TOTAL";

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;
	
	/**
	* Constructor
	*/
	public UserUtilizationMonthlyRollupReportVO() {
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
		rpt.setTitleCell(buildReportTitle());

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/**
	 * Builds the report title.
	 * @return
	 */
	protected String buildReportTitle() {
		StringBuilder sb = new StringBuilder(40);
		sb.append(REPORT_TITLE);
		
		if (dateStart != null) {
			sb.append(" (");
			sb.append(Convert.formatDate(dateStart,Convert.DATE_SLASH_MONTH_PATTERN));
			if (dateEnd != null) {
				sb.append(" - ");
				sb.append(Convert.formatDate(dateEnd,Convert.DATE_SLASH_MONTH_PATTERN));
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
		this.accounts =  (Map<AccountVO, List<UserVO>>)reportData.get(UserUtilizationReportAction.KEY_REPORT_DATA);
		dateStart = (Date)reportData.get(UserUtilizationReportAction.KEY_DATE_START);
		dateEnd = (Date)reportData.get(UserUtilizationReportAction.KEY_DATE_END);
		formatMonthHeaders();
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void generateDataRows(List<Map<String, Object>> rows) {

		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
				
		// loop the account map
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {

			Map<String,Integer> acctTotals = new HashMap<>();
			AccountVO a = acct.getKey();

			// add acct header row(s)
			addAccountHeader(rows,a.getAccountName());

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

	}
	
	/**
	 * Manages the monthly page view totals for an account.
	 * @param acctTotals
	 * @param currRow
	 * @param counts
	 * @param monthKey
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
	 * @param monthKey
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
	 */
	protected void addAccountHeader(List<Map<String,Object>> rows, 
			String acctNm) {
		
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
	 * @param rows
	 * @param acctNm
	 * @param acctTotals
	 */
	protected void addAccountFooter(List<Map<String,Object>> rows, String acctNm, 
			Map<String,Integer> acctTotals) {
		Map<String,Object> row = new HashMap<>();
		row.put(NAME,"Total");
		row.put(TITLE,"");
		row.put(EMAIL,"");
		row.put(PHONE,"");
		row.put(UPDATES,"");

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
	 * Formats the month headers based on the starting month. Header
	 * format is 'MMM YY' (e.g. Dec 16, Jan 17, etc.). The headers are added
	 * to the List in proper data order (e.g. Dec 16, Jan 17, Feb 17, etc.).
	 */
	protected void formatMonthHeaders() {
		Calendar cal = GregorianCalendar.getInstance();
		Locale loc = Locale.US;
		// get map of month display names for use in building month headers.
		Map<Integer,String> monthNames = formatMonthNames(cal,loc);

		// calc vals to use
		cal.setTime(dateStart);		
		int startDateYr = cal.get(Calendar.YEAR)%100;
		int startDateMth = cal.get(Calendar.MONTH);

		cal.setTime(dateEnd);
		int endDateYr = cal.get(Calendar.YEAR)%100;
		int endDateMth = cal.get(Calendar.MONTH);

		// create the month headers for the report based on start/end dates.
		addMonthHeaders(monthNames,startDateMth, endDateMth, startDateYr,endDateYr);

	}

	/**
	 * Uses a Calendar instance to get the display names of the months of the year in 
	 * 'short' form (e.g. Jan, Feb, etc.) and returns them in a map useful to us.
	 * @param cal
	 * @param loc
	 * @return
	 */
	protected Map<Integer,String> formatMonthNames(Calendar cal, Locale loc) {
		Map<Integer,String> displayMonths = new HashMap<>();
		Map<String,Integer> calMonths = cal.getDisplayNames(Calendar.MONTH, Calendar.SHORT, loc);
		for (Map.Entry<String,Integer> calMonth : calMonths.entrySet()) {
			displayMonths.put(calMonth.getValue(),calMonth.getKey());
		}
		return displayMonths;
	}

	/**
	 * Adds month header values to the month headers List.  The values
	 * are formatted to correspond to the keys (e.g. Jan 17, Feb 17, etc.)
	 * used to map a user's pageview count to a given month of a given year.
	 * @param monthNames
	 * @param minMonth
	 * @param maxMonth
	 * @param startYr
	 * @param endYr
	 */
	protected void addMonthHeaders(Map<Integer,String> monthNames, 
			int startDateMonth, int endDateMonth, int startYr, int endYr) {

		int minMonth = 0;
		int maxMonth = 0;
		
		for (int yr = startYr; yr <= endYr; yr++) {

			if (yr == startYr) {
				// use start date's starting month
				minMonth = startDateMonth;
				// determine end month to use
				if (startYr == endYr) {
					maxMonth = endDateMonth;
				} else {
					maxMonth = Calendar.DECEMBER;
				}
			} else if (yr == endYr) {
				// for the end yr, use Jan through the end date's month.
				minMonth = Calendar.JANUARY;
				maxMonth = endDateMonth;
			} else {
				// for all other years, use all the months in the year
				minMonth = Calendar.JANUARY;
				maxMonth = Calendar.DECEMBER;
			}
		
			for (int mth = minMonth; mth <= maxMonth; mth++) {
				monthHeaders.add(monthNames.get(mth) + " " + yr);
				log.debug("monthHeader: " + monthNames.get(mth) + " " + yr);
			}
			
			
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