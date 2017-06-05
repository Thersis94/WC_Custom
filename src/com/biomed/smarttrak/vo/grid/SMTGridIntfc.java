package com.biomed.smarttrak.vo.grid;

import java.util.List;
import java.util.Map;

/********************************************************************
 * <b>Title: </b>SMTGridIntfc.java<br/>
 * <b>Description: </b>Defines the Interface for the various charting engines.  
 * Each concrete class will implement and structure the data to match 
 * the json format required by that javascript charting engine<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public interface SMTGridIntfc {

	/**
	 * Adds a column to the data set
	 * @param column
	 */
	public void addColumn(SMTGridColumnIntfc column);
	
	/**
	 * Adds a set of row data to the chart
	 * @param row
	 */
	public void addRow(SMTGridRowIntfc row);
	
	/**
	 * @param p the p to set
	 */
	public void addCustomValue(String key, Object value);

	/**
	 * @param p the p to set
	 */
	public void addCustomValues(Map<String, Object> values);
	
	/**
	 * @return the cols
	 */
	public List<SMTGridColumnIntfc> getCols();

	/**
	 * @return the rows
	 */
	public List<SMTGridRowIntfc> getRows();

	/**
	 * @return the p
	 */
	public Map<String, Object> getCustomValue();
	
}

