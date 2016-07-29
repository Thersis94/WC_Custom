package com.ram.action.report;

// JDK 1.7.x
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.report.highchart.SeriesDataVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: DashboardReportAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>This action manages the data requests for the report graphs on the
 * RAM Dashboard.  These graphs will vary based upon the role of the user
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 28, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class DashboardReportAction extends SBActionAdapter {

	/**
	 * Possible transaction types. These are placed into proper order of execution
	 * which is why this is a linked Hashmap
	 */
	private final Map<String, String> transactionType = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1l;
		{
			put("region_li", "getRegionLineItems");
			put("event_sched", "getEventScheduled");
			put("weekly_items_sched", "getWeeklyItemsScheduled");
			put("region_ret", "getRegionReturns");
			put("prod_count_oem", "getProductCountOEM");
			put("prod_count_joint", "getProductCountJoint");
			put("event_upcoming", "getUpcomingEvent");
			put("upcoming_expiry", "getUpcomingExpiry");
		}
	};
	
	/**
	 * 
	 */
	public DashboardReportAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DashboardReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		// The transaction type must be passed or the action will exit
		String transType = StringUtil.checkVal(req.getParameter("transType"));
		if (transType.length() == 0) return;
		
		// Get the method name form the map
		String methodName = transactionType.get(transType);
		log.debug("retrieving " + transType);
		try {
			Method method = this.getClass().getMethod(methodName, SMTServletRequest.class);
			method.invoke(this, req);
		} catch (Exception e) {
			throw new ActionException("Unable to retrieve data for " + transType, e);
		}

	}
	
	/**
	 * Retrieves the Line Item Counts By Region Data
	 * @param req
	 */
	public void getRegionLineItems(SMTServletRequest req) throws SQLException{
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		String customerId = StringUtil.checkVal(user.getAttribute(0));
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Line item Counts, by Region");
		data.put("xLabel", "Regions");
		data.put("yLabel", "Line Items (000)");
		data.put("primaryLabel", "Value in ($M)");
		data.put("colors", new String[]{"#89A54E", "blue", "blue"});
		data.put("categories", getRegions());
		
		// Build the SQL statement
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select region_nm, sum(coalesce(rii.quantity_no, 0)) as item_count, ");
		sql.append("sum(coalesce(msrp_cost_no, 0)) as value from ").append(schema).append("ram_region rr ");
		sql.append("left outer join  ").append(schema);
		sql.append("ram_customer_location rcl on rr.region_id = rcl.region_id ");
		sql.append("left outer join ( ");
		sql.append("select inventory_event_id, customer_location_id, ");
		sql.append("inventory_complete_dt, max(inventory_complete_dt) as inv_dt ");
		sql.append("from  ").append(schema).append("ram_inventory_event ");
		sql.append("where inventory_complete_dt is not null ");
		sql.append("group by inventory_event_id,  customer_location_id, inventory_complete_dt ");
		sql.append(") as rie on rcl.customer_location_id = rie.customer_location_id ");
		sql.append("and rie.inventory_complete_dt = rie.inv_dt ");
		sql.append("left outer join  ").append(schema);
		sql.append("ram_inventory_event_auditor_xr aud on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append("left outer join ( ");
		sql.append("select inventory_event_auditor_xr_id, ii.quantity_no, msrp_cost_no ");
		sql.append("from ").append(schema).append("ram_inventory_item ii  ");
		sql.append("inner join ").append(schema).append("ram_product p ");
		sql.append("on ii.product_id = p.product_id ");
		if (customerId.length() > 0) sql.append("where customer_id = ").append(customerId);
		sql.append(" ) as rii ");
		sql.append("on aud.inventory_event_auditor_xr_id = rii.inventory_event_auditor_xr_id ");
		sql.append("group by region_nm ");
		sql.append("order by region_nm ");
		log.info("******* SQL: " + sql);
		
		// Get the data form the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO existCol = new SeriesDataVO("Existing Hosp", "column");
		SeriesDataVO newCol = new SeriesDataVO("New Hosp", "column");
		SeriesDataVO existSpline = new SeriesDataVO("Existing Hosp", "spline");
		SeriesDataVO newSpline = new SeriesDataVO("New Hosp", "spline");
		while (rs.next()) {
			existCol.addData(rs.getInt(2));
			newCol.addData(0);
			existSpline.addData(rs.getDouble(3));
			newSpline.addData(0);
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(existCol);
		series.add(newCol);
		series.add(existSpline);
		series.add(newSpline);
		data.put("series", series);

		// Add the data to the module vo
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Events Scheduled by region data
	 * @param req
	 */
	public void getEventScheduled(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		String customerId = StringUtil.checkVal(user.getAttribute(0));
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Weekly Events Scheduled, Executed");
		data.put("yLabel", "Events");
		data.put("xLabel", "Regions");
		data.put("colors", new String[]{"#89A54E", "red"});
		data.put("categories", getRegions());
		
		// Build the sql statement
		StringBuilder sql = new StringBuilder(500);
		sql.append("select region_nm, sum(case when schedule_dt is null then 0 else 1 end), ");
		sql.append("sum(case when inventory_complete_dt is null then 0 else 1 end) ");
		sql.append("from ").append(schema).append("ram_region rr ");
		sql.append("left outer join ").append(schema).append("ram_customer_location rcl "); 
		sql.append("on rr.region_id = rcl.region_id ");
		sql.append("left outer join ").append(schema).append("ram_inventory_event rie "); 
		sql.append("on rcl.customer_location_id = rie.customer_location_id "); 
		sql.append("and datepart(week, schedule_dt) = ? ");
		sql.append("and rie.inventory_event_id in ( ");
		sql.append("select inventory_event_id ");
		sql.append("from ").append(schema).append("ram_customer_event_xr ");
		if (customerId.length() > 0) sql.append("where customer_id = " + customerId);
		sql.append(") ");
		sql.append("group by region_nm ");
		sql.append("order by region_nm ");

		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO scheduled = new SeriesDataVO("Scheduled", "column");
		SeriesDataVO completed = new SeriesDataVO("Executed-to-Date", "spline");
		while (rs.next()) {
			scheduled.addData(rs.getInt(2));
			completed.addData(rs.getInt(3));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(scheduled);
		series.add(completed);
		data.put("series", series);
		
		// Add the data to the Module VO
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Line Item Counts By Region for the week data
	 * @param req
	 */
	public void getWeeklyItemsScheduled(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		String customerId = StringUtil.checkVal(user.getAttribute(0));
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Weekly Items Scheduled, Executed");
		data.put("yLabel", "Units(000)");
		data.put("xLabel", "Regions");
		data.put("colors", new String[]{"#aa4643", "#4572a7","#89a54e", "#5a5a5a"});
		data.put("categories", getRegions());
		
		// Build the sql statement
		StringBuilder sql = new StringBuilder(500);
		sql.append("select region_nm, sum(quantity_no) ");
		sql.append("from ").append(schema).append("ram_region rr ");
		sql.append("left outer join ").append(schema).append("ram_customer_location rcl "); 
		sql.append("on rr.region_id = rcl.region_id ");
		sql.append("left outer join ").append(schema).append("ram_inventory_event rie "); 
		sql.append("on rcl.customer_location_id = rie.customer_location_id "); 
		sql.append("and datepart(week, schedule_dt) = ? ");
		sql.append("and rie.inventory_event_id in ( ");
		sql.append("select inventory_event_id ");
		sql.append("from ").append(schema).append("ram_customer_event_xr ");
		if (customerId.length() > 0) sql.append("where customer_id = " + customerId);
		sql.append(") ");
		sql.append("left outer join ").append(schema).append("ram_inventory_event_auditor_xr aud ");
		sql.append("on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append("left outer join ").append(schema).append("ram_inventory_item rii ");
		sql.append("on aud.inventory_event_auditor_xr_id = rii.inventory_event_auditor_xr_id ");
		sql.append("group by region_nm ");
		sql.append("order by region_nm ");
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO completed = new SeriesDataVO("Executed-to-Date", "column");
		while (rs.next()) {
			completed.addData(rs.getInt(2));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(completed);
		data.put("series", series);
		
		// Add the data to the Module VO
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the receipts and returns by region
	 * @param req
	 */
	public void getRegionReturns(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		String customerId = StringUtil.checkVal(user.getAttribute(0));
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Receipts and Returns, by Region");
		data.put("yLabel", "Units");
		data.put("xLabel", "Regions");
		data.put("colors", new String[]{"#aa4643", "#4572a7", "#89A54E"});
		data.put("categories", getRegions());
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select region_nm, ");
		sql.append("sum(case ");
		sql.append("when inventory_item_type_cd = 'REPLENISHMENT' then 1 "); 
		sql.append("else 0 ");
		sql.append("end) as incoming,");
		sql.append("sum(case ");
		sql.append("when inventory_item_type_cd = 'DAMAGE_RETURN'  then 1 ");	
		sql.append("when inventory_item_type_cd = 'EXPIREE_RETURN'  then 1 ");
		sql.append("when inventory_item_type_cd = 'RECALL'  then 1 ");
		sql.append("when inventory_item_type_cd = 'TRANSFER'  then 1 ");
		sql.append("else 0 ");
		sql.append("end) as returned_items ");
		sql.append("from ").append(schema).append("ram_product rp ");
		sql.append("inner join ").append(schema).append("ram_inventory_item rii ");
		sql.append("on rii.product_id = rp.product_id ");
		if (customerId.length() > 0) sql.append("and customer_id = ").append(customerId);
		sql.append(" inner join ").append(schema).append("ram_inventory_event_auditor_xr aud "); 
		sql.append("on rii.inventory_event_auditor_xr_id = aud.inventory_event_auditor_xr_id "); 
		sql.append("and datepart(week, inventory_dt) = ? ");
		sql.append("inner join ").append(schema);
		sql.append("ram_inventory_event rie on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append("inner join ").append(schema);
		sql.append("ram_customer_location rcl on rie.customer_location_id = rcl.customer_location_id ");
		sql.append("right outer join ").append(schema);
		sql.append("ram_region rr on rcl.region_id = rr.region_id ");
		sql.append("group by region_nm ");
		sql.append("order by region_nm ");
		
		// Execute and store the data
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		
		SeriesDataVO receipt = new SeriesDataVO("Weekly Receipts", "column");
		SeriesDataVO returns = new SeriesDataVO("Weekly Returns", "spline");
		while (rs.next()) {
			receipt.addData(rs.getInt(2));
			returns.addData(rs.getInt(3));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(receipt);
		series.add(returns);
		data.put("series", series);
		
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the product count per OEM for the provider
	 * @param req
	 */
	public void getProductCountOEM(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		Integer customerId = Convert.formatInteger(user.getAttribute(0) + "");
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Products on Hand by OEM");
		data.put("yLabel", "Units");
		data.put("xLabel", "OEM");
		data.put("primaryLabel", "Average # Units");
		data.put("colors", new String[]{"#aa4643", "#4572a7", "#89A54E"});
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(400);
		sql.append("select customer_nm, sum(c.quantity_no) ");
		sql.append("from ").append(schema).append("ram_customer a ");
		sql.append("inner join ").append(schema).append("ram_product b ");
		sql.append("on a.customer_id = b.customer_id ");
		sql.append("inner join ").append(schema).append("ram_inventory_item c ");
		sql.append("on b.product_id = c.product_id ");
		sql.append("inner join ").append(schema).append("ram_inventory_event_auditor_xr d ");
		sql.append("on c.inventory_event_auditor_xr_id = d.inventory_event_auditor_xr_id ");
		sql.append("inner join ").append(schema).append("ram_inventory_event e ");
		sql.append("on d.inventory_event_id = e.inventory_event_id ");
		sql.append("inner join ").append(schema).append("ram_customer_location f ");
		sql.append("on e.customer_location_id = f.customer_location_id ");
		sql.append("where f.customer_id = ? and inventory_item_type_cd in ('SHELF','REPLENISHMENT') ");
		sql.append("and c.parent_id is null and inventory_complete_dt is not null ");
		sql.append("group by customer_nm, d.inventory_event_id, inventory_complete_dt ");
		sql.append("order by inventory_complete_dt desc");
		log.info("********* SQL: " + sql);
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, customerId);
		ResultSet rs = ps.executeQuery();
		SeriesDataVO units = new SeriesDataVO("Current Units", "column");
		SeriesDataVO average = new SeriesDataVO("Average Units", "spline");

		// Looping all of the events to grab the appropriate data
		List<String> categories = new ArrayList<>(32);
		int tot = 0, count = 0;
		String name = "", currName = "";
		while (rs.next()) {
			name = rs.getString(1);
			if (! name.equalsIgnoreCase(currName)) {
				if (currName.length() > 0) {
					average.addData(tot / count);
				}
				categories.add(rs.getString(1));
				units.addData(rs.getInt(2));
				tot = rs.getInt(2); count = 1;
			} else {
				count++;
				tot += rs.getInt(2);
			}
			
			currName = name;
		}
		// Collect the average for the final entry
		average.addData(tot / count);
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(units);
		series.add(average);
		data.put("series", series);
		data.put("categories", categories);
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Line Item Counts By joint for the provider
	 * @param req
	 */
	public void getProductCountJoint(SMTServletRequest req) throws SQLException {

		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Products on Hand by Type");
		data.put("yLabel", "Units)");
		data.put("xLabel", "OEM");
		data.put("colors", new String[]{"#aa4643", "#4572a7", "#89A54E"});
		data.put("categories", getCategories());
		
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves a list of upcoming events for a provider
	 * @param req
	 */
	public void getUpcomingEvent(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		Integer customerId = Convert.formatInteger(user.getAttribute(0) + "");
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(700);
		sql.append("select top 25 a.inventory_event_id,location_nm, schedule_dt, ");
		sql.append("count(c.customer_event_id) as oem_count, ");
		sql.append("count(d.event_return_id) as return_count ");
		sql.append("from ").append(schema).append("ram_inventory_event a ");
		sql.append("inner join ").append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("left outer join ").append(schema).append("ram_customer_event_xr c  ");
		sql.append("on a.inventory_event_id = c.inventory_event_id ");
		sql.append("left outer join ").append(schema).append("ram_event_return_xr d  ");
		sql.append("on a.inventory_event_id = d.inventory_event_id ");
		sql.append("where a.customer_location_id in ( ");
		sql.append("select customer_location_id  ");
		sql.append("from ").append(schema).append("ram_customer_location ");
		sql.append("where customer_id = ?) ");
		sql.append("and schedule_dt > ? ");
		sql.append("and a.active_flg = 1 ");
		sql.append("group by a.inventory_event_id,location_nm, schedule_dt ");
		sql.append("order by schedule_dt asc, location_nm ");
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, customerId);
		ps.setDate(2, Convert.formatSQLDate(Convert.formatDate("2014-06-01")));
		ResultSet rs = ps.executeQuery();
		
		List<Object[]> data = new ArrayList<>();
		while (rs.next()) {
			String sched = Convert.formatDate(rs.getDate(3), Convert.DATE_DASH_PATTERN);
			data.add(new Object[] {rs.getInt(1), rs.getString(2), sched, rs.getInt(4), rs.getInt(5)});
		}
		
		this.putModuleData(data, data.size(), false);
	}
	
	/**
	 * Retrieves the upcoming expiry data for a provider
	 * @param req
	 */
	public void getUpcomingExpiry(SMTServletRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		Integer customerId = Convert.formatInteger(user.getAttribute(0) + "");
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put("isSuccess", Boolean.TRUE);
		data.put("title", "Upcoming Expiree");
		data.put("yLabel", "Units");
		data.put("xLabel", "Days");
		data.put("colors", new String[]{"#4572a7", "#4572a7", "#89A54E"});
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select ");
		sql.append("sum(case when datediff(day, getdate(), expiration_dt) < 30 then 1 else 0 end) as thirty, ");
		sql.append("sum(case when datediff(day, getdate(), expiration_dt) between 30 and 60 then 1 else 0 end) as sixty, ");
		sql.append("sum(case when datediff(day, getdate(), expiration_dt) between 60 and 90 then 1 else 0 end) as ninety, ");
		sql.append("sum(case when datediff(day, getdate(), expiration_dt) between 90 and 120 then 1 else 0 end) as onetwenty ");
		sql.append("from ").append(schema).append("ram_inventory_event a ");
		sql.append("inner join ( ");
		sql.append("select a.customer_location_id, max(inventory_complete_dt) as complete_dt ");
		sql.append("from ").append(schema).append("ram_customer_location a  ");
		sql.append("inner join ").append(schema).append("ram_inventory_event b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("where a.customer_id = ? ");
		sql.append("group by a.customer_location_id ");
		sql.append(") b on a.customer_location_id = b.customer_location_id ");
		sql.append("and a.inventory_complete_dt = b.complete_dt ");
		sql.append("inner join ").append(schema).append("ram_inventory_event_auditor_xr c ");
		sql.append("on a.inventory_event_id = c.inventory_event_id ");
		sql.append("inner join ").append(schema).append("ram_inventory_item d ");
		sql.append("on c.inventory_event_auditor_xr_id = d.inventory_event_auditor_xr_id; ");
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, customerId);
		SeriesDataVO units = new SeriesDataVO("Units", "column");
		
		// Retrieve the data
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			units.addData(rs.getInt(1));
			units.addData(rs.getInt(2));
			units.addData(rs.getInt(3));
			units.addData(rs.getInt(4));
		}
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<SeriesDataVO>();
		series.add(units);
		data.put("series", series);
		data.put("categories", Arrays.asList(new String[]{"30 Days", "60 Days", "90 Days", "120 Days"}));
		
		this.putModuleData(data);
	}

	
	/**
	 * Retrieves the list of regions 
	 * @return
	 * @throws SQLException
	 */
	private List<String> getRegions() throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		List<String> cats = new ArrayList<>();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(schema).append("ram_region ");
		sql.append("where region_id not like 'dsj%' and region_id not like 'ram%' ");
		sql.append("order by region_id");
		
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			cats.add(rs.getString("region_nm"));
		}
		return cats;
	}
	
	/**
	 * Gets a list of categories for the provider report by type
	 * @return
	 * @throws SQLException
	 */
	private List<String> getCategories() throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		List<String> cats = new ArrayList<>();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select category_nm ");
		sql.append("from ").append(schema).append("ram_product_category a ");
		sql.append("where (parent_cd is null or parent_cd = 'ORTHO') ");
		sql.append("and a.product_category_cd != 'ORTHO' ");
		sql.append("order by category_nm ");
		
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			cats.add(rs.getString("category_nm"));
		}
		return cats;
	}	

	
}
