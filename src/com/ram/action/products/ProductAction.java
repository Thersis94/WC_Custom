/**
 * 
 */
package com.ram.action.products;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// WC Custom
import com.ram.action.data.RAMProductSearchVO;
import com.ram.action.util.SecurityUtil;
import com.ram.datafeed.data.ProductRecallItemVO;
import com.ram.datafeed.data.RAMProductCategoryVO;
// RAM Data Feed
import com.ram.datafeed.data.RAMProductVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs 3.3
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: ProductAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles all basic product related 
 * interactions.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since May 22, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ProductAction extends SBActionAdapter {

	public static final String CLONE_SUFFIX = "clone";
	
	// Constants for commonly used values
	private static final String PRODUCT_ID = "product_id";
	private static final String PRODUCTID = "productId";
	
	/**
	 * Default Constructor
	 */
	public ProductAction() {
		super();
		
	}

	/**
	 * General Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public ProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Copy method that clones a RAM Product.  If this is a kit then we forward
	 * the call to the KitLayerAction.  After the record is cloned, we update
	 * the name so we can determine what was changed before we commit the changes.
	 * If any errors are thrown then we rollback the changes.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void copy(ActionRequest req) throws ActionException {
		try {

			//Diable AutoCommit.
			dbConn.setAutoCommit(false);
			Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
			if(replaceVals == null) {
				replaceVals = new HashMap<>();
				attributes.put(RecordDuplicatorUtility.REPLACE_VALS, replaceVals);
			}

			//Clone RAM_PRODUCT Record.
			RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "ram_product", PRODUCT_ID, true);
			rdu.setSchemaNm((String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			rdu.addWhereClause(PRODUCT_ID, req.getParameter(PRODUCTID));
			Map<String, String> productIds = rdu.copy();
			replaceVals.put(PRODUCT_ID, productIds);

			//If this is a kit, then propagate through the KitAction Framework.
			if(Convert.formatBoolean(req.getParameter("isKit"))) {
				KitLayerAction kla = new KitLayerAction(getActionInit());
				kla.setDBConnection(dbConn);
				kla.setAttributes(getAttributes());
				kla.copy(req);
			}

			//Update the New Product Name.
			updateNames(productIds, CLONE_SUFFIX);

			//Commit the Db changes.
			dbConn.commit();
			dbConn.setAutoCommit(true);
		} catch(Exception e) {
			try {
				//Rollback Changes on Error.
				log.error("Rolling back database entries.", e);
				dbConn.rollback();
			} catch (SQLException e1) {
				throw new ActionException(e);
			}
		}
	}

	/**
	 * We can't delete anything in RAM due to sync issues so we mark items
	 * inactive.  This handles marking a given product as inactive.
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		
		//Build Query, we deactivate, not delete.
		StringBuilder sb = new StringBuilder(140);
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set ACTIVE_FLG = 0 where PRODUCT_ID = ?");

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build Statement and execute
		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, req.getParameter(PRODUCTID));
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deactivating Product: " + req.getParameter(PRODUCTID), sqle);
			throw new ActionException(sqle);
		}
	}

	/**
	 * If we have a productId then retrieve that products information, otherwise
	 * return the full list of products.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		if (req.hasParameter("product_recall")) {
			this.putModuleData(getProductRecalls(req));
			
		} else if (req.hasParameter(PRODUCTID))
			this.putModuleData(retrieveProduct(req));
		
		//Prevent double call at page load.  This ensures only the ajax call triggers load.
		else if(req.hasParameter("amid"))
			list(req);
	}
	
	/**
	 * Gets the gtin prefix number for the given customer id
	 * @param req
	 * @return
	 */
	public List<Object> getProductRecalls(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * ");
		sql.append(DBUtil.FROM_CLAUSE).append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("ram_product_recall_item ");
		sql.append(DBUtil.WHERE_CLAUSE).append("product_id = ? order by lot_number_txt ");
		log.debug(sql + "|" + req.getParameter(PRODUCTID));
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(req.getParameter(PRODUCTID)));
		return dbp.executeSelect(sql.toString(), params, new ProductRecallItemVO());
	}
	/**
	 * Retrieve all the products that match either the given CustomerId or ProductId
	 * on the request.
	 * @param req
	 */
	private RAMProductVO retrieveProduct(ActionRequest req) {			
		//Instantiate the products list for results and check for lookup type.
		RAMProductVO product = null;

		StringBuilder sb = new StringBuilder(150);
		sb.append("select * from ");
		sb.append("ram_product a inner join ");
		sb.append("ram_customer b on a.customer_id = b.customer_id ");
		sb.append(DBUtil.LEFT_OUTER_JOIN).append("ram_product_category_xr x on a.product_id = x.product_id ");
		sb.append(DBUtil.WHERE_1_CLAUSE).append("and a.product_id = ?");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(req.getParameter(PRODUCTID)));
		DBProcessor db = new DBProcessor(getDBConnection(), attributes.get(Constants.CUSTOM_DB_SCHEMA) + "");
		List<Object> data = db.executeSelect(sb.toString(), params, new RAMProductVO(), "product_id");
		
		// Get the product from the list
		if (data != null && ! data.isEmpty()) product = (RAMProductVO)data.get(0);
		
		//Return List to View
		return product;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		if (!SecurityUtil.isAdministratorRole(role.getRoleId())) return;
		
		if (req.hasParameter("product_recall")) {
			putModuleData(updateProductRecall(req), 1, false);			
		} else {
			saveProduct(req);
		}
	}
	
	/**
	 * Updates the active status of the recall
	 * @param req
	 */
	public ProductRecallItemVO updateProductRecall(ActionRequest req) throws ActionException{
		// Populate the VO
		ProductRecallItemVO item = new ProductRecallItemVO(req);
		item.setCreateDate(new Date());
		item.setUpdateDate(new Date());

		// Save the data
		DBProcessor db = new DBProcessor(getDBConnection(), (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			db.save(item);
			
			// Update the recall id with the generated id (new products only)
			String id = db.getGeneratedPKId();
			if (! StringUtil.isEmpty(id)) item.setRecallItemId(Convert.formatInteger(id));
		} catch(Exception e) {
			throw new ActionException("Unable to update product recall information", e);
		}
		
		return item;
	}
	
	/**
	 * Make the decision to either insert a new product or update
	 * and existing product based on presence of a productId on the
	 * request
	 * @param req
	 */
	public void saveProduct(ActionRequest req) {
		// Populate the data
		RAMProductVO product = new RAMProductVO(req);
		product.setCreateDate(new Date());
		if (product.getProductId() == 0) product.setProductId(null);
		
		// Save the product info
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			db.save(product);
			
			// Update the product id with the generated id (new products only)
			String id = db.getGeneratedPKId();
			if (! StringUtil.isEmpty(id)) product.setProductId(Convert.formatInteger(id));
			
			// Save the categories
			this.saveCategories(req.getParameterValues("categoryCode"), product);
			
			// Return the product info 
			this.putModuleData(product, 1, false, "Product Successfully Saved", false);
		} catch(Exception e) {
			log.error("Unable to save product", e);
			
			// Set the error onto the message 
			this.putModuleData(product, 1, false, "Unable to save product", true);
		}

	}
	
	/**
	 * Deletes the existing category mapping and adds the newly selected categories
	 * @param cats
	 * @param product
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveCategories(String[] cats, RAMProductVO product) throws InvalidDataException, DatabaseException {
		String sql = "delete from ram_product_category_xr where product_id = ? ";
		
		DBProcessor db = new DBProcessor(getDBConnection(), (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<String> fields = new ArrayList<>();
		fields.add("product_id");
		db.executeSqlUpdate(sql, product, fields);
		
		for(String val : cats) {
			RAMProductCategoryVO vo = new RAMProductCategoryVO();
			vo.setProductId(product.getProductId());
			vo.setCategoryCode(val);
			db.insert(vo);
		}
	}
	
	
	/**
	 * Retrieve an unfiltered list of products.
	 * 
	 * update - returns filtered list for providers and oems.
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		//Pull relevant data off the request
		RAMProductSearchVO svo = new RAMProductSearchVO(req);
		List<Object> params = buildProductLookupParams(svo);
		String sql = getProdList(svo);
		
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> products = dbp.executeSelect(sql, params, new RAMProductVO());

		/*
		 * Retrieve the total count of products to properly show pagination.
		 * Need to increment by one as this is a 0 start number and the list
		 * starts at 1.
		 */
		svo.setCount(true);
		
		//Return the data.
		putModuleData(products, getRecordCount(svo), false);
	}
	
	/**
	 * Gets the count of the records
	 * @param customerId
	 * @param term
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount(RAMProductSearchVO svo) {
		log.debug("Retrieving Total Counts");
		int cnt = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(getProdList(svo))) {
			int index = 1;
			if (svo.getActiveFlag() != null) ps.setInt(index++, svo.getActiveFlag());
			if (svo.getCustomerId() > 0) ps.setInt(index++, svo.getCustomerId());
			if(!StringUtil.isEmpty(svo.getTerm())) {
				ps.setString(index++, "%" + svo.getTerm() + "%");
				ps.setString(index++, "%" + svo.getTerm() + "%");
			}
			if(svo.getProviderId() > 0) {
				ps.setInt(index++, svo.getProviderId());
			}
			
			//Get the count off the first row.
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				cnt = rs.getInt(1);
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return cnt;
	}

	/**
	 * Helper method builds Product Lookup Param List for Product Query.
	 * @param svo
	 * @return
	 */
	public static List<Object> buildProductLookupParams(RAMProductSearchVO svo) {
		List<Object> params = new ArrayList<>();
		if (svo.getActiveFlag() != null) params.add(svo.getActiveFlag());
		if (svo.getCustomerId() > 0) params.add(svo.getCustomerId());
		if(!StringUtil.isEmpty(svo.getTerm())) {
			params.add("%" + svo.getTerm() + "%");
			params.add("%" + svo.getTerm() + "%");
		}
		
		params.add(svo.getStart());
		params.add(svo.getLimit());
		return params;
	}

	/**
	 * Build the where clause for the product Retrieval
	 * 
	 * Update Billy Larsen - Added additional clause to filter for providers allowing them
	 * to see just products at their locations.
	 * @param customerId
	 * @return
	 */
	protected StringBuilder getWhereClause(RAMProductSearchVO svo) {
		StringBuilder sb = new StringBuilder(700);
		sb.append(DBUtil.WHERE_1_CLAUSE);
		
		//Add clauses depending on what is passed in.
		if (svo.getActiveFlag() != null) sb.append("and a.active_flg = ? ");
		else sb.append("and a.active_flg = 1 ");
		
		if (svo.getCustomerId() > 0) sb.append("and a.customer_id = ? ");
		if(svo.getAdvFilter().contains("5")) sb.append("and kit_flg = 1 ");
		if(svo.getAdvFilter().contains("10")) sb.append("and (kit_flg = 0 or kit_flg is null) ");
		if(svo.getAdvFilter().contains("15")) sb.append("and manual_entry_flg = 1 ");
		if(svo.getAdvFilter().contains("20")) sb.append("and (manual_entry_flg = 0 or manual_entry_flg is null) ");
		if(svo.getTerm().length() > 0) sb.append("and (lower(product_nm) like ? or lower(cust_product_id) like ?) ");
		
		return sb;
	}
	
	/**
	 * Generates the queries for retrieving and counting products based on a number of input.
	 *
	 * Fixed query and database to use a View on the Customer Table to avoid the circular reference
	 * that was causing problems with the Product Lookup query.  Now performance is much better as we
	 * don't have to perform any intersects on the data.
	 *
	 * Cleaned up the select query to perform the pagination in query.  Reduces
	 * data returned and eliminates app server side looping to records we actually
	 * care about.
	 * @param customerId
	 * @param term
	 * @param kitFilter
	 * @param providerId
	 * @param isCount
	 * @param limit
	 * @return
	 */
	public String getProdList(RAMProductSearchVO svo) {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder(1300);

		if(svo.isCount()) {
			sb.append("select count(a.product_id) from ").append(schema);
		} else {
			sb.append("select  a.product_id, a.customer_id, a.cust_product_id, a.product_nm, a.desc_txt, a.short_desc, ");
			sb.append("b.gtin_number_txt || cast(a.gtin_product_id as varchar(64)) as gtin_number_txt, ");
			sb.append("a.lot_code_flg, a.active_flg, a.expiree_req_flg, a.gtin_product_id, b.customer_nm, a.kit_flg, a.manual_entry_flg from ").append(schema);
		}
		//Build Initial Query

		sb.append("ram_product a ");
		sb.append("inner join ").append(schema).append("RAM_OEM_CUSTOMER b ");
		sb.append("on a.customer_id = b.customer_id ");
		sb.append(this.getWhereClause(svo));

		//Lastly if this is not a count call order the results.
		if(!svo.isCount())
			sb.append(" order by product_nm offset ?  limit ? ");

		log.debug(svo.getCustomerId() + "|" + svo.getProviderId() + "|" + sb.toString());
		return sb.toString();
	}
	
	/**
	 * Helper method that updates the Product Name when it's copied.
	 * @param ids
	 * @param prefix
	 */
	private void updateNames(Map<String, String> ids, String suffix) {
		StringBuilder sb = new StringBuilder(150);
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set PRODUCT_NM = PRODUCT_NM + ' - ' + ? where PRODUCT_ID = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			for(String id : ids.values()) {
				ps.setString(1, suffix);
				ps.setString(2, id);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.debug(e);
		}
	}
}
