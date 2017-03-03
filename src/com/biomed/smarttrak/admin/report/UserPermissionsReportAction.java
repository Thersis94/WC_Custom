package com.biomed.smarttrak.admin.report;

// JDK 8
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

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserPermissionsReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 28, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserPermissionsReportAction extends SimpleActionAdapter {
		
	private static final String STATUS_EXCLUDE_INACTIVE = "I";
	
	/**
	* Constructor
	*/
	public UserPermissionsReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserPermissionsReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public List<AccountUsersVO> retrieveUserPermissions(ActionRequest req) throws ActionException {
		StringEncrypter se = initStringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		
		// 1. retrieve accounts/users
		List<AccountUsersVO> accounts = retrieveAccountsAndUsers(se);
		log.debug("accounts size: " + accounts.size());
		
		// 2. retrieve acct permissions
		retrieveAccountPermissions(req,accounts);
		
		return accounts;
	}
	
	/**
	 * Retrieves a List of AccountUsersVO comprised of an AccountVO and 
	 * a list of UserVO for that account.
	 * @param se
	 * @return
	 * @throws ActionException
	 */
	protected List<AccountUsersVO> retrieveAccountsAndUsers(StringEncrypter se) throws ActionException {
		List<AccountUsersVO> accounts;
		StringBuilder sql = buildAccountsAndUsersQuery();

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, STATUS_EXCLUDE_INACTIVE);
			ps.setString(2, STATUS_EXCLUDE_INACTIVE);

			ResultSet rs = ps.executeQuery();
			accounts = parseAccountsAndUsers(se,rs);

		} catch (SQLException sqle) {
			accounts = new ArrayList<>();
		}
		
		return accounts;
	}
	
	/**
	 * Parses the accounts and users query results into a List of AccountUsersVO.
	 * @param se
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<AccountUsersVO> parseAccountsAndUsers(StringEncrypter se, ResultSet rs) 
			throws SQLException {
		log.debug("parseAccountsAndUsers...");
		String prevAcctId = null;
		String currAcctId;
		AccountUsersVO acctUsersVO = null;
		List<AccountUsersVO> acctPerms = new ArrayList<>();
		while (rs.next()) {

			currAcctId = rs.getString("account_id");
			
			if (! currAcctId.equals(prevAcctId)) {
				
				if (prevAcctId != null) {
					acctPerms.add(acctUsersVO);
				}

				// create new acct vo.
				acctUsersVO = createAccount(rs);
				createAccountUser(se,rs,acctUsersVO);

			} else {
				// add this user to acctUsersVO
				createAccountUser(se,rs,acctUsersVO);
			}
			prevAcctId = currAcctId;
		}

		// catch the dangler
		if (prevAcctId != null) {
			acctPerms.add(acctUsersVO);
		}

		return acctPerms;
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
	 * Creates an AccountUsersVO from the result set record.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected AccountUsersVO createAccount(ResultSet rs) 
			throws SQLException {
		// init acctUsersVO, add acct vo and this user.
		AccountUsersVO acct = new AccountUsersVO();
		acct.setAccountId(rs.getString("account_id"));
		acct.setAccountName(rs.getString("account_nm"));
		// use Convert util in case of nulls.
		acct.setFdAuthFlg(Convert.formatInteger(rs.getInt("acct_fd_auth_flg")));
		acct.setGaAuthFlg(Convert.formatInteger(rs.getInt("acct_ga_auth_flg")));
		return acct;
	}
	
	/**
	 * Parses an account user from the result set record. 
	 * @param se
	 * @param rs
	 * @param acctUsersVO
	 */
	protected void createAccountUser(StringEncrypter se, 
			ResultSet rs, AccountUsersVO acctUsersVO) {
		try {
			UserVO user = new UserVO();
			user.setProfileId(rs.getString("profile_id"));
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
			user.setEmailAddress(se.decrypt(rs.getString("email_address_txt")));
			user.setUserId(rs.getString("user_id"));
			// use Convert util in case of nulls.
			user.setGaAuthFlg(Convert.formatInteger(rs.getInt("ga_auth_flg")));
			user.setFdAuthFlg(Convert.formatInteger(rs.getInt("fd_auth_flg")));
			acctUsersVO.addUser(user);
		} catch (Exception e) {
			log.error("Error parsing user from result set.");
		}
	}
	
	/**
	 * Formats the accounts/account user query.
	 * @return
	 */
	protected StringBuilder buildAccountsAndUsersQuery() {
		StringBuilder sql = new StringBuilder(350);
		sql.append("select ac.account_id, ac.account_nm, ac.fd_auth_flg as acct_fd_auth_flg, ");
		sql.append("ac.ga_auth_flg as acct_ga_auth_flg, ");
		sql.append("us.profile_id, us.user_id, us.fd_auth_flg, us.ga_auth_flg, ");
		sql.append("pf.first_nm, pf.last_nm, pf.email_address_txt ");
		sql.append("from custom.biomedgps_account ac ");
		sql.append("inner join custom.biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("and ac.status_no != ? and us.status_cd != ? ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("order by account_nm, profile_id");
		log.debug("accounts and users SQL: " + sql.toString());
		return sql;
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