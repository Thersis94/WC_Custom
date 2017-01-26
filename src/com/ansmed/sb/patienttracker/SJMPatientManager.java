package com.ansmed.sb.patienttracker;

// SMT Base libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.PatientManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>SJMPatientManager.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Aug 08, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMPatientManager extends SBActionAdapter {
	
	public final static Integer PATIENT_STATUS_ARCHIVED = 99;
	
	public SJMPatientManager() {
		super();
	}

	public SJMPatientManager(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("SJMPatientManager retrieve...");
		TrackerDataContainer data = new TrackerDataContainer();
		ActionInterface sai = null;
		// retrieve patients
		sai = new PatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		// if this was a search operation, return
		if (req.getParameter("searchSubmitted") != null) return; 
		
		// retrieve the container from the previous action
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		data.setPatients(tdc.getPatients());
		data.setPatientForm(tdc.getForm());
		log.debug("patients list size: " + data.getPatients().size());
		
		// retrieve assignees for the 'add' form
		sai = new AssigneeManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		tdc = (TrackerDataContainer) mod.getActionData();
		data.setAssignees(tdc.getAssignees());
		log.debug("assignees list size: " + data.getAssignees().size());
		
		// set data on request
		mod.setActionData(data);
		req.setAttribute(Constants.MODULE_DATA, mod);
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("SJMPatientManager build...");
		
		// insert or update?
		String patientId = StringUtil.checkVal(req.getParameter("patientId"));
		boolean isUpdate = (patientId.length() > 0);
		log.debug("isUpdate: " + isUpdate);
		
		// insert/update the assignment
		ActionInterface sai = null;
		sai = new PatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);

		// build the redirect depending upon insert or update
		this.processRedirect(req, isUpdate);
	}
	
	/**
	 * Helper method that checks to see if a patient has any 'active' assignments
	 * that reference the patient.  Assignments with status 'expired' and 'closed' 
	 * do not count.
	 * @param patientId
	 * @return
	 * @throws ActionException
	 */
	protected boolean checkForActiveAssignments(String patientId) throws ActionException {
		// check for active assignments referencing this patient
		boolean hasActiveAssignments = false;
		StringBuffer sql = new StringBuffer();
		sql.append("select * from pt_assignment where patient_id = ? and assignment_status_id < ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, patientId);
			ps.setInt(2, SJMTrackerConstants.STATUS_EXPIRED);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				hasActiveAssignments = true;
			}
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("patient hasActiveAssignments: " + hasActiveAssignments);
		return hasActiveAssignments;
	}
	
	/**
	 * Helper method that sets patient's status to 'archived'
	 * @param patientId
	 * @throws ActionException
	 */
	protected void archivePatient(String patientId) throws ActionException {
		// updates patient status to 'archived'
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_patient set pt_status_id = ? where patient_id = ?");
		log.debug("patient archive SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, SJMPatientManager.PATIENT_STATUS_ARCHIVED);
			ps.setString(2, patientId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error archiving patient, ", sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Builds redirect based on whether this is an update or an insert
	 * @param req
	 * @param isUpdate
	 */
	private void processRedirect(ActionRequest req, boolean isUpdate) {
		StringBuffer url = new StringBuffer();
		String fromType = StringUtil.checkVal(req.getParameter("fromType"));
		
		PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		url.append(StringUtil.checkVal(attributes.get("contextPath")));
		url.append(page.getFullPath());
		
		if (fromType.equalsIgnoreCase("interaction")) {
			// came from the admin's unified patient view
	    	url.append("?actionType=interaction");
	    	url.append("&assignmentId=").append(req.getParameter("assignmentId"));
	    	url.append("&logs=true");
		} else {
			// this was a 'patient' action type
			url.append("?actionType=patient");
		}
		url.append("&organizationId=").append(req.getParameter("organizationId"));
		url.append("&msg=").append(req.getAttribute(TrackerAction.TRACKER_BUILD_MSG));
		log.debug("SJMPatientManager redirect URL: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
}
