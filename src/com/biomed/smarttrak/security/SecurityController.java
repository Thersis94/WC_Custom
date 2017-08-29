package com.biomed.smarttrak.security;

// Log4j
import org.apache.log4j.Logger;


// SMTBaseLibs
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.action.SmarttrakSolrAction;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionNotAuthorizedException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;

// WC core
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: SecurityController.java<p/>
 * <b>Description: overload of Smarttrak permissions.  Makes the decisions about who gets to see what, 
 * based on the Roles of the user and the asset they're trying to view.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2017
 ****************************************************************************/
public class SecurityController {

	protected static final Logger log = Logger.getLogger(SecurityController.class);

	/**
	 * how far down the hierarchy tree are permissions applied.  
	 * Put in a constant in-case Smarttrak ever changes their hierarchy structure.
	 */
	public static final int PERMISSION_DEPTH_LVL = 4;

	private SmarttrakRoleVO role;

	private SecurityController(SmarttrakRoleVO role) {
		super();
		this.role = role;
	}

	/**
	 * static factory method.  useful for inline applications: SecurityController.getInstance(roleData).getSolrACL();
	 * @param role
	 * @return
	 */
	public static SecurityController getInstance(SmarttrakRoleVO role) {
		return new SecurityController(role);
	}

	/**
	 * overloaded static factory method. useful when the calling doesn't already have the RoleVO off of session yet.
	 * @param req
	 * @return
	 */
	public static SecurityController getInstance(ActionRequest req) {
		return new SecurityController((SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA));
	}

	/**
	 * is the user authorized to see the Financial Dashboard tool (period)
	 * @return
	 */
	public boolean isFdAuthorized() {
		return role.isFdAuthorized();
	}

	/**
	 * is the user authorized to see the Gap Analysis tool (period)
	 * @return
	 */
	public boolean isGaAuthorized() {
		return role.isGaAuthorized();
	}
	
	
	/**
	 * is the user authorized to browser companies/markets/products.  True if any sections are toggled for their account.
	 * @return
	 */
	public boolean isBrowseAuthorized() {
		return role.isBrowseAuthorized();
	}


	/**
	 * Called from within the SmartTRAK actions to ensure the data about to be presented to the user is 
	 * something they have permissions to view.  Unfortunately we have to load the data before we can make 
	 * this determination.
	 * @param object (a Market, a Company, a Product, an Insight, an Update)
	 * @throws ActionException
	 */
	public void isUserAuthorized(SecureSolrDocumentVO object, ActionRequest req) 
			throws ActionNotAuthorizedException {
		//use the same mechanisms solr is using to verify data access permissions.
		String assetAcl = object.getACLPermissions();
		String indexType = object.getSolrIndex();
		Section sec;
		if (BiomedInsightIndexer.INDEX_TYPE.equals(indexType)) {
			sec = Section.INSIGHT;
		} else if (UpdateIndexer.INDEX_TYPE.equals(indexType)) {
			sec = Section.UPDATES_EDITION;
		} else {
			sec = SmarttrakSolrAction.BROWSE_SECTION;
		}
		String[] roleAcl = role.getAuthorizedSections(sec);
		log.debug("user ACL from " + sec + ": " + StringUtil.getToString(roleAcl));

		if (roleAcl == null || roleAcl.length == 0 || !AccessControlQuery.isAllowed(assetAcl, null, roleAcl))
			throwAndRedirect(req);

		log.debug("user is authorized");
	}


	/**
	 * tests the user's role object to see if they should have access to this tool.
	 * if they do not redirect them to the insufficient permissions page.
	 * called from FinancialDashAction
	 * @param req
	 * @throws ActionNotAuthorizedException 
	 */
	public static void isFdAuth(ActionRequest req) throws ActionNotAuthorizedException {
		SmarttrakRoleVO role = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (!role.isFdAuthorized())
			throwAndRedirect(req);
	}


	/**
	 * tests the user's role object to see if they should have access to this tool.
	 * if they do not redirect them to the insufficient permissions page.
	 * called from GapAnalysisAction
	 * @param req
	 * @throws ActionNotAuthorizedException 
	 */
	public static void isGaAuth(ActionRequest req) throws ActionNotAuthorizedException {
		SmarttrakRoleVO role = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (!role.isGaAuthorized())
			throwAndRedirect(req);
	}


	/**
	 * tests the user's role object to see if they should have access to this tool.
	 * if they do not redirect them to the insufficient permissions page.
	 * called from ProductExplorerAction
	 * @param req
	 * @throws ActionNotAuthorizedException 
	 */
	public static void isPeAuth(ActionRequest req) throws ActionNotAuthorizedException {
		SmarttrakRoleVO role = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (!role.isPeAuthorized())
			throwAndRedirect(req);
	}


	/**
	 * Reused.  Calling this method sets the redirectUrl and throws the interrupt exception.  Use other methods to perform authorization tests.
	 * @param req
	 * @throws ActionNotAuthorizedException 
	 */
	public static void throwAndRedirect(ActionRequest req) throws ActionNotAuthorizedException {
		log.debug("user is not authorized.  Setting up redirect, then throwing exception");
		StringBuilder url = new StringBuilder(150);
		url.append(AdminControllerAction.PUBLIC_401_PG).append("?ref=").append(req.getRequestURL());
		new SiteBuilderUtil().manualRedirect(req, url.toString());
		throw new ActionNotAuthorizedException("not authorized");
	}
}