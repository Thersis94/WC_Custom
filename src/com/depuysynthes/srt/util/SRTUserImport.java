package com.depuysynthes.srt.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;

import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.depuysynthes.srt.SRTRosterVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title:</b> SRTUserImport.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 8, 2018
 ****************************************************************************/
public class SRTUserImport extends CommandLineUtil {

	private static final String SOURCE_FILE_CONFIG="scripts/srt/user_import_config.properties";
	private static final String SOURCE_FILE_LOG="scripts/srt/user_import_log4j.properties";

	// profile header vals
	private Map<String,StringBuilder> queries;
	private Map<String,String> duplicateProfiles;
	private Map<String,String> processedProfiles;
	private Map<String,String> failedSourceUserInserts;
	private Map<String,String> failedSourceUserProfileUpdates;
	private Map<String,String> failedSourceUserAuthenticationUpdates;
	private long startTimeInMillis;
	private String schema;
	private String siteId;
	private String orgId;
	private String registerId;
	private String registeredRole;
	private String adminRole;
	private String viewOnlyRole;
	private String opCoId;

	enum ImportField {
		ACCOUNT_NO,
		IS_ACTIVE,
		EMAIL_ADDRESS_TXT,
		FIRST_NM,
		LAST_NM,
		ADDRESS_TXT,
		CITY_NM,
		STATE_CD,
		ZIP_CD,
		PASSWORD_TXT,
		ROLE_TXT,
		WORKGROUP_ID,
		ALLOW_COMM_FLG,
		TERRITORY_ID,
		REGION_ID,
		AREA_ID,
		MOBILE_PHONE_TXT,
		OP_CO_ID,
		IS_ADMIN
	}

	/**
	 * @param args
	 */
	public SRTUserImport(String[] args) {
		super(args);
		PropertyConfigurator.configure(SOURCE_FILE_LOG);
		queries = initQueryStatements();
		duplicateProfiles = new LinkedHashMap<>();
		processedProfiles = new LinkedHashMap<>();
		failedSourceUserInserts = new LinkedHashMap<>();
		failedSourceUserProfileUpdates = new LinkedHashMap<>();
		failedSourceUserAuthenticationUpdates = new LinkedHashMap<>();
	}

	public static void main(String[] args) {
		SRTUserImport udi = new SRTUserImport(args);
		udi.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		startTimeInMillis = Calendar.getInstance().getTimeInMillis();

		// load props
		loadProperties(SOURCE_FILE_CONFIG);

		populatePropVars();

		// get dbconn
		loadDBConnection(props);

		// retrieve records
		log.info("loading records");
		List<Map<String,Object>> records = retrieveData();
		log.info("records retrieved: " + records.size());
		try {
			dbConn.setAutoCommit(false);

			insertRecords(records);

			dbConn.commit();
			dbConn.setAutoCommit(true);
		} catch(Exception e) {
			log.error("Error, failed to insert records, ", e);
		}

		// clean up
		closeDBConnection();
	}

	/**
	 * 
	 */
	private void populatePropVars() {
		orgId = props.getProperty(OrganizationAction.ORGANIZATION_ID);
		siteId = props.getProperty(SiteAction.SITE_ID);
		schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		registerId = props.getProperty("registerActionId");
		registeredRole = props.getProperty("registeredRole");
		adminRole = props.getProperty("adminRole");
		viewOnlyRole = props.getProperty("viewOnlyRole");
		opCoId = props.getProperty("opCoId");
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
		config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptionKey"));
		config.put(Constants.CFG_PASSWORD_SALT, props.getProperty("passwordSalt"));

		// init profile managers
	    ProfileManager pm = new SBProfileManager(config);
		ProfileRoleManager prm = new ProfileRoleManager();
		UserLogin ul = new UserLogin(dbConn, config);

		// init vars
		int recordCnt = 0;
		int successCnt = 0;
		int failedCnt = 0;
		int skipCnt = 0;
		SRTRosterVO user;
		Map<String,Object> dataSet;

		//iterate the records, inserting each
		Iterator<Map<String,Object>> iter = records.iterator();
		int comLevel = 1000;
		while (iter.hasNext()) {
			recordCnt++;
			dataSet = iter.next();

			// clean up certain values before we import them.
			sanitizeFieldData(dataSet);

			// init site user from import source
			user = initUser(dataSet);

			log.info("START: processing record|source: " + recordCnt + "|" + user.getRosterId());
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
				log.error("Error processing source ID " + user.getRosterId() + ", ", ex);
			}

			// if valid profile, insert reg records, create biomedgps user, update profile/auth.
			if (user.getProfileId() != null) {
				successCnt++;
				insertRegistrationRecords(dbConn, dataSet, user);
				insertSourceUser(dbConn, user);
				// if valid auth record, update profile/auth
				if (hasValidAuthentication(user)) {
					updateSourceUserProfile(dbConn, user);
					updateSourceUserAuthentication(dbConn, user);
				}
				log.info("END: inserting for record|source: " + recordCnt + "|" + user.getRosterId());
			} else {
				// if we couldn't successfully create a profile, add to failed count.
				logInvalidUserRecord(recordCnt, user);
				failedCnt++;
			}

			//Commit Every 1000 Records.
			if(recordCnt % comLevel == 0) {
			//	dbConn.commit();
			}
		}

		//Commit Hanging Records.
		//dbConn.commit();

		// log summary info.
		writeLogs(recordCnt,successCnt,failedCnt,skipCnt);
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
			SRTRosterVO user) {
		int idx = 1;
		String regSubId = new UUIDGenerator().getUUID();
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("REGSUB").toString())) {
			ps.setString(idx++, regSubId);
			ps.setString(idx++, siteId);
			ps.setString(idx++, registerId);
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
				formatRegistrationInsert(ps,regKey,regSubId, record.get(regKey.name()));
			}
			ps.executeBatch();
			log.debug("inserted registration records for user: " + user.getProfileId());

		} catch (Exception sqle) {
			log.error("Error inserting registration data records for user, ", sqle);
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
	protected void formatRegistrationInsert(PreparedStatement ps, RegistrationMap regKey, String regSubId, Object regVal) throws SQLException {
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
	 * Checks for the existence of a profile if no profileId was supplied for the user record.
	 * @param dbConn
	 * @param pm
	 * @param user
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void findProfile(Connection dbConn, ProfileManager pm, SRTRosterVO user) throws DatabaseException {
			user.setProfileId(pm.checkProfile(user, dbConn));
	}

	/**
	 * Check for duplicate profile.
	 * @param user
	 * @param recordCnt
	 * @return
	 */
	protected boolean isDuplicateProfile(SRTRosterVO user, int recordCnt) {
		if (user.getProfileId() != null) {
			if (processedProfiles.containsValue(user.getProfileId())) {
				log.info("END: IS DUPLICATE PROFILE, skipping record|userId: " + recordCnt + "|" + user.getRosterId());
				// add profile ID to dupes map.
				duplicateProfiles.put(user.getRosterId(),user.getProfileId());
				return true;
			} else {
				processedProfiles.put(user.getRosterId(), user.getProfileId());
				return false;
			}
		}
		return false;
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
			SRTRosterVO user, int skipCnt) throws DatabaseException {
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
	 * Inserts the SmartTRAK source user record.
	 * @param dbConn
	 * @param sUser
	 */
	protected void insertSourceUser(Connection dbConn, SRTRosterVO sUser) {

		try {
			new DBProcessor(dbConn, schema).save(sUser);
			log.debug("inserted source user record for user_id|profile_id: " + sUser.getRosterId() + "|" + sUser.getProfileId());
		} catch(InvalidDataException | com.siliconmtn.db.util.DatabaseException sqle) {
			log.error("Error inserting source user, ", sqle);
			failedSourceUserInserts.put(sUser.getRosterId(),sUser.getProfileId());
		}
	}

	/**
	 * Updates a SmartTRAK user's profile with their auth ID.
	 * @param dbConn
	 * @param sUser
	 */
	protected void updateSourceUserProfile(Connection dbConn, SRTRosterVO sUser) {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("AUTH").toString())) {
			ps.setString(++idx, sUser.getAuthenticationId());
			ps.setString(++idx, sUser.getProfileId());
			ps.execute();
			
		} catch(SQLException sqle) {
			failedSourceUserProfileUpdates.put(sUser.getRosterId(),sUser.getProfileId());
		}
	}

	/**
	 * Updates a SmartTRAK user's authentication record with their original 
	 * SmartTRAK password if it exists.
	 * @param dbConn
	 * @param sUser
	 */
	protected void updateSourceUserAuthentication(Connection dbConn, SRTRosterVO sUser) {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("PWD").toString())) {
			ps.setString(++idx, sUser.getPassword());
			ps.setString(++idx, sUser.getAuthenticationId());

			ps.execute();

		} catch(SQLException sqle) {
			failedSourceUserAuthenticationUpdates.put(sUser.getRosterId(),sUser.getAuthenticationId());
		}
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
			UserLogin ul, SRTRosterVO user) throws DatabaseException {
		String password = StringUtil.checkVal(dataSet.get(ImportField.PASSWORD_TXT.name()),null);
		if (password == null || user.getEmailAddress() == null) 
			return;
		
		user.setAuthenticationId(ul.checkAuth(user.getEmailAddress()));
		
		// if no auth record, create one, otherwise leave existing auth alone.
		if (user.getAuthenticationId() == null) {	
			/* We use the pwd passed in to the method, NOT the password on the user data VO
			 * so that we can come along behind and update the user's auth record with 
			 * the password that we set on the user data VO at a later time. */
			user.setAuthenticationId(ul.saveAuthRecord(user.getAuthenticationId(), 
					user.getEmailAddress(), password, 0));
		}
	}

	/**
	 * Checks for invalid authentication record data.
	 * @param sUser
	 * @return
	 */
	protected boolean hasValidAuthentication(SRTRosterVO sUser) {
		return ! (StringUtil.isEmpty(sUser.getAuthenticationId()) ||
				StringUtil.isEmpty(sUser.getPassword()) || 
				"INVALID".equalsIgnoreCase(sUser.getPassword()));
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
		String allowCommFlag = StringUtil.checkVal(dataSet.get(ImportField.ALLOW_COMM_FLG.name()));
		
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
		String newRoleId = StringUtil.checkVal(dataSet.get(ImportField.ROLE_TXT.name()),null);
		String siteId = props.getProperty(SiteAction.SITE_ID);
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
	 * 
	 * @param recordCnt
	 * @param user
	 */
	protected void logInvalidUserRecord(int recordCnt, SRTRosterVO user) {
		log.info("END: INVALID record, DID NOT insert for record|source: " + recordCnt + "|" + user.getRosterId());
	}

	/**
	 * Initializes a UserVO from the dataset map passed in.
	 * @param dataSet
	 * @return
	 */
	protected SRTRosterVO initUser(Map<String,Object> dataSet) {
		SRTRosterVO user = new SRTRosterVO();
		user.setData(dataSet);
		user.setPassword(StringUtil.checkVal(dataSet.get(ImportField.PASSWORD_TXT.name()),null));
		user.setIsAdmin(Convert.formatBoolean(dataSet.get(ImportField.IS_ADMIN.name())));
		user.setWorkgroupId(StringUtil.checkVal(dataSet.get(ImportField.WORKGROUP_ID.name())));
		user.setIsActive(Convert.formatBoolean(dataSet.get(ImportField.IS_ACTIVE.name())));
		user.setCompanyRole(StringUtil.checkVal(dataSet.get(ImportField.ROLE_TXT.name())));
		user.setOpCoId(StringUtil.checkVal(dataSet.get(ImportField.OP_CO_ID.name())));

		if(!user.isActive()) {
			user.setDeactivatedDt(Convert.getCurrentTimestamp());
		}

		user.setTerritory(StringUtil.checkVal(dataSet.get(ImportField.TERRITORY_ID.name())));
		user.setRegion(StringUtil.checkVal(dataSet.get(ImportField.REGION_ID.name())));
		user.setArea(StringUtil.checkVal(dataSet.get(ImportField.AREA_ID.name())));
		user.setAccountNo(StringUtil.checkVal(dataSet.get(ImportField.ACCOUNT_NO.name())));

		// parse user's email.
		parseUserEmail(user, (String)dataSet.get(ImportField.EMAIL_ADDRESS_TXT.name()));

		return user;
	}

	/**
	 * Checks user's email address.  If email address is invalid, the user's legacy 
	 * username is checked.  If the username is not a valid email address, 
	 * the user's email address is set to null.
	 * @param user
	 * @param originalUserName
	 */
	protected void parseUserEmail(SRTRosterVO user, String legacyUserName) {
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
	 * Sanitizes/cleans import data for certain fields
	 * @param records
	 */
	protected void sanitizeFieldData(Map<String,Object> record) {
		String tmpVal = StringUtil.checkVal(record.get(ImportField.ROLE_TXT.name()));
		record.put(ImportField.ROLE_TXT.name(), checkRole(tmpVal));
		tmpVal = (String) record.get(ImportField.IS_ACTIVE.name());
		record.put(ImportField.IS_ACTIVE.name(), Convert.formatInteger(Convert.formatBoolean(tmpVal)));
		tmpVal = (String) record.get(ImportField.FIRST_NM.name());
		record.put(ImportField.FIRST_NM.name(), cleanName(tmpVal));
	}

	/**
	 * @param tmpVal
	 * @return
	 */
	private Object cleanName(String tmpVal) {

		//Check if this is last, first notation.  If so, swap values.
		if(!StringUtil.isEmpty(tmpVal) && tmpVal.indexOf(',') > -1) {
			String last = tmpVal.substring(0, tmpVal.indexOf(','));
			String first = tmpVal.substring(tmpVal.indexOf(',') + 1, tmpVal.length());
			return new StringBuilder(50).append(first).append(" ").append(last).toString();
		}
		return tmpVal;
	}

	/**
	 * Convert the Role Numbers to something more WC Compatible
	 * @param tmpVal
	 * @return
	 */
	private Object checkRole(String tmpVal) {
		switch(Convert.formatInteger(tmpVal)) {
			case 5:
				return registeredRole;
			case 7:
				return adminRole;
			default:
				return viewOnlyRole;
				
		}
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
				record.put(field.name(), rs.getObject(field.name().toLowerCase()));
			}
			records.add(record);
		}
		return records;
	}

	/**
	 * @return
	 */
	protected StringBuilder buildMainQuery() {
		StringBuilder megaQuery = new StringBuilder(2200);
		megaQuery.append(buildAdminUserQuery());
		megaQuery.append(" union ");
		megaQuery.append(buildSalesRosterQuery());
		megaQuery.append(" union ");
		megaQuery.append(buildProjectUserQuery());
		return megaQuery;
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
		tmp.append("update authentication set password_txt = ? ");
		tmp.append("where authentication_id = ?");
		sql.put("PWD",tmp);

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
		log.info(duplicateProfiles.size() + " duplicate profiles found.");
		log.info("Elapsed time: " + (Calendar.getInstance().getTimeInMillis() - startTimeInMillis)/1000 + " seconds.");

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

	/* (non-Javadoc)
	 * @see com.depuysynthes.srt.util.SRTUserImport#buildMainQuery()
	 */
	protected StringBuilder buildProjectUserQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select distinct ");
		sql.append("case when salesrep like '%,%' then trim(split_part(salesrep, ',', 1)) ");
		sql.append("when salesrep not like '%,%' then trim(split_part(salesrep, ' ', 1)) ");
		sql.append("else salesrep end as FIRST_NM, ");
		sql.append("case when salesrep like '%,%' then trim(split_part(salesrep, ',', 2)) ");
		sql.append("when salesrep not like '%,%' then trim(split_part(salesrep, ' ', 2)) end as LAST_NM, ");
		sql.append("0 as allow_comm_flg, ");
		sql.append("null as wwid, ");
		sql.append("'0' as IS_ACTIVE, ");
		sql.append("'0' as ROLE_TXT, ");
		sql.append("null as ADDRESS_TXT, ");
		sql.append("null as CITY_NM, ");
		sql.append("null as STATE_CD, ");
		sql.append("null as ZIP_CD, ");
		sql.append("case when salesrepemail like '%@%' then salesrepemail ");
		sql.append("when salesrepemail not like '%@%' then concat(replace(salesrep, ' ', ''), '@srt.com') end ");
		sql.append("as EMAIL_ADDRESS_TXT, ");
		sql.append("'6' as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("case when salesrepcellphone is not null and salesrepcellphone != '' ");
		sql.append("then salesrepcellphone ");
		sql.append("when salesreppager is not null and salesreppager != '' ");
		sql.append("then salesreppager else null end as MOBILE_PHONE_TXT, ");
		sql.append("cast(territoryid as varchar) as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("0 as is_admin, ");
		sql.append("replace(core.newId(), '-', '') as ACCOUNT_NO ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.projects ");
		sql.append("where salesrep not in ");
		sql.append("(select concat(s.last_name, ', ', s.first_name) ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster s) ");
		sql.append("and salesrep not in ");
		sql.append("(select concat(s.first_name, ' ', s.last_name) ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster s) ");
		sql.append("and salesrep != '' ");
		return sql;
	}

	protected StringBuilder buildSalesRosterQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select ");
		sql.append("first_name as first_nm, ");
		sql.append("last_name as last_nm, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("wwid, ");
		sql.append("'1' as IS_ACTIVE, ");
		sql.append("'5' as ROLE_TXT, ");
		sql.append("address as ADDRESS_TXT, ");
		sql.append("city as CITY_NM, ");
		sql.append("state as STATE_CD, ");
		sql.append("zip as ZIP_CD, ");
		sql.append("email as EMAIL_ADDRESS_TXT, ");
		sql.append("'8' as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("cell_phone as MOBILE_PHONE_TXT, ");
		sql.append("cast(territoryid as varchar) as TERRITORY_ID, ");
		sql.append("cast(region as varchar) as REGION_ID, ");
		sql.append("cast(area as varchar) as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("0 as is_admin, ");
		sql.append("cast(id as varchar) as ACCOUNT_NO ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster ");

		return sql;
	}

	protected StringBuilder buildAdminUserQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select ");
		sql.append("userfirstname as first_nm, ");
		sql.append("userlastname as last_nm, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("wwid as WWID, ");
		sql.append("case when status = 'Active' then '1' else '0' end as IS_ACTIVE, ");
		sql.append("cast (role as varchar) as ROLE_TXT, ");
		sql.append("null as ADDRESS_TXT, ");
		sql.append("null as CITY_NM, ");
		sql.append("null as STATE_CD, ");
		sql.append("null as ZIP_CD, ");
		sql.append("lower(emailaddresstxt) as EMAIL_ADDRESS_TXT, ");
		sql.append("cast(workgroupid as varchar) as workgroup_id, ");
		sql.append("userpassword as PASSWORD_TXT, ");
		sql.append("null as MOBILE_PHONE_TXT, ");
		sql.append("'-1' as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("1 as is_admin, ");
		sql.append("cast(userid as varchar) as ACCOUNT_NO ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.users ");
		return sql;
	}
}