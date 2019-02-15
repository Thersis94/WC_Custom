package com.perfectstorm.action.weather;

import java.time.LocalDateTime;
import java.util.Map;

import com.perfectstorm.action.weather.manager.ForecastManagerFactory;
import com.perfectstorm.action.weather.manager.ForecastManagerFactory.ForecastManager;
import com.perfectstorm.action.weather.manager.ForecastManagerInterface;
import com.perfectstorm.data.VenueVO;
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
	
	private double latitude;
	private double longitude;
	private Map<String, ForecastVO> detailForecast;
	private Map<String, ForecastVO> extendedForecast;
	
	/**
	 * Creates a new forecast manager for the given venue
	 * 
	 * @param venueId
	 * @throws ActionException 
	 */
	public VenueForecastManager(VenueVO venue) throws ActionException {
		this.latitude = venue.getLatitude();
		this.longitude = venue.getLongitude();

		ForecastManagerInterface fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_DETAILED, latitude, longitude);
		detailForecast = fmi.retrieveForecast();

		fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_EXTENDED, latitude, longitude);
		extendedForecast = fmi.retrieveForecast();
	}
	
	/**
	 * Main method for testing the API and parser
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		VenueVO venue = new VenueVO();
		venue.setVenueId("TEST");
		venue.setLatitude(39.8595789);
		venue.setLongitude(-104.9447375);
		
		new VenueForecastManager(venue);
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
	 * Returns the detailed forecast for the designated date/time period.
	 * 
	 * @param date
	 * @return
	 */
	public ForecastVO getDetailForecast(LocalDateTime date) {
		return detailForecast.get(getTimeKey(date));
	}

	/**
	 * Returns the extended forecast for the designated date/time period.
	 * 
	 * @param date
	 * @return
	 */
	public ForecastVO getExtendedForecast(LocalDateTime date) {
		return extendedForecast.get(getTimeKey(date));
	}
	
	/**
	 * Returns a key from a date/time that will get the requested forecast
	 * 
	 * @param date
	 * @return
	 */
	public String getTimeKey(LocalDateTime date) {
		return date.getDayOfMonth() + "_" + date.getHour();
	}
	
	/**
	 * Standardizes the units of measure used by the app
	 * 
	 * @param origValue
	 * @param unitOfMeasure
	 * @return
	 */
	public static double normalizeByUnitOfMeasure(double origValue, String unitOfMeasure) {
		double convertedValue = origValue;
		
		// Convert based on the submitted unit of measure. If the unit of measure
		// is not found below, the value is assumed to already be in the expected units.
		switch (unitOfMeasure) {
			case "degC": // Convert celsius to farenheit
				convertedValue = (origValue * 1.8) + 32;
				break;
			case "m": // Convert meters to feet
				convertedValue = origValue * 3.2808;
				break;
			default:
		}
		
		return convertedValue;
	}
	
	/**
	 * Refreshes the cached weather forecast data for a given venue
	 */
	private void refreshCache() {
		// TODO: implement method, account for multi-server
	}
}
