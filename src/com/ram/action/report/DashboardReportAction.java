package com.ram.action.report;

// JDK 1.7.x
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.util.SecurityUtil;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.report.highchart.SeriesDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
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
	
	// Constants for the class
	private static final String IS_SUCCESS = "isSuccess";
	private static final String RAM_REGION = "ram_region rr ";
	private static final String SERIES = "series";
	private static final String SPLINE = "spline";
	private static final String TITLE = "title";
	private static final String X_LABEL = "xLabel";
	private static final String Y_LABEL = "yLabel";
	private static final String UNITS = "Units";
	private static final String CATEGORIES = "categories";
	private static final String COLORS = "colors";
	private static final String REGIONS = "Regions";
	private static final String COLUMN = "column";
	private static final String REGION_GROUP = "group by region_nm ";
	private static final String REGION_ORDER = "order by region_nm ";
	private static final String COLOR_1 = "#aa4643";
	private static final String COLOR_2 = "#4572a7";
	private static final String COLOR_3 = "#89a54e";
	private static final String COLOR_4 = "#5a5a5a";

	/**
	 * Possible transaction types. These are placed into proper order of execution
	 * which is why this is a linked Hashmap
	 */
	private final Map<String, String> transactionType = new LinkedHashMap<>(16);
	
	/**
	 * 
	 */
	public DashboardReportAction() {
		super();
		initialize();
	}

	/**
	 * @param actionInit
	 */
	public DashboardReportAction(ActionInitVO actionInit) {
		super(actionInit);
		initialize();
	}
	
	
	private void initialize() {
		transactionType.put("region_li", "getRegionLineItems");
		transactionType.put("event_sched", "getEventScheduled");
		transactionType.put("weekly_items_sched", "getWeeklyItemsScheduled");
		transactionType.put("region_ret", "getRegionReturns");
		transactionType.put("prod_count_oem", "getProductCountOEM");
		transactionType.put("prod_count_joint", "getProductCountJoint");
		transactionType.put("surgery_month", "getProviderSurgeries");
		transactionType.put("upcoming_expiry", "getUpcomingExpiry");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// The transaction type must be passed or the action will exit
		String transType = StringUtil.checkVal(req.getParameter("transType"));
		if (transType.length() == 0) return;
		
		// Get the method name form the map
		String methodName = transactionType.get(transType);
		log.debug("retrieving " + transType);
		try {
			Method method = this.getClass().getMethod(methodName, ActionRequest.class);
			method.invoke(this, req);
		} catch (Exception e) {
			throw new ActionException("Unable to retrieve data for " + transType, e);
		}

	}
	
	/**
	 * Retrieves the Line Item Counts By Region Data
	 * @param req
	 */
	public void getRegionLineItems(ActionRequest req) throws SQLException{
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Line item Counts, by Region");
		data.put(X_LABEL, REGIONS);
		data.put(Y_LABEL, "Line Items (000)");
		data.put("primaryLabel", "Value in ($M)");
		data.put(COLORS, new String[]{COLOR_3, "blue", "blue"});
		data.put(CATEGORIES, getRegions());
		
		// Build the SQL statement
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select region_nm, sum(coalesce(rii.quantity_no, 0)) as item_count, ");
		sql.append("sum(coalesce(msrp_cost_no, 0)) as value from ").append(getCustomSchema()).append(RAM_REGION);
		sql.append("left outer join  ").append(getCustomSchema());
		sql.append("ram_customer_location rcl on rr.region_id = rcl.region_id ");
		sql.append("left outer join ( ");
		sql.append("select inventory_event_id, customer_location_id, ");
		sql.append("inventory_complete_dt, max(inventory_complete_dt) as inv_dt ");
		sql.append("from  ").append(getCustomSchema()).append("ram_inventory_event ");
		sql.append("where inventory_complete_dt is not null ");
		sql.append("group by inventory_event_id,  customer_location_id, inventory_complete_dt ");
		sql.append(") as rie on rcl.customer_location_id = rie.customer_location_id ");
		sql.append("and rie.inventory_complete_dt = rie.inv_dt ");
		sql.append("left outer join  ").append(getCustomSchema());
		sql.append("ram_inventory_event_auditor_xr aud on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append("left outer join ( ");
		sql.append("select inventory_event_auditor_xr_id, ii.quantity_no, msrp_cost_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_inventory_item ii  ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product p ");
		sql.append("on ii.product_id = p.product_id ");
		sql.append("inner join custom.ram_customer rc on p.customer_id = rc.customer_id ");
		sql.append(SecurityUtil.addOEMFilter(req, "rc"));
		sql.append(" ) as rii ");
		sql.append("on aud.inventory_event_auditor_xr_id = rii.inventory_event_auditor_xr_id ");
		sql.append(REGION_GROUP);
		sql.append(REGION_ORDER);
		log.debug("SQL: " + sql);
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO existCol = new SeriesDataVO("Existing Hosp", COLUMN);
		SeriesDataVO newCol = new SeriesDataVO("New Hosp", COLUMN);
		SeriesDataVO existSpline = new SeriesDataVO("Existing Hosp", SPLINE);
		SeriesDataVO newSpline = new SeriesDataVO("New Hosp", SPLINE);
		while (rs.next()) {
			existCol.addData(rs.getInt(2));
			newCol.addData(0);
			existSpline.addData(rs.getDouble(3));
			newSpline.addData(0);
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(existCol);
		series.add(newCol);
		series.add(existSpline);
		series.add(newSpline);
		data.put(SERIES, series);

		// Add the data to the module vo
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Events Scheduled by region data
	 * @param req
	 */
	public void getEventScheduled(ActionRequest req) throws SQLException {
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Weekly Events Scheduled, Executed");
		data.put(Y_LABEL, "Events");
		data.put(X_LABEL, REGIONS);
		data.put(COLORS, new String[]{COLOR_3, "red"});
		data.put(CATEGORIES, getRegions());
		
		// Build the sql statement
		StringBuilder sql = new StringBuilder(500);
		sql.append("select region_nm, sum(case when schedule_dt is null then 0 else 1 end), ");
		sql.append("sum(case when inventory_complete_dt is null then 0 else 1 end) ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append(RAM_REGION);
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_customer_location rcl "); 
		sql.append("on rr.region_id = rcl.region_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_inventory_event rie "); 
		sql.append("on rcl.customer_location_id = rie.customer_location_id "); 
		sql.append("and date_part('week', schedule_dt) = ? ");
		sql.append("and rie.inventory_event_id in ( ");
		sql.append("select inventory_event_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_customer_event_xr ");
		sql.append(DBUtil.WHERE_1_CLAUSE).append(SecurityUtil.addOEMFilter(req, ""));
		sql.append(") ");
		sql.append(REGION_GROUP);
		sql.append(REGION_ORDER);
		log.debug(sql);
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO scheduled = new SeriesDataVO("Scheduled", COLUMN);
		SeriesDataVO completed = new SeriesDataVO("Executed-to-Date", SPLINE);
		while (rs.next()) {
			scheduled.addData(rs.getInt(2));
			completed.addData(rs.getInt(3));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(scheduled);
		series.add(completed);
		data.put(SERIES, series);
		
		// Add the data to the Module VO
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Line Item Counts By Region for the week data
	 * @param req
	 */
	public void getWeeklyItemsScheduled(ActionRequest req) throws SQLException {
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Weekly Items Scheduled, Executed");
		data.put(Y_LABEL, "Units(000)");
		data.put(X_LABEL, REGIONS);
		data.put(COLORS, new String[]{COLOR_1, COLOR_2,COLOR_3, COLOR_4});
		data.put(CATEGORIES, getRegions());
		
		// Build the sql statement
		StringBuilder sql = new StringBuilder(500);
		sql.append("select region_nm, sum(coalesce(quantity_no, 0)) ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append(RAM_REGION);
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(select region_id, rii.quantity_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_customer_location rcl "); 
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_event rie "); 
		sql.append("on rcl.customer_location_id = rie.customer_location_id "); 
		sql.append("and date_part('week', schedule_dt) = ? ");
		sql.append("and rie.inventory_event_id in ( ");
		sql.append("select inventory_event_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_customer_event_xr ");
		sql.append(") ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_event_auditor_xr aud ");
		sql.append("on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_item rii ");
		sql.append("on aud.inventory_event_auditor_xr_id = rii.inventory_event_auditor_xr_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product rp on rii.product_id = rp.product_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE).append(SecurityUtil.addOEMFilter(req, "rp"));
		sql.append(") j on rr.region_id = j.region_id ");
		sql.append(REGION_GROUP);
		sql.append(REGION_ORDER);
		log.debug(sql);
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO completed = new SeriesDataVO("Executed-to-Date", COLUMN);
		while (rs.next()) {
			completed.addData(rs.getInt(2));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(completed);
		data.put(SERIES, series);
		
		// Add the data to the Module VO
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the receipts and returns by region
	 * @param req
	 */
	public void getRegionReturns(ActionRequest req) throws SQLException {
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Receipts and Returns, by Region");
		data.put(Y_LABEL, UNITS);
		data.put(X_LABEL, REGIONS);
		data.put(COLORS, new String[]{COLOR_1, COLOR_2, COLOR_3});
		data.put(CATEGORIES, getRegions());
		
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
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_product rp ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer rc ");
		sql.append("on rp.customer_id = rc.customer_id ").append(SecurityUtil.addOEMFilter(req, "rc"));
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_inventory_item rii ");
		sql.append("on rii.product_id = rp.product_id ");
		sql.append(" inner join ").append(getCustomSchema()).append("ram_inventory_event_auditor_xr aud "); 
		sql.append("on rii.inventory_event_auditor_xr_id = aud.inventory_event_auditor_xr_id "); 
		sql.append("and date_part('week', inventory_dt) = ? ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("ram_inventory_event rie on rie.inventory_event_id = aud.inventory_event_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("ram_customer_location rcl on rie.customer_location_id = rcl.customer_location_id ");
		sql.append("right outer join ").append(getCustomSchema());
		sql.append("ram_region rr on rcl.region_id = rr.region_id ");
		sql.append(REGION_GROUP);
		sql.append(REGION_ORDER);
		log.debug(sql);
		
		// Execute and store the data
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, Convert.getCurrentWeek());
		ResultSet rs = ps.executeQuery();
		
		SeriesDataVO receipt = new SeriesDataVO("Weekly Receipts", COLUMN);
		SeriesDataVO returns = new SeriesDataVO("Weekly Returns", SPLINE);
		while (rs.next()) {
			receipt.addData(rs.getInt(2));
			returns.addData(rs.getInt(3));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(receipt);
		series.add(returns);
		data.put(SERIES, series);
		
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the product count per OEM for the provider
	 * @param req
	 */
	public void getProductCountOEM(ActionRequest req) throws SQLException {
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Products on Hand by OEM");
		data.put(Y_LABEL, UNITS);
		data.put(X_LABEL, "OEM");
		data.put("primaryLabel", "Average # Units");
		data.put(COLORS, new String[]{COLOR_1, COLOR_2, COLOR_3});
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(400);
		sql.append("select customer_nm, sum(qty_on_hand_no), sum(qty_on_hand_no)/count(*) ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_location_item_master a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product b ");
		sql.append("on a.product_id = b.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer c ");
		sql.append("on b.customer_id = c.customer_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		sql.append(" and a.customer_location_id in (select customer_location_id ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("ram_customer_location ").append(DBUtil.WHERE_1_CLAUSE);
		sql.append(SecurityUtil.addCustomerFilter(req, "")).append(") ");
		sql.append("group by customer_nm ");
		sql.append("order by customer_nm ");
		log.debug("SQL: " + sql);
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		SeriesDataVO units = new SeriesDataVO("Current Units", COLUMN);
		SeriesDataVO average = new SeriesDataVO("Average Units", SPLINE);

		// Looping all of the events to grab the appropriate data
		List<String> categories = new ArrayList<>(32);
		while (rs.next()) {
			average.addData(rs.getDouble(3));
			units.addData(rs.getInt(2));
			categories.add(rs.getString(1));
		}
		ps.close();
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(units);
		series.add(average);
		data.put(SERIES, series);
		data.put(CATEGORIES, categories);
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the Line Item Counts By joint for the provider
	 * @param req
	 */
	public void getProductCountJoint(ActionRequest req) throws SQLException {

		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Products on Hand by Type");
		data.put(Y_LABEL, "Units)");
		data.put(X_LABEL, "OEM");
		data.put(COLORS, new String[]{COLOR_1, COLOR_2, COLOR_3});
		
		// Looping all of the events to grab the appropriate data
		List<String> categories = new ArrayList<>(32);
		SeriesDataVO counts = new SeriesDataVO("On Hand", COLUMN);
		
		StringBuilder sql = new StringBuilder(768);
		sql.append("select coalesce(e.category_nm, 'Unassigned'), sum(a.qty_on_hand_no) ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_location_item_master a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_product b on a.product_id = b.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_product_category_xr c on b.product_id = c.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_product_category d on c.product_category_cd = d.product_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_product_category e on d.category_group_id = e.product_category_cd ");
		sql.append("where 1=1 and a.customer_location_id in ( ");
		sql.append("select customer_location_id ").append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("ram_customer_location where 1=1 ");
		sql.append(SecurityUtil.addCustomerFilter(req, "")); 
		sql.append(") group by e.category_nm order by e.category_nm ");
		log.debug(sql.length() + "|" + sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				counts.addData(rs.getInt(2));
				categories.add(rs.getString(1));
			}
		}
		
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(counts);
		data.put(SERIES, series);
		data.put(CATEGORIES, categories);
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves a list of upcoming events for a provider
	 * @param req
	 */
	public void getProviderSurgeries(ActionRequest req) throws SQLException {
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Surgeries by Month");
		data.put(Y_LABEL, "Surgeries");
		data.put(X_LABEL, "Month and Year");
		data.put(COLORS, new String[]{COLOR_1, COLOR_2, COLOR_3});
		
		// Looping all of the events to grab the appropriate data
		List<String> categories = new ArrayList<>(8);
		SeriesDataVO surgeries = new SeriesDataVO("Surgery", COLUMN);
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(512);
		sql.append("select count(*), to_char(date_trunc( 'month', surgery_dt ), 'Monthyyyy') as month_grp ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_case ");
		sql.append("where case_status_cd in ('CLOSED', 'OR_COMPLETE', 'SPD_IN_PROGRESS') ");
		sql.append("and surgery_dt > ? ");
		sql.append("and customer_location_id in (select customer_location_id ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("ram_customer_location ").append(DBUtil.WHERE_1_CLAUSE);
		sql.append(SecurityUtil.addCustomerFilter(req, "")); 
		sql.append(") group by month_grp order by month_grp desc ");
		log.debug(sql.length() + "|" + sql + "|" + Convert.formatSQLDate(Convert.formatDate(new Date(), Calendar.YEAR, -1)));
		
		// Get the data from the SQL
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDate(1, Convert.formatSQLDate(Convert.formatDate(new Date(), Calendar.YEAR, -1)));
			ResultSet rs = ps.executeQuery();
	
			while (rs.next()) {
				surgeries.addData(rs.getInt(1));
				categories.add(rs.getString(2));
			}
		}
		
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(surgeries);
		data.put(SERIES, series);
		data.put(CATEGORIES, categories);
		this.putModuleData(data);
	}
	
	/**
	 * Retrieves the upcoming expiry data for a provider
	 * @param req
	 */
	public void getUpcomingExpiry(ActionRequest req) throws SQLException {
		Object schema = attributes.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole user = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		Integer customerId = Convert.formatInteger(user.getAttribute(0) + "");
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select ");
		sql.append("sum(case when date_part('day', now()) - date_part('day', expiration_dt) < 30 then 1 else 0 end) as thirty, ");
		sql.append("sum(case when date_part('day', now()) - date_part('day', expiration_dt) between 30 and 60 then 1 else 0 end) as sixty, ");
		sql.append("sum(case when date_part('day', now()) - date_part('day', expiration_dt) between 60 and 90 then 1 else 0 end) as ninety, ");
		sql.append("sum(case when date_part('day', now()) - date_part('day', expiration_dt) between 90 and 120 then 1 else 0 end) as onetwenty ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_inventory_event a ");
		sql.append("inner join ( ");
		sql.append("select a.customer_location_id, max(inventory_complete_dt) as complete_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_customer_location a  ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ram_inventory_event b ");
		sql.append("on a.customer_location_id = b.customer_location_id ");
		sql.append("where a.customer_id = ? ");
		sql.append("group by a.customer_location_id ");
		sql.append(") b on a.customer_location_id = b.customer_location_id ");
		sql.append("and a.inventory_complete_dt = b.complete_dt ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ram_inventory_event_auditor_xr c ");
		sql.append("on a.inventory_event_id = c.inventory_event_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ram_inventory_item d ");
		sql.append("on c.inventory_event_auditor_xr_id = d.inventory_event_auditor_xr_id; ");
		
		// Get the data from the SQL
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, customerId);
		SeriesDataVO units = new SeriesDataVO(UNITS, COLUMN);
		
		// Retrieve the data
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			units.addData(rs.getInt(1));
			units.addData(rs.getInt(2));
			units.addData(rs.getInt(3));
			units.addData(rs.getInt(4));
		}
		
		//Add it to the list of series
		List<SeriesDataVO> series = new ArrayList<>();
		series.add(units);
		
		// Add the base chart information
		Map<String, Object> data = new HashMap<>();
		data.put(IS_SUCCESS, Boolean.TRUE);
		data.put(TITLE, "Upcoming Expiree");
		data.put(Y_LABEL, UNITS);
		data.put(X_LABEL, "Days");
		data.put(COLORS, new String[]{COLOR_2, COLOR_2, COLOR_3});
		data.put(SERIES, series);
		data.put(CATEGORIES, new Object[]{"30 Days", "60 Days", "90 Days", "120 Days"});
		
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
}
