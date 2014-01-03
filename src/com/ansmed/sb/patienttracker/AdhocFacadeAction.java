package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WC libs
import com.smt.sitebuilder.action.tracker.PatientManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.tracker.data.AssignmentPatientComparator;
import com.smt.sitebuilder.action.tracker.exception.PatientException;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
* <b>Title</b>AdhocFacadeAction.java<p/>
* <b>Description: Facades the creation of an adhoc interaction for a new patient/assignment or
* an existing patient/assignment.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 12, 2012
* <b>Changes: </b>
* Oct 12, 2012: David Bargerhuff - Phase 3 - #3007
****************************************************************************/
public class AdhocFacadeAction extends TrackerAction {
	
	public AdhocFacadeAction () {}
	
	public AdhocFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) {
		log.debug("AdhocInteractionFacadeAction retrieve...");
		String patientId = StringUtil.checkVal(req.getParameter("patientId"));
		List<AssignmentVO> patientData = null;
		// make sure assigneeId is on the req. for the JSTL to use
		String assigneeId = this.checkAssignee(req);
		if (patientId.length() == 0) {
			// retrieve patient records.
			if (assigneeId != null) {
				Map<String, AssignmentVO> assignmentData = this.retrieveBaseData(req, assigneeId);
				patientData = this.retrievePatientProfiles(assignmentData);
			}
		} else {
			// retrieve patient record for the given patientId
			AssignmentVO avo = new AssignmentVO();
			PatientVO p = null;
			try {
				p = this.retrievePatient(req, patientId);
			} catch (PatientException pe) {
				log.error("Error retrieving patient data, ", pe);
			}
			avo.setPatient(p);
			patientData = new ArrayList<AssignmentVO>();
			patientData.add(avo);
		}
		TrackerDataContainer tdc = new TrackerDataContainer();
		tdc.setAssignments(patientData);
		
		// retrieve the 'patient' form for use by the JSTL view.
		FormVO patientForm = this.retrieveForm(req, "patient");
		tdc.setPatientForm(patientForm);
		
		// put data on module
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Returns the assignee ID based on the ambassador's role.  For ambassadors who are admins
	 * the assignee ID must be retrieved from the db.  Assignee IDs for non-admin ambassadors 
	 * exist on the request.
	 * @param req
	 * @return
	 */
	private String checkAssignee(SMTServletRequest req) {
		String assigneeId = null;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL) {
			assigneeId = req.getParameter("assigneeId");
		} else {
			assigneeId = this.retrieveAssigneeId(req);
			req.setParameter("assigneeId", assigneeId, true);
		}
		return assigneeId;
	}
	
	/**
	 * Retrieves the assignee ID for this ambassador based on the ambassador's profile ID.
	 * @param req
	 * @return
	 */
	private String retrieveAssigneeId(SMTServletRequest req) {
		String id = null;
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String sql = "select assignee_id from pt_assignee where assignee_profile_id = ? ";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getString(1);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving assignee ID, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		return id;
	}
	
	/**
	 * Retrieves basic data needed for determining to whom this assignee has been assigned
	 * in the past.
	 * @param req
	 * @return
	 */
	private Map<String, AssignmentVO> retrieveBaseData(SMTServletRequest req, String assigneeId) {
		StringBuilder sql = new StringBuilder();
		sql.append("select b.pt_assignment_id, b.assignee_id, a.patient_profile_id, a.patient_id ");
		sql.append("from pt_patient a ");
		sql.append("inner join PT_ASSIGNMENT b on a.patient_id = b.patient_id ");
		sql.append("inner join PT_ASSIGNMENT_LOG c on b.PT_ASSIGNMENT_ID = c.PT_ASSIGNMENT_ID ");
		sql.append("where c.ASSIGNEE_ID = ? and (c.ASSIGNMENT_STATUS_ID = ? or ");
		sql.append("(c.ASSIGNMENT_STATUS_ID = ? and c.ASSIGNMENT_RESPONSE_ID = ?)) ");
		sql.append("and b.ASSIGNMENT_STATUS_ID >= ? ");
		sql.append("group by b.PT_ASSIGNMENT_ID, b.ASSIGNEE_ID, a.PATIENT_PROFILE_ID, a.PATIENT_ID");
		log.debug("base data SQL: " + sql.toString());
		log.debug("assigneeId: " + req.getParameter("assigneeId"));
		PreparedStatement ps = null;
		Map<String, AssignmentVO> data = new HashMap<String, AssignmentVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, assigneeId);
			ps.setInt(2, SJMTrackerConstants.STATUS_COMPLETED);
			ps.setInt(3, SJMTrackerConstants.STATUS_REASSIGNED);
			ps.setInt(4, SJMTrackerConstants.RESPONSE_ADHOC);
			ps.setInt(5, SJMTrackerConstants.STATUS_COMPLETED);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssignmentVO avo = new AssignmentVO();
				avo.setData(rs);
				PatientVO p = new PatientVO();
				p.setData(rs);
				avo.setPatient(p);
				data.put(rs.getString("patient_profile_id"), avo);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving patients historically assigned to this assignee, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		log.debug("assignment/patient map size: " + data.size());
		return data;
	}
	
	/**
	 * Retrieves list of AssignmentVOs populated with the first/lastname of the patient.
	 * @param baseData
	 * @return
	 */
	private List<AssignmentVO> retrievePatientProfiles(Map<String, AssignmentVO> baseData) {
		List<String> profileIds = new ArrayList<String>();
		List<AssignmentVO> assignData = new ArrayList<AssignmentVO>();
		// create list of profile ids...map's keys are the profile ids.
		for (String key : baseData.keySet()) {
			profileIds.add(key);
		}
		// retrieve profiles.
		List<UserDataVO> profiles = null;
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			profiles = pm.searchProfile(dbConn, profileIds);
		} catch (DatabaseException de) {
			log.error("Error retrieving patient profiles, ", de);
		}
		
		// create list of PatientVOs.
		for (UserDataVO user : profiles) {
			if (baseData.get(user.getProfileId()) != null) {
				baseData.get(user.getProfileId()).getPatient().setFirstName(user.getFirstName());
				baseData.get(user.getProfileId()).getPatient().setLastName(user.getLastName());
				assignData.add(baseData.get(user.getProfileId()));
			}
		}
		// sort by patient name;
		Collections.sort(assignData, new AssignmentPatientComparator());
		log.debug("assignData size: " + assignData.size());
		return assignData;
	}
	
	/**
	 * Retrieves a single patient
	 * @param req
	 * @param patientId
	 * @return A TrackerDataContainer containing the patient data for the patient with the 
	 * given patientId.
	 * @throws PatientException
	 */
	private PatientVO retrievePatient(SMTServletRequest req, String patientId) 
		throws PatientException {
		log.debug("retrieving patient records for patientID: " + patientId);
		
		/* DBargerhuff 10-25-2012
		 * Note: When querying for patients, the PatientManager filters the query
		 * by assigneeId for non-admins performing the query.  In the 'adhoc' case
		 * we need to query for the patient using the 'current' assigneeId which may
		 * be different than the assigneeId of the actual ambassador doing the query.
		 * We're going to temporarily replace the assigneeId on the request to ensure
		 * that we retrieve the patient data correctly.
		 */ 
		String origAssigneeId = StringUtil.checkVal(req.getParameter("assigneeId"));
		req.setParameter("assigneeId", req.getParameter("currAssigneeId"), true);
		log.debug("original/proxy assigneeId is: " + origAssigneeId + "/" + req.getParameter("assigneeId"));
		SMTActionInterface sai = new PatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			throw new PatientException();
		} finally {
			req.setParameter("assigneeId", origAssigneeId, true);
		}
		log.debug("assigneeId on req is now: " + req.getParameter("assigneeId"));
		PatientVO patient = null;
		TrackerDataContainer tdc = null;
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			tdc = (TrackerDataContainer) mod.getActionData();
			if (tdc.getPatients() != null && tdc.getPatients().size() > 0) {
				patient = tdc.getPatients().get(0);
			}
		}
		return patient;
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) {
		log.debug("AdhocFacadeAction build...");
		String patientId = StringUtil.checkVal(req.getParameter("patientId"));
		SMTActionInterface sai = null;
		if (patientId.length() == 0) {
			sai = new AdhocCreateAction(actionInit);
		} else {
			sai = new AdhocUpdateAction(actionInit);
		}
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		
		try {
			dbConn.setAutoCommit(false);
			sai.build(req);
		} catch (Exception e1) {
			log.error("Error processing adhoc interaction, ", e1);
			try {
				dbConn.rollback();
			} catch (Exception e2) {
				log.error("Error rolling back adhoc processing transaction, ", e2);
			}
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e3) {log.error("Error resetting autocommit to 'true', ", e3);}
		}
	}
}
