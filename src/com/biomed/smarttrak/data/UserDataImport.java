package com.biomed.smarttrak.data;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
 * BiomedGPS SmartTRAK site.  This class inserts/updates profiles, created roles, and 
 * pushes the registration records from an Excel file through the front door of the website's
 * registration page, rather than doing direct database insertion.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author DBargerhuff
 * @version 1.0
 * @since Jan 19, 2017
 ****************************************************************************/
public class UserDataImport extends ProfileImport {

	private static Map<String,String> regFieldMap = createRegFieldMap();
	private final String GEOCODE_CLASS="com.siliconmtn.gis.SMTGeocoder";
	private final String GEOCODE_URL="http://localhost:9000/websvc/geocoder";
	private final String REGISTRATION_PAGE_URL = "http://smarttrak.siliconmtn.com/subscribe";
	//private static String FILE_PATH="/home/groot/Downloads/smarttrak/user-import/test/smarttrak-profiles-ACTIVE-USERS-WIP-2017-01-20-1803pm.csv";
	private static String FILE_PATH="/home/groot/Downloads/smarttrak/user-import/test/smarttrak-profiles-INACTIVE-USERS-WIP-2017-01-20-1803pm.csv";
	
	public UserDataImport() {
		super();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {        
        UserDataImport db = new UserDataImport();
		try {
			System.out.println("importFile=" + FILE_PATH);
			List<Map<String,String>> data = db.parseFile(FILE_PATH);
			db.sanitizeFieldData(data);
			db.insertRecords(data);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		}
		db = null;
	}
	
	/**
	 * Sanitizes/cleans import data for certain fields
	 * @param records
	 */
	protected void sanitizeFieldData(List<Map<String,String>> records) {
		log.debug("parsing phones...");
		String country;
		for (Map<String,String> record: records) {
			country = StringUtil.checkVal(record.get("COUNTRY_CD"));
			//log.debug("main before: " + record.get("MAIN_PHONE_TXT"));
			//log.debug("mobile before: " + record.get("MOBILE_PHONE_TXT"));
			record.put("ZIP_CD",fixZipCode(record.get("ZIP_CD"),country));
			record.put("MAIN_PHONE_TXT", stripPhoneExtension(record.get("MAIN_PHONE_TXT"),country));
			record.put("MOBILE_PHONE_TXT",stripPhoneExtension(record.get("MOBILE_PHONE_TXT"),country));
			//log.debug("main after: " + record.get("MAIN_PHONE_TXT"));
			//log.debug("mobile after: " + record.get("MOBILE_PHONE_TXT"));
		}
	}
	
	/**
	 * Fixes US zip codes that need a leading 0.
	 * @param zip
	 * @param country
	 * @return
	 */
	protected String fixZipCode(String zip, String country) {
		if ("US".equalsIgnoreCase(country)) {
			if (StringUtil.checkVal(zip).length() == 4) {
				return "0" + zip;
			}
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
		phone = phone.toLowerCase();
		int idx = phone.indexOf(',');
		if (idx == -1) {
			idx = phone.indexOf('e');
			if (idx == -1) idx = phone.indexOf('x');
		}
		if (idx > -1) return phone.substring(0, idx).trim();
		return phone.trim();
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
		String currRecord;
		//Open DB Connection
		Connection dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
		
		List<String> profileIds = new ArrayList<String>();
		Map<String,String> profileIdMap = new HashMap<>();
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(Constants.ENCRYPT_KEY, encKey);
		config.put(Constants.GEOCODE_CLASS, GEOCODE_CLASS);
		config.put(Constants.GEOCODE_URL, GEOCODE_URL);
	    ProfileManager pm = new SBProfileManager(config);
		ProfileRoleManager prm = new ProfileRoleManager();
		UserLogin ul = new UserLogin(dbConn, encKey);
		SiteUserVO user = null;
		
		//iterate the records, inserting each
		Iterator iter = records.iterator();
		while (iter.hasNext()) {
			recordCnt++;
			// populate user data vo
			user = new SiteUserVO();
			Map<String,Object> dataSet = (Map<String,Object>) iter.next();
			currRecord = (String)dataSet.get("SMARTTRAK_ID");
			user.setData(dataSet);
			if (!StringUtil.isValidEmail(user.getEmailAddress())) {
				log.warn("Invalid email found, skipping record|email: " + recordCnt + "|" + user.getEmailAddress());
				profileIdMap.put(currRecord,"Profile creation failed - invalid email address.");
				failedCnt++;
				continue;
			}
			
			user.setValidEmailFlag(1);
			
try {			
			// check for pre-existing user profile
			if (!dataSet.containsKey("PROFILE_ID")) 
				user.setProfileId(pm.checkProfile(user, dbConn));
			
			/* If password was supplied, check for existing auth ID.
			 * Create an auth record only if an auth record doesn't 
			 * already exist because we don't want to overwrite what
			 * is already there. */
			if (dataSet.containsKey("PASSWORD_TXT")) {
				user.setAuthenticationId(ul.checkAuth(user.getEmailAddress()));
				if (user.getAuthenticationId() == null) {
					//pwd will be encrypted at qry, 0 sets password reset flag to false
					user.setAuthenticationId(ul.modifyUser(user.getAuthenticationId(), 
							user.getEmailAddress(), user.getPassword(), 0));
				}
			}
		
			/* 2017-01-19: If profile doesn't exist, insert it.  Otherwise leave the existing 
			 * profile alone. */
			if (user.getProfileId() == null) {
				 //runs insert query
				pm.updateProfile(user, dbConn);
			} else {
				skipCnt++;
				//pm.updateProfilePartially(dataSet, user, dbConn); //runs dynamic update query; impacts on the columns we're importing
			}
			
			/* If an org ID and comm flag were supplied, opt-in this user for the given org.	 */
			if (dataSet.containsKey("ALLOW_COMM_FLG") && dataSet.containsKey("ORGANIZATION_ID"))
				pm.assignCommunicationFlg((String)dataSet.get("ORGANIZATION_ID"), user.getProfileId(), Convert.formatInteger((String)dataSet.get("ALLOW_COMM_FLG")), dbConn);
			
			/* Add profile roles for this user for the specified site ID. */
			if (dataSet.containsKey("ROLE_ID") && dataSet.containsKey("SITE_ID")) {
				// If both columns are populated, process role.
				if (StringUtil.checkVal(dataSet.get("ROLE_ID"),null) != null &&
						StringUtil.checkVal(dataSet.get("SITE_ID"),null) != null) {
					if (!prm.roleExists(user.getProfileId(), (String)dataSet.get("SITE_ID"), (String)dataSet.get("ROLE_ID"), dbConn)) {
						SBUserRole userRole = new SBUserRole();
						userRole.setSiteId((String)dataSet.get("SITE_ID"));
						userRole.setRoleId((String)dataSet.get("ROLE_ID"));
						userRole.setStatusId(20);
						userRole.setProfileId(user.getProfileId());
						try {
							prm.addRole(userRole, dbConn);
						} catch (Exception e) {
							log.error("Error: Cannot add role for this record number: ", e);
						}
					}
				}
			}
		} catch(Exception ex) {
			log.error("Error processing source ID " + currRecord + ", " + ex.getMessage());
		}
			//increment our counters
			if (user.getProfileId() != null) {
				successCnt++;
				profileIds.add(user.getProfileId());
				profileIdMap.put(currRecord, user.getProfileId());
				try {
					insertRegistrationRecords(dataSet);
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
		
		/* Output the source ID mapped to the WC profile ID found/created. */
		for (Map.Entry<String,String> entry : profileIdMap.entrySet()) {
			log.debug("SmartTRAK ID --> WC profile ID: " + entry.getKey() + " --> " + entry.getValue());
		}
	}
	
	/**
	 * @param records
	 * @throws Exception
	 */
	protected void insertRegistrationRecords(Map<String, Object> record) 
			throws Exception {
		int count=0;
		int failCnt = 0;
		
		SMTHttpConnectionManager conn = null;
		
		try {
			conn = new SMTHttpConnectionManager();
			conn.retrieveDataViaPost(REGISTRATION_PAGE_URL, buildRegistrationParams(record));
			log.info("retStatus= " + conn.getResponseCode());
			if (conn.getResponseCode() == 200) { ++count; } else { ++failCnt; };
			//if (count == 10) return;
			conn = null;
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
	private String buildRegistrationParams(Map<String, Object> data) {
		StringBuilder params = new StringBuilder("1=1");		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		int fieldIndex = -1;
		String param;
		// only append the field names and values that we have specified in the reg field map.
		for (String p : data.keySet()) {
			//String key = StringUtil.replace(p, "%", "|");
			param = (String)data.get(p);
			if (param == null) continue;
			String fieldKey = regFieldMap.get(p);
			if (fieldKey != null) {
				params.append("&").append(fieldKey).append("=").append(StringUtil.replace(param, "\\n","\n").trim());
			}
		}
		
		// now append the formFields params.
		for (String regFieldKey : data.keySet()) {
			param = regFieldMap.get(regFieldKey);
			if (param == null) continue;
			fieldIndex = (param.lastIndexOf("|"));
			params.append("&formFields=");
			if (fieldIndex == -1) {
				params.append(param);
			} else {
				params.append(param.substring(fieldIndex + 1));
			}
		}
		
		//append some form constants that WC passed in hidden fields
		params.append("&pmid=6d9674d8b7dc54077f0001019b2cb979");
		params.append("&requestType=reqBuild");
		params.append("&actionName=");
		params.append("&sbActionId=ea884793b2ef163f7f0001011a253456");
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
	private static final Map<String, String> createRegFieldMap() {
		Map<String, String> fieldMap = new TreeMap<String,String>();
		// profile fields
		fieldMap.put("EMAIL_ADDRESS_TXT","reg_enc|EMAIL_ADDRESS_TXT|7f000001397b18842a834a598cdeafa");
		fieldMap.put("FIRST_NM","reg_enc|FIRST_NM|7f000001427b18842a834a598cdeafa");
		fieldMap.put("LAST_NM","reg_enc|LAST_NM|7f000001447b18842a834a598cdeafa");
		fieldMap.put("ADDRESS_TXT","reg_enc|ADDRESS_TXT|7f000001467b18842a834a598cdeafa");
		fieldMap.put("ADDRESS2_TXT","reg_enc|ADDRESS2_TXT|7f000001477b18842a834a598cdeafa");
		fieldMap.put("CITY_NM","reg_enc|CITY_NM|7f000001487b18842a834a598cdeafa");
		fieldMap.put("STATE_CD","reg_enc|STATE_CD|7f000001497b18842a834a598cdeafa");
		fieldMap.put("ZIP_CD","reg_enc|ZIP_CD|7f000001507b18842a834a598cdeafa");
		fieldMap.put("MOBILE_PHONE_TXT","reg_enc|MOBILE_PHONE_TXT|7f000001517b18842a834a598cdeafa");
		fieldMap.put("MAIN_PHONE_TXT","reg_enc|MAIN_PHONE_TXT|7f000001527b18842a834a598cdeafa");
		fieldMap.put("COUNTRY_CD","pfl_COUNTRY_CD");
		// non-profile fields
		fieldMap.put("TITLE","reg_||dd64d07fb37c2c067f0001012b4210ff");
		fieldMap.put("UPDATE_FREQ","reg_||9b079506b37cc0de7f0001014b63ad3c");
		fieldMap.put("UPDATE_FAVORITES_FREQ","reg_||d5ed674eb37da7fd7f000101d875b114");
		return fieldMap;
	}
}
