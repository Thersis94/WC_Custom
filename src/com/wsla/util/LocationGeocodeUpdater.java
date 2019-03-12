package com.wsla.util;

// JDK 1.8.x
import java.sql.Connection;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: LocationGeocodeUpdater.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Geocodes the provider locations that did not get geocoded
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 12, 2019
 * @updates:
 ****************************************************************************/

public class LocationGeocodeUpdater {
	public static final String GEOCODER_CLASS = "com.siliconmtn.gis.SMTGeocoder";
	public static final String GEOCODER_URL = "http://localhost:8080/websvc/geocoder";
	
	/**
	 * 
	 */
	public LocationGeocodeUpdater() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("javax.sql.DataSource");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("*** Update This ***");
		Connection conn = dc.getConnection();
		System.out.println("Started");
		List<ProviderLocationVO> locations = getLocations(conn);
		System.out.println("number of locations: " + locations.size());
		geocodeLocations(locations);
		System.out.println("\n--------------------------------------------------");
		System.out.println("Completed");
	}
	
	/**
	 * Geocodes the locations and writes the log to the console
	 * @param locations
	 */
	public static void geocodeLocations(List<ProviderLocationVO> locations) {
		AbstractGeocoder ag = GeocodeFactory.getInstance(GEOCODER_CLASS);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, GEOCODER_URL);
		ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);
		System.out.println("--------------------------------------------------\n");
		for (Location l : locations) {
			GeocodeLocation gl = ag.geocodeLocation(l).get(0);
			StringBuilder upd = new StringBuilder();
			upd.append("update custom.wsla_provider_location set ");
			upd.append("latitude_no = %f, longitude_no = %f, match_cd = '%s', ");
			upd.append("manual_geocode_flg = 0 where location_id = '%s';");
			
			System.out.println(String.format(upd.toString(), gl.getLatitude(), gl.getLongitude(), gl.getMatchCode(), l.getLocationId()));
		}
	}
	
	/**
	 * gets the locations from the database where the geocode didn't occur
	 * @param conn
	 * @return
	 */
	private static List<ProviderLocationVO> getLocations(Connection conn) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from custom.wsla_provider_location ");
		sql.append("where latitude_no is null or latitude_no = 0 ");
		sql.append("or longitude_no is null or longitude_no = 0 or match_cd = 'noMatch' ");
		
		DBProcessor db = new DBProcessor(conn);
		return db.executeSelect(sql.toString(), null, new ProviderLocationVO());
	}

}

