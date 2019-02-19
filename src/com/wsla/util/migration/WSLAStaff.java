package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.wsla.data.provider.ProviderUserVO;
import com.wsla.util.migration.vo.WSLAStaffFileVO;

/****************************************************************************
 * <p><b>Title:</b> ProviderImporter.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class WSLAStaff extends AbsImporter {

	private static final String PORTAL_SITE_ID = "WSLA_2";

	private List<WSLAStaffFileVO> data;
	private UserLogin ul;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("wslaAdminsFile"), WSLAStaffFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_user where email_address_txt like '%@wsla.mx'");
		delete(sql);
		save();
	}


	/**
	 * Save the imported users to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		ul = new UserLogin(dbConn, getAttributes());
		createProfiles();
		createProfileRoles();
		writeToDB(data);
		bindToDefaultLocation();
	}


	/**
	 * Create a WC profile for each user in the dataset.
	 * Update the dataset and set a profileId for each user so they save properly.
	 */
	private void createProfiles() {
		UserDataVO profile;
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		for (WSLAStaffFileVO user : data) {
			profile = new UserDataVO();
			profile.setFirstName(user.getFirstName());
			profile.setLastName(user.getLastName());
			profile.setEmailAddress(user.getEmail());
			profile.setLanguage("es");
			profile.setAuthenticationId(createAuthRecord(user.getEmail()));
			try {
				pm.updateProfile(profile, dbConn);
				pm.assignCommunicationFlg("WSLA", profile.getProfileId(), 1, dbConn);
				user.setProfileId(profile.getProfileId());
			} catch (DatabaseException e) {
				log.error("could not create user profile for " + user.getUserId(), e);
			}
		}
	}


	/**
	 * create the authentication record.
	 * @param email
	 * @return
	 */
	private String createAuthRecord(String userName) {
		try {
			return ul.saveAuthRecord(null, userName, RandomAlphaNumeric.generateRandom(6), 1);
		} catch (DatabaseException de) {
			log.error("could not create auth record for " + userName, de);
		}
		return null;
	}


	/**
	 * create profile roles so the staff have permission to login
	 */
	private void createProfileRoles() {
		ProfileRoleManager prm = new ProfileRoleManager();
		for (WSLAStaffFileVO user : data) {
			try {
				if (!prm.roleExists(user.getProfileId(), PORTAL_SITE_ID, null, dbConn)) {
					SBUserRole role = new SBUserRole(PORTAL_SITE_ID);
					role.setProfileId(user.getProfileId());
					role.setRoleId("WSLA_CUSTOMER_SVC");
					role.setStatusId(SecurityController.STATUS_ACTIVE);
					prm.addRole(role, dbConn);
				}
			} catch (Exception e) {
				log.error("could not add profile_role for profileId=" + user.getProfileId(), e);
			}
		}
	}


	/**
	 * Create a default user_location_xr for each employee
	 * @param users (OEMs)
	 * @throws Exception 
	 */
	private void bindToDefaultLocation() throws Exception {
		List<ProviderUserVO> xrs = new ArrayList<>(data.size());
		for (WSLAStaffFileVO staff : data) {
			ProviderUserVO vo = new ProviderUserVO();
			vo.setLocationId("WSLA_030"); // WSLA's default warehouse location
			vo.setUserId(staff.getUserId());
			vo.setActiveFlag(1);
			vo.setCreateDate(Convert.getCurrentTimestamp());
			xrs.add(vo);
		}
		writeToDB(xrs);
	}
}