/**
 * 
 */
package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.CustomerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CustomerProductAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> TODO
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 10, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class CustomerProductAction extends SBActionAdapter {

	public CustomerProductAction() {
		
	}
	
	public CustomerProductAction(ActionInitVO init) {
		super(init); 
	}
	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		//Instantiate List and determine lookup method.
		List<CustomerVO> customers = new ArrayList<CustomerVO>();
				
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct a.customer_id, b.CUSTOMER_NM from ");
		sb.append(customDb).append("RAM_PRODUCT a ");
		sb.append("inner join ").append(customDb).append("RAM_CUSTOMER b ");
		sb.append("on a.CUSTOMER_ID = b.CUSTOMER_ID");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				customers.add(new CustomerVO(rs, false));
			}
		} catch(SQLException sqle) {
			log.error(sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		//Return List to View
		this.putModuleData(customers);
	}
}
