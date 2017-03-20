package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// App Libs
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;

// SMT Base Libs
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>SMTChartOptions.java<br/>
 * <b>Description: </b><<<< Some Desc Goes Here >>>><br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleBaseChartOptions implements SMTChartOptionIntfc {
	
	/**
	 * Defines the default font size for the legend text
	 */
	public static final int DEFAULT_LEGEND_FONT_SIZE = 8;
	
	/**
	 * Defines the font size for the full image legend text
	 */
	public static final int FULL_LEGEND_FONT_SIZE = 16;
	
	/**
	 * Defines the font size for the title text
	 */
	public static final int TITLE_FONT_SIZE = 20;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Data source for chart options
	 */
	protected Map<String, Object> chart;
	
	/**
	 * Data source for row options
	 */
	protected Map<String, Object> row;
	
	/**
	 * Data source for cell options
	 */
	protected Map<String, Object> cell;

	/**
	 * Parameter for distinguishing between a thumbnail and full view
	 */
	private boolean full = false;

	/**
	 * 
	 */
	public GoogleBaseChartOptions(boolean full) {
		super();
		this.full = full;
		chart = new LinkedHashMap<>();
		row = new LinkedHashMap<>();
		cell = new LinkedHashMap<>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, ", ");
	}
	
	/**
	 * Creates the main attributes shared across most charts
	 * @param position
	 */
	protected void createChartOptions(String pos) {
		// Turn off the legend if not full
		String position = pos;
		int fontSize = FULL_LEGEND_FONT_SIZE;
		if (! full) {
			position = "none";
			fontSize = DEFAULT_LEGEND_FONT_SIZE;
		}
		
		// Set the legend font
		Map<String, Object> text = new HashMap<>();
		text.put("color", "black");
		text.put("fontSize", fontSize);
		
		// Define the legend
		Map<String, Object> legend = new HashMap<>();
		legend.put("position", position);
		legend.put("textStyle", text);
		legend.put("maxLines", 3);
		legend.put("top", 50);
		
		// Define the area for the actual chart
		Map<String, Object> chartArea = new HashMap<>();
		chartArea.put("width", "85%");
		chartArea.put("height", "70%");
		
		chart.put("legend", legend); // none to hide
		chart.put("tooltip", " {text: 'value'}");
		chart.put("chartArea", chartArea);
		
		chart.put("colors", new String[]{ "#3366cc","#dc3912","#ff9900","#109618","#990099","#0099c6","#8f8f8f","#e53ac3","#f96125","#316395" });
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addGridData(com.biomed.smarttrak.admin.vo.GridVO)
	 */
	public void addOptionsFromGridData(GridVO grid) {
		// Set the font attributes
		Map<String, Object> text = new HashMap<>();
		text.put("color", "blue");
		text.put("fontSize", TITLE_FONT_SIZE);
		
		final String TITLE_LABEL = "title";
		if (full) {
			chart.put(TITLE_LABEL, grid.getTitle());
			chart.put("titleTextStyle", text);
		}

		// Add vAxis Labels
		Map<String, Object> vAxis = new LinkedHashMap<>();
		if (full) vAxis.put(TITLE_LABEL, grid.getPrimaryYTitle());
		vAxis.put("format", "short");
		vAxis.put("gridlines", 6);
		vAxis.put("scaleType", "linear");  // Also supports log
		chart.put("vAxis", vAxis);
		
		if(full) {	
			// Add the hAxis label with copyright
			Map<String, Object> hAxis = new LinkedHashMap<>();
			String label = String.format("\nCopyright© %s BioMedGPS, LLC", Convert.getCurrentYear());
			hAxis.put(TITLE_LABEL, StringUtil.checkVal(grid.getPrimaryXTitle()) + label);
			chart.put("hAxis", hAxis);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#getChartOptions()
	 */
	@Override
	public Map<String,Object> getChartOptions() {
		return chart;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#getRowOptions()
	 */
	@Override
	public Map<String,Object> getRowOptions() {
		return row;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#getCellOptions()
	 */
	@Override
	public Map<String,Object> getCellOptions() {
		return cell;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addRowOptions(com.biomed.smarttrak.admin.vo.GridDetailVO)
	 */
	@Override
	public void addRowOptions(GridDetailVO detail) {
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addCellOptions(com.biomed.smarttrak.admin.vo.GridDetailVO)
	 */
	@Override
	public void addCellOptions(GridDetailVO detail) {
		
	}
}

