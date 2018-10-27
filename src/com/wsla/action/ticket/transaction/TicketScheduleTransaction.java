package com.wsla.action.ticket.transaction;

import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketScheduleVO;
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
			if (req.hasParameter(REQ_COMPLETE)) {
				this.completeSchedule(req);
			} else {
				this.saveSchedule(req);
			}
		} catch (InvalidDataException | DatabaseException e) {
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
	public void saveSchedule(ActionRequest req) throws InvalidDataException, DatabaseException {
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Build the Ticket Schedule data and save
		TicketScheduleVO ts = new TicketScheduleVO(req);
		db.save(ts);
	}
	
	/**
	 * Handles completion of a scheduled pick-up or drop-off when the equipment
	 * is transfered to another party. This also logs a ledger entry.
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void completeSchedule(ActionRequest req) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketScheduleVO ts = new TicketScheduleVO(req);
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
		sql.append("notes_txt = concat(notes_txt, ?), ledger_entry_id = ?, complete_dt = ?, ");
		sql.append("update_dt = ? where ticket_schedule_id = ? ");
		
		// Set the fields we are updating from
		List<String> fields = Arrays.asList("signer_nm", "signature_txt", "product_validated_flg", "notes_txt", "ledger_entry_id", "complete_dt", "update_dt", "ticket_schedule_id");

		// Save the updates to the record
		try {
			db.executeSqlUpdate(sql.toString(), ts, fields);
		} catch (DatabaseException e1) {
			log.error("could not delete old records",e1);
		}
	}
}

