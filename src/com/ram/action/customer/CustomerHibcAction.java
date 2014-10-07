/**
 * 
 */
package com.ram.action.customer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ram.datafeed.data.CustomerHibcVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CustomerHibcAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles saving the HIBC records out to 
 * their own table.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Sep 29, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class CustomerHibcAction extends SBActionAdapter {

	/**
	 * 
	 */
	public CustomerHibcAction() {
	}

	/**
	 * @param actionInit
	 */
	public CustomerHibcAction(ActionInitVO actionInit) {
		super(actionInit);

	}

	/**
	 * Retrieve all the Customer Hibc Codes for a given CustomerId
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		//Fast fail if customerId is missing.
		if(!req.hasParameter("customerId"))
			return;
		
		//Instantiate List and determine lookup method.
		List<CustomerHibcVO> hibcs = new ArrayList<CustomerHibcVO>();		
		
		//Build query for individual or list lookup.
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_HIBC_CODE where CUSTOMER_ID = ?");
		
		//Build the Statement for lookup
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("customerId"));
			
			//Retrieve results and populate the Hibc List
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				hibcs.add(new CustomerHibcVO(rs));
			
		} catch(SQLException sqle) {
			log.error("Error retrieving hibc codes", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Return List to View
		this.putModuleData(hibcs);
	}
	
	/**
	 * Hibc Codes come back as a list of values.  Here we parse out the
	 * values into individual Codes and send them to be saved.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
		//Get the List of parameter Names.
		List<String> paramNames =Collections.list(req.getParameterNames());
		CustomerHibcVO item = null;

		/*
		 * Iterate over all the parameters.  If the name matches our prefix then 
		 */
		for (String name: paramNames) {
			if (name.startsWith("customerHibc_")) {
				item = new CustomerHibcVO();
				// Parse the delimited data into the vo
				String data = req.getParameter(name);
				String[] vals = data.split("\\|");
				item.setHibcCodeId(Convert.formatInteger(vals[0]));
				item.setCustomerId(Convert.formatInteger(vals[1]));
				item.setHibcCode(vals[2]);
				item.setActiveFlag(Convert.formatInteger(vals[3]));
				item.setCustomerHibcNm(vals[4]);
				
				// Only add the recall item if the lot number has been passed
				if (StringUtil.checkVal(item.getHibcCode()).length() > 0)
					buildItem(item);
			}
		}
		
		//If we updated a record then update the Customer Record to show a modification.
		if(item != null) updateCustomerRecord(item.getCustomerId());
	}
	
	/**
	 * Used to update the customer Record so that the sync operation will pick
	 * up the changes to the hibc codes.  This way when a field user requests a
	 * sync the customer table will show pending changes.
	 * @param customerId
	 */
	private void updateCustomerRecord(int customerId) {
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer set update_dt = ? where customer_id = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setInt(2, customerId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not update customer record : " + customerId, sqle);
		} finally {
			DBUtil.close(ps);
		}
	}

	/**
	 * Updates or inserts a Customer Hibc record into the table.
	 * @param item
	 */
	protected void buildItem(CustomerHibcVO item) throws ActionException{
		//Build Query
		StringBuilder sb = new StringBuilder();
		if(item.getHibcCodeId() == 0) {
			sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("ram_hibc_code (active_flg, create_dt, hibc_code, ");
			sb.append("hibc_customer_nm, customer_id) values (?,?,?,?,?)");
			
		} else {
			sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("ram_hibc_code set active_flg = ?, update_dt = ?, ");
			sb.append("hibc_code = ?, hibc_customer_nm = ?  where hibc_code_id = ?");
		}
		log.info("Customer Hibc SQL: " + sb);
		
		PreparedStatement ps = null;
		int i = 1;
		
		//Build PreparedStatement and set params based on presence of HibcId 
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(i++, item.getActiveFlag());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, item.getHibcCode());
			ps.setString(i++, item.getCustomerHibcNm());
			if (item.getHibcCodeId() > 0) ps.setInt(i++, item.getHibcCodeId());
			else ps.setInt(i++, item.getCustomerId());
			
			//Execute
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error updating Product: " + item.getHibcCodeId(), sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
	}

}
