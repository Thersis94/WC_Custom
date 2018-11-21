package com.wsla.action.ticket.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.wsla.action.BasePortalAction;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: DiagnosticTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages adding a new diagnostic on the ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 24, 2018
 * @updates:
 ****************************************************************************/

public class DiagnosticTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "diagnostic";
	
	/**
	 * 
	 */
	public DiagnosticTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DiagnosticTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("isRepairable")) {
				setRepairable(req);
			} else {
				saveDiagnosticRun(req);
			}
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Stores the user information
	 * @param req
	 * @return
	 * @throws DatabaseException
	 */
	public void saveDiagnosticRun(ActionRequest req) throws DatabaseException {
		
		// Get the WSLA User & ticket id
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		String ticketId = req.getParameter(TicketEditAction.TICKET_ID);
		
		// Add a ledger entry, change the status
		TicketLedgerVO ledger = changeStatus(ticketId, user.getUserId(), StatusCode.CAS_IN_DIAG, LedgerSummary.RAN_DIAGNOSTIC.summary, null);

		// Build next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), new BasePortalAction().getResourceBundle(req), params, false);
		
		DiagnosticRunVO dr = new DiagnosticRunVO(req);
		
		try {
			TicketOverviewAction toa = new TicketOverviewAction(attributes, dbConn);
			toa.saveDiagnosticRun(dr);
		} catch (InvalidDataException e) {
			throw new DatabaseException(e);
		}
	}
	
	/**
	 * Sets the repairable status of the equipment, and moves the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	private void setRepairable(ActionRequest req) throws DatabaseException {
		boolean isRepairable = Convert.formatBoolean(req.getParameter("isRepairable"));
		ResourceBundle bundle = new BasePortalAction().getResourceBundle(req);
		
		// Set the repairable status
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), isRepairable ? StatusCode.REPAIRABLE : StatusCode.UNREPAIRABLE, LedgerSummary.DIAGNOSTIC_COMPLETED.summary, null);
		buildNextStep(ledger.getStatusCode(), bundle, new HashMap<>(), false);
	}
}

