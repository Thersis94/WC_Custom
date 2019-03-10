package com.perfectstorm.action;

import com.perfectstorm.action.venue.VenueAction;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;

/****************************************************************************
 * <b>Title</b>: VenueFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main facade for venues. All actions related to the venues main
 * processing will be run through this facade widget. Each  action will be registered
 * with a key and accessible using the request parameter of type.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/

public class VenueFacadeAction extends FacadeActionAdapter {

	/**
	 * Default type if not passed
	 */
	public static final String DEFAULT_TYPE = "venue_public";

	/**
	 * Request key utilized to determine widget to call
	 */
	public static final String SELECTOR_KEY = "type";

	public VenueFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public VenueFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}

	/**
	 * Populate the list of Actions this facade supports running
	 */
	private void loadTypes() {
		// Add the actions and their types here
		actionMap.put(DEFAULT_TYPE, VenueAction.class);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		loadActionByType(req.getStringParameter(SELECTOR_KEY, DEFAULT_TYPE)).retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		// Set the action to use the simple admin view
		ModuleVO module = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		module.setSimpleAction(true);
		super.list(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		loadActionByType(req.getStringParameter(SELECTOR_KEY, DEFAULT_TYPE)).build(req);
	}
}