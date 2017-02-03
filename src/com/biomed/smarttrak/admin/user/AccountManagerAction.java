package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.SQLException;
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
		ModuleVO mod = (ModuleVO)req.getAttribute(Constants.MODULE_DATA);
		
		Map<String, AccountVO> accounts;
		try {
			accounts = retrieveAccounts(req);
		} catch (Exception e) {
			mod.setError(e.getMessage(),e);
			accounts = new HashMap<>();
		}
		
		this.putModuleData(accounts, accounts.size(), false, mod.getErrorMessage(), mod.getErrorCondition());

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
		AccountManager am = new AccountManager(dbConn, attributes);
		am.setAccountId(accountId);
		
		Map<String, AccountVO> accounts;
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
		if (accountId == null) return;

		log.debug("Retrieving users for account...");
		UserManager um = new UserManager(dbConn, attributes);
		um.setAccountId(accountId);
		List<UserVO> users = um.retrieveCompleteUser();
		accounts.get(accountId).setUsers(users);
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
