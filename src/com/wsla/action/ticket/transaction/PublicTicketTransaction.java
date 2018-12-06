package com.wsla.action.ticket.transaction;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.ticket.TicketTransactionAction;

/****************************************************************************
 * <b>Title</b>: PublicTicketTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Use to open up access to areas of the code that should be 
 * 	presentable to the public.  
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Nov 26, 2018
 * @updates:
 ****************************************************************************/
public class PublicTicketTransaction extends TicketTransactionAction {

	private List<String> publicActions = new ArrayList<>();
	
	/**
	 * 
	 */
	public PublicTicketTransaction() {
		super();
		assignPublicActionMap();
	}

	/**
	 * @param actionInit
	 */
	public PublicTicketTransaction(ActionInitVO actionInit) {
		super(actionInit);
		assignPublicActionMap();
	}
	/**
	 * Assigns the keys and classes for the facade
	 */
	private void assignPublicActionMap() {
		publicActions.add(TicketAssetTransaction.AJAX_KEY);
		publicActions.add(TicketTransaction.AJAX_KEY);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Type: " + req.getParameter("type") + "|" + req.getParameter("comment"));
		if (publicActions.contains(StringUtil.checkVal(req.getParameter("type")))) super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Building: " + req.getParameter("type"));
		if (publicActions.contains(StringUtil.checkVal(req.getParameter("type")))) super.build(req);
	}

}
