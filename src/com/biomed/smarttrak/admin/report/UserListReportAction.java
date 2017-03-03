package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.UserVO;

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

/*****************************************************************************
 <p><b>Title</b>: UserListReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Mar 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserListReportAction extends SimpleActionAdapter {

	/**
	* Constructor
	*/
	public UserListReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserListReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public List<AccountUsersVO> retrieveUserList(ActionRequest req) throws ActionException {

		StringEncrypter se = initStringEncrypter();

		// 1. retrieve account/users
		List<AccountUsersVO> accounts = retrieveAccountUsers(se);

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String siteId = StringUtil.isEmpty(site.getAliasPathParentId()) ? site.getSiteId() : site.getAliasPathParentId();
		
		// 2. retrieve login attributes
		Map<String,Map<String,Object>> authAttributes = retrieveAuthAttributes(accounts, siteId);

		// 3. retrieve pageviews counts
		Map<String,Integer> userPageCounts = retrieveUserPageCounts(siteId);
		
		// 4. Merge data and return.
		mergeData(accounts, authAttributes, userPageCounts);

		return accounts;
	}

	/**
	 * Retrieves accounts and users.
	 * @param se
	 * @return
	 */
	protected List<AccountUsersVO> retrieveAccountUsers(StringEncrypter se) {
		// 1. build query
		StringBuilder sql = buildAccountsUsersQuery();

		// 2. build PS
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();

			return parseAccountUsers(se,rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving accounts and users, ",sqle);
			return new ArrayList<>();
		}
	}
	
	/**
	 * Retrieves accounts and users.
	 * @param req
	 * @param accounts
	 * @return
	 */
	protected Map<String,Map<String,Object>> retrieveAuthAttributes(List<AccountUsersVO> accounts, String siteId) {
		log.debug("retrieveAuthAttributes...");
		// 1. build query
		StringBuilder sql = buildLastLoginQuery();

		// 2. build PS
		Map<String,Object> userAttribs;
		Map<String,Map<String,Object>> attribMap = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, siteId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				userAttribs = new HashMap<>();
				userAttribs.put(UserListReportVO.LAST_LOGIN_DT, rs.getDate("login_dt"));
				userAttribs.put(UserListReportVO.OS,rs.getString("oper_sys_txt"));
				userAttribs.put(UserListReportVO.BROWSER,rs.getString("browser_txt"));
				userAttribs.put(UserListReportVO.DEVICE_TYPE,rs.getString("device_txt"));
				attribMap.put(rs.getString("authentication_id"), userAttribs);
			}

		} catch (SQLException sqle) {
			log.error("Error retrieving user authentication log attributes, ",sqle);
		}
		
		return attribMap;
	}

	/**
	 * Retrieves a users cumulative total of page views.
	 * @param siteId
	 * @return
	 */
	protected Map<String,Integer> retrieveUserPageCounts(String siteId) {
		log.debug("retrieveUserPageCounts...");
		StringBuilder sql = buildUserPageCountsQuery();
		Map<String,Integer> pageCounts = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, siteId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				pageCounts.put(rs.getString("profile_id"),Convert.formatInteger(rs.getInt("page_count")));
			}

		} catch (SQLException sqle) {
			log.error("Error retrieving user page counts, ", sqle);
		}

		return pageCounts;
	}

	/**
	 * Merges last login data with account users.
	 * @param accounts
	 * @param lastLogins
	 */
	protected void mergeData(List<AccountUsersVO> accounts, 
			Map<String,Map<String,Object>> authAttribs, Map<String,Integer> userPageCounts) {
		log.debug("mergeData...");
		Map<String,Object> userAttribs;
		for (AccountUsersVO account : accounts) {

			List<UserVO> users = account.getUsers();
			for (UserVO user : users) {
				// add page counts first
				user.addAttribute(UserListReportVO.PAGEVIEWS, userPageCounts.get(user.getProfileId()));

				// now add auth attributes if they exist.
				userAttribs = authAttribs.get(user.getAuthenticationId());
				if (userAttribs == null || userAttribs.isEmpty()) continue;
				for (Map.Entry<String,Object> loginAttrib : userAttribs.entrySet()) {
					user.addAttribute(loginAttrib.getKey(), loginAttrib.getValue());
				}
			}
		}
	}

	/**
	 * Builds the base accounts/users query.
	 * @return
	 */
	protected StringBuilder buildAccountsUsersQuery() {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select ac.account_id, ac.account_nm, ac.expiration_dt as acct_expiration_dt, ac.status_no, ");
		sql.append("us.status_cd, us.expiration_dt, us.fd_auth_flg, us.create_dt, ");
		sql.append("pf.profile_id, pf.authentication_id, pf.first_nm, pf.last_nm, pf.email_address_txt, ");
		sql.append("pfa.address_txt, pfa.address2_txt, pfa.city_nm, pfa.state_cd, pfa.zip_cd, pfa.country_cd, ");
		sql.append("ph.phone_number_txt, ph.phone_type_cd, ");
		sql.append("rd.register_field_id, rd.value_txt ");
		sql.append("from custom.biomedgps_account ac ");
		sql.append("inner join custom.biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("left join profile_address pfa on pf.profile_id = pfa.profile_id ");
		sql.append("left join phone_number ph on pf.profile_id = ph.profile_id ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("inner join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("order by ac.account_nm, us.profile_id, phone_type_cd");
		log.debug("user retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Builds the last logins query.
	 * @return
	 */
	protected StringBuilder buildLastLoginQuery() {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select authentication_id, oper_sys_txt, browser_txt, device_txt, login_dt from ( ");
		sql.append("select distinct(al.authentication_id), oper_sys_txt, browser_txt, device_txt, login_dt, ");
		sql.append("rank() over ( partition by al.authentication_id order by login_dt desc ) ");
		sql.append("from custom.biomedgps_user us ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("inner join authentication_log al on pf.authentication_id = al.authentication_id ");
		sql.append("where site_id = ? ) rank_filter where rank = 1 order by authentication_id ");
		log.debug("last login retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Builds the user page count query.
	 * @return
	 */
	protected StringBuilder buildUserPageCountsQuery() {
		StringBuilder sql = new StringBuilder(115);
		sql.append("select profile_id, count(profile_id) as page_count ");
		sql.append("from pageview_user where site_id = ? ");
		sql.append("group by profile_id");
		log.debug("page count retrieval: " + sql.toString());
		return sql;
	}

	/**
	 * Parses the accounts & users query results.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<AccountUsersVO> parseAccountUsers(StringEncrypter se, 
			ResultSet rs) throws SQLException {
		
		log.debug("parseAccountsUsers...");
		String prevAcctId = null;
		String prevPid = null;
		String prevPhoneCd =  null;
		String currAcctId;
		String currPid;
		String currPhoneCd;

		UserVO user = new UserVO();
		AccountUsersVO account = new AccountUsersVO();
		List<AccountUsersVO> accounts = new ArrayList<>();

		while (rs.next()) {

			currAcctId = rs.getString("account_id");
			currPid = rs.getString("profile_id");
			currPhoneCd = StringUtil.checkVal(rs.getString("phone_type_cd"));

			if (! currAcctId.equals(prevAcctId)) {
				// acct changed, capture 'previous' user and account
				if (prevAcctId != null) {
					account.addUser(user);
					accounts.add(account);
				}

				// now create new account, and new user
				account = createBaseAccount(rs);
				user = createBaseUser(se,rs);

			} else {
				// same account, check for user change
				if (! currPid.equals(prevPid)) {
					// user changed, capture 'previous' user
					account.addUser(user);

					// now create new user
					user = createBaseUser(se,rs);

				} else {
					// same user, check to see if phone type changed.
					checkUserPhoneType(se,user,rs.getString("phone_number_txt"),prevPhoneCd,currPhoneCd);

				}	

				// add registration record
				user.addAttribute(rs.getString("register_field_id"), rs.getString("value_txt"));

			}

			prevAcctId = currAcctId;
			prevPid = currPid;
			prevPhoneCd = currPhoneCd;
		}

		// pick up the dangler
		if (prevAcctId != null) {
			if (prevPid != null) { 
				account.addUser(user);
			}
			accounts.add(account);
		}

		return accounts;
	}

	/**
	 * Creates base AccountUsersVO.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected AccountUsersVO createBaseAccount(ResultSet rs) throws SQLException {
		AccountUsersVO account = new AccountUsersVO();
		account.setAccountId(rs.getString("account_id"));
		account.setAccountName(rs.getString("account_nm"));
		account.setExpirationDate(rs.getDate("acct_expiration_dt"));
		account.setStatusNo(rs.getString("status_no"));
		return account;
	}
	
	/**
	 * Creates base UserVO
	 * @param se
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected UserVO createBaseUser(StringEncrypter se, ResultSet rs) throws SQLException {
		UserVO user = new UserVO();
		// set unencrypted fields
		user.setAccountId(rs.getString("account_id"));
		user.setProfileId(rs.getString("profile_id"));
		user.setAuthenticationId(rs.getString("authentication_id"));
		user.setStatusCode(rs.getString("status_cd"));
		user.setFdAuthFlg(Convert.formatInteger(rs.getInt("fd_auth_flg")));
		user.setExpirationDate(rs.getDate("expiration_dt"));
		user.setCreateDate(rs.getDate("create_dt"));
		user.setAddress2(rs.getString("address2_txt"));
		user.setCity(rs.getString("city_nm"));
		user.setState(rs.getString("state_cd"));
		user.setZipCode(rs.getString("zip_cd"));
		user.setCountryCode(rs.getString("country_cd"));
		
		// decrypt encrypted fields and set.
		try {
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
			user.setEmailAddress(se.decrypt(rs.getString("email_address_txt")));
			// check address txt, decrypt if populated.
			String tmp = StringUtil.checkVal(rs.getString("address_txt"));
			if (! tmp.isEmpty()) user.setAddress(se.decrypt(tmp));
			tmp = StringUtil.checkVal(se.decrypt(rs.getString("phone_number_txt")));
			if (! tmp.isEmpty()) {
				if (PhoneVO.HOME_PHONE.equals(rs.getString("phone_type_cd"))) {
					user.setMainPhone(tmp);
				} else {
					user.setMobilePhone(tmp);
				}
			}
		} catch (Exception e) {
			log.warn("Warning: Unable to decrypt profile fields for profile ID " + user.getProfileId());
		}
		return user;
	}
	
	/**
	 * Checks for a phone type code change and adds phone to UserVO if necessary.
	 * @param se
	 * @param user
	 * @param phoneText
	 * @param prevPhoneCd
	 * @param currPhoneCd
	 */
	protected void checkUserPhoneType(StringEncrypter se, UserVO user, 
			String phoneText, String prevPhoneCd, String currPhoneCd) {
		if (currPhoneCd.equals(prevPhoneCd) || currPhoneCd.isEmpty()) return;
		try {
			String tmp = se.decrypt(phoneText);
			if (PhoneVO.HOME_PHONE.equals(currPhoneCd)) {
				user.setMainPhone(tmp);
			} else {
				user.setMobilePhone(tmp);
			}
		} catch (Exception e) {
			log.warn("Warning: Unable to decrypt user phone type.");
		}
	}
	
	/**
	 * Instantiates a StringEncrypter.
	 * @return
	 * @throws ActionException
	 */
	protected StringEncrypter initStringEncrypter() throws ActionException {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (Exception e) {
			throw new ActionException("Error instantiating StringEncrypter: " + e.getMessage());
		}
		return se;
	}
	
}
