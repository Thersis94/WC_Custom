package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





import com.ram.action.user.RamUserAction;
// RAM Data Feed Libs
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
import com.ram.datafeed.data.InventoryEventReturnVO;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
	 * Maps the extjs column names to the actual field names
	 */
	protected final Map<String, String> fieldMap = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1l;
		{
			put("locationName", "location_nm");
			put("scheduleDate", "schedule_dt");
			put("activeFlag", "active_Flg");
			put("inventoryCompleteDate", "location_nm");
			put("dataLoadCompleteDate", "location_nm");
			put("returnProducts", "location_nm");
		}
	};
	
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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		boolean isGlobal = Convert.formatBoolean(req.getParameter("isGlobal"));
		
		//build a VO off the request object, then call the reusable update(vo); method.
		InventoryEventVO event = new InventoryEventVO();
		event.setInventoryEventId(Convert.formatInteger(req.getParameter("inventoryEventId")));
		event.setInventoryEventGroupId(req.getParameter("inventoryEventGroupId"));
		event.setCustomerLocationId(Convert.formatInteger(req.getParameter("customerLocationId")));
		event.setScheduleDate(Convert.parseDateUnknownPattern(req.getParameter("scheduleDate")));
		event.setComment(req.getParameter("comments"));
		event.setActiveFlag(Convert.formatInteger(req.getParameter("activeFlag")));
		event.setVendorEventId(req.getParameter("vendorEventId"));
		
		//if this is a global update, take the abive fields and populate them into all events in this eventGroup
		if (isGlobal) {
			globalUpdate(event);
		} else {
			//update the single record passed  on the request
			List<InventoryEventVO> list = new ArrayList<InventoryEventVO>();
			list.add(event);
			this.update(list);
			req.setParameter("inventoryEventId", "" + list.get(0).getInventoryEventId());
		}
	}

	/**
	 * a reusable update method that works off a VO instead of the request object.
	 * also called from InventoryEventRecurrenceAction
	 * @param event
	 * @return
	 * @throws ActionException
	 */
	public void update(List<InventoryEventVO> data) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		// as the system grows this could be separated into two batch queries; one for inserts and one for updates.
		for (InventoryEventVO event : data) {
			if (event.getInventoryEventId() == null)
				event.setInventoryEventId(Integer.valueOf(0));
			
			// Build the 2 sql statements
			StringBuilder sql = new StringBuilder();
			if (event.getInventoryEventId() == 0) {
				sql.append("insert into ").append(schema).append("ram_inventory_event ");
				sql.append("(inventory_event_group_id, customer_location_id, comment_txt, ");
				sql.append("schedule_dt, active_flg, vendor_event_id, create_dt) ");
				sql.append("values (?,?,?,?,?,?,?) ");
			} else {
				sql.append("update ").append(schema).append("ram_inventory_event ");
				sql.append("set inventory_event_group_id = ?, customer_location_id = ?,");
				sql.append(" comment_txt = ?, schedule_dt = ?, active_flg = ?, ");
				sql.append("vendor_event_id = ?, update_dt = ?  ");
				sql.append("where inventory_event_id = ?");
			}
			
			// update or insert the record
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, event.getInventoryEventGroupId());
				ps.setInt(2, event.getCustomerLocationId());
				ps.setString(3, event.getComment());
				ps.setTimestamp(4, Convert.formatTimestamp(event.getScheduleDate()));
				ps.setInt(5, event.getActiveFlag());
				ps.setString(6, event.getVendorEventId());
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				if (event.getInventoryEventId() > 0) ps.setInt(8, event.getInventoryEventId());
				ps.executeUpdate();
				
				// Get the identity column id on an insert
				if (event.getInventoryEventId() == 0) {
					ResultSet generatedKeys = ps.getGeneratedKeys();
					if (generatedKeys.next())
						event.setInventoryEventId(generatedKeys.getInt(1));
				}
			} catch(SQLException sqle) {
				throw new ActionException(sqle);
			} finally {
				DBUtil.close(ps); 
			}
		}
		
		return;
	}
	

	/**
	 * global update performs an update of all the events in the given eventGroup to 
	 * have the same 'base' information.
	 * @param event
	 * @throws ActionException
	 */
	private void globalUpdate(InventoryEventVO event) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(schema).append("RAM_INVENTORY_EVENT ");
		sql.append("set schedule_dt=cast(convert(varchar,schedule_dt, 1)+? as datetime), ");
		sql.append("vendor_event_id=?, active_flg=?, comment_txt=?, ");
		sql.append("update_dt=? where inventory_event_group_id=? and schedule_dt >=getDate()");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			//we're going to allow time changes on the recurrences, but not the date; otherwise they'd all be on the same day
			ps.setString(1, Convert.formatDate(event.getScheduleDate(), " HH:mm:ss"));
			ps.setString(2, event.getVendorEventId());
			ps.setInt(3, event.getActiveFlag());
			ps.setString(4, event.getComment());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, event.getInventoryEventGroupId());
			int cnt = ps.executeUpdate();
			log.debug("updated " + cnt + " event records");
			
		} catch(SQLException sqle) {
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps); 
		}
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		int inventoryEventId = Convert.formatInteger(req.getParameter("inventoryEventId"));
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (inventoryEventId == 0) this.retrieveAll(req);
		else if(r.getRoleLevel() != RamUserAction.ROLE_LEVEL_PROVIDER) this.retrieveEvent(req, inventoryEventId);
	}
	
	/**
	 * Retrieves a single event data set
	 * @param req
	 * @param id
	 * @throws ActionException
	 */
	public void retrieveEvent(SMTServletRequest req, int id) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_inventory_event a ");
		sql.append("inner join ").append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("where a.inventory_event_id = ? ");
		log.info("Inventory Event Retrieve: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, id);
			
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
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

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
		
		String dir = StringUtil.checkVal(req.getParameter("dir"), "desc");
		String sort = StringUtil.checkVal(req.getParameter("sort"), "scheduleDate");
		sql.append("order by ").append(fieldMap.get(sort)).append(" " ).append(dir);
		
		log.info("SQL: " + sql);
		PreparedStatement ps = null;
		int ctr = -1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			if(r.getRoleLevel() == RamUserAction.ROLE_LEVEL_PROVIDER || r.getRoleLevel() == RamUserAction.ROLE_LEVEL_OEM)
				ps.setInt(3, Convert.formatInteger((String)r.getAttribute("roleAttributeKey_1")));
			ResultSet rs = ps.executeQuery();
			int navStart = Convert.formatInteger(req.getParameter("start"), 0);
			int navLimit = Convert.formatInteger(req.getParameter("limit"), 25);
			int navEnd = navStart + navLimit;
			
			while(rs.next()) {
				ctr ++;
				if (! (ctr >= navStart && ctr < navEnd)) continue;
				
				InventoryEventVO vo = new InventoryEventVO(rs, true, (String)attributes.get(Constants.ENCRYPT_KEY));
				String[] auditors = StringUtil.checkVal(rs.getString("auditors")).split(",");
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
		data.put("count", ctr + 1);
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
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		StringBuilder where = new StringBuilder();
		
		//If role is provider filter so it's only their locations.
		if(r.getRoleLevel() == RamUserAction.ROLE_LEVEL_PROVIDER)
			where.append(" and cl.customer_id = ? ");
		// Filter by the active flag
		if (req.hasParameter("activeFlag"))
			where.append(" and ie.active_flg = ").append(req.getParameter("activeFlag"));
		
		// Filter by the customer location
		if (Convert.formatInteger(req.getParameter("customerLocationId")) > 0) {
			where.append(" and ie.customer_location_id = '");
			where.append(req.getParameter("customerLocationId")).append("' ");
		}
		
		//If the user is an OEM, filter by locations by only those they have products at.
		if(r.getRoleLevel() == RamUserAction.ROLE_LEVEL_OEM) {
			where.append(" and cl.customer_location_id in ( ");
			where.append("select z.customer_location_id from ").append(schema).append("RAM_CUSTOMER_LOCATION z ");
			where.append("inner join ").append(schema).append("RAM_INVENTORY_EVENT ze on z.customer_location_id = ze.customer_location_id ");
			where.append("inner join ").append(schema).append("RAM_INVENTORY_EVENT_AUDITOR_XR za on ze.inventory_event_id = za.inventory_event_id ");
			where.append("inner join ").append(schema).append("RAM_INVENTORY_ITEM zi on za.inventory_Event_auditor_xr_id = za.inventory_Event_auditor_xr_id ");
			where.append("inner join ").append(schema).append("RAM_PRODUCT zp on zi.product_id = zp.product_id ");
			where.append("and zp.customer_id = ?)");
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
