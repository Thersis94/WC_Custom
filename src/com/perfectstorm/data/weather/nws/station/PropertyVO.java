package com.perfectstorm.data.weather.nws.station;


import com.perfectstorm.data.weather.nws.ElevationVO;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: PropertyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Properties of the feed
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 6, 2019
 * @updates:
 ****************************************************************************/

public class PropertyVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3820209197543816373L;
	private ElevationVO elevation;
	private String stationIdentifier;
	private String name;
	private String timeZone;

	/**
	 * 
	 */
	public PropertyVO() {
		super();
	}

	/**
	 * @return the elevation
	 */
	public ElevationVO getElevation() {
		return elevation;
	}

	/**
	 * @return the stationIdentifier
	 */
	public String getStationIdentifier() {
		return stationIdentifier;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the timeZone
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(ElevationVO elevation) {
		this.elevation = elevation;
	}

	/**
	 * @param stationIdentifier the stationIdentifier to set
	 */
	public void setStationIdentifier(String stationIdentifier) {
		this.stationIdentifier = stationIdentifier;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param timeZone the timeZone to set
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

}

