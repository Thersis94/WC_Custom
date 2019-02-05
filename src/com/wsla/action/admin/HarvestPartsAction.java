package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.transaction.TicketDataTransaction;
import com.wsla.action.ticket.transaction.TicketTransaction;
import com.wsla.data.ticket.HarvestApprovalVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.ProductHarvestVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: HarvestPartsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Orchastrates a technician harvesting reusable components from a TV/set.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 31, 2018
 * @updates:
 ****************************************************************************/
public class HarvestPartsAction extends SBActionAdapter {
	
	/**
	 * this enum exists to help control and organize the process for shipping back 
	 * serial number stickers/plates to wsla
	 */
	public enum SerialPlateStatus {
		HARVEST_COMPLETED, DISPOSE_COMPLETED, NOT_SHIPPED, SHIPPING, RECEIVIED;
	}
	
	public static final String SN_PLATE_SHIP_ATTRIBUTE = "attr_snPlateShipStatus";
	public static final String HARVEST_STATUS_ATTRIBUTE = "attr_harvest_status";
	
	public static final String SO_NAME = "soName";

	public HarvestPartsAction() {
		super();
	}

	public HarvestPartsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public HarvestPartsAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		String productSerialId = req.getParameter("productSerialId");
		Object data;
		
		if(req.hasParameter("harvestStatus")) {
			String ticketId = StringUtil.checkVal(req.getStringParameter("ticketId"));
			if(req.hasParameter(SO_NAME)) {
				String soName = req.getStringParameter(SO_NAME);
				try {
					ticketId = getTicketIdBySONumber(req.getParameter(SO_NAME));
					log.debug("ticket id " + ticketId + " found for service order number " + soName );
				} catch (InvalidDataException e) {
					log.error("could not get ticket with supplied service order number",e);
					putModuleData(soName, 0, false, e.getLocalizedMessage(), true);
				}
			}
			
			UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
			processHarvestStatus(ticketId, StringUtil.checkVal(req.getStringParameter("harvestStatus")),user);
			return;
		}

		// load a single product (set), inclusive of all it's parts, joined to the product_harvest table for dissection view.
		if (!StringUtil.isEmpty(productSerialId)) {
			data = loadBOM(productSerialId, new BSTableControlVO(req, ProductHarvestVO.class));
		} else {
			// load a list of products approved for harvesting if a single product was 
			//not requested.  This is based on ticket status.
			String locationId = req.getStringParameter("locationId");
			data = listProducts(new BSTableControlVO(req, ProductHarvestVO.class), locationId);
		}
		setModuleData(data);
	}

	/**
	 * @param parameter
	 * @return
	 * @throws InvalidDataException 
	 */
	private String getTicketIdBySONumber(String soName) throws InvalidDataException {
		List<Object> vals = new ArrayList<>();
		vals.add(soName);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		StringBuilder sql = new StringBuilder(63);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket where ticket_no = ? ");
		
		List<TicketVO> data = db.executeSelect(sql.toString(), vals, new TicketVO());
		
		if(data != null && !data.isEmpty()) {
			return data.get(0).getTicketId();
		}else {
			throw new InvalidDataException("Service order not linked to a ticket");
		}
	}

	/**
	 * this method exists to control the changing of the states related to harvesting a tv
	 * @param ticketId
	 * @param user 
	 * @param harvestStatus
	 */
	public void processHarvestStatus(String ticketId, String plateStatus, UserVO user) {
		
		if (SerialPlateStatus.HARVEST_COMPLETED.name().equalsIgnoreCase(plateStatus)) {
			try {
				completeHarvest(ticketId, user);
			} catch (Exception e) {
				log.error("could not complete harvest ",e);
				putModuleData(ticketId, 0, false, e.getLocalizedMessage(), true);
			}
		}
		
	}

	/**
	 * this method handles processing attributes and status after a ticket enters harvest completed.
	 * @param ticketId
	 * @param user 
	 * @throws Exception 
	 */
	private void completeHarvest(String ticketId, UserVO user) throws Exception {
		TicketTransaction tta = new TicketTransaction();
		tta.setActionInit(actionInit);
		tta.setAttributes(getAttributes());
		tta.setDBConnection(getDBConnection());
		//save the ticket data object to not shipped
		TicketDataTransaction tdt = new TicketDataTransaction();
		tdt.setActionInit(actionInit);
		tdt.setAttributes(getAttributes());
		tdt.setDBConnection(getDBConnection());
		tdt.saveDataAttribute(ticketId, SN_PLATE_SHIP_ATTRIBUTE, SerialPlateStatus.NOT_SHIPPED.name(), true);

		//save the status of the harvest in the data attribute
		tdt.saveDataAttribute(ticketId, HARVEST_STATUS_ATTRIBUTE, StatusCode.HARVEST_COMPLETE.name(), true);
		
		//set the ticket status to harvest complete changing the location to decommissioned
		tta.addLedger(ticketId, user.getUserId(), StatusCode.HARVEST_COMPLETE, LedgerSummary.HARVEST_COMPETE.summary, UnitLocation.DECOMMISSIONED);

	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductHarvestVO vo = new ProductHarvestVO(req);
		//mark harvest complete
		updateRecord(vo);
		//increment inventory if parts were recovered
		if (vo.getQuantity() > 0)
			incrementInventory(vo);
	}


	/**
	 * Update the product_harvest database record to reflect the outcome
	 * @param vo
	 * @throws ActionException 
	 */
	private void updateRecord(ProductHarvestVO vo) throws ActionException {
		//update the harvest record to capture the outcome:
		String schema = getCustomSchema();
		String sql = StringUtil.join("update ", schema, "wsla_product_harvest ",
				"set outcome_cd=?, note_txt=?, qnty_no=?, update_dt=CURRENT_TIMESTAMP where product_harvest_id=?");
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		try {
			db.executeSqlUpdate(sql, vo, Arrays.asList("OUTCOME_CD", "NOTE_TXT", "QNTY_NO", "PRODUCT_HARVEST_ID"));
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Call InventoryAction to increment the product inventory at this location
	 * @param vo
	 * @param locationId
	 * @throws ActionException 
	 */
	private void incrementInventory(ProductHarvestVO vo) throws ActionException {
		InventoryAction inv = new InventoryAction(getAttributes(), getDBConnection());
		inv.recordInventory(vo.getProductId(), vo.getLocationId(), vo.getQuantity());
	}

	/**
	 * overwrite to accept only table 
	 * @param bst
	 * @return
	 */
	protected GridDataVO<HarvestApprovalVO> listProducts(BSTableControlVO bst) {
		return listProducts( bst , null);
	}

	/**
	 * Generate a list of products ready for harvesting
	 * @param locationId 
	 * @param bsTableControlVO
	 * @return
	 */
	protected GridDataVO<HarvestApprovalVO> listProducts(BSTableControlVO bst, String locationId) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select p.*, ps.*, t.ticket_id, t.ticket_no, t.status_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial ps on t.product_serial_id=ps.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_data td on t.ticket_id = td.ticket_id and td.attribute_cd ='attr_harvest_status' and td.value_txt = 'HARVEST_APPROVED' ");
		
		if(!StringUtil.isEmpty(locationId)) {
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment ta on t.ticket_id = ta.ticket_id and ta.assg_type_cd ='CAS' and ta.location_id = ? ");
			params.add(locationId);
		}
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master p on ps.product_id=p.product_id ");
		sql.append("where 1=1 ");

		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(p.product_nm) like ? or lower(p.cust_product_id) like ? or lower(ps.serial_no_txt) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
		}

		sql.append(bst.getSQLOrderBy("p.product_nm, ps.serial_no_txt",  "asc"));
		log.debug("sql: "+ sql+ "|"+params);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new HarvestApprovalVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Generate a bill of materials (BOM) for the given Unit that the tech is expected to harvest from it
	 * @param productSerialId
	 * @param bsTableControlVO
	 * @return
	 */
	public GridDataVO<ProductHarvestVO> loadBOM(String productSerialId, BSTableControlVO bst) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("wsla_product_harvest ph ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master p on ph.product_id=p.product_id ");
		sql.append("where ph.product_serial_id=? and ph.outcome_cd is null "); //omit what's already complete
		sql.append(bst.getSQLOrderBy("p.product_nm",  "asc"));
		log.debug(sql);
		log.debug("serial id = " + productSerialId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(productSerialId), new ProductHarvestVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Creates line-items of the parts to be harvested from the given Unit (a Product Set).
	 * Use a single query to identify and insert the records - we don't need to read from the DB
	 * @param parameter
	 */
	public void prepareHarvest(String productSerialId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(500);
		sql.append("insert into ").append(schema).append("wsla_product_harvest ");
		sql.append("(product_harvest_id, product_id, product_serial_id, qnty_no, create_dt) ");
		//the product w/serial# is actaually a set (TV set) to be harvested - it's internal components are what we want to capture.
		sql.append("select replace(newid(),'-',''), pm.product_id, ps.product_serial_id, pset.qnty_no, CURRENT_TIMESTAMP ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_product_serial ps ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_set pset on ps.product_id=pset.set_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master pm on pset.product_id=pm.product_id ");
		sql.append("where ps.product_serial_id=?");
		log.debug(sql);

		int partCnt = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productSerialId);
			partCnt = ps.executeUpdate();
			log.debug(String.format("Flagged %d parts for harvesting", partCnt));

		} catch (SQLException sqle) {
			log.error(String.format("could not prepare harvest for %s", productSerialId), sqle);
		}
	}
}