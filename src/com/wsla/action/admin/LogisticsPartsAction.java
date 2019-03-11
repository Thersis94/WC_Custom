package com.wsla.action.admin;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.PartsAction;
import com.wsla.action.ticket.ShipmentAction;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction.ApprovalTypes;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction.DispositionCodes;
import com.wsla.action.ticket.transaction.TicketCloneTransaction;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;

/****************************************************************************
 * <b>Title</b>: LogisticsPartsAction.java<br>
 * <b>Project</b>: WC_Custom<br>
 * <b>Description: </b> Manages the Parts inside a Shipment 
 * 		(from the Admin UI, see {@link com.wsla.action.ticket.PartsAction} for Ticket side)<br>
 * <b>Copyright:</b> Copyright (c) 2018<br>
 * <b>Company:</b> Silicon Mountain Technologies<br>
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 9, 2018
 * @updates:
 ****************************************************************************/
public class LogisticsPartsAction extends SBActionAdapter {

	public LogisticsPartsAction() {
		super();
	}

	public LogisticsPartsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public LogisticsPartsAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * The users viewing this page are Warehouse, OEM, or CAS (Role).
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(listParts(req.getParameter("shipmentId"), new BSTableControlVO(req, PartVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("completeIngest")) {
			completeIngest(req);
		} else {
			// Saving a single part - used when creating the shipment, not for ingest
			PartsAction partsAction = new PartsAction(getAttributes(), getDBConnection());
			partsAction.build(req);
		}
	}
	
	/**
	 * Completes a shipment, adding parts/units to inventory
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void completeIngest(ActionRequest req) throws ActionException {
		PartsAction partsAction = new PartsAction(getAttributes(), getDBConnection());
		
		// Load the shipment
		ShipmentVO shipment = loadShipment(req.getParameter("shipmentId"));
		if (shipment == null) throw new ActionException("null data - shipment not found");
		
		// Enforce front end security
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		secureShipmentReceipt(user, roleId, shipment);

		// Mark the shipment as complete
		markShipmentComplete(shipment, req.getParameter("comments"));
		boolean isUnitMovement = ShipmentType.UNIT_MOVEMENT == shipment.getShipmentType();
		
		// If this is for a service order, change the ticket's status
		boolean isHarvest = false;
		boolean isRepair = false;
		boolean hasPending = false;
		if (!StringUtil.isEmpty(shipment.getTicketId())) {
			// Determine if this is a harvest/repair ticket, and whether there is a corresponding shipment
			RefundReplacementTransaction rrt = new RefundReplacementTransaction(getDBConnection(), getAttributes());
			RefundReplacementVO refRep = rrt.getRefRepVoByTicketId(shipment.getTicketId());
			DispositionCodes disposition = EnumUtil.safeValueOf(DispositionCodes.class, refRep.getUnitDisposition());
			isHarvest = DispositionCodes.RETURN_HARVEST == disposition && isUnitMovement;
			isRepair = DispositionCodes.RETURN_REPAIR == disposition && isUnitMovement;
			hasPending = releasePendingShipment(shipment, refRep);
			
			// Set status accordingly
			updateTicketStatus(shipment, user, isHarvest, isRepair, hasPending);
		}

		// Process harvest (create BOM) if necessary
		processHarvest(shipment.getTicketId(), isHarvest);

		// If shipping src=dest, or if the unit will be harvested, don't modify inventory
		if (StringUtil.checkVal(shipment.getFromLocationId()).equals(shipment.getToLocationId()) || isHarvest)
			return;

		// Load the list of parts
		List<PartVO> parts = shipment.getParts();
		if (parts == null || parts.isEmpty()) return; //odd to receive an empty box, but permissible in the UI

		// Receive the parts or unit & adjust inventory
		saveQntyReceived(req, parts, partsAction);
		adjustInventories(shipment, parts);
		
		// If this was a repair, clone into a new ticket (CAS = WSLA, status = in repair, owner = WSLA)
		processRepair(shipment.getTicketId(), user, isRepair);
	}

	/**
	 * Decides whether the user can ingest a given shipment
	 * 
	 * @param user
	 * @param roleId
	 * @param shipment
	 * @throws ActionException
	 */
	private void secureShipmentReceipt(UserVO user, String roleId, ShipmentVO shipment) throws ActionException {
		if (!user.getLocationId().equals(shipment.getToLocationId()) && !WSLARole.ADMIN.getRoleId().equals(roleId))
			throw new ActionException("this user can not receive the shipment");
	}
	
	/**
	 * Sets up what is required for harvesting a unit after shipment has been
	 * received in the refund/replacement process.
	 * 
	 * @param ticketId
	 * @param isHarvest
	 * @throws ActionException 
	 */
	private void processHarvest(String ticketId, boolean isHarvest) throws ActionException {
		if (!isHarvest) return;
		
		// Create the BOM to be harvested, set the harvesting status
		HarvestApprovalAction haa = new HarvestApprovalAction(getAttributes(), getDBConnection());
		haa.approveHarvest(ticketId);
	}
	
	/**
	 * Sets up what is required for repairing a unit after shipment has been
	 * received in the refund/replacement process.
	 * 
	 * @param ticketId
	 * @param user
	 * @param isRepair
	 * @throws ActionException 
	 */
	private void processRepair(String ticketId, UserVO user, boolean isRepair) throws ActionException {
		if (!isRepair) return;
		
		// Clone the ticket. Repairs after a return of a defective unit happen on a new ticket.
		TicketCloneTransaction tct = new TicketCloneTransaction(getDBConnection(), getAttributes());
		tct.cloneTicketToWSLA(ticketId, user);
	}
	
	/**
	 * Returns the shipment data for the given id
	 * 
	 * @param shipmentId
	 * @return
	 */
	private ShipmentVO loadShipment(String shipmentId) {
		ShipmentAction sa = new ShipmentAction(getAttributes(), getDBConnection());
		List<ShipmentVO> lst = sa.listShipments(null, shipmentId, null);
		
		return !lst.isEmpty() ? lst.get(0) : null;
	}
	
	/**
	 * Sets a shipment as complete/received
	 * 
	 * @param shipment
	 * @param comments
	 * @throws ActionException
	 */
	private void markShipmentComplete(ShipmentVO shipment, String comments) throws ActionException {
		ShipmentAction sa = new ShipmentAction(getAttributes(), getDBConnection());
		
		shipment.setStatus(ShipmentStatus.RECEIVED);
		shipment.setArrivalDate(Calendar.getInstance().getTime());
		shipment.setCommentsText(comments);
		sa.saveShipment(shipment);
	}

	/**
	 * Updates a ticket's status based on parts or unit shipping data.
	 * 
	 * @param shipment
	 * @param user
	 * @param isHarvest
	 * @param isRepair
	 * @param hasPending - indicates whether this shipment has a related pending shipment
	 * @throws ActionException
	 */
	private void updateTicketStatus(ShipmentVO shipment, UserVO user, boolean isHarvest, boolean isRepair, boolean hasPending) throws ActionException {
		StatusCode status = getIngestStatusChange(shipment.getShipmentType());
		
		try {
			// Do the first (or only in some cases) status change
			BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
			TicketLedgerVO ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), status, LedgerSummary.SHIPMENT_RECEIVED.summary, null);
			
			// Do a second status change if this was a unit ticket
			if (isHarvest || isRepair) {
				ledger = updateDispositionStatus(shipment, user, isHarvest, hasPending);
			} else if (shipment.getShipmentType() == ShipmentType.REPLACEMENT_UNIT) {
				ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), StatusCode.PENDING_UNIT_RETURN, null, null);
			}
			
			Map<String, Object> params = new HashMap<>();
			params.put("ticketId", ledger.getTicketId());
			bta.buildNextStep(ledger.getStatusCode(), params, ledger.getStatusCode() == StatusCode.CLOSED);
			putModuleData(bta.getNextStep());
		} catch (DatabaseException e) {
			throw new ActionException(e);
		}
	}
	
	/**
	 * Returns an appropriate status change for when a shipment is received.
	 * 
	 * @param type
	 * @return
	 */
	private StatusCode getIngestStatusChange(ShipmentType type) {
		switch (type) {
			case REPLACEMENT_UNIT:
				return StatusCode.RPLC_DELIVEY_RCVD;
			case UNIT_MOVEMENT:
				return StatusCode.DEFECTIVE_RCVD;
			case PARTS_REQUEST:
			default:
				return StatusCode.PARTS_RCVD_CAS;
		}
	}

	/**
	 * Adds an additional status change based on the refund/replacement disposition.
	 * If harvesting is false, then repair is assumed to be true.
	 * 
	 * @param shipment
	 * @param user
	 * @param isHarvest
	 * @param hasPending - indicates whether this shipment has a related pending shipment
	 * @return
	 * @throws DatabaseException
	 */
	private TicketLedgerVO updateDispositionStatus(ShipmentVO shipment, UserVO user, boolean isHarvest, boolean hasPending) throws DatabaseException {
		BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
		TicketLedgerVO ledger;
		
		// Add a status change & ledger entry to track harvesting
		if (isHarvest) {
			bta.changeStatus(shipment.getTicketId(), user.getUserId(), StatusCode.HARVEST_APPROVED, LedgerSummary.HARVEST_AFTER_RECEIPT.summary, null);
		}

		// Add a status change depending on whether another shipment exists or not.
		// If this is a replacement request, there will be a pending shipment. Otherwise, this is a refund request.
		if (hasPending) {
			ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), StatusCode.REPLACEMENT_CONFIRMED, null, null);
		} else {
			String summary = !isHarvest ? LedgerSummary.REPAIR_AFTER_RECEIPT.summary : null;
			ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), StatusCode.CLOSED, summary, null);
		}
		
		return ledger;
	}
	
	/**
	 * Finds a corresponding shipment for a replacement unit, and moves it from
	 * pending status to created.
	 * 
	 * @param receivedShipment
	 * @param refRep
	 * @return
	 */
	private boolean releasePendingShipment(ShipmentVO receivedShipment, RefundReplacementVO refRep) {
		ApprovalTypes approvalType = EnumUtil.safeValueOf(ApprovalTypes.class, refRep.getApprovalType());
		
		// If there is no replacement shipment, there is no need to proceed. Additionally, a
		// pending shipment should not be released until the defective unit has been received.
		if (approvalType != ApprovalTypes.REPLACEMENT_REQUEST || receivedShipment.getShipmentType() != ShipmentType.UNIT_MOVEMENT)
			return false;
		
		// Set our search criteria
		ShipmentVO searchCriteria = new ShipmentVO();
		searchCriteria.setTicketId(refRep.getTicketId());
		searchCriteria.setStatus(ShipmentStatus.PENDING);
		searchCriteria.setShipmentType(ShipmentType.REPLACEMENT_UNIT);

		// Create the update sql
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_shipment ");
		sql.append("set status_cd = '").append(ShipmentStatus.CREATED.name()).append("' ");
		sql.append("where ticket_id = ? and status_cd = ? and shipment_type_cd = ? ");
		
		// Update the pending shipment
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		int updatedRows = 0;
		try {
			updatedRows = dbp.executeSqlUpdate(sql.toString(), searchCriteria, Arrays.asList("ticket_id", "status_cd", "shipment_type_cd"));
		} catch (DatabaseException e) {
			log.error("Could not update the pending shipment", e);
		}
		
		return updatedRows > 0;
	}

	/**
	 * save the quantities received for each of the parts - comes in from the browser form.
	 * @param req
	 * @param parts
	 * @param partsAction
	 * @throws ActionException 
	 */
	private void saveQntyReceived(ActionRequest req, List<PartVO> parts,
			PartsAction partsAction) throws ActionException {
		for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
			String paramNm = StringUtil.checkVal(param.getKey());
			if (! paramNm.startsWith("qnty_")) continue;
			String partId = paramNm.substring(5);
			for (PartVO part : parts) {
				if (part.getPartId().equals(partId))
					part.setQuantityReceived(Convert.formatInteger(req.getParameter(paramNm)));
			}
		}
		partsAction.saveQntyRcvd(parts.toArray(new PartVO[parts.size()]));
	}


	/**
	 * decrement the shipper's inventory and increment the receiver's
	 * @param shipment
	 * @param parts
	 * @throws ActionException 
	 */
	private void adjustInventories(ShipmentVO shipment, List<PartVO> parts) 
			throws ActionException {
		InventoryAction ia = new InventoryAction(getAttributes(), getDBConnection());
		boolean receiverExists = !StringUtil.isEmpty(shipment.getToLocationId());
		boolean senderExists = !StringUtil.isEmpty(shipment.getFromLocationId());
		for (PartVO part : parts) {
			//no movement if no part
			if (part.getQuantityReceived() == 0) continue;

			//add what was recieved to the receiver's inventory
			if (receiverExists)
				ia.recordInventory(part.getProductId(), shipment.getToLocationId(), part.getQuantityReceived());

			//remove what was sent from the sender's inventory
			if (senderExists)
				ia.recordInventory(part.getProductId(), shipment.getFromLocationId(), 0-part.getQuantity());
		}
	}


	/**
	 * List parts tied to the given shipment
	 * @param shipmentId
	 * @param bst
	 * @return
	 */
	protected GridDataVO<PartVO> listParts(String shipmentId, BSTableControlVO bst) {
		//always fetch all rows, the UI is not paginated here:
		bst.setLimit(10000);
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select p.*, pm.*, lim.actual_qnty_no, limd.actual_qnty_no as dest_actual_qnty_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_shipment s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_part p on s.shipment_id=p.shipment_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master pm on p.product_id=pm.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_location_item_master lim on s.from_location_id=lim.location_id and p.product_id=lim.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_location_item_master limd on s.to_location_id=lim.location_id and p.product_id=lim.product_id ");
		sql.append("where s.shipment_id=? ");

		sql.append(bst.getSQLOrderBy("pm.product_nm",  "asc"));
		log.debug(sql + "|" + shipmentId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(shipmentId), new PartVO(), bst);
	}
}