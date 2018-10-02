package com.wsla.action.ticket;

// JDK 1.8.x
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;

// WC Libs 3.x
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

// WSLA Libs
import com.wsla.action.BasePortalAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketOverviewAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the overview data for the service order ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/

public class TicketOverviewAction extends BasePortalAction {

	/**
	 * 
	 */
	public TicketOverviewAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketOverviewAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Listing Tickets ....");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Saving Tickets ....");
		
		if(StringUtil.isEmpty(req.getParameter("ticketId"))) {
			try {
				putModuleData(createTicket(req));
			} catch(Exception e) {
				log.error("Unable to create a ticket", e);
				putModuleData("", 0, false, (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE), true);
			}
		}
	}
	
	/**
	 * Creates the base ticket information
	 * @param req
	 * @return
	 * @throws Exception 
	 */
	public TicketVO createTicket(ActionRequest req)  throws Exception {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		// Add the core information
		TicketVO ticket = new TicketVO(req);
		String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
		ticket.setTicketIdText(slug.toUpperCase());
		saveCoreTicket(ticket);
		req.setParameter("ticketId", ticket.getTicketId());
		
		// Add User and assignment to the ticket
		UserVO user = new UserVO(req);
		if (user.getProfile() == null) user.setProfile(new UserDataVO(req));
		this.saveUser(site, user, false, true);
		ticket.addAssignment(manageTicketAssignment(user, ticket.getTicketId(), null, 0));

		// Add an item to the ledger
		TicketLedgerVO ledger = addLedger(user.getUserId(), req, LedgerSummary.CALL_RECVD.summary);
		
		// Add Data Attributes
		assignDataAttributes(ticket, ledger);
		
		return ticket;
	}
	
	/**
	 * Adds a ticket assignment based upon the provided data
	 * @param tId Ticket id must always be present.
	 * @param uId User id for the assignment,  May be null if assigning a location
	 * @param lId The location id is a Provider location id for an OEM, 
	 * Retailer or service center
	 * @param owner Identifies whether this assignee is the owner of the unit
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public TicketAssignmentVO manageTicketAssignment(UserVO user, String tId, String lId, int owner) 
	throws InvalidDataException, DatabaseException {
		
		TicketAssignmentVO tass = new TicketAssignmentVO();
		tass.setUserId(user.getUserId());
		tass.setTicketId(tId);
		tass.setOwnerFlag(owner);
		tass.setLocationId(lId);
		tass.setUser(user);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(tass);
		
		return tass;
	}
	
	/**
	 * Assigns
	 * @param vo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void assignDataAttributes(TicketVO vo, TicketLedgerVO ledger) 
	throws InvalidDataException, DatabaseException {
		Map<String, String> ids = getTicketDataIds(vo.getTicketId());

		for (TicketDataVO data : vo.getTicketData()) {
			
			// Assign the ticket id as it may or not be present when creating the 
			// Data map
			data.setTicketId(vo.getTicketId());
			
			// Add the ledger id
			data.setLedgerEntryId(ledger.getLedgerEntryId());
			
			// Get the data entry id if empty and assign if it exists
			if (StringUtil.isEmpty(data.getDataEntryId()) && ids.containsKey(data.getAttributeCode())) 
				data.setDataEntryId(ids.get(data.getAttributeCode()));
			
			// Save the attribute data
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(data);
		}
	}
	
	/**
	 * Gets a listing of the data entry ids to the attribute code.  Needed to ensure
	 * the ticket data can be updated if it exists
	 * @param ticketId
	 * @return Map of attribute code as the key and the data entry id as the value
	 */
	private Map<String, String> getTicketDataIds(String ticketId) {
		StringBuilder sql = new StringBuilder(64);
		sql.append("select attribute_cd as key, data_entry_id as value from ");
		sql.append(getCustomSchema()).append("wsla_ticket_data where ticket_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> data = db.executeSelect(sql.toString(), Arrays.asList(ticketId), new GenericVO());
		Map<String, String> dataMap = new HashMap<>();
		for(GenericVO ele : data) { dataMap.put((String)ele.getKey(), (String)ele.getValue()); }
		
		return dataMap;
	}
	
	/**
	 * 
	 * @param ticket
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveCoreTicket(TicketVO ticket) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(ticket);
	}
	
	
	public List<TicketVO> getTicketList(ActionRequest req) {
		
		return null;
	}

}

