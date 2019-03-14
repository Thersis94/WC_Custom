package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.transaction.TicketDataTransaction;
import com.wsla.action.ticket.transaction.TicketTransaction;
import com.wsla.data.ticket.HarvestApprovalVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.ProductHarvestVO;
import com.wsla.data.ticket.ProductHarvestVO.OutcomeCode;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
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
	 * this method exists to control the changing of the states related to harvesting a tv
	 * @param plateStatus
	 * @param ticketId
	 * @param user 
	 * @param summary
	 */
	public void processHarvestStatus(SerialPlateStatus plateStatus, String ticketId, UserVO user, String summary) {
		if (SerialPlateStatus.HARVEST_COMPLETED == plateStatus) {
			try {
				completeHarvest(ticketId, user, summary);
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
	 * @param summary
	 * @throws Exception 
	 */
	private void completeHarvest(String ticketId, UserVO user, String summary) throws Exception {
		TicketTransaction tta = new TicketTransaction(getAttributes(), getDBConnection());
		TicketDataTransaction tdt = new TicketDataTransaction(getDBConnection(), getAttributes());

		//save the ticket data object to not shipped
		tdt.saveDataAttribute(ticketId, SN_PLATE_SHIP_ATTRIBUTE, SerialPlateStatus.NOT_SHIPPED.name(), true);

		//save the status of the harvest in the data attribute
		tdt.saveDataAttribute(ticketId, HARVEST_STATUS_ATTRIBUTE, StatusCode.HARVEST_COMPLETE.name(), true);
		
		//set the ticket status to harvest complete changing the location to decommissioned
		tta.addLedger(ticketId, user.getUserId(), StatusCode.HARVEST_COMPLETE, LedgerSummary.HARVEST_COMPETE.summary + summary, UnitLocation.DECOMMISSIONED);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		String ticketId = StringUtil.checkVal(req.getStringParameter("ticketId"));
		String productSerialId = StringUtil.checkVal(req.getStringParameter("productSerialId"));
		String locationId = StringUtil.checkVal(req.getStringParameter("locationId"));
		
		// Build the HarvestVOs from the submitted data, create a list of products to add into the ticket ledger
		List<ProductHarvestVO> harvestList = new ArrayList<>();
		List<String> productNames = new ArrayList<>();
		for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
			String paramName = StringUtil.checkVal(param.getKey());
			if (!paramName.startsWith("qnty_") || req.getIntegerParameter(paramName) <= 0)
				continue;

			// Get the product harvest data from the request
			ProductHarvestVO product = new ProductHarvestVO();
			String productId = paramName.substring(5);
			product.setProductId(productId);
			product.setProductSerialId(productSerialId);
			product.setOutcomeCode(OutcomeCode.RECLAIMED);
			product.setCreateDate(new Date());
			product.setQuantity(req.getIntegerParameter(paramName));
			product.setLocationId(locationId);
			product.setProductName(req.getParameter("name_" + productId));
			
			harvestList.add(product);
			productNames.add(StringUtil.join(product.getProductName(), " (", Integer.toString(product.getQuantity()), ")"));
		}
		
		// Insert the harvest records
		if (!harvestList.isEmpty()) {
			DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
			try {
				dbp.executeBatch(harvestList);
			} catch (DatabaseException e) {
				throw new ActionException(e);
			}
		}
		
		// Increment inventory for the parts that were recovered
		incrementInventory(harvestList);
		
		// Update the harvest status
		processHarvestStatus(SerialPlateStatus.HARVEST_COMPLETED, ticketId, user, String.join(", ", productNames));
	}

	/**
	 * Call InventoryAction to increment the product inventory at this location
	 * @param harvestList
	 * @throws ActionException 
	 */
	private void incrementInventory(List<ProductHarvestVO> harvestList) throws ActionException {
		for (ProductHarvestVO product : harvestList) {
			InventoryAction inv = new InventoryAction(getAttributes(), getDBConnection());
			inv.recordInventory(product.getProductId(), product.getLocationId(), product.getQuantity());
		}
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
		sql.append("select p.*, ps.*, t.ticket_id, t.ticket_no, t.status_cd, pl.location_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial ps on t.product_serial_id=ps.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_data td on t.ticket_id = td.ticket_id and td.attribute_cd = ? and td.value_txt = ? ");
		params.add(HARVEST_STATUS_ATTRIBUTE);
		params.add(StatusCode.HARVEST_APPROVED.name());
		
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment ta on t.ticket_id = ta.ticket_id and ta.assg_type_cd = ? ");
		params.add(TypeCode.CAS.name());
		if(!StringUtil.isEmpty(locationId)) {
			sql.append("and ta.location_id = ? ");
			params.add(locationId);
		}
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location pl on ta.location_id = pl.location_id ");
		
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
		
		StringBuilder sql = new StringBuilder(550);
		sql.append(DBUtil.SELECT_CLAUSE).append("ph.product_harvest_id, pm.product_id, ps.product_serial_id, ph.outcome_cd, ");
		sql.append("ph.create_dt, ph.update_dt, ph.qnty_no, pset.qnty_no as set_qnty_no, ph.note_txt, pm.*");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_product_serial ps");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_set pset on ps.product_id = pset.set_id");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master pm on pset.product_id = pm.product_id");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_harvest ph on pm.product_id = ph.product_id and ps.product_serial_id = ph.product_serial_id");
		sql.append(DBUtil.WHERE_CLAUSE).append("ps.product_serial_id = ? ");
		sql.append(bst.getSQLOrderBy("pm.product_nm",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(productSerialId), new ProductHarvestVO(), "product_id",  bst.getLimit(), bst.getOffset());
	}
}