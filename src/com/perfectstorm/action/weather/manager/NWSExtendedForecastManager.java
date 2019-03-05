package com.perfectstorm.action.weather.manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfectstorm.data.weather.WeatherStationVO;
import com.perfectstorm.data.weather.forecast.ForecastVO;
import com.perfectstorm.data.weather.nws.extended.ExtendedForecastVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: NWSExtendedForecastManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Retrieves extended daily forecast data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class NWSExtendedForecastManager implements ForecastManagerInterface {
	private static Logger log = Logger.getLogger(NWSExtendedForecastManager.class);
	private static final String NWS_POINT_FORECAST_URL = "https://api.weather.gov/points/%f,%f/forecast";
	private static final String NWS_GRIDPOINT_FORECAST_URL = "https://api.weather.gov/gridpoints/%s/%d,%d/forecast";
	
	private double latitude;
	private double longitude;
	private WeatherStationVO station = null;

	public NWSExtendedForecastManager() {
		super();
	}
	
	/**
	 * Constructs an extended forecast manager with the required coordinates.
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public NWSExtendedForecastManager(double latitude, double longitude) {
		this();
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();

		NWSExtendedForecastManager nws = new NWSExtendedForecastManager();
		nws.setCoordinates(39.8595789, -104.9447375);
		nws.retrieveForecast();
	}
	
	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#retrieveForecast()
	 */
	public Map<String, ForecastVO> retrieveForecast() throws ActionException {
		Map<String, ForecastVO> forecast;
		
		try {
			ExtendedForecastVO extended = getForecast();
			forecast = processData(extended);
		} catch (IOException e) {
			throw new ActionException(e);
		}
		
		return forecast;
	}
	
	/**
	 * Processes the NWS data into our normalized beans.
	 * 
	 * @param extended
	 * @return
	 */
	private Map<String, ForecastVO> processData(ExtendedForecastVO extended) {
		Map<String, ForecastVO> forecast = new HashMap<>();
		
		
		
		return forecast;
	}

	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#setCoordinates(double, double)
	 */
	public void setCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#setWeatherStation(com.perfectstorm.data.weather.WeatherStationVO)
	 */
	@Override
	public void setWeatherStation(WeatherStationVO station) {
		this.station = station;
	}
	
	/**
	 * Retrieve the extended forecast data from the NWS.
	 * 
	 * @return
	 * @throws IOException
	 */
	private ExtendedForecastVO getForecast() throws IOException {
<<<<<<< HEAD
		String url = String.format(NWS_URL, latitude, longitude);
		log.debug("Extended URL: " + url);
=======
		String url;
		
		if (station == null) {
			url = String.format(NWS_POINT_FORECAST_URL, latitude, longitude);
		} else {
			url = String.format(NWS_GRIDPOINT_FORECAST_URL, station.getForecastOfficeCode(), station.getForecastGridXNo(), station.getForecastGridYNo());
		}
		log.info("Extended Forecast URL: " + url);
>>>>>>> 7a5f114ca82f35caeeaac7fa8c82d3274919d9fb
		
		// Retrieve the forecast data for the given coordinates
		SMTHttpConnectionManager httpConn = new SMTHttpConnectionManager();
		httpConn.setFollowRedirects(true);
		byte[] data = httpConn.retrieveData(url);
		
		// Parse the data into an object
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
<<<<<<< HEAD
		ExtendedForecastVO forecast = gson.fromJson(new String(data), ExtendedForecastVO.class);
		log.debug("Extended Detail: " + forecast);
		
		return forecast;
=======
		return gson.fromJson(new String(data), ExtendedForecastVO.class);
>>>>>>> 7a5f114ca82f35caeeaac7fa8c82d3274919d9fb
	}
}

