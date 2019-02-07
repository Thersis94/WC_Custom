package com.perfectstorm.data.weather;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: StationExtVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> //TODO Change Me
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 6, 2019
 * @updates:
 ****************************************************************************/

public class StationExtVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2441124554610768900L;
	private String id;
	private String type;
	private PropertyVO properties;
	
	
	/**
	 * 
	 */
	public StationExtVO() {
		super();
	}


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}


	/**
	 * @return the properties
	 */
	public PropertyVO getProperties() {
		return properties;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}


	/**
	 * @param properties the properties to set
	 */
	public void setProperties(PropertyVO properties) {
		this.properties = properties;
	}

}

