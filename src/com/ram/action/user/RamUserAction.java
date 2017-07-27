package com.ram.action.user;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// RAMDataFeed libs
import com.ram.datafeed.data.RAMUserVO;
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
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
	public void retrieve(ActionRequest req) throws ActionException {
		// if this is an 'add user' operation, simply return.
		if (StringUtil.checkVal(req.getParameter("addUser")).length() > 0) return;
		
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		if (req.hasParameter("edit")) {
			retrieveUser(req, schema, role);
		} else if (req.hasParameter("amid")){
			retrieveList(req, role, schema, role.getRoleLevel() == 100);
		}

	}
	
	/**
	 * Returns the specific data for a user
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	public void retrieveUser(ActionRequest req, String schema, SBUserRole role) 
	throws ActionException {
		// Make sure the user has permission to edit the user information
		boolean siteAdmin = role.getRoleLevel() == 100;
		boolean adminFlag = Convert.formatBoolean(role.getAttribute(RAMRoleModule.ADMIN_ROLE));
		if (! siteAdmin && ! adminFlag && ! role.getProfileId().equals(req.getParameter("profileId"))) return;
		Set<Object> customers = (Set<Object>)role.getAttributes().get(CustomerVO.CustomerType.PROVIDER.toString());
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(req.getParameter("userRoleId")));
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.user_role_id, c.profile_id, a.first_nm, a.last_nm, c.email_address_txt,");
		sql.append("d.phone_number_txt, a.profile_role_id, b.role_id, b.status_id, b.site_id, a.admin_flg ");
		sql.append("from custom.ram_user_role a "); 
		sql.append("inner join profile_role b on a.profile_role_id = b.profile_role_id ");
		sql.append("inner join profile c on b.profile_id = c.profile_id ");
		sql.append("left outer join phone_number d on c.profile_id = d.profile_id and phone_type_cd = 'WORK' ");
		
		// Make sure the admin can only modify users in their prodvider locations
		if (!siteAdmin && adminFlag) {
			sql.append("inner join custom.ram_user_role_customer_xr xr on a.user_role_id = xr.user_role_id ");
			sql.append("and a.user_role_id in (select user_role_id "); 
			sql.append("from custom.ram_user_role_customer_xr  ");
			sql.append("where user_role_id = ? and customer_id in (");
			sql.append(StringUtil.getDelimitedList(customers.toArray(new String[customers.size()]), false, ",")).append(") ");
		} else {
			sql.append("where a.user_role_id = ? ");
		}
		
		log.debug(sql + "|" + params);
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> data = db.executeSelect(sql.toString(), params, new RAMUserVO());
		RAMUserVO user = (RAMUserVO) data.get(0);
		
		try {
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			user.setPhoneNumber(se.decrypt(user.getPhoneNumber()));
			user.setEmailAddress(se.decrypt(user.getEmailAddress()));
		} catch(Exception e) {
			log.error("Unable to decrypt data", e);
		}
		
		this.putModuleData(user);
	}
	
	/**
	 * Retrieves the list of users that may be managed in the tool
	 * @param req
	 * @param role
	 * @param schema
	 * @param admin
	 * @throws ActionException
	 */
	public void retrieveList(ActionRequest req, SBUserRole role, String schema, boolean siteAdmin) {
		// Build the SQL
		List<Object> params = new ArrayList<>();
		List<Object> cParams = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(512);
		StringBuilder cSql = new StringBuilder(512);
		cSql.append("select count(*) as key ");
		buildSelect(sql);

		// build the meat of the query
		buildJoins(cSql, schema);
		buildJoins(sql, schema);
		
		// Build the filters
		buildFilter(sql, siteAdmin, req, role, params);
		buildFilter(cSql, siteAdmin, req, role, cParams);
		
		// Add the order and offsets
		buildOrder(sql, req, params);
		
		// Get the list of items
		log.debug(sql + "|" + params);
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> data = dbp.executeSelect(sql.toString(), params, new RAMUserVO());
		
		// Get the counts
		List<Object> count = dbp.executeSelect(cSql.toString(), cParams, new GenericVO());
		
		// Add the data
		putModuleData(data, Convert.formatInteger(((GenericVO)count.get(0)).getKey()+""), false, null);
	}
	
	/**
	 * Builds the ordering of the display
	 * @param sql
	 * @param req
	 * @param params
	 */
	public void buildOrder(StringBuilder sql, ActionRequest req,List<Object> params) {
		String sort = StringUtil.checkVal(req.getParameter("sort"), "last_nm asc ");
		if (req.hasParameter("sort")) {
			if("lastName".equalsIgnoreCase(sort)) sort = "last_nm ";
			if("roleName".equalsIgnoreCase(sort)) sort = "role_nm ";
			if("adminFlag".equalsIgnoreCase(sort)) sort = "admin_flg ";
			
			sort += StringUtil.checkVal(req.getParameter("order"), "asc");
			sort += ", last_nm ";
		}
		
		sql.append("order by ").append(sort).append(" limit ? offset ? ");
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
	}
	
	/**
	 * Builds the where clause
	 * @param sql
	 * @param siteAdmin
	 * @param req
	 * @param role
	 */
	@SuppressWarnings("unchecked")
	public void buildFilter(StringBuilder sql, boolean siteAdmin, ActionRequest req, SBUserRole role, List<Object> params) {
		if (! siteAdmin) {
			List<Object> customers = (List<Object>)role.getAttributes().get(CustomerVO.CustomerType.PROVIDER.toString());
			sql.append("and a.user_role_id in (select user_role_id ");
			sql.append("from custom.ram_user_role_customer_xr where customer_id in (");
			sql.append(StringUtil.getDelimitedList(customers.toArray(new String[customers.size()]), false, ",")).append(") ");
		}
		
		// Add a filter for the search params
		String search = req.getParameter("search");
		if (! StringUtil.isEmpty(search)) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ?) ");
			params.add("%" + search.toLowerCase() + "%");
			params.add("%" + search.toLowerCase() + "%");
		}
	}
	
	/**
	 * Build the joins portion of the query
	 * @param sql
	 */
	public void buildJoins(StringBuilder sql, String schema) {
		sql.append("from ").append(schema).append("ram_user_role a  ");
		sql.append("inner join profile_role pr on a.profile_role_id = pr.profile_role_id ");
		sql.append("inner join role r on pr.role_id = r.role_id and pr.role_id not in ('0', '10') ");
		sql.append("left outer join ( ");
		sql.append("select user_role_id, count(*) as customer_count ");
		sql.append("from ").append(schema).append("ram_user_role_customer_xr rcx ");
		sql.append("inner join ").append(schema).append("ram_customer rc ");
		sql.append("on rcx.customer_id = rc.customer_id and rc.customer_type_id = 'PROVIDER' ");
		sql.append("group by user_role_id ");
		sql.append(") as cc on a.user_role_id = cc.user_role_id ");
		sql.append("where 1=1 ");
	}
	
	/**
	 * Builds the select portion of the query
	 * @param sql
	 */
	public void buildSelect(StringBuilder sql) {
		sql.append("select a.user_role_id, first_nm, last_nm, role_nm, cast(coalesce(cc.customer_count,0) as integer) as providers, ");
		sql.append("status_id, a.profile_role_id, profile_id, admin_flg, pr.profile_id ");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
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
	private void manageAssociatedHospitals(ActionRequest req,
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
	private void manageProfile(ActionRequest req, UserDataVO user, boolean isProfileInsert, Object msg) {
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
	private void manageRole(ActionRequest req, SBUserRole userRole, boolean isProfileInsert, Object msg) {
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
	private void manageAuthentication(ActionRequest req, SiteVO site, UserDataVO user, SBUserRole userRole) 
			throws ApplicationException {
		log.debug("managing authentication...");
		String loginClass = site.getLoginModule();
		Map<String, Object> lm = new HashMap<>();
		lm.put(Constants.ENCRYPT_KEY, (String) getAttribute(Constants.ENCRYPT_KEY));
		lm.put(Constants.CFG_PASSWORD_SALT, (String) getAttribute(Constants.CFG_PASSWORD_SALT));
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
			authId = loginModule.saveAuthRecord(authId, user.getEmailAddress(), authPwd, 0);
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
	private void manageAuditor(ActionRequest req, SiteVO site, UserDataVO user, SBUserRole userRole, int origRoleLevel) {
		log.debug("Managing auditor...");
		String auditorId = checkAuditor(user.getProfileId());
		if (origRoleLevel != -1 && origRoleLevel != userRole.getRoleLevel() && origRoleLevel == ROLE_LEVEL_AUDITOR) {
				// changed FROM auditor so disable RAM_AUDITOR record
				//updateAuditor(user, auditorId, RamUserFacadeAction.PROFILE_STATUS_DISABLED);
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
		//if (newStatusId == RamUserFacadeAction.PROFILE_STATUS_ACTIVE) activeFlg = 1;

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
	protected void formatUserData(List<RAMUserVO> users) {
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
	private String retrieveNonAdminProfileId(ActionRequest req) {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		return user.getProfileId(); 
	}

	/**
	 * Formats an SBUserRole with role data from the request.
	 * @param req
	 * @param user
	 * @return
	 */
	private SBUserRole preformatUserRoleData(ActionRequest req, SiteVO site, UserDataVO user) {
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
}