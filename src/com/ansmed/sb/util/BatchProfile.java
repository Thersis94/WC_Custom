package com.ansmed.sb.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ansmed.sb.sales.SalesRepVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: BatchProfile.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 10, 2007
 Last Updated:
 ***************************************************************************/

public class BatchProfile {
	Logger log = null;
	
	/**
	 * 
	 */
	public BatchProfile() {
	}
	
	public static void main(String[] args) {
		BatchProfile bp = new BatchProfile();
		
		// Get the Config
		Properties p = bp.loadConfiguration("scripts/ans_config.properties");
		
		bp.log = Logger.getLogger(BatchProfile.class);
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		// Connect to the database
		Connection conn = null;
		DatabaseConnection dbc = new DatabaseConnection(p.getProperty("dbDriver"),p.getProperty("dbUrl"),p.getProperty("dbUser"),p.getProperty("dbPassword"));
		bp.log.info("Starting Batch Profile");
		try {
			conn = dbc.getConnection();
			bp.process(p.getProperty("encryptKey"), p.getProperty("sbANSSchema"),p.getProperty("sbSchema"), conn, false);
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
	protected void process(String encKey, String ansSchema, String sbSchema, Connection conn, boolean multi) 
	throws SQLException {
		String s = "select * from " + ansSchema + "ans_sales_rep ";
		if(multi) {
			s+= "where sales_rep_id not in (select profile_id from" + sbSchema + "profile";
		}
		
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			try {
				insertProfile(encKey, sbSchema, conn, new SalesRepVO(rs));
			} catch(Exception e) {
				log.error("Error loading sales rep: " + rs.getString("sales_rep_id"), e);
			}
		}
	}
	
	/**
	 * Adds a sales rep to the  profile table
	 * @param encKey
	 * @param conn
	 * @param vo
	 * @throws SQLException
	 * @throws EncryptionException
	 */
	protected void insertProfile(String encKey, String schema, Connection conn, SalesRepVO vo) 
	throws SQLException, EncryptionException  {
		StringEncrypter se = new StringEncrypter(encKey);
		StringBuffer s = new StringBuffer();
		s.append("insert into ").append(schema).append("profile (profile_id, ");
		s.append("first_nm, last_nm, email_address_txt, authentication_id, ");
		s.append("search_first_nm, search_last_nm, search_email_txt, create_dt) ");
		s.append("values(?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement ps = conn.prepareStatement(s.toString());
		ps.setString(1, vo.getActionId());
		ps.setString(2, se.encrypt(vo.getFirstName()));
		ps.setString(3, se.encrypt(vo.getLastName()));
		ps.setString(4, se.encrypt(vo.getEmailAddress()));
		ps.setString(5, vo.getLoginId());
		ps.setString(6, se.encrypt(StringUtil.checkVal(vo.getFirstName()).toUpperCase()));
		ps.setString(7, se.encrypt(StringUtil.checkVal(vo.getLastName()).toUpperCase()));
		ps.setString(8, se.encrypt(StringUtil.checkVal(vo.getEmailAddress()).toUpperCase()));
		ps.setTimestamp(9, Convert.getCurrentTimestamp());
		
		ps.executeUpdate();
		log.info("Successfully updated " + vo.getActionId());
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
