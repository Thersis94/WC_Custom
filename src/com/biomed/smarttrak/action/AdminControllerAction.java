/**
 *
 */
package com.biomed.smarttrak.action;

import com.biomed.smarttrak.FinancialDashAction;
import com.biomed.smarttrak.FinancialDashScenarioAction;
import com.biomed.smarttrak.action.gap.GapFacadeAction;
import com.biomed.smarttrak.admin.ContentHierarchyAction;
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
 * @author raptor
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
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		String msg = (String) attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		try {
			ActionInterface act = loadAction(actionType);
			if(act != null) {
				act.build(req);
			}
		} catch (ActionException ae) {
			log.error("could not forward requested Action.", ae.getCause());
			msg = (String) attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath()).append("?msg=").append(msg);
		url.append("&actionType=").append(actionType);

		sbUtil.manualRedirect(req, url.toString());
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		ActionInterface act = loadAction(actionType);
		if(act != null) {
			act.retrieve(req);
		}
	}


	/**
	 * Based on passed cPage, instantiate the appropriate class and return.
	 * @param cPage
	 * @return
	 * @throws ActionException
	 */
	private ActionInterface loadAction(String actionType) throws ActionException {
		ActionInterface action;
		switch (StringUtil.checkVal(actionType)) {
			case "contentHierarchy":
				action = new ContentHierarchyAction();
				break;
			case "gapAnalysis":
				action = new GapFacadeAction();
				break;
			case "financialDashboard":
				action = new FinancialDashAction();
				break;
			case "noteAction":
				action = new NoteAction();
			case "financialDashScenario":
				action = new FinancialDashScenarioAction();
				break;
			default:
				return null;
		}

		action.setDBConnection(dbConn);
		action.setAttributes(getAttributes());

		return action;
	}
}
