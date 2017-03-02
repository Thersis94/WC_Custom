package com.biomed.smarttrak.vo.grid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.GridVO;

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
	 * 
	 */
	public GoogleBaseChartOptions() {
		super();
		chart = new LinkedHashMap<>();
		row = new LinkedHashMap<>();
		cell = new LinkedHashMap<>();
	}
	
	/**
	 * Creates the main attributes shared across most charts
	 * @param position
	 */
	protected void createChartOptions(String position) {
		Map<String, Object> text = new HashMap<>();
		text.put("color", "blue");
		text.put("fontSize", 24);
		
		Map<String, Object> title = new HashMap<>();
		text.put("color", "blue");
		text.put("fontSize", 8);
		
		Map<String, Object> legend = new HashMap<>();
		legend.put("position", "top");
		legend.put("textStyle", text);
		legend.put("maxLines", 3);
		
		Map<String, Object> chartArea = new HashMap<>();
		legend.put("top", 50);
		legend.put("width", "96%");
		legend.put("height", "98%");

		chart.put("legend", legend); // none to hide
		chart.put("tooltip", " {text: 'value'}");
		chart.put("chartArea", chartArea);
		chart.put("titleTextStyle", title);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addGridData(com.biomed.smarttrak.admin.vo.GridVO)
	 */
	public void addOptionsFromGridData(GridVO grid) {
		final String TITLE_LABEL = "title";
		chart.put(TITLE_LABEL, grid.getTitle());
		
		Map<String, Object> vAxis = new LinkedHashMap<>();
		vAxis.put(TITLE_LABEL, grid.getPrimaryYTitle());
		chart.put("vAxis", vAxis);
		
		Map<String, Object> hAxis = new LinkedHashMap<>();
		hAxis.put(TITLE_LABEL, grid.getPrimaryXTitle());
		chart.put("hAxis", hAxis);
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
}

