package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//SMB Baselibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.PatientInteractionManager;
import com.smt.sitebuilder.action.tracker.PatientManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientInteractionVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>SJMReassigmentManager.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Nov 15, 2011
* <b>Changes: </b>
* Feb 16, 2012: DBargerhuff; refactored class to accommodate additional reassignment scenarios
****************************************************************************/
public class SJMReassignmentManager extends TrackerAction {
	
	private Integer emailType = new Integer(0);
	
	public SJMReassignmentManager() {
		super();
	}

	public SJMReassignmentManager(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("SJMReassignmentManager retrieve...");
		
		ActionInterface sai = null;
		req.setParameter("assignmentId", req.getParameter("fromAssignmentId"));
		// retrieve the assignment
		sai = new SJMAssignmentManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("SJMReassignmentManager build...");
		AssignmentVO avo = new AssignmentVO();
		avo.setData(req);
		boolean isDuplicate = this.checkForDuplicateAssignment(req, avo);
		if (isDuplicate) return;
		// retrieve patient data
		this.retrievePatient(req, avo);
		// set the email type flag
		this.processFlags(req, avo);
		// process the reassign/deassign
		try {
			this.processDeAssignFrom(req, avo);
			this.processReAssignTo(req, avo);
			msg.append("You have successfully processed the assignment. ");
		} catch (ActionException ae) {
			log.error("Error reassigning/deassigning assignment, ", ae);
			msg.append("Error: Unable to reassign this assignment. ");
		}
		this.processRedirect(req, avo);
	}
	
	/**
	 * Checks to see if this assignee has an active assignment to this patient already.
	 * @param avo
	 * @param msg
	 * @param isInsert
	 * @return
	 */
	private boolean checkForDuplicateAssignment(ActionRequest req, AssignmentVO avo) {
		log.debug("checking for duplicate assignment attempt");
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		if (! actionType.equalsIgnoreCase("reassign")) return false;
		
		boolean isDuplicate = false;
		String msg = null;
		// "reassign" assignee (the one being assigned this patient).
		// If no toAssigneeId, return true to skip reassign attempt
		String toAssigneeId = StringUtil.checkVal(req.getParameter("toAssigneeId"));
		if (toAssigneeId.length() == 0) return true;
		
		// check for duplicate assignment attempt
		StringBuffer sql = new StringBuffer();
		sql.append("select * from pt_assignment where assignee_id = ? and patient_id = ? ");
		sql.append("and assignment_status_id < ? ");
		log.debug("check for dupe assignments SQL: " + sql.toString());
		log.debug("assignee/patient: " + toAssigneeId + " / " + avo.getPatientId());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, toAssigneeId);
			ps.setString(2, avo.getPatientId());
			ps.setInt(3, SJMTrackerConstants.STATUS_COMPLETED);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				isDuplicate = true;
			}
		} catch (SQLException sqle) {
			log.error("Error checking for duplicate assignment, ", sqle);
			 isDuplicate = true;
			 msg = "Error: Unable to verify assignment.";
		}			

		if (isDuplicate) {
			msg = "Re-assignment disallowed. The previously selected 'Re-assign To' ambassador has an active assignment with this patient.";
	     	// Setup the redirect and return.
	    	StringBuffer url = new StringBuffer();
	    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
	    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
	    	url.append(page.getFullPath()).append("?actionType=reassign");
	    	url.append("&fromAssignmentId=").append(req.getParameter("fromAssignmentId"));
	    	url.append("&fromAssigneeId=").append(req.getParameter("fromAssigneeId"));
	    	url.append("&patientId=").append(req.getParameter("patientId"));
	    	url.append("&organizationId=").append(req.getParameter("organizationId"));
	    	url.append("&msg=").append(msg);
	    	log.debug("SJMAssignmentManager redirect URL: " + url);
		   	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
		return isDuplicate;
	}
	
	/**
	 * Retrieves patient data.
	 * @param req
	 * @param avo
	 */
	private void retrievePatient(ActionRequest req, AssignmentVO avo) {
		log.debug("retrieving patient data...");
		ActionInterface sai = new PatientManager(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.debug("Error retrieving patient data, ", ae);
		}
		
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
			if (tdc.getPatients() != null && ! tdc.getPatients().isEmpty()) {
				avo.setPatient(tdc.getPatients().get(0));
				log.debug("setting patientVO on assignmentVO, patientVO is: " + avo.getPatient());
			}
		}
	}
	
	/**
	 * Retrieves patient data.
	 * @param req
	 * @param avo
	 */
	private void retrieveAmbassador(ActionRequest req, AssignmentVO avo) {
		log.debug("retrieving ambassador data...");
		// assigneeId is already on the request
		ActionInterface sai = new AssigneeManager(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.debug("Error retrieving ambassador data, ", ae);
		}
		
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
			if (tdc.getAssignees() != null && ! tdc.getAssignees().isEmpty()) {
				avo.setAssignee(tdc.getAssignees().get(0));
			} else {
				log.debug("no ambassadors on container.");
			}
		} else {
			log.debug("mod is null...");
		}
	}
	
	/**
	 * Retrieves all data for the assignment based on the assignment ID passed in on 
	 * the request.
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unused")
	private AssignmentVO retrieveAssignmentData(ActionRequest req) {
		log.debug("retrieving full assignment data...");
		AssignmentVO avo = null;
		// set the assignment ID on the request if need be
		if (StringUtil.checkVal(req.getParameter("actionType")).equals("reassign")) {
			req.setParameter("assignmentId", req.getParameter("fromAssignmentId"));
		}
		ActionInterface sai = new SJMAssignmentManager(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving assignment data, ", ae);
		}
		
		TrackerDataContainer tdc = null;
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			tdc = (TrackerDataContainer) mod.getActionData();
			if (tdc.getAssignments() != null && ! tdc.getAssignments().isEmpty()) {
				avo = tdc.getAssignments().get(0);	
			}
		}
		// capture the current status from the request.
		if (avo != null) {
			avo.setCurrentStatusId(Convert.formatInteger(StringUtil.checkVal(req.getParameter("currentAssignmentStatusId"))));
		}
		return avo;
	}
	
	/**
	 * Processes status change and sets flags based on whether or not a status change has occurred.
	 * @param req
	 */
	private void processFlags(ActionRequest req, AssignmentVO avo) {
		log.debug("processing reassignment flags...");
		
		if (StringUtil.checkVal(req.getParameter("actionType")).equals("reassign")) {
			emailType = SJMTrackerConstants.RESPONSE_REASSIGNED;
		} else if (! avo.getCurrentStatusId().equals(avo.getAssignmentStatusId())) {
			// process certain changes in status
			switch(avo.getAssignmentStatusId()) {
				case 50: // request reassignment
					emailType = SJMTrackerConstants.STATUS_REQUEST_REASSIGN;
					break;
				case 60: // complaint
					emailType = SJMTrackerConstants.STATUS_COMPLAINT;
					// send email to admin
					break;
				case 900: // expired
					if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
						// this was a declined assignment
						emailType = SJMTrackerConstants.RESPONSE_DECLINED;
					}
					break;
				default:
					break;
			}
		} else if (Convert.formatBoolean(req.getParameter("productComplaint"))) {
			// if this is a product complaint, update the assignment status on the vo
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_COMPLAINT);
			emailType = SJMTrackerConstants.STATUS_COMPLAINT;
		}
		log.debug("reassignment emailType has been set to: " + emailType);
	}
	
	
	/**
	 * Processes the first of two steps that take place during a reassignment.  In this step,
	 * the assignment record is updated for this assignee before this assignment record is
	 * re-assigned to another assignee.  This 'de-assignment' update changes the record's
	 * status so that the de-assignment is logged.
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void processDeAssignFrom(ActionRequest req, AssignmentVO avo) 
		throws ActionException {
		log.debug("starting processDeAssignFrom...");
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		if (actionType.equals("reassign")) {
			this.processByReassignType(req, avo, "deassign");
		} else if (actionType.equals("adhoc")) {
			this.processByAdhocType(req, avo, "deassign");
		} else {
			// process based on the assignment status ID
			this.processByAssignmentStatus(req, avo, "deassign");
		}
		
		// de-assign the assignment from the original assignee.
		ActionInterface sai = new SJMAssignmentManager(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);			
		try {
			sai.build(req);
		} catch (ActionException ae) {
			log.error("Error processing 'deassign from' initiated by admin, ", ae);
			throw new ActionException(ae.getMessage());
		}
		
		//send email notification
		log.debug("sending 'deassign from' email notification");
		TrackerDataContainer tdc = new TrackerDataContainer();
		tdc.addAssignment(avo);
		tdc.addPatient(avo.getPatient());
		tdc.addAssignee(avo.getAssignee());
		try {
			this.sendEmail(req, emailType, tdc);
		} catch (MailException me) {
			log.debug("Error sending 'deassign from' email, ", me);
		}
	}
	
	/**
	 * Second half of a reassignment:  Re-assigns the patient to the appropriate admin 
	 * or 'replacement' ambassador.  The same assignment ID is kept but the assignee ID is
	 * updated reflecting the newly assigned assignee.
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void processReAssignTo(ActionRequest req, AssignmentVO avo) throws ActionException {
		log.debug("starting processReAssignTo...");
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		// set additional parameters
		if (actionType.equals("reassign")) {
			this.processByReassignType(req, avo, "reassign");
		} else if (actionType.equals("adhoc")) {
			this.processByAdhocType(req, avo, "reassign");
		} else {
			// process based on assignment status ID
			this.processByAssignmentStatus(req, avo, "reassign");
		}
		// create the new assignment
		ActionInterface sai = new SJMAssignmentManager(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		try {
			sai.build(req);
		} catch (ActionException ae) {
			log.error("Error processing 'assign to', ", ae);
			throw new ActionException(ae.getMessage());
		}
		
		List<PatientInteractionVO> interactions = null;
		if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
			// if this is a request for reassignment, get the interaction summary
			interactions = this.retrievePatientInteractions(req, avo);
		}
		
		// send an email notification for certain status
		if (emailType.equals(SJMTrackerConstants.STATUS_COMPLAINT) ||
				emailType.equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
			log.debug("sending 'assign to' email notification");
			TrackerDataContainer tdc = new TrackerDataContainer();
			tdc.addAssignment(avo);
			tdc.addPatient(avo.getPatient());
			tdc.addAssignee(avo.getAssignee());
			if (interactions != null) tdc.setInteractions(interactions);
			try {
				this.sendEmail(req, emailType, tdc);
			} catch (MailException me) {
				log.debug("Error sending 'assign to' email, ", me);
			}
		}
	}
	
	/**
	 * Sets request parameters for deassign/reassign operation for 'reassign' action type
	 * @param req
	 * @param avo
	 * @param toFrom
	 */
	private void processByReassignType(ActionRequest req, AssignmentVO avo, String toFrom) {
		if (toFrom.equalsIgnoreCase("deassign")) {
			log.debug("deassigning from 'reassign' action type");
			// set original assignment parameters on the request for the update, patientId is already on request
			// get the 'from' ambassador data and set it on the assignment VO
			req.setParameter("assigneeId", req.getParameter("fromAssigneeId"), true);
			this.retrieveAmbassador(req, avo);
			req.setParameter("assignmentId", req.getParameter("fromAssignmentId"), true);
			req.setParameter("assigneeProfileId", req.getParameter("fromAssigneeProfileId"), true);
			log.debug("assignmentId/assigneeId: " + req.getParameter("assignmentId") + "/" + req.getParameter("assigneeId"));
			Integer fromAssignmentStatus = Convert.formatInteger(req.getParameter("fromAssignmentStatusId"));
			log.debug("fromAssignmentStatusId: " + fromAssignmentStatus);
			if (fromAssignmentStatus.equals(SJMTrackerConstants.STATUS_PENDING)) {
				// suppress the 'reassignment' email if admin was originally assigned this assignment and is re-assigning it to someone else.
				if (avo.getAssignee().getTypeId() >= AssigneeManager.MIN_ADMIN_TYPE_ID) emailType = 0;
			}
			req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_REASSIGNED.toString(), true);
			req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_REASSIGNED.toString(), true);
			
			// set logging parameters
			req.setParameter(AssignmentLogManager.LOG_STATUS_ID, req.getParameter("assignmentStatusId")); // status
			req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, req.getParameter("assignmentResponseId")); // response
			req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, "Admin is reassigning this patient to another ambassador."); // notes
			
		} else if (toFrom.equalsIgnoreCase("reassign")) {
			log.debug("reassigning from 'reassign' action type");
			log.debug("assignmentId/assigneeId: " + req.getParameter("assignmentId") + "/" + req.getParameter("assigneeId"));
			// this is from the 'reassign' view, set assigneeId; patientId is already on the request
			req.setParameter("assigneeId", req.getParameter("toAssigneeId"), true);
			req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_PENDING.toString(), true);
			req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED.toString(), true);
			req.setParameter("resetAssignDate", "true", true);
			// get the 'to' ambassador data and set it on the assignment VO
			this.retrieveAmbassador(req, avo);
			log.debug("reassigning to specified assigneeId: " + req.getParameter("toAssigneeId"));
			// reset formSubmittalId so that logging takes place properly
			String tmp = null;
			req.setParameter("formSubmittalId", tmp, true);
			// set logging parameters
			req.setParameter(AssignmentLogManager.LOG_STATUS_ID, req.getParameter("assignmentStatusId")); // status
			req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, req.getParameter("assignmentResponseId")); // response
			StringBuffer toText = new StringBuffer();
			toText.append("Admin has reassigned this patient to ");
			toText.append(avo.getAssignee().getFirstName()).append(" ").append(avo.getAssignee().getLastName().substring(0,1)).append(".");
			req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, toText.toString()); // notes
		}
	}
	
	/**
	 * Processes 'adhoc' reassignments
	 * @param req
	 * @param avo
	 * @param type
	 */
	private void processByAdhocType (ActionRequest req, AssignmentVO avo, String type) {
		log.debug("processing by 'adhoc' actionType, type is: " + type);
		log.debug("assignmentId: " + req.getParameter("assignmentId"));
		log.debug("assigneeId: " + req.getParameter("assigneeId"));
		log.debug("patientId: " + req.getParameter("patientId"));
		if (type.equals("deassign")) {
			// set assignment to 910/180
			avo.setCurrentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_REASSIGNED);
			avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			// retrieve ambassador
			this.retrieveAmbassador(req, avo);
			// suppress email
			emailType = 0;
		} else if (type.equals("reassign")) {
			// set assignment to 60/180, assigned to admin
			log.debug("updating assignment to 'complaint'");
			avo.setCurrentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_COMPLAINT);
			avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			
			Integer ambType = AssigneeManager.MIN_ADMIN_TYPE_ID;
			// reset formSubmittalId so that logging takes place properly
			String tmp = null;
			req.setParameter("formSubmittalId", tmp, true);
			
			// retrieve ambassador match
			List<AssigneeVO> todaysAmbs = this.retrieveTodaysAmbassadors(req, ambType);
			AssigneeVO ambMatch = this.retrieveAmbassadorMatch(todaysAmbs, avo.getPatient(), avo.getAssigneeId());
			// set the matched amb ID on the request
			req.setParameter("assigneeId", ambMatch.getAssigneeId(), true);
			avo.setAssignee(ambMatch);
			emailType = SJMTrackerConstants.STATUS_COMPLAINT;
		}
		req.setParameter("currentAssignmentStatusId", avo.getCurrentStatusId().toString(), true);
		req.setParameter("assignmentStatusId", avo.getAssignmentStatusId().toString(), true);
		req.setParameter("assignmentResponseId", avo.getAssignmentResponseId().toString(), true);
	}
	
	/**
	 * Sets request parameters for assignment and logging based on the assignment status ID.
	 * @param req
	 * @param avo
	 * @param toFrom
	 */
	private void processByAssignmentStatus (ActionRequest req, AssignmentVO avo, String toFrom) {
		log.debug(toFrom + " processing by assignment status...");
		if (toFrom.equalsIgnoreCase("deassign")) { // deassign from
			// replace params with original values so it can be updated properly.
			req.setParameter("assignmentId", avo.getAssignmentId(), true);
			req.setParameter("assigneeId", avo.getAssigneeId(), true);
			// get the currently assigned ambassador's data and set it on the assignment VO
			this.retrieveAmbassador(req, avo);
			//set status to 'reassigned' for all cases
			req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_REASSIGNED.toString(), true);
			
			if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT)) { // complaint
				log.debug("deassign complaint");
				emailType = SJMTrackerConstants.RESPONSE_REASSIGNED;
				req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_COMPLAINT.toString(), true);
	
				// assignment logging parameters
				req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, "Patient is being reassigned to admin due to product complaint.");
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, SJMTrackerConstants.RESPONSE_REASSIGNED.toString()); // response
				
			} else if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
				log.debug("deassign request_reassign");
				emailType = SJMTrackerConstants.RESPONSE_REASSIGNED;
				req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_REASSIGNED.toString(), true);
	
				// assignment logging parameters
				req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, "Ambassador has requested the reassignment of this patient to a different ambassador."); // notes
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, SJMTrackerConstants.RESPONSE_REASSIGNED.toString()); // response
				
			} else if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_EXPIRED)) {
				log.debug("deassign expired");
				emailType = avo.getAssignmentResponseId();
				req.setParameter("assignmentResponseId", avo.getAssignmentResponseId().toString(), true);
				StringBuffer sysText = new StringBuffer();
				if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
					sysText.append("Assignment declined.  Reassigning patient to another ambassador.");
				} else if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
					sysText.append("Assignment has expired (48 hours time limit expiration).");
				} else if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_NONE_96)) {
					sysText.append("Assignment has expired (96 hours time limit expiration).");
				} else {
					sysText.append("Assignment has expired.");
				}
				// assignment logging parameters
				req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_REASSIGNED.toString()); // status
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, avo.getAssignmentResponseId().toString()); // response
				req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, sysText.toString()); // notes
			}
		} else if (toFrom.equalsIgnoreCase("reassign")) { // reassign to
			// reassignments use the same assignment ID but update the assignee ID to ensure
			// that any assignment history is carried forward with the reassignment			
			List<AssigneeVO> todaysAmbs = null;
			Integer ambType = 0;
			String ambToExclude = null;
			// Make sure the original assignment ID is on the request so that an update is performed.
			req.setParameter("assignmentId", avo.getAssignmentId(), true);
			StringBuffer toText = new StringBuffer();
			// set params based on assignment status
			if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
				log.debug("reassignment of complaint");
				emailType = SJMTrackerConstants.STATUS_COMPLAINT;
				req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_COMPLAINT.toString(), true);
				req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_REASSIGNED.toString(), true);
				// make sure only an admin is chosen to re-assign this complaint to
				ambType = AssigneeManager.MIN_ADMIN_TYPE_ID;
				// reset formSubmittalId so that logging takes place properly
				String tmp = null;
				req.setParameter("formSubmittalId", tmp, true);
				// logging params for the update
				toText.append("Patient reassigned to admin ");
				req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_COMPLAINT.toString(), true); // status
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, SJMTrackerConstants.RESPONSE_REASSIGNED.toString(), true); // response
			} else if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
				log.debug("reassign request_reassign");
				// make sure we exclude the currently assigned ambassador so not chosen again
				ambToExclude = avo.getAssigneeId();
				emailType = SJMTrackerConstants.STATUS_REQUEST_REASSIGN;
				// reset formSubmittalId so that logging takes place properly
				String tmp = null;
				req.setParameter("formSubmittalId", tmp, true);
				// logging params for the update
				toText.append("Patient reassigned to ambassador ");
				req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_PENDING.toString(), true);
				req.setParameter("assignmentResponseId", SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED.toString(), true);
				req.setParameter("resetAssignDate", "true", true);
				// logging params for the update
				req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_PENDING.toString(), true); // status
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, SJMTrackerConstants.RESPONSE_DEFAULT.toString(), true); // response
			} else if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_EXPIRED)) {
				log.debug("re-assignment of an expired assignment");
				// make sure we exclude the currently assigned ambassador so not chosen again
				ambToExclude = avo.getAssigneeId();
				// reset formSubmittalId so that logging takes place properly
				String tmp = null;
				req.setParameter("formSubmittalId", tmp, true);
				toText.append("Patient reassigned to ambassador ");
				req.setParameter("currentAssignmentStatusId", "0", true);
				req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_PENDING.toString(), true);
				// set default response ID for this scenario
				Integer responseId = SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED;
				if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
					// since this was a 'declined' assignment, set the response ID to 'reassigned'
					responseId = SJMTrackerConstants.RESPONSE_REASSIGNED;
				}
				req.setParameter("assignmentResponseId", responseId.toString(), true);
				// logging params for the update
				req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_PENDING.toString(), true); // status
				req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, responseId.toString(), true); // response
			}
			
			// retrieve ambassador match
			todaysAmbs = this.retrieveTodaysAmbassadors(req, ambType);
			AssigneeVO ambMatch = this.retrieveAmbassadorMatch(todaysAmbs, avo.getPatient(), ambToExclude);
			// set the matched amb ID on the request
			req.setParameter("assigneeId", ambMatch.getAssigneeId(), true);
			
			toText.append(ambMatch.getFirstName()).append(" ").append(ambMatch.getLastName().substring(0, 1)).append(".");
			req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, toText.toString()); // notes
		}
	}
	
	/**
	 * Retrieves the patient interaction for the assignment that has been set to a complaint
	 * status.  The interactionId value is on the request so only the interaction that set
	 * the complaint status is retrieved.
	 * @param req
	 * @param tdc
	 */
	private List<PatientInteractionVO> retrievePatientInteractions(ActionRequest req, AssignmentVO avo) {
		log.debug("retrieving patient interaction data for request_reassignment email...");
		List<PatientInteractionVO> livo = null;
		// make sure there aren't form submittal ids on the request
		String[] formSubmittalIds = req.getParameterValues("formSubmittalId");
		req.setParameter("formSubmittalId", null);
		
		// preserve assignee ID and assignment ID values that were put on the request
		String reqAssigneeId = req.getParameter("assigneeId");
		String reqAssignmentId = req.getParameter("assignmentId");
		
		log.debug("reqAssignmentId: " + reqAssignmentId);
		log.debug("reqAssigneeId: " + reqAssigneeId);
		req.setParameter("assigneeId", avo.getAssigneeId(), true);
		req.setParameter("assignmentId", avo.getAssignmentId(), true);
		log.debug("assignmentId/assigneeId/patientId on req: " + req.getParameter("assignmentId") + "/" + req.getParameter("assigneeId") + "/" + req.getParameter("patientId"));
		try {
			ActionInterface sai = new PatientInteractionManager(this.actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving patient interaction data for email notification, ", ae);
		}
		
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			TrackerDataContainer t = (TrackerDataContainer) mod.getActionData();
			livo = t.getInteractions();
		}
		
		// put the form submittal IDs back on the request
		if (formSubmittalIds != null) {
			req.setParameter("formSubmittalId", formSubmittalIds, true);
		}
		// reset assignee/assignment
		req.setParameter("assigneeId", reqAssigneeId, true);
		req.setParameter("assignmentId", reqAssignmentId, true);
		return livo;
	}
	
	/**
	 * Builds redirect
	 * @param req
	 * @param avo
	 */
	private void processRedirect(ActionRequest req, AssignmentVO avo) {
		log.debug("building redirect...");
		// redirect back to the original assignment
    	StringBuffer url = new StringBuffer();
    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
    	url.append(page.getFullPath());
    	if (msg != null) url.append("?msg=").append(msg);
    	log.debug("SJMReassignmentManager redirect URL: " + url);
    	
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());		
	}
	
	/**
	 * Retrieves today's available ambassadors based on the actionType passed in.
	 * @param req
	 * @param actionType
	 * @return
	 */
	private List<AssigneeVO> retrieveTodaysAmbassadors(ActionRequest req, Integer ambType) {
		log.debug("retrieving today's available ambassadors...");
		List<AssigneeVO> ambs = null;
		log.debug("retrieving today's ambassadors");
		// retrieve today's available ambassadors
		AmbassadorRetriever ar = new AmbassadorRetriever();
		ar.setAttributes(attributes);
		ar.setDbConn(dbConn);
		ar.setCheckAssignmentLimit(true);
		ar.setAmbassadorType(ambType);
		try {
			ambs = ar.retrieveAmbassadors();
		} catch (SQLException sqle) {
			log.error("Error retrieving today's ambassadors, ", sqle);
		}
		return ambs;
	}
	
	/**
	 * Matches an ambassador to the patient passed in.
	 * @param ambs
	 * @param pvo
	 * @param ambToExclude
	 * @return
	 */
	private AssigneeVO retrieveAmbassadorMatch(List<AssigneeVO> ambs, PatientVO pvo, String ambToExclude) {
		log.debug("finding ambassador to reassign to patient...");
		AmbassadorMatcher am = new AmbassadorMatcher();
		am.setDbConn(dbConn);
		am.setPatient(pvo);
		am.setAmbassadors(ambs);
		if (ambToExclude != null) am.addToExcludeList(ambToExclude);
		AssigneeVO match = am.findAmbassadorMatch();
		return match;
	}
		
	/**
	 * Formats and sends email to recipients based on change in assignment status
	 * @param req
	 * @param emailType
	 * @param assignment
	 * @throws MailException
	 */
	private void sendEmail(ActionRequest req, Integer emailType, TrackerDataContainer tdc) throws MailException {
		if (tdc.getAssignments() == null || tdc.getAssignments().isEmpty()) return;
		if (emailType == 0) return;
		log.debug("setting TrackerMailFormatter...");
		TrackerMailFormatter mf = new TrackerMailFormatter(tdc);
		mf.setAttributes(attributes);
		mf.setType(emailType);
		mf.setRecipients(this.setRecipients(tdc, emailType));
		mf.sendEmail();
	}
	
	/**
	 * Determines email recipients based on the emailType
	 * @param emailType
	 * @param tdc
	 * @return
	 */
	private String[] setRecipients(TrackerDataContainer tdc, Integer emailType) {
		List<String> recpts = new ArrayList<String>();
		if (emailType.equals(SJMTrackerConstants.STATUS_COMPLAINT) ||
				emailType.equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
			// mail to the appropriate admin
			recpts.add(this.retrieveAmbassadorAdminEmail(tdc.getAssignees().get(0)));
		} else {
			recpts.add(tdc.getAssignees().get(0).getEmailAddress());
		}
		/* DEBUG */
		//recpts.add(this.retrieveAmbassadorAdminEmail(tdc.getAssignees().get(0)));
		return recpts.toArray(new String[0]);
	}
	
	/**
	 * Performs look-up to retrieve admin email associated with this ambassador
	 * @param avo
	 * @return
	 */
	private String retrieveAmbassadorAdminEmail(AssigneeVO avo) {
		// TODO future: will look-up ambassador's associated admin
		// and return the appropriate email.  As of 11/28/2011, we
		// return a specified admin email address
		return SJMTrackerConstants.AMB_ADMIN_EMAIL_ADDRESS;
	}

}
