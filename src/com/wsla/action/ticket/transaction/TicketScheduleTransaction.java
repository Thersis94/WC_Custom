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
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.TicketEditAction;
// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
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

public class TicketScheduleTransaction extends SBActionAdapter {
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
		try {
			TicketScheduleVO ts;
			
			if (req.hasParameter(REQ_COMPLETE))
				ts = completeSchedule(req);
			else
				ts = saveSchedule(req);

			// Add in the details of the ticket assignments that this schedule is using
			TicketEditAction tea = new TicketEditAction(getAttributes(), getDBConnection());
			List<TicketScheduleVO> tsList = tea.getSchedule(null, ts.getTicketScheduleId());
			List<TicketAssignmentVO> taList = tea.getAssignments(ts.getTicketId());
			tea.populateScheduleAssignments(tsList, taList);
			ts = tsList.get(0);
			
			// Make sure the unit location is current (requires the assignment data to set)
			if (req.hasParameter(REQ_COMPLETE))
				updateUnitLocation(ts);
			
			putModuleData(ts);
			
		} catch (InvalidDataException | DatabaseException | com.siliconmtn.exception.DatabaseException e) {
			log.error("Unable to save ticket schedule", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves/edits a ticket schedule record when equipment is scheduled or
	 * re-schedule for drop-off or pick-up.
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public TicketScheduleVO saveSchedule(ActionRequest req) throws InvalidDataException, DatabaseException {
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Build the Ticket Schedule data and save
		TicketScheduleVO ts = new TicketScheduleVO(req);
		modifyNotes(ts);
		db.save(ts);
		
		return ts;
	}
	
	/**
	 * Handles completion of a scheduled pick-up or drop-off when the equipment
	 * is transfered to another party. This also logs a ledger entry.
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public TicketScheduleVO completeSchedule(ActionRequest req) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketScheduleVO ts = new TicketScheduleVO(req);
		ts.setUpdateDate(new Date());
		modifyNotes(ts);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		
		// Add a ledger entry
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		ledger.setDispositionBy(user.getUserId());
		ledger.setSummary(LedgerSummary.SCHEDULE_TRANSFER_COMPLETE.summary);
		db.save(ledger);
		ts.setLedgerEntryId(ledger.getLedgerEntryId());

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
			log.error("could not delete old records",e1);
		}
		
		return ts;
	}
	
	/**
	 * Modify notes per given requirements:
	 * 	 - Store any/all notes in one field.
	 * 	 - Notes are additive, can not be edited once saved.
	 *   - Date and time should be added to note.
	 * 
	 * @param ts
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	protected void modifyNotes(TicketScheduleVO ts) throws InvalidDataException, DatabaseException {
		// Get previous notes (if any)
		TicketScheduleVO prevTs = new TicketScheduleVO();
		prevTs.setTicketScheduleId(ts.getTicketScheduleId());
		if (!StringUtil.isEmpty(ts.getTicketScheduleId())) {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.getByPrimaryKey(prevTs);
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
	private void updateUnitLocation(TicketScheduleVO ts) throws DatabaseException {
		String unitLoc = ts.getRecordTypeCode().equals(PRE_REPAIR) ? ts.getCasLocation().getTypeCode().toString() : ts.getOwnerLocation().getTypeCode().toString();
		UnitLocation location = UnitLocation.valueOf(unitLoc);
		
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ts.getTicketId());
		ticket.setUnitLocation(location);

		TicketTransaction tt = new TicketTransaction(getAttributes(), getDBConnection());
		tt.updateUnitLocation(ticket);
	}
}

