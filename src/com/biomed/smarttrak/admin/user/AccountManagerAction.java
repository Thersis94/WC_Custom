package com.biomed.smarttrak.admin.user;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountManagerAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountManagerAction extends SBActionAdapter {

	/**
	 * Constructor
	 */
	public AccountManagerAction() {
		// constructor stub
	}

	/**
	 * Constructor
	 */
	public AccountManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("AccountManagerAction retrieve...");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String accountId = req.hasParameter("accountId") ? req.getParameter("accountId") : null;
		Map<String, AccountVO> accounts;

		try {
			// 1. Retrieve accounts
			accounts = retrieveAccounts(accountId);
			
			// 2. Retrieve account teams if necessary
			retrieveTeams(req, accounts, accountId);
		
			// 3. Retrieve account users if necessary
			retrieveUsers(req, accounts, accountId);
			
		} catch (Exception e) {
			log.error("Error retrieving accounts...", e);
			throw new ActionException(e.getMessage());
		}
		
		// put module data
		putModuleData(accounts, accounts.size(), false, mod.getErrorMessage(), mod.getErrorCondition());

	}

	/**
	 * Retrieves account data.  Query format is based on 
	 * the values of the ID fields that are set on this object
	 * before the method is called.
	 * @return
	 * @throws SQLException
	 */
	protected Map<String, AccountVO> retrieveAccounts(String accountId) throws SQLException {
		StringBuilder sql = formatRetrieveQuery(accountId);
		log.debug("Smarttrak account(s) retrieve SQL: " + sql.toString());
		
		Map<String,AccountVO> accounts;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			AccountVO account;
			accounts =  new LinkedHashMap<>();
			int idx = 1;
			if (accountId != null) ps.setString(idx++, accountId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				account = new AccountVO(rs);
				accounts.put(account.getAccountId(), account);
			}
		} catch (SQLException sqle) {
			throw new SQLException(sqle.getMessage());
		}
		log.debug("found accounts: " + accounts.size());
		return accounts;
	}
	/**
	 * Retrieves all teams or a specific team for an account.
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	protected void retrieveTeams(ActionRequest req, 
			Map<String, AccountVO> accounts, String accountId) throws ActionException {
		log.debug("retrieving teams...");
		if (accountId == null) {
			log.debug("no team ID, not retrieving teams data.");
			return;
		}
		ActionInterface tma = new TeamManagerAction(actionInit);
		tma.setDBConnection(dbConn);
		tma.setAttributes(getAttributes());
		tma.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		List<TeamVO> teams = (List<TeamVO>)mod.getActionData();
		accounts.get(accountId).setTeams(teams);
	}
	
	/**
	 * Retrieves all users for an account
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	protected void retrieveUsers(ActionRequest req, 
			Map<String, AccountVO> accounts, String accountId) throws ActionException {
		log.debug("retrieving users...");
		ActionInterface uma = new UserManagerAction(actionInit);
		uma.setDBConnection(dbConn);
		uma.setAttributes(getAttributes());
		uma.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		Map<String, UserVO> users = (Map<String, UserVO>)mod.getActionData();
		log.debug("users to process: " + users.size());

		assignAccountUsers(accounts, users, accountId);
	}
	
	
	/**
	 * Associates users to their parent account or sets account owners names depending
	 * upon the existence of an account ID.
	 * @param accounts
	 * @param users
	 * @param accountId
	 */
	protected void assignAccountUsers(Map<String,AccountVO> accounts, 
			Map<String,UserVO> users, String accountId) {
		if (accountId == null) {
			// set owner first name
			for (Map.Entry<String, AccountVO> account : accounts.entrySet()) {
				AccountVO avo = account.getValue();
				if (avo.getOwnerProfileId() != null) {
					avo.setOwnerName(users.get(avo.getOwnerProfileId()).getFirstName());
				}
			}
		} else {
			// add users to their respective account
			for (Map.Entry<String, UserVO> user : users.entrySet()) {
				UserVO uvo = user.getValue();
				AccountVO acct = accounts.get(uvo.getAccountId());
				if (acct == null) continue;
				// add user to the acct.
				acct.addUser(uvo);
			}			
		}
		
		
	}
	
	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public StringBuilder formatRetrieveQuery(String accountId) {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.account_id, a.company_id, a.account_nm, a.type_id, ");
		sql.append("a.start_dt, a.expiration_dt, a.owner_profile_id, a.address_txt, ");
		sql.append("a.address2_txt, a.city_nm, a.state_cd, a.zip_cd, a.country_cd, ");
		sql.append("a.status_no, a.create_dt, a.update_dt ");
		sql.append("from ").append(schema).append("biomedgps_account a where 1=1 ");		
		if (accountId != null) sql.append("and a.account_id = ? ");
		sql.append("order by a.account_nm");
		return sql;
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Auto-generated method stub
		// call acct mgr
		// if has acct id, is update
		// else is acct add.
	}
	

}
