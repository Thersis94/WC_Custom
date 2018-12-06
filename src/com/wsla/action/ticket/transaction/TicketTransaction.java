package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
// WSLA Libs
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes to the core ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 5, 2018
 * @updates:
 ****************************************************************************/

public class TicketTransaction extends BaseTransactionAction {
	
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "ticketTrans";
	
	/**
	 * Indicates that the unit location code is being updated
	 */
	public static final String REQ_UNIT_LOCATION = "unitLocation";
	
	/**
	 * 
	 */
	public TicketTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public TicketTransaction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
			TicketVO ticket = new TicketVO(req);
			
			if (req.hasParameter("existing")) {
				closeForExistingTicket(ticket.getTicketId(), user.getUserId());
				putModuleData("SUCCESS");
			} else if (req.hasParameter(REQ_UNIT_LOCATION)) {
				updateUnitLocation(ticket);
				putModuleData(ticket);
			}
			
		} catch (DatabaseException e) {
			log.error("Unable to save ticket micro transaction", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Closes a ticket when another exists for the same unit
	 * 
	 * @param ticketId
	 * @param userId
	 * @throws DatabaseException 
	 */
	public void closeForExistingTicket(String ticketId, String userId) throws DatabaseException {
		log.info("Closing ticket id: " + ticketId);
		
		// Add ledger for move to status EXISTING_TICKET
		changeStatus(ticketId, userId, StatusCode.EXISTING_TICKET, null, null);
		
		// Add ledger & update ticket status for move to status CLOSED
		changeStatus(ticketId, userId, StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary, null);
	}
	
	/**
	 * Updates a ticket's unit location
	 * 
	 * @param ticket
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public TicketVO updateUnitLocation(TicketVO ticket) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		ticket.setUpdateDate(new Date());

		// Create the SQL for updating the record
		StringBuilder sql = new StringBuilder(110);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket " );
		sql.append("set unit_location_cd = ?, update_dt = ? ");
		sql.append("where ticket_id = ? ");
		log.debug(sql);
		
		// Set the fields we are updating from
		List<String> fields = Arrays.asList("unit_location_cd", "update_dt", "ticket_id");

		// Save the updates to the record
		try {
			db.executeSqlUpdate(sql.toString(), ticket, fields);
		} catch (DatabaseException e1) {
			log.error("Could not update ticket unit location",e1);
		}
		
		return ticket;
	}
}

