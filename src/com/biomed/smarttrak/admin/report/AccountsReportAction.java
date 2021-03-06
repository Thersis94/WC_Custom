package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//WC custom
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AccountVO.Type;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.vo.UserVO.Status;
import com.biomed.smarttrak.vo.UserVO.LicenseType;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.LastNameComparator;
// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountsReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 6, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountsReportAction extends SimpleActionAdapter {
	
	/**
	* Constructor
	*/
	public AccountsReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public AccountsReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public Map<String,Object> retrieveAccountsList(ActionRequest req) throws ActionException {

		// 1. init the StringEncrypter or die trying.
		StringEncrypter se = initStringEncrypter();
		
		// 2. retrieve the user registration field IDs that we need to use in the accounts/users query
		List<String> regFields = initUserRegistrationFields();
		
		// 4. retrieve account/users
		List<AccountUsersVO> accounts = retrieveAccountUsers(se,regFields);

		// 5. retrieve acct permissions for each account
		retrieveAccountPermissions(req,accounts);
		
		// 6. retrieve registration field options map
		Map<String,Map<String,String>> optionMap = retrieveRegistrationFieldOptions(regFields);

		Map<String,Object> reportData = new HashMap<>();
		reportData.put(AccountReportVO.KEY_ACCOUNTS, accounts);
		reportData.put(AccountReportVO.KEY_FIELD_OPTIONS, optionMap);
		
		return reportData;
	}

	/**
	 * Retrieves accounts and users.
	 * @param se
	 * @param siteId
	 * @return
	 */
	protected List<AccountUsersVO> retrieveAccountUsers(StringEncrypter se, List<String> regFields) {
		// 1. build query
		StringBuilder sql = buildAccountsUsersQuery(regFields);

		// 2. build PS
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			ps.setString(++idx, Type.FULL.getId());
			ps.setString(++idx, LicenseType.INACTIVE.getCode());
			ps.setString(++idx, LicenseType.INACTIVE.getCode());
			ps.setString(++idx, LicenseType.COMPUPDATES.getCode());
			ps.setInt(++idx, Status.INACTIVE.getCode());
			for (int x = 0; x < regFields.size(); x++) {
				ps.setString(++idx, regFields.get(x));
			}
			ResultSet rs = ps.executeQuery();
			return parseAccountUsers(se,rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving accounts and users, ",sqle);
			return new ArrayList<>();
		}
	}
	
	/**
	 * Calls AccountPermissionAction to retrieve accounts permissions.
	 * @param req
	 * @param accounts
	 */
	protected void retrieveAccountPermissions(ActionRequest req, 
			List<AccountUsersVO> accounts) {
		log.debug("retrieveAccountPermissions...");
		long start = Calendar.getInstance().getTimeInMillis();
		AccountPermissionAction apa;
		ModuleVO mod;
		for (AccountUsersVO acctPerms : accounts) {
			apa = new AccountPermissionAction();
			apa.setDBConnection(dbConn);
			apa.setAttributes(attributes);
			// set the curr acct ID on the req
			req.setParameter(AccountAction.ACCOUNT_ID, acctPerms.getAccountId());
			// retrieve stuff
			try {
				apa.retrieve(req);
			} catch (Exception e) {
				log.error("Could not retrieve account permissions for account ID: " + acctPerms.getAccountId());
			}
			// try to get to it
			mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
			if (mod != null) {
				SmarttrakTree t = (SmarttrakTree)mod.getActionData();
				acctPerms.setPermissions(t);
			}
			// reset the req param.
			req.setParameter(AccountAction.ACCOUNT_ID, null);
		}
		log.debug("retrieveAccountPermissions finished in: " + (Calendar.getInstance().getTimeInMillis() - start) + "ms");
	}

	/**
	 * Builds the base accounts/users query.
	 * @param userRegFields
	 * @return
	 */
	protected StringBuilder buildAccountsUsersQuery(List<String> userRegFields) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(650);
		sql.append("select ac.account_id, ac.account_nm, ac.start_dt, ac.expiration_dt, ac.status_no, ac.classification_id, ac.type_id, ");
		sql.append("us.user_id, us.profile_id, us.status_cd, us.active_flg, us.acct_owner_flg, ");
		sql.append("pf.first_nm, pf.last_nm, pfa.country_cd, ");
		sql.append("rd.register_field_id, rd.value_txt, rfo.option_desc ");
		sql.append("from ").append(schema).append("biomedgps_account ac ");
		sql.append("inner join ").append(schema).append("biomedgps_user us ");
		sql.append("on ac.account_id = us.account_id ");
		sql.append("and ac.type_id = ? and ac.status_no != ? and us.status_cd != ? and us.status_cd != ? and us.active_flg != ? ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("left join profile_address pfa on pf.profile_id = pfa.profile_id ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("left join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("and rd.register_field_id in (");
		for (int x=0; x < userRegFields.size(); x++) {
			if (x > 0) sql.append(",");
			sql.append("?");
		}
		sql.append(") ");
		sql.append("left join register_field_option rfo on rd.value_txt = rfo.option_value_txt  and rfo.register_field_id = rd.register_field_id ");
		sql.append("order by ac.account_nm, us.profile_id, rd.register_field_id");
		log.debug("accounts users retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Parses the accounts & users query results.
	 * @param se
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<AccountUsersVO> parseAccountUsers(StringEncrypter se, ResultSet rs) throws SQLException {
		log.debug("parseAccountsUsers...");
		String prevAcctId = null;
		String prevPid = null;
		String currAcctId;
		String currPid;

		UserVO user = new UserVO();
		AccountUsersVO account = new AccountUsersVO();
		List<AccountUsersVO> accounts = new ArrayList<>();
		while (rs.next()) {
			currAcctId = rs.getString("account_id");
			currPid = rs.getString("profile_id");

			if (! currAcctId.equals(prevAcctId)) {
				// acct changed
				if (prevAcctId != null) {
					// add acct to accounts list.
					accounts.add(account);
				}
				checkDivisions(user, account);

				// create new account
				account = createBaseAccount(rs);
				
				// create new user
				user = createBaseUser(se,rs, account);

			} else {
				// same account, check for user change
				if (! currPid.equals(prevPid)) {
					checkDivisions(user, account);
					// user changed, create new user
					user = createBaseUser(se,rs, account);
				}
			}

			// add registration data from row for this user.
			processUserRegistrationField(account, user,rs.getString("register_field_id"),rs.getString("value_txt"), rs.getString("option_desc"));

			prevAcctId = currAcctId;
			prevPid = currPid;

		}

		// pick up the dangler
		if (prevAcctId != null) {
			checkDivisions(user, account);
			accounts.add(account);
		}
		
		for (AccountUsersVO acct : accounts) {
			for (Entry<String, List<UserVO>> division  : acct.getDivisions().entrySet())
				Collections.sort(division.getValue(), new LastNameComparator());
		}

		return accounts;
	}
	
	/**
	 * Check to see if the user has any divisions.
	 * If not add the No Divisions division
	 * @param user
	 * @param account
	 */
	private void checkDivisions(UserVO user, AccountUsersVO account) {
		if (!user.getAttributes().keySet().contains(RegistrationMap.DIVISIONS.getFieldId()))
			addUserToAccountDivisions(account.getDivisions(), user, "No Division");
	}

	/**
	 * Processes the user's registration field for the given record.
	 * @param acct
	 * @param user
	 * @param currFieldId
	 * @param currFieldVal
	 */
	@SuppressWarnings("unchecked")
	protected void processUserRegistrationField(AccountUsersVO acct, UserVO user, 
			String currFieldId, String currFieldVal, String fieldName) {
		if (StringUtil.isEmpty(currFieldId) || StringUtil.isEmpty(currFieldVal)) return;
		// process reg field value
		if (RegistrationMap.DIVISIONS.getFieldId().equals(currFieldId)) {
			// user can belong to more than one division, we use a List here.
			List<String> divs;
			if (user.getAttribute(currFieldVal) == null) {
				divs = new ArrayList<>();
			} else {
				divs = (List<String>)user.getAttribute(currFieldId);
			}
			// add value to list, set/replace on user attrib map.
			divs.add(currFieldVal);
			user.addAttribute(currFieldId, divs);

			// add to the account's division map
			addUserToAccountDivisions(acct.getDivisions(),user, fieldName);

		} else {
			user.addAttribute(currFieldId, currFieldVal);
		}
	}
	
	/**
	 * Adds the user to the List of UserVO for the given division.
	 * @param divs
	 * @param user
	 * @param currDivVal
	 */
	protected void addUserToAccountDivisions(Map<String,List<UserVO>> divs, 
			UserVO user, String currDivVal) {
		if (currDivVal == null) currDivVal = "No Division";
		// if the divs map doesn't have a map entry for the currDivVal, create one
		divs.computeIfAbsent(currDivVal, k -> new ArrayList<>());
		divs.get(currDivVal).add(user);
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
		account.setStartDate(rs.getDate("start_dt"));
		account.setExpirationDate(rs.getDate("expiration_dt"));
		account.setStatusNo(rs.getString("status_no"));
		account.setClassificationId(rs.getInt("classification_id"));
		account.setTypeId(rs.getString("type_id"));
		return account;
	}
	
	/**
	 * Creates base UserVO
	 * @param se
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected UserVO createBaseUser(StringEncrypter se, ResultSet rs, AccountUsersVO acct) throws SQLException {
		UserVO user = new UserVO();

		// set unencrypted fields
		user.setAccountId(rs.getString("account_id"));
		user.setProfileId(rs.getString("profile_id"));
		user.setLicenseType(rs.getString("status_cd"));
		user.setStatusFlg(rs.getInt("active_flg"));
		user.setAcctOwnerFlg(Convert.formatInteger(rs.getString("acct_owner_flg")));
		user.setCountryCode(rs.getString("country_cd"));
		// decrypt encrypted fields and set.
		try {
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
		} catch (Exception e) {
			log.warn("Warning: Unable to decrypt profile fields for profile ID " + user.getProfileId());
		}
		
		acct.countLicenseType(user);
		
		return user;
	}
	
	/**
	 * Retrieves a Map of registration field ID mapped to the corresponding options for that ID for each
	 * registration field used by this report.
	 * @param regFields
	 * @return
	 */
	protected Map<String,Map<String,String>> retrieveRegistrationFieldOptions(List<String> regFields) {
		StringBuilder sql = buildRegistrationFieldOptionQuery(regFields);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			int idx = 0;
			for (int x = 0; x < regFields.size(); x++) {
				ps.setString(++idx, regFields.get(x));
			}

			ResultSet rs = ps.executeQuery();
			return parseRegistrationFieldOptions(rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving registration field options, ", sqle);
			return new HashMap<>();
		}
	}

	/**
	 * Parses the Registration field options results set into a Map of registration field ID mapped
	 * to a map of option key/value pairs.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected Map<String,Map<String,String>> parseRegistrationFieldOptions(ResultSet rs) 
			throws SQLException {
		Map<String,String> fieldOptionsMap = new HashMap<>();
		Map<String,Map<String,String>> optionsMap = new HashMap<>();
		String prevFieldId = null;
		String currFieldId;

		while (rs.next()) {
			currFieldId = rs.getString("register_field_id");
			
			if (! currFieldId.equals(prevFieldId)) {
				// field change, add 'previous' field option map to overall map
				if (prevFieldId != null) {
					optionsMap.put(prevFieldId, fieldOptionsMap);
				}

				// init new field map and add current option to it.
				fieldOptionsMap = new HashMap<>();
				fieldOptionsMap.put(rs.getString("option_value_txt"), rs.getString("option_desc"));

			} else {
				// same field, different option.
				fieldOptionsMap.put(rs.getString("option_value_txt"), rs.getString("option_desc"));
			}
			prevFieldId = currFieldId;
		}
		
		// pick up the dangler.
		if (prevFieldId != null) {
			optionsMap.put(prevFieldId, fieldOptionsMap);
		}
		
		// Add the empty division option.
		optionsMap.get(RegistrationMap.DIVISIONS.getFieldId()).put("", "No Division");
		
		return optionsMap;
	}
	
	/**
	 * Retrieves a Map of registration field option key/value pairs for the registration
	 * fields used for this report.
	 * @param regFields
	 * @return
	 */
	protected StringBuilder buildRegistrationFieldOptionQuery(List<String> regFields) {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select register_field_id, option_desc, option_value_txt ");
		sql.append("from register_field_option where register_field_id in (");
		for (int x = 0; x < regFields.size(); x++) {
			if (x > 0) sql.append(",");
			sql.append("?");
		}
		sql.append(") order by register_field_id, order_no");
		return sql;
	}
	
	
	/**
	 * Returns a List of String containing the registration field IDs that are used by 
	 * the query that retrieves accounts/users data.
	 * @return
	 */
	protected List<String> initUserRegistrationFields() {
		List<String> regFields = new ArrayList<>();
		regFields.add(RegistrationMap.DIVISIONS.getFieldId());
		regFields.add(RegistrationMap.JOBCATEGORY.getFieldId());
		regFields.add(RegistrationMap.JOBLEVEL.getFieldId());
		return regFields;
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
