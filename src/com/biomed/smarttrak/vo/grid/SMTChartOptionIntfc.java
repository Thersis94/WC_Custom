package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.io.Serializable;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;

/********************************************************************
 * <b>Title: </b>SMTChartOptionIntfc.java<br/>
 * <b>Description: </b>Interface for concrete implementations that provide 
 * configuration options for the charting services<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public interface SMTChartOptionIntfc extends Serializable {
	
	/**
	 * Provides the configuration information for the overall chart.
	 * @return Object containing config.  
	 */
	public Map<String,Object> getChartOptions();
	
	/**
	 * Provides the configuration information for each row in the data grid.
	 * @return Object containing config.  
	 */
	public Map<String,Object> getRowOptions();
	
	/**
	 * Assigns options to each individual row
	 * @param detail
	 */
	public void addRowOptions(GridDetailVO detail);
	
	
	/**
	 * Assigns options to each individual cell
	 * @param detail
	 */
	public void addCellOptions(GridDetailVO detail);
	
	/**
	 * Provides the configuration information for each cell.
	 * @return Object containing config.  
	 */
	public Map<String,Object> getCellOptions();
	
	/**
	 * Adds the grid data to the option classes.  This allows data specific
	 * options to be set (such as titles, etc ..)
	 * @param grid
	 */
	public void addOptionsFromGridData(GridVO grid);
}

