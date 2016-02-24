package com.depuysynthesinst;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.depuysynthesinst.assg.AssignmentVO;
import com.depuysynthesinst.assg.MyAssignmentsAction;
import com.depuysynthesinst.assg.MyAssignmentsAdminAction;
import com.depuysynthesinst.lms.LMSWSClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * <b>Title</b>: DSILoginModule.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 9, 2015
 ****************************************************************************/
public class DSILoginModule extends SAMLLoginModule {

	/**
	 * 
	 */
	public DSILoginModule() {
	}

	/**
	 * @param config
	 */
	public DSILoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO retrieveUserData(String user, String pwd)
			throws AuthenticationException {
		
		DSIUserDataVO dsiUser = new DSIUserDataVO(super.retrieveUserData(user, pwd));
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		
		if (dsiUser.getTtLmsId() != null && Convert.formatInteger(dsiUser.getTtLmsId()) > 0) {
			loadLMSData(dsiUser);
		} else if (dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isFellow(dsiUser) || dsiRoleMgr.isChiefResident(dsiUser)) {
			//flag the account as incomplete so we can prompt them to complete their registration data (and get a TTLMSID)
			dsiUser.addAttribute("incomplete", true);
		} else if (UserDataVO.AuthenticationType.SAML == dsiUser.getAuthType()) { 
			//allow all J&J WWID users through, but they need to be given a TTLMS account first
			SMTServletRequest req = (SMTServletRequest)initVals.get(GlobalConfig.HTTP_REQUEST);
			makeLMSAccount(dsiUser, req);
		}
		
		if (dsiRoleMgr.isAssgUser(dsiUser))
			addAssgCount(dsiUser);
		
		log.debug("loaded dsiUser " + dsiUser.getEmailAddress());
		return dsiUser.getUserDataVO();
	}
	
	@Override
	public UserDataVO retrieveUserData(String encProfileId) throws AuthenticationException {
		DSIUserDataVO dsiUser = new DSIUserDataVO(super.retrieveUserData(encProfileId));
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		
		if (dsiUser.getTtLmsId() != null) {
			loadLMSData(dsiUser);
		} else if (dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isFellow(dsiUser) || dsiRoleMgr.isChiefResident(dsiUser)) {
			//flag the account as incomplete so we can prompt them to complete their registration data (and get a TTLMSID)
			dsiUser.addAttribute("incomplete", true);
		}
		
		if (dsiRoleMgr.isAssgUser(dsiUser))
			addAssgCount(dsiUser);
		
		log.debug("loaded dsiUser from cookie " + dsiUser.getEmailAddress());
		return dsiUser.getUserDataVO();
	}
	
	
	/**
	 * call the LMS to get all the user's course records/stats, so we have them on-demand
	 * where needed on the website.
	 * LMS Student's see a section for "My Transcript" on their My Profile page.
	 * @param dsiUser
	 */
	private void loadLMSData(DSIUserDataVO dsiUser) {
		try {
			LMSWSClient lms = new LMSWSClient((String)super.initVals.get(LMSWSClient.CFG_SECURITY_KEY));
			dsiUser.setMyCourses(lms.getUserCourseList(dsiUser.getDsiId()));
		} catch (ActionException ae) {
			//ignore these errors; most users don't have courses to be concerned about and this doesn't impact functionality - JM 02.18.16
			//log.warn("could not load user course list", ae);
		}

		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		//if this is a Resident or Chief Resident, see if they have any pending 
		//resident director invitations they need to accept/decline
		if (dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isChiefResident(dsiUser)) {
			Map<String,UserDataVO> resDirs = loadPendingInvites(dsiUser.getProfileId());
			if (resDirs != null && resDirs.size() > 0)
				dsiUser.setPendingResDirs(resDirs);
		}
	}
	
	
	/**
	 * load a list of Resident Directors that have asked to be my proctor - which I need to acknowledge.
	 * @param profileId
	 * @return
	 */
	private Map<String,UserDataVO> loadPendingInvites(String profileId) {
		MyAssignmentsAction mra = new MyAssignmentsAction();
		mra.setAttributes(getInitVals());
		mra.setDBConnection(new SMTDBConnection((Connection)initVals.get(GlobalConfig.KEY_DB_CONN)));
		return mra.loadResidencyDirectors(profileId, true); //true=pendingOnly
	}
	
	
	/**
	 * get a count of the courses this user is enrolled in, or owns (Directors) - displays in left menu
	 * @param user
	 */
	private void addAssgCount(DSIUserDataVO user) {
		int cnt = 0;
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		
		if (dsiRoleMgr.isDirector(user)) {
			MyAssignmentsAdminAction maaa = new MyAssignmentsAdminAction();
			maaa.setAttributes(getInitVals());
			maaa.setDBConnection(new SMTDBConnection((Connection)initVals.get(GlobalConfig.KEY_DB_CONN)));
			cnt = maaa.loadAssignmentList(user.getProfileId(), null).size();
		} else {
			MyAssignmentsAction mra = new MyAssignmentsAction();
			mra.setAttributes(getInitVals());
			mra.setDBConnection(new SMTDBConnection((Connection)initVals.get(GlobalConfig.KEY_DB_CONN)));
			List<AssignmentVO> data =  mra.loadAssignmentList(user.getProfileId(), null);
			for (AssignmentVO vo : data)
				if (! vo.isComplete()) ++cnt;
		}
		user.addAttribute("myAssgCnt", cnt);	
	}
	
	
	/**
	 * create an LMS account for this WWID user, but make sure they don't already have one
	 * that we're not aware of first.
	 * @param user
	 */
	private void makeLMSAccount(DSIUserDataVO user, SMTServletRequest req) {
		SMTDBConnection dbConn = new SMTDBConnection((Connection)initVals.get(GlobalConfig.KEY_DB_CONN));
		RegistrationAction ra = new RegistrationAction();
		ra.setAttributes(getInitVals());
		ra.setDBConnection(dbConn);
		
		try {
			String rsId = loadRSId(user, req, dbConn);
			if (rsId == null) return; //not a legitimately registered user
			req.setAttribute("registerSubmittalId", rsId);
			user.setCountryCode("US");
			user.setHospital("WWID");
			user.setProfession("PROF");
			user.setSpecialty("AAWDS");
			user.setEligible(false);
			user.setVerified(false);
			ra.saveUser(user);
		
			String[] regFields = new String[]{ RegField.DSI_TTLMS_ID.toString(), 
															 RegField.DSI_SYNTHES_ID.toString(), 
															 RegField.DSI_PROG_ELIGIBLE.toString(), 
															 RegField.DSI_VERIFIED.toString(),
															 RegField.DSI_ACAD_NM.toString(),
															 RegField.DSI_COUNTRY.toString(),
															 RegField.c0a80241b71c9d40a59dbd6f4b621260.toString(), //Prof
															 RegField.c0a80241b71d27b038342fcb3ab567a0.toString()}; //Spec
			
			ra.captureLMSResponses(req, user, regFields);
		} catch (Exception e) {
			log.error("could not create LMS account for WWID user, profileId= " + user.getProfileId(), e);
		}
	}
	
	
	/**
	 * retrieves the register_submittal_id for this user on this website.
	 * If a registration record does not exist it gets created.
	 */
	private String loadRSId(DSIUserDataVO user, SMTServletRequest req, SMTDBConnection dbConn) 
			throws SQLException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select register_submittal_id from register_submittal ");
		sql.append("where profile_id=? and site_id=?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getProfileId());
			ps.setString(2, site.getSiteId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getString(1);
			
		} catch (SQLException sqle) {
			log.warn("no registerSubmittalId to bind data too", sqle);
		}
		
		//no account found (above), 
		//need to create a mock registration, so we have a place to save this user's data
		sql = new StringBuilder(100);
		sql.append("insert into register_submittal (register_submittal_id, site_id, ");
		sql.append("profile_id, action_id, create_dt, user_session_id) values (?,?,?,?,?,?)");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String pkId = new UUIDGenerator().getUUID();
			ps.setString(1, pkId);
			ps.setString(2, site.getSiteId());
			ps.setString(3, user.getProfileId());
			ps.setString(4, DSIUserDataVO.REG_ACTION_GROUP_ID);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, req.getSession().getId());
			ps.executeUpdate();
			return pkId;
			
		} catch (SQLException sqle) {
			throw sqle;
		}
	}
}
