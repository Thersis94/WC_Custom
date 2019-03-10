package com.perfectstorm.data.weather.nws.extended;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ForecastVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data elements for an extended daily forecast.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class ExtendedForecastVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9091722028240674656L;
	private String type;
	private PropertyVO properties;
	
	
	/**
	 * 
	 */
	public ExtendedForecastVO() {
		super();
	}


	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}


	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}


	/**
	 * @return the properties
	 */
	public PropertyVO getProperties() {
		return properties;
	}


	/**
	 * @param properties the properties to set
	 */
	public void setProperties(PropertyVO properties) {
		this.properties = properties;
	}
}

