/**
 * 
 */
package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.ProductRecallItemVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductRecallAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Handles all Actions related to creating, listing and
 * editing RAM Product Recall Items.
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
public class ProductRecallAction extends SBActionAdapter {

	/**
	 * Default Constructor
	 */
	public ProductRecallAction() {
		super();
		
	}

	/**
	 * General Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public ProductRecallAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		
		//Build Query.  We don't delete and rows, we only deactivate them.
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT_RECALL_ITEM set ACTIVE_FLG = 0 where PRODUCT_RECALL_ITEM_ID = ?");
		
		PreparedStatement ps = null;
		
		//Build statement and execute.
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("productRecallItemId"));
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deactivating Product Recall: " + req.getParameter("productRecallItemId"), sqle);
		}
		
		//Redirect User to list
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		//Instantiate List and determine lookup method.
		List<ProductRecallItemVO> recalls = new ArrayList<ProductRecallItemVO>();		
		//Build query for individual or list lookup.
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT_RECALL_ITEM where PRODUCT_ID = ?");
		
		//Build the Statement for lookup
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("productId"));
			
			//Retreive results and populate the Products List
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				recalls.add(new ProductRecallItemVO(rs, false));
		} catch(SQLException sqle) {
			log.error("Error retrieving product list", sqle);
		}
		//Return List to View
		this.putModuleData(recalls);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
		//Determine if Update or Insert
		boolean isInsert = Convert.formatBoolean(req.hasParameter("isInsert"));
		for(String param : req.getParameterMap().keySet()) {
			log.debug(param);
		}
		//Build Query
		StringBuilder sb = new StringBuilder();
		if(isInsert) {
			sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_PRODUCT_RECALL_ITEM (ACTIVE_FLG, CREATE_DT, LOT_NUMBER_TXT, ");
			sb.append("PRODUCT_ID) values (?,?,?,?)");
			
		} else {
			sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_PRODUCT_RECALL_ITEM set ACTIVE_FLG = ?, UPDATE_DT = ? ");
			sb.append("where PRODUCT_RECALL_ITEM_ID = ?");
		}
		
		PreparedStatement ps = null;
		int i = 1;
		
		//Build PreparedStatement and set params based on isInsert Flag 
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, req.getParameter("activeFlg"));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			if(isInsert) {
				ps.setString(i++, req.getParameter("lotNumberTxt"));
				ps.setString(i++, req.getParameter("productId"));
			} else {
				ps.setString(i++, req.getParameter("productRecallItemId"));
			}
			
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + req.getParameter("productRecallItemId"), sqle);
		}
		
		//Redirect User to list
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}
	
}
