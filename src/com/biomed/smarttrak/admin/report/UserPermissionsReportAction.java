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
import com.biomed.smarttrak.vo.AccountVO;
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
	public List<AccountPermissionsVO> retrieveUserPermissions(ActionRequest req) throws ActionException {

		StringEncrypter se = initStringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		
		/* Report fields.
		 * account ID, account name, user ID, username (email addr), user full name, has fd, has ga
		 * section hierarchy.
		 */

		// 1. retrieve accounts/users
		List<AccountPermissionsVO> accounts = this.retrieveAccountsAndUsers(se);
		
		// 2. retrieve acct permissions
		retrieveAccountPermissions(req,accounts);
		
		return accounts;
		
	}
	
	/**
	 * Retrieves a List of AccountPermissionsVO comprised of an AccountVO and 
	 * a list of UserVO for that account.
	 * @param se
	 * @return
	 * @throws ActionException
	 */
	protected List<AccountPermissionsVO> retrieveAccountsAndUsers(StringEncrypter se) throws ActionException {
		List<AccountPermissionsVO> accounts;
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
	
	protected List<AccountPermissionsVO> parseAccountsAndUsers(StringEncrypter se, ResultSet rs) 
			throws SQLException {
		String prevAcctId = null;
		String currAcctId;
		AccountPermissionsVO acctPermVO = null;
		List<AccountPermissionsVO> acctPerms = new ArrayList<>();
		while (rs.next()) {

			currAcctId = rs.getString("account_id");
			
			if (! currAcctId.equals(prevAcctId)) {
				
				if (prevAcctId != null) {
					acctPerms.add(acctPermVO);
				}

				// create new acct vo.
				AccountVO account = new AccountVO();
				account.setAccountId(rs.getString("account_id"));
				account.setAccountName(rs.getString("account_nm"));

				// init acctPermVO, add acct vo and this user.
				acctPermVO = new AccountPermissionsVO();
				acctPermVO.setAccount(account);
				parseAccountUser(se,rs,acctPermVO);

			} else {
				// add this user to acctPermVO
				parseAccountUser(se,rs,acctPermVO);
			}
			prevAcctId = currAcctId;
		}

		// catch the dangler
		if (prevAcctId != null) {
			acctPerms.add(acctPermVO);
		}

		return acctPerms;
	}
	
	protected void retrieveAccountPermissions(ActionRequest req, 
			List<AccountPermissionsVO> accounts) {
		log.debug("retrieveAccountPermissions...");
		long start = Calendar.getInstance().getTimeInMillis();
		AccountPermissionAction apa;
		ModuleVO mod;
		for (AccountPermissionsVO acctPerms : accounts) {
			apa = new AccountPermissionAction();
			apa.setDBConnection(dbConn);
			apa.setAttributes(attributes);
			// set the curr acct ID on the req
			req.setParameter(AccountAction.ACCOUNT_ID, acctPerms.getAccount().getAccountId());
			// retrieve stuff
			try {
				apa.retrieve(req);
			} catch (Exception e) {
				log.error("Could not retrieve account permissions for account ID: " + acctPerms.getAccount().getAccountId());
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
		log.debug("retrieveAccountPermissions end (seconds): " + (start - Calendar.getInstance().getTimeInMillis()));
	}
	
	/**
	 * Parses an account user from the result set record. 
	 * @param se
	 * @param rs
	 * @param acctPermVO
	 */
	protected void parseAccountUser(StringEncrypter se, 
			ResultSet rs, AccountPermissionsVO acctPermVO) {
		try {
			UserVO user = new UserVO();
			user.setProfileId(rs.getString("profile_id"));
			user.setFirstName(se.decrypt(rs.getString("first_nm")));
			user.setLastName(se.decrypt(rs.getString("last_nm")));
			user.setEmailAddress(se.decrypt(rs.getString("email_address_txt")));
			user.setUserId(rs.getString("user_id"));
			user.setGaAuthFlg(Convert.formatInteger(rs.getInt("ga_auth_flg")));
			user.setFdAuthFlg(Convert.formatInteger(rs.getInt("fd_auth_flg")));
			acctPermVO.addUser(user);
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
		sql.append("select ac.account_id, ac.account_nm, ");
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