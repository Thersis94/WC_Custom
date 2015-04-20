/**
 * 
 */
package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.user.RamUserAction;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
	public void copy(SMTServletRequest req) throws ActionException {
		try {

			//Diable AutoCommit.
			dbConn.setAutoCommit(false);
			Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
			if(replaceVals == null) {
				replaceVals = new HashMap<String, Object>();
				attributes.put(RecordDuplicatorUtility.REPLACE_VALS, replaceVals);
			}

			//Clone RAM_PRODUCT Record.
			RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "RAM_PRODUCT", "PRODUCT_ID", true);
			rdu.setSchemaNm((String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			rdu.addWhereClause("PRODUCT_ID", req.getParameter("productId"));
			Map<String, String> productIds = rdu.copy();
			replaceVals.put("PRODUCT_ID", productIds);

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
	public void delete(SMTServletRequest req) throws ActionException {
		
		//Build Query, we deactivate, not delete.
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set ACTIVE_FLG = 0 where PRODUCT_ID = ?");

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build Statement and execute
		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("productId"));
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deactivating Product: " + req.getParameter("productId"), sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
	}

	/**
	 * If we have a productId then retrieve that products information, otherwise
	 * return the full list of products.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		if(req.hasParameter("productId"))
			retrieveProducts(req);
		//Prevent double call at page load.  This ensures only the ajax call triggers load.
		else if(req.hasParameter("amid"))
			list(req);
	}

	/**
	 * Retrieve all the products that match either the given CustomerId or ProductId
	 * on the request.
	 * @param req
	 */
	private void retrieveProducts(SMTServletRequest req) throws ActionException {			
		
		//Instantiate the products list for results and check for lookup type.
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		boolean isProductLookup = req.hasParameter("productId");

		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT where ");
		if(isProductLookup)
			sb.append("PRODUCT_ID = ?");
		else {
			sb.append("CUSTOMER_ID = ?");
		}
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build the Statement and execute
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(isProductLookup)
				ps.setString(1, req.getParameter("productId"));
			else
				ps.setString(1, req.getParameter("customerId"));
			
			//Loop the results and add to products list.
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				products.add(new RAMProductVO(rs));
		} catch(SQLException sqle) {
			log.error("Error retrieving product list", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Return List to View
		this.putModuleData(products);
	}

	/**
	 * Make the decision to either insert a new product or update
	 * and existing product based on presense of a productId on the
	 * request.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(r.getRoleLevel() == RamUserAction.ROLE_LEVEL_PROVIDER) {
			result.put("success", "false");
			result.put("msg", "User has invalid permissions for this action.");
			return;
		}
		
		//Build Update Query.
		String query = null;
		boolean isInsert = !req.hasParameter("productId");
		
		if(isInsert) {
			query = getProdInsert();
		} else {
			query = getProdUpdate();
		}
		
		//Log sql Statement for verification
		log.debug("sql: " + query);
		
		
		//Build PreparedStatement and set Parameters
		PreparedStatement ps = null;
		int i = 1;
		log.debug(req.getParameter("kitFlag"));
		log.debug(req.getParameter("lotCodeRequired"));
		try {
			ps = dbConn.prepareStatement(query);
			ps.setString(i++, req.getParameter("productNm"));
			ps.setString(i++, req.getParameter("shortDesc"));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("activeFlag"))));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("lotCodeRequired"))));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("kitFlag"))));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("expireeRequired"))));
			ps.setString(i++, req.getParameter("customerProductId"));
			ps.setInt(i++, 1);
			if(req.hasParameter("productId")) {
				ps.setString(i++, req.getParameter("productId"));
			} else {
				ps.setString(i++, req.getParameter("customerId"));
				ps.setString(i++, req.getParameter("gtinProductId"));
			}
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + req.getParameter("productId"), sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Redirect User
		super.putModuleData(result);
	}

	/**
	 * Retrieve an unfiltered list of products.
	 * 
	 * update - returns filtered list for providers and oems.
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		
		//Instantiate necessary items
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		
		//Check for providerId, providers are only allowed to see products at their locations.
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		int providerId = r.getRoleLevel() == RamUserAction.ROLE_LEVEL_PROVIDER ? Convert.formatInteger((String) r.getAttribute("roleAttributeKey_1")) : 0;

		//Check for oem, oem are only allowed to see their products.
		int customerId = r.getRoleLevel() == RamUserAction.ROLE_LEVEL_OEM ? Convert.formatInteger((String) r.getAttribute("roleAttributeKey_1")) : Convert.formatInteger(req.getParameter("customerId"));

		//Pull relevant data off the request
		int advFilter = Convert.formatInteger(req.getParameter("advFilter"), -1);
		String term = StringUtil.checkVal(req.getParameter("term"));
		int start = Convert.formatInteger(req.getParameter("start"), 0);
		int limit = Convert.formatInteger(req.getParameter("limit"), 25) + start;
		
		log.debug("Retrieving Products");
		PreparedStatement ps = null;
		int index = 1, ctr = 0;
		try{
			ps = dbConn.prepareStatement(getProdList(customerId, term, advFilter, providerId, false, limit));
			if (customerId > 0) ps.setInt(index++, customerId);
			if (advFilter > -1 && advFilter < 2) ps.setInt(index++, advFilter);
			else if (advFilter > 1) ps.setInt(index++, (advFilter == 2 ? 1 : 0));
			if(term.length() > 0) {
				ps.setString(index++, "%" + term + "%");
				ps.setString(index++, "%" + term + "%");
			}
			/*
			 * Providers use an intersect to get the correct products
			 * so we need to set the same attributes again.
			 */
			if(providerId > 0) {
				if (advFilter > -1 && advFilter < 2) ps.setInt(index++, advFilter);
				else if (advFilter > 1) ps.setInt(index++, (advFilter == 2 ? 1 : 0));
				if(term.length() > 0) {
					ps.setString(index++, "%" + term + "%");
					ps.setString(index++, "%" + term + "%");
				}
				ps.setInt(index++, providerId);
			}
			ps.setInt(index++, start);
			ps.setInt(index++, limit);

			/*
			 * Iterate over results to get our paginated selection
			 */
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				products.add(new RAMProductVO(rs));
			
			/*
			 * Retrieve the total count of products to properly show pagination.
			 * Need to increment by one as this is a 0 start number and the list
			 * starts at 1.
			 */
			ctr = 1 + getRecordCount(customerId, term, advFilter, providerId);
		} catch(SQLException sqle) {
			log.error("Error retrieving product list", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}

		//Return the data.
		putModuleData(products, ctr, false);		
	}
	
	/**
	 * Gets the count of the records
	 * @param customerId
	 * @param term
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount(int customerId, String term, int advFilter, int providerId) 
	throws SQLException {
		log.debug("Retrieving Total Counts");
		PreparedStatement ps = dbConn.prepareStatement(getProdList(customerId, term, advFilter, providerId, true, 0));
		int index = 1;
		if (customerId > 0) ps.setInt(index++, customerId);
		if (advFilter > -1 && advFilter < 2) ps.setInt(index++, advFilter);
		else if (advFilter > 1) ps.setInt(index++, (advFilter == 2 ? 1 : 0));
		if(term.length() > 0) {
			ps.setString(index++, "%" + term + "%");
			ps.setString(index++, "%" + term + "%");
		}
		if(providerId > 0) {
			if (advFilter > -1 && advFilter < 2) ps.setInt(index++, advFilter);
			else if (advFilter > 1) ps.setInt(index++, (advFilter == 2 ? 1 : 0));
			if(term.length() > 0) {
				ps.setString(index++, "%" + term + "%");
				ps.setString(index++, "%" + term + "%");
			}
			ps.setInt(index++, providerId);
		}
		int cnt = 0;
		//Get the count off the first row.
		ResultSet rs = ps.executeQuery();
		if(rs.next())
			cnt = rs.getInt(1);

		return cnt;
	}
	
	/**
	 * Build the where clause for the product Retrieval
	 * 
	 * Update Billy Larsen - Added additional clause to filter for providers allowing them
	 * to see just products at their locations.
	 * @param customerId
	 * @return
	 */
	protected StringBuilder getWhereClause(int customerId, String term, int advFilter, int providerId) {
		StringBuilder sb = new StringBuilder();
		String schema = attributes.get(Constants.CUSTOM_DB_SCHEMA) + "";

		sb.append("where 1=1 ");
		
		//Add clauses depending on what is passed in.
		if (customerId > 0) sb.append("and a.customer_id = ? ");
		if(advFilter > -1 && advFilter < 2) sb.append("and kit_flg = ? ");
		if(advFilter > 1) sb.append("and manual_entry_flg = ? ");
		if(term.length() > 0) sb.append("and (product_nm like ? or cust_product_id like ?) ");
		
		//Providers filter by inventoryItems that are related to their customerId
		if(providerId > 0) {
			sb.append("and a.product_id in (select c.product_id from ");
			sb.append(schema).append("ram_inventory_item c ");
			sb.append("inner join ").append(schema).append("ram_inventory_event_auditor_xr d on c.inventory_event_auditor_xr_id = d.inventory_event_auditor_xr_id ");
			sb.append("inner join ").append(schema).append("ram_inventory_event e on e.inventory_event_id = d.inventory_event_id ");
			sb.append("inner join ").append(schema).append("ram_customer_location f on e.customer_location_id = f.customer_location_id and f.customer_id = ?) ");
		}
		
		return sb;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
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
	public String getProdList(int customerId, String term, int advFilter, int providerId, boolean isCount, int limit) {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();

		if(isCount) {
			sb.append("select count(a.product_id) from ").append(schema);
		} else {
			sb.append("select * from (select ROW_NUMBER() OVER (order by Product_nm) as RowNum, a.PRODUCT_ID, a.CUSTOMER_ID, a.CUST_PRODUCT_ID, a.PRODUCT_NM, a.DESC_TXT, a.SHORT_DESC, a.LOT_CODE_FLG, a.ACTIVE_FLG, a.EXPIREE_REQ_FLG, a.GTIN_PRODUCT_ID, b.CUSTOMER_NM, a.KIT_FLG, a.MANUAL_ENTRY_FLG from ").append(schema);
		}
		//Build Initial Query

		sb.append("ram_product a ");
		sb.append("inner join ").append(schema).append("RAM_OEM_CUSTOMER b ");
		sb.append("on a.customer_id = b.customer_id ");
		sb.append(this.getWhereClause(customerId, term, advFilter, providerId));

		//Lastly if this is not a count call order the results.
		if(!isCount)
			sb.append(") as paginatedResult where RowNum >= ? and RowNum < ? order by RowNum");

		log.debug(customerId + "|" + providerId + "|" + sb.toString());
		return sb.toString();
	}

	/**
	 * Method for retrieving the insert clause for a product
	 * @return
	 */
	public String getProdInsert() {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT (PRODUCT_NM, SHORT_DESC, ACTIVE_FLG, LOT_CODE_FLG, ");
		sb.append("KIT_FLG, CREATE_DT, EXPIREE_REQ_FLG, CUST_PRODUCT_ID, ");
		sb.append("MANUAL_ENTRY_FLG, CUSTOMER_ID, GTIN_PRODUCT_ID) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		return sb.toString();
	}

	/**
	 * Method for retrieving the update clause for a product
	 * @return
	 */
	public String getProdUpdate() {
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set PRODUCT_NM = ?, SHORT_DESC = ?, ACTIVE_FLG = ?, ");
		sb.append("LOT_CODE_FLG = ?, KIT_FLG = ?, UPDATE_DT = ?, ");
		sb.append("EXPIREE_REQ_FLG = ?, CUST_PRODUCT_ID = ?, MANUAL_ENTRY_FLG = ? where PRODUCT_ID = ? ");
		return sb.toString();
	}

	/**
	 * Helper method that updates the Product Name when it's copied.
	 * @param ids
	 * @param prefix
	 */
	private void updateNames(Map<String, String> ids, String suffix) {
		StringBuilder sb = new StringBuilder(90);
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
