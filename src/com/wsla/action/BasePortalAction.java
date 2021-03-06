package com.wsla.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
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
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.resource.WCResourceBundle;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.wsla.data.ticket.StatusCode;
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
	 * @param dbConn
	 * @param attributes
	 */
	public BasePortalAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/**
	 * 
	 * @param summary if the summary is passed, the first param will be added to the bean
	 * @param req
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public TicketLedgerVO addLedger(String userId, ActionRequest req, StatusCode statusCode, String summary) 
			throws InvalidDataException, DatabaseException {
		// Create the ledger and fill out the bean
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		ledger.setStatusCode(statusCode);
		if (!StringUtil.isEmpty(summary))
			ledger.setSummary(summary);

		// Add the user's profile id and user id
		if (StringUtil.isEmpty(ledger.getDispositionBy()))
			ledger.setDispositionBy(userId);

		// Add the ledger entry
		addLedger(ledger);

		return ledger;
	}


	/**
	 * Save the LedgerVO to the database
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
		if (user.getProfile() == null) 
			throw new InvalidDataException("Profile must be present");

		UserDataVO profile = user.getProfile();
		boolean isInsert = StringUtil.isEmpty(profile.getProfileId());
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);

		// Ensure we don't attempt to add the same user twice
		if (isInsert && hasAuth && !StringUtil.isEmpty(getUserIdFromEmail(user.getEmail())))
			throw new ActionException("User already exists");

		//transpose some data between the UserVO and UserDataVO
		configureProfile(user, profile);

		//create or update the auth record before saving the profile
		if (hasAuth)
			profile.setAuthenticationId(saveAuthenticationRecord(profile));

		// Update / add the profile.
		pm.updateProfile(profile, getDBConnection());

		// opt-in the user if this is a new record
		if (isInsert) {
			pm.assignCommunicationFlg(site.getOrganizationId(), profile.getProfileId(), 
					profile.getAllowCommunication(), getDBConnection(),null);
		}

		//put the saved/updated UserDataVO back onto the UserVO
		user.setProfile(profile);

		if (StringUtil.isEmpty(user.getProfileId()))
			user.setProfileId(profile.getProfileId());

		// Update / add the role
		if (hasAuth)
			saveRole(site, user, user.getActiveFlag() == 1);

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
	public String getUserIdByProfileId(String profileId) {
		String sql = StringUtil.join("select user_id from ", getCustomSchema(), "wsla_user where profile_id = ?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getString(1);

		} catch (SQLException sqle) {
			log.warn("could not find existing user.  Message: " + sqle.getMessage());
		}
		return null;
	}


	/**
	 * Transpose certain data from UserVO to UserDataVO for saving to the WC core
	 * @param user
	 * @param profile
	 */
	private void configureProfile(UserVO user, UserDataVO profile) {
		profile.setEmailAddress(user.getEmail());
		profile.setAllowCommunication(1);

		// Add the country and language of the locale if present
		if (! StringUtil.isEmpty(user.getLocale())) {
			profile.setCountryCode(user.getLocale().substring(3));
			profile.setLanguage(user.getLocale().substring(0,2));
			log.debug(String.format("Set country=%s and language=%s from Locale %s", 
					profile.getCountryCode(), profile.getLanguage(), user.getLocale()));
		}

		// Replace (recreate) the phone#s now that we have established a country code
		if (! StringUtil.isEmpty(profile.getMobilePhone())) {
			profile.addPhone(new PhoneVO(PhoneVO.MOBILE_PHONE, profile.getMobilePhone(), profile.getCountryCode()));
			//if the UserVO doesn't have a main phone, use mobile from the profile data
			if (StringUtil.isEmpty(user.getMainPhone()))
				user.setMainPhone(profile.getMobilePhone());
		}

		if (! StringUtil.isEmpty(profile.getWorkPhone()))
			profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, profile.getWorkPhone(), profile.getCountryCode()));
	}


	/**
	 * Adds the auth record for a new user.  Checks for the existence (in case 
	 * there is a record for that user) 
	 * @param user
	 * @return
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public String saveAuthenticationRecord(UserDataVO profile) throws com.siliconmtn.exception.DatabaseException {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		String authId = login.checkAuth(profile.getEmailAddress()); //lookup authId using the NEW email address

		//if we find an authId using the NEW email, see if it matches the old authId (if we had one)
		if (!StringUtil.checkVal(authId).equals(StringUtil.checkVal(profile.getAuthenticationId()))) {
			//edit the existing record, we have a changed email address
			//note use of the pre-existing authenticationId
			authId = login.saveAuthRecord(profile.getAuthenticationId(), profile.getEmailAddress(), UserLogin.DUMMY_PSWD, 0, false);
		} else if (StringUtil.isEmpty(authId)) {
			//add a record, there wasn't one prior
			authId = login.saveAuthRecord(null, profile.getEmailAddress(), RandomAlphaNumeric.generateRandom(10), 1, false);
		} //the 'else' here is that the auth record does not need modification or creation - do nothing

		return authId;
	}


	/**
	 * Updates or saves the profile role value for the user
	 * @param site
	 * @param user
	 * @param isActive
	 * @return the RoleVO created by the method, which contains any generated PKID
	 * @throws com.siliconmtn.exception.DatabaseException
	 */
	protected SBUserRole saveRole(SiteVO site, UserVO user, boolean isActive) 
			throws com.siliconmtn.exception.DatabaseException {
		SBUserRole role = new SBUserRole();
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId())); //use parent site
		role.setProfileRoleId(user.getProfileRoleId());
		role.setProfileId(user.getProfileId());
		role.setRoleId(user.getRoleId());
		role.setStatusId(isActive ? SecurityController.STATUS_ACTIVE : SecurityController.STATUS_DISABLED);

		new ProfileRoleManager().addRole(role, getDBConnection());
		return role;
	}

	/**
	 * Returns the resource bundle for the logged in user
	 * 
	 * @param req
	 * @return
	 */
	public ResourceBundle getResourceBundle(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO udvo = getAdminUser(req);
		return WCResourceBundle.getBundle(site, udvo);  
	}

	/**
	 * Get the base user data for the user id
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException 
	 */
	public UserVO getUser(String userId) throws SQLException {
		UserVO user = new UserVO();
		user.setUserId(userId);

		try {
			DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
			dbp.getByPrimaryKey(user);
		} catch (InvalidDataException | DatabaseException e) {
			throw new SQLException(e);
		}

		return user;
	}

	/**
	 * Returns a list of users for a given role
	 * 
	 * @param roleId
	 * @return
	 */
	public List<UserVO> getUsersByRole(String roleId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_user u ");
		sql.append(DBUtil.INNER_JOIN).append("profile p on u.profile_id = p.profile_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role pr on p.profile_id = pr.profile_id ");
		sql.append("where role_id = ? ");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(roleId), new UserVO());
	}

	/**
	 * Searches for a user by email address
	 * 
	 * @param emailAddress
	 * @return
	 * @throws SQLException
	 */
	public String getUserIdFromEmail(String emailAddress) {
		if (StringUtil.isEmpty(emailAddress))
			return null;

		String sql = StringUtil.join("select user_id from ", getCustomSchema(), "wsla_user where email_address_txt = ?");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<UserVO> data = dbp.executeSelect(sql, Arrays.asList(emailAddress), new UserVO());
		return data != null && !data.isEmpty() ? data.get(0).getUserId() : null;
	}
}