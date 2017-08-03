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

import com.ram.action.data.RAMProductSearchVO;
import com.ram.action.util.SecurityUtil;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
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
	public void delete(ActionRequest req) throws ActionException {
		
		//Build Query, we deactivate, not delete.
		StringBuilder sb = new StringBuilder(140);
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set ACTIVE_FLG = 0 where PRODUCT_ID = ?");

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build Statement and execute
		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, req.getParameter("productId"));
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deactivating Product: " + req.getParameter("productId"), sqle);
			throw new ActionException(sqle);
		}
	}

	/**
	 * If we have a productId then retrieve that products information, otherwise
	 * return the full list of products.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
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
	private void retrieveProducts(ActionRequest req) throws ActionException {			
		
		//Instantiate the products list for results and check for lookup type.
		List<RAMProductVO> products = new ArrayList<>();
		boolean isProductLookup = req.hasParameter("productId");

		StringBuilder sb = new StringBuilder(150);
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
		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			if(isProductLookup)
				ps.setInt(1, Convert.formatInteger(req.getParameter("productId")));
			else
				ps.setInt(1, Convert.formatInteger(req.getParameter("customerId")));
			
			//Loop the results and add to products list.
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				products.add(new RAMProductVO(rs));
		} catch(SQLException sqle) {
			log.error("Error retrieving product list", sqle);
			throw new ActionException(sqle);
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
	public void build(ActionRequest req) throws ActionException {
		Map<String, String> result = new HashMap<>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(r.getRoleLevel() == SecurityUtil.RAMRoles.PROVIDER.getLevel()) {
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
		int i = 1;
		try(PreparedStatement ps = dbConn.prepareStatement(query)) {
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
				ps.setInt(i++, Convert.formatInteger(req.getParameter("productId")));
			} else {
				ps.setInt(i++, Convert.formatInteger(req.getParameter("customerId")));
				ps.setString(i++, req.getParameter("gtinProductId"));
			}
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + req.getParameter("productId"), sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
			throw new ActionException(sqle);
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
	public void list(ActionRequest req) throws ActionException {
		
		//Instantiate necessary items
		List<RAMProductVO> products = new ArrayList<>();
		
		//Pull relevant data off the request
		RAMProductSearchVO svo = new RAMProductSearchVO(req);
		int index = 1;
		int ctr = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(getProdList(svo))){

			if (svo.getCustomerId() > 0) ps.setInt(index++, svo.getCustomerId());
			if (svo.getAdvFilter() > -1 && svo.getAdvFilter() < 2) ps.setInt(index++, svo.getAdvFilter());
			else if (svo.getAdvFilter() > 1) ps.setInt(index++, (svo.getAdvFilter() == 2 ? 1 : 0));
			if(!StringUtil.isEmpty(svo.getTerm())) {
				ps.setString(index++, "%" + svo.getTerm() + "%");
				ps.setString(index++, "%" + svo.getTerm() + "%");
			}

			/*
			 * Providers use an intersect to get the correct products
			 * so we need to set the same attributes again.
			 */
			if(svo.getProviderId() > 0) {
				ps.setInt(index++, svo.getProviderId());
			}
			ps.setInt(index++, svo.getStart());
			ps.setInt(index++, svo.getLimit());
			
			/*
			 * Iterate over results to get our paginated selection
			 */
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				products.add(new RAMProductVO(rs));
			}

			/*
			 * Retrieve the total count of products to properly show pagination.
			 * Need to increment by one as this is a 0 start number and the list
			 * starts at 1.
			 */
			svo.setCount(true);
			ctr = getRecordCount(svo);
		} catch(Exception sqle) {
			log.error("Error retrieving product list", sqle);
			throw new ActionException(sqle);
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
	protected int getRecordCount(RAMProductSearchVO svo) throws SQLException {
		log.debug("Retrieving Total Counts");
		int cnt = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(getProdList(svo))) {
			int index = 1;
			if (svo.getCustomerId() > 0) ps.setInt(index++, svo.getCustomerId());
			if (svo.getAdvFilter() > -1 && svo.getAdvFilter() < 2) ps.setInt(index++, svo.getAdvFilter());
			else if (svo.getAdvFilter() > 1) ps.setInt(index++, (svo.getAdvFilter() == 2 ? 1 : 0));
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
		}

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
	protected StringBuilder getWhereClause(RAMProductSearchVO svo) {
		StringBuilder sb = new StringBuilder(700);
		String schema = attributes.get(Constants.CUSTOM_DB_SCHEMA) + "";

		sb.append("where 1=1 ");
		
		//Add clauses depending on what is passed in.
		if(svo.isActiveOnly()) sb.append("and a.ACTIVE_FLG = 1 ");
		if (svo.getCustomerId() > 0) sb.append("and a.customer_id = ? ");
		if(svo.getAdvFilter() > -1 && svo.getAdvFilter() < 2) sb.append("and kit_flg = ? ");
		if(svo.getAdvFilter() > 1) sb.append("and manual_entry_flg = ? ");
		if(svo.getTerm().length() > 0) sb.append("and (product_nm like ? or cust_product_id like ?) ");
		
		//Providers filter by inventoryItems that are related to their customerId
		if(svo.getProviderId() > 0) {
			sb.append("and a.product_id in (select i.product_id from ");
			sb.append(schema).append("ram_location_item_master i ");
			sb.append("inner join ").append(schema).append("ram_customer_location f on i.customer_location_id = f.customer_location_id and f.customer_id = ?) ");
		}
		
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
			sb.append("select  a.PRODUCT_ID, a.CUSTOMER_ID, a.CUST_PRODUCT_ID, a.PRODUCT_NM, a.DESC_TXT, a.SHORT_DESC, a.LOT_CODE_FLG, a.ACTIVE_FLG, a.EXPIREE_REQ_FLG, a.GTIN_PRODUCT_ID, b.CUSTOMER_NM, a.KIT_FLG, a.MANUAL_ENTRY_FLG from ").append(schema);
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
	 * Method for retrieving the insert clause for a product
	 * @return
	 */
	public String getProdInsert() {
		StringBuilder sb = new StringBuilder(300);
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
		StringBuilder sb = new StringBuilder(300);
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
