package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





// RAM Data Feed Libs
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
import com.ram.datafeed.data.InventoryEventReturnVO;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Manages the event data for the ram analytics engine
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james camire
 * @version 1.0
 * @since May 27, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventAction extends SBActionAdapter {

	/**
	 * 
	 */
	public InventoryEventAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String inventoryEventId = StringUtil.checkVal(req.getParameter("inventoryEventId"));
		
		if (inventoryEventId.length() == 0) this.retrieveAll(req);
		else if (! inventoryEventId.equalsIgnoreCase("NEW_EVENT"))
			this.retrieveEvent(req, inventoryEventId);
	}
	
	/**
	 * Retrieves a single event data set
	 * @param req
	 * @param id
	 * @throws ActionException
	 */
	public void retrieveEvent(SMTServletRequest req, String id) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_inventory_event a ");
		sql.append("inner join ").append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("where a.inventory_event_id = ? ");
		log.info("****************** " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				this.putModuleData(new InventoryEventVO(rs, true, null));
			}
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
	}
	
	/**
	 * Retrieves all for the data for the Events grid 
	 * @param req
	 * @throws ActionException
	 */
	public void retrieveAll(SMTServletRequest req) throws ActionException {
		List<InventoryEventVO> items = new ArrayList<>();
		
		// set the date start filter
		Date start = Convert.formatStartDate(Convert.formatDate(new Date(), Calendar.DAY_OF_MONTH, -1));
		if (StringUtil.checkVal(req.getParameter("from_date")).length() > 0) {
			start = Convert.formatStartDate(req.getParameter("from_date"));
		}
		
		// set the date end
		Date end = Convert.formatEndDate(Convert.formatDate(new Date(), Calendar.DAY_OF_MONTH, 7));
		if (StringUtil.checkVal(req.getParameter("to_date")).length() > 0) 
			end = Convert.formatEndDate(req.getParameter("to_date"));
		
		// Build the sql statement
		StringBuilder sql = this.getBaseSQL();
		sql.append(this.getListWhere(req));
		sql.append(this.getGroupBy());
		sql.append("order by location_nm, ie.inventory_event_id ");
		
		log.info("SQL: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				InventoryEventVO vo = new InventoryEventVO(rs, true, attributes.get(Constants.ENCRYPT_KEY) + "");
				String[] auditors = rs.getString("auditors").split(",");
				for (String auditor : auditors) {
					InventoryEventAuditorVO aud = new InventoryEventAuditorVO();
					aud.setAuditorName(auditor);
					vo.addAuditor(aud);
				}
				
				int numTransfers = rs.getInt("returnProducts"); 
				for (int i = 0; i < numTransfers; i++) {
					vo.addReturnProduct(new InventoryEventReturnVO());
				}
				
				items.add(vo);
			}
		} catch(SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {} 
		}
		
		
		Map<String, Object> data = new HashMap<>();
		data.put("count", items.size());
		data.put("data", items);
		data.put(GlobalConfig.SUCCESS_KEY, Boolean.TRUE);
		this.putModuleData(data, 3, false);
	}
	
	/**
	 * Builds the where clause for the Grid/select all
	 * @param req
	 * @return
	 */
	public StringBuilder getListWhere(SMTServletRequest req) {
		StringBuilder where = new StringBuilder();
		
		// Filter by the active flag
		if (StringUtil.checkVal(req.getParameter("activeFlag")).length() > 0) {
			where.append(" and ie.active_flg = ").append(req.getParameter("activeFlag"));
		}
		
		// Filter by the customer location
		if (StringUtil.checkVal(req.getParameter("customerLocationId")).length() > 0) {
			where.append(" and ie.customer_location_id = '");
			where.append(req.getParameter("customerLocationId")).append("' ");
		}

		return where;
	}
	
	/**
	 * Builds the base SQL Statement for retrieving the list of events for the
	 * admintool
	 * @return
	 */
	protected StringBuilder getBaseSQL() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select cl.location_nm, schedule_dt, ie.active_flg, inventory_complete_dt, ");
		sql.append("data_load_complete_dt, ie.inventory_event_id, ");
		sql.append("inventory_event_group_id, count(d.event_return_id) as returnProducts, ");
		sql.append("count(e.customer_event_id) as numberCustomers, ");
		sql.append("stuff(( ");
		sql.append("select ', ' + b.first_nm + ' ' + b.last_nm ");
		sql.append("from ").append(schema).append("ram_inventory_event_auditor_xr a ");
		sql.append("left outer join ").append(schema).append("ram_auditor b on a.auditor_id = b.auditor_id and b.active_flg = 1 ");
		sql.append("where a.inventory_event_id = ie.inventory_event_id ");
		sql.append("for xml path('')), 1, 1, '') as auditors ");
		sql.append("from ").append(schema).append("ram_inventory_event ie ");
		sql.append("inner join ").append(schema).append("ram_customer_location cl on ie.customer_location_id = cl.customer_location_id ");
		sql.append("left outer join ").append(schema).append("ram_event_return_xr d on ie.inventory_event_id = d.inventory_event_id ");
		sql.append("left outer join ").append(schema).append("ram_customer_event_xr e on ie.inventory_event_id = e.inventory_event_id ");
		sql.append("where schedule_dt between ? and ? ");
		
		return sql;
	}
	
	/**
	 * Builds the group by clause for the event list
	 * @return
	 */
	protected StringBuilder getGroupBy() {
		StringBuilder sql = new StringBuilder();
		sql.append(" group by cl.location_nm, schedule_dt, ie.active_flg, inventory_complete_dt,  ");
		sql.append("data_load_complete_dt, ie.inventory_event_id, ");
		sql.append("inventory_event_group_id ");
		
		return sql;
	}
	
}
