package com.depuysynthes.gfp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: GFPUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Handle the creation and handling of GFP roles for users
 * and handles the assignment of those users to hospitals.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 ****************************************************************************/

public class GFPUserAction extends SBActionAdapter {
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		String userId = StringUtil.checkVal(req.getParameter("userId"));
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		
		sql.append("SELECT * FROM ").append(customDb).append("DPY_SYN_GFP_USER u ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_HOSPITAL h ");
		sql.append("ON u.HOSPITAL_ID = h.HOSPITAL_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
		sql.append("ON u.PROGRAM_ID = p.PROGRAM_ID ");
		sql.append("LEFT JOIN PROFILE pr on pr.PROFILE_ID = u.PROFILE_ID ");
		if (userId.length() > 0) sql.append("WHERE u.USER_ID = ?");
		List<GFPUserVO> users = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
			if (userId.length() > 0) ps.setString(1, userId);
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				GFPUserVO user = new GFPUserVO(rs);
				user.setProfile(new UserDataVO(rs));
	            	user.getProfile().setFirstName(se.decrypt(user.getProfile().getFirstName()));
	            	user.getProfile().setLastName(se.decrypt(user.getProfile().getLastName()));
	            	user.getProfile().setEmailAddress(se.decrypt(user.getProfile().getEmailAddress()));
				users.add(user);
			}
		} catch (SQLException e) {
			log.error("Unable to get user " + (userId.length()>0? "with id " + userId: ""), e);
			throw new ActionException(e);
		} catch (EncryptionException e) {
			log.error("Unable to decode profile data", e);
			throw new ActionException(e);
		}
		
		// If a single user is being edited the full list of programs and
		// hospitals will be needed for use in the edit form.
		if (userId.length() > 0) {
			req.setAttribute("hospitals", getHospitals());
			req.setAttribute("programs", getPrograms());
		}
	}
	
	
	/**
	 * Call out to the ProgramAction in order to get a list of all products
	 * @param req
	 * @throws ActionException
	 */
	private List<GFPProgramVO> getPrograms() throws ActionException {
		GFPProgramAction p = new GFPProgramAction();
		p.setDBConnection(dbConn);
		p.setAttributes(attributes);
		
		return p.getAllPrograms();
	}
	
	
	/**
	 * Get all hospitals from the database
	 * @param req
	 * @throws ActionException
	 */
	private Map<String, String> getHospitals() throws ActionException {
		StringBuilder sql = new StringBuilder(75);
		
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_GFP_HOSPITAL");
		
		Map<String, String> hospitals = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) 
				hospitals.put(rs.getString("HOSPITAL_ID"), rs.getString("HOSPITAL_NM"));
			
		} catch (SQLException e) {
			log.error("Unable to get list of all hospitals", e);
			throw new ActionException(e);
		}
		return hospitals;
	}
	

	public void delete(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(75);
		
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_GFP_USER WHERE USER_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("userId"));
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to delete user with id " + req.getParameter("userId"));
			throw new ActionException(e);
		}
	}
	

	public void update(SMTServletRequest req) throws ActionException {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(175);
		GFPUserVO user = new GFPUserVO(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		user.setProfile(new UserDataVO(req));
		updateProfile(user, site.getOrganizationId());
		updateAuthentication(user, site.getAliasPathParentId(), req.getParameter("password"), Convert.formatBoolean(req.getParameter("overwritePassword")));
		
		if (user.getUserId() == null) {
			sql.append("INSERT INTO ").append(customDb).append("DPY_SYN_GFP_USER ");
			sql.append("(HOSPITAL_ID, PROGRAM_ID, PROFILE_ID, ACTIVE_FLG, CREATE_DT, USER_ID) ");
			sql.append("VALUES(?,?,?,?,?,?)");
			user.setUserId(new UUIDGenerator().getUUID());
		} else {
			sql.append("UPDATE ").append(customDb).append("DPY_SYN_GFP_USER SET ");
			sql.append("HOSPITAL_ID=?, PROGRAM_ID=?, PROFILE_ID=?, ACTIVE_FLG=?, ");
			sql.append("CREATE_DT=? WHERE USER_ID=?");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, user.getHospitalId());
			ps.setString(i++, user.getProgramId());
			ps.setString(i++, user.getProfileId());
			ps.setInt(i++, user.getActiveFlg());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, user.getUserId());
		} catch (SQLException e) {
			log.error("Unable to update user with id " + user.getUserId(), e);
			throw new ActionException(e);
		}
	}


	/**
	 * Update the Authentication information for the user.  If the user already 
	 * has an entry in the authentication table we only update that if explicitly
	 * stated.
	 * @param user
	 * @param siteId
	 * @param password
	 * @param overwritePassword
	 * @throws ActionException
	 */
	private void updateAuthentication(GFPUserVO user, String siteId, String password, boolean overwritePassword) throws ActionException {
		try {
			if (overwritePassword || user.getProfile().getAuthenticationId() == null) {
				UserLogin ul = new UserLogin(dbConn, (String) attributes.get(Constants.ENCRYPT_KEY));
					ul.modifyUser(user.getProfile().getAuthenticationId(), user.getProfile().getEmailAddress(), password, 0);
			}
	
			ProfileRoleManager prm = new ProfileRoleManager();
			int userState = user.getActiveFlg() == 1? 20 : 5;
			prm.addRole(user.getProfileId(), siteId, "10", userState, dbConn);
		} catch (DatabaseException e) {
			log.error("Unable to update authentication and role infromation for user profile id " + user.getProfileId(), e);
			throw new ActionException(e);
		}
	}


	/**
	 * Update the profile associated with the current user.
	 * @param user
	 * @throws ActionException
	 */
	private void updateProfile(GFPUserVO user, String orgId) throws ActionException {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			// If no profile id was passed we need to either find or create a
			// proper profile id.
			if (user.getProfileId() == null) {
				String profileId = pm.checkProfile(user.getProfile(), dbConn);
				if (profileId == null) {
					profileId = new UUIDGenerator().getUUID();
					user.getProfile().setProfileId(profileId);
				} else {
					user.setProfile(pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, orgId));
				}
				user.setProfileId(profileId);
			}
			pm.updateProfile(user.getProfile(), dbConn);
		} catch (DatabaseException e) {
			log.error("Unable to update profile properly.", e);
			throw new ActionException(e);
		}
	}
}
