package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// RAM Data Feed
import com.ram.datafeed.data.CustomerEventVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

// WC Libs
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
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_customer_event_xr a ");
		sql.append("inner join ").append(schema).append("ram_customer b ");
		sql.append("on a.customer_id = b.customer_id and inventory_event_id = ? ");
		
		PreparedStatement ps = null;
		List<CustomerEventVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("inventoryEventId")));
			
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
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		int inventoryEventId = Convert.formatInteger(req.getParameter("inventoryEventId"));
		StringBuilder sql = null;
		List<String> ele = this.getParameters(req);
		for(String val : ele) {
			boolean insert = false;
			CustomerEventVO vo = parseData(val, inventoryEventId);
			
			if (vo.getCustomerEventId() == 0) {
				insert = true;
				sql = buildInsertRecord();
			} else sql = buildUpdateRecord();
			
			this.updateDatabase(vo, sql, insert);
		}
	}
	
	/**
	 * Takes the pipe delimited list of data and splits into the appropriate variables
	 * @param data
	 * @param inventoryEventId
	 * @return
	 */
	protected CustomerEventVO parseData(String data, int inventoryEventId) {
		String[] items = data.split("\\|");
		CustomerEventVO vo = new CustomerEventVO();
		vo.setInventoryEventId(inventoryEventId);
		vo.setCustomerEventId(Convert.formatInteger(items[0]));
		vo.setCustomerId(Convert.formatInteger(items[1]));
		vo.setActiveFlag(Convert.formatInteger(items[2]));
		
		return vo;
	}
	
	/**
	 * Updates the database using the provided data and sql
	 * @param vo
	 * @param sql
	 * @throws ActionException
	 */
	public void updateDatabase(CustomerEventVO vo, StringBuilder sql, boolean insert) 
	throws ActionException {
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, vo.getInventoryEventId());
			ps.setInt(2, vo.getCustomerId());
			ps.setInt(3, vo.getActiveFlag());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			if (! insert) ps.setInt(5, vo.getCustomerEventId());
			
			ps.executeUpdate();
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
	}
	
	/**
	 * Gets the pipe delimited list of values for the event returns
	 * @param req
	 * @return
	 */
	protected List<String> getParameters(ActionRequest req) {
		List<String> data = new ArrayList<>();
		List<String> vals = Collections.list(req.getParameterNames());
		
		for(String val : vals) {
			if (val.startsWith("customerEvents_"))  data.add(req.getParameter(val));
		}
		
		return data;
	}
	
	/**
	 * Builds the insert statement
	 * @return
	 */
	protected StringBuilder buildInsertRecord() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema).append("ram_customer_event_xr ");
		sql.append("(inventory_event_id, customer_id, active_flg, create_dt) "); 
		sql.append("values(?,?,?,?)");
		
		return sql;		
	}
	
	/**
	 * Builds the update statement
	 * @return
	 */
	protected StringBuilder buildUpdateRecord() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(schema).append("ram_customer_event_xr ");
		sql.append("set inventory_event_id = ?, customer_id = ?, ");
		sql.append("active_flg = ?, update_dt = ? ");
		sql.append("where customer_event_id = ?");
		
		return sql;		
	}
}
