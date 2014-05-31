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

import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

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

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
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
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		if(req.hasParameter("term")) {
			list(req);
		} else if(req.hasParameter("productId"))
			retrieveProducts(req);
	}

	/**
	 * Retrieve all the products that match either the given CustomerId or ProductId
	 * on the request.
	 * @param req
	 */
	private void retrieveProducts(SMTServletRequest req) {			
		
		//Instantiate the products list for results and check for lookup type.
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		boolean isProductLookup = req.hasParameter("productId");
		
		//Build Query and specialize for individual or list lookup
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
		}
		
		//Return List to View
		this.putModuleData(products);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
		//Build Update Query.  We don't insert Products
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set PRODUCT_NM = ?, SHORT_DESC = ?, ACTIVE_FLG = ?, ");
		sb.append("LOT_CODE_FLG = ?, KIT_FLG = ?, UPDATE_DT = ?, ");
		sb.append("EXPIREE_REQ_FLG = ?, CUST_PRODUCT_ID = ? where PRODUCT_ID = ? ");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		
		//Build PreparedStatement and set Parameters
		PreparedStatement ps = null;
		int i = 1;
		log.debug(req.getParameter("kitFlag"));
		log.debug(req.getParameter("lotCodeRequired"));
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, req.getParameter("productNm"));
			ps.setString(i++, req.getParameter("shortDesc"));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("activeFlag"))));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("lotCodeRequired"))));
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("kitFlag"))));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setInt(i++, Convert.formatInteger(Convert.formatBoolean(req.getParameter("expireeRequired"))));
			ps.setString(i++, req.getParameter("customerProductId"));
			ps.setString(i++, req.getParameter("productId"));
			
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + req.getParameter("productId"), sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
		}
		
		//Redirect User
		super.putModuleData(result);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		String schema = attributes.get(Constants.CUSTOM_DB_SCHEMA) + "";
		int customerId = Convert.formatInteger(req.getParameter(KitAction.CUSTOMER_ID));
		
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select top 25 * from ").append(schema).append("RAM_PRODUCT ");
		sb.append("where 1=1 ");
		if (customerId > 0) sb.append("and CUSTOMER_ID = ? ");
		sb.append("and (PRODUCT_NM like ? or CUST_PRODUCT_ID like ?)");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build PreparedStatement
		log.debug("Retrieving Products: " + sb.toString() + " where term is " + req.getParameter("term"));
		PreparedStatement ps = null;
		int index = 1;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if (customerId > 0) ps.setInt(index++, customerId);
			ps.setString(index++, "%" + req.getParameter("term") + "%");
			ps.setString(index++, "%" + req.getParameter("term") + "%");

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				products.add(new RAMProductVO(rs));
			}
			} catch(SQLException sqle) {
				log.error("Error retrieving product list", sqle);
			}
			
		//obj.add("products", products);
			
		//Return the data.
		putModuleData(products);		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
//
//	/**
//	 * Build JSON Representation of the Object
//	 * @return
//	 */
//
//	public JsonObject getJson(RAMProductVO prod) {
//		JsonObject json = new JsonObject();
//		json.addProperty("productId", prod.getProductId());
//		json.addProperty("parentId", prod.getParentId());
//		json.addProperty("customerProductId", prod.getCustomerProductId());
//		json.addProperty("gtinProductId", prod.getGtinProductId());
//		json.addProperty("productName", prod.getProductName());
//		json.addProperty("shortDesc", prod.getShortDesc());
//		json.addProperty("customerId", prod.getCustomerId());
//		json.addProperty("customerName", prod.getCustomerName());
//		json.addProperty("lotNumber", prod.getLotNumber());
//		json.addProperty("lotCodeRequired", prod.getLotCodeRequired());
//		json.addProperty("expireeRequired", prod.getExpireeRequired());
//		json.addProperty("parLevel", prod.getParLevel());
//		json.addProperty("kitFlag", prod.getKitFlag());
//		json.addProperty("quantity", prod.getQuantity());
//		json.addProperty("msrpCostNo", prod.getMsrpCostNo());
//		json.addProperty("activeFlag", prod.getActiveFlag());
//	
//		return json;
//	}
}
