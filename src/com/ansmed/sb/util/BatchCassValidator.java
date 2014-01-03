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
import com.siliconmtn.address.AbstractAddressFormatter;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.gis.Location;

/*****************************************************************************
 <p><b>Title</b>: BatchCassValidator.java</p>
 <p>Description: <b/>Performs CASS Validation on the address</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 4, 2007
 Last Updated:
 ***************************************************************************/

public class BatchCassValidator {
	String geoUrl = null;
	Logger log = null;
	
	/**
	 * 
	 */
	public BatchCassValidator() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BatchCassValidator bg = new BatchCassValidator();
		
		// Get the Config
		Properties p = bg.loadConfiguration("scripts/ans_config.properties");
		bg.geoUrl = p.getProperty("geoUrl");
		
		bg.log = Logger.getLogger(BatchCassValidator.class);
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		// Connect to the database
		Connection conn = null;
		DatabaseConnection dbc = new DatabaseConnection(p.getProperty("dbDriver"),p.getProperty("dbUrl"),p.getProperty("dbUser"),p.getProperty("dbPassword"));
		bg.log.info("Starting Batch geocode");
		try {
			conn = dbc.getConnection();
			bg.validate(conn);
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
	 * Gets the data from the db and geocodes each record
	 * @param conn
	 */
	public void validate(Connection conn) {
		String sql = "select * from WebCrescendo_custom.dbo.ans_clinic ";
		sql += "where cass_validate_flg is null";
		
		String cName = "com.siliconmtn.address.DotsAddressFormatter";
		AbstractAddressFormatter aaf = AbstractAddressFormatter.getInstance(cName);
		
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
				
				// validate the record
				try {
					Location cassLoc = aaf.checkAddress(loc);
					if (cassLoc.isValidAddress()) loc = cassLoc;
				} catch(Exception e) {}	

				
				// Update the record
				if (loc.isCassValidated()) 
					updateRecord(conn, loc, rs.getString("clinic_id"));
				
				// Pause for .5 seconds
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates a single location with the CASS validation info
	 * @param conn
	 * @param gl
	 * @param clinicId
	 */
	public void updateRecord(Connection conn, Location gl, String clinicId) {
		log.info("clinic ID: " + clinicId);
		StringBuffer sql = new StringBuffer();
		sql.append("update WebCrescendo.dbo.ans_clinic set address_txt = ?, ");
		sql.append("city_nm = ?, state_cd = ?, zip_cd = ?, cass_validate_flg = ? ");
		sql.append("where clinic_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, gl.getAddress());
			ps.setString(2, gl.getCity());
			ps.setString(3, gl.getState());
			ps.setString(4, gl.getZipCode());
			ps.setInt(5, gl.getCassValidated());
			ps.setString(6, clinicId);
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
