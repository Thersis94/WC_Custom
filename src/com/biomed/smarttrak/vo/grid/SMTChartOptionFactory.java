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
		GEO("Geo Chart"), LINE("Line Chart"), PIE("PIE Chart"), 
		SCATTER("Scatter Chart"), TABLE("Table Grid"), NONE("None Selected");
		
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
	public static SMTChartOptionIntfc getInstance(ChartType type, ProviderType prov, boolean full) {
		SMTChartOptionIntfc chart = null;
		
		switch(type) {
			case DONUT:
			case PIE:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GooglePieChartOptions(full);
				break;
			case BAR:
			case COLUMN:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GoogleColumnChartOptions(full);
				break;
			case TABLE:
				if (ProviderType.GOOGLE.equals(prov)) chart = new GoogleTableChartOptions(full);
				break;
			case LINE:
			case AREA: 
				if (ProviderType.GOOGLE.equals(prov)) chart = new GoogleLineChartOptions(full);
			default:
				break;
		}
		
		if (chart == null) chart = new SMTChartOptions();
		
		return chart;
	}
}

