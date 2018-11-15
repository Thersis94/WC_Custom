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
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.HarvestApprovalVO;
import com.wsla.data.ticket.ProductHarvestVO;
import com.wsla.data.ticket.StatusCode;

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
			data = listProducts(new BSTableControlVO(req, ProductHarvestVO.class));
		}
		setModuleData(data);
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
	private void incrementInventory(ProductHarvestVO vo) 
			throws ActionException {
		InventoryAction inv = new InventoryAction(getAttributes(), getDBConnection());
		inv.recordInventory(vo.getProductId(), vo.getLocationId(), vo.getQuantity());
	}


	/**
	 * Generate a list of products ready for harvesting
	 * @param bsTableControlVO
	 * @return
	 */
	protected GridDataVO<HarvestApprovalVO> listProducts(BSTableControlVO bst) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select p.*, ps.*, t.ticket_id, t.ticket_no, t.status_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial ps on t.product_serial_id=ps.product_serial_id ");
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

		//only show harvesting-approved records
		sql.append("and t.status_cd=? ");
		params.add(StatusCode.HARVEST_APPROVED.toString());

		sql.append(bst.getSQLOrderBy("p.product_nm, ps.serial_no_txt",  "asc"));
		log.debug(sql);

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

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), Arrays.asList(productSerialId), 
				new ProductHarvestVO(), bst.getLimit(), bst.getOffset());
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