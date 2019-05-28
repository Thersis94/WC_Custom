package com.wsla.action.ticket.transaction;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;

// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketScheduleVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketScheduleAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes to the schedule feature
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Oct 24, 2018
 * @updates:
 ****************************************************************************/

public class TicketScheduleTransaction extends BaseTransactionAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "schedule";
	
	/**
	 * Indicates that a scheduled item is being picked up or dropped off
	 */
	public static final String REQ_COMPLETE = "complete";
	
	/**
	 * Ticket schedule pre-repair indicator
	 */
	public static final String PRE_REPAIR = "preRepair";
	
	/**
	 * Ticket schedule post repair indicatior
	 */
	public static final String POST_REPAIR = "postRepair";
	
	/**
	 * 
	 */
	public TicketScheduleTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketScheduleTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug(" build called");
		try {
			TicketScheduleVO ts;
			
			if (req.hasParameter(REQ_COMPLETE))
				ts = completeSchedule(req);
			else
				ts = saveSchedule(req);

			putModuleData(ts);
			
		} catch (DatabaseException e) {
			log.error("Unable to save ticket schedule", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Merges the details of the ticket assignments that the given schedule is using
	 * 
	 * @param ts
	 * @throws DatabaseException 
	 */
	private void mergeTicketAssignments(TicketScheduleVO ts) throws DatabaseException {
		TicketEditAction tea = new TicketEditAction(getAttributes(), getDBConnection());
		
		try {
			List<TicketAssignmentVO> taList = tea.getAssignments(ts.getTicketId());
			tea.populateScheduleAssignments(Arrays.asList(ts), taList);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new DatabaseException(e);
		}
	}
	
	/**
	 * Saves/edits a ticket schedule record when equipment is scheduled or
	 * re-schedule for drop-off or pick-up.
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public TicketScheduleVO saveSchedule(ActionRequest req) throws DatabaseException {
		// Get the DB Processor & user
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		UserVO user = null;
		// Get the WSLA User
		if(req.hasParameter("userId") && req.hasParameter("publicUserForm")) {
			//coming in from the public user portal the id is on the form.
			user = new UserVO();
			user.setUserId(req.getParameter("userId"));
		}else {
			//coming in from he secure wsla portal the user object is available
			user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		}
		
		// Build the Ticket Schedule data and save
		TicketScheduleVO ts = new TicketScheduleVO(req);
		modifyNotes(ts);
		try {
			db.save(ts);
		} catch (InvalidDataException e) {
			throw new DatabaseException(e);
		}
		
		// Change the status & build next step
		boolean isPreRepair = PRE_REPAIR.equals(ts.getRecordTypeCode());
		TicketLedgerVO ledger = changeStatus(ts.getTicketId(), user.getUserId(), isPreRepair ? StatusCode.PENDING_PICKUP : StatusCode.DELIVERY_SCHEDULED, LedgerSummary.SCHEDULE_TRANSFER.summary, null);
		buildNextStep(ledger.getStatusCode(), null, false);
		
		// Merge assignment data into the VO
		mergeTicketAssignments(ts);
		
		return ts;
	}
	
	/**
	 * Handles completion of a scheduled pick-up or drop-off when the equipment
	 * is transfered to another party. This also logs a ledger entry.
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public TicketScheduleVO completeSchedule(ActionRequest req) throws DatabaseException {
		TicketScheduleVO ts = new TicketScheduleVO(req);
		ts.setUpdateDate(new Date());
		modifyNotes(ts);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		boolean isRefund = req.getBooleanParameter("isRefund");
		
		if(user == null) {
			user = new UserVO();
			user.setUserId(req.getStringParameter("userId"));
		}
		
		// Add in the details of the ticket assignments that this schedule is using
		mergeTicketAssignments(ts);
		
		// Make sure the unit location is current (requires the assignment data to set)
		UnitLocation location = updateUnitLocation(ts);
		
		// Change the status
		boolean isPreRepair = PRE_REPAIR.equals(ts.getRecordTypeCode());
		String summary = LedgerSummary.SCHEDULE_TRANSFER_COMPLETE.summary + " - " + ts.getNotesText();
		TicketLedgerVO ledger = changeStatus(ts.getTicketId(), user.getUserId(), isPreRepair ? StatusCode.PICKUP_COMPLETE : StatusCode.DELIVERY_COMPLETE, summary, location);
		ts.setLedgerEntryId(ledger.getLedgerEntryId());

		// Save the transfer completion data
		saveCompletion(ts);
		
		//if this is  a refund process the disposition
		if(ledger.getStatusCode() == StatusCode.PICKUP_COMPLETE && isRefund) {
			log.debug("pick up complete and is refund");
			RefundReplacementTransaction rrt = new RefundReplacementTransaction();
			rrt.setActionInit(actionInit);
			rrt.setAttributes(getAttributes());
			rrt.setDBConnection(getDBConnection());
			
			//Instantiate the refrep transaction and call process disposition code,
			rrt.processDisposition(ts.getTicketId(), req);
			
		}
		
		// When the post repair transfer is complete, the ticket is finished (closed).
		// Add an additional ledger entry & status change to denote this.  
		if (ledger.getStatusCode() == StatusCode.DELIVERY_COMPLETE) {
			ledger = changeStatus(ts.getTicketId(), user.getUserId(), StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary, null);
		}
		
		// Build the next step
		buildNextStep(ledger.getStatusCode(), null, ledger.getStatusCode() == StatusCode.CLOSED);
		
		return ts;
	}
	
	/**
	 * Saves the completion data to the schedule record
	 * 
	 * @param ts
	 */
	private void saveCompletion(TicketScheduleVO ts) {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Create the SQL for updating the record
		StringBuilder sql = new StringBuilder(110);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket_schedule " );
		sql.append("set signer_nm = ?, signature_txt = ?, product_validated_flg = ?, ");
		sql.append("notes_txt = ?, ledger_entry_id = ?, complete_dt = ?, update_dt = ? ");
		sql.append("where ticket_schedule_id = ? ");
		log.debug(sql);
		
		// Set the fields we are updating from
		List<String> fields = Arrays.asList("signer_nm", "signature_txt", "product_validated_flg", "notes_txt", "ledger_entry_id", "complete_dt", "update_dt", "ticket_schedule_id");

		// Save the updates to the record
		try {
			db.executeSqlUpdate(sql.toString(), ts, fields);
		} catch (DatabaseException e1) {
			log.error("Could not update the ticket schedule completion",e1);
		}
	}
	
	/**
	 * Modify notes per given requirements:
	 * 	 - Store any/all notes in one field.
	 * 	 - Notes are additive, can not be edited once saved.
	 *   - Date and time should be added to note.
	 * 
	 * @param ts
	 * @throws DatabaseException 
	 */
	protected void modifyNotes(TicketScheduleVO ts) throws DatabaseException {
		// Get previous notes (if any)
		TicketScheduleVO prevTs = new TicketScheduleVO();
		prevTs.setTicketScheduleId(ts.getTicketScheduleId());
		if (!StringUtil.isEmpty(ts.getTicketScheduleId())) {
			try {
				DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
				db.getByPrimaryKey(prevTs);
			} catch (InvalidDataException e) {
				throw new DatabaseException(e);
			}
		}
		
		String newNotes = StringUtil.checkVal(ts.getNotesText());
		String prevNotes = StringUtil.checkVal(prevTs.getNotesText());

		// Prepend previous notes if they exist
		StringBuilder note = new StringBuilder(newNotes.length() + prevNotes.length() + 1);
		if (!StringUtil.isEmpty(prevNotes)) {
			note.append(prevNotes).append(StringUtil.isEmpty(newNotes) ? "" : StringUtil.join(System.lineSeparator(), "-----", System.lineSeparator()));
		}
		
		// Append the new note
		if (!StringUtil.isEmpty(newNotes)) {
			note.append(StringUtil.join(Convert.formatDate(new Date(), Convert.DATETIME_DASH_PATTERN), ":", System.lineSeparator(), newNotes));
		}
		
		ts.setNotesText(note.toString());
	}
	
	/**
	 * Set's the ticket's unit location based on the schedule assignments and schedule record type
	 * 
	 * @param ts
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private UnitLocation updateUnitLocation(TicketScheduleVO ts) throws DatabaseException {
		String unitLoc = ts.getRecordTypeCode().equals(PRE_REPAIR) ? ts.getCasLocation().getTypeCode().toString() : ts.getOwnerLocation().getTypeCode().toString();
		UnitLocation location = UnitLocation.valueOf(unitLoc);
		
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ts.getTicketId());
		ticket.setUnitLocation(location);

		TicketTransaction tt = new TicketTransaction(getAttributes(), getDBConnection());
		tt.updateUnitLocation(ticket);
		
		return location;
	}
}

