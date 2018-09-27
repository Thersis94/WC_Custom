package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
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
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.wsla.data.provider.ProviderUserVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: ProviderLocationUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the user accounts for the providers
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 25, 2018
 * @updates:
 ****************************************************************************/

public class ProviderLocationUserAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ProviderLocationUserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderLocationUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		boolean isError = false;
		try {
			String locId = req.getParameter("locationId");
			setModuleData(getUsers(locId, new BSTableControlVO(req, ProviderUserVO.class)));
		} catch(Exception e) {
			isError = true;
			Object msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			putModuleData("", 0, false, (String)msg, isError);
		}
	}
	
	/**
	 * Manages the user data for the grid
	 * @param locationId
	 * @param providerUserId
	 * @param bst
	 * @return
	 * @throws DatabaseException 
	 */
	public List<ProviderUserVO> getUsers(String locationId, BSTableControlVO bst) 
	throws DatabaseException {
		List<Object> params = new ArrayList<>();
		params.add(locationId);
		
		StringBuilder sql = new StringBuilder(320);
		sql.append("select * from custom.wsla_provider_user_xr a ");
		sql.append("inner join custom.wsla_user b on a.user_id = b.user_id ");
		sql.append("inner join profile_role d on b.profile_id = d.profile_id ");
		sql.append("and site_id = 'WSLA_1' ");
		sql.append("inner join role e on d.role_id = e.role_id ");
		sql.append("where location_id = ? ");
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append(bst.getSQLOrderBy("last_nm",  "asc"));
		
		// Get the Provider Location users
		DBProcessor db = new DBProcessor(getDBConnection());
		List<ProviderUserVO> users = db.executeSelect(sql.toString(), params, new ProviderUserVO());
		assignProfileData(users);
		
		return users;
	}
	
	/**
	 * Gets the profiles for each user and add the data to the user vo
	 * @param users
	 * @throws DatabaseException
	 */
	protected void assignProfileData(List<ProviderUserVO> users) throws DatabaseException {
		// Get the list of Profiles
		List<String> profileIds = new ArrayList<>();
		for (ProviderUserVO user : users) profileIds.add(user.getProfileId());
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> profiles = pm.searchProfile(getDBConnection(), profileIds);
		
		// Loop the profiles and assign to the appropriate user vo 
		for (UserDataVO profile: profiles) {
			for (ProviderUserVO user : users) {
				if (StringUtil.checkVal(profile.getProfileId()).equals(user.getProfileId())) {
					user.setProfile(profile);
					user.setFormattedPhoneNumbers(profile.getPhoneNumbers(), "<br/>");
					user.setWorkPhoneNumber(profile.getMobilePhone());
					user.setMobilePhoneNumber(profile.getWorkPhone());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		try {
			this.saveUser(req);
		} catch(Exception e) {
			log.error("Unable to save provider user", e);
			putModuleData("", 0, false, AdminConstants.KEY_ERROR_MESSAGE, true);
		}
	}
	
	/**
	 * Saves the profile, role, user and provider user information
	 * @param req
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void saveUser(ActionRequest req) 
	throws Exception {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		ProviderUserVO user = new ProviderUserVO(req);
		UserDataVO profile = new UserDataVO(req);
		Map<String, Object> fieldMap = getProfileExtended(profile, user, pm);

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		// Update / add the profile.  if new, allow communications to them
		if (StringUtil.isEmpty(profile.getProfileId())) {
			pm.updateProfile(profile, getDBConnection());
		} else {
			pm.updateProfilePartially(fieldMap, profile, getDBConnection());
		}
		
		// Update / add the role
		this.saveRole(req, site, profile, user.getActiveFlag() == 1);
		
		// Update / add the wsla user
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		db.save((UserVO) user);
		
		// Update / add the wsla provider user
		db.save(user);
		
		// if the user is the default, reset the other defaults to not the default contact
		if (user.getPrimaryContactFlag() == 1) updateDefaultContact(user);
		
	}
	
	/**
	 * Adds the auth record for a new user.  Checks for the existence (in case 
	 * there is a record for that user) 
	 * @param user
	 * @return
	 * @throws DatabaseException
	 */
	public String addAuthenticationRecord(ProviderUserVO user) throws DatabaseException {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		String authId = login.checkAuth(user.getEmail());
		if (StringUtil.isEmpty(authId))
			login.saveAuthRecord(null, user.getEmail(), RandomAlphaNumeric.generateRandom(10), 1);
		
		return authId;
	}
	
	/**
	 * Processes the extra data for the profile
	 * @param profile
	 * @param user
	 * @return
	 * @throws DatabaseException 
	 */
	protected Map<String, Object> getProfileExtended(UserDataVO profile, ProviderUserVO user, ProfileManager pm) 
	throws DatabaseException {
		profile.setCountryCode(user.getLocale().substring(3));
		profile.setLanguage(user.getLocale().substring(0,2));
		profile.setEmailAddress(user.getEmail());
		profile.addPhone(new PhoneVO(PhoneVO.MOBILE_PHONE, user.getMobilePhoneNumber(), profile.getCountryCode()));
		profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, user.getWorkPhoneNumber(), profile.getCountryCode()));
		profile.setAllowCommunication(1);
		
		// Check to see if the user already exists in the system
		if (StringUtil.isEmpty(profile.getProfileId())) {
			profile.setProfileId(pm.checkProfile(profile, getDBConnection()));
		}
		
		// If the profile is new, go get a new record
		if (StringUtil.isEmpty(profile.getAuthenticationId())) {
			profile.setAuthenticationId(addAuthenticationRecord(user));
		}
		
		// Set the fields to be updated
		Map<String, Object> fieldMap = profile.getDataMap();
		fieldMap.put("LANGUAGE_CD", profile.getLanguage());
		fieldMap.remove("ADDRESS2_TXT");
		fieldMap.remove("ADDRESS_TXT");
		fieldMap.remove("BIRTH_YEAR_NO");
		fieldMap.remove("CASS_VALIDATE_FLG");
		fieldMap.remove("CITY_NM");
		fieldMap.remove("LATITUDE_NO");
		fieldMap.remove("LONGITUDE_NO");
		fieldMap.remove("MAIN_PHONE_TXT");
		fieldMap.remove("MIDDLE_NM");
		fieldMap.remove("PASSWORD_TXT");
		fieldMap.remove("VALID_ADDRESS_FLG");
		
		return fieldMap;
	}
	
	/**
	 * 
	 * @param user
	 * @throws SQLException
	 */
	public void updateDefaultContact(ProviderUserVO user) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_user_xr ");
		sql.append("set primary_contact_flg = 0 where location_id = ? and user_id != ? ");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getLocationId());
			ps.setString(2, user.getUserId());
			ps.executeUpdate();
		}
	}
	
	/**
	 * Updates or saves the profile role value for the user
	 * @param req
	 * @param site
	 * @param profile
	 * @param active
	 * @throws DatabaseException
	 */
	protected void saveRole(ActionRequest req, SiteVO site, UserDataVO profile, boolean active) 
	throws DatabaseException {

		SBUserRole role = new SBUserRole();
		role.setRoleName("");
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(site.getSiteId());
		role.setRoleId(req.getParameter("roleId"));
		role.setProfileId(profile.getProfileId());
		role.setProfileRoleId(req.getParameter("profileRoleId"));
		role.setStatusId(active ? SecurityController.STATUS_ACTIVE : SecurityController.STATUS_DISABLED);
		
		ProfileRoleManager prm = new ProfileRoleManager();
		prm.addRole(role, getDBConnection());
	}
}

