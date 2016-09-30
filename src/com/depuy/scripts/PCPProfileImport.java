package com.depuy.scripts;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: ProfileImport.java<p/>
 * <b>Description: Imports users from a flat file</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 17, 2007
 ****************************************************************************/
public class PCPProfileImport {
   
	private static String SOURCE_DB_URL = "jdbc:sqlserver://10.0.20.53:2007";
	private static String DESTINATION_DB_URL = "jdbc:sqlserver://192.168.3.120:2007";
    //private static String DESTINATION_DB_URL = "jdbc:sqlserver://10.0.20.53:2007";
    private static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DESTINATION_AUTH = new String[] {"sb_user", "sqll0gin"};
    
    //from SB's config
    private static final String encKey = "s1l1c0nmtnT3chm0l0g13$JC";
    private static final String profileManagerClass = "com.smt.sitebuilder.action.user.SBProfileManager";
    private static final String geoUrl = "http://localhost:8080/gis/geocoder";
    private static final String geoClass = "com.siliconmtn.gis.SMTGeocoder";
    
    public static final Logger log = Logger.getLogger(PCPProfileImport.class);
	Map<String, Object> config = new HashMap<String, Object>();
	Connection dbConn = null;
	Connection srcDbConn = null;

    public PCPProfileImport() {
    	PropertyConfigurator.configure("/data/log4j.properties");

		try {
			dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
			srcDbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, SOURCE_DB_URL);
		} catch (DatabaseException de) {
			log.error(de);
		}

		config.put(Constants.ENCRYPT_KEY, encKey);
		config.put(Constants.GEOCODE_CLASS, geoClass);
		config.put(Constants.GEOCODE_URL, geoUrl);
		config.put(Constants.PROFILE_MANAGER_CLASS, profileManagerClass);
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {        
		PCPProfileImport db = new PCPProfileImport();
		try {
			db.process();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		} finally {
			db.closeConnection();
		}
		db = null;
	}
	
	/**
	 * 
	 * @param records
	 * @throws Exception
	 * 
	 */
	private void process() {
		int count = 0;
	    ProfileManager pm = ProfileManagerFactory.getInstance(config);
		String sql = "select * from PCP_MASTER where profile_id is null";
		String pcpSql = "insert into Sitebuilder_custom.dbo.DEPUY_PCP " +
		  "(PCP_ID, COMPANY_NM, WEBSITE_TXT, TITLE_TXT, CONTACT_PROFILE_ID, CREATE_DT) " +
		  "values (?,?,?,?,?,?)";
		
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		ResultSet rs = null;
		try {
			ps = srcDbConn.prepareStatement(sql);
			ps2 = dbConn.prepareStatement(pcpSql);  //the custom-data insert query, batched
			ps3 = srcDbConn.prepareStatement("update PCP_MASTER set profile_id=? where pk_id=?");
			rs = ps.executeQuery();
		} catch (SQLException sqle) {
			log.error(sqle);
		}
		
		//iterate the records, inserting each into the SB core
		try {
			while (rs.next()) {
				UserDataVO user = new UserDataVO();
				user.setEmailAddress(rs.getString("Email"));
				user.setPrefixName(rs.getString("Prefix"));
				user.setFirstName(rs.getString("First Name"));
				user.setMiddleName(rs.getString("Middle Name"));
				user.setLastName(rs.getString("Last Name"));
				user.setAddress(rs.getString("Address"));
				user.setAddress2(rs.getString("Address1"));
				user.setCity(rs.getString("City"));
				user.setState(rs.getString("State"));
				user.setZipCode(rs.getString("ZIP Code"));
				user.setCountryCode("US");
				user.setAllowCommunication(1);
				user.setValidEmailFlag(1);
				
				PhoneVO phone = new PhoneVO(PhoneVO.DAYTIME_PHONE, rs.getString("Phone Number"), user.getCountryCode());
				PhoneVO fax = new PhoneVO(PhoneVO.FAX_PHONE, rs.getString("FAX Number"), user.getCountryCode());
				List<PhoneVO> phs = new ArrayList<PhoneVO>();
				phs.add(phone);
				phs.add(fax);
				user.setPhoneNumbers(phs);
				
				try {
					//check for existing user
					user.setProfileId(pm.checkProfile(user, dbConn));
					
					//save the profile
					if (user.getProfileId() != null) {
						pm.updateProfilePartially(user.getDataMap(), user, dbConn);
						log.info("updated profile " + user.getProfileId());
					} else {
						pm.updateProfile(user, dbConn); //runs insert query
						log.info("added profile " + user.getProfileId());
					}
					
					if (user.getProfileId() == null) 
						log.error("PROFILE_ID is null, vo=" + user.toString());
					
					//insert comm flag
					pm.assignCommunicationFlg("DEPUY", user.getProfileId(), user.getAllowCommunication(), dbConn);
				} catch (DatabaseException de) {
					log.error("could not store profileData for ID=" + rs.getInt("pk_id"), de);
					log.error("UserVO=" + user.toString());
					continue;
				}
				
				//update the source table with a profile_id value
				try {
					ps3.setString(1, user.getProfileId());
					ps3.setInt(2, rs.getInt("pk_id"));
					ps3.addBatch();
				} catch (SQLException sqle3) {
					log.error(sqle3);
				}
				
				try {
					ps2.setString(1, new UUIDGenerator().getUUID());
					ps2.setString(2, rs.getString("Company Name"));
					ps2.setString(3, rs.getString("Web Address"));
					ps2.setString(4, rs.getString("Title"));
					ps2.setString(5, user.getProfileId());
					ps2.setTimestamp(6, Convert.getCurrentTimestamp());
					ps2.addBatch();
				} catch (SQLException sqle) {
					log.error(sqle);
				}
				
				//increment our counter
				count++;
				
				if (count % 500 == 0) {
					log.info("committing batch transactions at " + count);
					writeBatch(ps2);
					writeBatch(ps3);
				}
				
			}  //while
		} catch (SQLException sqle) {
			log.error(sqle);
		}
		
		writeBatch(ps2);
		log.info("written DEPUY_PCP table");
		writeBatch(ps3);
		log.info("written PCP_MASTER table");
				
		try {
			ps.close();
			ps2.close();
			ps3.close();
		} catch (Exception e) {}
		
		log.info(count + " rows inserted into profile table");
	}

	
	private void writeBatch(PreparedStatement ps) {
		try {
			ps.executeBatch();
			ps.clearBatch();
		} catch (SQLException e) {
			log.error(e);
		}
	}
	
	
	/**
	 * 
	 * @param userName Login Account
	 * @param pwd Login password info
	 * @param driver Class to load
	 * @param url JDBC URL to call
	 * @return Database Connection object
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String userName, String pwd, String driver, String url) 
	throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver", cnfe);
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, userName, pwd);
		} catch (SQLException sqle) {
			sqle.printStackTrace(System.out);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}
		
		return conn;
	}

	protected void closeConnection() {
		try {
			dbConn.close();
		} catch(Exception e) {}
	}

}
