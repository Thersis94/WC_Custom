package com.perfectstorm.action.weather.manager;

import java.util.Map;

import com.perfectstorm.data.weather.WeatherStationVO;
import com.perfectstorm.data.weather.forecast.ForecastVO;
import com.siliconmtn.action.ActionException;

/****************************************************************************
 * <b>Title:</b> ForecastManagerInterface.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Interface that defines requirements of a weather forecast manager.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 14 2019
 * @updates:
 ****************************************************************************/
public interface ForecastManagerInterface {
	
	/**
	 * Retrieves weather forecast data from an API.
	 * 
	 * @return
	 * @throws ActionException 
	 */
	public abstract Map<String, ForecastVO> retrieveForecast() throws ActionException;
	
	/**
	 * Sets the coordinates to retrieve the weather data from.
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public abstract void setCoordinates(double latitude, double longitude);
	
	/**
	 * Sets a weather station to retrieve weather data from.
	 * 
	 * @param station
	 */
	public abstract void setWeatherStation(WeatherStationVO station);
	
}
