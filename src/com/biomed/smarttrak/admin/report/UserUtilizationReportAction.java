package com.biomed.smarttrak.admin.report;

// JDK 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

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
		
	/**
	* Constructor
	*/
	public UserUtilizationReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserUtilizationReportAction(ActionInitVO arg0) {
		super(arg0);
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

		// 1. get user page views for the given time interval
		Map<String,Map<Integer,Integer>> userPageCounts = retrieveMonthlyPageCount(site.getSiteId(), req.getParameter("dateStart"));

		// 2. get accounts data
		Map<AccountVO, List<UserVO>> accounts = retrieveAccountsUsers(se, userPageCounts, site.getSiteId());

		// 3. merge
		mergeData(accounts, userPageCounts);

		return accounts;

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
			Map<String,Map<Integer,Integer>> userPageCounts, String siteId) {
		Map<String,String> fieldMap = buildFieldMap();
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
			return parseResults(rs,se);
		} catch (Exception e) {
			log.error("Error retrieving accounts users, ", e);
			return new LinkedHashMap<>();
		}

	}
	
	/**
	 * Parses the result set from the accounts /account user query.
	 * @param rs
	 * @param se
	 * @return
	 * @throws SQLException
	 */
	protected Map<AccountVO,List<UserVO>> parseResults(ResultSet rs, 
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
				// same account, check for user change
				if (! currPid.equalsIgnoreCase(prevPid)) {
					// changed users, add prev user to list
					users.add(user);
					// init new user
					user = formatNextUser(se,rs,currPid);
				} else {
					// same user, just add reg field value
					user.addAttribute(rs.getString("register_field_id"), rs.getString("value_txt"));
				}
			}

			// capture values for comparison
			prevAcct = currAcct;
			prevPid = currPid;
		}

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
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
			user.setEmailAddress(se.decrypt(rs.getString("email_address_txt")));
			user.setMainPhone(se.decrypt(rs.getString("phone_number_txt")));
			user.addAttribute(rs.getString("register_field_id"), rs.getString("value_txt"));
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
	protected Map<String,String> buildFieldMap() {
		Map<String,String> fieldMap = new HashMap<>();
		fieldMap.put(RegistrationMap.TITLE.getFieldId(), "Title");
		fieldMap.put(RegistrationMap.FAVORITEUPDATES.getFieldId(), "Favorites Updates");
		fieldMap.put(RegistrationMap.UPDATES.getFieldId(), "Updates");
		return fieldMap;
	}
	
	/**
	 * Retrieves the monthly page view counts for all logged in users who have accessed the given
	 * site since the given starting date.
	 * @param siteId
	 * @param dateStart
	 * @return
	 */
	protected Map<String, Map<Integer,Integer>> retrieveMonthlyPageCount(String siteId, String dateStart) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select profile_id, visit_dt from pageview_user ");
		sql.append("where site_id = ? and profile_id is not null and visit_dt > ? ");
		sql.append("order by profile_id, visit_dt ");
		log.debug("pageview user SQL: " + sql.toString());

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, siteId);
			ps.setDate(2, Convert.formatSQLDate(checkStartDate(dateStart)));
			ResultSet rs = ps.executeQuery();
			
			return this.parseMonthlyPageCountResults(rs);
			
		} catch (SQLException sqle) {
			log.error("Error retrieving user page views, ", sqle);
			return new HashMap<>();
		}
		
	}
	
	/**
	 * Parses the monthly page count results.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected Map<String, Map<Integer,Integer>> parseMonthlyPageCountResults(ResultSet rs) 
			throws SQLException {
		String prevId = null;
		String currId;
		int prevMonthNo = -1;
		int currMonthNo;
		int monthPageCnt = 0;

		// Map of month number to pageviews
		Map<Integer,Integer> userMonths =  new HashMap<>();
		// Map of profileId to Map of Month, pageCount
		Map< String, Map<Integer,Integer> > userPageViewsByMonth = new HashMap<>();
		Calendar cal = Calendar.getInstance();

		while (rs.next()) {
			currId = rs.getString("profile_id");
			currMonthNo = parseMonthFromDate(cal, rs.getDate("visit_dt"));
			
			if (! currId.equalsIgnoreCase(prevId)) {

				// changed users or first time through.
				if (! userMonths.isEmpty()) {
					// put month count on map
					userMonths.put(prevMonthNo, monthPageCnt);
					// put list on map of user|user's months
					userPageViewsByMonth.put(prevId, userMonths);
				}

				// init userMonths map
				userMonths = new HashMap<>();
				
				// init current month's view count.
				monthPageCnt = 1;
				
			} else {
				// process record for current user
				if (currMonthNo != prevMonthNo) {
					// month changed, put previous month page count on map
					userMonths.put(prevMonthNo, monthPageCnt);
					
					// reset monthCnt to 1.
					monthPageCnt = 1;
					
				} else {
					// incr this month's count
					monthPageCnt++;
				}

			}
			// capture previous vals for comparison
			prevId = currId;
			prevMonthNo = currMonthNo;
		}

		// pick up the last record/user.
		userMonths.put(prevMonthNo, monthPageCnt);
		userPageViewsByMonth.put(prevId,userMonths);

		return userPageViewsByMonth;
	}
	
	/**
	 * Formats the given date into a start date.
	 * @param date
	 * @return
	 */
	protected Date checkStartDate(String date) {
		Date d;
		if (date == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -365);
			d = Convert.formatStartDate(cal.getTime());
		} else {
			d = Convert.formatStartDate(date);
		}
		return d;
	}

	/**
	 * Parses the int value of the month for the given date.
	 * @param cal
	 * @param date
	 * @return
	 */
	protected int parseMonthFromDate(Calendar cal, Date date) {
		cal.setTime(date);
		return cal.get(Calendar.MONTH);
	}
	
	/**
	 * Merges user page view history with accounts' user data.
	 * @param accounts
	 * @param userData
	 */
	protected void mergeData(Map<AccountVO,List<UserVO>> accounts, Map<String,Map<Integer,Integer>> userData) {
		for (Map.Entry<AccountVO, List<UserVO>> acct : accounts.entrySet()) {
			List<UserVO> acctUsers = acct.getValue();
			for (UserVO user : acctUsers) {
				// set this user's page count history as extended info.
				user.setUserExtendedInfo(userData.get(user.getProfileId()));
			}
		}
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