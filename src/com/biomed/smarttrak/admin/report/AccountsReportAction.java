package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//WC custom
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.vo.UserVO.Status;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
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
	public List<AccountUsersVO> retrieveUserList(ActionRequest req) throws ActionException {

		StringEncrypter se = initStringEncrypter();
		
		SiteVO site = (SiteVO)req.getAttribute(Constants.USER_DATA);
		String siteId = StringUtil.isEmpty(site.getAliasPathParentId()) ? site.getSiteId() : site.getAliasPathParentId();
		
		// 1. retrieve account/users
		List<AccountUsersVO> accounts = retrieveAccountUsers(se,siteId);

		// 2. retrieve acct permissions
		retrieveAccountPermissions(req,accounts);

		return accounts;
	}

	/**
	 * Retrieves accounts and users.
	 * @param se
	 * @return
	 */
	protected List<AccountUsersVO> retrieveAccountUsers(StringEncrypter se, String siteId) {
		// 1. build query
		StringBuilder sql = buildAccountsUsersQuery();

		// 2. build PS
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			ps.setString(++idx, Status.INACTIVE.getCode());
			ps.setString(++idx, Status.INACTIVE.getCode());
			ps.setString(++idx, siteId);
			ps.setString(++idx, RegistrationMap.DIVISIONS.getFieldId());
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
	 * @return
	 */
	protected StringBuilder buildAccountsUsersQuery() {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select ac.account_id, ac.account_nm, ac.create_dt, ac.expiration_dt, ");
		sql.append("us.user_id, us.profile_id, us.status_cd, ");
		sql.append("pf.first_nm, pf.last_nm, pfr.role_id, ");
		sql.append("rd.register_field_id, rd.value_txt ");
		sql.append("from custom.biomedgps_account ac ");
		sql.append("inner join custom.biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("and ac.status_no != ? and us.status_cd != ? ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("inner join profile_role pfr on pf.profile_id = pfr.profile_id and pfr.site_id = ? ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("left join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("and rd.register_field_id = ? ");
		sql.append("order by ac.account_id, us.profile_id, rd.value_txt");
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
	protected List<AccountUsersVO> parseAccountUsers(StringEncrypter se, 
			ResultSet rs) throws SQLException {
		
		log.debug("parseAccountsUsers...");
		String prevAcctId = null;
		String prevDivId = null;
		String prevPid = null;
		String currAcctId;
		String currDivId;
		String currPid;

		UserVO user = new UserVO();
		List<UserVO> divUsers = new ArrayList<>();
		AccountUsersVO account = new AccountUsersVO();
		List<AccountUsersVO> accounts = new ArrayList<>();

		while (rs.next()) {

			currAcctId = rs.getString("account_id");
			currDivId = rs.getString("value_txt");
			currPid = rs.getString("profile_id");

			if (! currAcctId.equals(prevAcctId)) {
				// acct changed, capture 'previous' user and account
				if (prevAcctId != null) {
					// add user to division users list
					divUsers.add(user);
					// add division to account's division map
					account.addDivision(prevDivId, divUsers);
					// add acct to list.
					accounts.add(account);
				}

				// init the division users list
				divUsers = new ArrayList<>();
				
				// now create new account, and new user
				account = createBaseAccount(rs);
				user = createBaseUser(se,rs);
				// update user status count
				account.countUserStatus(user.getStatusCode());
				
			} else {
				// same account, check for user change
				if (! currPid.equals(prevPid)) {
					// user changed, add 'previous' user to division users list
					divUsers.add(user);

					// if division change, add division users to account.
					divUsers = processDivisionChange(account,divUsers,currDivId,prevDivId);

					// now create new user
					user = createBaseUser(se,rs);
					//update user status count
					account.countUserStatus(user.getStatusCode());

				}
			}

			// add registration data from row for this user.
			user.addAttribute(rs.getString("register_field_id"), rs.getString("value_txt"));
			
			prevAcctId = currAcctId;
			prevDivId = currDivId;
			prevPid = currPid;

		}

		// pick up the dangler
		if (prevAcctId != null) {
			if (prevPid != null) { 
				if (prevDivId != null) {
					divUsers.add(user);
					account.addDivision(prevDivId,divUsers);
					accounts.add(account);
				}
			}
		}

		return accounts;
	}
	
	/**
	 * Checks to see if the division changed.
	 * @param account
	 * @param divUsers
	 * @param currDivId
	 * @param prevDivId
	 * @return
	 */
	protected List<UserVO> processDivisionChange(AccountUsersVO account, 
			List<UserVO> divUsers, String currDivId, String prevDivId) {
		if (! currDivId.equals(prevDivId)) {
			account.addDivision(prevDivId, divUsers);
			return new ArrayList<>();
		}
		return divUsers;
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
		account.setCreateDate(rs.getDate("create_dt"));
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
	protected UserVO createBaseUser(StringEncrypter se, 	ResultSet rs) throws SQLException {
		UserVO user = new UserVO();

		// set unencrypted fields
		user.setAccountId(rs.getString("account_id"));
		user.setProfileId(rs.getString("profile_id"));
		user.setStatusCode(rs.getString("status_cd"));
		// decrypt encrypted fields and set.
		try {
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
		} catch (Exception e) {
			log.warn("Warning: Unable to decrypt profile fields for profile ID " + user.getProfileId());
		}
		
		return user;
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
