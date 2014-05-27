/**
 * 
 */
package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ram.datafeed.data.KitLayerVO;
import com.ram.datafeed.data.ProductRecallItemVO;
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
		}else if(req.hasParameter("productId"))
			retrieveProducts(req);
	}

	/**
	 * Retrieve all the products that match either the given CustomerId or ProductId
	 * on the request.
	 * @param req
	 */
	private void retrieveProducts(SMTServletRequest req) {			
		
		//Instantiate the products list for results and check for lookup type.
		JsonObject obj = new JsonObject();
		JsonArray products = new JsonArray();
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
				products.add(getJson(new RAMProductVO(rs)));
		} catch(SQLException sqle) {
			log.error("Error retrieving product list", sqle);
		}
		
		obj.add("products", products);
		//Return List to View
		this.putModuleData(obj);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
		//Build Update Query.  We don't insert Products
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("set CUST_PRODUCT_ID = ?, PARENT_ID = ?, CUSTOMER_ID = ?, ");
		sb.append("PRODUCT_NM = ?, DESC_TXT = ?, SHORT_DESC = ?, ACTIVE_FLG = ?, ");
		sb.append("LOT_CODE_FLG = ?, KIT_FLG = ?, UPDATE_DT = ?, ");
		sb.append("EXPIREE_REQ_FLG = ? where PRODUCT_ID = ? ");
		
		//Build PreparedStatement and set Parameters
		PreparedStatement ps = null;
		int i = 1;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, req.getParameter("customerProductId"));
			ps.setString(i++, req.getParameter("parentId"));
			ps.setString(i++, req.getParameter("customerId"));
			ps.setString(i++, req.getParameter("productNm"));
			ps.setString(i++, req.getParameter("descText"));
			ps.setString(i++, req.getParameter("shortDesc"));
			ps.setString(i++, req.getParameter("activeFlg"));
			ps.setString(i++, req.getParameter("lotCodeFlg"));
			ps.setString(i++, req.getParameter("kitFlg"));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, req.getParameter("expireeReqFlg"));
			ps.setString(i++, req.getParameter("productId"));
			
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + req.getParameter("productId"), sqle);
		}
		
		//Redirect User
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT where CUSTOMER_ID = ? and (PRODUCT_NM like ? ");
		sb.append("or CUST_PRODUCT_ID like ?)");
		
		//Build PreparedStatement
		log.debug("Retrieving Products: " + sb.toString() + " where term is " + req.getParameter("term"));
		PreparedStatement ps = null;
		
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter(KitAction.CUSTOMER_ID));
			ps.setString(2, req.getParameter("term") + "%");
			ps.setString(3, req.getParameter("term") + "%");

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

	/**
	 * Build JSON Representation of the Object
	 * @return
	 */

	public JsonObject getJson(RAMProductVO prod) {
		JsonObject json = new JsonObject();
		json.addProperty("productId", prod.getProductId());
		json.addProperty("parentId", prod.getParentId());
		json.addProperty("customerProductId", prod.getCustomerProductId());
		json.addProperty("gtinProductId", prod.getGtinProductId());
		json.addProperty("productName", prod.getProductName());
		json.addProperty("shortDesc", prod.getShortDesc());
		json.addProperty("customerId", prod.getCustomerId());
		json.addProperty("customerName", prod.getCustomerName());
		json.addProperty("lotNumber", prod.getLotNumber());
		json.addProperty("lotCodeRequired", prod.getLotCodeRequired());
		json.addProperty("expireeRequired", prod.getExpireeRequired());
		json.addProperty("parLevel", prod.getParLevel());
		json.addProperty("kitFlag", prod.getKitFlag());
		json.addProperty("quantity", prod.getQuantity());
		json.addProperty("msrpCostNo", prod.getMsrpCostNo());
		
//		
//		//Add the Recall Items to the Object as an Array
//		JsonArray recalls = new JsonArray();
//		for(ProductRecallItemVO recall : recallLotNumbers)
//			recalls.add(recall.getJson());
//		json.add("recallLotNumbers", recalls);
//		
//		//Add the kit Items to the Object as an Array
//		JsonArray kits = new JsonArray();
//		for(KitLayerVO kit : kitLayers)
//			kits.add(kit.getJson());
//		json.add("kitLayers", kits);
//		
		return json;
	}
}
