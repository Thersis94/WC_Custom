package com.ram.action.user;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RAMDataFeed libs
import com.ram.datafeed.data.RAMUserVO;
import com.ram.action.util.SecurityUtil;
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title: </b>RamUserAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: Manages RAM Users</b>
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
	
	// Keys for Request Variables
	public static final String OEM_KEY = "manufacturerId";
	public static final String PROVIDER_KEY = "customerIds";
	public static final String USER_ROLE_KEY = "userRoleId";
	
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
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		if (req.hasParameter("edit")) {
			if (! req.hasParameter("addUser")) putModuleData(retrieveUser(req, schema));
			req.setAttribute("RAM_USER_OEM", this.getOemList(req, schema));
			req.setAttribute("RAM_USER_PROV", this.getProviderList(req, schema, req.getParameter(USER_ROLE_KEY)));
			
		} else if (req.hasParameter("amid")){
			GenericVO data = retrieveList(req, schema, role.getRoleLevel() == 100);
			this.putModuleData(data.getKey(), (Integer)data.getValue(), false);
		}

	}
	
	/**
	 * Returns the specific data for a user
	 * @param req
	 * @throws ActionException
	 */
	public RAMUserVO retrieveUser(ActionRequest req, String schema) {
		// Make sure the user has permission to edit the user information
		int oemId = Convert.formatInteger(req.getParameter(OEM_KEY));
		if (! SecurityUtil.isAuthorized(req, oemId, req.getParameterValues(PROVIDER_KEY))) return null;
		
		// Build the SQL and params list
		StringBuilder sql = new StringBuilder(256);
		this.retrieveUserSQL(req, sql);
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(req.getParameter(USER_ROLE_KEY)));
		log.debug(sql + "|" + params);

		// Execute the lookup
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> data = db.executeSelect(sql.toString(), params, new RAMUserVO());
		RAMUserVO user = (RAMUserVO) data.get(0);
		
		try {
			// Decrypt the values from the profile record
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			user.setPhoneNumber(se.decrypt(user.getPhoneNumber()));
			user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			
		} catch(Exception e) { log.error("Unable to decrypt data", e); }
		
		// Get the user role attributes
		assignUserAttributes(user, schema);
		
		// add the user to the req
		return user;
	}
	
	/**
	 * Gets the user role customer attributes
	 * @param user
	 * @param schema
	 */
	public void assignUserAttributes(RAMUserVO user, String schema) {
		StringBuilder sql = new StringBuilder();
		sql.append("select a.customer_id, customer_nm, customer_type_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role_customer_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ram_customer b on a.customer_id = b.customer_id ");
		sql.append("where user_role_id = ? order by customer_nm");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, user.getUserRoleId());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				GenericVO g = new GenericVO(rs.getInt(1), rs.getString(2));
				
				if ("OEM".equalsIgnoreCase(rs.getString(3))) user.setOem(g);
				else user.addHospital(g);
			}
		} catch(Exception e) {
			log.error("Unable to retireve ser permissions", e);
		}
	}
	
	/**
	 * Retrieves the core user info
	 * @param sql
	 * @param siteAdmin
	 * @param adminFlag
	 * @param role
	 */
	public void retrieveUserSQL(ActionRequest req, StringBuilder sql) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select a.user_role_id, c.profile_id, a.first_nm, a.last_nm, c.email_address_txt,");
		sql.append("d.phone_number_txt, a.profile_role_id, b.role_id, b.status_id, b.site_id, a.admin_flg ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role a "); 
		sql.append("inner join profile_role b on a.profile_role_id = b.profile_role_id ");
		sql.append("inner join profile c on b.profile_id = c.profile_id ");
		sql.append("left outer join phone_number d on c.profile_id = d.profile_id and phone_type_cd = 'WORK' ");
		
		// Make sure the admin can only modify users in their provider locations
		if (SecurityUtil.hasAdminFlag(req)) {
			sql.append("inner join ").append(schema).append("ram_user_role_customer_xr xr on a.user_role_id = xr.user_role_id ");
			sql.append("and a.user_role_id in (select user_role_id "); 
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role_customer_xr  ");
			sql.append("where user_role_id = ? ");
			sql.append(SecurityUtil.addCustomerFilter(req, ""));
		} else {
			sql.append("where a.user_role_id = ? ");
		}
	}
	
	/**
	 * Retrieves the list of users that may be managed in the tool
	 * @param req
	 * @param role
	 * @param schema
	 * @param admin
	 * @throws ActionException
	 */
	public GenericVO retrieveList(ActionRequest req, String schema, boolean siteAdmin) {
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
		buildFilter(sql, siteAdmin, req, params);
		buildFilter(cSql, siteAdmin, req, cParams);
		
		// Add the order and offsets
		buildOrder(sql, req, params);
		
		// Get the list of items
		log.debug(sql + "|" + params);
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> data = dbp.executeSelect(sql.toString(), params, new RAMUserVO());
		
		// Get the counts
		List<Object> count = dbp.executeSelect(cSql.toString(), cParams, new GenericVO());
		
		// return the data
		return new GenericVO(data, Convert.formatInteger(((GenericVO)count.get(0)).getKey() + ""));
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
	public void buildFilter(StringBuilder sql, boolean isAdmin, ActionRequest req, List<Object> params) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		if(! isAdmin) {
			sql.append("and a.user_role_id in (select user_role_id ");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role_customer_xr where 1=1 ");
			sql.append(SecurityUtil.addCustomerFilter(req, "a"));
			sql.append(") ");
		}
		
		// Add a filter for the search params
		String search = req.getParameter("search");
		if (! StringUtil.isEmpty(search)) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ?) ");
			params.add("%" + search.toLowerCase() + "%");
			params.add("%" + search.toLowerCase() + "%");
		} 
		
		// Add the active filter
		if (! StringUtil.isEmpty(req.getParameter("activeFilter"))) {
			sql.append("and status_id = ? ");
			params.add(Convert.formatInteger(req.getParameter("activeFilter")));
		}
		
		// Add the role filter
		if (! StringUtil.isEmpty(req.getParameter("roleFilter"))) {
			sql.append("and r.role_id = ? ");
			params.add(req.getParameter("roleFilter"));
		}
	}
	
	/**
	 * Build the joins portion of the query
	 * @param sql
	 */
	public void buildJoins(StringBuilder sql, String schema) {
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role a  ");
		sql.append("inner join profile_role pr on a.profile_role_id = pr.profile_role_id ");
		sql.append("inner join role r on pr.role_id = r.role_id and pr.role_id not in ('0', '10') ");
		sql.append("left outer join ( ");
		sql.append("select user_role_id, count(*) as customer_count ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_user_role_customer_xr rcx ");
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
		// Save the user data
		if ("update".equalsIgnoreCase(req.getParameter("transType"))) {
			try {
				this.putModuleData(saveUserData(req));
			} catch(Exception e) {
				log.error("Unable to save user", e);
				this.putModuleData(null, 0, false, "NOT_AUTHORIZED", true);
			}
		}
	}
	
	/**
	 * Saves the data for the user
	 * @param req
	 * @param role
	 * @param isAdmin
	 * @throws ApplicationException 
	 */
	public RAMUserVO saveUserData(ActionRequest req) throws NotAuthorizedException, ApplicationException {
		Integer oem = Convert.formatInteger(req.getParameter(OEM_KEY));
		
		// Check security
		if (! SecurityUtil.isAuthorized(req, oem, req.getParameterValues(PROVIDER_KEY)))
			throw new NotAuthorizedException("Permission Check Failed");
		
		// Create/update Profile and authentication
		UserDataVO usr = manageProfile(req);
		
		// Manage the auth info
		if (! StringUtil.isEmpty(req.getParameter("password")))
			manageAuthentication(req, (SiteVO)req.getAttribute(Constants.SITE_DATA), usr);

		// Manage WC role
		SBUserRole role = manageRole(req, usr.getProfileId());
		
		// Update/Add RAM User Role
		RAMUserVO rUser = saveUserRole(req, role);
		
		// Delete Role Attributes
		this.deleteCurrentCustomers(rUser.getUserRoleId());
		
		// Insert new Attributes
		try {
			addUserCustomers(req, rUser.getUserRoleId());
		} catch(Exception e) {
			log.error("Unable to add customers", e);
			throw new ApplicationException(e);
		}

		return rUser;
	}
	
	/**
	 * Adds the customers (Provider and OEM) the the user role
	 * 
	 * @param req
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * @throws InvalidDataException 
	 * @throws Exception
	 */
	protected void addUserCustomers(ActionRequest req, int id) throws InvalidDataException, com.siliconmtn.db.util.DatabaseException {
		Integer userRoleId = Convert.formatInteger(req.getParameter(USER_ROLE_KEY), id);
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		UserRoleCustomerVO vo = new UserRoleCustomerVO();
		String manufacturerId = req.getParameter(OEM_KEY);
		String roleId = req.getParameter("roleId");
		
		// Add the OEM if assigned
		if (! StringUtil.isEmpty(manufacturerId) && SecurityUtil.isOEMGroup(roleId)) {
			vo.setUserRoleId(userRoleId);
			vo.setCustomerId(Convert.formatInteger(manufacturerId));
			vo.setCreateDate(new Date());
			dbp.insert(vo);
		}

		// Loop the providers and add tot he db if they can be mapped
		if (SecurityUtil.hasProviders(roleId) && req.getParameterValues(PROVIDER_KEY) != null) {
			for(String customerId : req.getParameterValues(PROVIDER_KEY)) {
				vo = new UserRoleCustomerVO();
				vo.setUserRoleId(userRoleId);
				vo.setCustomerId(Convert.formatInteger(customerId));
				vo.setCreateDate(new Date());
				dbp.insert(vo);
			}
		}
	}
	
	/**
	 * 
	 * @param req
	 * @throws ApplicationException
	 */
	public RAMUserVO saveUserRole(ActionRequest req, SBUserRole role) throws ApplicationException {
		RAMUserVO user = new RAMUserVO(req);
		if (StringUtil.isEmpty(user.getRoleId())) user.setRoleId(role.getRoleId());
		if (StringUtil.isEmpty(user.getProfileRoleId())) user.setProfileRoleId(role.getProfileRoleId());
		if (StringUtil.isEmpty(user.getProfileId())) user.setProfileId(role.getProfileId());
		user.setCreateDate(new Date());
		
		DBProcessor db = new DBProcessor(getDBConnection(), (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			// Make sure the user role is null for an unsert
			if (user.getUserRoleId() == 0) user.setUserRoleId(null);
			
			// Save the user role
			db.save(user);
			
			// Update the role with the generated id
			user.setUserRoleId(Convert.formatInteger(db.getGeneratedPKId()));
		} catch (Exception e) {
			log.error("Unable to add user role", e);
			throw new ApplicationException("Unable to add user role", e);
		}
		
		return user;
	}
		
	/**
	 * Delete any hospital associations this user has at the moment in order
	 * to make way for the new list of 
	 * @param profileId
	 * @throws SQLException
	 */
	private void deleteCurrentCustomers(int userRoleId) throws ApplicationException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(getAttribute("customDbSchema")).append("ram_user_role_customer_xr ");
		sql.append("where user_role_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, userRoleId);
			ps.executeUpdate();
		} catch(Exception e) {
			log.error("Unable to delete user customers", e);
			throw new ApplicationException("Unable to delete user customers", e);
		}
		
	}

	/**
	 * Inserts or updates a user profile
	 * @param req
	 * @param user
	 * @param isProfileInsert
	 * @param msg
	 */
	private UserDataVO manageProfile(ActionRequest req) {
		UserDataVO user = new UserDataVO(req);
		PhoneVO phone = new PhoneVO(PhoneVO.WORK_PHONE, StringUtil.removeNonNumeric(req.getParameter("phoneNumber")), "US");
		user.addPhone(phone);
		
		log.debug("User: " + user);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			pm.updateProfile(user, dbConn);
		} catch (DatabaseException de) {
			log.error("unable to update profile", de);
		}
		
		return user;
	}

	/**
	 * Inserts or updates a user's role
	 * @param req
	 * @param userRole
	 */
	private SBUserRole manageRole(ActionRequest req, String profileId) {
		String id = StringUtil.checkVal(req.getParameter("profileId"), profileId);
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole role = new SBUserRole();
		role.setRoleName("");
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(site.getSiteId());
		role.setRoleId(req.getParameter("roleId"));
		role.setProfileId(id);
		role.setProfileRoleId(req.getParameter("profileRoleId"));
		role.setStatusId(Convert.formatInteger(req.getParameter("statusId")));
		
		log.debug("managing user role..." + role);
		try {
			ProfileRoleManager prm = new ProfileRoleManager();
			
			// check to see if this user already has a role
			String profileRoleId = prm.checkRole(role.getProfileId(), role.getSiteId(), dbConn);
			if (StringUtil.checkVal(profileRoleId).length() > 0) role.setProfileRoleId(profileRoleId);
			
			// insert or update role data
			prm.addRole(role, dbConn);
		} catch (DatabaseException de) {
			log.error("Unable to update role", de);
		}
		
		return role;
	}

	/**
	 * Inserts authentication record for a user if that user does not already have one.
	 * @param req
	 * @param user
	 * @param userRole
	 * @param msg
	 * @throws ApplicationException 
	 */
	private void manageAuthentication(ActionRequest req, SiteVO site, UserDataVO user) 
	throws ApplicationException {
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
			if (oldEmail.length() > 0 && ! oldEmail.equalsIgnoreCase(user.getEmailAddress())) {
				// email address changed, use old email address for auth lookup
				authEmail = oldEmail;
			}

			// retrieve authID for this user if it exists.
			authId = loginModule.retrieveAuthenticationId(authEmail);
			boolean updateProfileAuthId = false;
			String authPwd = StringUtil.checkVal(req.getParameter("password"));
			if (StringUtil.checkVal(authId).length() == 0) {
				// no existing auth record, so update profile with auth ID after auth
				// record is created.
				updateProfileAuthId = true;
			}

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
			log.error("Unable to update profile auth", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
	}
	
	/**
	 * Gets a selection list of providers for the view
	 * @param role USer role
	 * @param schema db schema
	 * @return
	 */
	public List<Object> getOemList(ActionRequest req, String schema) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_id, customer_nm ");
		sql.append("from ").append(schema).append("ram_customer ");
		sql.append("where customer_type_id = 'OEM' ");
		sql.append(SecurityUtil.addOEMFilter(req, ""));
		sql.append("order by customer_nm ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), null, new CustomerVO());
	}
	
	/**
	 * Gets a selection list of providers for the view
	 * @param role USer role
	 * @param schema db schema
	 * @return
	 */
	public List<Object> getProviderList(ActionRequest req, String schema, String userRoleId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_id, customer_nm from ").append("ram_customer "); 
		sql.append("where customer_type_id = 'PROVIDER' and active_flg = 1 and customer_id not in ( ");
		sql.append("select customer_id from ").append("ram_user_role_customer_xr ");
		sql.append("where user_role_id = ?) ");
		sql.append(SecurityUtil.addCustomerFilter(req, ""));
		sql.append("order by customer_nm; ");
		
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(userRoleId));
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new CustomerVO());
	}
}