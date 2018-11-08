package com.wsla.action.ticket;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.StatusCode;

/****************************************************************************
 * <b>Title</b>: BaseTransactionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Base class that accommodates common functionality
 * needed by all micro transactions.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 8, 2018
 * @updates:
 ****************************************************************************/
public class BaseTransactionAction extends SBActionAdapter {

	public BaseTransactionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BaseTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Calls an appropriate notification workflow for the given status.
	 * Uses data from the ticket to localize the notifications.
	 * 
	 * @param ticketId
	 * @param status
	 */
	public void processNotification(String ticketId, StatusCode status) {
		
	}
	
	/**
	 * Updates the ticket to the new status.
	 * 
	 * @param ticketId
	 * @param status
	 */
	public void changeStatus(String ticketId, StatusCode newStatus) {
		
	}

	/**
	 * Adds a ledger entry for the given ticket. Determination is made as to
	 * which billable code to use here based on the status.
	 * 
	 * @param ticketId
	 * @param status
	 * @param summary
	 */
	public void addLedger(String ticketId, StatusCode status, String summary) {
		
	}
}