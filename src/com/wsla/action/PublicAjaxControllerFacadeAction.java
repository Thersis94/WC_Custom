package com.wsla.action;

import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
// WSLA Libs
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketLedgerAction;


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
 * @author Ryan Riker
 * @version 3.0
 * @since Nov 17, 2018
 * @updates:
 ****************************************************************************/
public class PublicAjaxControllerFacadeAction extends AjaxControllerFacadeAction {

	/**
	 * 
	 */
	public PublicAjaxControllerFacadeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PublicAjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Type: " + req.getParameter("type") + "|" + req.getParameter("comment"));
		List<String> publicActions = new ArrayList<>(); 
		publicActions.add(TicketEditAction.AJAX_KEY);
		publicActions.add(TicketLedgerAction.AJAX_KEY);
		
		if (publicActions.contains(StringUtil.checkVal(req.getParameter("type")))) super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Building: " + req.getParameter("type"));
		List<String> publicActions = new ArrayList<>(); 
		publicActions.add(TicketEditAction.AJAX_KEY);
		publicActions.add(TicketLedgerAction.AJAX_KEY);
		
		if (publicActions.contains(StringUtil.checkVal(req.getParameter("type")))) super.build(req);
	}
}
