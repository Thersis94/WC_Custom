package com.perfectstorm.data.weather.nws.extended;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: PeriodVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds forecast data for one period of time.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class PeriodVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8236302993936521033L;
	private String name;
	private String startTime;
	private String endTime;
	private boolean isDaytime;
	private int temperature;
	private String temperatureUnit;
	private String temperatureTrend;
	private String windSpeed;
	private String windDirection;
	private String shortForecast;
	private String detailedForecast;
	
	/**
	 * 
	 */
	public PeriodVO() {
		super();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the startTime
	 */
	public String getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public String getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the isDaytime
	 */
	public boolean isDaytime() {
		return isDaytime;
	}

	/**
	 * @param isDaytime the isDaytime to set
	 */
	public void setDaytime(boolean isDaytime) {
		this.isDaytime = isDaytime;
	}

	/**
	 * @return the temperature
	 */
	public int getTemperature() {
		return temperature;
	}

	/**
	 * @param temperature the temperature to set
	 */
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	/**
	 * @return the temperatureUnit
	 */
	public String getTemperatureUnit() {
		return temperatureUnit;
	}

	/**
	 * @param temperatureUnit the temperatureUnit to set
	 */
	public void setTemperatureUnit(String temperatureUnit) {
		this.temperatureUnit = temperatureUnit;
	}

	/**
	 * @return the temperatureTrend
	 */
	public String getTemperatureTrend() {
		return temperatureTrend;
	}

	/**
	 * @param temperatureTrend the temperatureTrend to set
	 */
	public void setTemperatureTrend(String temperatureTrend) {
		this.temperatureTrend = temperatureTrend;
	}

	/**
	 * @return the windSpeed
	 */
	public String getWindSpeed() {
		return windSpeed;
	}

	/**
	 * @param windSpeed the windSpeed to set
	 */
	public void setWindSpeed(String windSpeed) {
		this.windSpeed = windSpeed;
	}

	/**
	 * @return the windDirection
	 */
	public String getWindDirection() {
		return windDirection;
	}

	/**
	 * @param windDirection the windDirection to set
	 */
	public void setWindDirection(String windDirection) {
		this.windDirection = windDirection;
	}

	/**
	 * @return the shortForecast
	 */
	public String getShortForecast() {
		return shortForecast;
	}

	/**
	 * @param shortForecast the shortForecast to set
	 */
	public void setShortForecast(String shortForecast) {
		this.shortForecast = shortForecast;
	}

	/**
	 * @return the detailedForecast
	 */
	public String getDetailedForecast() {
		return detailedForecast;
	}

	/**
	 * @param detailedForecast the detailedForecast to set
	 */
	public void setDetailedForecast(String detailedForecast) {
		this.detailedForecast = detailedForecast;
	}
}

