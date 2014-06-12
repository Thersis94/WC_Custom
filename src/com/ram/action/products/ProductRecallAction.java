package com.ram.action.products;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// RAM Data Feed Libs
import com.ram.datafeed.data.ProductRecallItemVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
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
 *        James Camire: 6/12/2014 Modified the way the recall products were saved.
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
		List<String> paramNames =Collections.list(req.getParameterNames());
		for (String name: paramNames) {
			if (name.startsWith("productRecall_")) {
				ProductRecallItemVO item = new ProductRecallItemVO();
				
				// Parse the delimited data into the vo
				String data = req.getParameter(name);
				String[] vals = data.split("\\|");
				item.setProductId(Convert.formatInteger(vals[0]));
				item.setRecallItemId(Convert.formatInteger(vals[1]));
				item.setLotNumber(vals[2]);
				item.setActiveFlag(Convert.formatInteger(vals[3]));
				
				// Only add the recall item if the lot number has been passed
				if (StringUtil.checkVal(item.getLotNumber()).length() > 0)
					buildItem(item);
			}
		}
		
		
	}
	
	/**
	 * Updates or inserts a record into the table
	 * @param item
	 */
	protected void buildItem(ProductRecallItemVO item) {
		//Build Query
		StringBuilder sb = new StringBuilder();
		if(item.getRecallItemId() == 0) {
			sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("ram_product_recall_item (active_flg, create_dt, lot_number_txt, ");
			sb.append("product_id) values (?,?,?,?)");
			
		} else {
			sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("ram_product_recall_item set active_flg = ?, update_dt = ?, ");
			sb.append("lot_number_txt = ?, product_id = ?  where product_recall_item_id = ?");
		}
		log.info("Product Recall SQL: " + sb);
		
		PreparedStatement ps = null;
		int i = 1;
		
		//Build PreparedStatement and set params based on isInsert Flag 
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(i++, item.getActiveFlag());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, item.getLotNumber());
			ps.setInt(i++, item.getProductId());
			if (item.getRecallItemId() > 0) ps.setInt(i++, item.getRecallItemId());
			
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + item.getRecallItemId(), sqle);
		}
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
