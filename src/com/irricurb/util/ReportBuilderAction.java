package com.irricurb.util;

import java.util.ArrayList;
// JDK 1.8.x
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
import com.siliconmtn.db.orm.DBProcessor;

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
		putModuleData(generateHumidityTempReport());
	}
	
	/**
	 * Generates the report and formats it into a chart object for humidity and temperature
	 * @return
	 */
	protected SMTChartIntfc generateHumidityTempReport() {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select cast(row_number() over (order by b.device_attribute_id nulls last) as varchar(32)) as chart_detail_id, ");
		sql.append("b.device_attribute_id as serie_nm, cast(round(avg(reading_value_no), 1) as varchar(32)) as value, ");
		sql.append("extract(hour from reading_dt) || ':00' as label_nm ");
		sql.append("from custom.ic_project_device p ");
		sql.append("inner join custom.ic_project_device_data a on p.project_device_id = a.project_device_id ");
		sql.append("inner join custom.ic_data_entity b on a.project_device_data_id = b.project_device_data_id ");
		sql.append("inner join custom.ic_device_attribute c on b.device_attribute_id = c.device_attribute_id ");
		sql.append("where b.device_attribute_id in ('TEMPERATURE', 'HUMIDITY') and project_id = 'PROJECT1' ");
		sql.append("group by serie_nm, label_nm ");
		sql.append("order by label_nm, serie_nm ");
		log.debug(sql);
		
		// retrieve the data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<SMTChartDetailVO> chartData = db.executeSelect(sql.toString(), new ArrayList<Object>(), new SMTChartDetailVO());
		
		// Process the data into the chartvo
		SMTChartVO chart = new SMTChartVO(chartData);
		chart.setPrimaryXTitle("Hour of Day");
		chart.setPrimaryYTitle("Temperature(F)");
		
		SMTChartIntfc theChart = SMTChartFactory.getInstance(ProviderType.GOOGLE, chart, null);
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(ChartType.PIE, ProviderType.GOOGLE, true);
		options.addOptionsFromGridData(chart);
		theChart.addCustomValues(options.getChartOptions());
		
		return theChart;
	}

}
