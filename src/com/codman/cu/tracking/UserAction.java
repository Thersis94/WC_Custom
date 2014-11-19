package com.codman.cu.tracking;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import java.util.Set;

// DePuy SB
import com.codman.cu.tracking.vo.PersonVO;
import com.codman.cu.tracking.vo.UserSearchVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB II
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: UserAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 09, 2010
 ****************************************************************************/
public class UserAction extends SimpleActionAdapter {
	
	private Object msg = null;
	
	/**
	 * 
	 */
	public UserAction() {
		super();
	}
	
	/**
	 * @param arg0
	 */
	public UserAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		SBUserRole roleVo = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		final String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//ensure only admins can perform deletes
		if (roleVo.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL) return;
		
		String sql = "delete from profile_role where profile_id=? and site_id=?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, req.getParameter("del"));
			ps.setString(2, site.getSiteId());
			int x = ps.executeUpdate();
			if (x < 1)
				msg = "No user records were deleted";
		} catch (SQLException sqle) {
			log.error(sqle);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		sql = "delete from " + custom_db + "codman_cu_person where person_id=?";
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, req.getParameter("del"));
			int x = ps.executeUpdate();
			if (x < 1)
				msg = "No user records were deleted";
		} catch (SQLException sqle) {
			log.error(sqle);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		// Setup the redirect.
    	StringBuilder url = new StringBuilder();
    	url.append(req.getRequestURI()).append("?msg=").append(msg.toString());
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Starting UserAction build...");

		PersonVO vo = new PersonVO(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		// create or update user profile
       	this.createUserProfile(req, pm, site, vo);
		
		// save custom CODMAN_CU_PERSON
		String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();

		//verify they're not already in the system
		if (vo.getPersonId() == null || vo.getPersonId().length() == 0)
			vo.setPersonId(this.lookupPersonId(vo.getProfileId(), site.getOrganizationId()));
		
		//build the sql
		if (vo.getPersonId() == null || vo.getPersonId().length() == 0) {
			vo.setPersonId(vo.getProfileId());
			sql.append("insert into ").append(custom_db).append("codman_cu_person ");
			sql.append("(territory_id, profile_id, organization_id, sample_acct_no, create_dt, person_id) ");
			sql.append("values (?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(custom_db).append("codman_cu_person ");
			sql.append("set territory_id=?, profile_id=?, organization_id=?, sample_acct_no=?, update_dt=? ");
			sql.append("where person_id=?");
		}
		log.debug("UserAction build SQL: " + sql.toString() + "|" + vo.getPersonId());
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getTerritoryId());
			ps.setString(2, vo.getProfileId());
			ps.setString(3, vo.getOrganizationId());
			ps.setString(4, vo.getSampleAccountNo());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, vo.getPersonId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		this.sendEmail(req, site, vo);
		
		//if everything succeeded, set a success message for the user
		if (msg == null) msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		// Setup the redirect.
    	StringBuilder url = new StringBuilder();
    	url.append(req.getRequestURI()).append("?msg=").append(msg.toString());
    	if (req.getParameter("my") != null) url.append("&my=1");
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
    	
	}
	

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		//handle deletions gracefully
		if (req.hasParameter("del"))
	        delete(req);
		
		List<PersonVO> results = new ArrayList<PersonVO>();
		
		try {
			results = this.retrieveUsers(req);
		} catch (Exception de) {
			log.error(de);
		}
		
		//sort the list Alpha by last name
		Collections.sort(results, new UserDataComparator());
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionData(results);
		mod.setDataSize(results.size());
		setAttribute(Constants.MODULE_DATA, mod);
		
	}
	
	public String lookupPersonId(String profileId, String orgId) {
		String personId = null;
		StringBuilder sql = new StringBuilder();
		sql.append("select person_id from ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("CODMAN_CU_PERSON where profile_id=? and organization_id=?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, profileId);
			ps.setString(2, orgId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				personId = rs.getString(1);
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		return personId;
	}
	
	/**
	 * Retrieve user data 
	 * @param req
	 * @return
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	public List<PersonVO> retrieveUsers(SMTServletRequest req) 
	throws SQLException, DatabaseException {
		
		Map<String, PersonVO> data = new HashMap<String, PersonVO>();
		List<PersonVO> results = new ArrayList<PersonVO>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		String personId = StringUtil.checkVal(req.getParameter("personId"));
		UserSearchVO search = new UserSearchVO(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);	
		
		StringBuilder sql = new StringBuilder();
		sql.append("select a.territory_id, a.person_id, b.profile_id, a.sample_acct_no, ");
		sql.append("b.authentication_id, e.role_id, e.status_id, e.profile_role_id, ");
		sql.append("c.password_txt, max(d.login_dt) as 'login_dt' ");
		sql.append("from profile b ");
		if (role.getRoleLevel() == SecurityController.ADMIN_ROLE_LEVEL && req.hasParameter("my")) {
			//this prevents SMT site admins (not actual CU admins) from appearing in "Manage Users" on the site
			sql.append("left outer join ");
		} else { 
			sql.append("inner join ");
		}
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("codman_cu_person a ");
		sql.append("on a.profile_id=b.profile_id ");
		sql.append("inner join authentication c on b.authentication_id=c.authentication_id ");
		sql.append("left outer join authentication_log d on c.authentication_id=d.authentication_id and d.site_id=? ");
		sql.append("inner join profile_role e on b.profile_id=e.profile_id and e.site_id=? ");
		sql.append("where 1=1 ");
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL || req.getParameter("my") != null) {
			sql.append("and b.profile_id = ? ");
		}
		if (personId.length() > 0) sql.append("and a.person_id=? ");
		if (search.getRoleId() != null) sql.append("and e.role_id=? ");
		if (search.getLastName() != null) sql.append("and b.search_last_nm=? ");
		if (search.getEmailAddress() != null) sql.append("and b.search_email_txt=? ");
		sql.append("group by a.person_id, b.profile_id, a.territory_id, a.sample_acct_no, ");
		sql.append("b.authentication_id, c.password_txt, e.role_id, e.status_id, e.profile_role_id ");
		log.info(sql);
		
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, site.getSiteId());
			ps.setString(++i, site.getSiteId());
			if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL || req.hasParameter("my"))
				ps.setString(++i, user.getProfileId());

			if (personId.length() > 0) ps.setString(++i, personId);
			if (search.getRoleId() != null) ps.setString(++i, search.getRoleId());
			if (search.getLastName() != null) ps.setString(++i, pm.getEncValue("SEARCH_LAST_NM", search.getLastName().toUpperCase()));
			if (search.getEmailAddress() != null) ps.setString(++i, pm.getEncValue("SEARCH_EMAIL_TXT", search.getEmailAddress().toUpperCase()));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PersonVO person = new PersonVO(rs);
				person.setPassword(pm.getStringValue("FIRST_NM", rs.getString("password_txt")));
				if (rs.getDate("login_dt") != null) person.setLastLoginDate(rs.getDate("login_dt"));
				data.put(rs.getString("profile_id"), person);
			}
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new SQLException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//get the UserDataVOs for these people...
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(data.keySet()));
			log.debug("profileCnt=" + profiles.size());
			for (String p : profiles.keySet()) {
				PersonVO person = data.get(p);
				person.setData(profiles.get(p).getDataMap()); //merge the two VO's
				results.add(person);
			}
		} catch (DatabaseException de) {
			log.error(de);
			throw new DatabaseException(de);
		}
		return results;
	}
	
	/**
	 * Creates core login, profile, and profile role info for a rep.
	 * @param req
	 * @param site
	 * @param vo
	 * @param pm
	 */
	public void createUserProfile(SMTServletRequest req, ProfileManager pm, SiteVO site, PersonVO vo) {
		log.debug("creating/updating user's profile...");
		
		//force new users to change their password
		int resetFlg = (StringUtil.checkVal(vo.getProfileId()).length() == 0 
				&& vo.getRoleId().equals(String.valueOf(SecurityController.PUBLIC_REGISTERED_LEVEL))) ? 1 : 0;
		
		//save core LOGIN info
		try {
			Map<String, Object> lm = new HashMap<String, Object>();
			lm.put(Constants.ENCRYPT_KEY, (String)getAttribute(Constants.ENCRYPT_KEY));
			lm.put(GlobalConfig.KEY_DB_CONN, dbConn);
			AbstractLoginModule loginModule = SecurityModuleFactoryImpl.getLoginInstance(site.getLoginModule(), lm);
			
			//try to discover the authId via emailAddress lookup
			if (StringUtil.checkVal(vo.getAuthenticationId()).length() == 0)
				vo.setAuthenticationId(loginModule.retrieveAuthenticationId(vo.getEmailAddress()));

			//insert or update the AUTHENTICATION table, returns the pkId
			String authId = loginModule.manageUser(vo.getAuthenticationId(), 
								vo.getEmailAddress(),	vo.getPassword(), resetFlg);
			vo.setAuthenticationId(authId);
			log.debug("done saving auth info");
		} catch (Exception e) {
			log.error("profileAuthId exception", e);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		this.checkProfile(req, pm, vo);
		
		this.saveProfileRole(req, site, vo);
		
	}
	
	private void saveProfileRole(SMTServletRequest req, SiteVO site, PersonVO vo) {
		ProfileRoleManager prm = new ProfileRoleManager();
		
		//create a base ROLE VO
		SBUserRole role = new SBUserRole(site.getSiteId());
		role.setProfileId(vo.getProfileId());
		role.setRoleId(vo.getRoleId());
		role.setProfileRoleId(vo.getProfileRoleId());
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		log.debug(StringUtil.getToString(role));
		
		// retrieve any existing profileRoleId so that we can update it
		if (role.getProfileRoleId() == null || role.getProfileRoleId().length() == 0) {
			try {
				role.setProfileRoleId(prm.checkRole(role.getProfileId(), site.getSiteId(), dbConn));
			} catch (DatabaseException de) {
				log.error("profileRoleId exception, ", de);
	            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
		}
		
		log.debug("existing profileRoleId: " + role.getProfileRoleId());
		try {
			if (role.getRoleId() != null) 
				prm.addRole(role, dbConn);
			log.debug("Role saved");
		} catch (DatabaseException de) {
			log.error(de);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
	}
	
	/**
	 * 
	 * @param req
	 * @param pm
	 * @param vo
	 */
	public void checkProfile(SMTServletRequest req, ProfileManager pm, PersonVO vo) {
		//save core PROFILE, PHONE_NO, & PROFILE_ADDRESS
		try {
			if (vo.getProfileId() == null || vo.getProfileId().length() == 0) {
				vo.setProfileId(pm.checkProfile(vo, dbConn));
			}
			
			if (vo.getProfileId() == null) {
				vo.setAllowCommunication(1); //opt-in new users only
				pm.updateProfile(vo, dbConn);
			} else {
				pm.updateProfilePartially(vo.getDataMap(), vo.getProfileId(), dbConn);
			}
			log.debug("profile saved");
		} catch (DatabaseException de) {
			log.error(de);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
	}
	
	/**
	 * Sends email notification to user upon account creation.
	 * @param req
	 * @param site
	 * @param vo
	 */
	private void sendEmail(SMTServletRequest req, SiteVO site, PersonVO vo) {
				
		if (msg == null && Convert.formatBoolean(req.getParameter("email"))) {
			MedstreamEmailer emailer = null;
			if (site.getOrganizationId().equals("CODMAN_EU")) {
				emailer = new MedstreamEmailerEU(this.actionInit);
			} else {
				emailer = new MedstreamEmailer(this.actionInit);
			}
			
			emailer.setAttributes(attributes);
			emailer.setDBConnection(dbConn);
			
			try {
				emailer.sendAccountInfo(vo, site);
			} catch (MailException me) {
				log.error(me);
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			} finally {
				emailer = null;
			}
		}
	}
	
	
	public List<UserDataVO> loadUserList(Integer roleLvl, String organizationId) {
		List<UserDataVO> data = new ArrayList<UserDataVO>();
		Set<String> profileIds = new HashSet<String>();
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("select a.profile_id from ").append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("codman_cu_person a inner join profile_role b ");
		sql.append("on a.profile_id=b.profile_id ");
		sql.append("inner join role c on c.role_id = b.role_id ");
		sql.append("where c.role_order_no=? and b.status_id=? and a.organization_id=?");
		log.debug(sql + " " + roleLvl);
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, roleLvl);
			ps.setInt(2, SecurityController.STATUS_ACTIVE);
			ps.setString(3, organizationId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				profileIds.add(rs.getString(1));
		} catch (SQLException sqle) {
			log.error(sqle);
		}
		
		try {
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			data = pm.searchProfile(dbConn, new ArrayList<String>(profileIds));
			
			//re-order the data using the decrypted names
	    	Collections.sort(data, new UserDataComparator());
		} catch (DatabaseException de) {
			log.error(de);
		}
		
		log.info("finished loading " + data.size() + " users");
		return data;
		
	}

}