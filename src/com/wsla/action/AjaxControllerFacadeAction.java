package com.wsla.action;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.wsla.action.admin.ProductMasterAction;
import com.wsla.action.admin.ProviderAction;
import com.wsla.action.admin.ProviderLocationAction;

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
 * @since Sep 11, 2018
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
	 * 
	 */
	private void loadTypes() {
		actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
		actionMap.put("provider", ProviderAction.class);
		actionMap.put("providerLocation", ProviderLocationAction.class);
		actionMap.put("productMaster", ProductMasterAction.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		ModuleVO module = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		module.setSimpleAction(true);
		loadActionByType(req.getParameter(SELECTOR_KEY	, DEFAULT_TYPE)).list(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE)).retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE)).build(req);
	}
}