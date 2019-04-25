package com.mts.util;

// JDK 1.8.x
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4J
import org.apache.log4j.Logger;

// MTS Libs
import com.mts.common.MTSConstants;
import com.mts.common.MTSConstants.MTSRole;
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;

// SMT Base Libs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.action.user.UserVO;
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
	public static final String FILE_LOC = "/home/etewa/Downloads/MTS_subscribers.txt";
	private Map<String, String> typeMap = new HashMap<>();
	private Connection conn;
	private Map<String, Object> attributes = new HashMap<>();
	
	/**
	 * 
	 */
	public MTSSubscriberImporter() {
		super();
		
		typeMap.put("single", "USER");
		typeMap.put("multi", "MULTIPLE");
		typeMap.put("corporate", "CORPORATE");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		log.info("---------- Starting");
		MTSSubscriberImporter imp = new MTSSubscriberImporter();
		List<MTSUserVO> users = imp.parseUserFile();
		/*
		for (MTSUserVO user : users) {
			imp.addProfile(user.getProfile());
			user.setProfileId(user.getProfile().getProfileId());
			
			SBUserRole role = imp.saveRole(user);
			user.setRoleId(role.getRoleId());
			user.setProfileRoleId(role.getProfileRoleId());
			
			DBProcessor db = new DBProcessor(imp.conn, "custom.");
			db.insert(user);
		}
		*/
		log.info("-------- Complete");
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
		if (!StringUtil.checkVal(authId).equals(profile.getAuthenticationId())) {
			//edit the existing record, we have a changed email address
			//note use of the pre-existing authenticationId
			authId = login.saveAuthRecord(profile.getAuthenticationId(), profile.getEmailAddress(), UserLogin.DUMMY_PSWD, 0);
		} else if (StringUtil.isEmpty(authId)) {
			//add a record, there wasn't one prior
			authId = login.saveAuthRecord(null, profile.getEmailAddress(), RandomAlphaNumeric.generateRandom(10), 1);
		} //the 'else' here is that the auth record does not need modification or creation - do nothing

		return authId;
	}
	
	/**
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<MTSUserVO> parseUserFile() throws FileNotFoundException, IOException {
		List<MTSUserVO> users = new ArrayList<>(384);
		int ctr = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(new File(FILE_LOC)))) {
			
			String temp;
			MTSUserVO user = new MTSUserVO();
			while((temp = br.readLine()) != null) {
				try {
					String[] cols = temp.split("\\t");
					if (ctr++ == 0) continue;
					SubscriptionUserVO subscription = new SubscriptionUserVO();
					subscription.setPublicationId("MED_TECH_STRATEGIST");
					subscription.setUserId(cols[0]);
					user.setUserId(cols[0]);
					user.setSecondaryUserId(cols[0]);
					user.setFirstName(cols[2]);
					user.setLastName(cols[3]);
					user.setCompanyName(cols[4]);
					user.addSubscription(subscription);
					user.setSubscriptionType(SubscriptionType.valueOf(typeMap.get(cols[6].toLowerCase())));
					user.setCreateDate(Convert.formatDate(cols[7]));
					user.setExpirationDate(Convert.formatDate(cols[8]));
					user.setPrintCopyFlag(Convert.formatInteger(cols[10]));
					user.setEmailAddress(cols[11]);
					user.setRoleId(MTSRole.SUBSCRIBER.getRoleId());
	
					UserDataVO profile = new UserDataVO();
					profile.setFirstName(user.getFirstName());
					profile.setLastName(user.getLastName());
					profile.setEmailAddress(user.getEmailAddress());
					if (cols.length > 12) profile.setAddress(cols[12]);
					if (cols.length > 13) profile.setAddress2(cols[13]);
					if (cols.length > 14) profile.setCity(cols[14]);
					if (cols.length > 15) profile.setState(cols[15]);
					if (cols.length > 16) profile.setZipCode(cols[16]);
					if (cols.length > 17) profile.setCountryCode(cols[17]);
					profile.setAllowCommunication(1);
					
					user.setProfile(profile);
					users.add(user);

				} catch (Exception e) {
					log.error("ID: " + user.getUserId());
				}
			}
		}
		
		return users;
	}

}
