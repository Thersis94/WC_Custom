package com.perfectstorm.action.weather;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.mapquest.GeocodeVO;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: WeatherStationLocationLoader.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> updates the weather stations with zip and city
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class WeatherStationLocationLoader {
	private Connection conn;
	private static Logger log = Logger.getLogger(StationLoader.class);
	private static final String MQ_KEY = "Y2XUmYMLp5RVoH3mneDVKP119LzqpFuA";
	private static final String MQ_URL = "http://open.mapquestapi.com/geocoding/v1/reverse?key=%s&location=%f,%f&includeRoadMetadata=true&includeNearestIntersection=true";
	/**
	 * 
	 */
	public WeatherStationLocationLoader() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		WeatherStationLocationLoader ll = new WeatherStationLocationLoader();
		BasicConfigurator.configure();
		log.info("Starting ...");
		
		// Get the DB Connection
		DatabaseConnection dc = new DatabaseConnection();
		dc.setPassword("sqll0gin");
		dc.setUserName("ryan_user_sb");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setDriverClass("org.postgresql.Driver");
		ll.conn = dc.getConnection();
		
		ll.processRecords();
	}
	
	
	public void processRecords() throws SQLException {
		
		String sql = "select weather_station_cd, latitude_no, longitude_no from custom.ps_weather_station where city_nm is null and weather_station_cd = 'KFLY'";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				String id = rs.getString(1);
				double lat = rs.getDouble(2);
				double lng = rs.getDouble(3);
				
				try {
					GeocodeLocation loc = geocode(lat, lng);
				} catch(Exception e) {
					log.error("Ubale to retrieve location", e);
				}
			}
		}
	
	}
	
	
	public GeocodeLocation geocode(double lat, double lng) throws IOException {
		String url = String.format(MQ_URL, MQ_KEY, lat, lng);
		log.info("URL: " + url);
		SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
		byte[] data = hConn.retrieveData(url);
		
		Gson g = new Gson();
		GeocodeVO results = g.fromJson(new String(data), GeocodeVO.class);
		log.info(results);
		return null;
	}

}

