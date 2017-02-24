package com.biomed.smarttrak.action;

import org.apache.commons.lang.StringEscapeUtils;

// WC custom
import com.biomed.smarttrak.FinancialDashAction;
import com.biomed.smarttrak.FinancialDashScenarioAction;
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.admin.AccountUserAction;
import com.biomed.smarttrak.admin.CompanyManagementAction;
import com.biomed.smarttrak.admin.GapAnalysisAdminAction;
import com.biomed.smarttrak.admin.InsightAction;
import com.biomed.smarttrak.admin.ListAction;
import com.biomed.smarttrak.admin.MarketManagementAction;
import com.biomed.smarttrak.admin.ProductManagementAction;
import com.biomed.smarttrak.admin.ReportFacadeAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.admin.TeamAction;
import com.biomed.smarttrak.admin.TeamMemberAction;
import com.biomed.smarttrak.admin.UpdatesAction;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
// WC core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.support.SupportTicketFacadeAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AdminControllerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Controller for the SMARTTRAK Admin website (/manage).  
 * Loads and invokes all internal functions after permissions are validated.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class AdminControllerAction extends SimpleActionAdapter {

	protected static final String ACTION_TYPE = "actionType"; //reqParam this class executes around

	// application constants  - these could be moved to sb_config if subject to change
	public static final String BIOMED_ORG_ID = "BMG_SMARTTRAK"; 
	public static final String PUBLIC_SITE_ID = "BMG_SMARTTRAK_1";
	public static final String STAFF_ROLE_ID = "3eef678eb39e87277f000101dfd4f140";
	public static final String REGISTRATION_GRP_ID = "ea884793b2ef163f7f0001011a253456";

	// All logged-in users are Registered Users or Site Administrators.  
	// Roles, as they apply to the site's section hierarchy, are administered by the SecurityController
	public static final int DEFAULT_ROLE_LEVEL = SecurityController.PUBLIC_REGISTERED_LEVEL;

	/*
	 * 'sections' of the SmartTRAK website - used for Solr as well as Recently Viewed/Favorites
	 */
	public enum Section {
		MARKET("market/"), PRODUCT("products/"), COMPANY("companies/");

		private String path;
		Section(String path) { this.path = path; }
		public String getURLToken() { return path; }
	}


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
		String actionType = req.getParameter(ACTION_TYPE);
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

		//setup the redirect.  Build a URL for 'this' page if a child action didn't build one of it's own.
		//NOTE: the controller should (and does) control the redirect.  It also sets 'msg' properly if the child action pukes.
		String redirUrl = (String)req.getAttribute(Constants.REDIRECT_URL);
		if (StringUtil.isEmpty(redirUrl)) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(200);
			url.append(page.getFullPath());
			if (!StringUtil.isEmpty(actionType)) url.append("?actionType=").append(actionType);
			redirUrl = url.toString();
		}
		sendRedirect(redirUrl, msg, req);
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter(ACTION_TYPE)) {
			loadAction(req.getParameter(ACTION_TYPE)).retrieve(req);
		} else {
			//go to view, display the content from the WYSWIYG in /admintool
			super.retrieve(req);
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
			case "hierarchy":
				action = new SectionHierarchyAction();
				break;
			case "agap":
				action = new GapAnalysisAdminAction();
				break;
			case "fd":
				action = new FinancialDashAction();
				break;
			case "fdScenario":
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
			case "account-permissions":
				action = new AccountPermissionAction();
				break;
			case "users":
				action = new AccountUserAction();
				break;
			case "insights":
				action = new InsightAction();
				break;
			case "teams":
				action = new TeamAction();
				break;
			case "team-members":
				action = new TeamMemberAction();
				break;
			case "marketAdmin":
				action = new MarketManagementAction();
				break;
			case "updates":
				action = new UpdatesAction();
				break;
			case "list":
				action = new ListAction();
				break;
			case "activityLog":
				action = new UserActivityAction();
				break;
			case "reports":
				action = new ReportFacadeAction();
				break;
			case "support":
				action = new SupportTicketFacadeAction();
				break;
			default:
				throw new ActionException("unknown action type:" + actionType);
		}

		action.setDBConnection(dbConn);
		action.setAttributes(getAttributes());
		return action;
	}


	/**
	 * takes the pain out of passing Strings in and out of URLs/forms.  Typically these form values arrive HTML encoded.  
	 * Use encodeURIComponent in your JS to compliment what this is doing server-side (at the client).
	 * @param value
	 * @return
	 */
	public static String urlEncode(String value) {
		if (StringUtil.isEmpty(value)) return ""; //going in a URL, we don't want to return a null
		return StringEncoder.urlEncode(StringEscapeUtils.unescapeHtml(value)).replace("+", "%20");
	}
}