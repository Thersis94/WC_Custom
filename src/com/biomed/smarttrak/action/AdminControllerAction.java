package com.biomed.smarttrak.action;

import java.util.HashMap;
import java.util.Map;

//apache commons-lang jar
import org.apache.commons.lang.StringEscapeUtils;

// WC custom
import com.biomed.smarttrak.fd.FinancialDashAction;
import com.biomed.smarttrak.fd.FinancialDashScenarioAction;
import com.biomed.smarttrak.admin.FinancialDashHierarchyAction;
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.admin.AccountUserAction;
import com.biomed.smarttrak.admin.CompanyManagementAction;
import com.biomed.smarttrak.admin.GapAnalysisAdminAction;
import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.InsightAction;
import com.biomed.smarttrak.admin.ListAction;
import com.biomed.smarttrak.admin.MarketManagementAction;
import com.biomed.smarttrak.admin.ProductManagementAction;
import com.biomed.smarttrak.admin.ReportFacadeAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.admin.SupportFacadeAction;
import com.biomed.smarttrak.admin.TeamAction;
import com.biomed.smarttrak.admin.TeamMemberAction;
import com.biomed.smarttrak.admin.UpdatesAction;

//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.solr.management.SolrSynonymAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

//WC Email Campaigns
import com.smt.sitebuilder.action.emailcampaign.CampaignInstanceAction;
import com.smt.sitebuilder.action.emailcampaign.InstanceReport;

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
	public static final String EUREPORT_ROLE_ID = "6f5c869a9b0e9b640a001421bce81c9b";
	public static final String REGISTRATION_GRP_ID = "ea884793b2ef163f7f0001011a253456";

	// All logged-in users are Registered Users or Site Administrators.  
	// Roles, as they apply to the site's section hierarchy, are administered by the SecurityController
	public static final int DEFAULT_ROLE_LEVEL = SecurityController.PUBLIC_REGISTERED_LEVEL;
	public static final int EUREPORT_ROLE_LEVEL = 5;
	public static final int STAFF_ROLE_LEVEL = 90;

	public static final int DOC_ID_MIN_LEN = 15;  //used to determine if a pkId will be globally unique if fed to Solr as documentId, 

	public static final String PUBLIC_401_PG = "/subscribe"; //where users get redirected when they're not authorized to view an asset


	/*
	 * 'sections' of the SmartTRAK website - used for Solr as well as Recently Viewed/Favorites
	 */
	public enum Section {
		MARKET("markets/"), PRODUCT("products/"), COMPANY("companies/"), INSIGHT("insights/");

		private String path;
		Section(String path) { this.path = path; }
		public String getURLToken() { return path; }
		public String getPageURL() { //reverses the slash to the front of the urlToken, making it a relative URL to the given page
			return "/" + getURLToken().substring(0, getURLToken().length());
		}
	}

	/*
	 * the master list of actions this Controller can execute
	 */
	protected static final Map<String, Class<? extends ActionInterface>> ACTIONS;

	public AdminControllerAction() {
		super();
	}

	public AdminControllerAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * populates the action map when the static constructor is called.  This will make our map live once in the JVM
	 */
	static {
		ACTIONS = new HashMap<>(35);
		ACTIONS.put("hierarchy", SectionHierarchyAction.class);
		ACTIONS.put("agap", GapAnalysisAdminAction.class);
		ACTIONS.put("fd", FinancialDashAction.class);
		ACTIONS.put("fdScenario", FinancialDashScenarioAction.class);
		ACTIONS.put("fdHierarchy", FinancialDashHierarchyAction.class);
		ACTIONS.put("productAdmin", ProductManagementAction.class);
		ACTIONS.put("companyAdmin", CompanyManagementAction.class);
		ACTIONS.put("accounts", AccountAction.class);
		ACTIONS.put("account-permissions", AccountPermissionAction.class);
		ACTIONS.put("users", AccountUserAction.class);
		ACTIONS.put("insights", InsightAction.class);
		ACTIONS.put("teams", TeamAction.class);
		ACTIONS.put("team-members", TeamMemberAction.class);
		ACTIONS.put("marketAdmin", MarketManagementAction.class);
		ACTIONS.put("updates", UpdatesAction.class);
		ACTIONS.put("list", ListAction.class);
		ACTIONS.put("reports", ReportFacadeAction.class);
		ACTIONS.put("support", SupportFacadeAction.class);
		ACTIONS.put("synonyms", SolrSynonymAction.class);
		ACTIONS.put("marketingCampaigns", CampaignInstanceAction.class);
		ACTIONS.put("marketingInstanceReport", InstanceReport.class);
		ACTIONS.put("uwr", UpdatesWeeklyReportAction.class); 
		ACTIONS.put("grid", GridChartAction.class);
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
			} else if(AdminConstants.REQ_COPY.equals(req.getParameter("actionPerform"))){
				action.copy(req);
			}else {
				action.build(req);
			}
			msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		} catch (ActionException ae) {
			log.error("could not execute " + actionType, ae.getCause());
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		// Only proceed to redirect if it not a json request (?json=true)
		if (Convert.formatBoolean(req.getParameter("json")))
			return;

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
	protected ActionInterface loadAction(String actionType) throws ActionException {
		Class<?> c = ACTIONS.get(actionType);
		if (c == null) 
			throw new ActionException("unknown action type:" + actionType);

		//instantiate the action & return it - pass attributes & dbConn
		try {
			ActionInterface action = (ActionInterface) c.newInstance();
			action.setDBConnection(dbConn);
			action.setAttributes(getAttributes());
			return action;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ActionException("Problem Instantiating type: " + actionType);
		}
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