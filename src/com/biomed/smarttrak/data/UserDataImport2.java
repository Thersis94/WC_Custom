package com.biomed.smarttrak.data;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.PropertyConfigurator;

// WC custom
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;

//SMTBaseLibs
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: UserDataImport.java<p/>
 * <b>Description: This class was created to batch-load legacy SmartTRAK data for the
 * BiomedGPS SmartTRAK site.  This class uses an Excel file as the source data for 
 * inserting/updating profiles, creating roles, and creating registration records for a user.
 * Registration records are either created by pushing them through the front door of the website's
 * registration page or by manual insertion, depending upon the profile of the user.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author DBargerhuff
 * @version 1.0
 * @since Jan 19, 2017
 ****************************************************************************/
public class UserDataImport2 extends CommandLineUtil {

	// import env params
	private static final String SOURCE_FILE_CONFIG="scripts/bmg_smarttrak/user_import_config.properties";
	private static final String SOURCE_FILE_LOG="scripts/bmg_smarttrak/user_import_log4j.properties";

	// profile header vals
	private Map<String,StringBuilder> queries;
	private Map<String,String> duplicateProfiles;
	private Map<String,String> processedProfiles;
	private Map<String,String> failedSourceUserInserts;
	private Map<String,String> failedSourceUserProfileUpdates;
	private Map<String,String> failedSourceUserAuthenticationUpdates;
	
	enum ImportField {
		account_id, active, address_txt, address2_txt, 
		advtrainingdt, allow_comm_flg, 
		city_nm, company, companyurl, country_cd,
		date_expiration, date_joined, demodt,
		email_address_txt,
		favoriteupdates, first_nm,
		industry, inittrainingdt, 
		jobcategory, 	joblevel, last_nm, 
		main_phone_txt, mobile_phone_txt, notes,  
		organization_id, othertrainingdt, password_txt,
		role_id,
		site_id,	smarttrak_id, smarttrak_user_nm, smarttrak_password_txt, 
		source, staff, status, state_cd, super_user,
		title, trainingdt,
		updates, username,
		zip_cd
	}

	public UserDataImport2(String[] args) {
		super(args);
		PropertyConfigurator.configure(SOURCE_FILE_LOG);
		queries = initQueryStatements();
		duplicateProfiles = new LinkedHashMap<>();
		processedProfiles = new LinkedHashMap<>();
		failedSourceUserInserts = new LinkedHashMap<>();
		failedSourceUserProfileUpdates = new LinkedHashMap<>();
		failedSourceUserAuthenticationUpdates = new LinkedHashMap<>();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		UserDataImport2 udi = new UserDataImport2(args);
		udi.run();
	}
	
	public void run() {
		// load props
		loadProperties(SOURCE_FILE_CONFIG);
		// get dbconn
		loadDBConnection(props);
		// retrieve records
		List<Map<String,Object>> records = retrieveData();
		log.info("records retrieved: " + records.size());
		try {
			insertRecords(records);
		} catch(Exception e) {
			log.error("Error, failed to insert records, ", e);
		}
		
		// clean up
		closeDBConnection();
	}
	
	/**
	 * Retrieve data.
	 * @return
	 */
	protected List<Map<String,Object>> retrieveData() {
		// doQuery
		StringBuilder sql = buildMainQuery();
		log.info("SQL: " + sql.toString());
		List<Map<String,Object>> records;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			records = parseResults(rs);
		} catch (Exception e) {
			log.error("Error retrieving data, ", e);
			records = new ArrayList<>();
		}
		return records;
	}
	
	/**
	 * Parses the result set into a Map of String (key) to Object (value).
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<Map<String,Object>> parseResults(ResultSet rs) throws SQLException {
		Map<String,Object> record;
		List<Map<String,Object>> records = new ArrayList<>();
		while(rs.next()) {
			record = new HashMap<>();
			for (ImportField field : ImportField.values()) {
				record.put(field.name(), rs.getObject(field.name()));
			}
			records.add(record);
		}
		return records;
	}

	/**
	 * Processes import records
	 * @param records
	 * @throws Exception
	 */
	protected void insertRecords(List<Map<String, Object>> records) throws Exception {
		log.debug("inserting records..., records list size is: " + records.size());
		if (records.isEmpty()) return;
		
		// create config map
		Map<String, Object> config = new HashMap<>();
		config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptionKey"));
		config.put(Constants.GEOCODE_CLASS, props.getProperty("geocodeClass"));
		config.put(Constants.GEOCODE_URL, props.getProperty("geocodeUrl"));

		// init profile managers
	    ProfileManager pm = new SBProfileManager(config);
		ProfileRoleManager prm = new ProfileRoleManager();
		UserLogin ul = new UserLogin(dbConn, props.getProperty("encryptionKey"));

		// load user divisions
		Map<String,List<String>> userDivs = loadUserDivisions(dbConn);

		// init vars
		int recordCnt = 0;
		int successCnt = 0;
		int failedCnt = 0;
		int skipCnt = 0;
		UserVO user;
		Map<String,Object> dataSet;

		//iterate the records, inserting each
		Iterator<Map<String,Object>> iter = records.iterator();
		while (iter.hasNext()) {
			recordCnt++;
			dataSet = iter.next();
			
			// clean up certain values before we import them.
			sanitizeFieldData(dataSet);
			
			// init site user from import source
			user = initUser(dataSet);
			
			log.info("START: processing record|source: " + recordCnt + "|" + user.getUserId());
			// if we weren't able to get a valid email address for the user, don't even process it.
			if (user.getValidEmailFlag() == 0) {
				logInvalidUserRecord(recordCnt, user);
				failedCnt++;
				continue;
			}
			
			try {
				/* check for pre-existing user profile */
				findProfile(dbConn, pm, user);
				
				/* Check for duplicates */
				if (isDuplicateProfile(user,recordCnt))
					continue;
				
				/* create a profile if appropriate, otherwise, leave profile untouched */
				skipCnt = processProfile(dbConn, pm, user, skipCnt);

				/* Create an auth record if appropriate, otherwise, leave auth untouched */
				processAuthentication(dataSet,ul,user);

				/* If an org ID and comm flag were supplied, opt-in this user for the given org.	 */
				processCommFlag(dbConn, pm, dataSet, user.getProfileId());

				/* Add profile roles for this user for the specified site ID. */
				processRole(dbConn, prm, dataSet, user.getProfileId());

			} catch(Exception ex) {
				log.error("Error processing source ID " + user.getUserId() + ", " + ex);
			}

			// if valid profile, insert reg records, create biomedgps user, update profile/auth.
			if (user.getProfileId() != null) {
				successCnt++;
				insertRegistrationRecords(dbConn, dataSet, user, userDivs.get(user.getUserId()));
				insertSourceUser(dbConn, user);
				// if valid auth record, update profile/auth
				if (hasValidAuthentication(user)) {
					updateSourceUserProfile(dbConn, user);
					updateSourceUserAuthentication(dbConn, user);
				}
				log.info("END: inserting for record|source: " + recordCnt + "|" + user.getUserId());
			} else {
				// if we couldn't successfully create a profile, add to failed count.
				logInvalidUserRecord(recordCnt, user);
				failedCnt++;
			}
		}

		// log summary info.
		writeLogs(recordCnt,successCnt,failedCnt,skipCnt);
	}

	/**
	 * Initializes a UserVO from the dataset map passed in.
	 * @param dataSet
	 * @return
	 */
	protected UserVO initUser(Map<String,Object> dataSet) {
		UserVO user = new UserVO();
		user.setData(dataSet);
		user.setUserId((String)dataSet.get(ImportField.smarttrak_id.name()));
		user.setPassword(StringUtil.checkVal(dataSet.get(ImportField.smarttrak_password_txt.name()),null));
		// ensure status code is set to a default or uppercase value.
		user.setStatusCode(StringUtil.checkVal(dataSet.get(ImportField.status.name()),UserVO.Status.INACTIVE.getCode()).toUpperCase());
		user.setCreateDate(Convert.formatDate(StringUtil.checkVal(dataSet.get(ImportField.date_joined.name()),null)));
		user.setExpirationDate(Convert.formatDate(StringUtil.checkVal(dataSet.get(ImportField.date_expiration.name()),null)));
		user.setAccountId(StringUtil.checkVal(dataSet.get(ImportField.account_id.name()),null));

		// parse user's email.
		parseUserEmail(user, (String)dataSet.get(ImportField.smarttrak_user_nm.name()));

		return user;
	}
	
	/**
	 * 
	 * @param recordCnt
	 * @param user
	 */
	protected void logInvalidUserRecord(int recordCnt, UserVO user) {
		log.info("END: INVALID record, DID NOT insert for record|source: " + recordCnt + "|" + user.getUserId());
	}
	
	/**
	 * Check for duplicate profile.
	 * @param user
	 * @param recordCnt
	 * @return
	 */
	protected boolean isDuplicateProfile(UserVO user, int recordCnt) {
		if (user.getProfileId() != null) {
			if (processedProfiles.containsValue(user.getProfileId())) {
				log.info("END: IS DUPLICATE PROFILE, skipping record|userId: " + recordCnt + "|" + user.getUserId());
				// add profile ID to dupes map.
				duplicateProfiles.put(user.getUserId(),user.getProfileId());
				return true;
			} else {
				processedProfiles.put(user.getUserId(), user.getProfileId());
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Checks user's email address.  If email address is invalid, the user's legacy 
	 * username is checked.  If the username is not a valid email address, 
	 * the user's email address is set to null.
	 * @param user
	 * @param originalUserName
	 */
	protected void parseUserEmail(UserVO user, String legacyUserName) {
		// check email address
		if (! StringUtil.isValidEmail(user.getEmailAddress())) {
			// email address not valid, check username
			String stUserName = StringUtil.checkVal(legacyUserName);
			if (! StringUtil.isValidEmail(stUserName)) {
				// username not valid email address either, append fallback email suffix
				user.setEmailAddress(stUserName + props.getProperty("emailSuffixDefault"));
			} else {
				user.setEmailAddress(stUserName);
			}
		}
		// Check to see if we can set 'valid' email flag
		if (StringUtil.isValidEmail(user.getEmailAddress())) {
			user.setValidEmailFlag(1);
		} else {
			user.setValidEmailFlag(0);
		}
		log.debug("User email address is: " + user.getEmailAddress());
	}
	
	/**
	 * Inserts the SmartTRAK source user record.
	 * @param dbConn
	 * @param sUser
	 */
	protected void insertSourceUser(Connection dbConn, UserVO sUser) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into custom.biomedgps_user (user_id, profile_id, account_id, ");
		if (sUser.getRegisterSubmittalId() != null) 
			sql.append("register_submittal_id, ");
		
		if (sUser.getExpirationDate() != null) 
			sql.append("expiration_dt, ");
		
		if (sUser.getCreateDate() != null) 
			sql.append("create_dt, ");
		
		sql.append("status_cd) values (?,?,?");

		if (sUser.getRegisterSubmittalId() != null) 
			sql.append(",?");
		
		if (sUser.getExpirationDate() != null) 
			sql.append(",?");
		
		if (sUser.getCreateDate() != null) 
			sql.append(",?");
	
		// add last one for status
		sql.append(",?)");

		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++idx, sUser.getUserId());
			ps.setString(++idx, sUser.getProfileId());
			ps.setString(++idx, sUser.getAccountId());
			if (sUser.getRegisterSubmittalId() != null) {
				ps.setString(++idx, sUser.getRegisterSubmittalId());
			}
			if (sUser.getExpirationDate() != null) {
				ps.setDate(++idx, Convert.formatSQLDate(sUser.getExpirationDate()));
			}
			if (sUser.getCreateDate() != null) {
				ps.setDate(++idx, Convert.formatSQLDate(sUser.getCreateDate()));
			}
			ps.setString(++idx, sUser.getStatusCode());
			ps.execute();
			log.debug("inserted source user record for user_id|profile_id: " + sUser.getUserId() + "|" + sUser.getProfileId());
		} catch(SQLException sqle) {
			log.error("Error inserting source user, ", sqle);
			failedSourceUserInserts.put(sUser.getUserId(),sUser.getProfileId());
		}

	}
	
	/**
	 * Updates a SmartTRAK user's profile with their auth ID.
	 * @param dbConn
	 * @param sUser
	 */
	protected void updateSourceUserProfile(Connection dbConn, UserVO sUser) {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("AUTH").toString())) {
			ps.setString(++idx, sUser.getAuthenticationId());
			ps.setString(++idx, sUser.getProfileId());
			ps.execute();
			
		} catch(SQLException sqle) {
			failedSourceUserProfileUpdates.put(sUser.getUserId(),sUser.getProfileId());
		}
	}
	
	/**
	 * Updates a SmartTRAK user's authentication record with their original 
	 * SmartTRAK password if it exists.
	 * @param dbConn
	 * @param sUser
	 */
	protected void updateSourceUserAuthentication(Connection dbConn, UserVO sUser) {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("PWD").toString())) {
			ps.setString(++idx, sUser.getPassword());
			ps.setString(++idx, sUser.getPassword());
			ps.setString(++idx, sUser.getAuthenticationId());

			ps.execute();

		} catch(SQLException sqle) {
			failedSourceUserAuthenticationUpdates.put(sUser.getUserId(),sUser.getAuthenticationId());
		}
	}	

	/**
	 * Checks for invalid authentication record data.
	 * @param sUser
	 * @return
	 */
	protected boolean hasValidAuthentication(UserVO sUser) {
		return ! (StringUtil.isEmpty(sUser.getAuthenticationId()) ||
				StringUtil.isEmpty(sUser.getPassword()) || 
				"INVALID".equalsIgnoreCase(sUser.getPassword()));
	}
	
	/**
	 * Checks for the existence of a profile if no profileId was supplied for the user record.
	 * @param dbConn
	 * @param pm
	 * @param user
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void findProfile(Connection dbConn, ProfileManager pm, 
			UserVO user) throws DatabaseException {
			user.setProfileId(pm.checkProfile(user, dbConn));
	}
	
	/**
	 * Checks for an authentication for the user if the user has a valid email address
	 * and if a password was supplied in the user's source record.  If an authentication
	 * ID is not found, the authentication record is created.
	 * @param dataSet
	 * @param ul
	 * @param user
	 * @throws DatabaseException
	 */
	protected void processAuthentication(Map<String,Object> dataSet, 
			UserLogin ul, UserVO user) throws DatabaseException {
		String password = StringUtil.checkVal(dataSet.get(ImportField.password_txt.name()),null);
		if (password == null || user.getEmailAddress() == null) 
			return;
		
		user.setAuthenticationId(ul.checkAuth(user.getEmailAddress()));
		
		// if no auth record, create one, otherwise leave existing auth alone.
		if (user.getAuthenticationId() == null) {	
			/* We use the pwd passed in to the method, NOT the password on the user data VO
			 * so that we can come along behind and update the user's auth record with 
			 * the password that we set on the user data VO at a later time. */
			user.setAuthenticationId(ul.modifyUser(user.getAuthenticationId(), 
					user.getEmailAddress(), password, 0));
		}
	}

	/**
	 * Checks for the existence of a user profile ID on the user object.  If no 
	 * profile ID is found we attempt to create the profile.  If a profile ID is
	 * found we update the 'skipped profiles' count.
	 * @param dbConn
	 * @param pm
	 * @param user
	 * @param skipCnt
	 * @return
	 * @throws DatabaseException
	 */
	protected int processProfile(Connection dbConn, ProfileManager pm, 
			UserVO user, int skipCnt) throws DatabaseException {
		/* 2017-01-19: If profile doesn't exist, insert it.  Otherwise leave the existing 
		 * profile alone. */
		if (user.getProfileId() == null) {
			pm.updateProfile(user, dbConn);
			return skipCnt;
		} else {
			return skipCnt + 1;
		}
	}
	
	/**
	 * Checks to see if an 'allow communications flag' (a.k.a. opt-in flag) was supplied
	 * for this user.
	 * @param dbConn
	 * @param pm
	 * @param dataSet
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void processCommFlag(Connection dbConn, ProfileManager pm, 
			Map<String,Object> dataSet, String profileId) throws DatabaseException {
		String orgId = StringUtil.checkVal(dataSet.get(ImportField.organization_id.name()),null);
		String allowCommFlag = StringUtil.checkVal(dataSet.get(ImportField.allow_comm_flg.name()),null);
		
		if (orgId == null || allowCommFlag == null) 
			return;
		
		pm.assignCommunicationFlg(orgId, profileId, Convert.formatInteger(allowCommFlag), dbConn);
	}
	
	/**
	 * Checks to see if valid role ID and site ID were supplied on record, then checks
	 * for an existing role for this user.  If no role is found we add the role.
	 * @param dbConn
	 * @param prm
	 * @param dataSet
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void processRole(Connection dbConn, ProfileRoleManager prm, 
			Map<String,Object> dataSet, String profileId) throws DatabaseException {
		String newRoleId = StringUtil.checkVal(dataSet.get(ImportField.role_id.name()),null);
		String siteId = StringUtil.checkVal(dataSet.get(ImportField.site_id.name()),null);
		if (newRoleId == null || siteId == null) 
			return;
		
		/* Check for existing role for this user for this site? Returns profile role id (primary key)
		 * if role exists, null if not. */
		String currProfileRoleId = prm.checkRole(profileId, siteId, dbConn);

		log.info("Profile-role ID: " + currProfileRoleId);
		
		// create role VO for the insert or update
		SBUserRole userRole = new SBUserRole();
		userRole.setSiteId(siteId);
		userRole.setRoleId(newRoleId);
		userRole.setStatusId(20);
		userRole.setProfileId(profileId);
		
		// if found existing profile role id, set on vo so update occurs.
		if (currProfileRoleId != null) 
			userRole.setProfileRoleId(currProfileRoleId);

		// process role
		try {
			prm.addRole(userRole, dbConn);
			log.info("Processed profile role - profileId|roleId|siteId: " + profileId + "|" + newRoleId + "|" + siteId);
		} catch (Exception e) {
			log.error("Error processing profile role for this record number: ", e);
		}
	}
	
	/**
	 * Loads user divisions.
	 * @param dbConn
	 * @return
	 */
	protected Map<String,List<String>> loadUserDivisions(Connection dbConn) {
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("DIVS").toString())) {

			ResultSet rs = ps.executeQuery();
			return parseDivisions(rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving user divisions, ", sqle);
			return new HashMap<>();
		}
	}
	
	/**
	 * Parses the divisions query result set.
	 * @param rs
	 * @throws SQLException
	 */
	protected Map<String,List<String>> parseDivisions(ResultSet rs) throws SQLException {
		String prevId = null;
		String currId;
		List<String> divs = new ArrayList<>();
		Map<String,List<String>> divsMap = new HashMap<>();
		while (rs.next()) {
			currId = rs.getString("user_id");
			if (! currId.equals(prevId)) {
				// changed users
				if (prevId != null) 
					divsMap.put(prevId, divs);
			
				// 	init the divs List.
				divs = new ArrayList<>();
			}
			// add div to list
			divs.add(rs.getString("division_id"));
			prevId = currId;
		}
	
		// pick up the dangler
		if (prevId != null) 
			divsMap.put(prevId, divs);
		
		return divsMap;
	}

	/**
	 * Used to insert registration records for profiles with no email address.  If we try to push these through the API, 
	 * WC creates a new profile for this user in addition to creating the registration records.  This results in multiple
	 * duplicated profiles.  Additionally, the API does not return the profile ID that was created so we don't have
	 * a way of mapping the original source ID to the WC profile ID.
	 * @param dbConn
	 * @param record
	 * @param user
	 * @param userDivs
	 * @throws SQLException
	 */
	protected void insertRegistrationRecords(Connection dbConn, Map<String, Object> record, 
			UserVO user, List<String> userDivs) {
		int idx = 1;
		String regSubId = new UUIDGenerator().getUUID();
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("REGSUB").toString())) {
			ps.setString(idx++, regSubId);
			ps.setString(idx++, (String)record.get(ImportField.site_id.name()));
			ps.setString(idx++, props.getProperty("registerActionId"));
			ps.setString(idx++, user.getProfileId());
			ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
			ps.execute();
			// set the register submittal ID on an unused user field so we can get it later.
			user.setRegisterSubmittalId(regSubId);
		} catch (SQLException sqle) {
			log.error("Error inserting registration submittal record, ", sqle);
		}

		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("REGDATA").toString())) {
			for (RegistrationMap regKey : RegistrationMap.values()) {
				if (RegistrationMap.DIVISIONS.equals(regKey)) {
					formatDivisionInserts(ps,regKey,regSubId,userDivs);
				} else {
					formatRegistrationInsert(ps,regKey,regSubId, record.get(regKey.name().toLowerCase()));
				}
			}
			ps.executeBatch();
			log.debug("inserted registration records for user: " + user.getProfileId());

		} catch (Exception sqle) {
			log.error("Error inserting registration data records for user, ", sqle);
		}
	}
	
	/**
	 * Loops the user's divisions list and adds each record to the registration batch insert
	 * @param ps
	 * @param regKey
	 * @param regSubId
	 * @param userDivs
	 * @throws SQLException
	 */
	protected void formatDivisionInserts(PreparedStatement ps, RegistrationMap regKey, 
			String regSubId, List<String> userDivs) throws SQLException {
		if (userDivs == null || userDivs.isEmpty()) 
			return;
		
		for (String div : userDivs) {
			formatRegistrationInsert(ps,regKey,regSubId,div);
		}
	}
	
	/**
	 * Creates a record for insertion and adds it to the batch of records to insert.
	 * @param ps
	 * @param regKey
	 * @param regSubId
	 * @param regVal
	 * @throws SQLException
	 */
	protected void formatRegistrationInsert(PreparedStatement ps, RegistrationMap regKey, 
			String regSubId, Object regVal) throws SQLException {
		// check record val, only write reg records if have a value.
		String val = StringUtil.checkVal(regVal);
		if (val.isEmpty()) 
			return;
		
		int idx = 0;
		ps.setString(++idx, new UUIDGenerator().getUUID());
		ps.setString(++idx, regSubId);
		ps.setString(++idx, regKey.getFieldId());
		ps.setString(++idx, val);
		ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
		ps.addBatch();
	}
	
	/**
	 * Writes out log summaries.
	 * @param recordCnt
	 * @param successCnt
	 * @param failedCnt
	 * @param skipCnt
	 */
	protected void writeLogs(int recordCnt, int successCnt, int failedCnt, int skipCnt) {
		log.info(recordCnt + " total profile import records processed.");
		log.info(successCnt + " profile import records successfully processed.");
		log.info(failedCnt + " profile import records failed or were invalid.");
		log.info(skipCnt + " pre-existing profiles found.");

		/* output list of existing profiles found as they may be duplicates	 */
		if (! duplicateProfiles.isEmpty()) {
			log.info("Duplicate Profiles in Import Source: ");
			for (Map.Entry<String,String> rec : duplicateProfiles.entrySet()) {
				log.info(rec.getKey() + "|" + rec.getValue());
			}
			log.info("-----------------------------------------------\n\n");
		}

		if (! failedSourceUserInserts.isEmpty()) {
			log.info("Failed Source User Inserts: ");
			for (Map.Entry<String,String> fail : failedSourceUserInserts.entrySet()) {
				log.info(fail.getKey() + "|" + fail.getValue());
			}
			log.info("------------------------------------------------\n\n");
		}

		if (! failedSourceUserProfileUpdates.isEmpty()) {
			log.info("Failed Source User Profile Updates: ");
			for (Map.Entry<String,String> fail : failedSourceUserProfileUpdates.entrySet()) {
				log.info(fail.getKey() + "|" + fail.getValue());
			}
			log.info("-------------------------------------------------\n\n");
		}

		if (! failedSourceUserAuthenticationUpdates.isEmpty()) {
			log.info("Failed Source User Authentication Updates: ");
			for (Map.Entry<String,String> fail : failedSourceUserAuthenticationUpdates.entrySet()) {
				log.info(fail.getKey() + "|" + fail.getValue());
			}
			log.info("--------------------------------------------------");
		}

	}

	/**
	 * Sanitizes/cleans import data for certain fields
	 * @param records
	 */
	protected void sanitizeFieldData(Map<String,Object> record) {
		String country = StringUtil.checkVal(record.get(ImportField.country_cd.name()));
		String tmpVal = (String)record.get(ImportField.zip_cd.name());
		record.put(ImportField.zip_cd.name(),fixZipCode(tmpVal,country));
		tmpVal = (String)record.get(ImportField.main_phone_txt.name());
		record.put(ImportField.main_phone_txt.name(), stripPhoneExtension(tmpVal));
		tmpVal = (String)record.get(ImportField.mobile_phone_txt.name());
		record.put(ImportField.mobile_phone_txt.name(),stripPhoneExtension(tmpVal));
		tmpVal = (String)record.get(ImportField.first_nm.name());
		record.put(ImportField.first_nm.name(), checkNameField(tmpVal));
		tmpVal = (String)record.get(ImportField.last_nm.name());
		record.put(ImportField.last_nm.name(), checkNameField(tmpVal));
	}
	
	/**
	 * Fixes US zip codes that need a leading 0.
	 * @param zip
	 * @param country
	 * @return
	 */
	protected String fixZipCode(String zip, String country) {
		if (StringUtil.isEmpty(zip)) return zip;
		if ("US".equalsIgnoreCase(country) &&
				StringUtil.checkVal(zip).length() == 4) {
					return "0" + zip;
		}
		return zip;
	}
	
	/**
	 * Strips out any extension text that was included as part of a phone number
	 * and strips out alphanumerics.
	 * e.g.
	 * 		123-456-7890 ext 123 ('ext 123' is removed)
	 * 		123-456-7890, xt 456 (', xt 456' is removed)
	 * 		123-456-7890,9999 (',9999' is removed)
	 * 
	 * @param phone
	 * @param country
	 * @return
	 */
	protected String stripPhoneExtension(String phone) {
		if (StringUtil.isEmpty(phone)) 
			return phone;
		
		String tmpPhone = phone.toLowerCase();
		int idx = tmpPhone.indexOf(',');
		if (idx == -1) {
			idx = tmpPhone.indexOf('e');
			if (idx == -1) 
				idx = tmpPhone.indexOf('x');
		}
		if (idx > -1)
			tmpPhone = tmpPhone.substring(0, idx);
		
		return StringUtil.removeNonNumeric(tmpPhone);
		
	}
	
	/**
	 * Cleans name fields
	 * @param val
	 * @return
	 */
	protected String checkNameField(String val) {
		if (StringUtil.isEmpty(val)) 
			return val;

		String tmpVal = val;
		if (tmpVal.indexOf('"') > -1)
			tmpVal = tmpVal.replace("\"", "");

		if (tmpVal.indexOf(',') > -1) 
			tmpVal = tmpVal.substring(0,tmpVal.indexOf(','));

		return tmpVal;
	}
	
	/**
	 * Creates map of query statements that have fixed number of
	 * fields so that the queries can be reused without re-creating
	 * thousands of times.
	 * @return
	 */
	protected Map<String,StringBuilder> initQueryStatements() {
		Map<String,StringBuilder> sql = new HashMap<>();

		StringBuilder tmp = new StringBuilder(100);
		tmp.append("update profile set authentication_id = ? ");
		tmp.append("where profile_id = ?");
		sql.put("AUTH",tmp);

		tmp = new StringBuilder(200);
		tmp.append("update authentication set password_txt = ?, ");
		tmp.append("password_history_txt = ? ");
		tmp.append("where authentication_id = ?");
		sql.put("PWD",tmp);

		// query for user divisions
		tmp = new StringBuilder(200);
		tmp.append("select user_id, division_id ");
		tmp.append("from biomedgps.profiles_user_divisions ");
		tmp.append("order by user_id");
		sql.put("DIVS",tmp);

		tmp = new StringBuilder(200);
		tmp.append("insert into register_submittal (register_submittal_id, site_id, action_id, ");
		tmp.append("profile_id, create_dt) values (?,?,?,?,?)");
		sql.put("REGSUB",tmp);

		tmp = new StringBuilder(200);
		tmp.append("insert into register_data (register_data_id, register_submittal_id, ");
		tmp.append("register_field_id, value_txt, create_dt) values (?,?,?,?,?)");
		sql.put("REGDATA",tmp);

		return sql;
	}
	
	/**
	 * 
	 * @return
	 */
	protected StringBuilder buildMainQuery() {
		StringBuilder sql = new StringBuilder(1375);
		sql.append("select cast(id as varchar) AS SMARTTRAK_ID, username AS SMARTTRAK_USER_NM, ");
		sql.append("first_name AS FIRST_NM, last_name AS LAST_NM, lower(email) AS EMAIL_ADDRESS_TXT,  ");
		sql.append("address AS ADDRESS_TXT, address1 AS ADDRESS2_TXT, city AS CITY_NM, state AS STATE_CD,  ");
		sql.append("zip_code AS ZIP_CD, country as COUNTRY_CD,phone_number AS MAIN_PHONE_TXT,  ");
		sql.append("mobile_number AS MOBILE_PHONE_TXT,is_active AS ACTIVE, is_staff AS STAFF,  ");
		sql.append("is_superuser AS SUPER_USER, ");
		sql.append("case when is_active='TRUE' then case when is_staff='TRUE' and status = 'a' then 'S' ");
		sql.append("else upper(status) end else upper(status) end as STATUS, ");
		sql.append("date_joined as DATE_JOINED, ");
		sql.append("expiration as DATE_EXPIRATION,username AS USERNAME, password AS SMARTTRAK_PASSWORD_TXT, ");
		sql.append("title AS TITLE, cast(account_id as varchar) as ACCOUNT_ID,update_frequency AS UPDATES,  ");
		sql.append("fav_frequency AS FAVORITEUPDATES, company as COMPANY,company_url as COMPANYURL, ");
		sql.append("biomedgps.profiles_user.\"source\" as \"SOURCE\",date_demoed as DEMODT, ");
		sql.append("date_trained as TRAININGDT,date_training_initial as INITTRAININGDT, ");
		sql.append("date_training_advanced as ADVTRAININGDT,date_training_other as OTHERTRAININGDT, ");
		sql.append("cast(job_category_id as varchar) as JOBCATEGORY, cast(job_level_id as varchar) as JOBLEVEL, ");
		sql.append("cast(industry_id as varchar) as INDUSTRY, ");
		sql.append("regexp_replace(quick_notes, E'[\\n\\r\\f\\t\\v]+', ';;', 'g') as NOTES, ");
		sql.append("case when is_active='TRUE' then ");
		sql.append("case when is_staff='TRUE' then '3eef678eb39e87277f000101dfd4f140' ");
		sql.append("else '10' end else null end as ROLE_ID, ");
		sql.append("'wc1mp0rt' AS PASSWORD_TXT, 'BMG_SMARTTRAK' as ORGANIZATION_ID, ");
		sql.append("'BMG_SMARTTRAK_1' as SITE_ID, '1' as ALLOW_COMM_FLG ");
		sql.append("from biomedgps.profiles_user ");
		
		// TODO DEBUG filter - remove after testing
		//sql.append("where COMPANY = 'Biomedgps' ");
		
		sql.append("order by active desc, id; ");
		return sql;
	}

}
