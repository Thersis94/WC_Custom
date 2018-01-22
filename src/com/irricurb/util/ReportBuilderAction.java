package com.irricurb.util;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Irricurb Libs
import com.irricurb.action.project.ProjectSelectionAction;

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
import com.siliconmtn.util.EnumUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

/********************************************************************
 * <b>Title: </b>ReportBuilderAction.java<br/>
 * <b>Description: </b>Retrieves the data for a given report.  Assumes the return of a SMTChartVO<br/>
 * <b>Copyright: </b>Copyright (c) 2018<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jan 19, 2018
 * Last Updated: 
 *******************************************************************/
public class ReportBuilderAction extends SimpleActionAdapter {
	/**
	 * Key for the parameter when passing json data
	 */
	public static final String ACTION_TYPE_KEY = "actionType";
	
	/**
	 * Key for the parameter when passing json data
	 */
	public static final String CHART_TYPE_KEY = "chartType";
	
	/**
	 * Collection of actionTypes that identifies the list of data types to include
	 */
	public static Map<String, List<String>> ACTION_TYPES = new HashMap<String, List<String>>() {
		private static final long serialVersionUID = 1L; {
			put("SOIL", Arrays.asList("MOISTURE"));
			put("AIR", Arrays.asList("TEMPERATURE", "HUMIDITY"));
			put("PH",  Arrays.asList("PH"));
		}
		
	};
	/**
	 * 
	 */
	public ReportBuilderAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ReportBuilderAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter(ACTION_TYPE_KEY)) return;
		
		String projectId = (String) req.getSession().getAttribute(ProjectSelectionAction.PROJECT_LOOKUP);
		String actionType = req.getStringParameter(ACTION_TYPE_KEY, "").toUpperCase();
		String chartType = req.getStringParameter(CHART_TYPE_KEY, "").toUpperCase();
		ChartType ct = EnumUtil.safeValueOf(ChartType.class, chartType, ChartType.COLUMN);
		putModuleData(buildDashboardReport(projectId, ACTION_TYPES.get(actionType), ct));
	}
	
	/**
	 * Generates the report and formats it into a chart object for humidity and temperature
	 * @return
	 */
	protected SMTChartIntfc buildDashboardReport(String projectId, List<String> attributes, ChartType chartType) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select cast(row_number() over (order by b.device_attribute_id nulls last) as varchar(32)) as chart_detail_id, ");
		sql.append("b.device_attribute_id as serie_nm, cast(round(avg(reading_value_no), 1) as varchar(32)) as value, ");
		sql.append("extract(hour from reading_dt) || ':00' as label_nm ");
		sql.append("from custom.ic_project_device p ");
		sql.append("inner join custom.ic_project_device_data a on p.project_device_id = a.project_device_id ");
		sql.append("inner join custom.ic_data_entity b on a.project_device_data_id = b.project_device_data_id ");
		sql.append("inner join custom.ic_device_attribute c on b.device_attribute_id = c.device_attribute_id ");
		sql.append("where project_id = ? and b.device_attribute_id in (").append(DBUtil.preparedStatmentQuestion(attributes.size())).append(") ");
		sql.append("group by serie_nm, label_nm ");
		sql.append("order by label_nm, serie_nm");
		log.debug(sql);
		
		// retrieve the data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<Object> params = new ArrayList<>();
		params.add(projectId);
		params.addAll(attributes);
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(),params, new SMTChartDetailVO());
		
		// Process the data into the chartvo
		SMTChartVO chart = new SMTChartVO(chartData);
		chart.setPrimaryXTitle("Hour of Day");
		chart.setPrimaryYTitle("Temperature(F)");
		
		SMTChartIntfc theChart = SMTChartFactory.getInstance(ProviderType.GOOGLE, chart, null);
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(chartType, ProviderType.GOOGLE, true);
		options.addOptionsFromGridData(chart);
		theChart.addCustomValues(options.getChartOptions());
		
		return theChart;
	}

}
