package com.biomed.smarttrak.admin.user;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.AccountVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
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
		
		AccountManager am = new AccountManager(dbConn, attributes);
		am.setAccountId(StringUtil.checkVal(req.getParameter("accountId"),null));
		
		List<AccountVO> accounts;
		try {
			accounts = am.retrieveAccounts();
		} catch(SQLException sqle) {
			accounts = new ArrayList<>();
			mod.setError(sqle.getMessage(),sqle);
		}
		
		this.putModuleData(accounts, accounts.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
			
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
