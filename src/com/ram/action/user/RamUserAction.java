package com.ram.action.user;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AbstractPasswordComplexity;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PasswordException;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.PasswordComplexityFactory;
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
	
	private final int ROLE_LEVEL_AUDITOR = 15;
	
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
		List<UserDataVO> data = new ArrayList<>();
		
		String schema = (String)getAttribute("customDbSchema");
		
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.FIRST_NM, b.LAST_NM, b.EMAIL_ADDRESS_TXT, c.ROLE_ORDER_NO, d.AUDITOR_ID ");
		sql.append("from PROFILE_ROLE a ");
		sql.append("inner join PROFILE b on a.PROFILE_ID = b.PROFILE_ID ");
		sql.append("inner join ROLE c on a.ROLE_ID = c.ROLE_ID ");
		sql.append("left outer join ").append(schema).append("RAM_AUDITOR d ");
		sql.append("on a.PROFILE_ID = d.PROFILE_ID where 1 = 1 ");
		
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
		
		if (profileId.length() > 0) sql.append("and a.PROFILE_ID = ?");
		log.debug("RamUserAction retrieve SQL: " + sql.toString() + " | " + profileId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, siteId);
			if (profileId.length() > 0) ps.setString(2, profileId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				UserDataVO user = new UserDataVO();
				user.setProfileId(rs.getString("PROFILE_ID"));
				user.setFirstName(rs.getString("FIRST_NM"));
				user.setLastName(rs.getString("LAST_NM"));
				user.setEmailAddress(rs.getString("EMAIL_ADDRESS_TXT"));
				
				SBUserRole su = new SBUserRole();
				su.setProfileRoleId(rs.getString("PROFILE_ROLE_ID"));
				su.setSiteId(rs.getString("SITE_ID"));
				su.setRoleId(rs.getString("ROLE_ID"));
				su.setRoleLevel(rs.getInt("ROLE_ORDER_NO"));
				su.setStatusId(rs.getInt("STATUS_ID"));
				// attrib1Txt contains the customer ID to whom this user is associated.
				su.setAttrib1Txt(rs.getString("ATTRIB_TXT_1"));
				
				// set the auditor ID value if present
				su.addAttribute("auditorId", rs.getString("AUDITOR_ID"));
				
				// set the role data on the user vo as extended data
				user.setUserExtendedInfo(su);
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
		Collections.sort(data, new UserDataComparator());
		
		log.debug("user data size: " + data.size());
		
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
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
		
		// manage role and auth
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole userRole = formatUserRoleData(req, site, user);

		if (isAdmin) { // only admins can manipulate role/auth/auditor records
			if (msg == null) { // profile added successfully, continue.
				// manage role
				manageRole(req, userRole, isProfileInsert, msg);
				
				if (msg == null) { // role added successfully, continue.
					try {
						manageAuthentication(req, site, user, userRole);
						msg = "You have successfully " + (isProfileInsert ? "created" : "updated") + " the user.";
					} catch (Exception e) {
						msg = "Error creating login for Ram user.";
					}
				}
				
				// check for role change TO 'auditor' or FROM 'auditor'
				int origRoleLevel = Convert.formatInteger(req.getParameter("origRoleLevel"), -1);
				if (origRoleLevel == ROLE_LEVEL_AUDITOR || userRole.getRoleLevel() == ROLE_LEVEL_AUDITOR) {
					manageAuditor(req, site, user, userRole, origRoleLevel);
				}
			}
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
		
		putModuleData(msg);
		
		log.debug("RamUserAction redir: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
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
		ProfileRoleManager prm = new ProfileRoleManager();
		try {
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
			
			authId = loginModule.retrieveAuthenticationId(user.getEmailAddress());
			
			log.debug("authId after check: " + authId);
			if (StringUtil.checkVal(authId).length() == 0) {
				
				// create password for this user
				managePassword(req, site, user);
				
				// create auth record.
				authId = loginModule.manageUser(authId, user.getEmailAddress(), user.getPassword(), 1);
				user.setAuthenticationId(authId);
				
				// update profile with auth record
				updateProfileAuth(user);
			}
			
		} catch (Exception e) {
			log.error("Error creating Ram user authentication record, ", e);
			throw new ApplicationException(e.getMessage());
		}
	}
	
	/**
	 * Manages the data record for a user of role 'Auditor'.
	 * @param req
	 * @param site
	 * @param user
	 * @param userRole
	 * @param origRoleLevel
	 */
	private void manageAuditor(SMTServletRequest req, SiteVO site, UserDataVO user, SBUserRole userRole, int origRoleLevel) {
		log.debug("Managing auditor...");
		// lookup auditor ID if it exists.
		String auditorId = checkAuditor(user.getProfileId());
		if (origRoleLevel == -1) {
			// new auditor user, insert new record
			updateAuditor(user.getProfileId(), auditorId, userRole.getStatusId());
		} else {
			if (origRoleLevel != userRole.getRoleLevel()) {
				// role changed...is it TO or FROM?
				if (origRoleLevel == ROLE_LEVEL_AUDITOR) {
					// changed FROM auditor so disable RAM_AUDITOR record
					updateAuditor(user.getProfileId(), auditorId, RamUserFacadeAction.PROFILE_STATUS_DISABLED);
				} else if (userRole.getRoleLevel() == ROLE_LEVEL_AUDITOR) {
					// changed TO auditor, set status according to current status
					updateAuditor(user.getProfileId(), auditorId, userRole.getStatusId());
				}
			} else {
				int origStatusId = Convert.formatInteger(req.getParameter("origStatusId"), -1);
				if (userRole.getStatusId() != origStatusId && origStatusId > -1) {
					// role hasn't changed but status has changed, reflect that in auditor record
					updateAuditor(user.getProfileId(), auditorId, userRole.getStatusId());
				}
			}
		}
	}
	
	/**
	 * Inserts or updates a RAM_AUDITOR record
	 * @param profileId
	 * @param auditorId
	 * @param newStatusId
	 */
	private void updateAuditor(String profileId, String auditorId, int newStatusId) {
		log.debug("updating auditor record...");
		boolean isInsert = (StringUtil.checkVal(auditorId).length() == 0);
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		if (isInsert) {
			// insert
			sql.append("insert into ").append(schema).append("RAM_AUDITOR ");
			sql.append("(PROFILE_ID, ACTIVE_FLG, CREATE_DT)").append("values (?,?,?)");
		} else {
			// update
			sql.append("update ").append(schema).append("RAM_AUDITOR ");
			sql.append("set PROFILE_ID = ?, ACTIVE_FLG = ?, UPDATE_DT = ? ");
			sql.append("where AUDITOR_ID = ?");
		}
		
		// set active/inactive, default to inactive
		int activeFlg = 0;
		if (newStatusId == RamUserFacadeAction.PROFILE_STATUS_ACTIVE) activeFlg = 1;
		
		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++,profileId);
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
	 * Creates a password for the new authentication record being created for a user.
	 * @param req
	 * @param site
	 * @param user
	 * @throws PasswordException
	 * @throws EncryptionException 
	 */
	private void managePassword(SMTServletRequest req, SiteVO site, UserDataVO user) 
			throws PasswordException, EncryptionException {
		log.debug("managing password...");
		AbstractPasswordComplexity apc = null;
		try {
			apc = PasswordComplexityFactory.getInstance(site.getPasswordModule(), attributes);
		} catch (ApplicationException ae) {
			log.error("Error instantiating password complexity module, ", ae);
			throw new PasswordException(ae.getMessage());
		}
		if (apc != null) {
			String pwd = apc.generate();
			try {
				user.setPassword(apc.encrypt(pwd));
			} catch (ApplicationException pe) {
				log.error("Error encrypting password for Ram user, ", pe);
				throw new EncryptionException(pe.getMessage());
			}
		}
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
	private void formatUserData(List<UserDataVO> users) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException ee) {
			log.error("Error instantiating StringEncrypter, ", ee);
			return;
		}
		
		for (UserDataVO user : users) {
			try {
				user.setFirstName(se.decrypt(user.getFirstName()));
			} catch (Exception e) {log.error("Error decrypting first name, " + e.getMessage());}
			try {
				user.setLastName(se.decrypt(user.getLastName()));
			} catch (Exception e) {log.error("Error decrypting last name, " + e.getMessage());}
			try {
				user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			} catch (Exception e) {log.error("Error decrypting email address, " + e.getMessage());}
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
	private SBUserRole formatUserRoleData(SMTServletRequest req, SiteVO site, UserDataVO user) {
		log.debug("formatUserRoleData...");
		SBUserRole userRole = new SBUserRole();
		userRole.setProfileId(user.getProfileId());
		userRole.setProfileRoleId(req.getParameter("profileRoleId"));
		userRole.setOrganizationId(site.getOrganizationId());
		userRole.setSiteId(site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId());
		userRole.setSiteName(site.getSiteName());
		String roleVal = StringUtil.checkVal(req.getParameter("roleId"));
		if (roleVal.indexOf(":") > -1) {
			String[] roleVals = StringUtil.checkVal(req.getParameter("roleId")).split(":");
			userRole.setRoleId(roleVals[0]);
			userRole.setRoleLevel(Convert.formatInteger(roleVals[1]));	
			if (userRole.getRoleLevel() == ROLE_LEVEL_AUDITOR) {
				// if Auditor, set customer association
				userRole.setAttrib1Txt(req.getParameter("customerId"));
				// ...and for backwards compatibility
				userRole.addAttribute("customerId", userRole.getAttrib1Txt());
			}
		} else {
			userRole.setRoleId(roleVal);
		}
		userRole.setStatusId(Convert.formatInteger(req.getParameter("statusId")));
		return userRole;
	}
	
}
