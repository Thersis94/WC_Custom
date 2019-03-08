package com.perfectstorm.action.weather;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.perfectstorm.action.weather.manager.ForecastManagerFactory;
import com.perfectstorm.action.weather.manager.ForecastManagerFactory.ForecastManager;
import com.perfectstorm.action.weather.manager.ForecastManagerInterface;
import com.perfectstorm.data.VenueVO;
import com.perfectstorm.data.weather.WeatherStationVO;
import com.perfectstorm.data.weather.forecast.ForecastVO;
import com.perfectstorm.data.weather.forecast.FullForecastVO;
import com.siliconmtn.action.ActionException;
import com.smt.sitebuilder.util.CacheAdministrator;

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
	
	private static Logger log = Logger.getLogger(VenueForecastManager.class);
	
	private static final String DETAIL_CACHE_KEY_PREFIX = "DETAIL_FORECAST_";
	private static final String EXTENDED_CACHE_KEY_PREFIX = "EXTENDED_FORECAST_";
	
	private VenueVO venue;
	private Map<String, ForecastVO> detailForecast;
	private Map<String, ForecastVO> extendedForecast;
	
	/**
	 * Creates a new forecast manager for the given venue
	 * 
	 * @param venueId
	 * @throws ActionException 
	 */
	public VenueForecastManager(VenueVO venue, Map<String, Object> attributes) throws ActionException {
		this.venue = venue;
		retrieveForecast(attributes);
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
			case "degC": // Convert celsius to fahrenheit
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
	 * Retrieves the forecast data, from cache if available or from the weather service.
	 * 
	 * @param attributes
	 * @throws ActionException
	 */
	private void retrieveForecast(Map<String, Object> attributes) throws ActionException {
		CacheAdministrator cache = new CacheAdministrator(attributes);
		
		FullForecastVO fullDetail = (FullForecastVO) cache.readObjectFromCache(DETAIL_CACHE_KEY_PREFIX + venue.getVenueId());
		FullForecastVO fullExtended = (FullForecastVO) cache.readObjectFromCache(EXTENDED_CACHE_KEY_PREFIX + venue.getVenueId());
		
		if (fullDetail == null || fullDetail.getHoursSinceForecast() > 1) {
			refreshCache(cache, DETAIL_CACHE_KEY_PREFIX);
		} else {
			detailForecast = fullDetail.getFullForecast();
		}
		
		if (fullExtended == null || fullExtended.getHoursSinceForecast() > 1) {
			refreshCache(cache, EXTENDED_CACHE_KEY_PREFIX);
		} else {
			extendedForecast = fullExtended.getFullForecast();
		}
	}
	
	/**
	 * Refreshes the cached weather forecast data for a given venue.
	 * TODO: Account for multiple servers.
	 * 
	 * @param cache
	 * @param type
	 * @throws ActionException
	 */
	private void refreshCache(CacheAdministrator cache, String type) throws ActionException {
		log.debug("refreshing forecast: " + type);
		
		if (venue.getWeatherStations().isEmpty()) {
			throw new ActionException("can't get weather data without a weather station");
		}
		
		ForecastManagerInterface fmi;
		Map<String, ForecastVO> forecastData = new HashMap<>();
		WeatherStationVO station = venue.getWeatherStations().get(0);
		
		switch (type) {
			case DETAIL_CACHE_KEY_PREFIX:
				fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_DETAILED, station);
				detailForecast = forecastData = fmi.retrieveForecast();
				break;
			case EXTENDED_CACHE_KEY_PREFIX:
				fmi = ForecastManagerFactory.getManager(ForecastManager.NWS_EXTENDED, station);
				extendedForecast = forecastData = fmi.retrieveForecast();
				break;
			default:
		}

		// Weather data should be cached for 1 hour
		FullForecastVO fullForecast = new FullForecastVO();
		fullForecast.setFullForecast(forecastData);
		fullForecast.setForecastDate(new Date());
		cache.writeToCache(type + venue.getVenueId(), fullForecast, 3600);
	}
}
