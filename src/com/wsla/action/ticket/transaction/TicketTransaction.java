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
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
// WSLA Libs
import com.wsla.data.ticket.TicketVO;

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
		log.debug("^^^^ ticket transaction build");
		
		if(req.hasParameter("publicUserForm")) {
			log.debug("^^^^ his ticket transaction build");
			TicketVO ticket = new TicketVO(req);
			if(req.getBooleanParameter("warrantyValidFlag")) {
				saveValidSerialToTicket(ticket);
				try {
					changeStatus(ticket.getTicketId(), ticket.getUserId(), StatusCode.USER_CALL_DATA_INCOMPLETE, LedgerSummary.VALID_SERIAL_SAVED.summary, null);
				} catch (DatabaseException e) {
					log.error("could not save ledger entry or save status ",e);
				}
			}else {
				saveInvalidSerialToTicket(req, ticket);
				try {
					changeStatus(ticket.getTicketId(), ticket.getUserId(), StatusCode.UNLISTED_SERIAL_NO, LedgerSummary.INVALID_SERIAL_SAVED.summary, null);
				} catch (DatabaseException e) {
					log.error("could not save ledger entry or save status ",e);
				}
			}
			
			
			return;
		}
		try {
			
			if (req.hasParameter("existing")) {
				closeForExistingTicket(req.getParameter("ticketId"));
				putModuleData("SUCCESS");
			} else {
				TicketVO ticket = new TicketVO(req);
				
				if (req.hasParameter(REQ_UNIT_LOCATION))
					updateUnitLocation(ticket);
	
				putModuleData(ticket);
			}
			
		} catch (DatabaseException e) {
			log.error("Unable to save ticket micro transaction", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	
	/**
	 * @param req 
	 * @param ticket
	 */
	private void saveInvalidSerialToTicket(ActionRequest req, TicketVO ticket) {
		// TODO Auto-generated method stub
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
	 */
	private void saveValidSerialToTicket(TicketVO ticket) {
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_ticket ");
		sql.append("set product_serial_id = ?, product_warranty_id = ? where ticket_id = ? ");
		List<String> fields = new ArrayList<>();
		fields.add("product_serial_id");
		fields.add("product_warranty_id");
		fields.add("ticket_id");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.executeSqlUpdate(sql.toString(), ticket, fields);
		} catch (DatabaseException e) {
			log.error("could not save serial ticket changes",e);
		}
		
	}

	public void closeForExistingTicket(String ticketId) {
		log.info("Closing ticket id: " + ticketId);
		
		// Add ledger for move to status EXISTING_TICKET
		
		
		// Add Ledger for move to status CLOSED
		
		
		// Update ticket status to CLOSED
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

