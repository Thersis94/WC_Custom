package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;

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
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);

		Map<String, AccountVO> accounts = new LinkedHashMap<>();
		// retrieve accounts stub
		
		// put module data
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
		Map<String, AccountVO> accounts = new LinkedHashMap<>();
		
		// 1. Retrieve accounts
		
		// 2. Retrieve account teams if necessary
		
		// 3. Retrieve account users if necessary
		
		return accounts;
	}
	
	/**
	 * Retrieves all teams or a specific team for an account.
	 * @throws SQLException
	 */
	protected void retrieveTeams() throws SQLException {
		// stub
	}
	
	/**
	 * Retrieves all users for an account
	 * @throws SQLException
	 */
	protected void retrieveUsers() throws SQLException {
		// stub
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
