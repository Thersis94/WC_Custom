package com.ram.action.user;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RAMDataFeed libs
import com.ram.datafeed.data.RAMUserVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title: </b>RamUserAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 28, 2014<p/>
 *<b>Changes: </b>
 * May 28, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class RamUserAction extends SBActionAdapter {

	public static final int ROLE_LEVEL_OR_MODULE = 10;
	public static final int ROLE_LEVEL_AUDITOR = 15;
	public static final int ROLE_LEVEL_OEM = 20;
	public static final int ROLE_LEVEL_PROVIDER = 25;
	/**
	 * 
	 */
	public RamUserAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public RamUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("RamUserAction retrieve...");
		// if this is an 'add user' operation, simply return.
		if (StringUtil.checkVal(req.getParameter("addUser")).length() > 0) return;

		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		boolean isAdmin = (role.getRoleLevel() == 100);
		List<RAMUserVO> data = new ArrayList<>();

		String schema = (String)getAttribute("customDbSchema");

		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.FIRST_NM, b.LAST_NM, b.EMAIL_ADDRESS_TXT, ");
		sql.append("c.ROLE_ORDER_NO, c.ROLE_NM, d.PHONE_NUMBER_TXT, ");
		sql.append("e.CUSTOMER_ID, e.CUSTOMER_NM, f.AUDITOR_ID, ");
		sql.append("hc.CUSTOMER_NM as HOSPITAL_NM, hc.CUSTOMER_ID as HOSPITAL_ID ");
		sql.append("from PROFILE_ROLE a ");
		sql.append("inner join PROFILE b on a.PROFILE_ID = b.PROFILE_ID ");
		sql.append("inner join ROLE c on a.ROLE_ID = c.ROLE_ID ");
		sql.append("left outer join PHONE_NUMBER d on a.PROFILE_ID = d.PROFILE_ID and d.PHONE_TYPE_CD = 'HOME' ");
		sql.append("left outer join ").append(schema).append("RAM_CUSTOMER e ");
		sql.append("on a.ATTRIB_TXT_1 = e.CUSTOMER_ID ");
		sql.append("left outer join ").append(schema).append("RAM_AUDITOR f ");
		sql.append("on a.PROFILE_ID = f.PROFILE_ID ");
		sql.append("left outer join ").append(schema).append("RAM_CUSTOMER_PROFILE_XR h ");
		sql.append("on h.PROFILE_ID = a.PROFILE_ID ");
		sql.append("left outer join ").append(schema).append("RAM_CUSTOMER hc ");
		sql.append("on hc.CUSTOMER_ID = h.CUSTOMER_ID where 1 = 1 ");

		// filter by site ID
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String siteId = site.getSiteId();
		if (StringUtil.checkVal(site.getAliasPathParentId()).length() > 0) {
			siteId = site.getAliasPathParentId();
		}
		sql.append("and a.SITE_ID = ? ");

		String profileId = null;
		// if admin, look for profileId passed in on request.
		if (isAdmin) {
			profileId = StringUtil.checkVal(req.getParameter("profileId"));
		} else {
			// otherwise, force filter by logged-in user's profile ID from session
			profileId = retrieveNonAdminProfileId(req);
		}

		if (profileId.length() > 0) sql.append("and a.PROFILE_ID = ? ");
		sql.append("order by a.PROFILE_ID");
		log.debug("RamUserAction retrieve SQL: " + sql.toString() + " | " + profileId);

		int recCtr = 0;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, siteId);
			if (profileId.length() > 0) ps.setString(2, profileId);
			ResultSet rs = ps.executeQuery();

			/*
			 * Since we sort on userName, we need to retrieve all records each 
			 * time to properly paginate the results.
			 */
			String currentProfile = "";
			RAMUserVO user = null;
			while (rs.next()) {
				if (!currentProfile.equals(rs.getString("PROFILE_ID"))) {
					if (user != null) {
						recCtr++;
						data.add(user);
					}
					user = new RAMUserVO(rs);
					currentProfile = rs.getString("PROFILE_ID");
				}
				
				if (rs.getString("HOSPITAL_ID") != null) {
					log.debug("Added " + rs.getString("HOSPITAL_NM") + " to " + rs.getString("PROFILE_ID"));
					user.addHospital(rs.getString("HOSPITAL_ID"), rs.getString("HOSPITAL_NM"));
				}
			}
			
			if (user != null) {
				recCtr++;
				data.add(user);
			}

		} catch (SQLException sqle) {
			log.error("Error retrieving user data, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e); }
			}
		}

		formatUserData(data);

		// sort collection by name
		Collections.sort(data, new RAMUserComparator());

		//Need to paginate the data after retrieving it.
		data = paginateData(req, data);

		putModuleData(data, recCtr, false, null);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("RamUserAction build...");
		Object msg = null;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		boolean isAdmin = (role.getRoleLevel() == 100);

		UserDataVO user = new UserDataVO(req);
		String profileId = null;
		if (isAdmin) {
			profileId = StringUtil.checkVal(req.getParameter("profileId"));
		} else {
			// otherwise, get logged-in user's profile ID from session
			profileId = retrieveNonAdminProfileId(req);
		}
		boolean isProfileInsert = (StringUtil.checkVal(profileId).length() == 0);
		log.debug("isProfileInsert: " + isProfileInsert);
		user.setProfileId(profileId);

		// manage profile
		manageProfile(req, user, isProfileInsert, msg);

		if (isAdmin) { // only admins can manipulate role/auth/auditor records
			// manage role and auth
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			SBUserRole userRole = preformatUserRoleData(req, site, user);
			manageRole(req, userRole, isProfileInsert, msg);

			try {
				manageAuthentication(req, site, user, userRole);
				msg = "You have successfully " + (isProfileInsert ? "created" : "updated") + " the user.";
			} catch (Exception e) {
				msg = "Error creating login for Ram user.";
			}

			// check for role change TO 'auditor' or FROM 'auditor'
			int origRoleLevel = Convert.formatInteger(req.getParameter("origRoleLevel"), -1);
			if (origRoleLevel == ROLE_LEVEL_AUDITOR || userRole.getRoleLevel() == ROLE_LEVEL_AUDITOR) {
				manageAuditor(req, site, user, userRole, origRoleLevel);
			} else if (origRoleLevel == ROLE_LEVEL_OR_MODULE) {
				manageAssociatedHospitals(req, user.getProfileId());
			}
		} else {
			// if non-admin and has changed email address, check auth record.
		}

        // Build the redirect and messages
		// Setup the redirect.
		StringBuilder url = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		url.append(page.getRequestURI());
		if (msg != null) {
			String txt = (String) msg;
			if (txt.toLowerCase().startsWith("you")) {
				url.append("?editUser=true&profileId=").append(user.getProfileId());
				url.append("&msg=").append(msg);
			} else {
				url.append("?msg=").append(msg);
			}
		}

		boolean isJson = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("amid")).length() > 0);
		if (isJson) {
			Map<String, Object> res = new HashMap<>(); 
			res.put("success", true);
			putModuleData(res);
		} else {
			log.debug("RamUserAction redir: " + url);
			putModuleData(msg);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}

	
	/**
	 * Add the hospitals the user is associated with.
	 */
	private void manageAssociatedHospitals(SMTServletRequest req,
			String profileId) {
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO ").append(getAttribute("customDbSchema")).append("RAM_CUSTOMER_PROFILE_XR ");
		sql.append("(CUSTOMER_PROFILE_XR_ID, PROFILE_ID, CUSTOMER_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			// Delete the current hospitals associated with this user.
			deleteCurrentHospitals(profileId);
			String hospitals = StringUtil.checkVal(req.getParameter("associatedHospitals"));
			for (String hospital : hospitals.split("\\|")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, profileId);
				ps.setString(3, hospital);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}

			ps.executeBatch();
		} catch (SQLException e) {
			log.error("Unable to add hospitals for user " + profileId, e);
		}
	}
	
	
	/**
	 * Delete any hospital associations this user has at the moment in order
	 * to make way for the new list of 
	 * @param profileId
	 * @throws SQLException
	 */
	private void deleteCurrentHospitals(String profileId) throws SQLException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("DELETE ").append(getAttribute("customDbSchema")).append("RAM_CUSTOMER_PROFILE_XR ");
		sql.append("WHERE PROFILE_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			
			ps.executeUpdate();
		}
		
	}

	/**
	 * Inserts or updates a user profile
	 * @param req
	 * @param user
	 * @param isProfileInsert
	 * @param msg
	 */
	private void manageProfile(SMTServletRequest req, UserDataVO user, boolean isProfileInsert, Object msg) {
		log.debug("managing user profile...");
		user.setMainPhone(StringUtil.removeNonNumeric(req.getParameter("phoneNumber")));
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			pm.updateProfile(user, dbConn);
		} catch (DatabaseException de) {
			msg = (isProfileInsert ? "Error creating " : "Error updating ") + "RAM user profile.";
			log.error(msg, de);
		}
	}

	/**
	 * Inserts or updates a user's role
	 * @param req
	 * @param userRole
	 * @param isProfileInsert
	 * @param msg
	 */
	private void manageRole(SMTServletRequest req, SBUserRole userRole, boolean isProfileInsert, Object msg) {
		log.debug("managing user role...");
		try {
			ProfileRoleManager prm = new ProfileRoleManager();
			// check to see if this user already has a role
			String profileRoleId = prm.checkRole(userRole.getProfileId(), userRole.getSiteId(), dbConn);
			if (StringUtil.checkVal(profileRoleId).length() > 0) userRole.setProfileRoleId(profileRoleId);
			// insert or update role data
			prm.addRole(userRole, dbConn);
			
		} catch (DatabaseException de) {
			msg = (isProfileInsert ? "Error creating " : "Error updating ") + "RAM user role.";
			log.error(msg, de);
		}
	}

	/**
	 * Inserts authentication record for a user if that user does not already have one.
	 * @param req
	 * @param user
	 * @param userRole
	 * @param msg
	 * @throws ApplicationException 
	 */
	private void manageAuthentication(SMTServletRequest req, SiteVO site, UserDataVO user, SBUserRole userRole) 
			throws ApplicationException {
		log.debug("managing authentication...");
		String loginClass = site.getLoginModule();
		Map<String, Object> lm = new HashMap<String, Object>();
		String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
		lm.put(Constants.ENCRYPT_KEY, encKey);
		lm.put(GlobalConfig.KEY_DB_CONN, dbConn);
		String authId = null;
		try {
			AbstractLoginModule loginModule = SecurityModuleFactoryImpl.getLoginInstance(loginClass, lm);

			/*
			 * Determine which email address to use for checking for an auth record,
			 * Assume the email address hasn't changed.
			 */
			String authEmail = user.getEmailAddress();
			// check for an 'old' email address which indicates an existing user
			String oldEmail = StringUtil.checkVal(req.getParameter("oldEmailAddress"));
			if (oldEmail.length() > 0) {
				// existing user
				if (! oldEmail.equalsIgnoreCase(user.getEmailAddress())) {
					// email address changed, use old email address for auth lookup
					authEmail = oldEmail;
				}
			}

			// retrieve authID for this user if it exists.
			authId = loginModule.retrieveAuthenticationId(authEmail);
			log.debug("authId after check: " + authId);
			boolean updateProfileAuthId = false;
			String authPwd = StringUtil.checkVal(req.getParameter("password"));
			log.debug("authPwd: " + authPwd);
			if (StringUtil.checkVal(authId).length() == 0) {
				// no existing auth record, so update profile with auth ID after auth
				// record is created.
				updateProfileAuthId = true;
			}

			// TODO 2014-08-01 DBargerhuff: commented out, verify that we are not
			// generating passwords for a RAM user.
			//managePassword(req, site, user);

			// create or update the auth record.  We are depending upon the form
			// for password and thus we are not setting the password reset flag to true.
			authId = loginModule.manageUser(authId, user.getEmailAddress(), authPwd, 0);
			user.setAuthenticationId(authId);

			// update profile with auth record
			if (updateProfileAuthId) updateProfileAuth(user);

		} catch (Exception e) {
			log.error("Error creating Ram user authentication record, ", e);
			throw new ApplicationException(e.getMessage());
		}
	}

	/**
	 * Manages the data record for a user of role 'Auditor'.
	 * updated to ensure we always update theAuditor record as that has more data
	 * on it than just active or inactive.  
	 * @param req
	 * @param site
	 * @param user
	 * @param userRole
	 * @param origRoleLevel
	 */
	private void manageAuditor(SMTServletRequest req, SiteVO site, UserDataVO user, SBUserRole userRole, int origRoleLevel) {
		log.debug("Managing auditor...");
		String auditorId = checkAuditor(user.getProfileId());
		if (origRoleLevel != -1 && origRoleLevel != userRole.getRoleLevel() && origRoleLevel == ROLE_LEVEL_AUDITOR) {
				// changed FROM auditor so disable RAM_AUDITOR record
				updateAuditor(user, auditorId, RamUserFacadeAction.PROFILE_STATUS_DISABLED);
		} else {
			updateAuditor(user, auditorId, userRole.getStatusId());
		}
	}

	/**
	 * Inserts or updates a RAM_AUDITOR record
	 * @param profileId
	 * @param auditorId
	 * @param newStatusId
	 */
	private void updateAuditor(UserDataVO user, String auditorId, int newStatusId) {
		log.debug("updating auditor record...");
		boolean isInsert = (StringUtil.checkVal(auditorId).length() == 0);
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		if (isInsert) {
			// insert
			sql.append("insert into ").append(schema).append("RAM_AUDITOR ");
			sql.append("(PROFILE_ID, FIRST_NM, LAST_NM, ACTIVE_FLG, CREATE_DT)").append("values (?,?,?,?,?)");
		} else {
			// update
			sql.append("update ").append(schema).append("RAM_AUDITOR ");
			sql.append("set PROFILE_ID = ?, FIRST_NM = ?, LAST_NM = ?, ACTIVE_FLG = ?, UPDATE_DT = ? ");
			sql.append("where AUDITOR_ID = ?");
		}

		// set active/inactive, default to inactive
		int activeFlg = 0;
		if (newStatusId == RamUserFacadeAction.PROFILE_STATUS_ACTIVE) activeFlg = 1;

		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++, user.getProfileId());
			ps.setString(index++, user.getFirstName());
			ps.setString(index++, user.getLastName());
			ps.setInt(index++, activeFlg);
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			if (! isInsert) {
				ps.setString(index++, auditorId);
			}

			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("Error retrieving RAM auditor ID, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
	}

	/**
	 * Performs a lookup of the auditor ID associated with the profile ID passed in.
	 * @param profileId
	 * @return
	 */
	private String checkAuditor(String profileId) {
		String auditorId = null;
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		sql.append("select AUDITOR_ID from ").append(schema).append("RAM_AUDITOR ");
		sql.append("where PROFILE_ID = ?");

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1,profileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				auditorId = rs.getString("AUDITOR_ID");
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving RAM auditor ID, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		return auditorId;
	}

	/**
	 * Updates user profile with the associated user's authentication ID.
	 * @param user
	 * @param authId
	 */
	private void updateProfileAuth(UserDataVO user) {
		StringBuilder sql = new StringBuilder();
		sql.append("update PROFILE set AUTHENTICATION_ID = ? where PROFILE_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, user.getAuthenticationId());
			ps.setString(2, user.getProfileId());
			ps.execute();
		} catch (SQLException sqle) {
			
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
	}

	/**
	 * Instantiates StringEncrypter to decrypt encrypted user information and
	 * sets the decrypted information on the user data.
	 * @param users
	 * @return
	 */
	private void formatUserData(List<RAMUserVO> users) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException ee) {
			log.error("Error instantiating StringEncrypter, ", ee);
			return;
		}

		for (RAMUserVO user : users) {
			try {
				user.setFirstName(se.decrypt(user.getFirstName()));
			} catch (Exception e) {log.error("Error decrypting first name, " + e.getMessage());}
			try {
				user.setLastName(se.decrypt(user.getLastName()));
			} catch (Exception e) {log.error("Error decrypting last name, " + e.getMessage());}
			try {
				user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			} catch (Exception e) {log.error("Error decrypting email address, " + e.getMessage());}
			try {
				user.setPhoneNumber(se.decrypt(user.getPhoneNumber()));
			} catch (Exception e) {log.error("Error decrypting phone number, " + e.getMessage());}
		}	
		return;
	}

	/**
	 * Returns the profile ID of the logged-in user from the user's session data.
	 * @param req
	 * @return
	 */
	private String retrieveNonAdminProfileId(SMTServletRequest req) {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		return user.getProfileId(); 
	}

	/**
	 * Formats an SBUserRole with role data from the request.
	 * @param req
	 * @param user
	 * @return
	 */
	private SBUserRole preformatUserRoleData(SMTServletRequest req, SiteVO site, UserDataVO user) {
		log.debug("formatUserRoleData...");
		SBUserRole userRole = new SBUserRole();
		userRole.setProfileId(user.getProfileId());
		userRole.setProfileRoleId(req.getParameter("profileRoleId"));
		userRole.setOrganizationId(site.getOrganizationId());
		userRole.setSiteId(site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId());
		userRole.setSiteName(site.getSiteName());
		String roleId = StringUtil.checkVal(req.getParameter("roleId"));
		userRole.setRoleId(roleId);
		String roleLevelId = StringUtil.checkVal(req.getParameter("newRoleLevel"));
		if (roleLevelId.length() > 0) {
			userRole.setRoleLevel(Convert.formatInteger(roleLevelId));
			if (userRole.getRoleLevel() == ROLE_LEVEL_OEM ||
					userRole.getRoleLevel() == ROLE_LEVEL_PROVIDER) {
				// if OEM or PROVIDER, set customer association
				userRole.setAttrib1Txt(req.getParameter("customerId"));
				// ...and for backwards compatibility
				userRole.addAttribute("customerId", userRole.getAttrib1Txt());
			}
		}
		userRole.setStatusId(Convert.formatInteger(req.getParameter("statusId")));
		return userRole;
	}

	/**
	 * Loops the sorted list and returns a list containing the records for the page number that was requested.
	 * @param req
	 * @param sortedList
	 * @return
	 */
	private List<RAMUserVO> paginateData(SMTServletRequest req, List<RAMUserVO> sortedList) {
		int navStart = Convert.formatInteger(req.getParameter("start"), 0);
		int navLimit = Convert.formatInteger(req.getParameter("limit"), 25);
		int navEnd = navStart + navLimit;
		int ctr = -1;
		List<RAMUserVO> paginatedList = new ArrayList<>();
		for (int i = 0; i < sortedList.size(); i++) {
			ctr++;
			// determine which records to add to the paginated list.
			if (ctr >= navStart) {
				if (ctr < navEnd) {
					paginatedList.add(sortedList.get(i));
				} else {
					break;
				}
			} else {
				continue;
			}
		}
		return paginatedList;
	}
}