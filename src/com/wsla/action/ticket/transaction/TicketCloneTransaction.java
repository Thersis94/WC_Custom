package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.util.Arrays;
import java.util.Date;
import java.util.List;

//SMT Base Lbs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketVO.Standing;

/****************************************************************************
 * <b>Title</b>: TicketCloneTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Clones an existing ticket so data doesn't have to be re-entered
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 3, 2018
 * @updates:
 ****************************************************************************/

public class TicketCloneTransaction extends SBActionAdapter {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "clone";
	
	
	/**
	 * 
	 */
	public TicketCloneTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketCloneTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * @throws InvalidDataException 
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketEditAction tea = new TicketEditAction(dbConn, attributes);
		UserVO user = ((UserVO)getAdminUser(req).getUserExtendedInfo());
		
		try {
			String ticketIdText = req.getParameter("ticketIdText");
			if (StringUtil.isEmpty(ticketIdText)) 
				throw new InvalidDataException("ticket info not passed");
			
			// Load ticket core data
			TicketVO ticket = processTicket(db, tea, ticketIdText);
			
			// Load ticket data
			ticket.setTicketData(processTicketData(db, ticket, tea));
			
			// Load User and Retailer Assignments
			ticket.setAssignments(processAssignments(db, ticket, tea));
			
			// Add a ledger entry
			addLedgerEntry(db, user, ticket.getTicketId());
			
			// Return the ticket data in case the js needs to display or use
			setModuleData(ticket);
		} catch (Exception e) {
			log.error("Unable to clone ticket", e);
			setModuleData("", 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Clones the core ticket information.  The original ticket id is assigned to 
	 * the parent id and a new ticket id text is assigned.  The unit location is 
	 * kept the same.  This assumes the unit location of the previous ticket now matches.
	 * The ticket standing is modified to be good, regardless of the previous state
	 * @param db
	 * @param tea
	 * @param ticketIdText
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public TicketVO processTicket(DBProcessor db, TicketEditAction tea, String ticketIdText) 
	throws InvalidDataException, DatabaseException {
		TicketVO ticket = tea.getBaseTicket(ticketIdText);
		String newTicketIdText = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
		ticket.setParentId(ticket.getTicketId());
		ticket.setStatusCode(StatusCode.USER_CALL_DATA_INCOMPLETE);
		ticket.setStandingCode(Standing.GOOD);
		ticket.setUpdateDate(null);
		ticket.setCreateDate(new Date());
		ticket.setTicketId(new UUIDGenerator().getUUID());
		ticket.setTicketIdText(newTicketIdText.toUpperCase());
		db.insert(ticket);
		
		return ticket;
	}
	
	/**
	 * Clones the extended ticket information
	 * @param db
	 * @param ticket
	 * @param tea
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public List<TicketDataVO> processTicketData(DBProcessor db, TicketVO ticket, TicketEditAction tea) 
	throws InvalidDataException, DatabaseException {
		
		List<TicketDataVO> extData = tea.getExtendedData(ticket.getParentId(), null);
		for (TicketDataVO tdvo : extData) {
			tdvo.setTicketId(ticket.getTicketId());
			tdvo.setDataEntryId(new UUIDGenerator().getUUID());
			tdvo.setUpdateDate(null);
			tdvo.setCreateDate(new Date());
			db.insert(tdvo);
		}
		
		return extData;
	}
	
	/**
	 * Clones the assignments
	 * @param db
	 * @param ticket
	 * @param tea
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public List<TicketAssignmentVO> processAssignments(DBProcessor db, TicketVO ticket, TicketEditAction tea) 
	throws InvalidDataException, DatabaseException, com.siliconmtn.exception.DatabaseException {
		List<TypeCode> types = Arrays.asList(TypeCode.CALLER, TypeCode.OEM, TypeCode.RETAILER);
		List<TicketAssignmentVO> assignments = tea.getAssignments(ticket.getParentId());
		
		for (TicketAssignmentVO assign : assignments) {
			if (! types.contains(assign.getTypeCode())) continue;
			assign.setTicketId(ticket.getTicketId());
			assign.setTicketAssignmentId(new UUIDGenerator().getUUID());
			assign.setUpdateDate(null);
			assign.setCreateDate(new Date());
			db.insert(assign);
		}
		
		return assignments;
	}
	
	/**
	 * Adds a ledger entry
	 * @param db
	 * @param user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void addLedgerEntry(DBProcessor db, UserVO user, String ticketId) 
	throws InvalidDataException, DatabaseException {
		TicketLedgerVO ledger = new TicketLedgerVO();
		ledger.setStatusCode(StatusCode.USER_CALL_DATA_INCOMPLETE);
		ledger.setDispositionBy(user.getUserId());
		ledger.setTicketId(ticketId);
		ledger.setSummary(LedgerSummary.TICKET_CLONED.summary);
		db.insert(ledger);
	}
}

