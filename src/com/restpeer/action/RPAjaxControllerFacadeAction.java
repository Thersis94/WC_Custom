package com.restpeer.action;

import com.restpeer.action.admin.UserAction;

import java.util.ArrayList;
import java.util.List;

import com.restpeer.action.account.LocationProductScheduleAction;
import com.restpeer.action.account.MemberLocationUserAction;
import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Core
import com.smt.sitebuilder.action.commerce.AjaxControllerFacadeAction;
import com.smt.sitebuilder.action.commerce.product.EcommProductAction;
import com.smt.sitebuilder.action.commerce.product.LocationProductAction;
import com.smt.sitebuilder.action.dealer.DealerAttributeXrAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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

	List<String> publicAllowed = new ArrayList<>();
	
	/**
	 * 
	 */
	public RPAjaxControllerFacadeAction() {
		super();
		setPublicAllowed();
	}

	/**
	 * @param actionInit
	 */
	public RPAjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		setPublicAllowed();
	}

	/**
	 * Loads the mapping to the various ajax calls
	 */
	@Override
	protected void loadTypes() {
		super.loadTypes();
		actionMap.put(DEFAULT_TYPE, RPSelectLookupAction.class);
		actionMap.put(MemberLocationUserAction.AJAX_KEY, MemberLocationUserAction.class);
		actionMap.put(UserAction.AJAX_KEY, UserAction.class);
		actionMap.put(LocationProductScheduleAction.AJAX_KEY, LocationProductScheduleAction.class);
	}
	
	/**
	 * Sets which actions are allowed public access
	 */
	protected void setPublicAllowed() {
		publicAllowed.add(DEFAULT_TYPE);
		publicAllowed.add(EcommProductAction.AJAX_KEY);
		publicAllowed.add(DealerAttributeXrAction.AJAX_KEY);
		publicAllowed.add(LocationProductAction.AJAX_KEY);
		publicAllowed.add(LocationProductScheduleAction.AJAX_KEY);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if ("0".equals(getRoleId(req))) {
			if (publicAllowed.indexOf(req.getParameter(SELECTOR_KEY)) != -1) {
				super.retrieve(req);
				return;
			} else {
				throw new ActionException("User is not allowed to access the ajax controller retrieve for this action.");
			}
		}
		
		super.retrieve(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if ("0".equals(getRoleId(req))) {
			throw new ActionException("User is not allowed to access the ajax controller build.");
		}
		
		super.build(req);
	}
	
	/**
	 * Gets the role for the request.
	 * 
	 * @param req
	 * @return
	 */
	private String getRoleId(ActionRequest req) {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) return "0";
		
		return role.getRoleId();
	}
}
