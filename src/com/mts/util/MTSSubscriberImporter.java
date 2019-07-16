package com.mts.util;

// JDK 1.8.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Log4J
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// MTS Libs
import com.mts.common.MTSConstants;
import com.mts.common.MTSConstants.MTSRole;
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.action.user.UserVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: MTSSubscriberImporter.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Importer of MTS Subscribers 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 24, 2019
 * @updates:
 ****************************************************************************/
public class MTSSubscriberImporter {
	
	private static final Logger log = Logger.getLogger(MTSSubscriberImporter.class);
	public static final String FILE_LOC = "/home/ryan/Downloads/FINAL_MTS_Subscribers.xlsx";
	public static final String DB_URL = "jdbc:postgresql://sonic:5432/webcrescendo_mts5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3";
	public static final String DB_USER = "ryan_user_sb";
	public static final String DBP_INFO = "sqll0gin";
	
	private Map<String, String> typeMap = new HashMap<>();
	private Map<String, String> countryMap = new HashMap<>();
	private Connection conn;
	private Map<String, Object> attributes = new HashMap<>();
	private Set<String> existingProfiles = new HashSet<>();
	
	/**
	 * 
	 * @throws DatabaseException
	 */
	public MTSSubscriberImporter() throws DatabaseException {
		super();
		getConnection();
		
		// Init the Type Map
		typeMap.put("single", "USER");
		typeMap.put("multi", "MULTIPLE");
		typeMap.put("corporate", "CORPORATE");
		
		// Init the Country Map
		countryMap.put("germany", "DE");
		countryMap.put("ireland", "IE");
		countryMap.put("united states", "US");
		countryMap.put("canada", "CA");
		countryMap.put("new zealand", "NZ");
		countryMap.put("japan", "JP");
		countryMap.put("netherlands", "NL");
		countryMap.put("singapore", "SG");
		countryMap.put("united kingdom", "GB");
		countryMap.put("switzerland", "CH");
		countryMap.put("australia", "AU");
		countryMap.put("israel", "IL");
		countryMap.put("taiwan", "TW");
		countryMap.put("belgium", "BE");
		countryMap.put("italy", "IT");
		countryMap.put("china", "CN");
		countryMap.put("india", "IN");
		countryMap.put("korea", "KR");
		
		// Add the necessary attributes
		attributes.put(Constants.ENCRYPT_KEY, "s1l1c0nmtnT3chm0l0g13$JC");
		attributes.put(Constants.CFG_PASSWORD_SALT, "So the combination is... one, two, three, four, five? That's the stupidest combination I've ever heard in my life! That's the kind of thing an idiot would have on his luggage!");
		attributes.put(Constants.GEOCODE_URL,"http://localhost:8080/websvc/geocoder");
		attributes.put(Constants.GEOCODE_CLASS, "com.siliconmtn.gis.SMTGeocoder");
		attributes.put(Constants.BINARY_PATH, "/home/ryan/git/WebCrescendo/binary");
		attributes.put(Constants.PROFILE_IMAGE_PATH, "profile/image/");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		log.info("---------- Starting");
		
		MTSSubscriberImporter imp = new MTSSubscriberImporter();
		imp.process();
		imp.conn.close();
		
		log.info("-------- Complete");
	} 
	
	/**
	 * 
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws Exception
	 */
	public void process() throws IOException {
		List<MTSUserVO> users = parseExcelFile();
		log.info("Number Users: " + users.size());
		int ctr = 1;
		// Loop the users and store the data
		for (MTSUserVO user : users) {
			try {
				// Add profile and Auth
				addProfile(user.getProfile());
				user.setProfileId(user.getProfile().getProfileId());
				
				// If the user is already in the mts_user table, skip them
				if (existingProfiles.contains(user.getProfileId())) continue;
				else existingProfiles.add(user.getProfileId());
				
				// Add Site Role
				SBUserRole role = saveRole(user);
				user.setRoleId(role.getRoleId());
				user.setProfileRoleId(role.getProfileRoleId());
				
				// Add user and subscriptions
				DBProcessor db = new DBProcessor(conn, "custom.");
				db.insert(user);
				db.insert(user.getSubscriptions().get(0));
			} catch (Exception e) {
				log.error(user.getSecondaryUserId() + "|" + user.getFullName() + "|" + e.getMessage(), e);
			}
			
			if ((ctr++ % 10) == 0) log.info("Records Processed: " + ctr);
		}
	}
	
	/**
	 * Checks for current users and loads the profile ID.  this way,
	 * if a user is loaded, we can skip the loading of their user acct
	 * @throws SQLException
	 */
	public void loadCurrentUsers() throws SQLException {
		String sql = "select profile_id from custom.mts_user";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			try (ResultSet rs = ps.executeQuery()) {
				existingProfiles.add(rs.getString(1));
			}
		}
	}
	
	/**
	 * Looks up the country code from the name
	 * @param name
	 * @return
	 */
	public String getCountry(String name) {
		
		return StringUtil.checkVal(countryMap.get(name), "US");
	}
	
	/**
	 * Updates or saves the profile role value for the user
	 * @param site
	 * @param user
	 * @param isActive
	 * @return the RoleVO created by the method, which contains any generated PKID
	 * @throws com.siliconmtn.exception.DatabaseException
	 */
	protected SBUserRole saveRole(UserVO user) 
	throws com.siliconmtn.exception.DatabaseException {
		SBUserRole role = new SBUserRole();
		role.setOrganizationId(MTSConstants.ORGANIZATON_ID);
		role.setSiteId(MTSConstants.PORTAL_SITE_ID);
		role.setProfileRoleId(user.getProfileRoleId());
		role.setProfileId(user.getProfileId());
		role.setRoleId(user.getRoleId());
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		
		// If the profile role id is missing, look for it
		if (StringUtil.isEmpty(role.getProfileRoleId())) {
			String prid = new ProfileRoleManager().checkRole(user.getProfileId(), MTSConstants.PORTAL_SITE_ID, null, null, conn);
			if (! StringUtil.isEmpty(prid)) role.setProfileRoleId(prid);
		}

		new ProfileRoleManager().addRole(role, conn);
		return role;
	}
	
	/**
	 * Adds the profile to the system
	 * @param user
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public void addProfile(UserDataVO profile) throws com.siliconmtn.exception.DatabaseException {
		profile.setAuthenticationId(saveAuthenticationRecord(profile));
		
		// Update / add the profile.
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		pm.updateProfile(profile, conn);
		
		// Assign the comm flag
		pm.assignCommunicationFlg(MTSConstants.ORGANIZATON_ID, profile.getProfileId(), 
				profile.getAllowCommunication(), conn,null);
	}

	/**
	 * Adds the auth record for a new user.  Checks for the existence (in case 
	 * there is a record for that user) 
	 * @param user
	 * @return
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public String saveAuthenticationRecord(UserDataVO profile)
	throws com.siliconmtn.exception.DatabaseException {
		UserLogin login = new UserLogin(conn, attributes);
		String authId = login.checkAuth(profile.getEmailAddress()); //lookup authId using the NEW email address
		
		//if we find an authId using the NEW email, see if it matches the old authId (if we had one)
		if (StringUtil.isEmpty(authId)) {
			//add a record, there wasn't one prior
			authId = login.saveAuthRecord(null, profile.getEmailAddress(), RandomAlphaNumeric.generateRandom(10), 1);
		} else if (!StringUtil.checkVal(authId).equals(profile.getAuthenticationId())) {
			//edit the existing record, we have a changed email address
			//note use of the pre-existing authenticationId
			authId = login.saveAuthRecord(profile.getAuthenticationId(), profile.getEmailAddress(), UserLogin.DUMMY_PSWD, 0);
		}  //the 'else' here is that the auth record does not need modification or creation - do nothing
		
		return authId;
	}
	
	/**
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<MTSUserVO> parseExcelFile() throws IOException {
		List<MTSUserVO> users = new ArrayList<>(375);
		try (InputStream is = new FileInputStream(new File(FILE_LOC))) {
			try (XSSFWorkbook wb = new XSSFWorkbook(is)) {
			    XSSFSheet sheet = wb.getSheetAt(0);
			    log.info("numbers of entry " + sheet.getPhysicalNumberOfRows());
			    for (int i=1; i < 376; i++) {
			    	XSSFRow row = sheet.getRow(i);
			    	MTSUserVO user = new MTSUserVO();
			    	
			    	SubscriptionUserVO subscription = new SubscriptionUserVO();
					subscription.setPublicationId("MED_TECH_STRATEGIST");
					subscription.setUserId(Convert.formatInteger(row.getCell(0).getNumericCellValue() + "") + "");
					user.addSubscription(subscription);
					user.setUserId(subscription.getUserId());
					user.setSecondaryUserId(Convert.formatInteger(row.getCell(0).getNumericCellValue() + "") + "");
					user.setFirstName(row.getCell(2).getStringCellValue());
					user.setLastName(row.getCell(3).getStringCellValue());
					user.setCompanyName(row.getCell(4).getStringCellValue());
					
					String key = row.getCell(14).getStringCellValue().toLowerCase();
					
					if("Site License".equalsIgnoreCase(key) ) {
						key  = "Corporate";
					}
					if("multi-user".equalsIgnoreCase(key) ) {
						key  = "multi";
					}
					
					
					log.info("key " + key);
					log.info("enum value " + typeMap.get(key.toLowerCase()));
					user.setSubscriptionType(SubscriptionType.valueOf(typeMap.get(key.toLowerCase())));
					
					user.setCreateDate(Convert.formatDate("dd-MMM-yyyy", row.getCell(6) + ""));
					
					user.setExpirationDate(Convert.formatDate("dd-MMM-yyyy", row.getCell(7) + ""));
					log.info("id " + user.getSecondaryUserId());
					XSSFCell cellValue = row.getCell(15);
					String targetValue = "";
					if ( cellValue != null) {
						targetValue = cellValue.getStringCellValue().toLowerCase();
					}
					
					user.setPrintCopyFlag(Convert.formatBoolean( targetValue ) ? 1 : 0);
					user.setEmailAddress((row.getCell(5) != null) ? row.getCell(5).getStringCellValue() : null);
					user.setRoleId(MTSRole.SUBSCRIBER.getRoleId());
					user.setActiveFlag(1);
					user.setLocale("en_US");
	
					DataFormatter fmt = new DataFormatter();
					UserDataVO profile = new UserDataVO();
					profile.setFirstName(user.getFirstName());
					profile.setLastName(user.getLastName());
					profile.setEmailAddress(user.getEmailAddress());
					
					profile.setAddress((row.getCell(8) != null) ? row.getCell(8).getStringCellValue(): null);
					profile.setAddress2((row.getCell(9) != null) ? row.getCell(9).getStringCellValue() : null);
					profile.setCity((row.getCell(10) != null) ? row.getCell(10).getStringCellValue() : null);
					profile.setState((row.getCell(11) != null) ? row.getCell(11).getStringCellValue() : null);
					profile.setZipCode((row.getCell(12) != null) ? fmt.formatCellValue(row.getCell(12)) : null);
					profile.setCountryCode((row.getCell(13) != null) ? getCountry(row.getCell(13).getStringCellValue()) : null);
					
					profile.setAllowCommunication(1);
					user.setProfile(profile);

					users.add(user);
					log.info("user added");
			    }
			}
	    } catch (Exception e) {
			throw new IOException("Unable to retrieve Excel Info", e);
		}

	    return users;
	}

	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public void getConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl(DB_URL);
		dc.setUserName(DB_USER);
		dc.setPassword(DBP_INFO);
		
		try {
			conn = dc.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
	}
}
