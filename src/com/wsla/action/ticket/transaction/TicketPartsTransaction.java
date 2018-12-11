package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.Convert;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketPartsTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages parts related micro-transactions
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 19, 2018
 * @updates:
 ****************************************************************************/

public class TicketPartsTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "parts";
	
	/**
	 * 
	 */
	public TicketPartsTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketPartsTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("isApproved")) {
				setApproval(req);
			} else {
				submitForApproval(req);
			}
		} catch (Exception e) {
			log.error("Unable to submit parts for approval", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * TODO: This is temporary to wire in the status change. Add in the required code to submit for approval.
	 * Submits the parts for approval and updates the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	private void submitForApproval(ActionRequest req) throws DatabaseException {
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.CAS_PARTS_REQUESTED, LedgerSummary.CAS_REQUESTED_PARTS.summary, null);

		// Build next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), params, false);
	}

	/**
	 * Sets the approval status of the requested parts
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	private void setApproval(ActionRequest req) throws DatabaseException {
		boolean isApproved = Convert.formatBoolean(req.getParameter("isApproved"));
		if (!isApproved)
			return; // Check with WSLA
		
		// Set the approval status for the parts request
		setPartsStatus(req, StatusCode.CAS_PARTS_ORDERED, LedgerSummary.PARTS_REQUEST_REVIEWED.summary, null);
	}

	/**
	 * Sets the given status data for the ticket's parts order
	 * 
	 * @param req
	 * @param sc
	 * @param summary
	 * @param params
	 * @throws DatabaseException
	 */
	private void setPartsStatus(ActionRequest req, StatusCode sc, String summary, Map<String, Object> params) throws DatabaseException {
		// Set the given status
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), sc, summary, null);
		buildNextStep(ledger.getStatusCode(), params, false);
	}
}

