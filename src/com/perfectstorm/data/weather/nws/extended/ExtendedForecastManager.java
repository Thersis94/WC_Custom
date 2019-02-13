package com.perfectstorm.data.weather.nws.extended;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: NWSForecastAPIManager.java
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

public class ExtendedForecastManager {
	private static Logger log = Logger.getLogger(ExtendedForecastManager.class);
	private static final String NWS_URL = "https://api.weather.gov/points/%f,%f/forecast";

	/**
	 * 
	 */
	public ExtendedForecastManager() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ExtendedForecastManager nws = new ExtendedForecastManager();
		BasicConfigurator.configure();
		log.info("Starting ...");
		
		nws.getForecast(39.8595789, -104.9447375);
	}
	
	/**
	 * Retrieve the extended forecast data using a GeocodeLocation
	 * 
	 * @param loc
	 * @return
	 * @throws IOException
	 */
	public ExtendedForecastVO getForecast(GeocodeLocation loc) throws IOException {
		return getForecast(loc.getLatitude(), loc.getLongitude());
	}
	
	/**
	 * Retrieve the extended forecast data
	 * 
	 * @param lat
	 * @param lng
	 * @return
	 * @throws IOException
	 */
	public ExtendedForecastVO getForecast(double lat, double lng) throws IOException {
		String url = String.format(NWS_URL, lat, lng);
		log.info("URL: " + url);
		
		// Retrieve the forecast data for the given coordinates
		SMTHttpConnectionManager httpConn = new SMTHttpConnectionManager();
		httpConn.setFollowRedirects(true);
		byte[] data = httpConn.retrieveData(url);
		
		// Parse the data into an object
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
		ExtendedForecastVO forecast = gson.fromJson(new String(data), ExtendedForecastVO.class);
		log.info(forecast);
		
		return forecast;
	}
	
}

