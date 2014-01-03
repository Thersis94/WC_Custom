package com.ansmed.sb.util;

//JDK 1.6.0
import java.io.FileInputStream;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.StringBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// SMT Base Libraries
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// SiteBuilder Libraries
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;

// ANSMED Libraries
import com.ansmed.sb.sales.SalesRepVO;

//Log4J 1.2.8
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/****************************************************************************
 * <b>Title</b>: SalesProfileProcessor.java<p/>
 * <b>Description: </b> Processes ANS sales rep profiles. 
 * <p/>
 * <b>Copyright:</b> (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar. 11, 2008
 ****************************************************************************/
public class SalesProfileProcessor {

	protected static Logger log = Logger.getLogger(SalesProfileProcessor.class);
	private final String sbANSSchema;
	private Map<String, Object> config = new HashMap<String, Object>();
	
	/**
	 * Configures logger and loads properties file.
	 */
	public SalesProfileProcessor() {
		
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		InputStream inStream = null;
		Properties props = new Properties();
		
		// Load the config file.
		try {
			inStream = new FileInputStream("scripts/ans_config.properties");
			props.load(inStream);
			log.debug("Successfully loaded config file");
		} catch (FileNotFoundException e){
			log.error("Unable to find configuration file.");
			System.exit(-1);
		} catch (IOException ioe) {
			log.error("Unable to access configuration file.");
			System.exit(-1);
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {
					log.error("Could not close file input stream.");
				}
			}
		}
		
		sbANSSchema = props.getProperty("sbANSSchema");
		config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptKey"));
		config.put(Constants.GEOCODE_URL, props.getProperty("geoUrl"));
		config.put(Constants.GEOCODE_CLASS, props.getProperty("geoClass"));
				
	}
	
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String[] args) {
		
		Connection conn = null;
		
		// Db connection parameters.
		String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		String dbUrl = "jdbc:sqlserver://10.0.70.2:2449";
		String user = "sitebuilder_sb_user";
		String pwd = "sqll0gin";
		
		SalesProfileProcessor spp = new SalesProfileProcessor();
		
		// Get a db connection.
		conn = spp.getConnection(driver, dbUrl, user, pwd);
		
		// Retrieve sales reps as UserDataVO's
		Map<String,UserDataVO> users = spp.retrieveSalesReps(conn);
		
		// Process inserts/updates and retrieve map of original Id, new Id.
		Map<String,String> profileMap = spp.processRepProfiles(users,conn);
		
		// Synchronize ANS_SALES_REP profile id's with PROFILE profile id's.
		spp.synchronizeProfiles(profileMap,conn,"ANS_SALES_REP",spp.sbANSSchema);
		
		//Close the db connection.
		try {
			conn.close();
		} catch (SQLException se) {
			log.error("DatabaseException: Could not close db connection.",se);
		}
		
	}
	
	/**
	 * Queries ANS_SALES_REP table and retrieves sales rep data. 
	 * @param cn
	 * @return
	 */
	protected Map<String,UserDataVO> retrieveSalesReps(Connection cn) {
		
		Map<String,UserDataVO> userMap = new HashMap<String,UserDataVO>();
		StringBuffer sql = new StringBuffer();
		sql.append("select SALES_REP_ID, PROFILE_ID, FIRST_NM, LAST_NM, EMAIL_ADDRESS_TXT from ");
		sql.append(sbANSSchema).append("ANS_SALES_REP");
		
		log.debug("retrieveSalesReps: " + sql.toString());
		
		PreparedStatement pStmt = null;
		
		try {
			ResultSet reps = null;
			pStmt = cn.prepareStatement(sql.toString());
			reps = pStmt.executeQuery();
			
			while (reps.next()) {
				SalesRepVO salesRep = new SalesRepVO();
				salesRep.setData(reps);
				userMap.put(salesRep.getProfileId(),salesRep.getUserData());
			}
			
		} catch (SQLException se){
			log.error("SQLException: ", se);
		} finally {
			if (pStmt != null) {
				try {
					pStmt.close();
				} catch (Exception e) {
					log.error("Could not close PreparedStatement.");
				}
			}
		}
		log.debug("Sales rep map size is: " + userMap.size());
		
		return userMap;
	}
	
	/**
	 * Processes sales rep's UserDataVO.  Calls ProfileManager to update/insert profile.
	 * @param udvs
	 * @param cn
	 * @return
	 */
	protected Map<String,String> processRepProfiles(Map<String,UserDataVO> udvs, Connection cn) {
		
		UserDataVO uVO = new UserDataVO();
		Map<String,Object> columns = new HashMap<String,Object>();
		Map<String,String> synchMap = new HashMap<String,String>();

		int updated = 0;
		int inserted = 0;
		
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		Set<String> keys = udvs.keySet();
		Iterator<String> iter = keys.iterator();
		
		while (iter.hasNext()) {

			String origId = iter.next();
			uVO = udvs.get(origId);
			log.debug("Original ANS_SALES_REP profileId is: " + origId);
		
			try {
				uVO.setProfileId(pm.checkProfile(uVO,cn));

				if (StringUtil.checkVal(uVO.getProfileId()).length() > 0) {
					
					columns.put("FIRST_NM", uVO.getFirstName());
					columns.put("LAST_NM", uVO.getLastName());
					columns.put("EMAIL_ADDRESS_TXT", uVO.getEmailAddress());
					
					pm.updateProfilePartially(columns,uVO.getProfileId(),cn);
					synchMap.put(origId,uVO.getProfileId());
					log.debug("Profile exists - performed partial update: " + uVO.getProfileId());
					updated++;
					
				} else {
					
					pm.updateProfile(uVO, cn);
					synchMap.put(origId,uVO.getProfileId());					
					log.debug("Profile did not exist - inserted profile.");
					inserted++;
				}
				
			} catch (DatabaseException de){
				log.error("DatabaseException: Could not insert or update profile.", de);
			}
			
		}
		
		log.info("Updated: " + updated + " profiles.");
		log.info("Inserted: " + inserted + " profiles.");
		return synchMap;
		
	}

	/**
	 * Synchronizes a sales rep's ANS_SALES_REP table profile ID with the sales rep's
	 * profile ID in the PROFILE table.
	 * @param profileMap
	 * @param cn
	 * @param table
	 * @param schema
	 */
	protected void synchronizeProfiles(Map<String,String> profileMap, Connection cn,
			String table, String schema) {
		
		int count = 0;
		String oldId = null;
		String newId = null;
		
		StringBuffer sql = new StringBuffer(); 
		sql.append("update ").append(schema).append(table);
		sql.append(" set PROFILE_ID = ? where PROFILE_ID = ?");

		PreparedStatement ps = null;
		
		Set<String> profKey = profileMap.keySet();
		Iterator<String> iter = profKey.iterator();
		
		try {
		
			while (iter.hasNext()) {
			
				oldId = iter.next();
				newId = profileMap.get(oldId);
			
				if (newId != null && newId.length() > 0) {
					
					if (!oldId.equals(newId)) {
		
						ps = cn.prepareStatement(sql.toString());
						ps.setString(1,newId);
						ps.setString(2,oldId);
	
						count = ps.executeUpdate();
						if (count == 0) throw new DatabaseException(ErrorCodes.ERR_DB_UPDATE);
						log.debug("Synchronized profile: Original: " + oldId + " - New: " + newId);
					} else {
						log.debug("Old and new profile_id are the same, skipping synchronization.");
					}
				}
			}
		} catch (SQLException se) {
					log.error("SQLException: Could not update profile.", se);
		} catch (DatabaseException dbe) {
					log.error("DatabaseException: Update failed.", dbe);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					log.error("Could not close PreparedStatement.");
				}
			}
		}
	}
	
	/**
	 * Returns a database connection based on the parameters passed in.
	 * @param driver
	 * @param url
	 * @param user
	 * @param pwd
	 * @return
	 */
	protected Connection getConnection(String driver, String url, String user,
			String pwd) {
						
		// Get a database connection.
		Connection cnx = null;
		DatabaseConnection dbc = new DatabaseConnection(driver,url,user,pwd);
		try {
			cnx = dbc.getConnection();
			log.debug("Got a database connection.");
		} catch (Exception de) {
			log.error("Couldn't get a database connection. ",de);
			System.exit(-1);
		}
		return cnx;
		
	}
	
}
