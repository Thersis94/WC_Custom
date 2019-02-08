package com.perfectstorm.action.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.MemberVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
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
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: MemberWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages users information and front ends the user
 * data vo (profile) information
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 8, 2019
 * @updates:
 ****************************************************************************/

public class MemberWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "member";
	
	/**
	 * 
	 */
	public MemberWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		this.setModuleData(getMembers(site, new BSTableControlVO(req, MemberVO.class)));
	}
	
	/**
	 * Gets the list of members and the customer associations
	 * @param bst
	 * @return
	 */
	public GridDataVO<MemberVO> getMembers(SiteVO site, BSTableControlVO bst) {
		List<Object> vals = new ArrayList<>();
		vals.add(site.getSiteId());
		
		StringBuilder sql = new StringBuilder(832);
		sql.append("select alias_nm, gender_cd, prefix_nm, a.profile_id, authentication_id, a.member_id, a.first_nm, ");
		sql.append(" a.last_nm, a.email_address_txt, a.phone_number_txt, locale_txt,");
		sql.append(" r.role_id, role_nm, profile_role_id,string_agg(customer_nm, ',') as customers_txt ");
		sql.append("from ").append(getCustomSchema()).append("ps_member a ");
		sql.append("inner join profile p on a.profile_id = p.profile_id ");
		sql.append("inner join profile_role pr on p.profile_id = pr.profile_id ");
		sql.append("inner join role r on pr.role_id = r.role_id ");
		sql.append("left outer join ").append(getCustomSchema());
		sql.append("ps_customer_member_xr b on a.member_id = b.member_id ");
		sql.append("left outer join ").append(getCustomSchema());
		sql.append("ps_customer c on b.customer_id = c.customer_id ");
		sql.append("where site_id = ? ");
		sql.append("group by alias_nm, gender_cd, prefix_nm, authentication_id, a.profile_id, a.member_id, a.first_nm, ");
		sql.append("a.last_nm, a.email_address_txt, a.phone_number_txt, locale_txt, ");
		sql.append("r.role_id, role_nm, profile_role_id order by a.last_nm ");
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MemberVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		MemberVO member = new MemberVO(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		log.info("Member: " + member);
		log.info("Email: " + member.getEmailAddress() + "|" + member.getProfile().getEmailAddress());
		log.info("Site: " + site);

		try {
			// Save the profile
			this.saveMember(site, member, true, true);
			
			// Save the member
			db.save(member);
			putModuleData(member);
		} catch (Exception e) {
			log.error("Unable to save member: " + member, e);
			putModuleData(member, 1, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves the user information and associated profile.
	 * @param site SMT Site information.  Needed to set proper roles and authentication
	 * for the site
	 * @param member PS User object. Saves that information.  If saving the profile, the profile must
	 * be loaded into the user object
	 * @param hasAuth If true, authentication record and roles will be updated
	 * @param hasLoc if true, address info will be stored for the user
	 * @throws Exception
	 */
	public void saveMember(SiteVO site, MemberVO member, boolean hasAuth, boolean hasLoc) 
			throws Exception {
		if (member.getProfile() == null) 
			throw new InvalidDataException("Profile must be present");

		UserDataVO profile = member.getProfile();
		boolean isInsert = StringUtil.isEmpty(profile.getProfileId());

		//transpose some data between the UserVO and UserDataVO
		configureProfile(member, profile);

		//create or update the auth record before saving the profile
		if (hasAuth && StringUtil.isEmpty(profile.getAuthenticationId()))
			profile.setAuthenticationId(addAuthenticationRecord(member));

		// Update / add the profile.
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		pm.updateProfile(profile, getDBConnection());

		// opt-in the user if this is a new record
		if (isInsert) {
			pm.assignCommunicationFlg(site.getOrganizationId(), profile.getProfileId(), 
					profile.getAllowCommunication(), getDBConnection(),null);
		}

		//put the saved/updated UserDataVO back onto the UserVO
		member.setProfile(profile);

		if (StringUtil.isEmpty(member.getProfileId()))
			member.setProfileId(profile.getProfileId());

		// Update / add the role
		if (hasAuth)
			saveRole(site, member);

		// Update / add the wsla user  Search for the user id if its not assigned
		// This avoids duplicating the user record
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		if (StringUtil.isEmpty(member.getMemberId()))
			member.setMemberId(getUserIdByProfileId(member.getProfileId()));

		db.save(member);
	}
	
	/**
	 * Adds the auth record for a new user.  Checks for the existence (in case 
	 * there is a record for that user) 
	 * @param member
	 * @return
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException 
	 */
	public String addAuthenticationRecord(MemberVO member) 
	throws com.siliconmtn.exception.DatabaseException {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		String authId = login.checkAuth(member.getEmailAddress());
		if (StringUtil.isEmpty(authId))
			authId = login.saveAuthRecord(null, member.getEmailAddress(), RandomAlphaNumeric.generateRandom(10), 1);

		return authId;
	}
	
	/**
	 * Transpose certain data from UserVO to UserDataVO for saving to the WC core
	 * @param member
	 * @param profile
	 */
	private void configureProfile(MemberVO member, UserDataVO profile) {
		profile.setEmailAddress(member.getEmailAddress());
		profile.setAllowCommunication(1);

		// Add the country and language of the locale if present
		if (! StringUtil.isEmpty(member.getLocale())) {
			profile.setCountryCode(member.getLocale().substring(3));
			profile.setLanguage(member.getLocale().substring(0,2));
			log.debug(String.format("Set country=%s and language=%s from Locale %s", 
					profile.getCountryCode(), profile.getLanguage(), member.getLocale()));
		}

		// Replace (recreate) the phone#s now that we have established a country code
		if (! StringUtil.isEmpty(profile.getMobilePhone()))
			profile.addPhone(new PhoneVO(PhoneVO.MOBILE_PHONE, profile.getMobilePhone(), profile.getCountryCode()));

		if (! StringUtil.isEmpty(profile.getWorkPhone()))
			profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, profile.getWorkPhone(), profile.getCountryCode()));
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
	 * Updates or saves the profile role value for the user
	 * @param site
	 * @param member
	 * @param isActive
	 * @return the RoleVO created by the method, which contains any generated PKID
	 * @throws com.siliconmtn.exception.DatabaseException
	 */
	protected SBUserRole saveRole(SiteVO site, MemberVO member) 
	throws com.siliconmtn.exception.DatabaseException {
		SBUserRole role = new SBUserRole();
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId())); //use parent site
		role.setProfileRoleId(member.getProfileRoleId());
		role.setProfileId(member.getProfileId());
		role.setRoleId(member.getRoleId());
		role.setStatusId(SecurityController.STATUS_ACTIVE);

		new ProfileRoleManager().addRole(role, getDBConnection());
		return role;
	}
}

