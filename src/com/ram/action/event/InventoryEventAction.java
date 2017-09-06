package com.ram.action.event;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// RAM Data Feed Libs
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryItemVO;
import com.ram.datafeed.data.RAMProductVO;
import com.ram.action.report.vo.InventoryEventPDFReport;
import com.ram.action.report.vo.InventoryEventXLSReport;
import com.ram.action.util.SecurityUtil;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.WebCrescendoReport;
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
	protected final Map<String, String> fieldMap = new LinkedHashMap<>();
	public final String LOAD_REPORT = "loadReport";
	public final String PDF = "pdf";
	public final String XLS = "xls";
	
	public InventoryEventAction() {
		super();
		initFieldMap();
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventAction(ActionInitVO actionInit) {
		super(actionInit);
		initFieldMap();
	}
	
	/**
	 * Maps the extjs column names to the actual field names
	 */
	private final void initFieldMap() {
		fieldMap.put("locationName", "location_nm");
		fieldMap.put("scheduleDate", "schedule_dt");
		fieldMap.put("activeFlag", "active_Flg");
		fieldMap.put("inventoryCompleteDate", "inventory_complete_dt");
		fieldMap.put("dataLoadCompleteDate", "data_load_complete_dt");
		fieldMap.put("numberReturnedProducts", "returned_products_no");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
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
		event.setInventoryTypeCode(req.getParameter("inventoryTypeCode"));
		
		//if this is a global update, take the abive fields and populate them into all events in this eventGroup
		if (isGlobal) {
			globalUpdate(event);
		} else {
			//update the single record passed  on the request
			List<InventoryEventVO> list = new ArrayList<>();
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
				sql.append("schedule_dt, active_flg, vendor_event_id, inventory_type_cd, create_dt) ");
				sql.append("values (?,?,?,?,?,?,?,?) ");
			} else {
				sql.append("update ").append(schema).append("ram_inventory_event ");
				sql.append("set inventory_event_group_id = ?, customer_location_id = ?,");
				sql.append(" comment_txt = ?, schedule_dt = ?, active_flg = ?, ");
				sql.append("vendor_event_id = ?, inventory_type_cd, update_dt = ?  ");
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
				ps.setString(7, event.getInventoryTypeCode());
				ps.setTimestamp(8, Convert.getCurrentTimestamp());
				if (event.getInventoryEventId() > 0) ps.setInt(9, event.getInventoryEventId());
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
		sql.append("update_dt=?, inventory_type_cd=? where inventory_event_group_id=? and schedule_dt >=getDate()");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			//we're going to allow time changes on the recurrences, but not the date; otherwise they'd all be on the same day
			ps.setString(1, Convert.formatDate(event.getScheduleDate(), " HH:mm:ss"));
			ps.setString(2, event.getVendorEventId());
			ps.setInt(3, event.getActiveFlag());
			ps.setString(4, event.getComment());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, event.getInventoryTypeCode());
			ps.setString(7, event.getInventoryEventGroupId());
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
	public void retrieve(ActionRequest req) throws ActionException {

		int inventoryEventId = Convert.formatInteger(req.getParameter("inventoryEventId"));
		if ("event_list".equalsIgnoreCase(req.getParameter("actionType"))) {
			GridDataVO<InventoryEventVO> data = retrieveAll(req);
			this.putModuleData(data.getRowData(), data.getTotal(), false);
		} else if ("item_list".equalsIgnoreCase(req.getParameter("actionType"))) {
			
			GridDataVO<InventoryItemVO> data = getInventoryItemsSummary(req);
			putModuleData(data.getRowData(), data.getTotal(), false);
		} else if ("item_info".equalsIgnoreCase(req.getParameter("actionType"))) {

			putModuleData(getProductInfo(req.getParameter("customerProductId"), inventoryEventId));
			
		} else if(inventoryEventId > 0) { 
			InventoryEventVO event = retrieveEvent(req, inventoryEventId);
			event.setNumberReturnedProducts(getNumberItems(inventoryEventId, "DAMAGE_RETURN","EXPIREE_RETURN","RECALL","TRANSFER"));
			event.setNumberReceivedProducts(getNumberItems(inventoryEventId, "REPLENISHMENT"));
			event.setNumberTotalProducts(getNumberItems(inventoryEventId, "SHELF"));

			if (req.hasParameter(LOAD_REPORT) ){
				loadLineItemData(req,event);
				String reportType = StringUtil.checkVal(req.getParameter(LOAD_REPORT));
				req.setAttribute(Constants.BINARY_DOCUMENT, generateReport(reportType, event));
				req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
			}else{
				putModuleData(event);
			}
			
		}
	}
	
	/**
	 * adds the line items to the main event vo for the reports
	 * @param req
	 * @param event
	 */
	private void loadLineItemData(ActionRequest req, InventoryEventVO event) {
		List<Object> lines = getAllInventoryItemsSummary(req);
		
		for (Object o : lines){
			InventoryItemVO vo = (InventoryItemVO) o;
			event.addInventoryItem(vo);
		}
	}

	/**
	 * uses the report type string  to determine and build the requested type of report
	 * @param reportType
	 * @param event 
	 * @return
	 */
	private Object generateReport(String reportType, InventoryEventVO event) {
		AbstractSBReportVO report = null;
		
		if (PDF.equalsIgnoreCase(reportType)){
			String fileName = "Event-"+event.getInventoryEventId();
			report = new InventoryEventPDFReport();
			report.setAttributes(attributes);
			report.setFileName(fileName + ".pdf");
			report.setData(event);
		}
		
		if (XLS.equalsIgnoreCase(reportType)){
			InventoryEventXLSReport eReport = new InventoryEventXLSReport();
			eReport.setData(event);
			report = new WebCrescendoReport(eReport);
		}
		
		return report;
	}
	
	/**
	 * Gets the list of products and their lot number/expiry for a given inventory event
	 * @param customerProductId
	 * @param inventoryEventId
	 * @return
	 */
	public List<Object> getProductInfo(String customerProductId, int inventoryEventId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select inventory_item_id, cust_product_id, lot_number_txt, expiration_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_inventory_item a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_event_auditor_xr b ");
		sql.append("on a.inventory_event_auditor_xr_id = b.inventory_event_auditor_xr_id " );
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product c on a.product_id = c.product_id ");
		sql.append("where cust_product_id = ? and inventory_event_id = ? order by expiration_dt asc ");
		
		List<Object> params = new ArrayList<>();
		params.add(customerProductId);
		params.add(inventoryEventId);
		log.info(sql + "|" + params);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), params, new InventoryItemVO(), "inventory_item_id");
	}
	
	/**
	 * Gets the count of items received for an inventory
	 * @param inventoryEventId
	 * @return
	 */
	public int getNumberItems(int inventoryEventId, String... types) {
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_inventory_item a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_event_auditor_xr b ");
		sql.append("on a.inventory_event_auditor_xr_id = b.inventory_event_auditor_xr_id " );
		sql.append("where inventory_event_id = ? and inventory_item_type_cd in (");
		sql.append(DBUtil.preparedStatmentQuestion(types.length)).append(") ");
		
		List<Object> params = new ArrayList<>();
		params.add(inventoryEventId);
		for (String type : types) {
			params.add(type);
		}
		
		// Get the data
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> data = db.executeSelect(sql.toString(), params, new SQLTotalVO());
		
		return ((SQLTotalVO) data.get(0)).getTotal();
	}
	
	/**
	 * Retrieves the list of inventory items
	 * @param req
	 * @return
	 */
	public GridDataVO<InventoryItemVO> getInventoryItemsSummary(ActionRequest req) {
		// Add the sql params
		List<Object> params = new ArrayList<>();

		// return the data
		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSQLWithCount(getItemSQL(req, params), params, new InventoryItemVO(), "cust_product_id", limit, offset);
	}

	/**
	 * Retrieves the list of inventory items
	 * @param req
	 * @return
	 */
	public List<Object> getAllInventoryItemsSummary(ActionRequest req) {
		// Add the sql params
		List<Object> params = new ArrayList<>();
		// no limit no offset send back everything
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(getItemSQL(req, params), params, new InventoryItemVO(),"cust_product_id");
	}

	/**
	 * SQL Body for the items query
	 * @param sql
	 */
	public String getItemSQL(ActionRequest req, List<Object> params) {
		String sort = StringUtil.checkVal(DBUtil.getColumn(req.getParameter("sort"),new RAMProductVO(),""), "product_nm");
		String order = req.getParameter("order", "asc");
		
		StringBuilder sql = new StringBuilder();
		sql.append("select customer_nm, product_nm, cust_product_id, kit_flg, cast(sum(c.quantity_no) as int) as quantity_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_inventory_event_auditor_xr b ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_item c on b.inventory_event_auditor_xr_id = c.inventory_event_auditor_xr_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product d on c.product_id = d.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer e on d.customer_id = e.customer_id ");
		sql.append("where b.inventory_event_id = ? and inventory_item_type_cd = 'SHELF' ");
		params.add(req.getIntegerParameter("inventoryEventId"));
		
		if (req.hasParameter("search")) {
			sql.append(" and (lower(product_nm) like ? or lower(cust_product_id) like ?) ");
			params.add("%" + req.getParameter("search").toLowerCase() + "%");
			params.add("%" + req.getParameter("search").toLowerCase() + "%");
		}
		
		sql.append("group by customer_nm, product_nm, cust_product_id, kit_flg ");
		sql.append("order by ").append(sort).append(" ").append(order);
		
		return sql.toString();
	}
	
	/**
	 * Retrieves a single event data set
	 * @param req
	 * @param id
	 * @throws ActionException
	 */
	public InventoryEventVO retrieveEvent(ActionRequest req, int id) throws ActionException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("ram_inventory_event a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("where a.inventory_event_id = ? ");
		log.debug("Inventory Event Retrieve: " + sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, id);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String[] providers = new String[]{rs.getString("customer_id")};
				if (SecurityUtil.isAuthorized(req, 0, providers))
					return new InventoryEventVO(rs, true, null);
			}
		} catch(SQLException sqle) {
			throw new ActionException("Unable to retrieve event", sqle);
		} 
		
		return null;
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 */
	public GridDataVO<InventoryEventVO> retrieveAll(ActionRequest req) {
		List<Object> params = new ArrayList<>();
		
		// Build the sql statement
		StringBuilder sql = new StringBuilder(512);
		getSelectSQL(sql);
		getBaseSQL(sql, req, params);
		getListWhere(sql, req, params);
		getSQLOrder(sql, req);
		
		// Get the data
		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		log.debug("sql " + sql.toString());
		return db.executeSQLWithCount(sql.toString(), params, new InventoryEventVO(), limit, offset);
	}
	
	/**
	 * 
	 * @param sql
	 * @param req
	 * @param params
	 */
	public void getSQLOrder(StringBuilder sql, ActionRequest req) {
		sql.append("order by ").append(StringUtil.checkVal(fieldMap.get(req.getParameter("sort")), "location_nm"));
		sql.append(" ").append(StringUtil.checkVal(req.getParameter("order"), "asc"));
	}
	
	/**
	 * Builds the where clause for the Grid/select all
	 * @param req
	 * @return
	 */
	public void getListWhere(StringBuilder where, ActionRequest req, List<Object> params) {
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		
		//If role is provider filter so it's only their locations.
		where.append(SecurityUtil.addCustomerFilter(req, "cl"));
		
		// Filter by the active flag
		if (req.hasParameter("activeFlag"))
			where.append(" and ie.active_flg = ").append(req.getParameter("activeFlag"));
		
		// Filter by the customer location
		if (req.hasParameter("customerLocationId")) {
			where.append(" and ie.customer_location_id = ? ");
			params.add(req.getIntegerParameter("customerLocationId"));
		}
		
		//If the user is an OEM, filter by locations by only those they have products at.
		if(SecurityUtil.isOEMRole(r.getRoleId())) {
			where.append(" and cl.customer_location_id in ( ");
			where.append("select z.customer_location_id from ").append(getCustomSchema()).append("RAM_CUSTOMER_LOCATION z ");
			where.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("RAM_INVENTORY_EVENT ze on z.customer_location_id = ze.customer_location_id ");
			where.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("RAM_INVENTORY_EVENT_AUDITOR_XR za on ze.inventory_event_id = za.inventory_event_id ");
			where.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("RAM_INVENTORY_ITEM zi on za.inventory_Event_auditor_xr_id = za.inventory_Event_auditor_xr_id ");
			where.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("RAM_PRODUCT zp on zi.product_id = zp.product_id ");
			where.append("and zp.customer_id = ?)");
		}
	}
	
	/**
	 * Builds the sql statement independently
	 * @param sql
	 */
	public void getSelectSQL(StringBuilder sql) {
		sql.append("select cl.location_nm, cl.customer_id, schedule_dt, ie.active_flg, inventory_complete_dt, ");
		sql.append("data_load_complete_dt, ie.inventory_event_id, ra.auditor_nm, ");
		sql.append("inventory_event_group_id, cast(coalesce(returned_products_no, 0) as int) as returned_products_no, ");
		sql.append("vendor_event_id ");
	}
	
	/**
	 * Builds the base SQL Statement for retrieving the list of events for the
	 * admintool
	 * @return
	 */
	protected StringBuilder getBaseSQL(StringBuilder sql, ActionRequest req, List<Object> params) {

		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_inventory_event ie ");
		sql.append("inner join ").append(getCustomSchema()).append("ram_customer_location cl on ie.customer_location_id = cl.customer_location_id ");
		sql.append("left outer join ( ");
		sql.append("select inventory_event_id, array_to_string(array_agg(first_nm || ' ' || last_nm), '<br/>') as auditor_nm ");
		sql.append("from ").append(getCustomSchema()).append("ram_inventory_event_auditor_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_auditor b on a.auditor_id = b.auditor_id ");
		sql.append("group by inventory_event_id ) ra on ra.inventory_event_id = ie.inventory_event_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("( select inventory_event_id,  sum(quantity_no) returned_products_no ");
		sql.append("from ").append(getCustomSchema()).append("ram_event_return_xr ");
		sql.append("group by inventory_event_id ) ret on ie.inventory_event_id = ret.inventory_event_id ");
		sql.append("where schedule_dt between ? and ? ");
		
		params.add(Convert.formatStartDate(Convert.formatDate(req.getParameter("from_date")), new Date()));
		params.add(Convert.formatEndDate(Convert.formatDate(req.getParameter("to_date")), new Date()));
		
		return sql;
	}
}
