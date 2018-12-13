package com.wsla.action.admin;

import java.util.Arrays;
import java.util.Calendar;
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
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.PartsAction;
import com.wsla.action.ticket.ShipmentAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.PartVO;
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
		PartsAction partsAction = new PartsAction(getAttributes(), getDBConnection());

		if (req.hasParameter("completeIngest")) {
			//load the shipment
			ShipmentAction sa = new ShipmentAction(getAttributes(), getDBConnection());
			List<ShipmentVO> lst = sa.listShipments(null, req.getParameter("shipmentId"), null);
			ShipmentVO shipment = !lst.isEmpty() ? lst.get(0) : null;
			if (shipment == null) throw new ActionException("null data - shipment not found");

			//mark the shipment as complete
			shipment.setStatus(ShipmentStatus.RECEIVED);
			shipment.setArrivalDate(Calendar.getInstance().getTime());
			shipment.setCommentsText(req.getParameter("comments"));
			sa.saveShipment(shipment);
			
			// If this is for a service order, change the ticket's status
			if (!StringUtil.isEmpty(shipment.getTicketId())) {
				try {
					UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
					BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
					TicketLedgerVO ledger = bta.changeStatus(shipment.getTicketId(), user.getUserId(), StatusCode.PARTS_RCVD_CAS, LedgerSummary.SHIPMENT_RECEIVED.summary, null);
					bta.buildNextStep(ledger.getStatusCode(), null, false);
					putModuleData(bta.getNextStep());
				} catch (DatabaseException e) {
					throw new ActionException(e);
				}
			}

			//if shipping src=dest, don't modify inventory
			if (StringUtil.checkVal(shipment.getFromLocationId()).equals(shipment.getToLocationId()))
				return;

			//load the list of parts
			List<PartVO> parts = shipment.getParts();
			if (parts == null || parts.isEmpty()) return; //odd to receive an empty box, but permissible in the UI

			saveQntyReceived(req, parts, partsAction);

			adjustInventories(shipment, parts);

		} else {
			//saving a single part - used when creating the shipment, not for ingest
			partsAction.build(req);
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