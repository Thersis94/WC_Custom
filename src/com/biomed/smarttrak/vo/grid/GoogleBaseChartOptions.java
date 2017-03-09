package com.biomed.smarttrak.vo.grid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
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
	
	/**
	 * Creates the main attributes shared across most charts
	 * @param position
	 */
	protected void createChartOptions(String position) {
		// Turn off the legend if not full
		if (! full) position = "none";
		
		// Set the legend font
		Map<String, Object> text = new HashMap<>();
		text.put("color", "blue");
		text.put("fontSize", 12);
		
		// Define the legend
		Map<String, Object> legend = new HashMap<>();
		legend.put("position", position);
		legend.put("textStyle", text);
		legend.put("maxLines", 3);
		
		// Define the area for the actual chart
		Map<String, Object> chartArea = new HashMap<>();
		legend.put("top", 50);
		legend.put("width", "96%");
		legend.put("height", "98%");

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
		text.put("fontSize", 16);
		
		final String TITLE_LABEL = "title";
		if (full) {
			chart.put(TITLE_LABEL, grid.getTitle());
			chart.put("titleTextStyle", text);
		}

		// Add vAxis Labels
		Map<String, Object> vAxis = new LinkedHashMap<>();
		vAxis.put(TITLE_LABEL, grid.getPrimaryYTitle());
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

