package com.wsla.action.ticket;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

//WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;

// WSLA libs
import com.wsla.action.ticket.transaction.TicketAssetAction;

/****************************************************************************
 * <b>Title</b>: TicketTransactionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the small transaction updates to the service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 22, 2018
 * @updates:
 ****************************************************************************/

public class TicketTransactionAction extends FacadeActionAdapter {
	/**
	 * Key for the AMID Registration to utilize when calling this class
	 */
	public static final String AMID_KEY = "ticketTransaction";
	
	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "transactionType";
	
	/**
	 * 
	 */
	public TicketTransactionAction() {
		super();
		assignActionMap();
	}

	/**
	 * @param actionInit
	 */
	public TicketTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
		assignActionMap();
	}
	
	/**
	 * Assigns the keys and classes for the facade
	 */
	public void assignActionMap() {
		actionMap.put(TicketAssetAction.AJAX_KEY, TicketAssetAction.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ActionInterface action = loadActionByType(req.getParameter(SELECT_KEY));
		action.build(req);
	}
}

