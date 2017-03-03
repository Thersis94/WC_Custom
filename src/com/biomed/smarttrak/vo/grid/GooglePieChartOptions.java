package com.biomed.smarttrak.vo.grid;

// APP Libs
import com.biomed.smarttrak.admin.vo.GridVO;

/********************************************************************
 * <b>Title: </b>GooglePieChartOptions.java<br/>
 * <b>Description: </b>Extends the google base options for  Pie Charts<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GooglePieChartOptions extends GoogleBaseChartOptions {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public GooglePieChartOptions() {
		super();
		
		this.createChartOptions();
	}
	
	/**
	 * Creates the attributes for the pie chart
	 */
	protected void createChartOptions() {
		super.createChartOptions("labeled");

		chart.put("pieHole", .4);
		chart.put("sliceVisibilityThreshold", .05);
		chart.put("pieSliceText", "value");
		//chart.put("is3D", true);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.GoogleBaseChartOptions#addOptionsFromGridData(com.biomed.smarttrak.admin.vo.GridVO)
	 */
	public void addOptionsFromGridData(GridVO grid) {
		// Not used for now.  Still formatting
	}
}

