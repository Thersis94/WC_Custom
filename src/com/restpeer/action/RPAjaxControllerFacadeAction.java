package com.restpeer.action;

import com.restpeer.action.admin.UserWidget;
import com.restpeer.action.account.LocationAttributeWidget;
import com.restpeer.action.account.LocationProductAction;
import com.restpeer.action.account.LocationProductScheduleAction;
import com.restpeer.action.account.MemberLocationUserAction;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;

// WC Core
import com.smt.sitebuilder.action.commerce.AjaxControllerFacadeAction;

/****************************************************************************
 * <b>Title</b>: AjaxControllerFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manager for ajax actions.  This class will act as a 
 * single interface / facade to multiple ajax actions.  These actions will typically
 * be simple ajax request / response and will replace having to register each one as
 * a widget.  More complex ajax actions should be registered with its own amid.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 11, 2018
 * @updates:
 ****************************************************************************/
public class RPAjaxControllerFacadeAction extends AjaxControllerFacadeAction {

	/**
	 * 
	 */
	public RPAjaxControllerFacadeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RPAjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Loads the mapping to the various ajax calls
	 */
	@Override
	protected void loadTypes() {
		super.loadTypes();
		actionMap.put(DEFAULT_TYPE, RPSelectLookupAction.class);
		
		actionMap.put(UserWidget.AJAX_KEY, UserWidget.class);
		actionMap.put(MemberLocationUserAction.AJAX_KEY, MemberLocationUserAction.class);
		actionMap.put(LocationProductAction.AJAX_KEY, LocationProductAction.class);
		actionMap.put(LocationProductScheduleAction.AJAX_KEY, LocationProductScheduleAction.class);
		actionMap.put(LocationAttributeWidget.AJAX_KEY, LocationAttributeWidget.class);
	}
}
