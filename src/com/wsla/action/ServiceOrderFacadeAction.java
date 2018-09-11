package com.wsla.action;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;

// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;

/****************************************************************************
 * <b>Title</b>: ServiceOrderFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main Facade for Service Orders
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
	 * 
	 */
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
	 * 
	 */
	private void loadTypes() {
		// actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
	}

}

