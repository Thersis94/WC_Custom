package com.biomed.smarttrak.admin.report;

// JDK 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.PageViewRetriever;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportAction extends SimpleActionAdapter {
	
	public static final String STATUS_NO_INACTIVE = "I";
	public static final String KEY_DATE_START = "dateStart";
	public static final String KEY_DATE_END = "dateEnd";
	public static final String KEY_REPORT_DATA = "reportData";
	public static final String PARAM_IS_DAILY = "isDaily";
	private Map<String,String> monthKeyMap;
	private String dateStart;
	private String dateEnd;
	private Date reportDateStart;
	private Date reportDateEnd;
	private Map<String,String> monthNames;
		
	/**
	* Constructor
	*/
	public UserUtilizationReportAction() {
		super();
		monthKeyMap = new HashMap<>();
		monthNames = formatMonthNames();
	}

	/**
	* Constructor
	*/
	public UserUtilizationReportAction(ActionInitVO arg0) {
		super(arg0);
		monthKeyMap = new HashMap<>();
		monthNames = formatMonthNames();
	}

	/**
	 * Retrieves the user utilization data and returns it as a Map of AccountVO mapped to a List of UserVO for
	 * each account.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public Map<String,Object> retrieveUserUtilization(ActionRequest req) throws ActionException {

		StringEncrypter se = initStringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String siteId = site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId();

		boolean isDaily = Convert.formatBoolean(req.getParameter(PARAM_IS_DAILY));
		dateStart = formatReportDate(req.getParameter("dateStart"), true, isDaily);
		dateEnd = formatReportDate(req.getParameter("dateEnd"), false, isDaily);

		// 1. get user pageviews for the given start date.
		List<PageViewVO> pageViews = retrieveBasePageViews(siteId,isDaily);
		log.debug("raw pageViews size: " + pageViews.size());

		// 2. parse the pageviews into a map of page counts for each user
		Map<String,Map<String,Integer>> pageCounts = parsePageCounts(pageViews,isDaily);

		// 3. if we retrieved nothing, return a proper report data map
		if (pageCounts.size() == 0) 
			return formatReportDataMap(new HashMap<>());

		// 4. get accounts and account users data for the profile IDs for which we found page views.
		Map<AccountVO, List<UserVO>> accounts = retrieveAccountsUsers(se, pageCounts, siteId);
		log.debug("accounts map size: " + accounts.size());

		// 5. merge accounts data and user page counts
		mergeData(accounts, pageCounts);

		return formatReportDataMap(accounts);
	}
	
	/**
	 * Formats the report data map.
	 * @param accounts
	 * @return
	 */
	protected Map<String,Object> formatReportDataMap(Map<AccountVO,List<UserVO>> accounts) {
		Map<String,Object> reportData = new HashMap<>();
		reportData.put(KEY_DATE_START, reportDateStart);
		reportData.put(KEY_DATE_END, reportDateEnd);
		reportData.put(KEY_REPORT_DATA, accounts);
		return reportData;
	}

	/**
	 * Checks the String date valued passed in.  If the String is null or empty, a null 
	 * value is returned, otherwise the value of the String date is returned.
	 * @param date
	 * @param isStartDate
	 * @param isDailyRollup
	 * @return
	 */
	protected String formatReportDate(String date, boolean isStartDate, boolean isDailyRollup) {
		// make sure we have a date to work with.
		String strDate = StringUtil.checkVal(date);
		if (strDate.isEmpty()) {
			Calendar cal = GregorianCalendar.getInstance();
			// make sure String representation of date is uniform with what comes from datepicker
			strDate = Convert.formatDate(cal.getTime());
		}
		// convert to a date with a uniform format before formatting for query
		Date tmpDate = Convert.formatDate(Convert.DATE_SLASH_PATTERN,strDate);
		if (isDailyRollup) {
			tmpDate = formatDailyRollupDate(tmpDate,isStartDate);
		} else {
			tmpDate = formatMonthlyRollupDate(tmpDate,isStartDate);
		}
		
		// return the date as a String in date/time dash pattern
		return Convert.formatDate(tmpDate,Convert.DATE_TIME_DASH_PATTERN); 
	}
	
	/**
	 * Formats a date for the daily rollup report.
	 * @param theDate
	 * @param isStartDate
	 * @return
	 */
	protected Date formatDailyRollupDate(Date theDate, boolean isStartDate) {
		if (isStartDate) {
			reportDateStart = theDate;
			return Convert.formatStartDate(theDate);
		}
		/* not start date, so return the date formatted as the end date.
		 * We capture the end date before we format it as an end date
		 * so that we preserve the original for the report.*/
		reportDateEnd = theDate;
		return Convert.formatEndDate(theDate);
	}
	
	/**
	 * Formats a date for the monthly rollup report.
	 * @param theDate
	 * @param isStartDate
	 * @return
	 */
	protected Date formatMonthlyRollupDate(Date theDate, boolean isStartDate) {
		if (isStartDate) {
			reportDateStart = theDate;
			/* Set the start date to be the 1st day of the start date's month. We want the
			 * data for the entire month. */
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(theDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			return cal.getTime();
		}
		// Not start date so return the date untouched.
		reportDateEnd = theDate;
		return theDate;
	}
	
	/**
	 * Retrieves the monthly page view counts for all logged in users who have accessed the given
	 * site since the given starting date.
	 * @param siteId
	 * @return
	 * @throws ActionException 
	 */
	protected List<PageViewVO> retrieveBasePageViews(String siteId,
			boolean isDaily) throws ActionException {
		log.debug("retrieveBasePageViews...");
		PageViewRetriever pvr = new PageViewRetriever(dbConn);
		return pvr.retrievePageViewsRollup(siteId,dateStart,dateEnd,isDaily);
	}
	
	/**
	 * Retrieves accounts and account user information for the list of users who accessed the
	 * given site.
	 * @param se
	 * @param userPageCounts
	 * @param siteId
	 * @return
	 */
	protected Map<AccountVO, List<UserVO>> retrieveAccountsUsers(StringEncrypter se, 
			Map<String,Map<String,Integer>> userPageCounts, String siteId) {
		log.debug("retrieveAccountsUsers...");
		Map<String,String> fieldMap = buildRegistrationFieldMap();
		String[] profileIds = userPageCounts.keySet().toArray(new String[]{});
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = buildAccountsQuery(schema, profileIds.length, fieldMap.size());
		log.debug("accounts SQL: " + sql.toString());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			ps.setString(++idx, STATUS_NO_INACTIVE);
			ps.setString(++idx, STATUS_NO_INACTIVE);
			for (String profileId : profileIds) {
				ps.setString(++idx, profileId);
			}
			ps.setString(++idx, PhoneVO.HOME_PHONE);
			ps.setString(++idx, siteId);
			for (Map.Entry<String,String> field : fieldMap.entrySet()) {
				ps.setString(++idx, field.getKey());
			}

			ResultSet rs = ps.executeQuery();

			return parseAccountsUsersResults(rs,se);

		} catch (Exception e) {
			log.error("Error retrieving accounts users, ", e);
			return new LinkedHashMap<>();
		}

	}

	/**
	 * Merges user page view history with accounts' user data.
	 * @param accounts
	 * @param userData
	 */
	protected void mergeData(Map<AccountVO,List<UserVO>> accounts, Map<String,Map<String,Integer>> userData) {
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {
			List<UserVO> acctUsers = acct.getValue();
			for (UserVO user : acctUsers) {
				// set this user's page count history as extended info.
				user.setUserExtendedInfo(userData.get(user.getProfileId()));
			}
		}
	}

	/**
	 * Parses the page count results into a Map of profile IDs.  Each profile ID is mapped 
	 * to a Map<String,Integer> representing the user's page count for that unit of time (month or day).
	 * passed in.
	 * @param pageViews
	 * @param isDaily
	 * @return
	 */
	protected Map<String, Map<String,Integer>> parsePageCounts(List<PageViewVO> pageViews, 
			boolean isDaily) {
		String currId;
		String currDateKey;

		// Map of profileId to Map of Month, pageCount
		Map< String, Map<String,Integer>> pageCounts = new HashMap<>();

		for (PageViewVO page : pageViews) {
			currId = page.getProfileId();
			currDateKey = buildDateKey(page.getReferenceCode(),isDaily);
			addPageCount(pageCounts,currId,currDateKey,page.getResponseCode());
		}

		log.debug("pageCounts map size: " + pageCounts.size());
		return pageCounts;
	}
	
	/**
	 * Adds the rolled up page count to the user's page count map.  If the user doesn't exist on
	 * the pageCounts map, the user is added.
	 * @param pageCounts
	 * @param profileId
	 * @param dateKey
	 * @param rolledUpPageCount
	 */
	protected void addPageCount(Map<String,Map<String,Integer>> pageCounts, 
			String profileId, String dateKey, Integer rolledUpPageCount) {
		// if user doesn't exist on main map, add.
		if (pageCounts.get(profileId) == null) {
			pageCounts.put(profileId, new LinkedHashMap<>());
		}
		// put the count for this date on user's count map
		pageCounts.get(profileId).put(dateKey, rolledUpPageCount);
	}
	
	/**
	 * Builds the page count date key from the given String depending upon the type of report.
	 * @param refCode
	 * @param isDailyRollup
	 * @return
	 */
	protected String buildDateKey(String refCode,boolean isDailyRollup) {
		if (isDailyRollup) {
			// return String representing the date in slash pattern (e.g. 10/27/16)
			return refCode;
		}
		// return String representing the date as 'month year' (e.g. Oct 16)
		return formatMonthlyDateKey(refCode);
	}

	/**
	 * Formats the page count date key used for the user's monthly page counts map.
	 * @param refCode
	 * @return
	 */
	protected String formatMonthlyDateKey(String refCode) {
		// split refCode into month (index 0) and year (index 1)
		String[] keyParts = refCode.split(PageViewRetriever.ROLLUP_DATE_DELIMITER);
		String unitKey = monthNames.get(keyParts[0]) + " " + keyParts[1].substring(keyParts[1].length() - 2);
		if (monthKeyMap.get(unitKey) != null) {
			return monthKeyMap.get(unitKey);
		} else {
			monthKeyMap.put(unitKey,unitKey);
			log.debug("added unitkey: " + unitKey);
			return unitKey;
		}
	}

	/**
	 * Parses the result set from the accounts /account user query.
	 * @param rs
	 * @param se
	 * @return
	 * @throws SQLException
	 */
	protected Map<AccountVO,List<UserVO>> parseAccountsUsersResults(ResultSet rs, 
			StringEncrypter se) throws SQLException {
		
		String prevAcct = null;
		String prevPid = null;
		String currAcct;
		String currPid;
		
		UserVO user = null;
		AccountVO acct = null;
		List<UserVO> users = null;
		Map<AccountVO, List<UserVO>> accounts = new LinkedHashMap<>();
		
		while (rs.next()) {

			currAcct = rs.getString("account_id");
			currPid = rs.getString("profile_id");
			
			if (currPid == null) 
				continue;

			if (! currAcct.equalsIgnoreCase(prevAcct)) {
				// first time through or changed accounts
				if (acct != null) {
					users.add(user);
					accounts.put(acct, users);
				}
				// init new acct
				acct = new AccountVO();
				acct.setAccountId(rs.getString("account_id"));
				acct.setAccountName(rs.getString("account_nm"));
				
				// init user list for this acct.
				users = formatUsersList(users,true);
				
				// init new user.
				user = formatNextUser(se,rs,currPid);
				
			} else {
				// ensure non-null objects.
				users = formatUsersList(users,false);

				if (user == null) 
					user = formatNextUser(se,rs,currPid);
				
				// check for user change
				if (! currPid.equalsIgnoreCase(prevPid)) {
					// changed users, add prev user to list
					users.add(user);
					// init new user
					user = formatNextUser(se,rs,currPid);
				}
			}
			
			// add registration field attribute here.
			user.addAttribute(rs.getString("register_field_id"), rs.getString("value_txt"));
			
			// capture values for comparison
			prevAcct = currAcct;
			prevPid = currPid;
		}
		log.debug("accounts map size: " + accounts.size());
		return accounts;
	}
	
	protected List<UserVO> formatUsersList(List<UserVO> users, boolean returnNewList) {
		if (users == null || returnNewList) 
			return new ArrayList<>();
		return users;
	}
		
	/**
	 * Creates, populates, and returns a UserVO based on the first record found for the user.
	 * @param se
	 * @param rs
	 * @param profileId
	 * @return
	 */
	protected UserVO formatNextUser(StringEncrypter se, ResultSet rs, String profileId) {
		UserVO user = new UserVO();
		try {
			user.setProfileId(profileId);
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
			user.setEmailAddress(se.decrypt(rs.getString("email_address_txt")));
			user.setMainPhone(se.decrypt(rs.getString("phone_number_txt")));
		} catch (Exception e) {
			log.error("Error formatting user, " + profileId + ", " + e.getMessage());
		}
		return user;
	}
	
	/**
	 * Formats the accounts/account user query.
	 * @param numProfileIds
	 * @param numFields
	 * @return
	 */
	protected StringBuilder buildAccountsQuery(String schema, int numProfileIds, int numFields) {
		StringBuilder sql = new StringBuilder(925);
		sql.append("select ac.account_id, ac.account_nm, ");
		sql.append("us.profile_id, us.user_id, ");
		sql.append("pf.first_nm, pf.last_nm, pf.email_address_txt, ");
		sql.append("ph.phone_number_txt, ");
		sql.append("rd.register_field_id, rd.value_txt ");
		sql.append("from ").append(schema).append("biomedgps_account ac ");
		sql.append("inner join ").append(schema).append("biomedgps_user us ");
		sql.append("on ac.account_id = us.account_id ");
		sql.append("and ac.status_no != ? and us.status_cd != ? ");
		sql.append("and us.profile_id in (");
		for (int i = 0; i < numProfileIds; i++) {
			if (i > 0) 
				sql.append(",");
			
			sql.append("?");
		}
		sql.append(")");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("left join phone_number ph on pf.profile_id = ph.profile_id ");
		sql.append("and ph.phone_type_cd = ? ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("and site_id = ? ");
		sql.append("inner join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("and rd.register_field_id in (");
		for (int i = 0; i < numFields; i++) {
			if (i > 0) 
				sql.append(",");
			
			sql.append("?");
		}
		sql.append(") ");
		sql.append("order by account_nm, profile_id");
		return sql;
	}
	
	/**
	 * Builds a map of registration field Ids/names.
	 * @return
	 */
	protected Map<String,String> buildRegistrationFieldMap() {
		Map<String,String> fieldMap = new HashMap<>();
		fieldMap.put(RegistrationMap.TITLE.getFieldId(), "Title");
		fieldMap.put(RegistrationMap.UPDATES.getFieldId(), "Updates");
		return fieldMap;
	}

	/**
	 * Uses a Calendar instance to get the display names of the months of the year in 
	 * 'short' form (e.g. Jan, Feb, etc.) and returns them in a map useful to us.
	 * @param cal
	 * @param loc
	 * @return
	 */
	protected Map<String,String> formatMonthNames() {
		Calendar cal = GregorianCalendar.getInstance();
		Map<String,String> displayMonths = new HashMap<>();
		Map<String,Integer> calMonths = cal.getDisplayNames(Calendar.MONTH, Calendar.SHORT, Locale.US);
		Integer monthVal;
		for (Map.Entry<String,Integer> calMonth : calMonths.entrySet()) {
			monthVal = calMonth.getValue() + 1; // Calendar months are zero-based, compensate here.
			displayMonths.put(monthVal.toString(),calMonth.getKey());
		}
		return displayMonths;
	}
	
	/**
	 * Instantiates a StringEncrypter.
	 * @param key
	 * @return
	 * @throws ActionException
	 */
	protected StringEncrypter initStringEncrypter(String key) throws ActionException {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (Exception e) {
			throw new ActionException("Error instantiating StringEncrypter: " + e.getMessage());
		}
		return se;
	}

}