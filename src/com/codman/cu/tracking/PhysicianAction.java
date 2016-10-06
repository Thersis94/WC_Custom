package com.codman.cu.tracking;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.codman.cu.tracking.vo.PhysicianVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PhysicianAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 02, 2010
 ****************************************************************************/
public class PhysicianAction extends SBActionAdapter {

	private Object msg = null;
	
	public PhysicianAction() {
		super();
	}
	
	public PhysicianAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String physicianId = req.getParameter("del");
		String[] queries = new String[2];
		StringBuilder sql = new StringBuilder();

		//delete all transactions not tied to 'active' Unit assignments
		sql.append("delete from ").append(customDb);
		sql.append("codman_cu_transaction where transaction_id in (");
		sql.append("select a.TRANSACTION_ID from ").append(customDb);
		sql.append("CODMAN_CU_TRANSACTION a left outer join ").append(customDb);
		sql.append("CODMAN_CU_UNIT_LEDGER b on a.TRANSACTION_ID=b.TRANSACTION_ID ");
		sql.append("where a.PHYSICIAN_ID=? and b.ACTIVE_RECORD_FLG != 1)");
		queries[0] = sql.toString();
		
		//delete the physician
		sql = new StringBuilder();
		sql.append("delete from ").append(customDb);
		sql.append("codman_cu_physician where physician_id = ? ");
		queries[1] = sql.toString();
				
		PreparedStatement ps = null;
		try {
			for (String s : queries) {
				log.debug(s + physicianId);
				ps = dbConn.prepareStatement(s);
				ps.setString(1, physicianId);
				ps.execute();
			}
		} catch (SQLException sqle) {
			log.warn(sqle);  //we know these will be foriegn key dependency errors
			msg = "This Physician cannot be deleted";
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.info("Starting PhysicianAction build...");
		msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		PhysicianVO pvo = new PhysicianVO(req);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		// create or update physician's profile
       	this.checkPhysicianProfile(req, pm, pvo);
		
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		
		if (StringUtil.checkVal(pvo.getPhysicianId()).length() == 0) {
			//insert
			pvo.setPhysicianId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(customDb).append("codman_cu_physician ");
			sql.append("(account_id, profile_id, create_dt, organization_id, center_txt, ");
			sql.append("department_txt, physician_id) values (?,?,?,?,?,?,?) ");
		} else {
			//update
			sql.append("update ").append(customDb).append("codman_cu_physician ");
			sql.append("set account_id = ?, profile_id = ?, update_dt=?, ");
			sql.append("organization_id=?, center_txt=?, department_txt=? ");
			sql.append("where physician_id = ?");
		}
		log.debug(sql + pvo.getPhysicianId());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			ps.setString(1, pvo.getAccountId());
			ps.setString(2, pvo.getProfileId());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, site.getOrganizationId());
			ps.setString(5, pvo.getCenterText());
			ps.setString(6, pvo.getDepartmentText());
			ps.setString(7, pvo.getPhysicianId());
			
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error(sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		

		// Setup the redirect
    	StringBuilder url = new StringBuilder();
    	url.append(req.getRequestURI());
    	url.append("?type=").append(req.getParameter("type"));
    	url.append("&accountId=").append(req.getParameter("accountId"));
    	url.append("&msg=").append(msg);
    	url.append("&jsCallback=").append(StringUtil.checkVal(req.getParameter("jsCallback")));
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
		
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Starting PhysicianAction retrieve...");
		if (req.getParameter("del") != null) 
			delete(req);
			
		String accountId = req.getParameter("accountId");
		String physicianId = req.getParameter("physicianId");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuilder sql = new StringBuilder();
		
		sql.append("select physician_id, account_id, profile_id, center_txt, department_txt, ");
		sql.append("create_dt from ").append(customDb);
		sql.append("codman_cu_physician b where account_id=? and organization_id=? ");
		
		if (physicianId != null)
			sql.append("and physician_id = ? ");
		
		log.debug(sql + " | " + accountId + " | " + physicianId);
		
		List<PhysicianVO> data = new ArrayList<PhysicianVO>();
		List<String> profileIds = new ArrayList<String>();
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, accountId);
			ps.setString(2, site.getOrganizationId());
			if (physicianId != null) ps.setString(3, physicianId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new PhysicianVO(rs));
				profileIds.add(rs.getString("profile_id"));
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		// retrieve the physician profiles and merge with the physician VO(s)
		try {
			this.retrievePhysicianProfiles(data, profileIds);
		} catch (DatabaseException de) {
			log.error(de);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		//sort the list by last name
		Collections.sort(data, new UserDataComparator());
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (msg != null) mod.setErrorMessage(msg.toString());
		//mod.setDataSize(data.size());
		//mod.setActionData(data);
		req.setAttribute("physicians", data);
		log.debug("loaded " + data.size() + " physicians");
	}
	
	/**
	 * Leverages ProfileManager to retrieve profile data for each physician profile ID.
	 * @param physicians
	 * @param pIds
	 * @throws DatabaseException
	 */
	private void retrievePhysicianProfiles(List<PhysicianVO> physicians, List<String> pIds)
		throws DatabaseException {

		//get the UserDataVOs for each physician
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, pIds);
		
		// loop the physician VO list
		for (PhysicianVO pv : physicians) {
			log.debug("pv=" + pv.getProfileId());
			if (pv.getProfileId() != null && profiles.containsKey(pv.getProfileId())) 
				pv.setData(profiles.get(pv.getProfileId()).getDataMap());
		}
		
	}

	/**
	 * Creates or updates physician's profile
	 * @param req
	 * @param pm
	 * @param vo
	 */
	public void checkPhysicianProfile(SMTServletRequest req, ProfileManager pm, PhysicianVO vo) {
		//save core PROFILE, PHONE_NO, & PROFILE_ADDRESS
		try {
			if (vo.getProfileId() == null || vo.getProfileId().length() == 0)
				vo.setProfileId(pm.checkProfile(vo, dbConn));
			
			if (vo.getProfileId() == null) {
				vo.setAllowCommunication(1); //opt-in new users only
				pm.updateProfile(vo, dbConn);
			} else {
				pm.updateProfilePartially(vo.getDataMap(), vo, dbConn);
			}
		} catch (DatabaseException de) {
			log.error(de);
		}
		log.debug("Physician profile saved");
	}
	
}
