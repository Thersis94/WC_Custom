package com.wsla.action;

// JDK 1.8.x
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: BasePortalAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Base class that provides common interfacing for the 
 * inserting of data into the ledger, calling the workflow engine and other common activities
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 29, 2018
 * @updates:
 ****************************************************************************/

public class BasePortalAction extends SBActionAdapter {

	/**
	 * 
	 */
	public BasePortalAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BasePortalAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param summary if the summary is passed, the first param will be added to the bean
	 * @param req
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(String userId, ActionRequest req, String... summary) 
	throws InvalidDataException, DatabaseException {
		// Create the ledger and fill out the bean
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		if (summary != null && summary.length > 0) ledger.setSummary(summary[0]);
		
		// Add the user's profile id and user id
		if (StringUtil.isEmpty(ledger.getDispositionBy())) {
			ledger.setDispositionBy(userId);
		}
		
		// Add the ledger entry
		this.addLedger(ledger);
		
		return ledger;
	}
	
	/**
	 * 
	 * @param ledger
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void addLedger(TicketLedgerVO ledger) throws InvalidDataException, DatabaseException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(ledger);
	}
	
	/**
	 * Saves the user information and associated profile.
	 * @param site SMT Site information.  Needed to set proper roles and authentication
	 * for the site
	 * @param user WSLA User object. Saves that information.  If saving the profile, the profile must
	 * be loaded into the user object
	 * @param hasAuth If true, authentication record and roles will be updated
	 * @param hasLoc if true, address info will be stored for the user
	 * @throws Exception
	 */
	public void saveUser(SiteVO site, UserVO user, boolean hasAuth, boolean hasLoc) 
	throws Exception {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		if (user.getProfile() == null) throw new InvalidDataException("Profile must be present");
		
		Map<String, Object> fieldMap = getProfileExtended(user, pm, hasAuth, hasLoc);

		// Update / add the profile.  if new, allow communications to them
		if (StringUtil.isEmpty(user.getProfileId())) {
			pm.updateProfile(user.getProfile(), getDBConnection());
			user.setProfileId(user.getProfile().getProfileId());
		} else {
			pm.updateProfilePartially(fieldMap, user.getProfile(), getDBConnection());
		}
		
		// Update / add the role
		if (hasAuth) this.saveRole(site, user, user.getActiveFlag() == 1);
		
		// Update / add the wsla user  Search for the user id if its not assigned
		// This avoids duplicating the user record
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		if (StringUtil.isEmpty(user.getUserId())) 
			user.setUserId(getUserIdByProfileId(user.getProfileId()));
		
		db.save(user);
	}
	
	/**
	 * Checks for the existence of a user id based upon a profile id
	 * @param profileId
	 * @return
	 * @throws SQLException
	 */
	public String getUserIdByProfileId(String profileId) throws SQLException {
		
		StringBuilder sql = new StringBuilder(64);
		sql.append("select user_id from ").append(getCustomSchema()).append("wsla_user ");
		sql.append("where profile_id = ?");
		
		String userId = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) userId = rs.getString(1);
			}
		}
		
		return userId;
	}
	
	/**
	 * Processes the extra data for the profile
	 * @param profile
	 * @param user
	 * @return
	 * @throws DatabaseException 
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	protected Map<String, Object> getProfileExtended(UserVO user, ProfileManager pm, boolean hasAuth, boolean hasLoc) 
	throws com.siliconmtn.exception.DatabaseException {
		UserDataVO profile = user.getProfile();
		Map<String, Object> fieldMap = profile.getDataMap();
		profile.setEmailAddress(user.getEmail());
		profile.setAllowCommunication(1);
		
		// Add the country and language of the locale is present
		if (! StringUtil.isEmpty(user.getLocale())) {
			profile.setCountryCode(user.getLocale().substring(3));
			profile.setLanguage(user.getLocale().substring(0,2));
			fieldMap.put("LANGUAGE_CD", profile.getLanguage());
		} else {
			fieldMap.remove("COUNTRY_CD");
			fieldMap.remove("LANGUAGE_CD");
		}
		
		// Add the phones if present
		if (! StringUtil.isEmpty(profile.getMobilePhone()))
			profile.addPhone(new PhoneVO(PhoneVO.MOBILE_PHONE, profile.getMobilePhone(), profile.getCountryCode()));
		
		if (! StringUtil.isEmpty(profile.getWorkPhone()))
			profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, profile.getWorkPhone(), profile.getCountryCode()));
		
		
		// Check to see if the user already exists in the system
		if (StringUtil.isEmpty(profile.getProfileId())) {
			profile.setProfileId(pm.checkProfile(profile, getDBConnection()));
		}
		
		// If the profile is new, go get a new record
		if (hasAuth && StringUtil.isEmpty(profile.getAuthenticationId())) {
			profile.setAuthenticationId(addAuthenticationRecord(user));
		}
		
		// Set the fields to be updated
		if (! hasLoc) fieldMap.remove("ADDRESS2_TXT");
		if (! hasLoc) fieldMap.remove("ADDRESS_TXT");
		if (! hasLoc) fieldMap.remove("BIRTH_YEAR_NO");
		if (! hasLoc) fieldMap.remove("CASS_VALIDATE_FLG");
		if (! hasLoc) fieldMap.remove("CITY_NM");
		if (! hasLoc) fieldMap.remove("LATITUDE_NO");
		if (! hasLoc) fieldMap.remove("LONGITUDE_NO");
		fieldMap.remove("MAIN_PHONE_TXT");
		fieldMap.remove("MIDDLE_NM");
		fieldMap.remove("PASSWORD_TXT");
		if (! hasLoc) fieldMap.remove("VALID_ADDRESS_FLG");
		
		return profile.getDataMap();
	}
	
	/**
	 * Adds the auth record for a new user.  Checks for the existence (in case 
	 * there is a record for that user) 
	 * @param user
	 * @return
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public String addAuthenticationRecord(UserVO user) throws com.siliconmtn.exception.DatabaseException {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		String authId = login.checkAuth(user.getEmail());
		if (StringUtil.isEmpty(authId))
			authId = login.saveAuthRecord(null, user.getEmail(), RandomAlphaNumeric.generateRandom(10), 1);
		
		return authId;
	}
	
	/**
	 * Updates or saves the profile role value for the user
	 * @param req
	 * @param site
	 * @param profile
	 * @param active
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	protected void saveRole(SiteVO site, UserVO user, boolean active) 
	throws com.siliconmtn.exception.DatabaseException {

		SBUserRole role = new SBUserRole();
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(site.getSiteId());
		role.setRoleId(user.getRoleId());
		role.setProfileId(user.getProfileId());
		role.setProfileRoleId(user.getProfileId());
		role.setStatusId(active ? SecurityController.STATUS_ACTIVE : SecurityController.STATUS_DISABLED);
		
		ProfileRoleManager prm = new ProfileRoleManager();
		prm.addRole(role, getDBConnection());
	}
}

