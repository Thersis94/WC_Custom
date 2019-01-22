package com.wsla.action;

import java.util.ArrayList;
//Jdk 1.8.x
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMTBase Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.chart.SMTChartDetailVO;
import com.siliconmtn.data.report.chart.SMTChartFactory;
import com.siliconmtn.data.report.chart.SMTChartFactory.ProviderType;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory.ChartType;
import com.siliconmtn.data.report.chart.SMTChartIntfc;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory;
import com.siliconmtn.data.report.chart.SMTChartOptionIntfc;
import com.siliconmtn.data.report.chart.SMTChartVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.common.UserSqlFilter;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: DashboardAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Calculates and generates the Dash board display for the
 * WSLA Portal
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 19, 2018
 * @updates:
 ****************************************************************************/

public class DashboardAction extends SimpleActionAdapter {
	
	/**
	 * Differentiates the request methods
	 */
	public static final String REQ_KEY = "type";

	/**
	 * Color pallette for the charts on the dashboard and the rest of the portal
	 */
	public static final List<String> CHART_COLORS = Collections.unmodifiableList(Arrays.asList(
		"#01579b", "#0277bd", "#0288d1", "#039be5", "#03a9f4", "#29b6f6")
	);
	
	/**
	 * 
	 */
	public DashboardAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DashboardAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		
		if (req.hasParameter("json")) {
			if ("attention".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getAttentionOrders(roleId));
				
			} else if ("trends".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getOrderTrends(req.getIntegerParameter("numMonths", 12), user, roleId));
				
			} else if ("progress".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getOrderProgress(req.getIntegerParameter("numDays", 30), user, roleId));
				
			}
			
		} else {
			// Get the summary data
			putModuleData(getSummaryData(roleId));
		}
	}
	
	/**
	 * Gets the summary data for the dashboard and places in a hashmap
	 * @return
	 */
	public Map<String, Integer> getSummaryData(String roleId) {
		Map<String, Integer> data = new HashMap<>();
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Get the number of opened
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket where status_cd != 'CLOSED'");
		data.put("open", db.executeSelect(sql.toString(), null, new SQLTotalVO()).get(0).getTotal());
		
		// Get the number closed since the first day of the week
		sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket_ledger where status_cd = 'CLOSED' ");
		sql.append("and create_dt > ? ");
		
		// Get the start of the week
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date d = Convert.formatDate(cal.getTime(), Calendar.DAY_OF_YEAR, 1 - cal.get(Calendar.DAY_OF_WEEK));
		data.put("closed-week", db.executeSelect(sql.toString(), Arrays.asList(d), new SQLTotalVO()).get(0).getTotal());
		
		// Get number of my orders
		sql = new StringBuilder(164);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_status b ");
		sql.append("on a.status_cd = b.status_cd where b.role_id = ? ");
		data.put("my-orders", db.executeSelect(sql.toString(), Arrays.asList(roleId), new SQLTotalVO()).get(0).getTotal());
		
		return data;
	}
	
	/**
	 * Gets the list of orders that need attention
	 * @return
	 */
	public List<TicketVO> getAttentionOrders(String roleId) {
		StringBuilder sql = new StringBuilder(884);
		sql.append("select a.create_dt, product_nm, role_nm, d.status_nm, ticket_no, ");
		sql.append("c.provider_nm, b.first_nm, b.last_nm, a.ticket_id, days_in_status ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user b ");
		sql.append("on a.originator_user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider c ");
		sql.append("on a.oem_id = c.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_status d ");
		sql.append("on a.status_cd = d.status_cd ");
		sql.append(DBUtil.INNER_JOIN).append("role e on d.role_id = e.role_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_serial f ");
		sql.append("on a.product_serial_id = f.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_master g ");
		sql.append("ON g.product_id = f.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(" ( ");
		sql.append("select cast(date_part('day',age(now(), create_dt)) as int) as days_in_status, "); 
		sql.append("ticket_id, status_cd ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket_ledger where status_cd != 'CLOSED') ");
		sql.append("as h on a.ticket_id = h.ticket_id and a.status_cd = h.status_cd ");
		sql.append("where standing_cd = 'CRITICAL'   and d.role_id = ? ");
		sql.append("order by create_dt asc");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(roleId), new TicketVO());
	}
	
	/**
	 * Gets the data for the order trends chart
	 * 
	 * @param numMonths
	 * @param roleId
	 * @param user
	 * @return
	 */
	public SMTChartIntfc getOrderTrends(int numMonths, UserVO user, String roleId) {
		UserSqlFilter filter = new UserSqlFilter(user, roleId, getCustomSchema());
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(1024);
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("to_char(t.create_dt, 'Mon') as label_nm, to_char(t.create_dt, 'Mon') as order_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'Closed' as serie_nm, ");
		sql.append("Extract(month from t.create_dt) as month_num, ");
		sql.append("Extract(year from t.create_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd = 'CLOSED' ");
		sql.append("and t.create_dt > to_char(t.create_dt - interval '");
		sql.append(numMonths).append(" month', 'YYYY-MM-01')::date ");
		sql.append("group by label_nm, year_num, month_num ");
		sql.append("union ");
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("to_char(t.create_dt, 'Mon') as label_nm, to_char(t.create_dt, 'Mon') as order_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'Open' as serie_nm, ");
		sql.append("Extract(month from t.create_dt) as month_num, ");
		sql.append("Extract(year from t.create_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd != 'CLOSED' ");
		sql.append("and t.create_dt > to_char(t.create_dt - interval '");
		sql.append(numMonths).append(" month', 'YYYY-MM-01')::date ");
		sql.append("group by label_nm, year_num, month_num ");
		sql.append("order by year_num, month_num, serie_nm ");
		log.debug("Dashboard SQL: " + sql.length() + "|" + sql);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), params, new SMTChartDetailVO());
		return buildChart(chartData, "", "", ChartType.COLUMN, true);
	}
	
	/**
	 * Gets the data for the order progress charts
	 * @param numDays
	 * @param roleId
	 * @return
	 */
	public SMTChartIntfc getOrderProgress(int numDays, UserVO user, String roleId) {
		UserSqlFilter filter = new UserSqlFilter(user, roleId, getCustomSchema());
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(800);
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("'Closed' as label_nm, cast(count(*) as varchar(10)) as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd = 'CLOSED' and ");
		sql.append("t.create_dt > now() - interval '").append(numDays).append(" day' ");
		sql.append(DBUtil.UNION);
		sql.append("select replace(newid(), '-', ''), 'Good', cast(count(*) as varchar(10)) "); 
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd != 'CLOSED' and t.standing_cd = 'GOOD' and ");
		sql.append("t.create_dt > now() - interval '").append(numDays).append(" day' ");
		sql.append(DBUtil.UNION);
		sql.append("select replace(newid(), '-', ''), 'Critical', cast(count(*) as varchar(10)) "); 
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd != 'CLOSED' and t.standing_cd = 'CRITICAL' and ");
		sql.append("t.create_dt > now() - interval '").append(numDays).append(" day' ");
		sql.append(DBUtil.UNION);
		sql.append("select replace(newid(), '-', ''), 'Delayed', cast(count(*) as varchar(10)) "); 
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(filter.getTicketFilter("t", params));
		sql.append("where t.status_cd != 'CLOSED' and t.standing_cd = 'DELAYED' and ");
		sql.append("t.create_dt > now() - interval '").append(numDays).append(" day' ");
		log.debug(sql);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), params, new SMTChartDetailVO());
		return buildChart(chartData, "Service Order Progress", "", ChartType.PIE, true);
	}
	
	/**
	 * Takes the chart data and returns the appropriate chart type
	 * @param data
	 * @param xTitle
	 * @param yTitle
	 * @param ct
	 * @param full
	 * @return
	 */
	protected SMTChartIntfc buildChart(List<SMTChartDetailVO> data, String xTitle, String yTitle, ChartType ct, boolean full) {
		// Process the data into the chartvo
		SMTChartVO chart = new SMTChartVO(data);
		chart.setPrimaryXTitle(xTitle);
		chart.setPrimaryYTitle(yTitle);
		SMTChartIntfc theChart = SMTChartFactory.getInstance(ProviderType.GOOGLE);
		theChart.processData(chart, ct);
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(ct, ProviderType.GOOGLE, full);

		// Add custom options for colors and stacking
		options.getChartOptions().put("colors", CHART_COLORS.toArray());
		if (ChartType.COLUMN.equals(ct)) options.getChartOptions().put("isStacked", Boolean.TRUE);
		options.addOptionsFromGridData(chart);
		theChart.addCustomValues(options.getChartOptions());
		
		return theChart;
	}
}

