package com.wsla.action.admin;

import static com.wsla.action.admin.ProductMasterAction.REQ_PRODUCT_ID;

// JDK 1.8.x
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
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.data.product.InventoryLedgerVO;
import com.wsla.data.product.LocationItemMasterVO;
import com.wsla.data.ticket.UserVO;

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
		
		if (req.hasParameter("suppliers")) {
			String partId = req.getParameter(REQ_PRODUCT_ID, req.getParameter("custProductId"));
			Integer min = req.getIntegerParameter("minInventory", 0);
			setModuleData(listInvetorySuppliers(partId, min, null));
		} else {
			String locationId;
		
			// Inventory management should always be filtered by the user's location
			String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
			if (!WSLARole.ADMIN.getRoleId().equals(roleId)) {
				UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
				locationId = user.getLocationId();
			} else {
				locationId = req.getParameter("locationId");
			}
			
			BSTableControlVO bst = new BSTableControlVO(req, LocationItemMasterVO.class);
			String orderBy =  bst.getSQLOrderBy("p.provider_nm, lcn.location_nm, pm.product_nm",  "asc");
			setModuleData(listInventory(locationId, orderBy, bst, false));
		}
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
				String roleId = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
				UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
				saveInventoryRecord(vo, roleId, user);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save location inventory", e);
		}
	}
	
	/**
	 * Saves an inventory record
	 * @param vo
	 * @param user 
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void saveInventoryRecord(LocationItemMasterVO vo, String roleId, UserVO user) throws InvalidDataException, DatabaseException {
		// CAS can't edit their inventory
		if (WSLARole.WSLA_SERVICE_CENTER.getRoleId().equals(roleId))
			return;

		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		
		LocationItemMasterVO original = new LocationItemMasterVO();
		
		if (!StringUtil.isEmpty(vo.getItemMasterId())) {
			original.setItemMasterId(vo.getItemMasterId());
			dbp.getByPrimaryKey(original);
			
			// Only Admins can directly edit quantity-on-hand. The UI prevents submission
			// of a changed value. However, if someone hacks the UI in order to submit
			// a different value anyway, we are changing it back to the original value here.
			if (!WSLARole.ADMIN.getRoleId().equals(roleId)) {
				vo.setQuantityOnHand(original.getQuantityOnHand());
			}
			
		}
		
		dbp.save(vo);
		
		//use the new and original vo to make a ledger entry
		saveInventoryLedger(vo, original, user);

	}
	
	/**
	 * this method can be used to write to the inventory ledger when needed to record 
	 * @param newLIMvo
	 * @param oriLIMvo
	 * @param user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveInventoryLedger(LocationItemMasterVO newLIMvo, LocationItemMasterVO oriLIMvo, UserVO user) throws InvalidDataException, DatabaseException {
		InventoryLedgerVO ilvo = new InventoryLedgerVO();
		ilvo.setOffsetNumber(newLIMvo.getQuantityOnHand()-oriLIMvo.getQuantityOnHand());
		ilvo.setUserId(user.getUserId());
		ilvo.setItemMasterId(newLIMvo.getItemMasterId());
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(ilvo);
		
	}

	/**
	 * Return a list of product inventory (counts) at the given provider_location
	 * @param locationId
	 * @param orderBy
	 * @param term (search keyword)
	 * @return
	 */
	public GridDataVO<LocationItemMasterVO> listInventory(String locationId, String orderBy, BSTableControlVO bst, boolean setOnly) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select lim.item_master_Id, lim.actual_qnty_no, lim.desired_qnty_no, pm.product_nm, ");
		sql.append("pm.product_id, pm.cust_product_id, pm.sec_cust_product_id, lcn.location_id, lcn.location_nm, p.provider_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_location_item_master lim ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master pm on lim.product_id=pm.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location lcn on lim.location_id=lcn.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider p on lcn.provider_id=p.provider_id ");
		sql.append("where 1=1 ");
		if (setOnly) sql.append("and set_flg = 1 ");
		
		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(p.provider_nm) like ? or lower(lcn.location_nm) like ? or lower(pm.product_nm) like ? or lower(pm.cust_product_id) like ? ) ");
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
		}
		//filter by inventory location
		if (!StringUtil.isEmpty(locationId)) {
			sql.append("and lim.location_id=? ");
			params.add(locationId);
		}

		if (StringUtil.isEmpty(orderBy))
			orderBy = "order by p.provider_nm, lcn.location_nm, pm.product_nm";

		sql.append(orderBy);
		log.debug(sql+ "|" + params);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new LocationItemMasterVO(), bst);
	}


	/**
	 * Generate a list of Provider Locations (<LocationId, LocationNm> pairs) bound to product inventory.
	 * @param productIdOrModelNo productId or custProductId (model#) to filter the data by - case insensitive.
	 * @param minInventory only used with productId - ensures the matched locationId has a minimum amount of inventory on hand.
	 * @return List<GenericVO> Used for data filter box (selectpicker)
	 */
	public List<LocationItemMasterVO> listInvetorySuppliers(String productIdOrModelNo, Integer minInventory, UserVO user) {
		String schema = getCustomSchema();
		List<Object> params = null;
		StringBuilder sql = new StringBuilder(250);
		sql.append("select * ").append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("wsla_provider_location lcn ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_location_item_master lim on lcn.location_id=lim.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_provider p on lcn.provider_id=p.provider_id ");
		if (!StringUtil.isEmpty(productIdOrModelNo)) {
			sql.append(DBUtil.INNER_JOIN).append(schema);
			sql.append("wsla_product_master prod on lim.product_id=prod.product_id ");
			sql.append("and (prod.product_id=? or lower(prod.cust_product_id)=?) ");
			params = new ArrayList<>(Arrays.asList(productIdOrModelNo, productIdOrModelNo));
			if (minInventory != null) {
				sql.append("where lim.actual_qnty_no >= ? ");
				params.add(minInventory);
			}
		} else if (user != null) {
			sql.append("where lcn.location_id = ? ");
			params = new ArrayList<>(Arrays.asList(user.getLocationId()));
		}
		sql.append("order by p.provider_nm, lcn.location_nm, lcn.store_no, lcn.location_id");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new LocationItemMasterVO(), "location_id");
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
		String updSql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_location_item_master ",
				"set actual_qnty_no=actual_qnty_no+?, update_dt=? ",
				"where product_id=? and location_id=?");
		log.debug(updSql);

		int cnt = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(updSql)) {
			ps.setInt(1, qnty);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, productId);
			ps.setString(4, locationId);
			cnt = ps.executeUpdate();
			log.debug(String.format("updated %d item_master records at %s", cnt, locationId));

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