package com.perfectstorm.action.weather;

import java.util.Map;

import com.perfectstorm.action.weather.manager.ForecastManagerFactory;
import com.perfectstorm.action.weather.manager.ForecastManagerFactory.ForecastManager;
import com.perfectstorm.action.weather.manager.ForecastManagerInterface;
import com.perfectstorm.data.weather.forecast.ForecastVO;
import com.siliconmtn.action.ActionException;

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
	
	public static void main(String[] args) throws Exception {
		VenueForecastManager vfm = new VenueForecastManager("TEST");
	}

	/**
	 * Creates a new forecast manager for the given venue
	 * 
	 * @param venueId
	 * @throws ActionException 
	 */
	public VenueForecastManager(String venueId) throws ActionException {
		this.venueId = venueId;
		
		// TODO: Get the actual coordinates of the venue, this is temporary
		this.latitude = 39.8595789;
		this.longitude = -104.9447375;

		ForecastManagerInterface fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_DETAILED, latitude, longitude);
		detailForecast = fmi.retrieveForecast();

		fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_EXTENDED, latitude, longitude);
		extendedForecast = fmi.retrieveForecast();
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
	
	/**
	 * Refreshes the cached weather forecast data for a given venue
	 */
	private void refreshCache() {
		// TODO: implement method, account for multi-server
	}
}
