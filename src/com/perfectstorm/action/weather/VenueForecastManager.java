package com.perfectstorm.action.weather;

import java.util.Map;

import com.perfectstorm.data.weather.forecast.ForecastVO;

/****************************************************************************
 * <b>Title:</b> VenueForecastManager.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 13 2019
 * @updates:
 ****************************************************************************/

public class VenueForecastManager {
	
	private String venueId;
	private double latitude;
	private double longitude;
	private Map<String, ForecastVO> detailForecast;
	private Map<String, ForecastVO> extendedForecast;

	/**
	 * Creates a new forecast manager for the given venue
	 * 
	 * @param venueId
	 */
	public VenueForecastManager(String venueId) {
		this.venueId = venueId;
		
		// Retrieve/set the venue data
		//this.latitude = 
		//this.longitude = 
		
		// TODO: Retrieve the weather data from cache or weather service
	}
	
	/**
	 * Returns the full detailed forecast (either cached or from the weather service)
	 * 
	 * @return
	 */
	public Map<String, ForecastVO> getFullDetailForecast() {
		return detailForecast;
	}

	/**
	 * Returns the full extended forecast (either cached or from the weather service)
	 * 
	 * @return
	 */
	public Map<String, ForecastVO> getFullExtendedForecast() {
		return extendedForecast;
	}

	/**
	 * Returns the detailed forecast for the designated time period denoted by the key.
	 * 
	 * @param timeKey
	 * @return
	 */
	public ForecastVO getDetailForecast(String timeKey) {
		return detailForecast.get(timeKey);
	}

	/**
	 * Returns the extended forecast for the designated time period denoted by the key.
	 * 
	 * @param timeKey
	 * @return
	 */
	public ForecastVO getExtendedForecast(String timeKey) {
		return extendedForecast.get(timeKey);
	}
}
