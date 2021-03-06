package com.biomed.smarttrak.vo.grid;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.biomed.smarttrak.vo.grid.GoogleChartVO.DataType;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>GoogleColumn.java<br/>
 * <b>Description: </b>Column definition for Google Charts and Grids<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartColumnVO implements Serializable, SMTGridColumnIntfc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Column unique identifier
	 */
	private String id;
	
	/**
	 * Column Label
	 */
	private String label;
	
	/**
	 * Chart options, styles and Javascript elements
	 */
	private Map<String, String> p;
	
	/**
	 * Role utilized by annotations
	 */
	private String role;
	
	/**
	 * String value for the enum DataType
	 */
	private String type;
	
	/**
	 * 
	 */
	public GoogleChartColumnVO() {
		super();
		
		p = new LinkedHashMap<>(16);
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
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the p
	 */
	public Map<String, String> getCustomValues() {
		return p;
	}

	/**
	 * @return the type
	 */
	public String getDataType() {
		return type;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @param p the p to set
	 */
	public void addCustomValue(String key, String value) {
		this.p.put(key, value);
	}

	/**
	 * @param type the type to set
	 */
	public void setDataType(String type) {
		this.type = type;
	}
	
	/**
	 * @param type the type to set as an enum
	 */
	public void setDataType(DataType dt) {
		this.type = dt.getName();
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}
}

