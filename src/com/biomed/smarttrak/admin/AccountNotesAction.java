package com.biomed.smarttrak.admin;

import java.util.List;
import java.util.Map;

// WC_Custom
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.NoteAction;
import com.biomed.smarttrak.vo.NoteVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountNotesAction.java</p>
 <p><b>Description: Facades Account Notes (a /manage functionality) through the Smartrak Notes Action.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since May 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountNotesAction extends SBActionAdapter {

	private static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param

	public AccountNotesAction() {
		super();
	}

	public AccountNotesAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		AccountAction.loadAccount(req, dbConn, getAttributes());
		
		String acctId = req.getParameter(ACCOUNT_ID);
		NoteAction na = new NoteAction(actionInit);
		na.setAttributes(getAttributes());
		na.setDBConnection(getDBConnection());
		Map<String, List<NoteVO>> notes = na.getAccountNotes(acctId);

		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		na.setupAttributes(modVo);
		modVo.setActionData(notes.get(acctId));
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		req.setParameter("teamId", "user"); //sets a null teamId value
		saveRecord(req);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		req.setParameter("isDelete", "true"); //tells the build method we want to delete the given record
		saveRecord(req);
	}


	/**
	 * Invokes NoteAction to save or delete a record
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		ActionInitVO actionInit = new ActionInitVO();
		actionInit.setActionId(mod.getActionId());
		NoteAction na = new NoteAction(actionInit);
		na.setAttributes(getAttributes());
		na.setDBConnection(getDBConnection());
		na.build(req);
		setupRedirect(req);
	}


	/**
	 * builds the redirect URL that takes us back to the list of teams page.
	 * @param req
	 */
	protected void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?actionType=").append(req.getParameter("actionType"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		url.append("&accountName=").append(AdminControllerAction.urlEncode(req.getParameter("accountName")));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
}