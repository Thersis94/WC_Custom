package com.wsla.action.ticket;

// JDK 1.8.x
import java.util.List;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
// WC Libs 3.x
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
// WSLA Libs
import com.wsla.action.BasePortalAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketAssignmentVO;
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
		saveCoreTicket(ticket);
		
		// Add User and assignment to the ticket
		UserVO user = new UserVO(req);
		this.saveUser(site, user, false, true);

		// Add an item to the ledger
		TicketLedgerVO ledger = addLedger(req, LedgerSummary.CALL_RECVD.summary);
		
		// Add Data Attributes
		assignDataAttributes(ticket);
		
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
	public void manageTicketAssignment(String tId, String uId, String lId, int owner) 
	throws InvalidDataException, DatabaseException {
		
		TicketAssignmentVO tass = new TicketAssignmentVO();
		tass.setUserId(uId);
		tass.setTicketId(tId);
		tass.setOwnerFlag(owner);
		tass.setLocationId(lId);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(tass);
	}
	
	
	public void assignDataAttributes(TicketVO vo) {
		
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

