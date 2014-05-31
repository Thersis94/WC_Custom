package com.ram.action.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.CustomerEventVO;
import com.ram.datafeed.data.InventoryEventReturnVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventCustomerAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Manages information for the event returns
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 31, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventCustomerAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryEventCustomerAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventCustomerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_customer_event_xr a ");
		sql.append("inner join ").append(schema).append("ram_customer b ");
		sql.append("on a.customer_id = b.customer_id and inventory_event_id = ? ");
		
		PreparedStatement ps = null;
		List<CustomerEventVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("inventoryEventId"));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new CustomerEventVO(rs, true));
			}
			
			this.putModuleData(data, data.size(), false);
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
	}
}
