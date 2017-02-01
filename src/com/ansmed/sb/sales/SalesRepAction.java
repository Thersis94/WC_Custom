package com.ansmed.sb.sales;

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
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.exception.DatabaseException;

// SB Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

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
		StringBuffer sql = new StringBuffer();
		String message = "You have successfully deleted the sales rep";
		String schema = (String)getAttribute("customDbSchema");
		sql.append("delete from ").append(schema).append("ans_sales_rep ");
		sql.append("where sales_rep_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("salesRepId"));
			
			int success = ps.executeUpdate();
			if (success == 0) message = "Error updating area information.";
		} catch (SQLException sqle) {
			log.error("Error updating ans sales rep", sqle);
			if (sqle.getMessage().contains(TM1_FKEY_VALUE)) {
				message = "Error deleting sales rep. Sales reps with a role of TM3 ";
				message += "cannot be deleted if a TM1 is still assigned to them.";
			} else {
				message = "Error deleting sales rep. Sales reps cannot be deleted ";
				message += "until all physicians are reassigned to other sales reps.";
			}
		}
		
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Retrieving sales reps");
		String order = StringUtil.checkVal(req.getParameter("order"), "last_nm, first_nm");
		Boolean repInfo = Convert.formatBoolean(req.getParameter("repInfo"));
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
		sql.append("select a.*, region_nm from ").append(schema).append("ans_sales_rep a ");
		sql.append("inner join ").append(schema).append("ans_sales_region b ");
		sql.append("on a.region_id = b.region_id where 1=1 "); 
		if (repInfo) sql.append("and a.sales_rep_id = ? ");
		if (searchFN.length() > 0) sql.append("and first_nm like ? ");
		if (searchLN.length() > 0) sql.append("and last_nm like ? ");
		
		sql.append("order by ").append(order);
		log.info("ANS Sales Rep SQL: " + sql + "|" + req.getParameter("salesRepId"));
		
		// Retrieve the data and store into a Map
		List<SalesRepVO> data = new ArrayList<SalesRepVO>();
		PreparedStatement ps = null;
		int val = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (repInfo) ps.setString(val++, req.getParameter("salesRepId"));
			if (repInfo) sql.append("and a.sales_rep_id = ? ");
			if (searchFN.length() > 0) ps.setString(val++, "%" + searchFN + "%");
			if (searchLN.length() > 0) ps.setString(val++, "%" + searchLN + "%");
			
			ResultSet rs = ps.executeQuery();
			for(;rs.next(); ctr++) {
				if ((ctr >= nav.getStart() && ctr <= nav.getEnd()) || repInfo) {
					data.add(new SalesRepVO(rs));
				}
			} 
		} catch(SQLException sqle) {
			log.error("Error retrieving ans sales reps", sqle);
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
	public void update(ActionRequest req) throws ActionException {
		StringBuffer sql = new StringBuffer();
		String salesRepId = StringUtil.checkVal(req.getParameter("salesRepId"));
		SalesRepVO rep = new SalesRepVO(req);
		
		// Only set the atmRepId if the role selected was ATM
		String atmRepId = null;
		if (Convert.formatBoolean(req.getParameter("atmType"))) 
			atmRepId = rep.getAtmRepId();
		
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the territory manager information";
		
		// Initialize the Profile Manager
	    ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
	    
		// Assign a Profile
		UserDataVO user = rep.getUserData();
		
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
	    try {
	    	// If there is no profile found, 
	    	if (StringUtil.checkVal(user.getProfileId()).length() == 0) {
	    		user.setAuthenticationId(null);
	    		pm.updateProfile(user, dbConn); //runs insert query
	    	} else {
		    	// update the profile
	    		log.debug("Updating the profile");
		    	Map<String, Object> fields = new HashMap<String, Object>();
		    	fields.put("FIRST_NM", user.getFirstName());
		    	fields.put("LAST_NM", user.getLastName());
		    	fields.put("EMAIL_ADDRESS_TXT", user.getEmailAddress());
		    	pm.updateProfilePartially(fields, user, dbConn); 
	    	}
	    	// Set the rep's communication flag to 1
	    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			pm.assignCommunicationFlg(site.getOrganizationId(), user.getProfileId(), 
					1, dbConn, "SJM_SALES_REP");
	    	
	    } catch (DatabaseException de) {
	    	log.error("Error adding profile", de);
	    }
		
		if (salesRepId.length() == 0) {
			sql.append("insert into ").append(schema).append("ans_sales_rep ");
			sql.append("(region_id, ans_login_id, role_id, first_nm, last_nm, ");
			sql.append("email_address_txt, phone_number_txt, profile_id, atm_rep_id, ");
			sql.append("create_dt, sales_rep_id) values (?,?,?,?,?,?,?,?,?,?,?) ");
			salesRepId = user.getProfileId();
		} else {
			sql.append("update ").append(schema).append("ans_sales_rep ");
			sql.append("set region_id = ?, ans_login_id = ?, role_id = ?, ");
			sql.append("first_nm = ?, last_nm = ?, email_address_txt = ?, ");
			sql.append("phone_number_txt = ?, profile_id = ?, atm_rep_id = ?, ");
			sql.append("update_dt = ? where sales_rep_id = ?");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, rep.getRegionId());
			ps.setString(2, rep.getLoginId());
			ps.setString(3, rep.getRoleId());
			ps.setString(4, rep.getFirstName().trim());
			ps.setString(5, rep.getLastName().trim());
			ps.setString(6, rep.getEmailAddress());
			ps.setString(7, rep.getPhoneNumber());
			ps.setString(8, user.getProfileId());
			ps.setString(9, atmRepId);
			ps.setTimestamp(10, Convert.getCurrentTimestamp());
			ps.setString(11, salesRepId);
			
			int success = ps.executeUpdate();
			if (success == 0) message = "Error updating territory manager information";
		} catch (SQLException sqle) {
			log.error("Error updating ans territory manager", sqle);
			message = "Error updating territory manager information";
		}
		
	
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);
	}

}
