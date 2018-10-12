package com.wsla.action.ticket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;

// WC Libs 3.x
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

// WSLA Libs
import com.wsla.action.BasePortalAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.DiagnosticTicketVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.ProductOwner;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
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
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "serviceOrder";
	
	/**
	 * OEM Id that indicates the caller wanted service for a non-supported OEM
	 */
	public static final String OEM_NOT_SUPPORTED = "NOT_SUPPORTED";
		
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
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		TicketVO ticket = null;
		try {
			if(StringUtil.isEmpty(req.getParameter("ticketId"))) {
				ticket = createTicket(req);
			} else {
				ticket = saveTicketCall(req);
			}
			
			// Return the populated ticket
			putModuleData(ticket);
		} catch(Exception e) {
			log.error("Unable to create / save a ticket", e);
			putModuleData("", 0, false, (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE), true);
		}
	}
	
	/**
	 * Second pass at saving the ticket.  First pass saves the base info.  
	 * This save gets the extended information as well
	 * @param req
	 * @return
	 * @throws Exception
	 */
	public TicketVO saveTicketCall(ActionRequest req) throws Exception {
		log.debug("****** Saving ticket from call");
		UserDataVO profile = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = (UserVO)profile.getUserExtendedInfo();
		TicketVO ticket = new TicketVO(req);
		ticket.addDiagnosticRun(new DiagnosticRunVO(req));
		
		// Check the productSerial and add it if it is missing
		if (StringUtil.isEmpty(req.getParameter("productSerialId"))) 
			this.addProductSerialNumber(req, ticket);
		
		// If the user resolved the ticket during diagnostics, close the ticket
		if (req.getIntegerParameter("attr_issueResolved", 0) == 1) 
			ticket.setStatusCode(StatusCode.CLOSED);

		// Save the ticket core data
		this.saveCoreTicket(ticket);

		// Save the diagnostic info
		this.saveDiagnosticRun(ticket.getDiagnosticRun().get(0));
		
		// Add an item to the ledger
		TicketLedgerVO ledger = addLedger(user.getUserId(), req, ticket.getStatusCode(), LedgerSummary.CALL_FINISHED.summary);
		
		// Save the extended data elements
		assignDataAttributes(ticket, ledger);
		
		// Update the caller's user record and profile
		UserVO caller = new UserVO(req);
		updateWSLAUser(caller);

		// Add the assignments
		String callerAssignmentId = req.getParameter("ticketAssignmentId");
		String ownsTv = req.getParameter("attr_ownsProduct");
		updateAllAssignments(ticket, callerAssignmentId, ownsTv, caller);
		
		return ticket;
	}
	
	/**
	 * When a serial number can't be located, this method adds a new serial
	 * number as unvalidated and updates the product serial id on the ticket and 
	 * changes the status to un
	 * @param req
	 * @param ticket
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void addProductSerialNumber(ActionRequest req, TicketVO ticket) 
	throws InvalidDataException, DatabaseException {
		// Set the product data
		ProductSerialNumberVO psn = new ProductSerialNumberVO();
		psn.setProductId(req.getParameter("productId"));
		psn.setSerialNumber(req.getParameter("serialNumber"));
		psn.setValidatedFlag(0);
		
		// add the serial
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(psn);
		
		// Update the ticket
		ticket.setProductSerialId(psn.getProductSerialId());
		ticket.setProductSerial(psn);
		ticket.setStatusCode(StatusCode.UNLISTED_SERIAL_NO);
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
		
		// If the OEM is unsupported, close the ticket
		if (OEM_NOT_SUPPORTED.equals(ticket.getOemId())) {
			ticket.setStatusCode(StatusCode.CLOSED);
		}
		
		// Save ticket core info
		saveCoreTicket(ticket);
		req.setParameter("ticketId", ticket.getTicketId());
		
		// Add User and assignment to the ticket
		UserVO user = new UserVO(req);
		if (user.getProfile() == null) user.setProfile(new UserDataVO(req));
		this.saveUser(site, user, false, true);
		ticket.addAssignment(manageTicketAssignment(user, null, ticket.getTicketId(), null, 0, TypeCode.CALLER));

		// Add an item to the ledger
		TicketLedgerVO ledger = addLedger(user.getUserId(), req, ticket.getStatusCode(), LedgerSummary.CALL_RECVD.summary);
		
		// Add Data Attributes
		assignDataAttributes(ticket, ledger);
		
		// Update the ticket originator from the user
		updateOriginator(user.getUserId(), ticket.getTicketId());
		
		return ticket;
	}
	
	/**
	 * Updates the originator id.  Needs to be done here in case the ticket stops at 
	 * the overview screen
	 * @param userId
	 * @param ticketId
	 * @throws SQLException
	 */
	public void updateOriginator(String userId, String ticketId) throws SQLException {
		StringBuilder sql = new StringBuilder(40);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket ");
		sql.append("set originator_user_id = ? where ticket_id = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, userId);
			ps.setString(2, ticketId);
			ps.executeUpdate();
		}
	}
	
	/**
	 * Once the user record exists on a ticket, update the user data
	 * @param user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public void updateWSLAUser(UserVO user) throws InvalidDataException, DatabaseException, com.siliconmtn.exception.DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.update(user);
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		pm.updateProfile(user.getProfile(), getDBConnection());
	}
	
	/**
	 * Saves the diagnostic run along with the results from each diagnostic check
	 * @param dr
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveDiagnosticRun(DiagnosticRunVO dr) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// insert the dr run
		db.insert(dr);
		
		// Loop the actual diagnostics and store them
		for (DiagnosticTicketVO diag : dr.getDiagnostics()) {
			db.insert(diag);
		}
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
	public TicketAssignmentVO manageTicketAssignment(UserVO user, String asgnId, String tId, String lId, int owner, TypeCode typeCode) 
	throws InvalidDataException, DatabaseException {
		if (user == null) user = new UserVO();
		
		TicketAssignmentVO tass = new TicketAssignmentVO();
		tass.setTicketAssignmentId(asgnId);
		tass.setUserId(user.getUserId());
		tass.setTicketId(tId);
		tass.setOwnerFlag(owner);
		tass.setLocationId(lId);
		tass.setUser(user);
		tass.setTypeCode(typeCode);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(tass);
		
		return tass;
	}
	
	/**
	 * Updates the assignments on the Ticket for Retailer and User (OEM if owned)
	 * @param t
	 * @param cai
	 * @param ownsProduct
	 * @param user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void updateAllAssignments(TicketVO t, String cai, String ownsProduct, UserVO user) 
	throws InvalidDataException, DatabaseException {
		ProductOwner owner = ProductOwner.END_USER;
		if (! StringUtil.isEmpty(ownsProduct)) owner = ProductOwner.valueOf(ownsProduct);
		
		// Assign the Retailer
		int isOwned = ProductOwner.RETAILER.equals(owner) ? 1 : 0;
		manageTicketAssignment(null, null, t.getTicketId(), t.getRetailerId(), isOwned, TypeCode.RETAILER);
		
		// Update the Assignment for the user if they own the product
		isOwned = ProductOwner.END_USER.equals(owner) ? 1 : 0;
		if (isOwned == 1) {
			manageTicketAssignment(user, cai, t.getTicketId(), null, isOwned, TypeCode.CALLER);
		}
		
		// Add the Assignment for the OEM if they own the product
		isOwned = ProductOwner.OEM.equals(owner) ? 1 : 0;
		if (isOwned == 1) {
			manageTicketAssignment(null, null, t.getTicketId(), null, isOwned, TypeCode.OEM);
		}
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

