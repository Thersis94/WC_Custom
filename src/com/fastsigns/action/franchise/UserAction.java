package com.fastsigns.action.franchise;

// JDK 1.6
import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// FTS_Custom
import com.fastsigns.security.FsFranchiseRoleAction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB II
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
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
	//private final String PAR_SITE_ID = "FTS_1";
	
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
		Object msg = "msg.updateSuccess";
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		ProfileRoleManager prm = new ProfileRoleManager();

		final String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = "delete from " + custom_db + "FTS_FRANCHISE_ROLE_XR where PROFILE_ID=? and FRANCHISE_ID=?";
		String profileId = req.getParameter("del");
		String roleId = req.getParameter("roleId");
		String siteId = site.getAliasPathParentId();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, req.getParameter("del"));
			ps.setString(2, CenterPageAction.getFranchiseId(req));
			int x = ps.executeUpdate();
			if (x < 1)
				msg = "msg.noRcrdDel";
			/*
			 * If they are no longer tied to a Franchise, we downgrade their
			 * role to a Standard Registered User
			 */
			if(!hasFranchiseTies(profileId)){
				prm.removeRole(profileId, siteId ,roleId , dbConn);
				prm.addRole(profileId, siteId, SecurityController.PUBLIC_REGISTERED_LEVEL + "", SecurityController.STATUS_ACTIVE, dbConn);
			}


		} catch (SQLException sqle) {
			log.error(sqle);
			msg = "msg.cannotUpdate";
		} catch (DatabaseException e) {
			log.debug("Error removing Profile Role.", e);
			msg = "msg.cannotUpdate";
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?msg=").append(msg.toString());
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
		
	}
	
	/**
	 * This method checks to see if this user is tied to any additional franchises.
	 * If they are returns true, otherwise false.
	 * @param profileId
	 * @return
	 */
	private boolean hasFranchiseTies(String profileId){
		PreparedStatement ps = null;
		final String custom_db = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(custom_db).append("FTS_FRANCHISE_ROLE_XR where PROFILE_ID=?");
		boolean hasTies = false;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				hasTies = true;
		} catch(Exception e){
			
		}finally{try { ps.close(); } catch (Exception e) {}}
		return hasTies;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Starting UserAction build...");
		UserDataVO vo = new UserDataVO(req);
       	String franchiseId = CenterPageAction.getFranchiseId(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		// create or update user profile
		this.saveCoreData(req, pm, site, vo);
		
		// save custom _XR record for FranchiseId
		Boolean isUpdate = (StringUtil.checkVal(req.getParameter("profileRoleId")).length() > 0);
       	FsFranchiseRoleAction ai = new FsFranchiseRoleAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.updateFranchiseXR(vo.getProfileId(), franchiseId, isUpdate, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA), site.getCountryCode());
		ai = null;
		
		//if everything succeeded, set a success message for the user
		if (msg == null) msg = "msg.updateSuccess";
		
		// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?msg=").append(msg.toString());
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
		if (req.getParameter("del") != null)
	        delete(req);
		
		List<UserDataVO> results = new ArrayList<UserDataVO>();
		try {
			results = this.retrieveUsers(req);
		} catch (Exception de) {
			//de.printStackTrace();
			log.error(de);
		}
		
		//sort the list Alpha by last name
		Collections.sort(results, new UserDataComparator());
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionData(results);
		mod.setDataSize(results.size());
		setAttribute(Constants.MODULE_DATA, mod);
		
	}
	
	/**
	 * Retrieve user data 
	 * @param req
	 * @return
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	public List<UserDataVO> retrieveUsers(SMTServletRequest req) 
	throws SQLException, DatabaseException {
		
		Map<String, UserDataVO> data = new HashMap<String, UserDataVO>();
		List<UserDataVO> results = new ArrayList<UserDataVO>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String siteId = site.getOrganizationId() + "_7";
		String profileId = StringUtil.checkVal(req.getParameter("profileId"));
    	UserRoleVO r = (UserRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
    	
		StringBuffer sql = new StringBuffer();
		sql.append("select b.profile_id, a.franchise_id, b.authentication_id, e.role_id, ");
		sql.append("f.role_order_no, f.role_nm, e.status_id, ");
		sql.append("e.profile_role_id, c.password_txt, max(d.login_dt) as 'login_dt' from ");
		sql.append("profile b ");
		sql.append("inner join authentication c on b.authentication_id=c.authentication_id ");
		sql.append("left outer join authentication_log d on c.authentication_id=d.authentication_id and d.site_id=? ");
		sql.append("inner join profile_role e on b.profile_id=e.profile_id and e.site_id=? ");
		sql.append("inner join role f on e.role_id=f.role_id and f.ROLE_ORDER_NO > 10 " );
		if(r.getRoleLevel() != 100)
			sql.append(" and f.ROLE_ORDER_NO < 100 ");
		sql.append(" inner join ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("fts_franchise_role_xr a ");
		sql.append("on b.profile_id = a.profile_id ");
		sql.append("where 1=1 and a.franchise_id=? ");
		if (profileId.length() > 0) sql.append("and b.profile_id=? ");
		sql.append("group by b.profile_id, a.franchise_id, b.authentication_id, c.password_txt, ");
		sql.append("e.role_id, f.role_order_no, f.role_nm, e.status_id, e.profile_role_id ");
		log.info(sql + "|" + siteId + "|" + CenterPageAction.getFranchiseId(req));
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, siteId);
			ps.setString(2, siteId);
			ps.setString(3, CenterPageAction.getFranchiseId(req));
			if (profileId.length() > 0) ps.setString(4, profileId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				UserDataVO vo = new UserDataVO(rs);
				SBUserRole role = new SBUserRole(siteId, rs.getString("role_nm"));
				role.setRoleId(rs.getString("role_id"));
				role.setRoleLevel(rs.getInt("role_order_no"));
				role.setStatusId(rs.getInt("status_id"));
				if (rs.getDate("login_dt") != null) role.setUpdateDate(rs.getDate("login_dt"));
				vo.setPassword(pm.getStringValue("FIRST_NM", rs.getString("password_txt")));
				vo.addAttribute("SB_ROLE", role);
				data.put(rs.getString("profile_id"), vo);
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
			for (String p : profiles.keySet()) {
				UserDataVO vo = data.get(p);
				vo.setData(profiles.get(p).getDataMap()); //merge the two VO's
				results.add(vo);
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
	public void saveCoreData(SMTServletRequest req, ProfileManager pm, SiteVO site, UserDataVO vo) {
		log.debug("creating/updating user's profile...");
		
		//save core LOGIN info
		this.saveAuthentication(req, vo, site);
		
		//save core PROFILE
		this.saveProfile(req, pm, vo, site);
		String parSiteId= req.getParameter("parSiteId");
		log.debug("Parent Site Id = " + parSiteId);
		//save core PROFILE_ROLE
		// this.saveProfileRole(req, site.getSiteId(), Convert.formatInteger(req.getParameter("roleLevelNo")), vo);
		//give them Franchise access to the main website too.
		this.saveProfileRole(req, parSiteId, Convert.formatInteger(req.getParameter("roleLevelNo")), vo);
	}
	
	/**
	 * 
	 * @param req
	 * @param pm
	 * @param vo
	 */
	private void saveProfile(SMTServletRequest req, ProfileManager pm, UserDataVO vo, SiteVO site) {
		//save core PROFILE
		try {
			if (vo.getProfileId() == null || vo.getProfileId().length() == 0) {
				vo.setProfileId(pm.checkProfile(vo, dbConn));
			}
			
			if (vo.getProfileId() == null) {
				pm.updateProfile(vo, dbConn);
				pm.assignCommunicationFlg(site.getOrganizationId(), vo.getProfileId(), Integer.valueOf(1), dbConn);
			} else {
				pm.updateProfilePartially(vo.getDataMap(), vo.getProfileId(), dbConn);
			}
			log.debug("profile saved");
		} catch (DatabaseException de) {
			log.error(de);
            msg = "msg.cannotUpdate";
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param siteId
	 * @param roleLvlNo
	 * @param vo
	 */
	private void saveProfileRole(SMTServletRequest req, String siteId, int roleLvlNo, UserDataVO vo) {
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole(siteId);
		role.setProfileId(vo.getProfileId());
		role.setProfileRoleId(req.getParameter("profileRoleId"));
		role.setRoleId(req.getParameter("roleId"));
		role.setRoleLevel(roleLvlNo);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		try {
			/*
			 * If a user is not already at Franchise/Admin role, promote them
			 * up.
			 */
			if (!prm.roleExists(vo.getProfileId(), siteId, role.getRoleId(), dbConn) &&
				!prm.roleExists(vo.getProfileId(), siteId, SecurityController.ADMIN_ROLE_LEVEL + "", dbConn)) {
				//Ensure we are not adding additional Roles.
				prm.removeRole(vo.getProfileId(), siteId, 10 + "", dbConn);
				//Add new Role
				prm.addRole(role, dbConn);
			}
			log.debug("Role saved");
		} catch (DatabaseException de) {
			log.error(de);
            msg = "msg.cannotUpdate";
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param vo
	 * @param site
	 */
	private void saveAuthentication(SMTServletRequest req, UserDataVO vo, SiteVO site) {
		//force new users to change their password
		int resetFlg = (StringUtil.checkVal(req.getParameter("profileRoleId")).length() == 0) ? 1 : 0;
		
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
            msg = "msg.cannotUpdate";
		}
	}
	
}