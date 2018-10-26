package com.wsla.action.ticket.transaction;

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
			this.saveSchedule(req);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save ticket schedule", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves a ticket schedule record. This can happen when:
	 *    - Equipment is scheduled for drop-off or pick-up
	 *    - Completion of a scheduled pick-up or drop-off
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void saveSchedule(ActionRequest req) throws InvalidDataException, DatabaseException {
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Build the Ticket Schedule Data
		TicketScheduleVO ts = new TicketScheduleVO(req);
		
		// Log a ledger entry when an equipment transfer takes place
		if (req.hasParameter(REQ_COMPLETE)) {
			// Get the WSLA User
			UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
			
			// Add a ledger entry
			TicketLedgerVO ledger = new TicketLedgerVO(req);
			ledger.setDispositionBy(user.getUserId());
			ledger.setSummary(LedgerSummary.SCHEDULE_TRANSFER_COMPLETE.summary);
			db.save(ledger);

			// Put the ledger entry onto the schedule record
			ts.setLedgerEntryId(ledger.getLedgerEntryId());
		}
		
		db.save(ts);
	}
}

