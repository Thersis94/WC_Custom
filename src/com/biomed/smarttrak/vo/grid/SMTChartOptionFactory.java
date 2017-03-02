package com.biomed.smarttrak.vo.grid;

import com.biomed.smarttrak.vo.grid.SMTChartFactory.ProviderType;

/********************************************************************
 * <b>Title: </b>SMTChartOptionFactory.java<br/>
 * <b>Description: </b><<<< Some Desc Goes Here >>>><br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class SMTChartOptionFactory {
	/**
	 * List of supported Chart types
	 */
	public enum ChartType {
		AREA("Area Chart"), BAR("Bar Chart"), BUBBLE("Bubble Chart"), 
		COLUMN("Column Chart"),	COMBO("Combo Chart"), DONUT("Donut Chart"), 
		GAUGE("Guage Chart"), GEO("Geo Chart"),	LINE("Line Chart"), 
		PIE("PIE Chart"), SCATTER("Scatter Chart"), TABLE("Table Grid"),
		TRENDLINE("Trendline Chart"), NONE("None Selected");
		
		// Assign the name (get and set)
		private final String name;
		private ChartType(String name) { this.name = name; }
		public String getName() { return name; }
	}

	/**
	 * Returns the Chart options based upon the provider and the chart type
	 * @param type
	 * @param prov
	 * @return
	 */
	public static SMTChartOptionIntfc getInstance(ChartType type, ProviderType prov) {
		SMTChartOptionIntfc chart = null;
		
		switch(type) {
			case DONUT:
			case PIE:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GooglePieChartOptions();
				break;
			case BAR:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GoogleBarChartOptions();
				break;
			case COLUMN:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GoogleColumnChartOptions();
				break;
			default:
				break;
		}
		
		if (chart == null) chart = new SMTChartOptions();
		
		return chart;
	}
}

