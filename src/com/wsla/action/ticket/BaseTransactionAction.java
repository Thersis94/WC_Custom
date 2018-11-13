package com.wsla.action.ticket;

import java.util.Map;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.workflow.WorkflowLookupUtil;
import com.siliconmtn.workflow.data.WorkflowMessageVO;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.util.MessageParser;
import com.smt.sitebuilder.util.ParseException;
import com.smt.sitebuilder.util.WorkflowSender;
import com.smt.sitebuilder.util.MessageParser.MessageType;
import com.wsla.action.BasePortalAction;
import com.wsla.common.WSLAConstants.WorkflowLookup;
import com.wsla.data.ticket.NextStepVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.StatusCodeVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.util.NotificationWorkflowModule;

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
	
	protected NextStepVO nextStep;

	public BaseTransactionAction() {
		super();
		nextStep = new NextStepVO();
	}

	/**
	 * @param actionInit
	 */
	public BaseTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
		nextStep = new NextStepVO();
	}

	/**
	 * Updates the ticket to the new status.
	 * 
	 * @param ticketId
	 * @param status
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void changeStatus(String ticketId, String userId, StatusCode newStatus, String summary, UnitLocation location) throws InvalidDataException, DatabaseException {
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ticketId);
		
		// Save the new status
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		dbp.getByPrimaryKey(ticket);
		ticket.setStatusCode(newStatus);
		dbp.save(ticket);
		
		// Send the notification and add the ledger entry
		processNotification(ticketId, userId, newStatus);
		addLedger(ticketId, userId, newStatus, summary, location);
	}

	/**
	 * Calls an appropriate notification workflow for the given status.
	 * Uses data from the ticket to localize the notifications.
	 * 
	 * @param ticketId
	 * @param status
	 */
	public void processNotification(String ticketId, String userId, StatusCode status) {
		// Lookup the workflow Id
		WorkflowLookupUtil wlu = new WorkflowLookupUtil(getDBConnection());
		String workflowId = wlu.getWorkflowFromLookupId(WorkflowLookup.WSLA_NOTIFICATION.name()).getWorkflowId();
		
		// Setup the workflow message
		WorkflowMessageVO wmv = new WorkflowMessageVO(workflowId);
		wmv.addAllParameters(getAttributes());
		wmv.addParameter(NotificationWorkflowModule.TICKET_ID, ticketId);
		wmv.addParameter(NotificationWorkflowModule.USER_ID, userId);
		wmv.addParameter(NotificationWorkflowModule.STATUS_CODE, status);
		
		// Send the workflow to the engine
		WorkflowSender ws = new WorkflowSender(getAttributes());
		ws.sendWorkflow(wmv);
	}
	
	/**
	 * Adds a ledger entry for the given ticket. Determination is made as to
	 * which billable code to use here based on the status.
	 * 
	 * @param ticketId
	 * @param status
	 * @param summary
	 * @return ledgerId
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public String addLedger(String ticketId, String userId, StatusCode status, String summary, UnitLocation location) throws InvalidDataException, DatabaseException {
		// Create a new ledger record
		TicketLedgerVO ledger = new TicketLedgerVO();
		ledger.setDispositionBy(userId);
		ledger.setTicketId(ticketId);
		ledger.setStatusCode(status);
		ledger.setSummary(summary);
		//ledger.setUnitLocation(location)
		
		// Get status data to be added to the ledger
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		StatusCodeVO sc = new StatusCodeVO();
		sc.setStatusCode(status.name());
		dbp.getByPrimaryKey(sc);
		//ledger.setBillableActivityCode(sc.getBillableActivityCode())
		
		// Add the ledger entry
		BasePortalAction bpa = new BasePortalAction(getDBConnection(), getAttributes());
		bpa.addLedger(ledger);
		
		return ledger.getLedgerEntryId();
	}
	
	/**
	 * Returns the next step for the user to take after this transaction completes
	 */
	public NextStepVO getNextStep() {
		return nextStep;
	}
	
	/**
	 * Builds the next step that will need to take place by the user after this transaction
	 * 
	 * @param status
	 * @param bundle
	 * @param params
	 * @param needsReload
	 * @throws InvalidDataException
	 */
	public void buildNextStep(StatusCode status, ResourceBundle bundle, Map<String, Object> params, boolean needsReload) throws InvalidDataException {
		StatusCodeVO sc = new StatusCodeVO();
		sc.setStatusCode(status.name());
		
		try {
			DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
			dbp.getByPrimaryKey(sc);

			// Create the next step data
			nextStep = new NextStepVO(status, bundle);
			nextStep.setButtonUrl(MessageParser.parse(sc.getNextStepUrl(), params, sc.getNextStepUrl(), MessageType.TEXT));
			nextStep.setButtonName(sc.getStatusName());
			nextStep.setNeedsReloadFlag(needsReload);
		} catch (InvalidDataException | DatabaseException | ParseException e) {
			throw new InvalidDataException(e);
		}
	}
}