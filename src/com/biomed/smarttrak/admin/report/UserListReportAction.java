package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import com.siliconmtn.util.user.LastNameComparator;
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
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		// 1. retrieve account/users
		List<AccountUsersVO> accounts = retrieveAccountUsers(se,schema);

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String siteId = StringUtil.isEmpty(site.getAliasPathParentId()) ? site.getSiteId() : site.getAliasPathParentId();

		// 2. retrieve login attributes
		Map<String,Map<String,Object>> authAttributes = retrieveAuthAttributes(schema, siteId);

		// 3. Merge data and return.
		mergeData(accounts, authAttributes);

		return accounts;
	}

	/**
	 * Retrieves accounts and users.
	 * @param se
	 * @return
	 */
	protected List<AccountUsersVO> retrieveAccountUsers(StringEncrypter se,String schema) {
		// 1. build query
		StringBuilder sql = buildAccountsUsersQuery(schema);

		// 2. build PS
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String [] params = getRetrieveAccountUsersParams();
			int i = 1;
			for(String p : params) {
				ps.setString(i++, p);
			}
			ResultSet rs = ps.executeQuery();

			return parseAccountUsers(se,rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving accounts and users, ",sqle);
			return new ArrayList<>();
		}
	}

	/**
	 * Build Params list for the RetrieveAccountUsers Query.
	 * @return
	 */
	protected String[] getRetrieveAccountUsersParams() {
		return new String [0];
	}

	/**
	 * Retrieves accounts and users.
	 * @param req
	 * @param accounts
	 * @return
	 */
	protected Map<String,Map<String,Object>> retrieveAuthAttributes(String schema, String siteId) {
		log.debug("retrieveAuthAttributes...");
		// 1. build query
		StringBuilder sql = buildLastLoginQuery(schema);

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
	 * Merges last login data with account users.
	 * @param accounts
	 * @param lastLogins
	 */
	protected void mergeData(List<AccountUsersVO> accounts, Map<String,Map<String,Object>> authAttribs) {
		log.debug("mergeData...");
		Map<String,Object> userAttribs;
		for (AccountUsersVO account : accounts) {

			List<UserVO> users = account.getUsers();
			for (UserVO user : users) {

				// now add auth attributes if they exist.
				userAttribs = authAttribs.get(user.getAuthenticationId());
				if (userAttribs == null || userAttribs.isEmpty()) continue;
				setUserExtendedData(user, userAttribs);
			}
		}
	}
	
	/**
	 * Helper method that sets the user's extended data appropriately
	 * @param user
	 * @param userAttribs
	 */
	protected void setUserExtendedData(UserVO user, Map<String,Object> userAttribs) {
		for (Map.Entry<String,Object> loginAttrib : userAttribs.entrySet()) {
			if(UserListReportVO.LAST_LOGIN_DT.equals(loginAttrib.getKey())) {
				user.setLoginDate((Date)loginAttrib.getValue());
			}else {
				user.addAttribute(loginAttrib.getKey(), loginAttrib.getValue());
			}
		}
	}

	/**
	 * Builds the base accounts/users query.
	 * @return
	 */
	protected StringBuilder buildAccountsUsersQuery(String schema) {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select ac.account_id, ac.account_nm, ac.expiration_dt as acct_expiration_dt, ac.status_no, ");
		sql.append("ac.start_dt as acct_start_dt, ac.classification_id, ac.type_id, us.acct_owner_flg, ");
		sql.append("us.status_cd, us.expiration_dt, us.fd_auth_flg, us.create_dt, us.active_flg as active_flg, ");
		sql.append("pf.profile_id, pf.authentication_id, pf.first_nm, pf.last_nm, pf.email_address_txt, ");
		sql.append("pfa.address_txt, pfa.address2_txt, pfa.city_nm, pfa.state_cd, pfa.zip_cd, pfa.country_cd, ");
		sql.append("ph.phone_number_txt, ph.phone_type_cd, us.create_dt as user_create_dt, us.entry_source, ");
		sql.append("rd.register_field_id, rd.value_txt, us.user_id, rfo.option_desc ");
		sql.append("from ").append(schema).append("biomedgps_account ac ");
		sql.append("inner join ").append(schema).append("biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("left join profile_address pfa on pf.profile_id = pfa.profile_id ");
		sql.append("left join phone_number ph on pf.profile_id = ph.profile_id ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("left join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("left join register_field_option rfo on rd.register_field_id = rfo.register_field_id ");  
		sql.append("and rd.value_txt = rfo.option_value_txt ");
		sql.append("order by ac.account_id, us.user_id, us.profile_id, phone_type_cd ");
		log.debug("user retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Builds the last logins query.
	 * @return
	 */
	protected StringBuilder buildLastLoginQuery(String schema) {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select authentication_id, oper_sys_txt, browser_txt, device_txt, login_dt from ( ");
		sql.append("select distinct(al.authentication_id), oper_sys_txt, browser_txt, device_txt, login_dt, ");
		sql.append("rank() over ( partition by al.authentication_id order by login_dt desc ) ");
		sql.append("from ").append(schema).append("biomedgps_user us ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("inner join authentication_log al on pf.authentication_id = al.authentication_id ");
		sql.append("where site_id = ? ) rank_filter where rank = 1 order by authentication_id ");
		log.debug("last login retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Parses the accounts & users query results.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<AccountUsersVO> parseAccountUsers(StringEncrypter se, ResultSet rs) throws SQLException {
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
					//sort list of users
					List<UserVO> users = account.getUsers();
					Collections.sort(users, new LastNameComparator());
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
			}

			// add registration record for the current user.
			if(rs.getString("option_desc") != null) {
				addAttribute(user, rs.getString("register_field_id"), rs.getString("option_desc"));	
			}else {
				addAttribute(user, rs.getString("register_field_id"), rs.getString("value_txt"));
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
	 * Add the supplied value to the user without overwriting multivalued items.
	 * @param user
	 * @param key
	 * @param val
	 */
	private void addAttribute(UserVO user, String key, String val) {
		if (key == null) return;
		if (user.getAttributes().containsKey(key)) {
			if (!StringUtil.checkVal(user.getAttributes().get(key)).contains(val))
				user.addAttribute(key, user.getAttributes().get(key) + ", " + val);
		} else {
			user.addAttribute(key, val);
		}
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
		account.setStartDate(rs.getDate("acct_start_dt"));
		account.setExpirationDate(rs.getDate("acct_expiration_dt"));
		account.setStatusNo(rs.getString("status_no"));
		account.setTypeId(rs.getString("type_id"));
		account.setClassificationId(rs.getInt("classification_id"));
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
		user.setUserId(rs.getString("user_id"));
		user.setAccountId(rs.getString("account_id"));
		user.setProfileId(rs.getString("profile_id"));
		user.setAuthenticationId(rs.getString("authentication_id"));
		user.setLicenseType(rs.getString("status_cd"));
		user.setFdAuthFlg(Convert.formatInteger(rs.getInt("fd_auth_flg")));
		user.setExpirationDate(rs.getDate("expiration_dt"));
		user.setCreateDate(rs.getDate("create_dt"));
		user.setAddress2(rs.getString("address2_txt"));
		user.setCity(rs.getString("city_nm"));
		user.setState(rs.getString("state_cd"));
		user.setZipCode(rs.getString("zip_cd"));
		user.setCountryCode(rs.getString("country_cd"));
		user.setStatusFlg(rs.getInt("active_flg"));
		user.setAcctOwnerFlg(rs.getInt("acct_owner_flg"));
		user.setCreateDate(rs.getDate("user_create_dt"));
		user.setEntrySource(rs.getString("entry_source"));
		
		// decrypt encrypted fields and set.
		try {
			//name & email
			user.setFirstName(decrypt(se, rs.getString("first_nm")));
			user.setLastName(decrypt(se, rs.getString("last_nm")));
			user.setEmailAddress(decrypt(se, rs.getString("email_address_txt")));
			
			// address txt
			user.setAddress(decrypt(se, rs.getString("address_txt")));
			
			//main or mobile phone
			if (PhoneVO.HOME_PHONE.equals(rs.getString("phone_type_cd"))) {
				user.setMainPhone(decrypt(se, rs.getString("phone_number_txt")));
			} else {
				user.setMobilePhone(decrypt(se, rs.getString("phone_number_txt")));
			}
		} catch (Exception e) {
			log.warn("Warning: Unable to decrypt profile fields for profile ID " + user.getProfileId(), e);
		}
		return user;
	}

	/**
	 * @param se
	 * @param string
	 * @return
	 */
	private String decrypt(StringEncrypter se, String string) {
		if (StringUtil.isEmpty(string)) return null;
		try {
			return se.decrypt(string);
		} catch (Exception e) {
			return string;
		}
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
