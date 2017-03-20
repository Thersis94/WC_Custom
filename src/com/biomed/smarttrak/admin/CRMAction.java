package com.biomed.smarttrak.admin;

import com.biomed.crm.CRMUtil;
import com.biomed.crm.CRMUtil.ActionType;
import com.biomed.crm.BiomedCRMCustomerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CRMAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action handling the retrieval and creation of CRM records
 * for smarttrak
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

public class CRMAction extends SBActionAdapter {
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			CRMUtil util = new CRMUtil((String) attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
			ActionType type;
			if (req.hasParameter("actionTarget")) {
				type = CRMUtil.ActionType.valueOf(req.getParameter("actionTarget"));
			} else {
				type = CRMUtil.ActionType.CUSTOMER;
			}
			if (req.hasParameter("alter")) {
				util.alterElement(type, req);
			} else if ("update".equals(buildAction)) {
				util.updateElement(type, req);
			}
		} catch(Exception e) {
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator about this issue.";
			log.error(e);
		}
		redirectRequest(msg, buildAction, req.getParameter("customerId"), req);
	}

	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		CRMUtil util = new CRMUtil((String) attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
		if ("customer".equals(req.getParameter("type"))) {
			putModuleData(util.getCustomerInfo(null));
		} else if ("deals".equals(req.getParameter("type"))) {
			BiomedCRMCustomerVO cust = new BiomedCRMCustomerVO(req);
			util.addDeals(cust);
			putModuleData(cust);
		} else if ("notes".equals(req.getParameter("type"))) {
			BiomedCRMCustomerVO cust = new BiomedCRMCustomerVO(req);
			util.addNotes(cust);
			putModuleData(cust);
		} else if ("reminders".equals(req.getParameter("type"))) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			BiomedCRMCustomerVO cust = new BiomedCRMCustomerVO(req);
			util.addReminders(cust, StringUtil.checkVal(user.getProfileId()));
			putModuleData(cust);
		} else if (!req.hasParameter("add") && req.hasParameter("customerId")) {
			putModuleData(util.getCustomerInfo(req.getParameter("customerId")).get(0));
		} 
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	protected void redirectRequest(String msg, String buildAction, String customerId, ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append(page.getFullPath()).append("?actionType=crm&").append("msg=").append(msg);

		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		//if a market is being deleted do not redirect the user to a market page
		if (!"delete".equals(buildAction) || 
				ActionType.valueOf(req.getParameter("actionTarget")) != ActionType.CUSTOMER) {
			url.append("&customerId=").append(customerId);
		}

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}
