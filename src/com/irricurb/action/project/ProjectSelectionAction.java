package com.irricurb.action.project;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.irricurb.util.LookupAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/********************************************************************
 * <b>Title: </b>ProjectSelectionAction.java<br/>
 * <b>Description: </b>Manages the state (session and cookie) for the 
 * irricurb customer and project selection on each page<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 21, 2017
 * Last Updated: 
 *******************************************************************/
public class ProjectSelectionAction extends SimpleActionAdapter {

	public static final String CUSTOMER_LOOKUP = "irricurbCustomerLookup";
	public static final String PROJECT_LOOKUP = "irricurbProjectLookup";
	
	/**
	 * 
	 */
	public ProjectSelectionAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ProjectSelectionAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTSession ses = req.getSession();
		
		if (req.hasParameter("amid") && req.hasParameter(PROJECT_LOOKUP)) {
			assignProjectCookie(req, ses, req.getParameter(PROJECT_LOOKUP));
			
		} else if (req.hasParameter("amid") && req.hasParameter(CUSTOMER_LOOKUP)) {
			assignCustomerCookie(req, ses, req.getParameter(CUSTOMER_LOOKUP));
			
		} else {
			// Gte the cookies loaded and added to the session on a new session
			if (ses.isNew()) checkCookies(req, ses);
			
			// Get the session values
			String customerId = StringUtil.checkVal(ses.getAttribute(CUSTOMER_LOOKUP));
			String projectId = StringUtil.checkVal(ses.getAttribute(PROJECT_LOOKUP));
			
			//. Load the data
			putModuleData(getSelectData(req, customerId, projectId));
		}
		
	}
	
	/**
	 * Updates the project cookie when modified
	 * @param req
	 * @param ses
	 * @param projectId
	 */
	public void assignProjectCookie(ActionRequest req, SMTSession ses, String projectId) {
		HttpServletResponse res = (HttpServletResponse) req.getAttribute(GlobalConfig.HTTP_RESPONSE);
		
		// Clear the project selection
		CookieUtil.add(res, PROJECT_LOOKUP, projectId, "/", (86400 * 365));
		ses.setAttribute(PROJECT_LOOKUP, projectId);
	}
	
	/**
	 * When a new customer is selected, the customer cookie and session value is updated.  The
	 * Project info is removed
	 * @param req
	 * @param ses
	 * @param customerId
	 * @return Select list of projects for a given customer
	 */
	public List<GenericVO> assignCustomerCookie(ActionRequest req, SMTSession ses, String customerId) {
		HttpServletResponse res = (HttpServletResponse) req.getAttribute(GlobalConfig.HTTP_RESPONSE);
		
		// Manage customer selection
		CookieUtil.add(res, CUSTOMER_LOOKUP, customerId, "/", (86400 * 365));
		ses.setAttribute(CUSTOMER_LOOKUP, customerId);
		
		// Clear the project selection
		CookieUtil.add(res, PROJECT_LOOKUP, "", "/", (86400 * 365));
		ses.setAttribute(PROJECT_LOOKUP, "");
		
		return getLookupData(req, PROJECT_LOOKUP);
	}
	
	/**
	 * Grabs the cookies and assigns them to a session
	 * @param req
	 * @param ses
	 */
	public void checkCookies(ActionRequest req, SMTSession ses) {
		SMTCookie[] cookies = req.getCookies();
		if (cookies == null) return;
		
		for (SMTCookie cookie : cookies) {
			if (CUSTOMER_LOOKUP.equalsIgnoreCase(cookie.getName())) {
				ses.setAttribute(CUSTOMER_LOOKUP, cookie.getValue());
			} else if(PROJECT_LOOKUP.equalsIgnoreCase(cookie.getName())) {
				ses.setAttribute(PROJECT_LOOKUP, cookie.getValue());
			}
		}
	}
	
	/**
	 * Retrieves the data for the selection boxes
	 * @param req
	 * @return
	 */
	public Map<String, List<GenericVO>> getSelectData(ActionRequest req, String customerId, String projectId) {
		Map<String, List<GenericVO>> items = new HashMap<String, List<GenericVO>>();
		items.put(CUSTOMER_LOOKUP, getLookupData(req, CUSTOMER_LOOKUP));
		
		if (! projectId.isEmpty())
			items.put(PROJECT_LOOKUP, getLookupData(req, PROJECT_LOOKUP));
		
		return items;
	}
	
	/**
	 * Retrieves the list of selection data
	 * @param req
	 * @param type
	 * @return
	 */
	protected List<GenericVO> getLookupData(ActionRequest req, String type) {
		LookupAction lookup = new LookupAction(this.actionInit);
		lookup.setAttributes(getAttributes());
		lookup.setDBConnection(getDBConnection());
		
		if (PROJECT_LOOKUP.equals(type)) return lookup.getProjects(req);
		else return lookup.getProjectCustomers();
	}
 
}
