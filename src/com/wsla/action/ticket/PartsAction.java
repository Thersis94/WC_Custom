package com.wsla.action.ticket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;

/****************************************************************************
 * <b>Title</b>: PartsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Ticket Parts relationship.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 02, 2018
 * @updates:
 ****************************************************************************/
public class PartsAction extends SBActionAdapter {

	public PartsAction() {
		super();
	}

	public PartsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public PartsAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		setModuleData(listTicketParts(ticketId, new BSTableControlVO(req, PartVO.class)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		PartVO vo = new PartVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (Exception e) {
			log.error("could not save part", e);
		}
	}


	/**
	 * Batch update the received quantities for the given parts.
	 * @param parts
	 * @throws ActionException
	 */
	public void saveQntyRcvd(PartVO... parts) throws ActionException {
		if (parts == null || parts.length == 0) return;
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, getCustomSchema(), 
				"wsla_part set rcvd_qnty_no=?, update_dt=? where part_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (PartVO vo : parts) {
				ps.setInt(1, vo.getQuantityReceived());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getPartId());
				ps.addBatch();
			}
			int[] rows = ps.executeBatch();
			log.debug(String.format("updated %d part quantities", rows.length));

		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		}
	}


	/**
	 * List parts tied to the given ticket
	 * @param ticketId
	 * @param bst
	 * @return
	 */
	protected GridDataVO<PartVO> listTicketParts(String ticketId, BSTableControlVO bst) {
		//always fetch all rows, the UI is not paginated here:
		bst.setLimit(10000);
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select p.*, pm.product_nm, lim.actual_qnty_no, s.status_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_part p ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master pm on p.product_id=pm.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_ticket_assignment ta on p.ticket_id=ta.ticket_id and ta.assg_type_cd='CAS' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location pl on ta.location_id=pl.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_shipment s on p.shipment_id=s.shipment_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_location_item_master lim on pl.location_id=lim.location_id and p.product_id=lim.product_id ");
		sql.append("where p.ticket_id = ? and (s.shipment_type_cd = ? or s.shipment_type_cd is null) ");

		sql.append(bst.getSQLOrderBy("pm.product_nm",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(ticketId, ShipmentType.PARTS_REQUEST.name()), new PartVO(), bst);
	}
}