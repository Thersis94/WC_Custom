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
	private Map<Integer,String> monthKeyMap;
	
	public enum UtilizationReportType {
		DAYS_14(14),
		DAYS_90(90),
		MONTHS_12(11); // report using months value has a unit val of 11.
		
		private int unitVal;
		UtilizationReportType(int unitVal) { this.unitVal = unitVal; }
		public int getUnitVal() { return unitVal; }
	}
		
	/**
	* Constructor
	*/
	public UserUtilizationReportAction() {
		super();
		monthKeyMap = new HashMap<>();
	}

	/**
	* Constructor
	*/
	public UserUtilizationReportAction(ActionInitVO arg0) {
		super(arg0);
		monthKeyMap = new HashMap<>();
	}

	/**
	 * Retrieves the user utilization data and returns it as a Map of AccountVO mapped to a List of UserVO for
	 * each account.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public Map<AccountVO, List<UserVO>> retrieveUserUtilization(ActionRequest req) throws ActionException {

		StringEncrypter se = initStringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String siteId = site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId();
		
		// determine utilization report type and craft start date from the type.
		UtilizationReportType urt = parseReportType(req.getParameter("utilizationReportType"));
		String dateStart = buildReportStartDate(urt);
		
		// 1. get user page views for the given start date.
		List<PageViewVO> pageViews = retrieveBasePageViews(siteId,dateStart);
		log.debug("raw pageViews size: " + pageViews.size());
		
		// parse the pageviews into the appropriate map
		Map<String,Map<String,Integer>> pageCounts = parsePageCounts(pageViews,urt);

		// 2. if we retrieved nothing, return emptiness.
		if (pageCounts.size() == 0) return new HashMap<>();

		// 3. get accounts data
		Map<AccountVO, List<UserVO>> accounts = retrieveAccountsUsers(se, pageCounts, siteId);
		log.debug("accounts map size: " + accounts.size());

		// 4. merge accounts data and user page counts
		mergeData(accounts, pageCounts);

		return accounts;

	}
	
	/**
	 * Parses the utilization report type parameter into a UtilizationReportType enum.
	 * @param reportTypeParam
	 * @return
	 */
	protected UtilizationReportType parseReportType(String reportTypeParam) {
		UtilizationReportType urt;
		// determine enum.
		try {
			urt = UtilizationReportType.valueOf(StringUtil.checkVal(reportTypeParam).toUpperCase());
		} catch (Exception e) {
			urt = UtilizationReportType.MONTHS_12;
		}
		return urt;
	}
	
	/**
	 * Builds the report start date based on the UtilizationReportType enum passed in.
	 * @param urt
	 * @return
	 */
	protected String buildReportStartDate(UtilizationReportType urt) {
		// build start date from enum.
		Calendar cal = GregorianCalendar.getInstance();
		switch(urt) {
			case DAYS_14:
			case DAYS_90:
				cal.add(Calendar.DAY_OF_YEAR, -1*urt.getUnitVal());
				break;
			default:
				cal.add(Calendar.MONTH, -1*urt.getUnitVal());
				break;
		}
		return Convert.formatDate(cal.getTime(),Convert.DATE_DASH_PATTERN);
	}
	
	/**
	 * Retrieves the monthly page view counts for all logged in users who have accessed the given
	 * site since the given starting date.
	 * @param siteId
	 * @return
	 * @throws ActionException 
	 */
	protected List<PageViewVO> retrieveBasePageViews(String siteId, 
			String dateStart) throws ActionException {
		log.debug("retrieveBasePageViews...");
		PageViewRetriever pvr = new PageViewRetriever(dbConn);
		return pvr.retrievePageViewsRollup(siteId, dateStart);		
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
		StringBuilder sql = buildAccountsQuery(profileIds.length, fieldMap.size());
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
	 * Parses the page count results into a Map of profile IDs that are mapped 
	 * to another map representing the month or day (key = String) and page count for that 
	 * unit of time (value = Integer).  The unit is determined by the UtilizationReportType enum
	 * passed in. 
	 * @param pageViews
	 * @param urt
	 * @return
	 */
	protected Map<String, Map<String,Integer>> parsePageCounts(List<PageViewVO> pageViews, 
			UtilizationReportType urt) {
		String prevId = null;
		String currId;

		String prevKey = null;
		String currKey;
		int pageCnt = 0;

		// Map of month number to pageviews
		Map<String,Integer> userCounts =  new LinkedHashMap<>();
		// Map of profileId to Map of Month, pageCount
		Map< String, Map<String,Integer> > pageCounts = new HashMap<>();
		Calendar cal = Calendar.getInstance();

		for (PageViewVO page : pageViews) {
			currId = page.getProfileId();
			currKey = buildUnitKey(urt, cal, page.getVisitDate());

			if (! currId.equalsIgnoreCase(prevId)) {
				// changed users or first time through.
				if (prevId != null) {
					// put month count on map
					userCounts.put(prevKey, pageCnt);
					
					// put list on map of user|user's months
					pageCounts.put(prevId, userCounts);
				}

				// init userMonths map
				userCounts = new LinkedHashMap<>();
				
				// init current month's view count.
				pageCnt = 1;
				
			} else {
				// process record for current user
				if (! currKey.equalsIgnoreCase(prevKey)) {
					// month changed, put previous month page count on map
					userCounts.put(prevKey, pageCnt);
					
					// reset monthCnt to 1.
					pageCnt = 1;
					
				} else {
					// incr this month's count
					pageCnt++;
				}

			}
			// capture previous vals for comparison
			prevId = currId;
			prevKey = currKey;
		}

		// If we processed records, pick up the last record.
		if (prevId != null) {
			userCounts.put(prevKey, pageCnt);
			pageCounts.put(prevId, userCounts);
		}
		log.debug("monthly pageCounts map size: " + pageCounts.size());
		return pageCounts;
	}

	/**
	 * Builds the count map key from the given date depending upon the UtilizationReportType
	 * enum passed in.  If this is a monthly rollup, the 
	 * @param urt
	 * @param cal
	 * @param date
	 * @return
	 */
	protected String buildUnitKey(UtilizationReportType urt, Calendar cal, Date date) {
		String unitKey;
		switch(urt) {
			case DAYS_14:
			case DAYS_90:
				unitKey = Convert.formatDate(date,Convert.DATE_SLASH_PATTERN);
				break;
			default:
				cal.setTime(date);
				int monthVal = cal.get(Calendar.MONTH);
				if (monthKeyMap.get(monthVal) != null) {
					unitKey = monthKeyMap.get(monthVal);
				} else {
					unitKey = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US);
					unitKey += " " + (cal.get(Calendar.YEAR)%100);
					monthKeyMap.put(monthVal,unitKey);
				}
				break;
		}
		return unitKey;
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
				users = new ArrayList<>();
				
				// init new user.
				user = formatNextUser(se,rs,currPid);
				
			} else {
				// ensure non-null objects.
				if (users == null) users = new ArrayList<>();
				if (user == null) user = formatNextUser(se,rs,currPid);
				
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
	protected StringBuilder buildAccountsQuery(int numProfileIds, int numFields) {
		StringBuilder sql = new StringBuilder(925);
		sql.append("select ac.account_id, ac.account_nm, ");
		sql.append("us.profile_id, us.user_id, ");
		sql.append("pf.first_nm, pf.last_nm, pf.email_address_txt, ");
		sql.append("ph.phone_number_txt, ");
		sql.append("rd.register_field_id, rd.value_txt ");
		sql.append("from custom.biomedgps_account ac ");
		sql.append("inner join custom.biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("and ac.status_no != ? and us.status_cd != ? ");
		sql.append("and us.profile_id in (");
		for (int i = 0; i < numProfileIds; i++) {
			if (i > 0) sql.append(",");
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
			if (i > 0) sql.append(",");
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