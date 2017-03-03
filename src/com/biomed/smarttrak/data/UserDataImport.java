package com.biomed.smarttrak.data;

// Java 7
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;

//SMTBaseLibs
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.admin.action.data.SiteUserVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.db.ProfileImport;
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
public class UserDataImport extends ProfileImport {

	// import env params
	private static final String SOURCE_FILE_PATH="/data/SMT/accounts/SmartTRAK/smarttrak/user-import/test/smarttrak-TEST-2017-03-03.csv";
	private static final String GEOCODE_CLASS="com.siliconmtn.gis.SMTGeocoder";
	private static final String GEOCODE_URL="http://localhost:9000/websvc/geocoder";
	// WC params
	private static final String REGISTRATION_PAGE_URL = "http://smarttrak.siliconmtn.com/my-account";
	private static final String REG_PMID = "6d9674d8b7dc54077f0001019b2cb979";
	private static final String REG_ACTION_ID = "ea884793b2ef163f7f0001011a253456";
	// profile header vals
	private static final String FIRST_NM = "FIRST_NM";
	private static final String LAST_NM = "LAST_NM";
	private static final String ZIP_CD = "ZIP_CD";
	private static final String MAIN_PHONE_TXT = "MAIN_PHONE_TXT";
	private static final String MOBILE_PHONE_TXT = "MOBILE_PHONE_TXT";
	private Map<String,String> regFieldMap;

	public UserDataImport() {
		super();
		regFieldMap = createRegFieldMap();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {        
        UserDataImport db = new UserDataImport();
		try {
			log.info("importFile=" + SOURCE_FILE_PATH);
			List<Map<String,String>> data = db.parseFile(SOURCE_FILE_PATH);
			db.insertRecords(data);
		} catch (Exception e) {
			log.error("Error Processing ... " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param records
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void insertRecords(List<Map<String,String>> records) throws Exception {
		log.debug("inserting records..., records list size is: " + records.size());
		int recordCnt = 0;
		int successCnt = 0;
		int failedCnt = 0;
		int skipCnt = 0;
		String sourceId;
		//Open DB Connection
		Connection dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
		
		List<String> profileIds = new ArrayList<>();
		List<UserVO> sourceUsers = new ArrayList<>();
		Map<String, Object> config = new HashMap<>();
		config.put(Constants.ENCRYPT_KEY, encKey);
		config.put(Constants.GEOCODE_CLASS, GEOCODE_CLASS);
		config.put(Constants.GEOCODE_URL, GEOCODE_URL);
	    ProfileManager pm = new SBProfileManager(config);
		ProfileRoleManager prm = new ProfileRoleManager();
		UserLogin ul = new UserLogin(dbConn, encKey);
		SiteUserVO user;
		Map<String,Object> dataSet;
		String tmpField1;
		String tmpField2;
		//iterate the records, inserting each
		Iterator iter = records.iterator();
		while (iter.hasNext()) {
			recordCnt++;
			// populate user data vo
			user = new SiteUserVO();
			dataSet = (Map<String,Object>) iter.next();
			sourceId = (String)dataSet.get("SMARTTRAK_ID");
			
			// clean up certain values before we use them.
			sanitizeFieldData(dataSet);
			user.setData(dataSet);

			// check email address
			if (!StringUtil.isValidEmail(user.getEmailAddress())) {
				user.setEmailAddress(null);
				log.warn("Invalid email found, setting to null for source id|email: " + sourceId + "|" + user.getEmailAddress());
			} else {
				user.setValidEmailFlag(1);
			}
			
			try {
				/* check for pre-existing user profile */
				tmpField1 = StringUtil.checkVal(dataSet.get("PROFILE_ID"),null);
				findProfile(dbConn, pm, user, tmpField1);
				
				/* Process a password if supplied for the user */
				tmpField1 = StringUtil.checkVal(dataSet.get("PASSWORD_TXT"),null);
				checkForPassword(ul, user,tmpField1);

				/* create a profile if appropriate */
				createProfile(dbConn, pm, user, skipCnt);
				
				/* If an org ID and comm flag were supplied, opt-in this user for the given org.	 */
				tmpField1 = StringUtil.checkVal(dataSet.get("ORGANIZATION_ID"),null);
				tmpField2 = StringUtil.checkVal(dataSet.get("ALLOW_COMM_FLG"),null);
				checkForCommFlag(dbConn, pm, user.getProfileId(), tmpField1, tmpField2);

				/* Add profile roles for this user for the specified site ID. */
				tmpField1 = StringUtil.checkVal(dataSet.get("ROLE_ID"),null);
				tmpField2 = StringUtil.checkVal(dataSet.get("SITE_ID"),null);
				checkForRole(dbConn, prm, user.getProfileId(), tmpField1, tmpField2);
				
			} catch(Exception ex) {
				log.error("Error processing source ID " + sourceId + ", " + ex.getMessage());
			}
			
			//increment our counters
			if (user.getProfileId() != null) {
				successCnt++;
				profileIds.add(user.getProfileId());
				sourceUsers.add(createSourceUser(dataSet,sourceId,user.getProfileId()));
				try {
					insertRegistrationRecords(dbConn, dataSet, user);
				} catch (Exception e) {
					log.error("Error inserting registration records for this record: " + recordCnt);
				}
			}
		}
		
		log.debug(recordCnt + " total profile import records processed.");
		log.debug(successCnt + " profile import records successfully processed.");
		log.debug(failedCnt + " profile import records failed.");
		log.debug(skipCnt + " pre-existing profiles found.");
		
		//close DB Connection
		this.closeConnection(dbConn);
		
		/* Output inserts for the source ID mapped to the WC profile ID found/created. */
		for (UserVO sUser : sourceUsers) {
			StringBuilder sql = new StringBuilder(200);
			sql.append("insert into custom.biomedgps_user (user_id, profile_id, register_submittal_id, status_cd, expiration_dt, create_dt) values (");
			sql.append("'").append(sUser.getUserId()).append("',");
			sql.append("'").append(sUser.getProfileId()).append("',");
			sql.append("'").append(sUser.getRegisterSubmittalId()).append("',");
			sql.append("'").append(sUser.getStatusCode().toUpperCase()).append("',");
			sql.append("'").append(Convert.formatDate(sUser.getExpirationDate(),Convert.DATE_TIME_DASH_PATTERN)).append("',");
			sql.append("'").append(Convert.formatDate(sUser.getCreateDate(),Convert.DATE_TIME_DASH_PATTERN)).append("',");
			sql.append(");");
			log.info(sql.toString());
		}
	}
	
	/**
	 * Creates a native UserVO and maps it to the user's WC profile ID.
	 * @param record
	 * @param sourceId
	 * @param profileId
	 * @return
	 */
	protected UserVO createSourceUser(Map<String,Object> record, String sourceId, String profileId) {
		UserVO user = new UserVO();
		user.setUserId(sourceId);
		user.setProfileId(profileId);
		user.setStatusCode(StringUtil.checkVal(record.get("STATUS"),null));
		user.setCreateDate(Convert.formatDate(StringUtil.checkVal(record.get("DATE_JOINED"),null)));
		user.setExpirationDate(Convert.formatDate(StringUtil.checkVal(record.get("DATE_EXPIRATION"),null)));
		user.setPassword(StringUtil.checkVal("SMARTTRAK_PASSWORD_TXT",null));
		return user;
	}
	
	/**
	 * Checks for the existence of a profile if no profileId was supplied for the user record.
	 * @param dbConn
	 * @param pm
	 * @param user
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void findProfile(Connection dbConn, ProfileManager pm, SiteUserVO user, 
			String profileId) throws DatabaseException {
		if (profileId == null)  
			user.setProfileId(pm.checkProfile(user, dbConn));
	}
	
	/**
	 * Checks for existence of a password for the user record and attempts to create one
	 * if password was supplied.
	 * @param ul
	 * @param user
	 * @param password
	 * @throws DatabaseException
	 */
	protected void checkForPassword(UserLogin ul, SiteUserVO user, 
			String password) throws DatabaseException {
		if (password == null) return;
		user.setAuthenticationId(ul.checkAuth(user.getEmailAddress()));
		if (user.getAuthenticationId() == null) {
			//pwd will be encrypted at qry, 0 sets password reset flag to false
			user.setAuthenticationId(ul.modifyUser(user.getAuthenticationId(), 
					user.getEmailAddress(), user.getPassword(), 0));
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
	protected int createProfile(Connection dbConn, ProfileManager pm, 
			SiteUserVO user, int skipCnt) throws DatabaseException {
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
	 * @param profileId
	 * @param orgId
	 * @param allowCommFlag
	 * @throws DatabaseException
	 */
	protected void checkForCommFlag(Connection dbConn, ProfileManager pm, String profileId, 
			String orgId, String allowCommFlag) throws DatabaseException {
		if (orgId == null || allowCommFlag == null) return;
		pm.assignCommunicationFlg(orgId, profileId, Convert.formatInteger(allowCommFlag), dbConn);
	}
	
	/**
	 * Checks to see if valid role ID and site ID were supplied on record, then checks
	 * for an existing role for this user.  If no role is found we add the role.
	 * @param dbConn
	 * @param prm
	 * @param profileId
	 * @param roleId
	 * @param siteId
	 * @throws DatabaseException
	 */
	protected void checkForRole(Connection dbConn, ProfileRoleManager prm, String profileId, 
			String roleId, String siteId) throws DatabaseException {
		if (roleId == null || siteId == null) return;
		
		if (! prm.roleExists(profileId, siteId, roleId, dbConn)) {
			SBUserRole userRole = new SBUserRole();
			userRole.setSiteId(siteId);
			userRole.setRoleId(roleId);
			userRole.setStatusId(20);
			userRole.setProfileId(profileId);
			try {
				prm.addRole(userRole, dbConn);
			} catch (Exception e) {
				log.error("Error: Cannot add role for this record number: ", e);
			}
		}
	}

	/**
	 * 
	 * @param dbConn
	 * @param record
	 * @param user
	 * @throws Exception
	 */
	protected void insertRegistrationRecords(Connection dbConn, Map<String,Object> record, SiteUserVO user) 
			throws Exception {
		if (user.getEmailAddress() == null) {
			// insert reg records manually because lack of an email will cause another profile to be created by SubmittalAction
			insertRegistrationRecordsManually(dbConn, record, user);
		} else {
			insertRegistrationRecordsViaForm(record);
		}
	}

	/**
	 * Used to insert registration records for profiles with no email address.  If we try to push these through the API, 
	 * WC creates a new profile for this user in addition to creating the registration records.  This results in multiple
	 * duplicated profiles.  Additionally, the API does not return the profile ID that was created so we don't have
	 * a way of mapping the original source ID to the WC profile ID.
	 * @param dbConn
	 * @param record
	 * @param user
	 * @throws Exception
	 */
	protected void insertRegistrationRecordsManually(Connection dbConn, Map<String, Object> record, 
			SiteUserVO user) throws Exception {
		log.debug("insertRegistrationRecordsManually...");
		StringBuilder regSub = new StringBuilder(122);
		regSub.append("insert into register_submittal (register_submittal_id, site_id, action_id, ");
		regSub.append("profile_id, create_dt) values (?,?,?,?,?)");
		int idx = 1;
		String regSubId = new UUIDGenerator().getUUID();
		try (PreparedStatement ps = dbConn.prepareStatement(regSub.toString())) {
			ps.setString(idx++, regSubId);
			ps.setString(idx++, (String)record.get("SITE_ID"));
			ps.setString(idx++, REG_ACTION_ID);
			ps.setString(idx++, user.getProfileId());
			ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
			ps.execute();
		} catch (SQLException sqle) {
			log.error("Error inserting registration submittal records manually, ", sqle);
			throw new Exception(sqle.getMessage());
		}

		regSub = new StringBuilder(132);
		regSub.append("insert into register_data (register_data_id, register_submittal_id, ");
		regSub.append("register_field_id, value_txt, create_dt) values (?,?,?,?,?)");

		try (PreparedStatement ps = dbConn.prepareStatement(regSub.toString())) {
			
			for (RegistrationMap mKey : RegistrationMap.values()) {
				// check record val, only write reg records if have a value.
				String recVal = (String)record.get(mKey.name());
				if (StringUtil.isEmpty(recVal)) continue;
				
				idx = 0;
				ps.setString(++idx, new UUIDGenerator().getUUID());
				ps.setString(++idx, regSubId);
				ps.setString(++idx, mKey.getFieldId());
				ps.setString(++idx, recVal);
				ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
				ps.addBatch();
				
			}
			
			ps.executeBatch();

		} catch (BatchUpdateException sqle) {
			log.error("Error inserting registration submittal records manually, ", sqle.getNextException());
			throw new Exception(sqle.getNextException().getMessage());
		}
		
	}
	
	/**
	 * @param records
	 * @throws Exception
	 */
	protected void insertRegistrationRecordsViaForm(Map<String, Object> record) throws Exception {
		log.debug("insertRegistrationRecordsViaForm...");
		int count=0;
		int failCnt = 0;
		
		SMTHttpConnectionManager conn = null;
		
		try {
			conn = new SMTHttpConnectionManager();
			conn.retrieveDataViaPost(REGISTRATION_PAGE_URL, buildRegistrationParams(record));
			log.info("retStatus= " + conn.getResponseCode());
			if (conn.getResponseCode() == 200) { 
				count++; 
			} else { 
				failCnt++; 
			}
		} catch (IOException ioe) {
			log.error("Error: IOException during registration " + ioe.getMessage(), ioe);
			throw new Exception();
		} catch (Exception e) {
			log.error("Error: Unexpected exception during registration: " + e.getMessage(), e);
			throw new Exception();
		}
		
		log.info("submitted " + count + " records with " + failCnt + " failures");
	}
	
	/**
	 * Builds the registration submittal params.
	 * @param data
	 * @return
	 */
	protected String buildRegistrationParams(Map<String, Object> data) {
		StringBuilder params = new StringBuilder("1=1");		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		String param;
		// only append the field names and values that we have specified in the reg field map.
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			param = (String)data.get(entry.getKey());
			if (param == null) continue;
			String fieldKey = regFieldMap.get(entry.getKey());
			if (fieldKey != null) {
				params.append("&").append(fieldKey);
				params.append("=").append(StringUtil.replace(param, "\\n","\n").trim());
			}
		}
		
		// now append the formFields params.
		int fieldIndex;
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			param = regFieldMap.get(entry.getKey());
			if (param == null) continue;
			fieldIndex = param.lastIndexOf('|');
			params.append("&formFields=");
			if (fieldIndex == -1) {
				params.append(param);
			} else {
				params.append(param.substring(fieldIndex + 1));
			}
		}
		
		//append some form constants that WC passed in hidden fields
		params.append("&pmid=").append(REG_PMID);
		params.append("&requestType=reqBuild");
		params.append("&actionName=");
		params.append("&sbActionId=").append(REG_ACTION_ID);
		params.append("&page=2");
		params.append("&registerSubmittalId=");
		params.append("&postProcess=");
		params.append("&notifyAdminFlg=0");
		params.append("&finalPage=1");
		params.append("&apprReg=0");
		log.debug("post data:" + params);
		return params.toString();
	}
	
	/**
	 * Created Map of column name to registration field name.
	 * @return
	 */
	protected final Map<String, String> createRegFieldMap() {
		Map<String, String> fieldMap = new TreeMap<>();
		// profile fields
		fieldMap.put("EMAIL_ADDRESS_TXT","reg_enc|EMAIL_ADDRESS_TXT|7f000001397b18842a834a598cdeafa");
		fieldMap.put(FIRST_NM,"reg_enc|FIRST_NM|7f000001427b18842a834a598cdeafa");
		fieldMap.put(LAST_NM,"reg_enc|LAST_NM|7f000001447b18842a834a598cdeafa");
		fieldMap.put("ADDRESS_TXT","reg_enc|ADDRESS_TXT|7f000001467b18842a834a598cdeafa");
		fieldMap.put("ADDRESS2_TXT","reg_enc|ADDRESS2_TXT|7f000001477b18842a834a598cdeafa");
		fieldMap.put("CITY_NM","reg_enc|CITY_NM|7f000001487b18842a834a598cdeafa");
		fieldMap.put("STATE_CD","reg_enc|STATE_CD|7f000001497b18842a834a598cdeafa");
		fieldMap.put(ZIP_CD,"reg_enc|ZIP_CD|7f000001507b18842a834a598cdeafa");
		fieldMap.put(MOBILE_PHONE_TXT,"reg_enc|MOBILE_PHONE_TXT|7f000001517b18842a834a598cdeafa");
		fieldMap.put(MAIN_PHONE_TXT,"reg_enc|MAIN_PHONE_TXT|7f000001527b18842a834a598cdeafa");
		fieldMap.put("COUNTRY_CD","pfl_COUNTRY_CD");
		// non-profile fields
		fieldMap.put(RegistrationMap.TITLE.name(), formatFieldId(RegistrationMap.TITLE.getFieldId()));
		fieldMap.put(RegistrationMap.UPDATES.name(), formatFieldId(RegistrationMap.UPDATES.getFieldId()));
		fieldMap.put(RegistrationMap.FAVORITEUPDATES.name(), formatFieldId(RegistrationMap.FAVORITEUPDATES.getFieldId()));
		fieldMap.put(RegistrationMap.COMPANY.name(), formatFieldId(RegistrationMap.COMPANY.getFieldId()));
		fieldMap.put(RegistrationMap.COMPANYURL.name(), formatFieldId(RegistrationMap.COMPANYURL.getFieldId()));
		fieldMap.put(RegistrationMap.SOURCE.name(), formatFieldId(RegistrationMap.SOURCE.getFieldId()));
		fieldMap.put(RegistrationMap.DEMODT.name(), formatFieldId(RegistrationMap.DEMODT.getFieldId()));
		fieldMap.put(RegistrationMap.TRAININGDT.name(), formatFieldId(RegistrationMap.TRAININGDT.getFieldId()));
		fieldMap.put(RegistrationMap.INITTRAININGDT.name(), formatFieldId(RegistrationMap.INITTRAININGDT.getFieldId()));
		fieldMap.put(RegistrationMap.ADVTRAININGDT.name(), formatFieldId(RegistrationMap.ADVTRAININGDT.getFieldId()));
		fieldMap.put(RegistrationMap.OTHERTRAININGDT.name(), formatFieldId(RegistrationMap.OTHERTRAININGDT.getFieldId()));
		fieldMap.put(RegistrationMap.JOBCATEGORY.name(), formatFieldId(RegistrationMap.JOBCATEGORY.getFieldId()));
		fieldMap.put(RegistrationMap.JOBLEVEL.name(), formatFieldId(RegistrationMap.JOBLEVEL.getFieldId()));
		fieldMap.put(RegistrationMap.INDUSTRY.name(), formatFieldId(RegistrationMap.INDUSTRY.getFieldId()));
		fieldMap.put(RegistrationMap.NOTES.name(), formatFieldId(RegistrationMap.NOTES.getFieldId()));
		return fieldMap;
	}
	
	protected String formatFieldId(String fieldId) {
		return "reg_||" + fieldId;
	}
	
	/**
	 * Sanitizes/cleans import data for certain fields
	 * @param records
	 */
	protected void sanitizeFieldData(Map<String,Object> record) {
		log.debug("sanitizing field data...");
		String country = StringUtil.checkVal(record.get("COUNTRY_CD"));
		String tmpVal = (String)record.get(ZIP_CD);
		record.put(ZIP_CD,fixZipCode(tmpVal,country));
		tmpVal = (String)record.get(MAIN_PHONE_TXT);
		record.put(MAIN_PHONE_TXT, stripPhoneExtension(tmpVal,country));
		tmpVal = (String)record.get(MOBILE_PHONE_TXT);
		record.put(MOBILE_PHONE_TXT,stripPhoneExtension(tmpVal,country));
		tmpVal = (String)record.get(FIRST_NM);
		record.put(FIRST_NM, checkField(tmpVal));
		tmpVal = (String)record.get(LAST_NM);
		record.put(LAST_NM, checkField(tmpVal));
	}
	
	/**
	 * Fixes US zip codes that need a leading 0.
	 * @param zip
	 * @param country
	 * @return
	 */
	protected String fixZipCode(String zip, String country) {
		if ("US".equalsIgnoreCase(country) &&
				StringUtil.checkVal(zip).length() == 4) {
					return "0" + zip;
		}
		return zip;
	}
	
	/**
	 * Strips out any extension text that was included as part of a phone number
	 * e.g.
	 * 		123-456-7890 ext 123 ('ext 123' is removed)
	 * 		123-456-7890, xt 456 (', xt 456' is removed)
	 * 		123-456-7890,9999 (',9999' is removed)
	 * 
	 * @param phone
	 * @param country
	 * @return
	 */
	protected String stripPhoneExtension(String phone, String country) {
		if (StringUtil.checkVal(phone,null) == null) return phone;
		String tmpPhone = phone.toLowerCase();
		int idx = tmpPhone.indexOf(',');
		if (idx == -1) {
			idx = tmpPhone.indexOf('e');
			if (idx == -1) idx = tmpPhone.indexOf('x');
		}
		if (idx > -1) return tmpPhone.substring(0, idx).trim();
		return tmpPhone.trim();
	}
	
	/**
	 * Returns a String whose length is less than or equal to the maxLength.
	 * @param val
	 * @param maxLength
	 * @return
	 */
	protected String checkField(String val) {
		if (val == null) return val;
		
		if (val.indexOf(',') > -1) 
			return val.substring(0,val.indexOf(','));
		return val;
	}
}
