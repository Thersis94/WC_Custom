package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.util.Map;

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
	
	
	private boolean full;

	/**
	 * 
	 */
	public GooglePieChartOptions(boolean full) {
		super(true);
		this.full = full;
		this.createChartOptions();
	}
	
	/**
	 * Creates the attributes for the pie chart
	 */
	@SuppressWarnings("unchecked")
	protected void createChartOptions() {
		super.createChartOptions("labeled");

		chart.put("pieHole", .4);
		chart.put("sliceVisibilityThreshold", .05);
		chart.put("pieSliceText", "percentage");
		
		if (! full) {
	 		Map<String, Object> legend = (Map<String, Object>) chart.get("legend");
			Map<String, Object> textStyle = (Map<String, Object>) legend.get("textStyle");
			textStyle.put("fontSize", DEFAULT_LEGEND_FONT_SIZE);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.GoogleBaseChartOptions#addOptionsFromGridData(com.biomed.smarttrak.admin.vo.GridVO)
	 */
	public void addOptionsFromGridData(GridVO grid) {
		if(full) super.addOptionsFromGridData(grid);
	}
}

