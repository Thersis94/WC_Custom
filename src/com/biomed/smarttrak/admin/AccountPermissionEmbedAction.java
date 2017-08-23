package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> AccountPermissionEmbedAction.java<br/>
 * <b>Description:</b>  This action's sole purpose is to embed in the Welcome Msg. user email.  It loads the list of Sections
 * the user's account is authorized for and displays the Tree as an indented hml list, embedded in the notification email.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 22, 2017
 ****************************************************************************/
public class AccountPermissionEmbedAction extends SimpleActionAdapter {

	public AccountPermissionEmbedAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AccountPermissionEmbedAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Load the accounts' permission tree and go to View (which is Freemarker)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		AccountPermissionAction act = new AccountPermissionAction();
		act.setDBConnection(getDBConnection());
		act.setAttributes(getAttributes());
		act.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

}
