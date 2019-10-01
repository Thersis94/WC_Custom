package com.wsla.action.ticket.transaction;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketOverviewAction;
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
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		
		// Process public user form
		if(req.hasParameter("publicUserForm")) publicUserForm(req, ticket);
		
		try {
			
			if (req.hasParameter("existing")) {
				closeForExistingTicket(ticket.getTicketId(), user.getUserId(), false);
				putModuleData("SUCCESS");
			} else if (req.hasParameter(REQ_UNIT_LOCATION)) {
				updateUnitLocation(ticket);
				putModuleData(ticket);
			} else if(req.hasParameter("dispose") && req.hasParameter("closing")) {
				closeForExistingTicket(ticket.getTicketId(), user.getUserId(), true);
				putModuleData("SUCCESS");
			} else if (req.hasParameter("perfecoStatus")) {
				changePerfecoStatus(ticket);
			}
			
		} catch (DatabaseException | InvalidDataException e) {
			log.error("Unable to save ticket micro transaction", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Changes the perfeco status of the ticket
	 * @param ticket
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private void changePerfecoStatus(TicketVO ticket) throws InvalidDataException, DatabaseException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.update(ticket, Arrays.asList("ticket_id", "profeco_status_cd"));
	}
	
	/**
	 * 
	 * @param req
	 * @param ticket
	 */
	private void publicUserForm(ActionRequest req, TicketVO ticket) {
		try {
			StatusCode sc;
			LedgerSummary ls;
			
			if(!req.getBooleanParameter("warrantyValidFlag")) {
				saveInvalidSerialToTicket(req, ticket);
				sc = StatusCode.UNLISTED_SERIAL_NO;
				ls = LedgerSummary.INVALID_SERIAL_SAVED;
			}else {
				saveValidSerialToTicket(ticket);
				sc = StatusCode.USER_CALL_DATA_INCOMPLETE;
				ls = LedgerSummary.VALID_SERIAL_SAVED;
			}
	
			changeStatus(ticket.getTicketId(), ticket.getUserId(), sc, ls.summary, null);
		
		} catch (DatabaseException e) {
			log.error("could not save product serial status ",e);
			putModuleData(null,0,false, e.getLocalizedMessage(), true);
		}
		
		return;
	}
	
	/**
	 * 
	 * @param req 
	 * @param ticket
	 */
	private void saveInvalidSerialToTicket(ActionRequest req, TicketVO ticket) {
		TicketOverviewAction tov = new TicketOverviewAction();
		tov.setDBConnection(getDBConnection());
		tov.setActionInit(getActionInit());
		tov.setAttributes(getAttributes());
		
		try {
			tov.addProductSerialNumber(req, ticket);
		} catch (Exception e) {
			log.error("could not ",e);
		}
	}

	/**
	 * saves the data for a valid serial and warrenty to the ticket 
	 * @param ticket
	 * @throws DatabaseException 
	 */
	public void saveValidSerialToTicket(TicketVO ticket) throws DatabaseException {
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_ticket ");
		sql.append("set product_serial_id = ?, product_warranty_id = ? where ticket_id = ? ");
		List<String> fields = new ArrayList<>();
		fields.add("product_serial_id");
		fields.add("product_warranty_id");
		fields.add("ticket_id");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.executeSqlUpdate(sql.toString(), ticket, fields);

		
	}

	/**
	 * Closes a ticket when another exists for the same unit
	 * 
	 * @param ticketId
	 * @param userId
	 * @throws DatabaseException 
	 */
	public void closeForExistingTicket(String ticketId, String userId,boolean isDisposed) throws DatabaseException {
		log.debug("Closing ticket id: " + ticketId);
		log.debug("is disposed " + isDisposed);
		if(isDisposed) {
			// Add ledger for move to status EXISTING_TICKET
			changeStatus(ticketId, userId, StatusCode.PRODUCT_DECOMMISSIONED, null, null);
		}else {
			// Add ledger for move to status EXISTING_TICKET
			changeStatus(ticketId, userId, StatusCode.EXISTING_TICKET, null, null);
		}
		
		
		
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
	
	/**
	 * Changes the standing of a ticket
	 * @param ticketId
	 * @param standingCode
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void assignStanding(String ticketId, TicketVO.Standing standingCode) 
	throws InvalidDataException, DatabaseException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ticketId);
		ticket.setStandingCode(standingCode);
		db.update(ticket, Arrays.asList("standing_cd", "ticket_id"));
	}
}

