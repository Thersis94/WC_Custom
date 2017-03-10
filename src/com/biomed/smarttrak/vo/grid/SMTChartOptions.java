package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.util.LinkedHashMap;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.GridDetailVO;
// App Libs
import com.biomed.smarttrak.admin.vo.GridVO;

/********************************************************************
 * <b>Title: </b>SMTChartOptions.java<br/>
 * <b>Description: </b>Base default class for styles<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class SMTChartOptions implements SMTChartOptionIntfc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Data source for chart options
	 */
	private Map<String, Object> chart;
	
	/**
	 * Data source for row options
	 */
	private Map<String, Object> row;
	
	/**
	 * Data source for cell options
	 */
	private Map<String, Object> cell;

	/**
	 * 
	 */
	public SMTChartOptions() {
		super();
		chart = new LinkedHashMap<>();
		row = new LinkedHashMap<>();
		cell = new LinkedHashMap<>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addGridData(com.biomed.smarttrak.admin.vo.GridVO)
	 */
	public void addOptionsFromGridData(GridVO grid) {
		// Not needed right now.  We will once we set default formatting
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
		
		// Not needed right now.  We will once we set default formatting
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc#addCellOptions(com.biomed.smarttrak.admin.vo.GridDetailVO)
	 */
	@Override
	public void addCellOptions(GridDetailVO detail) {
		// Not needed right now.  We will once we set default formatting
	}
}

