package com.ansmed.sb.patienttracker;

// SMT Base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC libs
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.PatientAction;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>AdhocCreateAction.java<p/>
* <b>Description: Creates an adhoc interaction for a new patient/assignment.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 23, 2012
* <b>Changes: </b>
* Oct 23, 2012: David Bargerhuff - Phase 3 - #3007
****************************************************************************/
public class AdhocCreateAction extends TrackerAction {
	
	public AdhocCreateAction () {}
	
	public AdhocCreateAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("AdhocCreateAction build...");
		// need patientId and assignmentId from the request
		AssignmentVO avo = new AssignmentVO();
		avo.setData(req);
		req.setParameter("assignmentId", null); // make sure assignmentId is null
		// 1. create patient
		PatientVO patient = this.createPatient(req);
		if (patient != null) {
			avo.setPatient(patient);
			avo.setPatientId(patient.getPatientId());
			this.manageAssignment(req, avo);
			msg.append("You have successfully created an interaction for this patient.");
		} else {
			msg.append("An interaction for this patient could not be created.");
		}
		
		// 2. return response redirect.
    	StringBuffer url = new StringBuffer();
    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
    	url.append(page.getFullPath());
    	url.append("?actionType=adhoc&msg=");
    	url.append(msg);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

	/**
	 * Creates a new patient
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private PatientVO createPatient(SMTServletRequest req) throws ActionException {
		PatientVO newPatient = null;
		
		// preserve the 'interaction' formId which is on the request as 'sbActionId'
		String iFormId = req.getParameter("sbActionId");
		req.setParameter("sbActionId", "", true);
		
		SMTActionInterface sai = new SJMPatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		newPatient = (PatientVO) req.getAttribute(PatientAction.PATIENT_DATA);
		req.setParameter("sbActionId", iFormId, true);
		return newPatient;		
	}
	
	private void manageAssignment(SMTServletRequest req, AssignmentVO avo) 
		throws ActionException {
		log.debug("managing assignment...");
		// create the assignment
		log.debug("creating the assignment...");
		req.setParameter("patientId", avo.getPatient().getPatientId());
		req.setParameter("formSubmittalId", null);
		avo.setCurrentStatusId(0);
		avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_PENDING);
		avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
		this.updateAssignment(req, avo);
		avo.setAssignmentId(StringUtil.checkVal(req.getParameter("assignmentId")));
		
		
		// update to 'in progress', log 'in progress'
		log.debug("updating assignment to 'in progress'");
		avo.setCurrentStatusId(SJMTrackerConstants.STATUS_PENDING);
		avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
		avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
		this.updateAssignment(req, avo);
		
		// save interaction
		this.createInteraction(req, avo);
		
		boolean isComplaint = Convert.formatBoolean(req.getParameter("productComplaint"));
		if (! isComplaint) {
			// update to 'completed', log to 'completed'
			log.debug("updating assignment to 'completed'");
			avo.setCurrentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS); // set same as assignment status to bypass email.
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_COMPLETED);
			avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			this.updateAssignment(req, avo);
		}
	}
	
	/**
	 * Updates the assignment
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void updateAssignment(SMTServletRequest req, AssignmentVO avo) 
			throws ActionException {
		// update the assignment
		if (avo.getAssignmentId() != null) {
			req.setParameter("currentAssignmentStatusId", avo.getCurrentStatusId().toString(), true);
		}
		req.setParameter("assignmentStatusId", avo.getAssignmentStatusId().toString(), true);
		req.setParameter("assignmentResponseId", avo.getAssignmentResponseId().toString(), true);
		SMTActionInterface sai = null;
		sai = new SJMAssignmentManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		req.setParameter("formSubmittalId", null);
		this.resetLoggingParameters(req);
	}
	
	/**
	 * Creates an interaction for this assignment based on the form data passed in on 
	 * the request.
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void createInteraction(SMTServletRequest req, AssignmentVO avo) 
		throws ActionException {
		log.debug("creating adhoc interaction entry...");
		req.setParameter("processInteraction", "true");
		req.setParameter("formSubmittalId", null); // prep
		SMTActionInterface sai = null;
		sai = new SJMPatientInteractionManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		req.setParameter("formSubmittalId", null); // clean-up
	}
	
	private void resetLoggingParameters(SMTServletRequest req) {
		// clear logging params
		req.setParameter(AssignmentLogManager.LOG_STATUS_ID, null);
		req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, null);
		req.setParameter(AssignmentLogManager.LOG_ASSIGN_TEXT, null);
		req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, null);
	}
	
}
