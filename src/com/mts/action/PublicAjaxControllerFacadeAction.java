package com.mts.action;

// MTS Libs
import com.mts.publication.action.ArticleByCategoryAction;
import com.mts.publication.action.DocumentBrowseAction;
import com.mts.subscriber.action.UserInfoWidget;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

// WC Core
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;

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
public class PublicAjaxControllerFacadeAction extends FacadeActionAdapter {

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
	public PublicAjaxControllerFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public PublicAjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}

	/**
	 * Loads the mapping to the various ajax calls
	 */
	private void loadTypes() {
		actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
		actionMap.put(ArticleByCategoryAction.AJAX_KEY, ArticleByCategoryAction.class);
		actionMap.put(DocumentBrowseAction.AJAX_KEY, DocumentBrowseAction.class);
		actionMap.put(UserInfoWidget.AJAX_KEY, UserInfoWidget.class);
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
