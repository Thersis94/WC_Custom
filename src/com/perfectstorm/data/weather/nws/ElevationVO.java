package com.perfectstorm.data.weather.nws;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ElevationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO For the elevation data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 6, 2019
 * @updates:
 ****************************************************************************/

public class ElevationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8236302993936521033L;
	private String unitCode;
	private double value;
	
	/**
	 * 
	 */
	public ElevationVO() {
		super();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public int getElevationInFeet() {
		
		return (int)Math.floor(value * 3.28084);
	}

	/**
	 * @return the unitCode
	 */
	public String getUnitCode() {
		return unitCode;
	}

	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @param unitCode the unitCode to set
	 */
	public void setUnitCode(String unitCode) {
		this.unitCode = unitCode;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(double value) {
		this.value = value;
	}

}

