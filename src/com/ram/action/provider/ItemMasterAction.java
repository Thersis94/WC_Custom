package com.ram.action.provider;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// RAM Data Feed Libs
import com.ram.workflow.data.vo.LocationItemMasterVO;
import com.ram.action.util.SecurityUtil;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: ItemMasterAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Manages the Item Master data for the ram analytics engine
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 3.0
 * @since August 8, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ItemMasterAction extends SimpleActionAdapter {

	/**
	 * Maps the Bootstrap column names to the db field names
	 */
	protected final Map<String, String> fieldMap = new LinkedHashMap<>();
	
	// Constants for code
	protected static final String LOCATION_ITEM_MASTER_ID = "LOCATION_ITEM_MASTER_ID";
	
	public ItemMasterAction() {
		super();
		initFieldMap();
	}

	/**
	 * @param actionInit
	 */
	public ItemMasterAction(ActionInitVO actionInit) {
		super(actionInit);
		initFieldMap();
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/**
	 * Maps the table column names to the actual field names
	 */
	private final void initFieldMap() {
		fieldMap.put("productNm", "p.product_nm");
		fieldMap.put("customerNm", "c.customer_nm");
		fieldMap.put("custProductId", "p.cust_product_id");
		fieldMap.put("serialNoTxt", "lim.serial_no_txt");
		fieldMap.put("qtyOnHand", "lim.qty_on_hand_no");
		fieldMap.put("parValueNo", "lim.par_value_no");
		fieldMap.put("safetyStockNo", "lim.safety_stock_no");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		LocationItemMasterVO limvo = new LocationItemMasterVO(req);
		String requestAction = StringUtil.checkVal(req.getParameter("requestAction"));
		
		if ("cloneKit".equals(requestAction)) {
			copy(req);
		} else if (!StringUtil.isEmpty(limvo.getLocationItemMasterId())) {
			updateLocationItemMaster(limvo);
		} else {
			addToLocationItemMaster(limvo);
		}
	}

	/**
	 * Updates an existing item in the location item master,
	 * with the only values that can be updated.
	 * 
	 * @param limvo
	 */
	protected void updateLocationItemMaster(LocationItemMasterVO limvo) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		limvo.setUpdateDt(Convert.getCurrentTimestamp());

		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append(schema).append("RAM_LOCATION_ITEM_MASTER ");
		sql.append("set par_value_no = ?, safety_stock_no = ?, serial_no_txt = ?, update_dt = ? ");
		sql.append("where ").append(LOCATION_ITEM_MASTER_ID).append(" = ? ");
		
		List<String> fields = new ArrayList<>();
		fields.addAll(Arrays.asList("PAR_VALUE_NO", "SAFETY_STOCK_NO", "SERIAL_NO_TXT", "UPDATE_DT", LOCATION_ITEM_MASTER_ID));
		
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), limvo, fields);
		} catch (Exception e) {
			log.error("Unable to update Location Item Master", e);
		}
	}
	
	/**
	 * Adds a new item to the location item master
	 * 
	 * @param limvo
	 */
	protected void addToLocationItemMaster(LocationItemMasterVO limvo) {
		DBProcessor dbp = new DBProcessor(dbConn);
		
		try {
			dbp.insert(limvo);
		} catch (Exception e) {
			log.error("Unable to add item to Location Item Master", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void copy(ActionRequest req) throws ActionException{
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sourceItemId = req.getParameter("locationItemMasterId");
		String newSerialNo = req.getParameter("serialNoTxt");
		
		// Set the replacement values
		Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
		if (replaceVals == null) {
			replaceVals = new HashMap<>();
			attributes.put(RecordDuplicatorUtility.REPLACE_VALS, replaceVals);
		}
		replaceVals.put("SERIAL_NO_TXT", newSerialNo);
		
		//Build our RecordDuplicatorUtility and set the where clause
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "RAM_LOCATION_ITEM_MASTER", LOCATION_ITEM_MASTER_ID, false);
		rdu.setReplaceVals(replaceVals);
		rdu.setSchemaNm(schema);
		rdu.addWhereClause("LOCATION_ITEM_MASTER_ID", sourceItemId);

		// Copy the data
		rdu.copy();
	}	

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		if (req.hasParameter("amid") && ! req.getBooleanParameter("detail"))
			this.retrieveAll(req);
		else if (req.hasParameter("amid") && req.getBooleanParameter("detail")) {
			putModuleData(getItemDetails(req.getParameter("locationItemMasterId")));
		}
			
	}
	
	/**
	 * 
	 * @param locationItemMasterId
	 * @return
	 */
	public List<GenericVO> getItemDetails(String locationItemMasterId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select lot_number_txt as key, expiration_dt as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_location_item_master_detail ");
		sql.append("where location_item_master_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(locationItemMasterId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), params, new GenericVO());
	}
	
	/**
	 * Retrieves all data for the Location Item Master table
	 *  
	 * @param req
	 * @throws ActionException
	 */
	public void retrieveAll(ActionRequest req) {
		DBProcessor dbp = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		
		// Build the results query
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select *, c.gtin_number_txt || cast(p.gtin_product_id as varchar(64)) as gtin_product_number_txt ").append(getBaseSQL());
		sql.append(getListWhere(req, params));
		sql.append(getListOrder(req, params));
		log.debug("Location Item Master SQL: " + sql.toString());
		
		// Build the count query
		List<Object> countParams = new ArrayList<>();
		StringBuilder countSql = new StringBuilder(1000);
		countSql.append("select count(*) as key ").append(getBaseSQL());
		countSql.append(getListWhere(req, countParams));
		
		// Get the results
		List<?> items = dbp.executeSelect(sql.toString(), params, new LocationItemMasterVO());
		List<?> count = dbp.executeSelect(countSql.toString(), countParams, new GenericVO());
		
		// Return the results
		this.putModuleData(items, Convert.formatInteger(((GenericVO) count.get(0)).getKey() + ""), false);
	}
	
	/**
	 * Builds the base SQL Statement for retrieving the list of products
	 * 
	 * @return
	 */
	protected StringBuilder getBaseSQL() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("RAM_LOCATION_ITEM_MASTER lim ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("RAM_PRODUCT p on lim.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("RAM_CUSTOMER c on p.CUSTOMER_ID = c.CUSTOMER_ID ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("RAM_CUSTOMER_LOCATION cl on lim.CUSTOMER_LOCATION_ID = cl.CUSTOMER_LOCATION_ID ");
		
		return sql;
	}
	
	/**
	 * Builds the where clause for the search table
	 * 
	 * @param req
	 * @param params
	 * @return
	 */
	public StringBuilder getListWhere(ActionRequest req, List<Object> params) {
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		StringBuilder where = new StringBuilder(400);
		
		// Apply the customer location
		where.append("where lim.CUSTOMER_LOCATION_ID = ? ");
		params.add(Convert.formatInteger(req.getParameter("customerLocationId")));
		
		// Filter by the active flag
		if (Convert.formatBoolean(req.getParameter("isKit"))) {
			where.append("and p.kit_flg = 1 ");
		}
		
		// If role is provider, filter to prevent returning a location that is not theirs.
		if(SecurityUtil.isProviderRole(r.getRoleId())) {
			where.append(SecurityUtil.addCustomerFilter(req, "cl"));
		}
		
		// If role is OEM, filter to prevent returning products that are not theirs.
		if(SecurityUtil.isOEMRole(r.getRoleId())) {
			where.append(SecurityUtil.addOEMFilter(req, "p"));
		}
		
		// Filter on search parameters
		String search = req.getParameter("search");
		if (!StringUtil.isEmpty(search)) {
			where.append("and (lower(product_nm) like ? or lower(cust_product_id) like ?) ");
			params.add("%" + search.toLowerCase() + "%");
			params.add("%" + search.toLowerCase() + "%");
		}
		
		return where;
	}
	
	/**
	 * Builds the order/limit for the search table
	 * 
	 * @return
	 */
	public StringBuilder getListOrder(ActionRequest req, List<Object> params) {
		StringBuilder order = new StringBuilder(200);
		
		order.append("order by ").append(fieldMap.get(StringUtil.checkVal(req.getParameter("sort"), "productNm")));
		order.append("desc".equalsIgnoreCase(req.getParameter("order")) ? " desc " : " asc ");
		order.append("limit ? offset ? ");

		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
		
		return order;
	}
}
