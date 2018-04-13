package com.irricurb.action.project;

// JDK 1.8.x
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// J2EE 6
import javax.servlet.http.HttpServletResponse;

// WC Libs
import com.irricurb.util.LookupAction;
import com.smt.sitebuilder.action.SimpleActionAdapter;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;


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
		
		if (req.hasParameter("pmid") && req.hasParameter(PROJECT_LOOKUP)) {
			assignProjectCookie(req, ses, req.getParameter(PROJECT_LOOKUP));
			
		} else if (req.hasParameter("pmid") && req.hasParameter(CUSTOMER_LOOKUP)) {
			putModuleData(assignCustomerCookie(req, ses, req.getParameter(CUSTOMER_LOOKUP)));
			
		} else {
			// Get the cookies loaded and added to the session on a new session
			if (ses.isNew()) checkCookies(req, ses);
			
			// Get the session values
			String customerId = StringUtil.checkVal(ses.getAttribute(CUSTOMER_LOOKUP));
			String projectId = StringUtil.checkVal(ses.getAttribute(PROJECT_LOOKUP));
			
			//. Load the data
			putModuleData(getSelectData(req, ses, customerId, projectId));
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
		
		// Add the customerId
		req.setParameter("customerId", customerId);
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
			log.info("Name: " + cookie.getName());
			if (CUSTOMER_LOOKUP.equalsIgnoreCase(cookie.getName())) {
				ses.setAttribute(CUSTOMER_LOOKUP, cookie.getValue());

			} else if(PROJECT_LOOKUP.equalsIgnoreCase(cookie.getName())) {
				ses.setAttribute(PROJECT_LOOKUP, cookie.getValue());

			}
		}
		
		log.info("Sess Info: " + ses.getAttribute(CUSTOMER_LOOKUP) + "|" + ses.getAttribute(PROJECT_LOOKUP));
	}
	
	/**
	 * Retrieves the data for the selection boxes
	 * @param req
	 * @return
	 */
	public Map<String, List<GenericVO>> getSelectData(ActionRequest req,SMTSession ses, String customerId, String projectId) {
		Map<String, List<GenericVO>> items = new HashMap<String, List<GenericVO>>();
				
		// Get the list of available customers.  If a customer isn't selected via cookie or session,
		// Use the first customer and assign to session and request 
		items.put(CUSTOMER_LOOKUP, getLookupData(req, CUSTOMER_LOOKUP));
		customerId = assignValues(CUSTOMER_LOOKUP, customerId, items, req, ses);
		
		// Get the list of available projects.  If a project isn't selected via cookie or session,
		// Use the first project and assign to session and request 
		items.put(PROJECT_LOOKUP, getLookupData(req, PROJECT_LOOKUP));
		assignValues(PROJECT_LOOKUP, projectId, items, req, ses);
		
		return items;
	}
	
	/**
	 * Assigns the values to the session, request and cookies if not assigned
	 * @param key The Key (PROJECT_LOOKUP or CUSTOMER_LOOKUP)
	 * @param value customerId or projectId
	 * @param items Map of Lists (Project or Customer)
	 * @param req
	 * @param ses
	 * @return
	 */
	protected String assignValues(String key, String value, Map<String, List<GenericVO>> items, ActionRequest req, SMTSession ses) {
		if(! value.isEmpty() || items.get(key).isEmpty()) return value;
		
		HttpServletResponse res = (HttpServletResponse) req.getAttribute(GlobalConfig.HTTP_RESPONSE);
		value = (String) items.get(key).get(0).getKey();
		ses.setAttribute(key, value);
		req.setParameter("customerId", value);
		CookieUtil.add(res, key, value, "/", (86400 * 365));
		
		return value;
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
