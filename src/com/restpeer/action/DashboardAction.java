package com.restpeer.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.restpeer.common.RPConstants.MemberType;
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
 * @since Feb 25, 2019
 * @updates:
 ****************************************************************************/

public class DashboardAction extends SimpleActionAdapter {

	/**
	 * Color pallette for the charts on the dashboard and the rest of the portal
	 */
	public static final List<String> CHART_COLORS = Collections.unmodifiableList(Arrays.asList(
		"#01579b", "#0277bd", "#0288d1", "#039be5", "#03a9f4", "#29b6f6")
	);
	
	private static final String CHART_SELECT = "select replace(newid(), '-', '') as chart_detail_id, ";
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
		if ("members".equalsIgnoreCase(req.getParameter("type"))) {
			int days = req.getIntegerParameter("membersNumDays", 30);
			setModuleData(getNewMemberChart(days));
		} else if ("kitchens".equalsIgnoreCase(req.getParameter("type"))) {
			int months = req.getIntegerParameter("numMonths", 12);
			setModuleData(getNumKitchenLocations(months));
		}
	}
	
	/**
	 * Builds the kitchen locations report
	 * @param numMonths
	 * @return
	 */
	public SMTChartIntfc getNumKitchenLocations(int numMonths) {
		List<Object> vals = new ArrayList<>();
		vals.add(MemberType.KITCHEN.getDealerId());
		
		StringBuilder sql = new StringBuilder(544);
		sql.append(CHART_SELECT);
		sql.append("to_char(b.create_dt, 'Mon') as label_nm, "); 
		sql.append("cast(count(*) as varchar(10)) as value, 'New Locations' as serie_nm, ");
		sql.append("Extract(month from b.create_dt) as month_num, ");
		sql.append("Extract(year from b.create_dt) as year_num ");
		sql.append(DBUtil.FROM_CLAUSE).append("dealer a ");
		sql.append("inner join ");
		sql.append("dealer_location b on a.dealer_id = b.dealer_id ");
		sql.append("where dealer_type_id = ? ");
		sql.append("and b.create_dt > now() - interval '");
		sql.append(numMonths).append(" month' ");
		sql.append("group by label_nm, year_num, month_num "); 
		sql.append("order by year_num, month_num, serie_nm ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "Kitchen Locations", "", ChartType.COLUMN, true);
	}
	/**
	 * Grabs the chart data for the new member chart
	 * @param numDays
	 * @return
	 */
	public SMTChartIntfc getNewMemberChart(int numDays) {
		List<Object> vals = new ArrayList<>();
		vals.add(MemberType.KITCHEN.getDealerId());
		vals.add(MemberType.CUSTOMER.getDealerId());
		
		StringBuilder sql = new StringBuilder(512);
		sql.append(CHART_SELECT);
		sql.append("'Kitchen' as label_nm, cast(coalesce(count(*), 0) as varchar(10)) as value ");
		sql.append("from ").append("dealer ");
		sql.append("where dealer_type_id = ? ");
		sql.append("and create_dt > now() - interval '").append(numDays).append(" day' ");
		sql.append("and active_flg = 1 ");
		sql.append("union ");
		sql.append(CHART_SELECT).append("'Mobile Restaurant' as label_nm, ");
		sql.append("cast(coalesce(count(*), 0) as varchar(10)) as value ");
		sql.append("from ").append("dealer ");
		sql.append("where dealer_type_id = ? ");
		sql.append("and create_dt > now() - interval '").append(numDays).append(" day' ");
		sql.append("and active_flg = 1 ");
		log.debug(sql.length() + ":" + sql + vals);
		
		// Get the data and process into a chart vo
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), vals, new SMTChartDetailVO());
		return buildChart(chartData, "New Members", "", ChartType.PIE, true);
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

