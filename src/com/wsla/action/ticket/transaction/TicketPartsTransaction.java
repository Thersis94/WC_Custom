package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.admin.InventoryAction;
import com.wsla.action.admin.LogisticsAction;

// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketPartsTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages parts related micro-transactions
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 19, 2018
 * @updates:
 ****************************************************************************/

public class TicketPartsTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "parts";
	
	
	public static final String PART_NOTE_APPROVAL_KEY = "attr_partsNotes";
	
	/**
	 * 
	 */
	public TicketPartsTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketPartsTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public TicketPartsTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("isApproved")) {
				setApproval(req);
			} else if (req.hasParameter("consumeParts")) {
				//******* Add a ledger entry for the Repair Type
				addLedgerForRepairType(req);
				//addRepairCode(req.getParameter(WSLAConstants.TICKET_ID), req.getParameter("attr_unitRepairCode")); 
				//consumeParts(req);
			} else {
				submitForApproval(req);
			}
		} catch (Exception e) {
			log.error("Unable to submit parts for approval", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * 
	 * @param req
	 */
	public void addLedgerForRepairType(ActionRequest req) {
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO l = new TicketLedgerVO(req);
		l.setSummary(LedgerSummary.REPAIR_TYPE.summary + ": " + l.getBillableActivityCode());
		l.setDispositionBy(user.getUserId());
		
		// Call base transaction
	}

	/**
	 * Submits the parts for approval and updates the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws SQLException 
	 */
	private void submitForApproval(ActionRequest req) throws DatabaseException, SQLException {
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.CAS_PARTS_REQUESTED, LedgerSummary.CAS_REQUESTED_PARTS.summary, null);

		// Update parts to having been submitted for approval
		updatePartStatusSubmitted(ticket.getTicketId());
		
		// Build next step
		Map<String, Object> params = new HashMap<>();
		params.put(WSLAConstants.TICKET_ID, ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), params, false);
	}
	
	/**
	 * Updates the sql 
	 * @param ticketId
	 * @throws SQLException
	 */
	public void updatePartStatusSubmitted(String ticketId) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("update ").append(getCustomSchema());
		sql.append("wsla_part set submit_approval_flg=1 where ticket_id = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			ps.executeUpdate();
		}
	}

	/**
	 * Sets the approval status of the requested parts
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws SQLException 
	 * @throws InvalidDataException 
	 */
	private void setApproval(ActionRequest req) throws Exception {
		String ticketId = req.getParameter(WSLAConstants.TICKET_ID);
		String note = req.getParameter(PART_NOTE_APPROVAL_KEY);
		boolean isApproved = Convert.formatBoolean(req.getParameter("isApproved"));
		if (!isApproved) {
			this.rejectApproval(req);
			return;
		}
		
		// Add the notes if passed to the ledger
		StringBuilder summary = new StringBuilder(LedgerSummary.PARTS_REQUEST_REVIEWED.summary);
		if (! StringUtil.isEmpty(note)) {
			summary.append(" : ").append(note);
		}		
		
		// Set the approval status for the parts request
		TicketLedgerVO ldgr = setPartsStatus(req, StatusCode.CAS_PARTS_ORDERED, summary.toString(), null);
		
		// Add the notes to the ticket data
		if (! StringUtil.isEmpty(note))
			addTicketData(ticketId, ldgr.getLedgerEntryId(), note);
		
		// Add the shipment , com.wsla.ction.admin.LogisticsAction.build()
		addShipmentFromParts(ticketId);
	}
	
	/**
	 * Processes the rejection of the parts request
	 * @param req
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public void rejectApproval(ActionRequest req) throws DatabaseException, InvalidDataException {
		String note = req.getParameter(PART_NOTE_APPROVAL_KEY);
		
		// Add ledger and change status for UNREPARIABLE with notes
		StringBuilder summary = new StringBuilder(LedgerSummary.PARTS_REQUEST_REJECTED.summary);
		summary.append(" : ").append(note);
		
		// Set the approval status for the parts request
		TicketLedgerVO ldgr = setPartsStatus(req, StatusCode.UNREPAIRABLE, summary.toString(), null);
		
		// Add ticket data for Rejection / Approval Note
		addTicketData(req.getParameter(WSLAConstants.TICKET_ID), ldgr.getLedgerEntryId(), note);
		
		// Depending on reject type add ledger/status 
		// for PENDING_UNIT_RETURN or REPLACMENT_REQUEST
		summary = new StringBuilder(LedgerSummary.REPAIR_STATUS_CHANGED.summary);
		StatusCode sc = StatusCode.valueOf(req.getParameter("rejectType"));
		setPartsStatus(req, sc, summary.toString(), null);
	}
	
	/**
	 * 
	 * @param tId
	 * @param lId
	 * @param value
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void addTicketData(String tId, String lId, String value) 
	throws InvalidDataException, DatabaseException {
		TicketDataVO td = new TicketDataVO();
		td.setTicketId(tId);
		td.setAttributeCode(PART_NOTE_APPROVAL_KEY);
		td.setLedgerEntryId(lId);
		td.setValue(value);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(td);
	}
	
	/**
	 * 
	 * @param tId
	 * @param from
	 * @param to
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws SQLException 
	 */
	public void addShipmentFromParts(String tId) throws Exception {
		saveShipment(tId, false);
	}
	
	/**
	 * will create and save a new shipment can also change direction if it is a return shipment.
	 * @throws Exception 
	 * 
	 */
	public void saveShipment(String tId, boolean isReturn) throws Exception {
		
		// Create a shipment
		ShipmentVO shipment = new ShipmentVO();
		if(isReturn) {
			shipment.setFromLocationId(getCasLocationId(tId));
			shipment.setToLocationId(WSLAConstants.DEFAULT_SHIPPING_SRC);
		}else {
			shipment.setFromLocationId(WSLAConstants.DEFAULT_SHIPPING_SRC);
			shipment.setToLocationId(getCasLocationId(tId));
		}
		
		shipment.setTicketId(tId);
		shipment.setStatus(ShipmentStatus.CREATED);
		shipment.setShipmentType(isReturn ? ShipmentType.UNIT_MOVEMENT : ShipmentType.PARTS_REQUEST);
		
		// Save the shipment
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(shipment);
		
		LogisticsAction la = new LogisticsAction(getAttributes(), getDBConnection());
		if(isReturn) {
			//look at the ticket and insert a new part for this ticket
			la.saveProductAsPart(tId, shipment.getShipmentId());
		}else {
			// update the parts entered by the human to this shipment 
			la.addTicketPartsToShipment(shipment.getShipmentId(), tId);
		}
		
	}
	
	
	/**
	 * Gets the location of the CAS so the shipment info can be preassigned
	 * @param ticketId
	 * @return
	 * @throws SQLException
	 */
	public String getCasLocationId(String ticketId) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select location_id from ").append(getCustomSchema());
		sql.append("wsla_ticket_assignment where assg_type_cd = 'CAS' and ticket_id = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getString(1);
				else return null;
			}
		}
	}

	/**
	 * Sets the given status data for the ticket's parts order
	 * 
	 * @param req
	 * @param sc
	 * @param summary
	 * @param params
	 * @throws DatabaseException
	 */
	private TicketLedgerVO setPartsStatus(ActionRequest req, StatusCode sc, String summary, Map<String, Object> params) throws DatabaseException {
		// Set the given status
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), sc, summary, null);
		buildNextStep(ledger.getStatusCode(), params, false);
		
		return ledger;
	}
	
	/**
	 * Adds the repair codefrom the consumption modal
	 * @param ticketId
	 * @param defectType
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void addRepairCode(String ticketId, String defectType) 
	throws InvalidDataException, DatabaseException {
		TicketDataVO rc = new TicketDataVO();
		rc.setAttributeCode("attr_unitRepairCode");
		rc.setValue(defectType);
		rc.setTicketId(ticketId);
		log.info(rc);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(rc);
	}
	
	/**
	 * Saves the used/consumption values for the parts,
	 * and decrements from the CAS inventory.
	 * 
	 * @param req
	 * @throws ActionException 
	 * @throws SQLException 
	 */
	protected void consumeParts(ActionRequest req) throws ActionException {
		List<PartVO> partsList = new ArrayList<>();
		
		// Build the PartVOs from the submitted data
		for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
			String paramName = StringUtil.checkVal(param.getKey());
			if (!paramName.startsWith("qnty_") || req.getIntegerParameter(paramName) <= 0)
				continue;

			PartVO part = new PartVO();
			String partId = paramName.substring(5);
			part.setPartId(partId);
			part.setProductId(req.getParameter("prodId_" + partId));
			part.setUsedQuantityNo(req.getIntegerParameter(paramName));
			part.setUpdateDate(new Date());
			partsList.add(part);
		}
		
		// Save the consumed values and decrement inventory
		saveConsumption(partsList);
		decrementInventory(req.getParameter(WSLAConstants.TICKET_ID), partsList);
	}
	
	/**
	 * Saves the quantities that were consumed on the requested ticket parts
	 * 
	 * @param partsList
	 * @throws ActionException 
	 */
	private void saveConsumption(List<PartVO> partsList) throws ActionException {
		if (partsList == null || partsList.isEmpty()) return;
		
		// Create the SQL to update the consumption for each part
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, getCustomSchema(), 
					"wsla_part set used_qnty_no = ?, update_dt = ? where part_id = ?");
		log.debug(sql);
		
		// Create the lists of values to be batch inserted from the VOs
		Map<String, List<Object>> psValues = new HashMap<>();
		for (PartVO part : partsList) {
			List<Object> recValues = Arrays.asList(part.getUsedQuantityNo(), part.getUpdateDate(), part.getPartId());
			psValues.put(part.getPartId(), recValues);
		}
		
		// Insert the records
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			dbp.executeBatch(sql, psValues);
		} catch (DatabaseException e) {
			throw new ActionException(e);
		}
	}
	
	/**
	 * Decrements the inventory for the CAS assigned to the ticket
	 * 
	 * @param ticketId
	 * @param partsList
	 * @throws ActionException 
	 */
	private void decrementInventory(String ticketId, List<PartVO> partsList) throws ActionException {
		if (partsList == null || partsList.isEmpty()) return;
		
		// Get the CAS location so we can update their inventory with the consumed parts
		String casLocationId = null;
		try {
			casLocationId = getCasLocationId(ticketId);
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		// Decrement what was used from the CAS location's inventory
		InventoryAction ia = new InventoryAction(getAttributes(), getDBConnection());
		for (PartVO part : partsList) {
			ia.recordInventory(part.getProductId(), casLocationId, part.getUsedQuantityNo() * -1);
		}
	}
}

