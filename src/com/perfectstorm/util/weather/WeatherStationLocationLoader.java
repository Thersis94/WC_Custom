package com.perfectstorm.util.weather;

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
	private static Logger log = Logger.getLogger(WeatherStationLocationLoader.class);
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
		
		log.info("Complete");
	}
	
	/**
	 * Grabs the records to be updated and geocodes then updates the DB
	 * @throws SQLException
	 */
	public void processRecords() throws SQLException {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select weather_station_cd, latitude_no, longitude_no from ");
		sql.append("custom.ps_weather_station where city_nm is null and zip_cd is null ");
		sql.append("and weather_station_cd not like 'C%' ");
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				String id = rs.getString(1);
				double lat = rs.getDouble(2);
				double lng = rs.getDouble(3);
				
				try {
					GeocodeLocation loc = geocode(lat, lng);
					updateStation(id, loc);
				} catch(Exception e) {
					log.error("Unable to retrieve location: " + id, e);
				}
			}
		}
	
	}
	
	/**
	 * Updates the info in the db
	 * @param id
	 * @param gl
	 * @throws SQLException
	 */
	public void updateStation(String id, GeocodeLocation gl) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("update custom.ps_weather_station set city_nm = ?, zip_cd = ? ");
		sql.append("where weather_station_cd = ? ");
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ps.setString(1, gl.getCity());
			ps.setString(2, gl.getZipCode());
			ps.setString(3, id);
			ps.executeUpdate();
		}
	}
	
	/**
	 * Performs a reverse geocode on the lat long
	 * @param lat
	 * @param lng
	 * @return
	 * @throws IOException
	 */
	public GeocodeLocation geocode(double lat, double lng) throws IOException {
		String url = String.format(MQ_URL, MQ_KEY, lat, lng);
		SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
		byte[] data = hConn.retrieveData(url);
		
		// Parse the response and convert to a GeocodeLocation Object
		Gson g = new Gson();
		GeocodeVO gvo = g.fromJson(new String(data), GeocodeVO.class);
		return gvo.getGeocodeLocation();
	}

}

