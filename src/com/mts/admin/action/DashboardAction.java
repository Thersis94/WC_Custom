package com.mts.admin.action;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.chart.SMTChartDetailVO;
import com.siliconmtn.data.report.chart.SMTChartFactory;
import com.siliconmtn.data.report.chart.SMTChartIntfc;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory;
import com.siliconmtn.data.report.chart.SMTChartOptionIntfc;
import com.siliconmtn.data.report.chart.SMTChartVO;
import com.siliconmtn.data.report.chart.SMTChartFactory.ProviderType;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory.ChartType;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: DashboardAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Retrieves the data and formats for the Dashboard Graphs
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since April 22, 2019
 * @updates:
 ****************************************************************************/

public class DashboardAction extends SimpleActionAdapter {

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
	 * @param arg0
	 */
	public DashboardAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		int days = req.getIntegerParameter("numDays", 30);
		int months = req.getIntegerParameter("numMonths", 12);
		
		if ("publications".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getNumPublications(days));
		} else if ("articles".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getNumArticles(months));
		} else if ("subscribers".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getNumSubscribers(months));
		} else if ("logins".equalsIgnoreCase(req.getParameter("type"))) {
			setModuleData(getNumLogins(months));
		}
	}
	
	/**
	 * Builds the subscribers report
	 * @param numMonths
	 * @return
	 */
	public SMTChartIntfc getNumLogins(int numMonths) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(544);
		
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("to_char(login_dt, 'Mon') as label_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'Logins' as serie_nm, ");
		sql.append("Extract(month from login_dt) as month_num, ");
		sql.append("Extract(year from login_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append("authentication_log ");
		sql.append("where login_dt > now() - interval '");
		sql.append(numMonths).append(" month' ");
		sql.append("and site_id in (select site_id from site where organization_id = 'MTS') ");
		sql.append("group by label_nm, year_num, month_num "); 
		sql.append("order by year_num, month_num, serie_nm ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "User Logins", "", ChartType.COLUMN, true);
	}
	
	/**
	 * Builds the subscribers report
	 * @param numMonths
	 * @return
	 */
	public SMTChartIntfc getNumSubscribers(int numMonths) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(544);
		
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("to_char(create_dt, 'Mon') as label_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'Subscribers' as serie_nm, ");
		sql.append("Extract(month from create_dt) as month_num, ");
		sql.append("Extract(year from create_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_subscription_publication_xr ");
		sql.append("where create_dt > now() - interval '");
		sql.append(numMonths).append(" month' ");
		sql.append("group by label_nm, year_num, month_num "); 
		sql.append("order by year_num, month_num, serie_nm ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "Subscribers", "", ChartType.COLUMN, true);
	}
	
	/**
	 * Builds the articles report
	 * @param numMonths
	 * @return
	 */
	public SMTChartIntfc getNumArticles(int numMonths) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(544);
		
		sql.append("select replace(newid(), '-', '') as chart_detail_id, ");
		sql.append("to_char(publish_dt, 'Mon') as label_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'Articles' as serie_nm, ");
		sql.append("Extract(month from publish_dt) as month_num, ");
		sql.append("Extract(year from publish_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document ");
		sql.append("where publish_dt > now() - interval '");
		sql.append(numMonths).append(" month' ");
		sql.append("group by label_nm, year_num, month_num "); 
		sql.append("order by year_num, month_num, serie_nm ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "Articles", "", ChartType.COLUMN, true);
	}
	
	/**
	 * Grabs the chart data for the new member chart
	 * @param numDays
	 * @return
	 */
	public SMTChartIntfc getNumPublications(int numDays) {
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(512);
		sql.append("select a.publication_id as chart_detail_id, publication_nm as label_nm, ");
		sql.append("cast(coalesce(count(*), 0) as varchar(10)) as value, 'Issues' as serie_nm  ");
		sql.append("from ").append(getCustomSchema()).append("mts_publication a ");
		sql.append("left outer join ").append(getCustomSchema()).append("mts_issue b ");
		sql.append("on a.publication_id = b.publication_id ");;;
		sql.append("where a.approval_flg = 1 and initial_publication_dt is not null ");
		sql.append("group by chart_detail_id, label_nm ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "Publications", "", ChartType.COLUMN, true);
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

