package com.wsla.action.ticket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.WorkflowLookupUtil;
import com.siliconmtn.workflow.data.WorkflowMessageVO;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.util.MessageParser;
import com.smt.sitebuilder.util.ParseException;
import com.smt.sitebuilder.util.WorkflowSender;
import com.smt.sitebuilder.util.MessageParser.MessageType;
import com.wsla.action.BasePortalAction;
import com.wsla.action.admin.StatusCodeAction;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.common.WSLAConstants.WorkflowSlug;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.WarrantyBillableVO;
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
	
	private NextStepVO nextStep;

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
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public BaseTransactionAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/**
	 * Updates the ticket to the new status.
	 * 
	 * @param ticketId
	 * @param status
	 * @throws DatabaseException 
	 */
	public TicketLedgerVO changeStatus(String ticketId, String userId, StatusCode newStatus, String summary, UnitLocation location) throws DatabaseException {
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ticketId);
		
		// Save the new status & location
		try {
			dbp.getByPrimaryKey(ticket);
			ticket.setStatusCode(newStatus);
			
			//if the unit is DECOMMISSIONED stop any status change unit location updates 
			if( UnitLocation.DECOMMISSIONED.equals( ticket.getUnitLocation( ))){
				location = UnitLocation.DECOMMISSIONED;

				ProductSerialNumberVO psvo = ticket.getProductSerial();
				psvo.setProductSerialId(ticket.getProductSerialId());
				dbp.getByPrimaryKey(psvo);
				psvo.setDisposeFlag(1);

				dbp.save(psvo);
				
			}
			
			ticket.setUnitLocation((location != null) ? location : ticket.getUnitLocation());
			
			dbp.save(ticket);
		} catch (InvalidDataException e) {
			throw new DatabaseException(e);
		}
		
		// Get the status data
		StatusCodeVO sc = new StatusCodeVO();
		sc.setStatusCode(newStatus.name());
		try {
			dbp.getByPrimaryKey(sc);
		} catch (InvalidDataException e) {
			log.error("Could not get status data");
		}
		
		// Populate the email data
		Map<String,Object> emailData = new HashMap<>();
		emailData.put(NotificationWorkflowModule.TICKET_ID_TEXT, ticket.getTicketIdText());
		emailData.put(NotificationWorkflowModule.USER_ID, userId);
		emailData.put(NotificationWorkflowModule.TICKET_ID, ticket.getTicketId());
		emailData.put("soNumber", ticket.getTicketIdText());
		emailData.put("statusCd", newStatus.name());
		emailData.put("groupStatusCode", sc.getGroupStatusCode().name());
		emailData.put("statusName", sc.getStatusName());
		
		// Send the notification and add the ledger entry
		processNotification(ticket.getTicketIdText(), userId, newStatus, emailData);
		return addLedger(ticketId, userId, newStatus, summary, location);
	}

	/**
	 * Calls an appropriate notification workflow for the given status.
	 * Uses data from the ticket to localize the notifications.
	 * 
	 * @param ticketIdText
	 * @param status
	 */
	public void processNotification(String ticketIdText, String userId, StatusCode status, Map<String,Object> emailData) {
		// Lookup the workflow Id
		WorkflowLookupUtil wlu = new WorkflowLookupUtil(getDBConnection());
		String workflowId = wlu.getWorkflowFromSlug(WorkflowSlug.WSLA_NOTIFICATION.name()).getWorkflowId();
		
		// Exclude anything that may not be serializable, a requirement of the message sender
		Map<String, Object> wfAttributes = new HashMap<>();
		for(Map.Entry<String, Object> item : getAttributes().entrySet()) {
			if(!(item.getValue() instanceof String))
				continue;
			
			wfAttributes.put(item.getKey(), item.getValue());
		}
		
		// Setup the workflow message
		WorkflowMessageVO wmv = new WorkflowMessageVO(workflowId);
		wmv.addAllParameters(wfAttributes);
		wmv.addParameter(NotificationWorkflowModule.TICKET_ID_TEXT, ticketIdText);
		wmv.addParameter(NotificationWorkflowModule.USER_ID, userId);
		wmv.addParameter(NotificationWorkflowModule.STATUS_CODE, status);
		wmv.addParameter(NotificationWorkflowModule.EMAIL_DATA, emailData);
		
		// Send the workflow to the engine
		WorkflowSender ws = new WorkflowSender(getAttributes());
		ws.sendWorkflow(wmv);
	}
	
	/**
	 * Adds a ledger entry for the given ticket. Determination is made as to
	 * which billable code to use here based on the status.
	 * 
	 * Uses the default billable amount for the status' billable code.
	 * 
	 * @param ticketId
	 * @param userId
	 * @param status
	 * @param summary
	 * @param location
	 * @return
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(String ticketId, String userId, StatusCode status, String summary, UnitLocation location) throws DatabaseException {
		return addLedger(ticketId, userId, status, summary, location, new WarrantyBillableVO());
	}
	
	/**
	 * Adds a ledger entry for the given ticket. Determination is made as to
	 * which billable code to use here based on the status.
	 * 
	 * Overloaded to take a different amount than what is specified on the billable code.
	 * 
	 * @param ticketId
	 * @param userId
	 * @param status
	 * @param summary
	 * @param location
	 * @param billableAmt
	 * @return
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(String ticketId, String userId, StatusCode status, String summary, UnitLocation location, Double billableAmt) throws DatabaseException {
		WarrantyBillableVO billableData = new WarrantyBillableVO();
		billableData.setInvoiceAmount(billableAmt);
		return addLedger(ticketId, userId, status, summary, location, billableData);
	}

	/**
	 * Adds a ledger entry for the given ticket. Determination is made as to
	 * which billable code to use here based on the status.
	 * 
	 * Overloaded to take a different amount than what is specified on the billable code.
	 * 
	 * @param ticketId
	 * @param userId
	 * @param status
	 * @param summary
	 * @param location
	 * @param billableAmt
	 * @return
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(String ticketId, String userId, StatusCode status, String summary, UnitLocation location, WarrantyBillableVO billableData) throws DatabaseException {
		// Create a new ledger record
		if (billableData == null) billableData = new WarrantyBillableVO();
		TicketLedgerVO ledger = new TicketLedgerVO();
		ledger.setDispositionBy(userId);
		ledger.setTicketId(ticketId);
		ledger.setStatusCode(status);
		ledger.setSummary(summary);
		ledger.setUnitLocation(location);
		ledger.setBillableAmtNo(Convert.formatDouble(billableData.getInvoiceAmount()));
		ledger.setBillableActivityCode(billableData.getBillableActivityCode());
		
		// Get status billable data to be added to the ledger
		if (status != null) {
			try {
				if (StringUtil.isEmpty(billableData.getBillableActivityCode())) 
					billableData = getBillableData(ticketId, status);
				
				ledger.setBillableActivityCode(billableData.getBillableActivityCode());
				
				// If we aren't overriding the default amount with a passed in value,
				// then just use the default amount.
				if (billableData.getInvoiceAmount() > 0) {
					ledger.setBillableAmtNo(billableData.getInvoiceAmount());
				}
			} catch (SQLException e) {
				throw new DatabaseException(e);
			}
		}
		
		// Add the ledger entry
		return addLedger(ledger);
	}
	
	/**
	 * Adds a ledger entry. Overloaded to directly add the ledger entry with
	 * pre-populated data.
	 * 
	 * @param ledger
	 * @return
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(TicketLedgerVO ledger) throws DatabaseException {
		try {
			BasePortalAction bpa = new BasePortalAction(getDBConnection(), getAttributes());
			bpa.addLedger(ledger);
		} catch (InvalidDataException e) {
			throw new DatabaseException(e);
		}
		
		return ledger;
	}
	
	/**
	 * Get's the billable data for the ticket's status
	 * 
	 * @return
	 * @throws SQLException 
	 */
	private WarrantyBillableVO getBillableData(String ticketId, StatusCode status) throws SQLException {
		StringBuilder sql = new StringBuilder(416);
		sql.append(DBUtil.SELECT_CLAUSE).append("ts.billable_activity_cd, b.invoice_amount_no");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_status ts");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(");
		sql.append(DBUtil.SELECT_CLAUSE).append("wb.invoice_amount_no, wb.billable_activity_cd");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_warranty pw on t.product_warranty_id = pw.product_warranty_id");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_warranty_billable_xr wb on pw.warranty_id = wb.warranty_id");
		sql.append(DBUtil.WHERE_CLAUSE).append("t.ticket_id = ?");
		sql.append(") as b on ts.billable_activity_cd = b.billable_activity_cd");
		sql.append(DBUtil.WHERE_CLAUSE).append("ts.status_cd = ?");
		log.debug(sql+ticketId + "|"+  status.name());
		
		WarrantyBillableVO billableData = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			ps.setString(2, status.name());
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				billableData = new WarrantyBillableVO(rs);
			}
		}
		
		return billableData == null ? new WarrantyBillableVO() : billableData;
	}
	
	/**
	 * Returns the next step for the user to take after this transaction completes
	 */
	public NextStepVO getNextStep() {
		return nextStep;
	}
	
	/**
	 * Sets the next step from a pre-built next step
	 * 
	 * @param nextStep
	 */
	public void setNextStep(NextStepVO nextStep) {
		this.nextStep = nextStep;
	}
	
	/**
	 * Builds the next step that will need to take place by the user after this transaction
	 * 
	 * @param status
	 * @param params
	 * @param needsReload
	 * @throws DatabaseException
	 */
	public void buildNextStep(StatusCode status, Map<String, Object> params, boolean needsReload) throws DatabaseException {
		StatusCodeVO sc = new StatusCodeVO();
		sc.setStatusCode(status.name());
		
		try {
			StatusCodeAction sca = new StatusCodeAction(getDBConnection(), getAttributes());
			List<StatusCodeVO> statusList = sca.getStatusCodes(null, null, sc.getStatusCode());
			sc = !statusList.isEmpty() ? statusList.get(0) : sc;

			// Create the next step data
			nextStep = new NextStepVO(status);
			
			if (!StringUtil.isEmpty(sc.getNextStepUrl()) && params != null && !params.isEmpty()) {
				nextStep.setButtonUrl(MessageParser.parse(sc.getNextStepUrl(), params, StringUtil.removeNonAlphaNumeric(sc.getNextStepUrl()), MessageType.TEXT));
			} else {
				nextStep.setButtonUrl(sc.getNextStepUrl());
			}

			nextStep.setButtonKeyCode(sc.getNextStepBtnKeyCode());
			nextStep.setStatusName(sc.getStatusName());
			nextStep.setStatusCode(sc.getStatusCode());
			nextStep.setGroupStatusCode(sc.getGroupStatusCode().name());
			nextStep.setRoleName(sc.getRoleName());
			nextStep.setNeedsReloadFlag(needsReload);
			
			for (WSLARole role : sc.getAuthorizedRole()) {
				nextStep.addAuthorizedRole(role.getRoleId());
			}
		} catch (ParseException e) {
			throw new DatabaseException(e);
		}
	}
}