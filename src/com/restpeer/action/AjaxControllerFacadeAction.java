package com.restpeer.action;

// RP Libs
import com.restpeer.action.admin.ProductWidget;
import com.restpeer.action.admin.UserWidget;
import com.restpeer.action.account.LocationAttributeWidget;
import com.restpeer.action.account.LocationProductAction;
import com.restpeer.action.account.MemberLocationUserAction;
import com.restpeer.action.admin.AttributeWidget;
import com.restpeer.action.admin.CategoryWidget;
import com.restpeer.action.admin.MemberLocationWidget;
import com.restpeer.action.admin.MemberWidget;

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
	 * Loads the mapping to the various ajax calls
	 */
	private void loadTypes() {
		actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
		actionMap.put(CategoryWidget.AJAX_KEY, CategoryWidget.class);
		actionMap.put(ProductWidget.AJAX_KEY, ProductWidget.class);
		actionMap.put(AttributeWidget.AJAX_KEY, AttributeWidget.class);
		actionMap.put(MemberWidget.AJAX_KEY, MemberWidget.class);
		actionMap.put(MemberLocationWidget.AJAX_KEY, MemberLocationWidget.class);
		actionMap.put(UserWidget.AJAX_KEY, UserWidget.class);
		actionMap.put(MemberLocationUserAction.AJAX_KEY, MemberLocationUserAction.class);
		actionMap.put(LocationProductAction.AJAX_KEY, LocationProductAction.class);
		actionMap.put(LocationAttributeWidget.AJAX_KEY, LocationAttributeWidget.class);
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
		ActionInterface action = loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE));
		action.build(req);
	}
}
