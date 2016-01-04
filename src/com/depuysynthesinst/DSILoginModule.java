package com.depuysynthesinst;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.assg.AssignmentVO;
import com.depuysynthesinst.assg.MyAssignmentsAction;
import com.depuysynthesinst.assg.MyAssignmentsAdminAction;
import com.depuysynthesinst.lms.LMSWSClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
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
		
		if (dsiUser.getTtLmsId() != null) {
			loadLMSData(dsiUser);
		} else if (dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isFellow(dsiUser) || dsiRoleMgr.isChiefResident(dsiUser)) {
			//flag the account as incomplete so we can prompt them to complete their registration data (and get a TTLMSID)
			dsiUser.addAttribute("incomplete", true);
		}
		
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
			log.error("could not load user course list", ae);
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
}