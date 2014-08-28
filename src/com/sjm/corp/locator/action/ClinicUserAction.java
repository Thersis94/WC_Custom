package com.sjm.corp.locator.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// J2EE 1.5
import javax.servlet.http.HttpSession;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.admin.action.data.SiteUserVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: ClinicUserAction.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 23, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicUserAction extends SBActionAdapter {
	public static final int SITE_ADMIN_USER = 100;
	public static final int COUNTRY_ADMIN_USER = 70;
	public static final int CLINIC_ADMIN_USER = 20;
	
	public String CLINIC_ROLE_ID = "c0a802376dd05e19a0a268136058b860";
	public String COUNTRY_ROLE_ID = "c0a802376dd095b0f48db75cbd689f35";
	
	/**
	 * 
	 */
	public ClinicUserAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public ClinicUserAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		String s = "delete from profile_role where profile_role_id = ? and site_id = ?";
		String msg = (String)attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, req.getParameter("profileRoleId"));
			ps.setString(2, ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getAliasPathParentId());
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Unable to remove profile_role");
			msg = (String)attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try {
				ps.close();
			} catch (Exception e){}
		}
		
		String url = ((PageVO)req.getAttribute(Constants.PAGE_DATA)).getFullPath();
		this.sendRedirect(url, msg + "&adminType=" + req.getParameter("adminType"), req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("delete"))) {
			this.delete(req);
			return;
		}
		
		log.debug("Updating user permissions");
		HttpSession ses = req.getSession();
		String msg = (String)attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String dealerLocationId = StringUtil.checkVal(ses.getAttribute("dealerLocationId"));
		SBUserRole role = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		String roleId = req.getParameter("roleId");
		String type = "";
		
		// Determine the attribute
		Boolean isAuthorized = true;
		String attr = "";
		if (CLINIC_ROLE_ID.equals(roleId)) {
			if (dealerLocationId.length() > 0)
				attr = dealerLocationId;
			else 
				attr = (String)role.getAttribute(0);
			
			type="clinic";
		} else if (COUNTRY_ROLE_ID.equals(roleId)) {
			String[] vals = req.getParameterValues("attribute");
			for (int i =0; i < vals.length; i++) {
				if (i > 0) attr += ",";
				attr += vals[i];
			}
			
			type="country";
			if (role.getRoleLevel() < COUNTRY_ADMIN_USER) isAuthorized = false;
		} else if ("100".equals(roleId)) {
			type="admin";
			if (role.getRoleLevel() < 100) isAuthorized = false;
		}
		
		// Assign the user data
		if (isAuthorized) {
			try {
				this.assignUserData(req, attr, roleId);
			} catch (Exception e) {
				log.error("Unable to assign user data", e);
				msg = (String)attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			}
		} else {
			msg = "Not authorized to perform this action";
		}
		
		// Send the redirect
		String url = ((PageVO)req.getAttribute(Constants.PAGE_DATA)).getFullPath();
		this.sendRedirect(url, msg + "&adminType=" + type, req);
	}
	
	/**
	 * 
	 * @param req
	 * @throws DatabaseException
	 */
	public void assignUserData(SMTServletRequest req, String attrib, String roleId) 
	throws DatabaseException {
		// Add user profile
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		UserDataVO user = new UserDataVO();
		user.setFirstName(req.getParameter("firstName"));
		user.setLastName(req.getParameter("lastName"));
		user.setEmailAddress(req.getParameter("emailAddress"));
		user.setPassword(req.getParameter("password"));
		user.setMainPhone(req.getParameter("phone"));
		user.setProfileId(StringUtil.checkVal(req.getParameter("profileId")));

		try {
			// Get the Auth ID
			String encKey = (String) this.getAttribute(Constants.ENCRYPT_KEY);
			log.debug("Enc Key: " + encKey);
			UserLogin ul = new UserLogin(dbConn, encKey);
			
			// Add the authentication entry
			String authId = StringUtil.checkVal(ul.checkAuth(user.getEmailAddress()));
			if (authId.length() == 0 || user.getPassword().length() > 0) {
				authId = ul.modifyUser(authId, user.getEmailAddress(), user.getPassword(), 0);
			}
			
			if (user.getPassword().length() > 0)
				user.setAuthenticationId(authId);
			
			// Add the user profile
			if (user.getProfileId().length() == 0) user.setProfileId(pm.checkProfile(user, dbConn));
			pm.updateProfile(user, dbConn);
			
		} catch (Exception e) {
			log.error("Unable to add profile for the user", e);
		}
		log.debug("***********************");
		// add the role if necessary
		String prid = StringUtil.checkVal(req.getParameter("profileRoleId"));
		addRole(req, user.getProfileId(), roleId, attrib, prid);
	}
	
	/**
	 * 
	 * @param req
	 * @throws DatabaseException
	 */
	public void addRole(SMTServletRequest req, String profileId, String roleId, String attrib, String rpid) 
	throws DatabaseException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RegisterClinicAction rca = new RegisterClinicAction();
		rca.setDBConnection(dbConn);
		if (rca.isRoleAssigned(profileId, site.getAliasPathParentId()) && rpid.length() == 0) return;
		int status = Convert.formatInteger(req.getParameter("statusId"), SecurityController.STATUS_ACTIVE);
		log.debug("************** Status: " + status + "|" + req.getParameter("statusId"));
		
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole();
		role.setProfileRoleId(rpid);
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(site.getAliasPathParentId());
		role.setRoleId(roleId);
		role.setRoleLevel(0);
		role.setStatusId(status);
		role.setProfileId(profileId);
		role.addAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1, attrib);
		prm.addRole(role, dbConn);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("add"))) return;
		
		// Get the session data
		HttpSession ses = req.getSession();
		SBUserRole userRole =  (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		int roleLevel = userRole.getRoleLevel(); 
		int role = CLINIC_ADMIN_USER;
		String adminType = StringUtil.checkVal(req.getParameter("adminType"));
		if (adminType.length() == 0) {
			switch (roleLevel) {
				case 20:
					adminType = "clinic";
					break;
				case 70:
					adminType = "country";
					break;
				case 100:
					adminType = "admin";
					break;
			}
			req.setParameter("adminType", adminType);
		}
		
		String attr = StringUtil.checkVal(ses.getAttribute("dealerLocationId"));
		if (attr.length() == 0) 
			attr = (String)userRole.getAttribute(0);
		
		String siteId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getAliasPathParentId();
		String profileId = StringUtil.checkVal(req.getParameter("profileId"));
		
		log.debug("Auth ID: " + ((UserDataVO)ses.getAttribute(Constants.USER_DATA)).getAuthenticationId());
		
		// Determine the user type to retrieve
		if ("country".equalsIgnoreCase(adminType)) {
			if (roleLevel == 100)
				attr = StringUtil.checkVal(ses.getAttribute("dealerCountry"));
			else
				attr = (String)((SBUserRole)ses.getAttribute(Constants.ROLE_DATA)).getAttribute(0);
			
			role = COUNTRY_ADMIN_USER;
		} else if ("admin".equalsIgnoreCase(adminType)) {
			attr = null;
			role = SITE_ADMIN_USER;
		}
		
		/** Make sure the role requested matched the role assigned **/
		if (role > roleLevel) role = roleLevel;
		
		// List the users or get a single record
		try {
			if (profileId.length() > 0) {
				SiteUserVO sUser = this.getUserData(profileId, siteId);
				this.putModuleData(sUser);
			} else {
				List<SiteUserVO> users = getAdminUsers(role, attr, siteId, adminType);
				this.putModuleData(users, users.size(), false);
			}
		} catch(Exception e) {
			log.error("Unable to retrieve adnin users", e);
		}
	}
	
	/**
	 * Gets a user's specific data
	 * @param profileId
	 * @param site
	 * @return
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	public SiteUserVO getUserData(String profileId, String site) 
	throws SQLException, DatabaseException {
		String s = "select * from role a inner join profile_role b ";
		s += "on a.role_id = b.role_id where site_id = ? and profile_id = ?";
		
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, site);
		ps.setString(2, profileId);
		ResultSet rs = ps.executeQuery();
		
		SiteUserVO sUser = new SiteUserVO();
		if (rs.next()) {
			sUser = new SiteUserVO(rs);
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			sUser.setUserData(pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, null));
			
			log.debug("Password: " + sUser.getPassword());
		}
		
		return sUser;
	}
	
	/**
	 * Retrieves all of the Site Administrators
	 * @return
	 */
	public List<SiteUserVO> getAdminUsers(int role, String attr, String site, String adminType) 
	throws SQLException, EncryptionException  {
		log.debug("Getting admin users");
		StringBuilder s = new StringBuilder();
		s.append("select c.email_address_txt, c.first_nm, c.last_nm, b.profile_id, ");
		s.append("b.role_id, b.status_id, status_nm, role_order_no,b.profile_role_id, ");  
		s.append("max(e.login_dt) as login_dt	from role a ");
		s.append("inner join profile_role b on a.role_id = b.role_id "); 
		s.append("inner join profile c on b.profile_id = c.profile_id ");
		s.append("inner join status d on b.status_id = d.status_id ");
		s.append("left outer join authentication_log e on c.authentication_id = e.authentication_id ");
		s.append("and e.site_id = ? ");
		s.append("where b.site_id = ? and role_order_no = ? ");
		if (! "admin".equalsIgnoreCase(adminType)) s.append("and attrib_txt_1 like ? ");
		s.append("group by c.email_address_txt, c.first_nm, c.last_nm, b.profile_id,");
		s.append(" b.role_id, b.status_id, status_nm, role_order_no, b.profile_role_id ");
		s.append("order by role_order_no desc  ");

		log.debug("Admin User SQL: " + s + "|" + role + "|" + attr + "|" + site);
		
		List<SiteUserVO> users = new ArrayList<SiteUserVO>();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, site);
		ps.setString(2, site);
		ps.setInt(3, role);
		if (! "admin".equalsIgnoreCase(adminType)) ps.setString(4, "%" + attr + "%");
		ResultSet rs = ps.executeQuery();
		
		StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		while (rs.next()) {
			SiteUserVO user = new SiteUserVO(rs);
			user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			user.setFirstName(se.decrypt(user.getFirstName()));
			user.setLastName(se.decrypt(user.getLastName()));
			users.add(user);
		}
		
		return users;
	}
	
}
