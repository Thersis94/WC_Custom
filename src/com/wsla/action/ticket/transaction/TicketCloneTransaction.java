package com.wsla.action.ticket.transaction;

import java.sql.SQLException;
import java.util.ArrayList;
// JDK 1.8.x
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

//SMT Base Lbs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
// WSLA Libs
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.DiagnosticTicketVO;
import com.wsla.data.ticket.DispositionCode;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;
import com.wsla.data.ticket.TicketAssignmentVO.ProductOwner;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketVO.Standing;
import com.wsla.data.ticket.TicketVO.UnitLocation;

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

public class TicketCloneTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "clone";
	private String orginalTicketId = "";
	
	
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
	 * @param dbConn
	 * @param attributes
	 */
	public TicketCloneTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this();
		this.dbConn = dbConn;
		this.attributes = attributes;
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
		UserVO user = ((UserVO)getAdminUser(req).getUserExtendedInfo());
		
		try {
			String ticketIdText = req.getParameter("ticketIdText");
			if (StringUtil.isEmpty(ticketIdText)) 
				throw new InvalidDataException("ticket info not passed");
			
			// Return the ticket data in case the js needs to display or use
			setModuleData(cloneTicket(ticketIdText, user));
		} catch (Exception e) {
			log.error("Unable to clone ticket", e);
			setModuleData("", 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Clones the given ticket.
	 * 
	 * @param ticketIdText - supports ticketId or ticketIdText
	 * @param user
	 * @return
	 * @throws ActionException
	 */
	public TicketVO cloneTicket(String ticketIdText, UserVO user) throws ActionException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketEditAction tea = new TicketEditAction(dbConn, attributes);
		TicketVO ticket;

		try {
			// Load ticket core data
			ticket = processTicket(db, tea, ticketIdText);
			// Load ticket data
			ticket.setTicketData(processTicketData(db, ticket, tea));
			
			// Load User and Retailer Assignments
			ticket.setAssignments(processAssignments(db, ticket, tea));
			
			//get and clone the ticket diagnostics
			ticket.setDiagnosticRun(cloneDiagnostics(ticketIdText, ticket.getTicketId(), tea, db));
			
			// Add a ledger entry
			addLedgerEntry(db, user, ticket.getTicketId());
		} catch (InvalidDataException | DatabaseException | com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
		return ticket;
	}
	
	/**
	 * gets all the diagnostics for the old ticket nubmer changes the ticket id and returns them so they can be added to the child ticket
	 * @param oldTicketId
	 * @param newTicketId
	 * @param tea 
	 * @param db 
	 * @return
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private List<DiagnosticRunVO> cloneDiagnostics(String oldTicketId, String newTicketId, TicketEditAction tea, DBProcessor db) throws InvalidDataException, DatabaseException {
		//it appears some cloning processes send a ticket no if thats the case get the oringal ticket id from ticket processing
		if (! UUIDGenerator.isUUID(oldTicketId)) {
			oldTicketId = orginalTicketId;
		}
		
		List<DiagnosticRunVO> diags = tea.getDiagnostics(oldTicketId);
		log.debug("old ticket id "+ oldTicketId);
		for (DiagnosticRunVO d : diags) {
			log.debug("setting ticket id to "+ newTicketId + " and  setting comments to cloned: " + d.getDiagComments());
			d.setDiagnosticRunId(null);
			d.setTicketId(newTicketId);
			d.setDiagComments("Cloned: "+d.getDiagComments());
			
			db.insert(d);
			
			List<DiagnosticTicketVO> diagRows = d.getDiagnostics();
			//loop the line items and update them with the new id
			for(DiagnosticTicketVO r : diagRows) {
				r.setDiagnosticRunId(d.getDiagnosticRunId());
				r.setDiagnosticTicketId(null);
				
				db.insert(r);
			}
			
		}
		
		return diags;
	}

	/**
	 * Clones a ticket and moves ownership of the unit to WSLA. Sets to in-repair status.
	 * 
	 * @param ticketIdText - supports ticketId or ticketIdText
	 * @param user
	 * @return
	 * @throws ActionException
	 */
	public TicketVO cloneTicketToWSLA(String ticketIdText, UserVO user) throws ActionException {
		// Clone the old ticket to the new ticket
		TicketVO newTicket = cloneTicket(ticketIdText, user);
		newTicket.setUnitLocation(UnitLocation.WSLA);
	
		removeNewTicketCasAssignment(newTicket.getTicketId());
		
		// Create a new ticket assignment, setting WSLA as the CAS on the new ticket
		TicketAssignmentVO assignment = new TicketAssignmentVO();
		assignment.setLocationId(WSLAConstants.DEFAULT_SHIPPING_SRC);
		assignment.setUserId(user.getUserId());
		assignment.setTicketId(newTicket.getTicketId());
		assignment.setOwnerFlag(1);
		assignment.setTypeCode(TypeCode.CAS);

		try {
			// Save updated ticket data
			DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
			dbp.save(newTicket);
			
			// Save the new ticket assignment
			TicketAssignmentTransaction tat = new TicketAssignmentTransaction(getDBConnection(), getAttributes());
			tat.assign(assignment, user);
			
			// Set the new ticket's status to repairable
			changeStatus(newTicket.getTicketId(), user.getUserId(), DispositionCode.REPAIRABLE.getStatus(), DispositionCode.REPAIRABLE.getLedgerSummary(), null);
			
			// Set the current disposition to repairable & WSLA as the owner of the unit
			TicketDataTransaction tdt = new TicketDataTransaction(getDBConnection(), getAttributes());
			tdt.saveDataAttribute(newTicket.getTicketId(), "attr_dispositionCode", DispositionCode.REPAIRABLE.name(), null, false);
			tdt.saveDataAttribute(newTicket.getTicketId(), "attr_ownsTv", ProductOwner.WSLA.name(), null, true);
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			throw new ActionException(e);
		}
		
		return newTicket;
	}
	
	/**
	 * used to remove the old record from the orginal clone to make way for the new custom wsla cas record
	 * @param ticketId
	 */
	private void removeNewTicketCasAssignment(String ticketId) {
		
		//check to make sure there isnt already a relationship for this attribute and role
		StringBuilder sb = new StringBuilder(120);
		sb.append("delete from ").append(getCustomSchema()).append("wsla_ticket_assignment wta where ticket_id = ? and assg_type_cd = 'CAS' " );
		List<String> fields = new ArrayList<>();
		fields.add("ticket_id");
		
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		TicketAssignmentVO tsVo = new TicketAssignmentVO();
		tsVo.setTicketId(ticketId);
		
		try {
			db.executeSqlUpdate(sb.toString(), tsVo, fields);
		} catch (DatabaseException e1) {
			log.error("could not delete old records",e1);
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
		orginalTicketId = ticket.getTicketId();
		
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
			//cloned tickets shouldnt have a disposition.
			if("attr_dispositionCode".equals(tdvo.getAttributeCode())) {continue;}
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
		List<TypeCode> types = Arrays.asList(TypeCode.CALLER, TypeCode.OEM, TypeCode.RETAILER, TypeCode.CAS);
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
		BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
		bta.addLedger(ticketId, user.getUserId(), StatusCode.OPENED, LedgerSummary.TICKET_CLONED.summary, null);
		bta.changeStatus(ticketId, user.getUserId(), StatusCode.USER_CALL_DATA_INCOMPLETE, null, null);
	}
}

