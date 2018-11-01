package com.wsla.action.ticket;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

//WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.wsla.action.ticket.transaction.DiagnosticTransaction;
import com.wsla.action.ticket.transaction.ProviderLocationTransaction;
// WSLA libs
import com.wsla.action.ticket.transaction.TicketAssetTransaction;
import com.wsla.action.ticket.transaction.TicketCommentTransaction;
import com.wsla.action.ticket.transaction.UserTransaction;
import com.wsla.action.ticket.transaction.TicketScheduleTransaction;

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
		actionMap.put(TicketAssetTransaction.AJAX_KEY, TicketAssetTransaction.class);
		actionMap.put(UserTransaction.AJAX_KEY, UserTransaction.class);
		actionMap.put(ProviderLocationTransaction.AJAX_KEY, ProviderLocationTransaction.class);
		actionMap.put(DiagnosticTransaction.AJAX_KEY, DiagnosticTransaction.class);
		actionMap.put(TicketScheduleTransaction.AJAX_KEY, TicketScheduleTransaction.class);
		actionMap.put(TicketCommentTransaction.AJAX_KEY, TicketCommentTransaction.class);
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

