package com.biomed.smarttrak.vo.grid;


/********************************************************************
 * <b>Title: </b>GoogleColumnChartOptions.java<br/>
 * <b>Description: </b>Provides the options for google column charts<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleColumnChartOptions extends GoogleBaseChartOptions {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 */
	public GoogleColumnChartOptions(boolean full) {
		super(full);
		
		this.createChartOptions();
	}
	
	/**
	 * Creates the attributes for the pie chart
	 */
	protected void createChartOptions() {
		super.createChartOptions("top");
	}

}