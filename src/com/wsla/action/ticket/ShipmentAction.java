package com.wsla.action.ticket;

import java.util.Arrays;
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
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.ShipmentVO;

/****************************************************************************
 * <b>Title</b>: PartsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Read-only rendering of a Ticket's shipments (inclusive of Parts).
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 02, 2018
 * @updates:
 ****************************************************************************/
public class ShipmentAction extends SBActionAdapter {

	public ShipmentAction() {
		super();
	}

	public ShipmentAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ShipmentAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String ticketId = req.getParameter("ticketId");
		setModuleData(loadShipments(ticketId, new BSTableControlVO(req, ShipmentVO.class)));
	}


	/**
	 * Save the VO to the database
	 * @param vo
	 * @throws ActionException
	 */
	public void saveShipment(ShipmentVO vo) throws ActionException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(vo);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Return the shipments tied to the given ticket (through parts)
	 * @param ticketId
	 * @param bst
	 * @return
	 */
	protected GridDataVO<ShipmentVO> loadShipments(String ticketId, BSTableControlVO bst) {
		GridDataVO<ShipmentVO> vo = new GridDataVO<>();
		vo.setRowData(listShipments(ticketId, null, bst.getSQLOrderBy("s.shipment_dt, pm.product_nm", "asc")));
		return vo;
	}


	/**
	 * Load a list of Shipments either by shipmentId or if empty ticketId (the default)
	 * @param ticketId
	 * @param shipmentId
	 * @param orderBy
	 * @return
	 */
	public List<ShipmentVO> listShipments(String ticketId, String shipmentId, String orderBy) {
		String schema = getCustomSchema();
		String lookupId = ticketId;
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, p.*, pm.product_nm, frm.location_nm as from_location_nm, dst.location_nm as to_location_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_shipment s ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_part p on s.shipment_id=p.shipment_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master pm on p.product_id=pm.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location frm on frm.location_id=s.from_location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location dst on dst.location_id=s.to_location_id ");
		sql.append("where "); //ticketId is part of the join constraint

		if (!StringUtil.isEmpty(shipmentId)) {
			sql.append("s.shipment_id=? ");
			lookupId = shipmentId;
		} else {
			sql.append("s.ticket_id=? ");
		}

		if (StringUtil.isEmpty(orderBy))
			orderBy = "order by s.shipment_dt, pm.product_nm";

		sql.append(orderBy);
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(lookupId), new ShipmentVO());
	}
}