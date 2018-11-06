package com.wsla.action.ticket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
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
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.ShipmentVO;

/****************************************************************************
 * <b>Title</b>: PartsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> WSLA Shipment Entity - interfaces to Parts.
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
		setModuleData(listShipments(ticketId, new BSTableControlVO(req, ShipmentVO.class)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ShipmentVO vo = new ShipmentVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
			saveShipmentParts(vo.getShipmentId(), req.getParameterValues("partId"));

		} catch (Exception e) {
			log.error("could not save shipment", e);
		}
	}


	/**
	 * Binds/Unbinds parts to the given Shipment
	 * @param shipmentId
	 * @param partIds in this shipment
	 */
	public void saveShipmentParts(String shipmentId, String... partIds) {
		if (partIds == null) partIds = new String[0];
		String schema = getCustomSchema();

		unbindParts(partIds, schema, shipmentId);

		if (partIds.length > 0)
			bindParts(partIds, schema, shipmentId);
	}


	/**
	 * remove any undesired parts from this shipment (or all parts)
	 * @param partIds
	 * @param schema
	 */
	private void unbindParts(String[] partIds, String schema, String shipmentId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("wsla_part ");
		sql.append("set shipment_id=?, update_dt=? where shipment_id=?");
		if (partIds.length > 0) {
			sql.append(" and part_id not in (");
			DBUtil.preparedStatmentQuestion(partIds.length, sql);
			sql.append(")");
		}
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setNull(1, Types.VARCHAR);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, shipmentId);
			for (int x=0; x < partIds.length; x++)
				ps.setString(4+x, partIds[x]);
			ps.executeUpdate();

		} catch(SQLException sqle) {
			log.error("could not remove parts from shipment", sqle);
		}
	}


	/**
	 * make sure the desired parts are included
	 * @param partIds
	 * @param schema
	 */
	private void bindParts(String[] partIds, String schema, String shipmentId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("wsla_part ");
		sql.append("set shipment_id=?, update_dt=? where part_id in (");
		DBUtil.preparedStatmentQuestion(partIds.length, sql);
		sql.append(") ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, shipmentId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			for (int x=0; x < partIds.length; x++)
				ps.setString(3+x, partIds[x]);
			ps.executeUpdate();

		} catch(SQLException sqle) {
			log.error("could not save parts to shipment", sqle);
		}
	}


	/**
	 * Return the shipments tied to the given ticket (through parts)
	 * @param ticketId
	 * @param bst
	 * @return
	 */
	protected GridDataVO<ShipmentVO> listShipments(String ticketId, BSTableControlVO bst) {
		bst.setLimit(1000); //make limit high enough to not paginate.

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, p.*, pm.product_nm, frm.location_nm as from_location_nm, dst.location_nm as to_location_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_shipment s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_part p on s.shipment_id=p.shipment_id and p.ticket_id=? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master pm on p.product_id=pm.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location frm on frm.location_id=s.from_location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location dst on dst.location_id=s.to_location_id ");
		sql.append("where  1=1"); //ticketId is part of the join constraint

		sql.append(bst.getSQLOrderBy("s.shipment_dt",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(ticketId), new ShipmentVO(), bst);
	}
}