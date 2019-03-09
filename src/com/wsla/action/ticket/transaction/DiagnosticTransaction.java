package com.wsla.action.ticket.transaction;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.admin.LogisticsAction;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.DiagnosticTicketVO;
import com.wsla.data.ticket.DispositionCode;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
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
			if (req.hasParameter("isDisposition")) {
				saveDisposition(req);
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
		
		// Save the diagnostic run
		DiagnosticRunVO dr = new DiagnosticRunVO(req);
		try {
			TicketOverviewAction toa = new TicketOverviewAction(attributes, dbConn);
			toa.saveDiagnosticRun(dr);
		} catch (InvalidDataException e) {
			throw new DatabaseException(e);
		}
		
		// Check to see if the issue was resolved, add any required ticket status ledger entries if true
		for (DiagnosticTicketVO diag : dr.getDiagnostics()) {
			if (Convert.formatBoolean(diag.getIssueResolvedFlag())) {
				changeStatus(ticketId, user.getUserId(), StatusCode.PROBLEM_RESOLVED, null, null);
				
				try {
					TicketDataTransaction tdt = new TicketDataTransaction(getDBConnection(), getAttributes());
					tdt.saveDataAttribute(ticketId, "attr_issueResolved", "1", true);
				} catch (SQLException e) {
					throw new DatabaseException(e);
				}
				
				break;
			}
		}

		// Add a ledger entry, change the status
		TicketLedgerVO ledger = changeStatus(ticketId, user.getUserId(), StatusCode.CAS_IN_DIAG, LedgerSummary.RAN_DIAGNOSTIC.summary, null);
		
		// Build next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), params, false);
	}
	
	/**
	 * Sets the repair status of the equipment, and moves the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void saveDisposition(ActionRequest req) throws DatabaseException, InvalidDataException {
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketDataVO td = new TicketDataVO(req);
		
		// Update the status
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		DispositionCode disposition = DispositionCode.valueOf(td.getValue());
		TicketLedgerVO ledger = changeStatus(td.getTicketId(), user.getUserId(), disposition.getStatus(), disposition.getLedgerSummary(), null);
		
		// Once repaired, create an additional ledger entry for pending return.
		// If not repairable, change status and save the user's notes.
		if (disposition == DispositionCode.REPAIRED) {
			ledger = changeStatus(td.getTicketId(), user.getUserId(), StatusCode.PENDING_UNIT_RETURN, null, null);
		
		} else if (disposition == DispositionCode.NONREPAIRABLE) {
			changeStatus(td.getTicketId(), user.getUserId(), StatusCode.CAS_REPAIR_COMPLETE, null, null);
			
			StatusCode nextStatus = StatusCode.valueOf(req.getParameter("nonRepairType"));
			String notes = req.getParameter("attr_partsNotes", "");
			String summary = StringUtil.join(LedgerSummary.REPAIR_STATUS_CHANGED.summary, ": ", notes);
			ledger = changeStatus(td.getTicketId(), user.getUserId(), nextStatus, summary, null);
			
			TicketDataVO notesData = new TicketDataVO(req);
			notesData.setAttributeCode("attr_partsNotes");
			notesData.setValue(notes);
			dbp.save(notesData);
			
			// Check for any pending shipments and cancel them
			new LogisticsAction(getAttributes(), getDBConnection()).cancelPendingShipments(td.getTicketId());
		}
		
		// Build the next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), params, false);
		
		// Save the ticket data record
		td.setLedgerEntryId(ledger.getLedgerEntryId());
		dbp.save(td);
	}
}

