package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/********************************************************************
 * <b>Title: </b>GoogleChartCellVO.java<br/>
 * <b>Description: </b>Data element for a single cell of data in Google Charts<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartCellVO implements Serializable, SMTGridCellIntfc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Cell Value. The data type should match the column data type. If the cell is null, 
	 * the v property should be null, though it can still have f and p properties.
	 */
	private Object v;
	
	/**
	 * A string version of the v value, formatted for display
	 */
	private String f;
	
	/**
	 * An object that is a map of custom values applied to the cell.
	 */
	private Map<String, Object>  p;
	
	/**
	 * 
	 */
	public GoogleChartCellVO() {
		super();
		
		p = new LinkedHashMap<>(16);
	}

	/**
	 * @return the v
	 */
	public Object getValue() {
		return v;
	}

	/**
	 * @return the f
	 */
	public String getFormat() {
		return f;
	}

	/**
	 * @return the p
	 */
	public Map<String, Object> getP() {
		return p;
	}

	/**
	 * @param v the v to set
	 */
	public void setValue(Object v) {
		this.v = v;
	}

	/**
	 * @param f the f to set
	 */
	public void setFormat(String f) {
		this.f = f;
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void addCustomValue(String key, Object value) {
		p.put(key, value);
	}

}

