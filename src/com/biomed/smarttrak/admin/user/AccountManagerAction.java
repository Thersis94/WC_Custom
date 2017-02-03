package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

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
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);

		Map<String, AccountVO> accounts;
		try {
			accounts = retrieveAccounts(req);
		} catch (Exception e) {
			mod.setError(e.getMessage(),e);
			accounts = new HashMap<>();
		}

		putModuleData(accounts, accounts.size(), false, mod.getErrorMessage(), mod.getErrorCondition());

	}
	
	/**
	 * Retrieves account(s) data.
	 * @param req
	 * @return
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	protected Map<String, AccountVO> retrieveAccounts(ActionRequest req) 
			throws DatabaseException, SQLException {
		String accountId = StringUtil.checkVal(req.getParameter("accountId"),null);
		String teamId = StringUtil.checkVal(req.getParameter("teamId"),null);
		Map<String, AccountVO> accounts;
		
		AccountManager am = new AccountManager(dbConn, attributes);
		am.setAccountId(accountId);
		am.setExcludeInactiveAccounts(true);
		accounts = am.retrieveAccounts();
		
		// Retrieve account teams
		retrieveTeams(accounts, accountId, teamId);
		
		// Retrieve account users.
		retrieveUsers(accounts, accountId);
		
		return accounts;
	}
	
	/**
	 * Retrieves all teams or a specific team for an account.
	 * @param accounts
	 * @param accountId
	 * @param teamId
	 * @throws SQLException
	 */
	protected void retrieveTeams(Map<String, AccountVO> accounts, 
			String accountId, String teamId) throws SQLException {
		if (accountId == null) return;

		log.debug("Retrieving teams for account...");
		TeamManager tm = new TeamManager(dbConn, attributes);
		tm.setAccountId(accountId);
		tm.setTeamId(teamId);
		List<TeamVO> teams = tm.retrieveTeams();
		accounts.get(accountId).setTeams(teams);
	}
	
	/**
	 * Retrieves all users for an account
	 * @param accounts
	 * @param accountId
	 * @throws DatabaseException 
	 * @throws ActionException
	 */
	protected void retrieveUsers(Map<String, AccountVO> accounts, 
			String accountId) throws DatabaseException, SQLException {
		log.debug("Retrieving users for account...");

		UserManager um = new UserManager(dbConn, attributes);
		um.setAccountId(accountId);
		List<UserVO> users;
		
		if (accountId == null) {
			/* This is a request for all accounts, so we only want to retrieve
			 * profiles each account owner, a very small set */
			users = findAccountOwners(accounts);
			Map<String, UserDataVO> ownerProfiles = um.retrieveProfiles(users);
			for (Map.Entry<String, AccountVO> entry : accounts.entrySet()) {
				String ownerProfileId = entry.getValue().getOwnerProfileId();
				if (ownerProfileId == null) continue;
				entry.getValue().setOwnerName(ownerProfiles.get(ownerProfileId).getFirstName());
			}
		} else {
			// retrieve users for this acct.
			users = um.retrieveCompleteUser();
			// set users on the account bean
			accounts.get(accountId).setUsers(users);
			// set the owner name (first name only) for view's use.
			accounts.get(accountId).setOwnerName();
		}
	}

	/**
	 * Finds account owners
	 * @param accounts
	 * @return
	 */
	protected List<UserVO> findAccountOwners(Map<String, AccountVO> accounts) {
		List<UserVO> owners = new ArrayList<>();
		
		// loop the accounts, find the owners
		for (Map.Entry<String, AccountVO> entry : accounts.entrySet()) {
			String profileId = entry.getValue().getOwnerProfileId();
			if (profileId == null) continue;
			UserVO user = new UserVO();
			user.setProfileId(profileId);
			if (! owners.contains(user)) owners.add(user);
		}
		
		return owners;
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
