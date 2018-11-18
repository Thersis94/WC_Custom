package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: LogisticsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Oversees Shipment creation (by WSLA or OEM) and ingest (by CAS recipient).
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 6, 2018
 * @updates:
 ****************************************************************************/
public class LogisticsAction extends SBActionAdapter {

	public static final String REQ_TICKET_ID = "ticketId";

	public LogisticsAction() {
		super();
	}

	public LogisticsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public LogisticsAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		//lookup the destination locationId for this shipment, based on ticketId
		if (req.hasParameter("isDestLookup")) {
			putModuleData(findDestLocnId(req.getParameter(REQ_TICKET_ID)));
			return;
		}

		UserDataVO userData = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = userData != null ? (UserVO) userData.getUserExtendedInfo() : null;
		String toLocnId = req.getParameter("toLocationId");
		ShipmentStatus sts = EnumUtil.safeValueOf(ShipmentStatus.class, req.getParameter("status"));

		setModuleData(getData(user, toLocnId, sts, new BSTableControlVO(req, ShipmentVO.class)));
	}


	/**
	 * Load the locationId of the CAS for the given ticket
	 * @param parameter
	 * @return
	 */
	private GenericVO findDestLocnId(String ticketId) {
		String schema = getCustomSchema();
		String sql = StringUtil.join("select location_id as key from ", schema, 
				"wsla_ticket_assignment where ticket_id=? and assg_type_cd=?");
		log.debug(sql);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql, Arrays.asList(ticketId, TypeCode.CAS), new GenericVO());
		return !data.isEmpty() ? data.get(0) : new GenericVO();
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
				boolean isInsert = StringUtil.isEmpty(vo.getShipmentId());
				//make sure shipmentDt gets set if status is shipped
				if (ShipmentStatus.SHIPPED.equals(vo.getStatus()) && vo.getShipmentDate() == null)
					vo.setShipmentDate(Calendar.getInstance().getTime());

				db.save(vo);

				//if this is a new shipment getting created, automatically put all the parts from the ticket into it
				//the admin can remove or add on the next screen, but this is a significant convenience for them.
				if (isInsert && req.hasParameter(REQ_TICKET_ID)) {
					addTicketPartsToShipment(vo.getShipmentId(), req.getParameter(REQ_TICKET_ID));
				}
			}

		} catch (Exception e) {
			log.error("could not save shipment", e);
		}
	}


	/**
	 * Add the ticket's parts to the shipment if they're not already allocated elsewhere
	 * @param partIds
	 * @param schema
	 */
	private void addTicketPartsToShipment(String shipmentId, String ticketId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_part ");
		sql.append("set shipment_id=?, update_dt=? where ticket_id=? and shipment_id is null");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, shipmentId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, ticketId);
			int cnt = ps.executeUpdate();
			log.debug(String.format("added %d parts to shipment %s", cnt, shipmentId));

		} catch(SQLException sqle) {
			log.error("could not add parts to shipment", sqle);
		}
	}


	/**
	 * Return a list of products tied to tickets that are status=HarvestPendingApproval.
	 * In this view we only care about the product, so the OEM can approve or reject the request.
	 * @param locationId location who's products to load
	 * @param bst vo to populate data into
	 * @return
	 */
	public GridDataVO<ShipmentVO> getData(UserVO user, String toLocationId, ShipmentStatus status, BSTableControlVO bst) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, p.*, pm.product_nm, t.ticket_no, ");
		sql.append("srclcn.location_nm as from_location_nm, destlcn.location_nm as to_location_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_shipment s ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_part p on s.shipment_id=p.shipment_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location srclcn on s.from_location_id=srclcn.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location destlcn on s.to_location_id=destlcn.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master pm on p.product_id=pm.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_ticket t on p.ticket_id=t.ticket_id ");
		sql.append("where (s.status_cd != ? or (s.status_cd=? and coalesce(s.shipment_dt, s.update_dt, s.create_dt) > CURRENT_DATE-31)) "); //only show ingested items for 30 days past receipt
		params.add(ShipmentStatus.RECEIVED.toString());
		params.add(ShipmentStatus.RECEIVED.toString());

		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(pm.product_nm) like ? or lower(pm.cust_product_id) like ? ");
			sql.append("or lower(t.ticket_no) like ? or lower(s.carrier_tracking_no) like ? ");
			sql.append("or lower(destlcn.location_nm) like ? or lower(srclcn.location_nm) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
		}

		if (!StringUtil.isEmpty(toLocationId)) {
			sql.append("and s.to_location_id=? ");
			params.add(toLocationId);
		}

		if (status != null) {
			sql.append("and s.status_cd=? ");
			params.add(status);
		}

		//TODO limit scope based on user's role
		//sql.append("and (s.from_location_id=? or s.to_location_id=?) ");
		//params.add(user.getLocationId());
		//params.add(user.getLocationId());

		sql.append(bst.getSQLOrderBy("s.create_dt desc, pm.product_nm",  "asc"));
		log.debug(sql);
		log.debug(String.format("userLocationId=%s", user.getLocationId()));

		//after query, adjust the count to be # of unique shipments, not total SQL rows
		bst.setLimit(10000);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		GridDataVO<ShipmentVO> grid = db.executeSQLWithCount(sql.toString(), params, new ShipmentVO(), "shipment_id", bst);
		grid.setSqlTotal(new SQLTotalVO(grid.getRowData().size()));
		return grid;
	}
}