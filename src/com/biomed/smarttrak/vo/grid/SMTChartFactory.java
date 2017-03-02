package com.biomed.smarttrak.vo.grid;

// App Libs
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory.ChartType;

/********************************************************************
 * <b>Title: </b>SMTChartFactory.java<br/>
 * <b>Description: </b>Factory class to switch between chart provider types<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class SMTChartFactory {
	
	/**
	 * Enum listing all of the different chart providers
	 */
	public enum ProviderType {
		GOOGLE("Google Charts");
		
		// Assign the name (get and set)
		private final String name;
		private ProviderType(String name) {	this.name = name; }
		public String getName() { return name; }
	}
	
	/**
	 * Creates the Grid Interface concrete class
	 * @param prov Provider Type
	 * @param grid GridVO Data
	 * @return
	 */
	public static SMTGridIntfc getInstance(ProviderType prov, GridVO grid, ChartType type) {
		
		return new GoogleChartVO(grid, type);
	}
	
	
}

