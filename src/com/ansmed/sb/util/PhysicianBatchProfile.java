package com.ansmed.sb.util;

// JDK 1.6.0
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
 <p><b>Title</b>: PhysicianBatchProfile.java</p>
 <p>Description: <b/></p>Batches adding of the physician info to the
 profile system and updates the ans_surgeon table with the profile ID
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 10, 2007
 Last Updated:
 ***************************************************************************/

public class PhysicianBatchProfile {
	Logger log = null;
	String sbSchema = null;
	String ansSchema = null;
	String encryptKey = null;
	
	/**
	 * 
	 */
	public PhysicianBatchProfile() {
	}
	
	public static void main(String[] args) {
		PhysicianBatchProfile bp = new PhysicianBatchProfile();
		
		// Get the Config
		Properties p = bp.loadConfiguration("scripts/ans_config.properties");
		
		bp.log = Logger.getLogger(PhysicianBatchProfile.class);
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		// Connect to the database
		Connection conn = null;
		DatabaseConnection dbc = new DatabaseConnection(p.getProperty("dbDriver"),p.getProperty("dbUrl"),p.getProperty("dbUser"),p.getProperty("dbPassword"));
		bp.log.info("Starting Batch Profile");
		try {
			conn = dbc.getConnection();
			bp.sbSchema = p.getProperty("sbSchema");
			bp.ansSchema = p.getProperty("sbANSSchema");
			bp.encryptKey =  p.getProperty("encryptKey");
			bp.process(conn);
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Gets the sales rep info from the db and stores it to the profile table
	 * @param encKey
	 * @param schema
	 * @param conn
	 * @throws SQLException
	 */
	protected void process(Connection conn) 
	throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("select * ");
		sb.append("from ").append(ansSchema).append("ans_surgeon a ");
		sb.append("left outer join ").append(ansSchema).append("ANS_CLINIC b ");
		sb.append("on a.SURGEON_ID = b.SURGEON_ID ");
		sb.append("and PRIMARY_LOCATION_FLG = 1 ");
		sb.append("where a.profile_id is null ");
		
		PreparedStatement ps = conn.prepareStatement(sb.toString());
		ResultSet rs = ps.executeQuery();
		for (int i=0; rs.next(); i++) {
			try {
				insertProfile(conn, rs);
				if ((i % 100) == 0) log.info("Number of records updated: " + i);
			} catch(Exception e) {
				log.error("Error loading sales rep: " + rs.getString("sales_rep_id"), e);
			}
		}
	}
	
	/**
	 * Adds a sales rep to the profile table
	 * @param encKey
	 * @param conn
	 * @param vo
	 * @throws SQLException
	 * @throws EncryptionException
	 */
	protected void insertProfile(Connection conn, ResultSet rs) 
	throws SQLException, DatabaseException, EncryptionException  {
		String profileId = new UUIDGenerator().getUUID();
		StringEncrypter se = new StringEncrypter(encryptKey);
		
		// Add an entry to the Profile table
	    StringBuilder s = new StringBuilder();
		s.append("insert into profile (profile_id, ");
		s.append("first_nm, last_nm, email_address_txt, ");
		s.append("search_first_nm, search_last_nm, search_email_txt, create_dt) ");
		s.append("values(?,?,?,?,?,?,?,?)");
		PreparedStatement ps = conn.prepareStatement(s.toString());
		ps.setString(1, profileId);
		ps.setString(2, se.encrypt(rs.getString("first_nm")));
		ps.setString(3, se.encrypt(rs.getString("last_nm")));
		//ps.setString(4, se.encrypt(rs.getString("email_address_txt")));
		ps.setString(4, profileId);
		ps.setString(5, se.encrypt(StringUtil.checkVal(rs.getString("first_nm")).toUpperCase()));
		ps.setString(6, se.encrypt(StringUtil.checkVal(rs.getString("last_nm")).toUpperCase()));
		ps.setString(7, se.encrypt(StringUtil.checkVal(rs.getString("email_address_txt")).toUpperCase()));
		ps.setTimestamp(8, Convert.getCurrentTimestamp());
		int pCount = ps.executeUpdate();
		log.debug("Profile ID: " + profileId + "|" + pCount + "|" + rs.getString("last_nm"));
		
		// Add an entry to the profile_address_table
		StringBuilder add = new StringBuilder();
		add.append("insert into profile_address (profile_address_id, profile_id, ");
		add.append("valid_address_flg, address_txt, city_nm, state_cd, zip_cd, ");
		add.append("county_nm, country_cd, latitude_no, longitude_no, geo_match_cd," );
		add.append("geo_address_txt, geo_city_nm, geo_state_cd, geo_zip_cd, ");
		add.append("cass_validate_flg, create_dt) ");
		add.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		int valid = 0;
		if (StringUtil.checkVal(rs.getString("address_txt")).length() > 0) valid = 1;
		PreparedStatement ps1 = conn.prepareStatement(add.toString());
		ps1.setString(1, new UUIDGenerator().getUUID());
		ps1.setString(2, profileId);
		ps1.setInt(3, valid);
		ps1.setString(4, se.encrypt(StringUtil.checkVal(rs.getString("address_txt"))));
		ps1.setString(5, rs.getString("city_nm"));
		ps1.setString(6, rs.getString("state_cd"));
		ps1.setString(7, rs.getString("zip_cd"));
		ps1.setString(8, "");
		ps1.setString(9, "US");
		ps1.setDouble(10, rs.getDouble("latitude_no"));
		ps1.setDouble(11, rs.getDouble("longitude_no"));
		ps1.setString(12, rs.getString("geo_match_cd"));
		ps1.setString(13, se.encrypt(StringUtil.checkVal(rs.getString("address_txt")).toUpperCase()));
		ps1.setString(14, StringUtil.checkVal(rs.getString("city_nm")).toUpperCase());
		ps1.setString(15, StringUtil.checkVal(rs.getString("state_cd")).toUpperCase());
		ps1.setString(16, rs.getString("zip_cd"));
		ps1.setInt(17, rs.getInt("cass_validate_flg"));
		ps1.setTimestamp(18, Convert.getCurrentTimestamp());
		ps1.executeUpdate();
		
		// Update the surgeon table
		StringBuffer sb = new StringBuffer();
		sb.append("update ").append(ansSchema).append("ans_surgeon ");
		sb.append("set profile_id = ?, update_dt = ? where surgeon_id = ? ");
		PreparedStatement ps2 = conn.prepareStatement(sb.toString());
		ps2.setString(1, profileId);
		ps2.setTimestamp(2, Convert.getCurrentTimestamp());
		ps2.setString(3, rs.getString("surgeon_id"));
		ps2.executeUpdate();
		
		log.debug("Successfully updated " + profileId);
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
