package com.biomed.smarttrak.action;

import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.TeamAction;
import com.biomed.smarttrak.admin.TeamMemberAction;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MyTeamsAction.java<p/>
 * <b>Description: </b> Simple action that lets account owners manage teams and user records within their Account. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 27, 2017
 ****************************************************************************/
public class MyTeamsAction extends SimpleActionAdapter {

	private static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID;

	public MyTeamsAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public MyTeamsAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		verifyRole(req);

		//retrieve a list of team members to manage
		ActionInterface ae;
		if ("team-members".equals(req.getParameter("actionType"))) {
			ae = new TeamMemberAction();
		} else {
			ae = new TeamAction();
		}
		ae.setDBConnection(dbConn);
		ae.setAttributes(getAttributes());
		ae.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		verifyRole(req);

		Object msg;
		ActionInterface ae;
		if ("team-members".equals(req.getParameter("actionType"))) {
			ae = new TeamMemberAction();
		} else {
			ae = new TeamAction();
		}
		ae.setDBConnection(dbConn);
		ae.setAttributes(getAttributes());
		try {
			if (AdminConstants.REQ_DELETE.equals(req.getParameter("actionPerform"))) {
				ae.delete(req);
			} else {
				ae.build(req);
			}
			msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		} catch (Exception e) {
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		//redirect
		StringBuilder sb = new StringBuilder(70);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		sb.append(page.getFullPath()).append("?msg=").append(msg);
		sbUtil.manualRedirect(req, sb.toString());
	}


	/**
	 * verifies the user is authorized to run this action, then takes their accountId off session and puts 
	 * it on the request, so it cannot be spoofed from the browser.
	 * @param req
	 */
	private void verifyRole(ActionRequest req) throws ActionException {
		SmarttrakRoleVO role = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);

		//restrict access to account owners
		if (role == null || user == null || !role.isAccountOwner())
			throw new ActionException("not authorized");

		//set the accountId - note this cannot be altered from the browser, we take from session
		req.setParameter(ACCOUNT_ID, user.getAccountId());
	}
}