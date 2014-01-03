package com.ansmed.sb.util;

// JDK 1.5.0
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

// Log4J 1.2.8
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs 2.0
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;

/*****************************************************************************
 <p><b>Title</b>: BatchGeocoder.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 4, 2007
 Last Updated:
 ***************************************************************************/

public class BatchGeocoder {
	String geoUrl = null;
	String geoClass = null;
	Logger log = null;
	
	/**
	 * 
	 */
	public BatchGeocoder() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BatchGeocoder bg = new BatchGeocoder();
		
		// Get the Config
		Properties p = bg.loadConfiguration("scripts/ans_config.properties");
		bg.geoUrl = p.getProperty("geoUrl");
		bg.geoClass = p.getProperty("geoClass");
		
		bg.log = Logger.getLogger(BatchGeocoder.class);
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		// Connect to the database
		Connection conn = null;
		DatabaseConnection dbc = new DatabaseConnection(p.getProperty("dbDriver"),p.getProperty("dbUrl"),p.getProperty("dbUser"),p.getProperty("dbPassword"));
		bg.log.info("Starting Batch geocode");
		try {
			conn = dbc.getConnection();
			bg.geocode(conn);
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
		
		bg.log.info("Batch geocode complete");
	}
	
	/**
	 * Gets the data fromt he db and gecodes each record
	 * @param conn
	 */
	public void geocode(Connection conn) {
		String sql = "select * from Sitebuilder_ans.dbo.ans_clinic where geo_match_cd is null";
		AbstractGeocoder gc = GeocodeFactory.getInstance(geoClass);
		gc.addAttribute(AbstractGeocoder.CONNECT_URL,geoUrl);
		
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(sql);
			while(rs.next()) {
				// Get the address info
				Location loc = new Location();
				loc.setAddress(rs.getString("address_txt"));
				loc.setCity(rs.getString("city_nm"));
				loc.setState(rs.getString("state_cd"));
				loc.setZipCode(rs.getString("zip_cd"));
				log.debug("Loc: " + loc);
				
				// Geocode the record
				GeocodeLocation gLoc = null;

				try {
					gLoc = gc.geocodeLocation(loc).get(0);
				} catch(Exception e) {
					log.error("Error Geocoding " + loc, e);
				}
				
				// Update the record
				if (gLoc != null && gLoc.getLatitude() > 0) 
					updateRecord(conn, gLoc, rs.getString("clinic_id"));
				
				// Pause for 5 seconds
				Thread.sleep(3000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates a single location with the geocode info
	 * @param conn
	 * @param gl
	 * @param clinicId
	 */
	public void updateRecord(Connection conn, GeocodeLocation gl, String clinicId) {
		log.debug("clinic ID: " + clinicId);
		StringBuffer sql = new StringBuffer();
		sql.append("update Sitebuilder_ans.dbo.ans_clinic set latitude_no = ?, ");
		sql.append("longitude_no = ?, geo_match_cd = ? where clinic_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDouble(1, gl.getLatitude());
			ps.setDouble(2, gl.getLongitude());
			ps.setString(3, gl.getMatchCode().toString());
			ps.setString(4, clinicId);
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	/**
	 * Loads the config properties to be used in the app
	 * @param path
	 * @return
	 */
	public Properties loadConfiguration(String path) {
		Properties config = new Properties();
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(path);
			config.load(inStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {}
			}
		}
		
		return config;
	}

}
