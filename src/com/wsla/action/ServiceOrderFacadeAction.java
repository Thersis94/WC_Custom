package com.wsla.action;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.wsla.action.ticket.PartsAction;
import com.wsla.action.ticket.ShipmentAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketOverviewAction;

/****************************************************************************
 * <b>Title</b>: ServiceOrderFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main Facade for Service Orders.  All actions related to the 
 * Service Order main processing will be run through this facade widget.  Each 
 * action will be registered with a key and accessible using the request parameter of type.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 11, 2018
 * @updates:
 ****************************************************************************/

public class ServiceOrderFacadeAction extends FacadeActionAdapter {

	/**
	 * Default type if not passed
	 */
	public static final String DEFAULT_TYPE = "overview";

	/**
	 * Request key utilized top determine widget to call
	 */
	public static final String SELECTOR_KEY = "type";

	public ServiceOrderFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public ServiceOrderFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}

	/**
	 * populate the list of Actions this facade supports running
	 */
	private void loadTypes() {
		// Add the actions and there types here
		actionMap.put(DEFAULT_TYPE, TicketOverviewAction.class);
		actionMap.put(TicketEditAction.AJAX_KEY, TicketEditAction.class);
		actionMap.put("parts", PartsAction.class);
		actionMap.put("shipment", ShipmentAction.class);
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