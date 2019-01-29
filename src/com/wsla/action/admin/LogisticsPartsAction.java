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
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.PartsAction;
import com.wsla.action.ticket.ShipmentAction;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction;
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
		if (!user.getLocationId().equals(shipment.getToLocationId()) && !WSLARole.ADMIN.getRoleId().equals(roleId)) {
			throw new ActionException("this user can not receive the shipment");
		}

		// Mark the shipment as complete
		markShipmentComplete(shipment, req.getParameter("comments"));
		
		// If this is for a service order, change the ticket's status
		boolean isHarvest = false;
		boolean isRepair = false;
		if (!StringUtil.isEmpty(shipment.getTicketId())) {
			// Determine if this is a harvest/repair ticket and set status accordingly
			RefundReplacementTransaction rrt = new RefundReplacementTransaction(getDBConnection(), getAttributes());
			RefundReplacementVO refRep = rrt.getRefRepVoByTicketId(shipment.getTicketId());
			isHarvest = DispositionCodes.RETURN_HARVEST.name().equals(refRep.getUnitDisposition());
			isRepair = DispositionCodes.RETURN_REPAIR.name().equals(refRep.getUnitDisposition());
			updateTicketStatus(shipment, refRep, user, isHarvest, isRepair);
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
	 * Sets up what is required for harvesting a unit after shipment has been
	 * received in the refund/replacement process.
	 * 
	 * @param ticketId
	 * @param isHarvest
	 */
	private void processHarvest(String ticketId, boolean isHarvest) {
		if (!isHarvest) return;
		
		HarvestApprovalAction haa = new HarvestApprovalAction(getAttributes(), getDBConnection());
		haa.createBOM(null, ticketId);
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
	 * @param refRep
	 * @param user
	 * @param isHarvest
	 * @param isRepair
	 * @throws ActionException
	 */
	private void updateTicketStatus(ShipmentVO shipment, RefundReplacementVO refRep, UserVO user, boolean isHarvest, boolean isRepair) throws ActionException {
		StatusCode status = StringUtil.isEmpty(refRep.getRefundReplacementId()) ? StatusCode.PARTS_RCVD_CAS : StatusCode.DEFECTIVE_RCVD;
		
		try {
			// Do the first (or only in some cases) status change
			BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
			TicketLedgerVO ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), status, LedgerSummary.SHIPMENT_RECEIVED.summary, null);
			
			// Do a second status change if this was a harvest/repair ticket
			if (isHarvest || isRepair) {
				LedgerSummary ls = isHarvest ? LedgerSummary.HARVEST_AFTER_RECEIPT : LedgerSummary.REPAIR_AFTER_RECEIPT;
				status = isHarvest ? StatusCode.HARVEST_APPROVED : StatusCode.CLOSED;
				ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), status, ls.summary, null);
			}
			
			Map<String, Object> params = new HashMap<>();
			params.put("ticketId", ledger.getTicketId());
			bta.buildNextStep(ledger.getStatusCode(), params, false);
			putModuleData(bta.getNextStep());
		} catch (DatabaseException e) {
			throw new ActionException(e);
		}
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