package com.orthopediatrics.action;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.exception.DatabaseException;

// SB Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.admin.action.UserAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
 <p><b>Title</b>: SalesRepAction.java</p>
 <p>Description: <b/>Manages the sales rep info</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 5, 2007
 Last Updated:
 ***************************************************************************/
public class SalesRepAction extends SBActionAdapter {
	public static final long serialVersionUID = 1l;
	public static final String TM1_FKEY_VALUE = "SALES_REP_ATM_FKEY";
	
	/**
	 * 
	 */
	public SalesRepAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public SalesRepAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		super.delete(req);
	}
	
	/**
	 * Deletes a sales rep. 
	 * @param req
	 * @throws ActionException
	 */
	public void deleteRep(ActionRequest req) throws ActionException {
		String salesRepId = req.getParameter("salesRepId");
		String message = "You have successfully deleted the sales rep";
		
		try {
			// Delete the rep info
			this.deleteRepInfo(salesRepId);
			
			// Delete the site auth info
			this.deleteSiteRole(req);
		} catch(Exception e) {
			log.error("Unable to delete op sales rep", e);
			message = "Unable to delete sales rep";
		}
		
		PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath(),message, req);
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 */
	public void deleteSiteRole(ActionRequest req) throws ActionException {
		UserAction ua = new UserAction(this.actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		
		try {
			ua.deleteInfo(req);
		} catch(Exception e) {
			log.error("Unable to delete Op Sales Rep Info", e);
		}
	}
	
	/**
	 * Deletes a record from the sales rep table
	 * @param salesRepId
	 * @throws SQLException
	 */
	public void deleteRepInfo(String salesRepId) throws SQLException {
		StringBuffer sql = new StringBuffer();

		String schema = (String)getAttribute("customDbSchema");
		sql.append("delete from ").append(schema).append("op_sales_rep ");
		sql.append("where sales_rep_id = ?");
		
		PreparedStatement ps = null;
		ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, salesRepId);
		ps.executeUpdate();
		ps.close();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(ActionRequest req) throws ActionException {
		String s = "select * from sb_action where action_id = ?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, req.getParameter("sbActionId"));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				SBModuleVO module = new SBModuleVO();
                module.setActionId(rs.getString("action_id"));
                module.setModuleTypeId(rs.getString("module_type_id"));
                module.setActionName(rs.getString("action_nm"));
                module.setActionDesc(rs.getString("action_desc"));
                module.setOrganizationId(rs.getString("organization_id"));
                module.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
                module.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
                module.setIntroText(rs.getString("intro_txt"));
                module.setActionGroupId(rs.getString("action_group_id"));
                module.setPendingSyncFlag(rs.getInt("pending_sync_flg"));

				this.putModuleData(module, 1, true);
			}
			
		} catch (SQLException sqle) {
			log.error("Unable to list OP Sales Rep Data", sqle);
		}
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Retrieving sales reps");
		String order = StringUtil.checkVal(req.getParameter("order"), "last_nm, first_nm");
		Boolean repInfo = Convert.formatBoolean(req.getParameter("repInfo"));
		Boolean searchSubmitted = (StringUtil.checkVal(req.getParameter("searchSubmitted")).length() > 0);
		log.debug("Order: " + order);
		StringEncoder se = new StringEncoder();
		String searchFN = se.decodeValue(StringUtil.checkVal(req.getParameter("searchFirstName")));
		String searchLN = se.decodeValue(StringUtil.checkVal(req.getParameter("searchLastName")));
		
		// Setup the paging
		NavManager nav = new NavManager();
		nav.setRpp(Convert.formatInteger(req.getParameter("rpp"), 25));
		nav.setCurrentPage(Convert.formatInteger(req.getParameter("page"), 1));
		nav.setBaseUrl(req.getRequestURI() + "?order=" + order + "&searchFirstName=" + searchFN + "&searchLastName=" + searchLN);
		int ctr = 1;
		
		// Setup the sql
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select * from ").append(schema).append("op_sales_rep a ");
		
		if (repInfo) {
			sql.append("left outer join profile_role b on a.profile_role_id = b.profile_role_id ");
			sql.append("left outer join role c on b.role_id = c.role_id ");
			sql.append("where a.sales_rep_id = ? ");
		} else if (searchSubmitted) {
			sql.append("where 1=1 ");
			if (searchFN.length() > 0) sql.append("and first_nm like ? ");
			if (searchLN.length() > 0) sql.append("and last_nm like ? ");
		}
		
		sql.append("order by ").append(order);
		log.info("op Sales Rep SQL: " + sql + "|" + req.getParameter("salesRepId"));
		
		// Retrieve the data and store into a Map
		List<SalesRepVO> data = new ArrayList<SalesRepVO>();
		PreparedStatement ps = null;
		int val = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (repInfo) {
				ps.setString(val++, req.getParameter("salesRepId"));
				//if (repInfo) sql.append("and a.sales_rep_id = ? ");
			} else if (searchSubmitted) {
				if (searchFN.length() > 0) ps.setString(val++, "%" + searchFN + "%");
				if (searchLN.length() > 0) ps.setString(val++, "%" + searchLN + "%");
			}
			
			ResultSet rs = ps.executeQuery();
			SalesRepVO rep = new SalesRepVO();
			for(;rs.next(); ctr++) {
				if ((ctr >= nav.getStart() && ctr <= nav.getEnd()) && ! repInfo) {
					data.add(new SalesRepVO(rs));
				} else if (repInfo) {
					if (ctr == 1) rep = new SalesRepVO(rs);
				}
			} 
			
			// Add the data for the info retrieve
			if (repInfo) data.add(rep);
		} catch(SQLException sqle) {
			log.error("Error retrieving OP sales reps", sqle);
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		nav.setTotalElements(ctr - 1);
		req.setAttribute("navigationManager", nav);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setDataSize(nav.getTotalElements());
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("deleteRep"))) {
			this.deleteRep(req);
		} else if (Convert.formatBoolean(req.getParameter("exportRepData"))) {
			this.exportSalesRepData(req);
		} else {
			this.manageUsers(req);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 */
	public void manageUsers(ActionRequest req) throws ActionException {
		SalesRepVO rep = new SalesRepVO(req);
		String salesRepId = StringUtil.checkVal(req.getParameter("salesRepId"));

		String message = "You have successfully updated the sales rep information";

		try {
			// Load the WC user
			UserDataVO user = this.manageWCProfile(req, rep); 
			user.setPassword(req.getParameter("password"));
			
			// Assign the site roles
			rep.setProfileRoleId(this.manageUserRole(req, user.getProfileId()));
			
			// Update the Sales Rep Table
			salesRepId = this.updateUser(user, rep, salesRepId);
			
			// update the password
			this.manageAuthUpdate(user, req);

		} catch (Exception sqle) {
			log.error("Error updating op sales repr", sqle);
			message = "Error updating op sales rep information";
		}
		
		PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath(),message, req);
	}
	
	/**
	 * If the password is present, update the authentication stuff
	 * @param user
	 * @param req
	 */
	public void manageAuthUpdate(UserDataVO user, ActionRequest req) {
		boolean expirePassword = Convert.formatBoolean(req.getParameter("expirePassword"));
		if (StringUtil.checkVal(user.getPassword()).length() > 0 || expirePassword) {
			log.debug("** Updating password and/or password reset flag **");
			String encryptKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			String oldEmail = req.getParameter("oldEmail");
			log.debug("Old Email: " + oldEmail);
			log.debug("Auth ID: " + user.getAuthenticationId());
			
			// if user auth id is null, this is an insert, set flag to update profile with auth id
			boolean updateProfileAuth = (user.getAuthenticationId() == null ? true : false);
			
			try {
				Map<String, Object> lm = new HashMap<String, Object>();
				lm.put(Constants.ENCRYPT_KEY, encryptKey);
				lm.put(GlobalConfig.KEY_DB_CONN, dbConn);
				AbstractLoginModule loginModule = SecurityModuleFactoryImpl.getLoginInstance(site.getLoginModule(), lm);
				
				//try to discover the authId via emailAddress lookup
				if (StringUtil.checkVal(user.getAuthenticationId()).length() == 0)
					user.setAuthenticationId(loginModule.retrieveAuthenticationId(oldEmail));
				
				log.debug("Auth ID from lookup: " + user.getAuthenticationId());
				
				// determine what the password reset value should be
				int resetVal = expirePassword ? 1 : 0;
				//insert or update the AUTHENTICATION table and set ID on the user object; pwd will be encrypted at qry
				user.setAuthenticationId(loginModule.saveAuthRecord(user.getAuthenticationId(), user.getEmailAddress(),	user.getPassword(), resetVal));
				
				// update profile with auth ID if applicable
				if (updateProfileAuth) this.updateProfileAuth(user);
			} catch (Exception e) {
				log.error("Error managing sales rep's authentication, ", e);
			}
		}
	}
	
	/**
	 * Manages the user role for the site
	 * @param req
	 * @throws DatabaseException
	 */
	public String manageUserRole(ActionRequest req, String profileId) 
	throws DatabaseException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole(site.getSiteId());
		role.setProfileRoleId(req.getParameter("profileRoleId"));
		role.setProfileId(profileId);
		role.setRoleId(req.getParameter("roleId"));
		role.setStatusId(20);
		
		// Check for an existing role on the requested site
		if (StringUtil.checkVal(role.getProfileRoleId()).length() == 0) {
			role.setProfileRoleId(prm.checkRole(role.getProfileId(), role.getSiteId(), dbConn));
		}

		// Update the role
		prm.addRole(role, dbConn);
		
		return role.getProfileRoleId();
	}
	
	/**
	 * 
	 * @param user
	 * @param rep
	 * @param salesRepId
	 * @throws SQLException
	 */
	public String updateUser(UserDataVO user, SalesRepVO rep, String salesRepId) 
	throws SQLException {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		
		if (salesRepId.length() == 0) {
			sql.append("insert into ").append(schema).append("op_sales_rep ");
			sql.append("(op_login_id, profile_role_id, first_nm, last_nm, ");
			sql.append("email_address_txt, phone_number_txt, profile_id, ");
			sql.append("create_dt, class_id, region_id, territory_id, sales_rep_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?) ");
			salesRepId = user.getProfileId();
		} else {
			sql.append("update ").append(schema).append("op_sales_rep ");
			sql.append("set op_login_id = ?, profile_role_id = ?, ");
			sql.append("first_nm = ?, last_nm = ?, email_address_txt = ?, ");
			sql.append("phone_number_txt = ?, profile_id = ?, ");
			sql.append("update_dt = ?, class_id = ?, region_id = ?, territory_id = ? where sales_rep_id = ?");
		}
		
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, rep.getLoginId());
		ps.setString(2, rep.getProfileRoleId());
		ps.setString(3, rep.getFirstName().trim());
		ps.setString(4, rep.getLastName().trim());
		ps.setString(5, rep.getEmailAddress());
		ps.setString(6, rep.getPhoneNumber());
		ps.setString(7, user.getProfileId());
		ps.setTimestamp(8, Convert.getCurrentTimestamp());
		ps.setString(9, rep.getClassId());
		ps.setString(10, rep.getRegionId());
		ps.setString(11, rep.getTerritoryId());
		ps.setString(12, salesRepId);
		ps.executeUpdate();
		ps.close();
		
		return salesRepId;
		
	}
	
	/**
	 * 
	 * @param req
	 * @param rep
	 * @return
	 * @throws DatabaseException
	 */
	public UserDataVO manageWCProfile(ActionRequest req, SalesRepVO rep) 
	throws DatabaseException {
		// Initialize the Profile Manager
	    ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
	    
		// Assign a Profile
		UserDataVO user = rep.getUserData();
		
		// set the phone type so the phone insert/update succeeds
		user.getPhoneNumbers().get(0).setPhoneType(PhoneVO.WORK_PHONE);
		
		// Set the 'allow communication' flag to 1 as reps are auto opted-in
		user.setAllowCommunication(new Integer(1));
		log.info("User: " + user);

    	// If no profile Id exists, try to look up the user profile.
		if (StringUtil.checkVal(user.getProfileId()).length() == 0) {
			try {
				user.setProfileId(StringUtil.checkVal(pm.checkProfile(user, dbConn)));
			} catch (Exception e) {
				user.setProfileId(null);
			}
		}
		log.info("ProfileId Info Again: " + user.getProfileId());
	    
    	// If there is no profile found, 
    	if (StringUtil.checkVal(user.getProfileId()).length() == 0) {
    		user.setAuthenticationId(null);
    		pm.updateProfile(user, dbConn); //runs insert query
    	} else {
	    	// update the profile
    		PhoneVO phone = user.getPhoneNumbers().get(0);
    		
    		log.debug("Updating the profile");
	    	Map<String, Object> fields = new HashMap<String, Object>();
	    	fields.put("FIRST_NM", user.getFirstName());
	    	fields.put("LAST_NM", user.getLastName());
	    	fields.put("EMAIL_ADDRESS_TXT", user.getEmailAddress());
	    	fields.put("PHONE_NUMBER_TXT", phone.getPhoneNumber());
	    	pm.updateProfilePartially(fields, user, dbConn);
    	}
    	// Set the rep's communication flag to 1
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		pm.assignCommunicationFlg(site.getOrganizationId(), user.getProfileId(),1, dbConn, "OP_SALES_REP");
		
		return user;
	}
	
	/**
	 * Updates the sales rep's profile with the rep's authentication ID.  Called only 
	 * when a new rep is being added.
	 * @param user
	 * @throws DatabaseException
	 */
	private void updateProfileAuth(UserDataVO user) throws DatabaseException {
		if (user.getAuthenticationId() == null) return;
		// Initialize the Profile Manager
	    ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
   		log.debug("Updating the profile with authentication id");
    	Map<String, Object> fields = new HashMap<String, Object>();
    	fields.put("AUTHENTICATION_ID", user.getAuthenticationId());
	    pm.updateProfilePartially(fields, user, dbConn);
	}
	
	/**
	 * Calls appropriate action to export sales rep data in the requested format.
	 * @param req
	 */
	private void exportSalesRepData(ActionRequest req) {
		log.debug("exporting sales rep data");
		ActionInterface sai = new SalesRepReportAction();
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving sales rep data export, ", ae);
		}
	}

}
