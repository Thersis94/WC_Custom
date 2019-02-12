package com.perfectstorm.data.weather.detail;

// Gson 2.3
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: WeatherGovManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the calls to the Weather.gov detail
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class WeatherGovManager {

	/**
	 * 
	 */
	public WeatherGovManager() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String url = "https://api.weather.gov/gridpoints/TOP/31,80";
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveData(url);
		
        Gson g = new GsonBuilder().create();
		WeatherDetailVO wvo = g.fromJson(new String(data), WeatherDetailVO.class);
		System.out.println("Detail: " + wvo.getProperties().getTemperature());
		
	}

}

