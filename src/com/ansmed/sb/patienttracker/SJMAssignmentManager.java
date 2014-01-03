package com.ansmed.sb.patienttracker;

// JDK 1.6 libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// SMB Baselibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.tracker.AssignmentManager;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.AssigneeAction;
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.PatientAction;
import com.smt.sitebuilder.action.tracker.PatientInteractionManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

// SB_ANS_MEDICAL libs
import com.ansmed.sb.patienttracker.comparator.AssignmentActionsComparator;

/****************************************************************************
* <b>Title</b>SJMAssigmentManager.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Aug 08, 2011
* <b>Changes: </b>
* Feb 16, 2012: DBargerhuff; refactored to remove 'reassignment' processing from this class
* Oct 12, 2012: DBargerhuff; refactored how patient source data is retrieved in support of Phase 3 updates.
****************************************************************************/
public class SJMAssignmentManager extends TrackerAction {
	
	private Integer emailType = new Integer(0);

	public SJMAssignmentManager() {
		super();
	}

	public SJMAssignmentManager(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssignmentManager retrieve...");
		
		SMTActionInterface sai = null;
		// retrieve assignments
		sai = new AssignmentManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer)mod.getActionData();
		// retrieve today's ambassadors is necessary
		this.retrieveTodaysAmbassadors(req, tdc);
		//retrieve additional source data if necessary
		this.retrieveAdditionalData(req, tdc);
		// sort assignments by custom sort field
		this.doCustomSort(req, tdc);
		
		// set the action data
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssignmentManager build...");
		AssignmentVO avo = new AssignmentVO();
		avo.setData(req);
		log.debug("assigneeId / patientId: " + avo.getAssigneeId() + " / " + avo.getPatientId());
		boolean isCreateNewAssignment = Convert.formatBoolean(req.getParameter("createNewAssignment"));
		boolean isPatientCreated = false;
		boolean isDuplicate = false;
		// check for 'createNewAssignment' flag and create patient if true
		if (isCreateNewAssignment) {
			PatientVO newPatient = this.createPatient(req);
			if (newPatient.getPatientId() != null) { 
				isPatientCreated = true;
				avo.setPatient(newPatient);
				if (StringUtil.checkVal(avo.getAssigneeId()).length() == 0) {
					// assignee from request was null or 'Unknown' so let's get one
					this.retrieveAssigneeMatch(req, avo);
				}
				if (StringUtil.checkVal(avo.getAssigneeId()).length() > 0) {
					// if we got an assignee, process the assignment
					this.processAssignment(req, avo, isCreateNewAssignment);
				}
			}
		} else {
			// check for duplicate assignment attempt (inserts only)
			isDuplicate = this.checkForDuplicateAssignment(req, avo);
			if (! isDuplicate) processAssignment(req, avo, isCreateNewAssignment);
		}
				
		// build the redirect
		this.processRedirect(req, avo, isCreateNewAssignment, isPatientCreated, isDuplicate);		
	}
	
	/**
	 * Processes standard assignment insert/update.
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void processAssignment(SMTServletRequest req, AssignmentVO avo,	boolean isCreateNewAssignment) 
			throws ActionException {
		log.debug("processing assignment...");
		
		// set email flags and/or request parameters before assignment insert/update
		this.processEmailFlags(req, avo);
		
		// capture assignment logging request parameters
		this.processLoggingParameters(req, avo);
				
		// insert/update the assignment
		SMTActionInterface sai = null;
		sai = new AssignmentManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		
		// update ambassador's assignment count if necessary
		this.updateCurrentAssignmentCount(req, avo);
		
		// notify appropriate recipients
		try {
			this.processNotifications(req, isCreateNewAssignment);
		} catch (ActionException ae) {
			log.error("Error retrieving patient/ambassador data for email notifications, ", ae);
		} catch (MailException me) {
			log.error("Error sending email notification for assignment status " + emailType, me);
		}
	}
	
	/**
	 * Builds standard redirect
	 * @param req
	 * @param avo
	 */
	private void processRedirect(SMTServletRequest req, AssignmentVO avo, 
			boolean isCreateNewAssignment, boolean isPatientCreated, boolean isDuplicate) {
		// set up the redirect
		StringBuffer url = new StringBuffer();
		PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		url.append(StringUtil.checkVal(attributes.get("contextPath")));
		url.append(page.getFullPath());
		
		if (isCreateNewAssignment) {
			url.append("?actionType=assignment");
			url.append("&organizationId=").append(req.getParameter("organizationId"));
			url.append("&msg=").append(msg);
			// append msg from assignment creation
			url.append(req.getAttribute(TRACKER_BUILD_MSG));
			
		} else if (isDuplicate) {
	     	// Setup the 'isDuplicate' redirect.
	    	url.append("?actionType=assignment");
	    	if (avo.getAssignmentId() != null) {
	    		url.append("&assignmentId=").append(avo.getAssignmentId());
	    	}
	    	url.append("&organizationId=").append(req.getParameter("organizationId"));
	    	url.append("&msg=");
	    	url.append("Assignment disallowed.  This ambassador already has an active assignment with this patient.");
	
		} else {
			// if assignment build op came from an 'interaction' page, modify the redirect
			if (StringUtil.checkVal(req.getParameter("fromType")).equalsIgnoreCase("interaction")) {
				// check the assignment status/response to determine where to redirect
				if (avo.getAssignmentStatusId() < 100 
						&& avo.getAssignmentStatusId() != 50
						&& avo.getAssignmentStatusId() != 60) {
					// was and update, redirect back to composite 'interaction' view
			    	url.append("?actionType=interaction");
			    	url.append("&assignmentId=").append(avo.getAssignmentId());
					url.append("&organizationId=").append(req.getParameter("organizationId"));
				} else {
					// was an insert or reassignment or expiration, redirect to dashboard assignment view
					if (StringUtil.checkVal(req.getParameter("subType")).length() > 0) {
						url.append("?subType=").append(req.getParameter("subType"));
						url.append("&organizationId=").append(req.getParameter("organizationId"));
					}
				}
			} else {
				// if in 'assignment' view and not 'currently closed' and has not been changed to 'closed'
				if (! avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_CLOSED) &&
					! avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_CLOSED)) {
					// redirect back to the assignment.
					url.append("?actionType=assignment");
					url.append("&organizationId=").append(req.getParameter("organizationId"));
					if (req.getParameter("logs") != null) {
						url.append("&logs=true");
					}
				} else {
					url.append("?organizationId=").append(req.getParameter("organizationId"));
				}
			}
			url.append("&msg=").append(parseRedirectMessage(req));
		}
		
		log.debug("SJMAssignmentManager redirect URL: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	/**
	 * Checks to see if this assignee has an active assignment to this patient already.
	 * @param avo
	 * @param msg
	 * @param isInsert
	 * @return
	 */
	private boolean checkForDuplicateAssignment(SMTServletRequest req, AssignmentVO avo) {
		log.debug("checking for duplicate assignment attempt");
		boolean isDuplicate = false;
		// only check for duplicate attempt if this is an insert.
		if (avo.getAssignmentId() == null) {
			StringBuffer sql = new StringBuffer();
			sql.append("select * from pt_assignment where assignee_id = ? and patient_id = ? ");
			sql.append("and assignment_status_id < ? ");
			log.debug("check for dupe assignments SQL: " + sql.toString());
			log.debug("assignee/patient: " + avo.getAssigneeId() + " / " + avo.getPatientId());
			
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, avo.getAssigneeId());
				ps.setString(2, avo.getPatientId());
				ps.setInt(3, SJMTrackerConstants.STATUS_COMPLETED);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					log.debug("found duplicate assignment: " + rs.getString("pt_assignment_id"));
					isDuplicate = true;
				}
			} catch (SQLException sqle) {
				log.error("Error checking for duplicate assignment, ", sqle);
				 isDuplicate = true;
			}			
		}
		return isDuplicate;
	}
	
	/**
	 * Creates a new patient.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private PatientVO createPatient (SMTServletRequest req) throws ActionException {
		log.debug("creating new patient...");
		SMTActionInterface sai = new SJMPatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		PatientVO newPatient = (PatientVO) req.getAttribute(PatientAction.PATIENT_DATA);
		req.setParameter("patientId", newPatient.getPatientId());
		req.setParameter("formSubmittalId", "", true);
		msg.append(StringUtil.checkVal(req.getAttribute(TRACKER_BUILD_MSG)));
		return newPatient;
	}
		
	/**
	 * Retrieves ambassadors and matches one with the patient.
	 * @param avo
	 */
	private void retrieveAssigneeMatch(SMTServletRequest req, AssignmentVO avo) {
		log.debug("retrieving assignee match...");
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setDbConn(dbConn);
		sar.setAttributes(attributes);
		sar.setIgnoreDailyAvailability(true);
		List<AssigneeVO> ambs = null;
		try {
			ambs = sar.retrieveAmbassadors();
		} catch (SQLException sqle) {
			log.error("Error retrieving ambassadors, ", sqle);
			msg.append("Unable to retrieve ambassador match. ");
			return;
		}
		
		AmbassadorMatcher am = new AmbassadorMatcher();
		am.setDbConn(dbConn);
		am.setAmbassadors(ambs);
		am.setPatient(avo.getPatient());
		AssigneeVO asvo = am.findAmbassadorMatch();
		if (asvo != null) {
			avo.setAssigneeId(asvo.getAssigneeId());
			req.setParameter("assigneeId", asvo.getAssigneeId());
		} else {
			msg.append("Unable to retrieve ambassador match. ");
		}
	}
	
	/**
	 * Processes status change and sets flags based on whether or not a status change has occurred.
	 * @param req
	 */
	private void processEmailFlags(SMTServletRequest req, AssignmentVO avo) {
		log.debug("processing flags...");
		if (avo.getAssignmentId() != null) {  // is an UPDATE
			// process certain status changes
			log.debug("getCurrentStatusId/getAssignmentStatusId: " + avo.getCurrentStatusId() + "/" + avo.getAssignmentStatusId());
			if (! avo.getCurrentStatusId().equals(avo.getAssignmentStatusId())) { // STATUS changed, evaluate
				switch(avo.getAssignmentStatusId()) {
					case 10: // set to 'PENDING' upon reassignment update
						if (avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_NONE_48) || 
							avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_NONE_96) || 
							avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED) ||
							avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_REASSIGNED)) {
							emailType = SJMTrackerConstants.STATUS_PENDING;
						}
						break;
					case 30: // set to 'IN PROGRESS'
						// we only set email flag/accept date if current status is not specified or is 'pending'
						if (avo.getCurrentStatusId() == 0 || 
								avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_PENDING)) {
							// PHASE 3 item #3001: removal: emailType = SJMTrackerConstants.STATUS_ACCEPTED;
							// make sure that the accept date is set on the request and the vo
							if (avo.getAcceptDate() == null) {
								Date d = new Date();
								req.setParameter("acceptDate", Convert.formatDate(d,Convert.DATE_SLASH_PATTERN));
								avo.setAcceptDate(d);
							}
						} else if (avo.getCurrentStatusId() == 40) {
							// assignment was reset from 'request assistance' to 'in progress'
							emailType = SJMTrackerConstants.EMAIL_TYPE_STATUS_IN_PROGRESS_AFTER_ASSIST;
						}
						break;
					case 40: // set to 'REQUEST ASSISTANCE'
						emailType = SJMTrackerConstants.STATUS_REQUEST_ASSIST;
						break;
					case 100: // set to 'COMPLETED'
						emailType = SJMTrackerConstants.STATUS_COMPLETED;
						break;
					default:
						break;
				}
			}
		} else { // is an INSERT or a COMPLAINT status change
			if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
				// suppress the 'new assignment' email to the admin.
				emailType = 0;
			} else {
				if (! StringUtil.checkVal(req.getParameter("actionType")).equalsIgnoreCase("adhoc")) {
					// 	this is a standard insert so we need to notify the assignee, log the status
					emailType = SJMTrackerConstants.STATUS_PENDING;
				}
			}
		}
		log.debug("emailType has been set to: " + emailType);
	}
	
	/**
	 * Sets assignment logging parameters to be used downstream by the assignment logger.
	 * @param req
	 * @param avo
	 */
	private void processLoggingParameters(SMTServletRequest req, AssignmentVO avo) {
		boolean insert = false;
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		if (avo.getAssignmentId() == null) insert = true;
		StringEncoder se = new StringEncoder();
		
		if (insert) { // INSERT
			List<AssigneeVO> aData = this.lookupAssignee(req, avo);
			req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_PENDING.toString());
			req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, avo.getAssignmentResponseId().toString());
			if (StringUtil.checkVal(req.getParameter(AssignmentLogManager.LOG_SYSTEM_TEXT)).length() == 0) {
				if (aData != null && ! aData.isEmpty()) {
					StringBuffer toText = new StringBuffer();
					if (actionType.equalsIgnoreCase("adhoc")) { // PHASE 3 #3007
						toText = new StringBuffer("Adhoc assignment created by ");
					} else {
						toText = new StringBuffer("New assignment created, patient assigned to ");
					}
					toText.append(aData.get(0).getFirstName()).append(" ").append(aData.get(0).getLastName().substring(0,1)).append(".");
					req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, toText.toString());
				} else {
					req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, "New assignment created.");					
				}
			}
			if (StringUtil.checkVal(avo.getAssignmentNotes()).length() > 0) {
				req.setParameter(AssignmentLogManager.LOG_ASSIGN_TEXT, se.decodeValue(avo.getAssignmentNotes()));
			}
			
		} else { // UPDATE
			// log status change for all but certain status IDs under certain conditions
			if (! avo.getAssignmentStatusId().equals(avo.getCurrentStatusId())) {
				log.debug("status change: curr/new: " + avo.getCurrentStatusId() + "/" + avo.getAssignmentStatusId());
				if (actionType.equalsIgnoreCase("adhoc")) {
					List<AssigneeVO> aData = this.lookupAssignee(req, avo);
					req.setParameter(AssignmentLogManager.LOG_STATUS_ID, avo.getAssignmentStatusId().toString(), true);
					req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, avo.getAssignmentResponseId().toString());
					StringBuffer toText = new StringBuffer("Adhoc assignment entry for ");
					toText.append(aData.get(0).getFirstName()).append(" ").append(aData.get(0).getLastName().substring(0,1)).append(".");
					req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, toText.toString(), true);
				} else if (! avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT) ||
						! avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN) ||
						! avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_EXPIRED)) {
					req.setParameter(AssignmentLogManager.LOG_STATUS_ID, avo.getAssignmentStatusId().toString(), true);					
				} else if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
					// if this is an assignment owned by an admin and the admin has updated the assignment
					// status to 'complaint', log the status change
					if (Convert.formatBoolean(req.getAttribute("isAdminOwnedAssignment"))) {
						req.setParameter(AssignmentLogManager.LOG_STATUS_ID, avo.getAssignmentStatusId().toString(), true);
					}
				}
			}
		}
		
		// set other logging params based on certain actionTypes			
		if (actionType.equals("assignment")) { // assignment updates from an 'assignment' view
			// log assignment notes change
			if (StringUtil.checkVal(avo.getAssignmentNotes()).length() > 0) {
				req.setParameter(AssignmentLogManager.LOG_ASSIGN_TEXT, se.decodeValue(avo.getAssignmentNotes()));
			}
		} else if (actionType.equals("interaction")) { // assignment updates as a result of interaction view
			// log notify date/notes or assignment notes change if this came from an interaction view
			boolean setNotifyDate = Convert.formatBoolean(req.getParameter("setNotifyDate"));
			boolean resetNotifyDate = Convert.formatBoolean(req.getParameter("resetNotifyDate"));
			String assignNotesText = StringUtil.checkVal(req.getParameter("assignmentNotes")).trim();
			String notifyNotesText = StringUtil.checkVal(req.getParameter("notifyNotes")).trim();
			if (setNotifyDate) {
				if (StringUtil.checkVal(req.getParameter("logNotifyDateVal")).equalsIgnoreCase("true")) {
					log.debug("logging notify date change");
					req.setParameter(AssignmentLogManager.LOG_NOTIFY_DATE, req.getParameter("notifyDate"), true);
				}
				if (StringUtil.checkVal(req.getParameter("logNotifyNotes")).equalsIgnoreCase("true")) {
					log.debug("logging notify notes text");
					if (notifyNotesText.length() > 0) {
						// decode the notify notes value before putting it on the req for logging
						req.setParameter(AssignmentLogManager.LOG_NOTIFY_TEXT, se.decodeValue(notifyNotesText));
					} else {
						req.setParameter(AssignmentLogManager.LOG_NOTIFY_TEXT, SJMTrackerConstants.NOTIFY_DATE_RESET_BY_USER);
					}
				}				
			} else if (resetNotifyDate) {
				log.debug("resetting notify date field and notes field");
				req.setParameter(AssignmentLogManager.LOG_NOTIFY_DATE, SJMTrackerConstants.NOTIFY_DATE_RESET_BY_USER, true);
				//req.setParameter(notesField, "", true);
			} else if (assignNotesText.length() > 0) {
				log.debug("logging assignment notes field");
				// decode the notes value before putting it on the req for logging
				req.setParameter(AssignmentLogManager.LOG_ASSIGN_TEXT, se.decodeValue(assignNotesText));
			}
		}
	}
	
	/**
	 * Retrieves the assignee data for the assignee ID passed in on the request.  For use only with an insert so
	 * we can log the assignee's name.
	 * @param req
	 * @param avo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<AssigneeVO> lookupAssignee(SMTServletRequest req, AssignmentVO avo) {
		log.debug("retrieving assignee data for assignment insert...");
		SMTActionInterface sai = new AssigneeAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving assignee data, ", ae);
		}
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		List<AssigneeVO> asvo = null;
		if (mod != null) {
			asvo = (List<AssigneeVO>)mod.getActionData();
		} else {
			asvo = new ArrayList<AssigneeVO>();
		}
		return asvo;
	}
	
	/**
	 * Retrieves today's available ambassadors based on the actionType passed in.
	 * @param req
	 * @param actionType
	 * @return
	 */
	private void retrieveTodaysAmbassadors(SMTServletRequest req, TrackerDataContainer tdc) {
		log.debug("checking to see if need to retrieve today's ambassadors");
		boolean getAmbs = false;
		boolean checkLimit = true;
		boolean ignoreToday = false;
		if (StringUtil.checkVal(req.getParameter("actionType")).equals("assignment")) {
			if (StringUtil.checkVal(req.getParameter("assignmentId")).length() == 0) {
				getAmbs = true;
				// if admin is creating a new assignment, set additional flags
				if (Convert.formatBoolean(req.getParameter("addAssignment"))) {
					log.debug("is 'addAssignment' op...");
					checkLimit = false;
					ignoreToday = true;
				}
			}
		} else if (StringUtil.checkVal(req.getParameter("actionType")).equals("reassign")) {
			getAmbs = true;
			checkLimit = false;
			ignoreToday = true;
		}
		
		List<AssigneeVO> ambs = null;
		if (getAmbs) {
			log.debug("retrieving today's ambassadors");
			// retrieve today's available ambassadors
			AmbassadorRetriever ar = new AmbassadorRetriever();
			ar.setAttributes(attributes);
			ar.setDbConn(dbConn);
			ar.setCheckAssignmentLimit(checkLimit);
			ar.setIgnoreDailyAvailability(ignoreToday);
			try {
				ambs = ar.retrieveAmbassadors();
			} catch (SQLException sqle) {
				log.error("Error retrieving today's ambassadors, ", sqle);
			}
		}
		tdc.addDataMapEntry("availableAmbassadors", ambs);
	}
	
	/**
	 * Retrieves additional assignment data depending upon the actionType.
	 * @param req
	 * @param tdc
	 */
	private void retrieveAdditionalData(SMTServletRequest req, TrackerDataContainer tdc) 
		throws ActionException {
		log.debug("retrieving additional data...");
		if ((StringUtil.checkVal(req.getParameter("actionType")).equals("assignment") || 
			StringUtil.checkVal(req.getParameter("actionType")).equals("interaction"))	&& 
			StringUtil.checkVal(req.getParameter("assignmentId")).length() > 0) {
			// we are retrieving a specific assignment so we will also retrieve the patient the patient source form
			this.retrievePatientSourceData(req, tdc);
		}
	}
	
	/**
	 * Checks to see if a custom sort field was specified.  If so, the assignments are
	 * sorted by the custom sort field.
	 * @param req
	 * @param tdc
	 */
	private void doCustomSort(SMTServletRequest req, TrackerDataContainer tdc) {
		String sortField = StringUtil.checkVal(req.getParameter("sortField"));
		if (sortField.equalsIgnoreCase("actionStatus")) {
			log.debug("sorting by actionStatus");
			List<AssignmentVO> assignments = tdc.getAssignments();
			AssignmentActionsComparator aac = new AssignmentActionsComparator();
			aac.setSortType(req.getParameter("sortType"));
			Collections.sort(assignments, aac);
		}
	}

	/**
	 * Examines emailType to determine if an email notification needs to be sent
	 * @param req
	 * @param emailType
	 * @throws ActionException
	 * @throws MailException
	 */
	private void processNotifications(SMTServletRequest req, boolean isCreateNewAssignment) 
		throws ActionException, MailException {
		log.debug("processing notifications...");
		if (emailType == 0) return;
		
		// retrieve the patient and assignee data to use in the email notification
		log.debug("retrieving full assignment record...");
		this.retrieve(req);
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		
		// retrieve additional data based on emailtype
		if (! isCreateNewAssignment) this.retrieveAdditionalNotificationData(req, tdc);
		
		// send the email notification
		this.sendEmail(req, tdc, isCreateNewAssignment);
		log.debug("sent emailType: " + emailType);
	}
	
	/**
	 * Updates the ambassadors current assignment count.  Adds 1 on an insert, 
	 * subtracts 1 on a decline/expire change.
	 * @param req
	 * @param avo
	 * @throws SQLException
	 */
	private void updateCurrentAssignmentCount(SMTServletRequest req, AssignmentVO avo) {
		log.debug("determining assignment count update");
		int addCount = 0;
		if (avo.getAssignmentId() == null) { // INSERT
			// this is an insert of a new assignment, add one to amb's count
			addCount = 1;
		} else if (avo.getCurrentStatusId() != avo.getAssignmentStatusId()) { // UPDATE, status changed
			int newStatus = avo.getAssignmentStatusId();
			log.debug("currentStatus/newStatus is: " + avo.getCurrentStatusId() + "/" + newStatus);
			if (newStatus == SJMTrackerConstants.STATUS_COMPLETED || 
					newStatus == SJMTrackerConstants.STATUS_EXPIRED || 
					newStatus == SJMTrackerConstants.STATUS_REASSIGNED) {
				// assignment was completed by amb or it expired, decrement the count
				addCount = -1;
			} else if (newStatus == SJMTrackerConstants.STATUS_CLOSED) {
				// new status is 'closed', check the previous status
				if (avo.getCurrentStatusId() < SJMTrackerConstants.STATUS_COMPLETED) {
					// this was closed without being set to 'completed' first, decrement the count
					addCount = -1;
				}
			} else if (newStatus == SJMTrackerConstants.STATUS_COMPLAINT) {
				// complaints are only updated by admins or reassigned to admins
				boolean isAdminOwned = false;
				isAdminOwned = Convert.formatBoolean(req.getAttribute("isAdminOwnedAssignment"));
				log.debug("isAdminOwned check in assignment count updater: " + isAdminOwned);
				if (! isAdminOwned) {
					// update is happening due to a re-assignment of the assignment to an admin
					// so increment the assignment count
					addCount = 1;
				}
			} else if (newStatus == SJMTrackerConstants.STATUS_PENDING) {
				// status changed back to 'pending' because this is a reassignment to a new ambassador
				addCount = 1;
			} else if (newStatus == SJMTrackerConstants.STATUS_IN_PROGRESS) {
				// only update count if assignment was changed back to 'in progress' from a 'completed' or 'closed' state.
				if (avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_COMPLETED) ||
						avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_CLOSED)) {
					addCount = 1;
				}
			}
		} else if (avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_PENDING) && 
				avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
			// status didn't change but assignment was declined by the assignee
			addCount = -1;
		}
		// if nothing to add/subtract, return
		if (addCount == 0) return;
		// update the current assignment count
		log.debug("updating ambassadors current assignment count; addCount: " + addCount);
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_assignee set curr_assign_no += ? where assignee_id = ? ");
		log.debug("curr assign update SQL: " + sql.toString());
		PreparedStatement ps =  null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, addCount);
			ps.setString(2, avo.getAssigneeId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error updating ambassador's current assignment count, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
	}
	
	
	/**
	 * Retrieves additional data for inclusion in email notifications.
	 * @param req
	 * @param tdc
	 * @param emailType
	 * @throws ActionException
	 */
	private void retrieveAdditionalNotificationData(SMTServletRequest req, TrackerDataContainer tdc) 
		throws ActionException {
		log.debug("retrieving additional notification data...");
		if (emailType.equals(SJMTrackerConstants.STATUS_PENDING)) {
		/* PHASE 3 item #3001: removed 'accepted', changed to 'PENDING'
		 * if (emailType.equals(SJMTrackerConstants.STATUS_ACCEPTED)) { */
			if (tdc.getContactForm() == null) this.retrievePatientSourceData(req, tdc);
		} else if (emailType.equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
			if (StringUtil.checkVal(req.getParameter("actionType")).equals("interaction")) {
				this.retrievePatientInteraction(req, tdc);
			}
		} else if (emailType.equals(SJMTrackerConstants.STATUS_REQUEST_ASSIST)) {
			if (StringUtil.checkVal(req.getParameter("actionType")).equals("interaction")) {
				this.retrievePatientInteraction(req, tdc);
			}
		}
	}

	/**
	 * Retrieves the form that was originally submitted to create the patient
	 * @param req
	 * @param tdc
	 * @throws ActionException
	 */
	private void retrievePatientSourceData(SMTServletRequest req, TrackerDataContainer tdc) throws ActionException {
		log.debug("retrieving patient source data...");
		// retrieve the contact submittal id for this patient from the assignment VO
		// make sure we have data for retrieving patient source data.
		if (tdc.getAssignments() == null || 
				tdc.getAssignments().isEmpty() ||
				tdc.getAssignments().get(0).getPatient() == null) return;
		
		PatientContactDataRetriever pcd = new PatientContactDataRetriever();
		pcd.setDbConn(dbConn);
		pcd.setContactSubmittalId(tdc.getAssignments().get(0).getPatient().getPatientSourceFormId());
		TrackerDataContainer t = null;
		try {
			t = pcd.retrievePatientContactData();
		} catch (SQLException sqle) {
			log.error("Error retrieving patient source contact data, ", sqle);
		}
		// set the contact form and data on the original tracker container
		if (t != null) {
			tdc.setContactForm(t.getContactForm());
			tdc.setContactData(t.getContactData());
		}
	}
	
	/**
	 * Retrieves the patient interaction for the assignment that has been set to a complaint
	 * status.  The interactionId value is on the request so only the interaction that set
	 * the complaint status is retrieved.
	 * @param req
	 * @param tdc
	 */
	private void retrievePatientInteraction(SMTServletRequest req, TrackerDataContainer tdc) {
		log.debug("retrieving patient interaction data for complaint email...");
		
		// make sure there aren't form submittal ids on the request
		String[] formSubmittalIds = req.getParameterValues("formSubmittalId");
		req.setParameter("formSubmittalId", null);
		
		try {
			SMTActionInterface sai = new PatientInteractionManager(this.actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving patient interaction data for email notification, ", ae);
		}
		
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			TrackerDataContainer t = (TrackerDataContainer) mod.getActionData();
			tdc.setInteractions(t.getInteractions());
		}
		
		// put the form submittal IDs back on the request
		if (formSubmittalIds != null) {
			req.setParameter("formSubmittalId", formSubmittalIds, true);
		}
	}
	
	/**
	 * Retrieves action id of patient source contact form
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unused")
	private String retrieveContactFormId(String contactSubmittalId) {
		String contactFormId = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select action_id from contact_submittal where contact_submittal_id = ?");
		log.debug("retrieveContactFormId SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, contactSubmittalId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				contactFormId = rs.getString("action_id");
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving action_id for patient source contact form, ", sqle);
		}
		return contactFormId;
	}
	
	/**
	 * Formats and sends email to recipients based on change in assignment status
	 * @param req
	 * @param emailType
	 * @param assignment
	 * @throws MailException
	 */
	private void sendEmail(SMTServletRequest req, TrackerDataContainer tdc, boolean isCreateNewAssignment) 
			throws MailException {
		log.debug("sending email...");
		if (tdc.getAssignments() == null || tdc.getAssignments().isEmpty()) return;

		// send alternate email if applicable
		this.sendAlternateEmail(req, tdc);
		String teamEmailAddress = "";
		if (isCreateNewAssignment) {
			teamEmailAddress = StringUtil.checkVal(req.getParameter("teamMemberEmailAddress"));
			if (teamEmailAddress.length() > 0 && StringUtil.isValidEmail(teamEmailAddress)) {
				attributes.put("teamMemberEmailAddress", teamEmailAddress);
			}
			attributes.put("urgencyRequest", StringUtil.checkVal(req.getParameter("urgencyRequest")));
		}
		
		// send standard email
		TrackerMailFormatter mf = new TrackerMailFormatter(tdc);
		mf.setAttributes(attributes);
		mf.setRecipients(this.setRecipients(tdc));
		// set emailType again since it may have been overridden by the setRecipients method
		mf.setType(emailType);
		mf.sendEmail();
		
		// send email to SJM team member if specified
		if (isCreateNewAssignment) {
			if (attributes.get("teamMemberEmailAddress") != null) {
				mf.setRecipients(new String[] {(String)attributes.get("teamMemberEmailAddress")});
				mf.setType(SJMTrackerConstants.EMAIL_TYPE_COPY_SJM_TEAM);
				mf.sendEmail();
			}
		}
	}
	
	/**
	 * Sends email notification for new assignment notifications to ambassadors alternate email address if 'use alternate email'
	 * flag is enabled and if an alternate email exists for the ambassador
	 * @param req
	 * @param tdc
	 * @throws MailException
	 */
	private void sendAlternateEmail(SMTServletRequest req, TrackerDataContainer tdc) throws MailException {
		log.debug("checking for alternate email send...");
		if (emailType.equals(SJMTrackerConstants.STATUS_PENDING)) {

			List<String> recpts = new ArrayList<String>();
			AssigneeVO amb = tdc.getAssignees().get(0);
			// use alternate email if exists
			if (amb.getUseAlternateEmail() == 0) return;
			
			String altAmbEmail = StringUtil.checkVal(amb.getAlternateEmailAddress()).trim();
			if (altAmbEmail.length() == 0) return;
			
			if (StringUtil.isValidEmail(altAmbEmail)) {
				log.debug("valid alternate email found: " + altAmbEmail);
				recpts.add(altAmbEmail);
			} else {
				return;
			}
			
			// set alternate email type so that we preserve the standard emailType value
			Integer altEmailType = new Integer(SJMTrackerConstants.EMAIL_TYPE_ALTERNATE_ADDRESS);
			
			log.debug("sending alternate email for altEmailType of: " + altEmailType);
			String[] toMail = recpts.toArray(new String[0]);
			// send email since we have a valid alternate email
			TrackerMailFormatter mf = new TrackerMailFormatter(tdc);
			mf.setAttributes(attributes);
			mf.setRecipients(toMail);
			// set emailType again since it may have been overridden by the setRecipients method
			mf.setType(altEmailType);
			mf.sendEmail();
		} 
	}
	
	/**
	 * Determines email recipients based on the emailType
	 * @param emailType
	 * @param tdc
	 * @return
	 */
	private String[] setRecipients(TrackerDataContainer tdc) {
		log.debug("setting recipient(s)...");
		List<String> recpts = new ArrayList<String>();
		AssigneeVO amb = tdc.getAssignees().get(0);
		if (emailType.equals(SJMTrackerConstants.STATUS_PENDING)) {
			// mail to amb SJM address
			recpts.add(amb.getEmailAddress());
		} 
		/* PHASE3 item #3001 removal of 'accepted email'
		 	else if (emailType.equals(SJMTrackerConstants.STATUS_ACCEPTED)) {
			// mail to amb SJM address
			recpts.add(amb.getEmailAddress());
		} 
		*/ 
		else if (emailType.equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
			// mail to the appropriate admin
			//log.debug("***** EMAIL SEND: email type: COMPLAINT; to ADMIN");
			recpts.add(this.retrieveAmbassadorAdminEmail(amb));
		} else if (emailType.equals(SJMTrackerConstants.EMAIL_TYPE_STATUS_IN_PROGRESS_AFTER_ASSIST)) {
			//recpts.add(tdc.getAssignments().get(0).getAssignee().getEmailAddress());
			recpts.add(amb.getEmailAddress());
		} else {
			// default case, send to admin
			recpts.add(this.retrieveAmbassadorAdminEmail(amb));
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
