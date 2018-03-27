package com.rezdox.util.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DecryptProfile.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Adds the user profile info to the member table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 27, 2018
 * @updates:
 ****************************************************************************/

public class DecryptProfile {
	public static final String DB_URL = "jdbc:postgresql://sonic:5432/tjohnson_webcrescendo_sb?defaultRowFetchSize=25&amp;prepareThreshold=3";
    public static final String DB_DRIVER ="org.postgresql.Driver";
    protected static final String[] DB_AUTH = new String[] {"ryan_user_sb", "sqll0gin"};
    
    // Add a logger
    Logger log = Logger.getLogger(DecryptProfile.class);
    
	/**
	 * 
	 */
	public DecryptProfile() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DecryptProfile dp = new DecryptProfile();
		Connection conn = dp.getDBConnection(DB_AUTH, DB_DRIVER, DB_URL);
		
		dp.processProfiles(conn);
		DBUtil.close(conn);
	}
	
	/**
	 * Gets a list of profiles
	 * @param conn
	 * @return
	 * @throws SQLException
	 * @throws EncryptionException 
	 */
	public void processProfiles(Connection conn) throws SQLException, EncryptionException {
		StringEncrypter se = new StringEncrypter("s1l1c0nmtnT3chm0l0g13$JC");
		String sql = "select member_id, b.first_nm, b.last_nm, b.email_address_txt from custom.rezdox_member a ";
		sql += "inner join profile b on a.profile_id = b.profile_id";
		
		// Loop each user and update their record
		try(PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while(rs.next()) {
				String fn = se.decrypt(rs.getString("first_nm"));
				String ln = se.decrypt(rs.getString("last_nm"));
				String email = StringUtil.checkVal(se.decrypt(rs.getString("email_address_txt"))).toLowerCase();
				String id = rs.getString("member_id");
				log.info(fn + " " + ln + "|" + email + "|" + id);
				updateRecord(conn, id, fn, ln, email);
			}
		}

	}
	
	/**
	 * Updates the member table
	 * @param conn
	 * @param id
	 * @param fn
	 * @param ln
	 * @param email
	 * @throws SQLException
	 */
	public void updateRecord(Connection conn, String id, String fn, String ln, String email) throws SQLException {
		String sql = "update custom.rezdox_member set first_nm = ?, last_nm = ?, email_address_txt = ? where member_id = ?";
		
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, fn);
			ps.setString(2, ln);
			ps.setString(3, email);
			ps.setString(4, id);
			
			ps.executeUpdate();
		}
	}

	/**
	 * 
	 * @param userName Login Account
	 * @param pwd Login password info
	 * @param driver Class to load
	 * @param url JDBC URL to call
	 * @return Database Conneciton object
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String[] auth, String driver, String url) 
	throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver", cnfe);
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, auth[0], auth[1]);
		} catch (SQLException sqle) {
			log.error("Unable to get db connection", sqle);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}
		
		return conn;
	}
}

