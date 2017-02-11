package com.biomed.smarttrak.action;

import com.biomed.smarttrak.FinancialDashAction;
import com.biomed.smarttrak.FinancialDashScenarioAction;
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.CompanyManagementAction;
import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.biomed.smarttrak.admin.GapAnalysisAdminAction;
import com.biomed.smarttrak.admin.MarketManagementAction;
import com.biomed.smarttrak.admin.ProductManagementAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BioMedAjaxAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> BioMed Ajax Action that will facade the proper call for
 * each Action.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class AdminControllerAction extends SimpleActionAdapter {

	public AdminControllerAction() {
		super();
	}

	public AdminControllerAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		//pass to superclass for portlet registration (WC admintool)
		//this method is not called from the front-end UI
		super.retrieve(req);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		String actionType = req.getParameter("actionType");
		String msg;

		try {
			ActionInterface action = loadAction(actionType);

			//allow either deletes or saves (build) to be called directly from the controller
			if (AdminConstants.REQ_DELETE.equals(req.getParameter("actionPerform"))) {
				action.delete(req);
			} else {
				action.build(req);
			}
			msg = (String) attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		} catch (ActionException ae) {
			log.error("could not execute " + actionType, ae.getCause());
			msg = (String) attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		}

		if (StringUtil.isEmpty((String)req.getAttribute(Constants.REDIRECT_URL))) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(200);
			url.append(page.getFullPath());
			if (!StringUtil.isEmpty(actionType)) url.append("?actionType=").append(actionType);
			sendRedirect(url.toString(), msg, req);
		}
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		loadAction(req.getParameter("actionType")).retrieve(req);
	}


	/**
	 * Based on passed cPage, instantiate the appropriate class and return.
	 * @param cPage
	 * @return
	 * @throws ActionException
	 */
	private ActionInterface loadAction(String actionType) throws ActionException {
		/*
		 * TODO add some means of verifying user role/permission before executing
		 * Actions.  Need to protect Admin functionality from the public side.
		 */
		ActionInterface action;
		switch (StringUtil.checkVal(actionType)) {
			case "hierarchy":
				action = new ContentHierarchyAction();
				break;
			case "agap":
				action = new GapAnalysisAdminAction();
				break;
			case "financialDashboard":
				action = new FinancialDashAction();
				break;
			case "financialDashScenario":
				action = new FinancialDashScenarioAction();
				break;
			case "productAdmin":
				action = new ProductManagementAction();
				break;
			case "companyAdmin":
				action = new CompanyManagementAction();
				break;
			case "accounts":
				action = new AccountAction();
				break;
			case "marketAdmin":
				action = new MarketManagementAction();
				break;
			default:
				throw new ActionException("unknown action type:" + actionType);
		}

		action.setDBConnection(dbConn);
		action.setAttributes(getAttributes());
		return action;
	}
}