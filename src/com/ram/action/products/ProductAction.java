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
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
		else
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
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Return List to View
		this.putModuleData(products);
	}

	/**
	 * We can only update products.  Inserts will come from data import or datafeed.
	 * Here we update a product record in the database.
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
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Redirect User
		super.putModuleData(result);
	}

	/**
	 * Retrieve an unfiltered list of products.
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		
		//Instantiate necessary items
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		String schema = attributes.get(Constants.CUSTOM_DB_SCHEMA) + "";
		
		//Pull relevant data off the request
		int customerId = Convert.formatInteger(req.getParameter("customerId"));
		int kitFilter = Convert.formatInteger(req.getParameter("kitFilter"), -1);
		String term = StringUtil.checkVal(req.getParameter("term"));
		int start = Convert.formatInteger(req.getParameter("start"), 0);
		int limit = Convert.formatInteger(req.getParameter("limit"), 25) + start;
		
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select top ").append(limit).append(" * from ").append(schema);
		sb.append("ram_product a ");
		sb.append("inner join ").append(schema).append("ram_customer b ");
		sb.append("on a.customer_id = b.customer_id ");
		sb.append(this.getWhereClause(customerId, term, kitFilter));
		sb.append("order by product_nm ");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build PreparedStatement
		log.debug("Retrieving Products: " + sb.toString() + " where term is " + req.getParameter("term"));
		PreparedStatement ps = null;
		int index = 1, ctr = 0;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if (customerId > 0) ps.setInt(index++, customerId);
			if(kitFilter > -1) ps.setInt(index++, kitFilter);
			if(term.length() > 0) {
				ps.setString(index++, "%" + term + "%");
				ps.setString(index++, "%" + term + "%");
			}
			
			// Get the result sets
			ResultSet rs = ps.executeQuery();
			for(int i=0; rs.next(); i++) {
				if (i >= start && i < limit)
					products.add(new RAMProductVO(rs));
			}
			
			//Retrieve the total count of products to properly show pagination 
			ctr = getRecordCount(customerId, term, kitFilter);
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
	protected int getRecordCount(int customerId, String term, int kitFilter) 
	throws SQLException {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder("select count(*) from ");
		s.append(schema).append("ram_product a ");
		s.append(this.getWhereClause(customerId, term, kitFilter));
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		int index = 1;
		if (customerId > 0) ps.setInt(index++, customerId);
		if (kitFilter > -1) ps.setInt(index++, kitFilter);
		if(term.length() > 0) {
			ps.setString(index++, "%" + term + "%");
			ps.setString(index++, "%" + term + "%");
		}
		
		
		ResultSet rs = ps.executeQuery();
		rs.next();
		
		return rs.getInt(1);
	}
	
	/**
	 * Build the where clause for the product Retrieval
	 * @param customerId
	 * @return
	 */
	protected StringBuilder getWhereClause(int customerId, String term, int kitFilter) {
		StringBuilder sb = new StringBuilder();
		sb.append("where 1=1 ");
		if (customerId > 0) sb.append("and a.customer_id = ? ");
		if(kitFilter > -1) sb.append("and kit_flg = ? ");
		if(term.length() > 0) sb.append("and (product_nm like ? or cust_product_id like ?) ");
		
		return sb;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
}
