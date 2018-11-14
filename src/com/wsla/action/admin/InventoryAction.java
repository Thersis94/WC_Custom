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
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.LocationItemMasterVO;

/****************************************************************************
 * <b>Title</b>: InventoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the location_item_master (location's inventory) table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 15, 2018
 * @updates:
 ****************************************************************************/
public class InventoryAction extends SBActionAdapter {

	public InventoryAction() {
		super();
	}

	public InventoryAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public InventoryAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String locationId = req.getParameter("locationId");
		setModuleData(getData(locationId, new BSTableControlVO(req, LocationItemMasterVO.class)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		LocationItemMasterVO vo = new LocationItemMasterVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save location inventory", e);
		}
	}


	/**
	 * Return a list of product inventory (counts) at the given provider_location
	 * @param locationId location who's products to load
	 * @param bst vo to populate data into
	 * @return
	 */
	public GridDataVO<LocationItemMasterVO> getData(String locationId, BSTableControlVO bst) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select lim.item_master_Id, lim.actual_qnty_no, lim.desired_qnty_no, pm.product_nm, ");
		sql.append("pm.product_id, pm.cust_product_id, lcn.location_id, lcn.location_nm, p.provider_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_location_item_master lim ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master pm on lim.product_id=pm.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location lcn on lim.location_id=lcn.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider p on lcn.provider_id=p.provider_id ");
		sql.append("where 1=1 ");

		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(p.provider_nm) like ? or lower(lcn.location_nm) like ? or lower(pm.product_nm) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
		}
		//filter by inventory location
		if (!StringUtil.isEmpty(locationId)) {
			sql.append("and lim.location_id=? ");
			params.add(locationId);
		}

		sql.append(bst.getSQLOrderBy("p.provider_nm, lcn.location_nm, pm.product_nm",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new LocationItemMasterVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Generate a list of Provider Locations (<LocationId, LocationNm> pairs) bound to product inventory.
	 * @param productIdOrModelNo productId or custProductId (model#) to filter the data by - case insensitive.
	 * @param minInventory only used with productId - ensures the matched locationId has a minimum amount of inventory on hand.
	 * @return List<GenericVO> Used for data filter box (selectpicker)
	 */
	public List<GenericVO> listInvetorySuppliers(String productIdOrModelNo, Integer minInventory) {
		String schema = getCustomSchema();
		List<Object> params = null;
		StringBuilder sql = new StringBuilder(250);
		sql.append("select lcn.location_id as key, concat(p.provider_nm, ': ', lcn.location_nm, ' ', lcn.store_no) as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_provider_location lcn ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_location_item_master lim on lcn.location_id=lim.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider p on lcn.provider_id=p.provider_id ");
		if (!StringUtil.isEmpty(productIdOrModelNo)) {
			sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master prod on lim.product_id=prod.product_id ");
			sql.append("and (prod.product_id=? or lower(prod.cust_product_id)=?) ");
			params = new ArrayList<>(Arrays.asList(productIdOrModelNo, productIdOrModelNo));
			if (minInventory != null) {
				sql.append("where lim.actual_qnty_no >= ? ");
				params.add(minInventory);
			}
		}
		sql.append("order by p.provider_nm, lcn.location_nm, lcn.store_no, lcn.location_id");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new GenericVO(), "key");
	}


	/**
	 * Add or subtract inventory from the item_master - "this product at this location"
	 * @param productId of part
	 * @param locationId holding the inventory
	 * @param qnty to increment or decrement
	 * @throws ActionException 
	 */
	public void recordInventory(String productId, String locationId, int qnty) 
			throws ActionException {
		String schema = getCustomSchema();
		String updSql = StringUtil.join("update ", schema, "wsla_location_item_master ",
				"set actual_qnty_no=actual_qnty_no+? where product_id=? and location_id=?");
		log.debug(updSql);

		int cnt = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(updSql)) {
			ps.setInt(1, qnty);
			ps.setString(2, productId);
			ps.setString(3, locationId);
			cnt = ps.executeUpdate();
			log.debug(String.format("updated %d item_master records", cnt));

		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		}
		//we're done if we updated an existing record
		if (cnt > 0) return;

		// Need to add inventory record.  Easiest done with a VO & DBProcessor
		LocationItemMasterVO vo = new LocationItemMasterVO();
		vo.setQuantityOnHand(qnty);
		vo.setProductId(productId);
		vo.setLocationId(locationId);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		try {
			db.insert(vo);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}