package com.rezdox.util.migration;

// Java 7
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

// Log4j
import org.apache.log4j.PropertyConfigurator;

// WC custom
import com.rezdox.vo.MemberVO;

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
 * <b>Title</b>: LegacyMemberImport.java<p/>
 * <b>Description: This class was created to batch-load legacy RezDox data
 * for the RezDox site. This class manages inserting/updating profiles, creating roles,
 * and creating registration records for a member.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 18, 2018
 ****************************************************************************/
public class LegacyMemberImport extends CommandLineUtil {

	// import env params
	private static final String SOURCE_FILE_CONFIG="scripts/rezdox/migration_config.properties";
	private static final String SOURCE_FILE_LOG="scripts/rezdox/migration_log4j.properties";

	// profile header vals
	private Map<String,StringBuilder> queries;
	private Map<String,String> duplicateProfiles;
	private Map<String,String> processedProfiles;
	private Map<String,String> failedSourceMemberInserts;
	private Map<String,String> failedSourceMemberProfileUpdates;
	private Map<String,String> failedSourceMemberAuthenticationUpdates;
	private long startTimeInMillis;
	
	enum ImportField {
		ADDRESS_TXT, ADDRESS2_TXT, ALLOW_COMM_FLG,
		CITY_NM, COUNTRY_CD, DATE_JOINED, EMAIL_ADDRESS_TXT,
		FIRST_NM, LAST_NM, MAIN_PHONE_TXT, ORGANIZATION_ID,
		PW_PT1_TXT, PW_PT2_TXT, PW_PT3_TXT, PRIVACY_FLG, PROFILE_PIC, ROLE_ID,
		SITE_ID, REZDOX_ID, STATUS, STATE_CD, ZIP_CD
	}

	public LegacyMemberImport(String[] args) {
		super(args);
		PropertyConfigurator.configure(SOURCE_FILE_LOG);
		queries = initQueryStatements();
		duplicateProfiles = new LinkedHashMap<>();
		processedProfiles = new LinkedHashMap<>();
		failedSourceMemberInserts = new LinkedHashMap<>();
		failedSourceMemberProfileUpdates = new LinkedHashMap<>();
		failedSourceMemberAuthenticationUpdates = new LinkedHashMap<>();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LegacyMemberImport lmi = new LegacyMemberImport(args);
		lmi.run();
	}
	
	public void run() {
		startTimeInMillis = Calendar.getInstance().getTimeInMillis();
		// load props
		loadProperties(SOURCE_FILE_CONFIG);
		// get dbconn
		loadDBConnection(props);
		// retrieve records
		log.info("loading records");
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
				record.put(field.name(), rs.getObject(field.name().toLowerCase()));
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
		MemberVO member;
		Map<String,Object> dataSet;

		//iterate the records, inserting each
		Iterator<Map<String,Object>> iter = records.iterator();
		while (iter.hasNext()) {
			recordCnt++;
			dataSet = iter.next();
			
			// clean up certain values before we import them.
			sanitizeFieldData(dataSet);
			
			// init site member from import source
			member = initMember(dataSet);
			
			log.info("START: processing record|source: " + recordCnt + "|" + member.getMemberId());
			// if we weren't able to get a valid email address for the member, don't even process it.
			if (member.getValidEmailFlag() == 0) {
				logInvalidMemberRecord(recordCnt, member);
				failedCnt++;
				continue;
			}
			
			try {
				/* check for pre-existing profile */
				findProfile(dbConn, pm, member);
				
				/* Check for duplicate rezdox profile */
				if (isDuplicateProfile(member,recordCnt))
					continue;
				
				/* create a profile if appropriate, otherwise, leave profile untouched */
				skipCnt = processProfile(dbConn, pm, member, skipCnt);

				/* Create an auth record if appropriate, otherwise, leave auth untouched */
				processAuthentication(dataSet,ul,member);
				
				/* If an org ID and comm flag were supplied, opt-in this member for the given org.	 */
				processCommFlag(dbConn, pm, dataSet, member.getProfileId());
				
				/* Add profile roles for this member for the specified site ID. */
				processRole(dbConn, prm, dataSet, member.getProfileId());

			} catch(Exception ex) {
				log.error("Error processing source ID " + member.getMemberId() + ", " + ex);
			}

			// if valid profile, insert reg records, create rezdox member, update profile/auth.
			if (member.getProfileId() != null) {
				successCnt++;
				insertRegistrationRecords(dbConn, dataSet, member);
				insertSourceMember(dbConn, member);
				// if valid auth record, update profile/auth
				if (hasValidAuthentication(member)) {
					updateSourceMemberProfile(dbConn, member);
				}
				log.info("END: inserting for record|source: " + recordCnt + "|" + member.getMemberId());
			} else {
				// if we couldn't successfully create a profile, add to failed count.
				logInvalidMemberRecord(recordCnt, member);
				failedCnt++;
			}
		}

		// log summary info.
		writeLogs(recordCnt,successCnt,failedCnt,skipCnt);
	}

	/**
	 * Initializes a MemberVO from the dataset map passed in.
	 * @param dataSet
	 * @return
	 */
	protected MemberVO initMember(Map<String,Object> dataSet) {
		MemberVO member = new MemberVO();
		member.setData(dataSet);
		member.setMemberId((String)dataSet.get(ImportField.REZDOX_ID.name()));
		member.setPassword(StringUtil.checkVal(decodePassword(dataSet),null));
		member.setCreateDate(Convert.formatDate(StringUtil.checkVal(dataSet.get(ImportField.DATE_JOINED.name()),null)));
		member.setProfilePicPath(StringUtil.checkVal(dataSet.get(ImportField.PROFILE_PIC.name()),null));
		member.setStatusFlg((int) dataSet.get(ImportField.STATUS.name()));
		member.setPrivacyFlg((int) dataSet.get(ImportField.PRIVACY_FLG.name()));
		
		// parse member's email.
		parseMemberEmail(member);

		return member;
	}
	
	/**
	 * Takes the hexadecimal representation of the password pieces and converts to ascii
	 * for later encryption to standard WC password encryption
	 * 
	 * @param dataSet
	 * @return
	 */
	private String decodePassword(Map<String,Object> dataSet) {
		StringBuilder password = new StringBuilder(30);
		
		String[] passwordParts = new String[3];
		passwordParts[0] = (String) dataSet.get(ImportField.PW_PT1_TXT.name());
		passwordParts[1] = (String) dataSet.get(ImportField.PW_PT2_TXT.name());
		passwordParts[2] = (String) dataSet.get(ImportField.PW_PT3_TXT.name());
		
		for (String part : passwordParts) {
			for (int i = 2; i < part.length(); i += 2) {
				String str = part.substring(i, i + 2);
				password.append((char) Integer.parseInt(str, 16));
			}
		}
		
		return password.toString();
	}
	
	/**
	 * 
	 * @param recordCnt
	 * @param member
	 */
	protected void logInvalidMemberRecord(int recordCnt, MemberVO member) {
		log.info("END: INVALID record, DID NOT insert for record|source: " + recordCnt + "|" + member.getMemberId());
	}
	
	/**
	 * Check for duplicate profile.
	 * @param member
	 * @param recordCnt
	 * @return
	 */
	protected boolean isDuplicateProfile(MemberVO member, int recordCnt) {
		if (member.getProfileId() != null) {
			if (processedProfiles.containsValue(member.getProfileId())) {
				log.info("END: IS DUPLICATE PROFILE, skipping record|memberId: " + recordCnt + "|" + member.getMemberId());
				// add profile ID to dupes map.
				duplicateProfiles.put(member.getMemberId(),member.getProfileId());
				return true;
			} else {
				processedProfiles.put(member.getMemberId(), member.getProfileId());
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Checks member's email address.  If email address is invalid,
	 * the member's email address is set to null.
	 * @param member
	 */
	protected void parseMemberEmail(MemberVO member) {
		// Check to see if we can set 'valid' email flag
		if (StringUtil.isValidEmail(member.getEmailAddress())) {
			member.setValidEmailFlag(1);
		} else {
			member.setValidEmailFlag(0);
		}
		log.debug("Member email address is: " + member.getEmailAddress());
	}
	
	/**
	 * Inserts the RezDox source member record.
	 * @param dbConn
	 * @param member
	 */
	protected void insertSourceMember(Connection dbConn, MemberVO member) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into custom.rezdox_member (member_id, profile_id, ");
		if (member.getRegisterSubmittalId() != null) 
			sql.append("register_submittal_id, ");
		
		if (member.getCreateDate() != null) 
			sql.append("create_dt, ");
		
		sql.append("status_flg, privacy_flg, profile_pic_pth) values (?,?");

		if (member.getRegisterSubmittalId() != null) 
			sql.append(",?");
		
		if (member.getCreateDate() != null) 
			sql.append(",?");
	
		// add last ones for remaining fields
		sql.append(",?,?,?)");

		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++idx, member.getMemberId());
			ps.setString(++idx, member.getProfileId());
			if (member.getRegisterSubmittalId() != null) {
				ps.setString(++idx, member.getRegisterSubmittalId());
			}
			if (member.getCreateDate() != null) {
				ps.setDate(++idx, Convert.formatSQLDate(member.getCreateDate()));
			}
			ps.setInt(++idx, member.getStatusFlg());
			ps.setInt(++idx, member.getPrivacyFlg());
			ps.setString(++idx, member.getProfilePicPath());
			ps.execute();
			log.debug("inserted source member record for member_id|profile_id: " + member.getMemberId() + "|" + member.getProfileId());
		} catch(SQLException sqle) {
			log.error("Error inserting source member, ", sqle);
			failedSourceMemberInserts.put(member.getMemberId(),member.getProfileId());
		}

	}
	
	/**
	 * Updates a RezDox member's profile with their auth ID.
	 * @param dbConn
	 * @param member
	 */
	protected void updateSourceMemberProfile(Connection dbConn, MemberVO member) {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("AUTH").toString())) {
			ps.setString(++idx, member.getAuthenticationId());
			ps.setString(++idx, member.getProfileId());
			ps.execute();
			
		} catch(SQLException sqle) {
			failedSourceMemberProfileUpdates.put(member.getMemberId(),member.getProfileId());
		}
	}
	
	/**
	 * Checks for invalid authentication record data.
	 * @param member
	 * @return
	 */
	protected boolean hasValidAuthentication(MemberVO member) {
		return ! (StringUtil.isEmpty(member.getAuthenticationId()) ||
				StringUtil.isEmpty(member.getPassword()) || 
				"INVALID".equalsIgnoreCase(member.getPassword()));
	}
	
	/**
	 * Checks for the existence of a profile if no profileId was supplied for the member record.
	 * @param dbConn
	 * @param pm
	 * @param member
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void findProfile(Connection dbConn, ProfileManager pm, 
			MemberVO member) throws DatabaseException {
			member.setProfileId(pm.checkProfile(member, dbConn));
	}
	
	/**
	 * Checks for an authentication for the member if the member has a valid email address
	 * and if a password was supplied in the member's source record.  If an authentication
	 * ID is not found, the authentication record is created.
	 * @param dataSet
	 * @param ul
	 * @param member
	 * @throws DatabaseException
	 */
	protected void processAuthentication(Map<String,Object> dataSet, 
			UserLogin ul, MemberVO member) throws DatabaseException {
		String password = StringUtil.checkVal(member.getPassword(), null);
		if (password == null || member.getEmailAddress() == null) 
			return;
		
		member.setAuthenticationId(ul.checkAuth(member.getEmailAddress()));
		
		// if no auth record, create one, otherwise leave existing auth alone.
		if (member.getAuthenticationId() == null) {	
			member.setAuthenticationId(ul.saveAuthRecord(member.getAuthenticationId(), 
					member.getEmailAddress(), password, 0));
		}
	}

	/**
	 * Checks for the existence of a member profile ID on the member object.  If no 
	 * profile ID is found we attempt to create the profile.  If a profile ID is
	 * found we update the 'skipped profiles' count.
	 * @param dbConn
	 * @param pm
	 * @param member
	 * @param skipCnt
	 * @return
	 * @throws DatabaseException
	 */
	protected int processProfile(Connection dbConn, ProfileManager pm, 
			MemberVO member, int skipCnt) throws DatabaseException {
		/* 2017-01-19: If profile doesn't exist, insert it.  Otherwise leave the existing 
		 * profile alone. */
		if (member.getProfileId() == null) {
			pm.updateProfile(member, dbConn);
			return skipCnt;
		} else {
			return skipCnt + 1;
		}
	}
	
	/**
	 * Checks to see if an 'allow communications flag' (a.k.a. opt-in flag) was supplied
	 * for this member.
	 * @param dbConn
	 * @param pm
	 * @param dataSet
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void processCommFlag(Connection dbConn, ProfileManager pm, 
			Map<String,Object> dataSet, String profileId) throws DatabaseException {
		String orgId = StringUtil.checkVal(dataSet.get(ImportField.ORGANIZATION_ID.name()),null);
		String allowCommFlag = StringUtil.checkVal(dataSet.get(ImportField.ALLOW_COMM_FLG.name()),null);
		
		if (orgId == null || allowCommFlag == null) 
			return;
		
		pm.assignCommunicationFlg(orgId, profileId, Convert.formatInteger(allowCommFlag), dbConn);
	}
	
	/**
	 * Checks to see if valid role ID and site ID were supplied on record, then checks
	 * for an existing role for this member.  If no role is found we add the role.
	 * @param dbConn
	 * @param prm
	 * @param dataSet
	 * @param profileId
	 * @throws DatabaseException
	 */
	protected void processRole(Connection dbConn, ProfileRoleManager prm, 
			Map<String,Object> dataSet, String profileId) throws DatabaseException {
		String newRoleId = StringUtil.checkVal(dataSet.get(ImportField.ROLE_ID.name()),null);
		String siteId = StringUtil.checkVal(dataSet.get(ImportField.SITE_ID.name()),null);
		if (newRoleId == null || siteId == null) 
			return;
		
		/* Check for existing role for this member for this site? Returns profile role id (primary key)
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
	 * Used to insert registration records for profiles with no email address.  If we try to push these through the API, 
	 * WC creates a new profile for this member in addition to creating the registration records.  This results in multiple
	 * duplicated profiles.  Additionally, the API does not return the profile ID that was created so we don't have
	 * a way of mapping the original source ID to the WC profile ID.
	 * @param dbConn
	 * @param record
	 * @param member
	 * @throws SQLException
	 */
	protected void insertRegistrationRecords(Connection dbConn, Map<String, Object> record, 
			MemberVO member) {
		int idx = 1;
		String regSubId = new UUIDGenerator().getUUID();
		try (PreparedStatement ps = dbConn.prepareStatement(queries.get("REGSUB").toString())) {
			ps.setString(idx++, regSubId);
			ps.setString(idx++, (String)record.get(ImportField.SITE_ID.name()));
			ps.setString(idx++, props.getProperty("registerActionId"));
			ps.setString(idx++, member.getProfileId());
			ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
			ps.execute();
			// set the register submittal ID on an unused member field so we can get it later.
			member.setRegisterSubmittalId(regSubId);
		} catch (SQLException sqle) {
			log.error("Error inserting registration submittal record, ", sqle);
		}
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

		if (! failedSourceMemberInserts.isEmpty()) {
			log.info("Failed Source Member Inserts: ");
			for (Map.Entry<String,String> fail : failedSourceMemberInserts.entrySet()) {
				log.info(fail.getKey() + "|" + fail.getValue());
			}
			log.info("------------------------------------------------\n\n");
		}

		if (! failedSourceMemberProfileUpdates.isEmpty()) {
			log.info("Failed Source Member Profile Updates: ");
			for (Map.Entry<String,String> fail : failedSourceMemberProfileUpdates.entrySet()) {
				log.info(fail.getKey() + "|" + fail.getValue());
			}
			log.info("-------------------------------------------------\n\n");
		}

		if (! failedSourceMemberAuthenticationUpdates.isEmpty()) {
			log.info("Failed Source Member Authentication Updates: ");
			for (Map.Entry<String,String> fail : failedSourceMemberAuthenticationUpdates.entrySet()) {
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
		String country = StringUtil.checkVal(record.get(ImportField.COUNTRY_CD.name()));
		
		String tmpVal = (String)record.get(ImportField.ZIP_CD.name());
		record.put(ImportField.ZIP_CD.name(),fixZipCode(tmpVal,country));
		
		tmpVal = (String)record.get(ImportField.MAIN_PHONE_TXT.name());
		record.put(ImportField.MAIN_PHONE_TXT.name(), stripPhoneExtension(tmpVal));
		
		tmpVal = (String)record.get(ImportField.FIRST_NM.name());
		record.put(ImportField.FIRST_NM.name(), checkNameField(tmpVal));
		
		tmpVal = (String)record.get(ImportField.LAST_NM.name());
		record.put(ImportField.LAST_NM.name(), checkNameField(tmpVal));
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
		tmp.append("insert into register_submittal (register_submittal_id, site_id, action_id, ");
		tmp.append("profile_id, create_dt) values (?,?,?,?,?)");
		sql.put("REGSUB",tmp);

		return sql;
	}
	
	/**
	 * 
	 * @return
	 */
	protected StringBuilder buildMainQuery() {
		StringBuilder sql = new StringBuilder(1375);
		sql.append("select cast(id as varchar) AS REZDOX_ID, fname AS FIRST_NM, lname AS LAST_NM, ");
		sql.append("upper(email) AS EMAIL_ADDRESS_TXT, address1 AS ADDRESS_TXT, address2 AS ADDRESS2_TXT, ");
		sql.append("city AS CITY_NM, state AS STATE_CD, zip AS ZIP_CD, 'US' as COUNTRY_CD, ");
		sql.append("phone AS MAIN_PHONE_TXT, profilepic AS PROFILE_PIC, ");
		sql.append("case when status='active' then 1 else 0 end AS STATUS, ");
		sql.append("start AS DATE_JOINED, privacy AS PRIVACY_FLG, ");
		sql.append("case when status='active' then 'REZDOX_RES_BUS' else null end AS ROLE_ID, ");
		sql.append("decrypt(decode(pw_pt1, 'hex'), 'RezDox', 'aes')::varchar as PW_PT1_TXT, decrypt(decode(pw_pt2, 'hex'), 'RezDox', 'aes')::varchar as PW_PT2_TXT, decrypt(decode(pw_pt3, 'hex'), 'RezDox', 'aes')::varchar as PW_PT3_TXT, ");
		sql.append("'REZDOX' as ORGANIZATION_ID, 'REZDOX_2' as SITE_ID, 1 as ALLOW_COMM_FLG ");
		sql.append("from rezdox.member_tbl ");
		sql.append("order by status desc, id; ");
		return sql;
	}

}
