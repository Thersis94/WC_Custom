package com.mts.action;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

//WC Core
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;

//WC Custom
import com.mts.admin.action.MTSApprovalAction;
import com.mts.admin.action.SSOProviderAction;
import com.mts.admin.action.UserAction;
import com.mts.publication.action.AssetAction;
import com.mts.publication.action.CategoryAction;
import com.mts.publication.action.IssueAction;
import com.mts.publication.action.IssueArticleAction;
import com.mts.publication.action.MTSDocumentAction;
import com.mts.publication.action.PublicationAction;
import com.mts.security.IPSecurityAction;

/****************************************************************************
 * <b>Title</b>: AjaxControllerFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manager for ajax actions.  This class will act as a 
 * single interface / facade to multiple ajax actions.  These actions will typically
 * be simple ajax request / response and will replace having to register each one as
 * a widget.  More complex ajax actions should be registered with its own amid
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 08, 2019
 * @updates:
 ****************************************************************************/
public class AjaxControllerFacadeAction extends FacadeActionAdapter {

	/**
	 * Default type if not passed
	 */
	public static final String DEFAULT_TYPE = "lookup";

	/**
	 * Request key utilized top determine widget to call
	 */
	public static final String SELECTOR_KEY = "type";

	/**
	 * 
	 */
	public AjaxControllerFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public AjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}

	/**
	 * Loads the mapping to the various ajax calls
	 */
	private void loadTypes() {
		actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
		actionMap.put(PublicationAction.AJAX_KEY, PublicationAction.class);
		actionMap.put(IssueAction.AJAX_KEY, IssueAction.class);
		actionMap.put(AssetAction.AJAX_KEY, AssetAction.class);
		actionMap.put(IssueArticleAction.AJAX_KEY, IssueArticleAction.class);
		actionMap.put(CategoryAction.AJAX_KEY, CategoryAction.class);
		actionMap.put(UserAction.AJAX_KEY, UserAction.class);
		actionMap.put(MTSDocumentAction.AJAX_KEY, MTSDocumentAction.class);
		actionMap.put(MTSApprovalAction.AJAX_KEY, MTSApprovalAction.class);
		actionMap.put(IPSecurityAction.AJAX_KEY, IPSecurityAction.class);
		actionMap.put("ssoProviders", SSOProviderAction.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		ModuleVO module = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		module.setSimpleAction(true);
		loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE)).list(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json") && ! req.getBooleanParameter("bypass")) return;
		loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE)).retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ActionInterface action = loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE));
		action.build(req);
	}
}
