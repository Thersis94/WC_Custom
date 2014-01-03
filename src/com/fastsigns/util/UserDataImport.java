package com.fastsigns.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

public class UserDataImport {
	private static Logger log = Logger.getLogger(UserDataImport.class);
	
	private Connection conn;
	private String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private String dbUrl = "jdbc:sqlserver://sql_server_db:1433";
	private String dbUser = "sb_user";
	private String dbPassword = "sqll0gin";
	
	//private String userDataSourceFile = "C:\\Temp\\SiliconMtn\\FTS\\import\\testImportLarge3.csv";
	private String userDataSourceFile = "/Users/james/Temp/fs_user_data_1.csv";
	
	private String registeredUserRoleId = "10"; //'registered user'
	private int userRoleStatusId = 20; //'approved'
	
	private String userSiteId = "FTS_1";
	
	private List<String> fields = null;
	private Map<String, Object> config = new HashMap<String, Object>();
	private String schema = "sitebuilder_custom.dbo.";
	
	private StringBuffer badDataBuffer = new StringBuffer();
	private int recCount = 0;
	private int newUserCount = 0;
	private int dupeUserCount = 0;
	private int badSourceRecordCount = 0;
	private int invalidProfileCount = 0;

	public UserDataImport() {
		PropertyConfigurator.configure("scripts/fts_log4j.properties");
		config.put(Constants.PROFILE_MANAGER_CLASS, "com.smt.sitebuilder.action.user.SBProfileManager");
		config.put(Constants.ENCRYPT_KEY, "s1l1c0nmtnT3chm0l0g13$JC");
		config.put(Constants.GEOCODE_URL, "http://localhost:9000/gis/geocoder");
		config.put(Constants.GEOCODE_CLASS, "com.siliconmtn.gis.SMTGeocoder");
	}
	
	/**
	 * MAIN
	 * @param args
	 */
	public static void main(String[] args) {
		UserDataImport udi = new UserDataImport();
		
		try {
			udi.getDBConnection();
		} catch (Exception e) {
			log.error("Could not obtain db connection.",e);
			System.exit(-1);
		}
		
		//initialize fields list
		udi.loadFields();
		
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(udi.userDataSourceFile);
			br = new BufferedReader(fr);
			String strIn = "";
			
			ProfileManager pm = ProfileManagerFactory.getInstance(udi.config);
			Map<String,Object> dataMap = new HashMap<String,Object>();
			
			// loop the records in the user data file
			while((strIn = br.readLine()) != null) {
				udi.recCount++;
				// clear the data map and populate map with new row values 
				dataMap.clear();
				
				try {
					dataMap = udi.parseUserData(strIn);
				} catch (Exception e) {
					udi.badSourceRecordCount++;
					udi.badDataBuffer.append(strIn).append("\n");
					continue;
				}
				
				// create user records
				udi.createUser(pm, dataMap);
			}
		} catch (FileNotFoundException fe) {
			log.error("Cannot load user source ", fe);
			System.exit(-1);
		} catch (IOException ioe) {
			log.error("Cannot access user source ", ioe);
			System.exit(-1);
		} finally {
			try {
				br.close();
				fr.close();
			} catch (Exception e) {}
		}
		log.info("total rows processed: " + udi.recCount);
		log.info("new users: " + udi.newUserCount);
		log.info("duplicate users: " + udi.dupeUserCount);
		log.info("invalid users: " + udi.invalidProfileCount);
		log.info("invalid source records: " + udi.badSourceRecordCount);
		// clean up
		udi.closeDBConnection();
		
		//dump the bad data buffer to the log
		log.error(udi.badDataBuffer.toString());
		
	}
	
	/**
	 * manages creation of user records
	 * @param pm
	 * @param dataMap
	 */
	public void createUser(ProfileManager pm, Map<String,Object> dataMap) {
		String authId = null;
		//add user's state/country codes to the dataMap if possible
		this.getUserStateCountry(dataMap);
		
		// pre-load user data
		UserDataVO uvo = new UserDataVO();
		uvo.setData(dataMap);
		
		// find or create profile first so that any duplicates are filtered out
		try {
			uvo = this.createProfile(pm, dataMap);
		} catch(Exception e) {
			log.error("Error creating user profile, skipping this user.");
			invalidProfileCount++;
			return;
		}
		
		// if we successfully found/created a profile, continue.
		if (uvo.getProfileId() != null && uvo.getProfileId().length() > 0) {
			log.info("user profileId: " + uvo.getProfileId());
			// check to see if authId exists for this user
			authId = this.checkAuthByProfile(uvo.getProfileId());
			
			// if authId doesn't exist, try to create one
			if (authId == null || authId.length() == 0) {
				try {
					log.debug("creating authId");
					authId = this.createAuthRecord(dataMap);
				} catch(Exception e) {
					log.error("Error creating authId record, ", e);
				}
			}
			
			// if authId was found/created, update profile, create profile-role.
			if (authId != null && authId.length() > 0) {
				// update profile with authId
				uvo.setAuthenticationId(authId);
				dataMap.put("AUTHENTICATION_ID",authId);
				try {
					pm.updateProfilePartially(dataMap,uvo,conn);
				} catch(Exception e) {
					log.error("could not update profile with authentication id ", e);
				}
				
				// create profile-role
				try {
					this.createProfileRole(uvo.getProfileId());
				} catch(Exception e) {
					log.error("could not create profile-role record ", e);
				}
			}
			
			// check to see if franchise XR record already exists for this user
			String xrId = null;
			try {
				xrId = this.checkXRRecord(uvo.getProfileId());
			} catch (Exception e) {
				log.error("could not check for existing XR record", e);
			}
			
			if (xrId == null) {
				// create xr record (independent of auth record)
				try {
					this.createXRRecord(uvo.getProfileId(),dataMap);
				} catch (Exception e) {
					log.error("error creating user XR record ",e);
				}
			}
		}
	}

	/**
	 * Parses a line read by the BufferedReader
	 * @param strIn
	 * @return
	 */
	public Map<String,Object> parseUserData(String strIn) {
		Map<String,Object> userData = new HashMap<String,Object>();
		StringTokenizer st = new StringTokenizer(strIn,"|");
		int tokenCnt = 0;
		
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equalsIgnoreCase("null")) token = "";
			userData.put(fields.get(tokenCnt), token);
			switch(tokenCnt) {
			case 1:
				log.info("processing user: " + token);
				break;
			}
			tokenCnt++;
		}
		return userData;
	}
	
	/**
	 * Creates user profile
	 * @param userData
	 * @return
	 */
	public UserDataVO createProfile(ProfileManager pm, Map<String,Object> userData) 
		throws DatabaseException {
		UserDataVO user = new UserDataVO();
		user.setData(userData);
		String profileId = null;
		
		try {
			log.debug("checking profile");
			profileId = pm.checkProfile(user, conn);
			user.setProfileId(profileId);
		
			if (profileId == null || profileId.length() == 0) {
				log.debug("new user");
				newUserCount++;
				pm.updateProfile(user, conn);
			} else {
				log.debug("dupe user");
				dupeUserCount++;
			}
		} catch(DatabaseException de) {
			throw new DatabaseException(de);
		}
		return user;
	}
	
	/**
	 * Creates user's profile role record
	 * @param profileId
	 * @throws SQLException
	 */
	public void createProfileRole(String profileId) throws SQLException {
		
		log.debug("creating profile-role");
		ProfileRoleManager prm = new ProfileRoleManager();
		String roleId = null;
		
		try {
			roleId = prm.checkRole(profileId, this.userSiteId, conn);
		} catch (DatabaseException de) {
			log.error("error checking user role");
		}
		// no role found, add role
		if (roleId == null) {
			SBUserRole role = new SBUserRole();
			role.setProfileId(profileId);
			role.setSiteId(userSiteId);
			role.setRoleId(registeredUserRoleId);
			role.setStatusId(userRoleStatusId);
			
			try {
				prm.addRole(role,conn);
			} catch (DatabaseException de) {
				log.error("could not add role for user");
			}
		}
	}
	
	/**
	 * Utility class - retrieves state/country code for given state name.
	 * @param dataMap
	 */
	public void getUserStateCountry(Map<String,Object> dataMap) {
		log.debug("checking state_nm");
		if (StringUtil.checkVal(dataMap.get("STATE_NM")).length() == 0) return;
		
		StringBuffer sql = new StringBuffer();
		sql.append("select state_cd, country_cd from state where state_nm = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, (String)dataMap.get("STATE_NM"));
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				dataMap.put("STATE_CD", rs.getString(1));
				dataMap.put("COUNTRY_CD", rs.getString(2));
			} else {
				log.info("No info found for state_nm: " + (String)dataMap.get("STATE_NM"));
			}
		} catch (SQLException sqle) {
			log.error("failed to retrieve state/country codes for user", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}

	/**
	 * checks to see if authId exists for this profileId
	 * @param profileId
	 * @return
	 */
	public String checkAuthByProfile(String profileId) {
		String authId = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select authentication_id from profile where profile_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1,profileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) authId = rs.getString(1);
		} catch (SQLException sqle) {
			return null;
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return authId;
	}
	
	/**
	 * Creates authentication record
	 * @param userMap
	 */
	public String createAuthRecord(Map<String,Object> userMap)
		throws SQLException {	
		StringBuffer sql = new StringBuffer();
		sql.append("insert into authentication (authentication_id, user_nm, password_txt,");
		sql.append("password_reset_flg, create_dt) values (?,?,?,?,?)");
		
		log.debug("auth sql: " + sql.toString());
		PreparedStatement ps = null;
		String authId = new UUIDGenerator().getUUID();
		log.debug("authId: " + authId);
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, authId);
			ps.setString(2, (String)userMap.get("EMAIL_ADDRESS_TXT"));
			ps.setString(3, (String)userMap.get("PASSWORD_TXT"));
			ps.setInt(4, 0);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
			
		} catch(SQLException sqle) {
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return authId;
	}
	
	/**
	 * Checks for existence of user's franchise role xr record
	 * @param profileId
	 */
	public String checkXRRecord(String profileId) throws SQLException {
		String xrId = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select fts_franchise_role_xr_id from ");
		sql.append(schema).append("fts_franchise_role_xr ");
		sql.append("where profile_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, profileId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				xrId = rs.getString(1);
			}
		} catch(SQLException sqle) {
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return xrId;
	}
	
	/**
	 * Creates user's franchise role xr record
	 * @param profileId
	 * @param dataMap
	 */
	public void createXRRecord(String profileId, Map<String,Object> dataMap)
		throws SQLException {
		
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("fts_franchise_role_xr ");
		sql.append("(fts_franchise_role_xr_id, franchise_id, profile_id, ");
		sql.append("legacy_company_nm, legacy_role_nm, create_dt) ");
		sql.append("values (?,?,?,?,?,?)");
		
		log.debug("XR sql: " + sql.toString() + "|" + (String)dataMap.get("FRANCHISE_ID") + "|" + profileId);
		PreparedStatement ps = null;
		String roleXrId = new UUIDGenerator().getUUID();
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, roleXrId);
			ps.setString(2, (String)dataMap.get("FRANCHISE_ID"));
			ps.setString(3, profileId);
			ps.setString(4, (String)dataMap.get("LEGACY_COMPANY_NM"));
			ps.setString(5, (String)dataMap.get("LEGACY_ROLE_NM"));
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
			
		} catch(SQLException sqle) {
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}

	private void loadFields() {
		// NOTE: these fields are in a specific order corresponding to the
		// order of the delimited fields in the data file
		fields = new ArrayList<String>();
		fields.add("LAST_NM");
		fields.add("EMAIL_ADDRESS_TXT");
		fields.add("PASSWORD_TXT");
		fields.add("ZIP_CD");
		fields.add("LEGACY_COMPANY_NM");
		fields.add("MAIN_PHONE_TXT");
		fields.add("ADDRESS_TXT");
		fields.add("ADDRESS2_TXT");
		fields.add("CITY_NM");
		fields.add("STATE_NM");
		fields.add("LEGACY_ROLE_NM");
		fields.add("FRANCHISE_ID");
	}
	
	/**
	 * Gets a db connection
	 * @throws Exception
	 */
	private void getDBConnection() throws Exception {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		try {
			conn = dbc.getConnection();
			log.info("Successfully established a database connection.");
		} catch (Exception de) {
			log.error("Could not establish a database connection. ",de);
			throw new Exception(de);
		}
	}
	
	/**
	 * closes the db connection
	 */
	private void closeDBConnection() {
		if (conn != null) {
			try {
				conn.close();
			} catch(Exception e) {
				log.error("could not close db connection",e);
			}
		}
	}
}
