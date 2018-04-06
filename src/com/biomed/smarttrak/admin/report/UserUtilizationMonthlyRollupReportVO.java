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
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportVO.java</p>
 <p><b>Description: Builds and formats the data for the monthly utilization report</b></p>
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
	private static final String ACCOUNT_NAME = "ACCOUNT_NAME";
	private static final String ACCOUNT_TYPE = "ACCOUNT_TYPE";
	private static final String ACCOUNT_START_DT = "ACCOUNT_START_DATE";
	private static final String ACCOUNT_EXPIRATION_DT = "ACCOUNT_EXPIRATION_DATE";
	private static final String NAME = "NAME";
	private static final String TITLE = "TITLE";
	private static final String EMAIL = "EMAIL_ADDRESS";
	private static final String PHONE = "PHONE";
	private static final String UPDATES = "UPDATES";
	private static final String ACCOUNT_OWNER_FLAG = "ACCOUNT_OWNER";
	private static final String DIVISIONS = "DIVISIONS";
	private static final String USER_STATUS = "USER_STATUS";
	private static final String EXPIRATION_DT = "EXPIRATION_DATE";
	public static final String LAST_LOGIN_DT = "LAST_LOGIN_DATE";
	public static final String LAST_LOGIN_AGE = "LAST_LOGIN_AGE";
	private static final String DAYS_SINCE_LAST_LOGIN = "DAYS_SINCE_LAST_LOGGED_IN";
	public static final String NO_ACTIVITY = "No activity";

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
		
		ExcelReport rpt = new UserUtilizationExcelReport(getHeader());
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
		StringEncoder se = new StringEncoder();
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
				
		// loop the account map
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {

			AccountVO a = acct.getKey();

			// user vals
			Map<String,Object> row;
			Map<String,Integer> counts;

			// loop account users
			for (UserVO user : acct.getValue()) {
				row = new HashMap<>();
				addAccountColumns(a, row);
				row.put(NAME, se.decodeValue(user.getFullName()));
				row.put(TITLE, se.decodeValue(user.getTitle()));
				row.put(EMAIL,user.getEmailAddress());
				pnf.setPhoneNumber(user.getMainPhone());
				row.put(PHONE, pnf.getFormattedNumber());
				row.put(UPDATES, StringUtil.capitalize(user.getUpdates()));
				row.put(ACCOUNT_OWNER_FLAG, user.getAcctOwnerFlg() == 0 ? "No" : "Yes");
				row.put(DIVISIONS, getUserDivisions(user));
				row.put(USER_STATUS, user.getLicenseName());
				row.put(EXPIRATION_DT, Convert.formatDate(user.getExpirationDate(), Convert.DATE_SLASH_PATTERN));
				if (user.getLoginDate() == null) row.put(LAST_LOGIN_DT, NO_ACTIVITY); 
				else row.put(LAST_LOGIN_DT, Convert.formatDate(user.getLoginDate(), Convert.DATE_SLASH_PATTERN));
				row.put(DAYS_SINCE_LAST_LOGIN, user.getLoginAge(true));	
				/* Add the login age to data map for reporting formatting. This particular field is utilized for styling 
				 * and not meant for actual display, hence no matching header column entry*/
				row.put(LAST_LOGIN_AGE, user.getLoginAge());
				
				/* Add monthly counts to user's row. We loop the month headers
				 * List using the values as keys to retrieve a user's counts for a
				 * given month.  If no key/value exists on the user's map, we 
				 * use a count value of zero. */
				counts = (Map<String,Integer>)user.getUserExtendedInfo();
				for (String monthKey : monthHeaders) {
					manageTotals(row,counts,monthKey);
				}
				rows.add(row);

			}

		}

	}
	
	/**
	 * Handles adding the columns associated to the user's account
	 * @param acct
	 * @param row
	 */
	protected void addAccountColumns(AccountVO acct, Map<String,Object> row) {
		row.put(ACCOUNT_NAME, acct.getAccountName());
		row.put(ACCOUNT_TYPE, acct.getTypeName());
		row.put(ACCOUNT_START_DT, acct.getStartDate());
		row.put(ACCOUNT_EXPIRATION_DT, acct.getExpirationDate());
	}
	
	/**
	 * Retrieves any applicable divisions for the user and formats for report
	 * @param user
	 * @return
	 */
	protected String getUserDivisions(UserVO user) {
		List<String> divisions = user.getDivisions();
		StringBuilder divs = new StringBuilder(150);
		
		if(divisions != null && !divisions.isEmpty()) {
			for (int i = 0; i < user.getDivisions().size(); i++) {
				if(i > 0) divs.append(",");
				divs.append(user.getDivisions().get(i));
			}
		}
		return divs.toString();
	}
	
	/**
	 * Manages the monthly page view totals for an account.
	 * @param currRow
	 * @param counts
	 * @param monthKey
	 * @return
	 */
	protected int manageTotals(Map<String,Object> currRow, Map<String,Integer> counts, String monthKey) {
		int mCnt = 0;
		if (counts == null) 
			return mCnt;
		
		if (counts.get(monthKey) != null) {
			mCnt = counts.get(monthKey);

		}
		currRow.put(monthKey,mCnt);
		return mCnt;
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

		int minMonth;
		int maxMonth;

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
		headerMap.put(ACCOUNT_NAME, "Account Name");
		headerMap.put(ACCOUNT_TYPE, "Account Type");
		headerMap.put(ACCOUNT_START_DT, "Account Start Date");
		headerMap.put(ACCOUNT_EXPIRATION_DT, "Account Expiration Date");
		headerMap.put(NAME,"Name");
		headerMap.put(TITLE,"Title");
		headerMap.put(EMAIL,"Email Address");
		headerMap.put(PHONE,"Phone");
		headerMap.put(UPDATES,"Update Frequency");
		headerMap.put(ACCOUNT_OWNER_FLAG, "Account Lead");
		headerMap.put(DIVISIONS, "Divisions");
		headerMap.put(USER_STATUS, "License Type");
		headerMap.put(EXPIRATION_DT, "Expiration Date");
		headerMap.put(LAST_LOGIN_DT, "Last Logged In Date");
		headerMap.put(DAYS_SINCE_LAST_LOGIN, "Days Since Last Logged In");
		for (String monthKey : monthHeaders) {
			headerMap.put(monthKey, monthKey);
		}
		return headerMap;
	} 
	
}