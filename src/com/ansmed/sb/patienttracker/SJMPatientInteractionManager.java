package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

//SMT Baselibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilderII libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.PatientInteractionManager;
import com.smt.sitebuilder.action.tracker.vo.PatientInteractionVO;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
* <b>Title</b>SJMPatientInteractionManager.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Apr 21, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMPatientInteractionManager extends TrackerAction {

	public SJMPatientInteractionManager() {
		super();
	}

	public SJMPatientInteractionManager(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMPatientInteractionManager retrieve...");
				
		// retrieve interaction(s)	
		SMTActionInterface sai = null;
		sai = new PatientInteractionManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer)mod.getActionData();
		mod = null;
				
		// retrieve assignment(s)/assignee(s)/patient(s)
		sai = new SJMAssignmentFacade(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc1 = (TrackerDataContainer) mod.getActionData();
		
		// merge interaction/assignment/assignee/patient data
		mergeInteractions(tdc1, tdc.getInteractions());
		
		// set data on module VO
		mod.setActionData(tdc1);
		mod.setDataSize(tdc1.getInteractions().size());
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMPatientInteractionManager build...");
		boolean isAdmin = false;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role != null && role.getRoleLevel() >= AssigneeManager.MIN_ADMIN_TYPE_ID) isAdmin = true;
		StringBuffer msg = new StringBuffer();
		boolean success = true;
		// process any reviews that were processed
		this.processReviews(req, isAdmin, msg);
		
		boolean processInteraction = Convert.formatBoolean(req.getParameter("processInteraction"));
		boolean processAssignment = Convert.formatBoolean(req.getParameter("processAssignment"));
		boolean isComplaint = false;
		boolean isRequestReassignment = false;
		boolean isNowClosed = false;
		Integer currStatusId = Convert.formatInteger(req.getParameter("currentAssignmentStatusId"));
		Integer statusId = Convert.formatInteger(req.getParameter("assignmentStatusId"));
		if (Convert.formatBoolean(req.getParameter("productComplaint")) && (! statusId.equals(SJMTrackerConstants.STATUS_COMPLAINT))) {
			isComplaint = true;
		} else if (statusId.equals(SJMTrackerConstants.STATUS_REQUEST_REASSIGN)) {
			isRequestReassignment = true;
		} else if (statusId.equals(SJMTrackerConstants.STATUS_CLOSED)) {
			if (currStatusId.equals(SJMTrackerConstants.STATUS_COMPLETED)){
				isNowClosed = true;
			}
		}
		log.debug("processInteraction: " + processInteraction);
		log.debug("processAssignment: " + processAssignment);
		
		// save the interaction first
		if (processInteraction) {
			// insert patient interaction base record.
			try {
				SMTActionInterface sai = null;
				sai = new PatientInteractionManager(this.actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.build(req);
				msg.append("You have successfully saved this interaction.");
			} catch (ActionException ae) {
				log.error("Error saving interaction., ", ae);
				msg.append("Error: Unable to save this interaction.");
				success = false;
			}
		}
		
		// if a summary email was requested, retrieve the interaction before we update
		// the assignment!!
		TrackerDataContainer tdc = this.retrieveInteractionSummary(req, isAdmin);
					
		// update the assignment if necessary
		if (success) {
			try {
				msg.append(this.checkAssignment(req, isAdmin, isComplaint));
			} catch (ActionException ae) {
				log.error("Error: Unable to save assignment update., ", ae);
				msg.append("<br/>Error: Unable to update your assignment.");
			}
		}
		
		// check for/send rep(summary) email and/or tech support email
		msg.append(this.processSummaryEmailSends(req, tdc));
		
      	// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
    	url.append(page.getFullPath());
    	if (isComplaint) {
    		if (msg.indexOf("Error") < 0) {
    			msg = new StringBuffer("The complaint has been processed and the patient has been re-assigned to the program manager.");
    	    	// append the build msg
    	    	url.append("?msg=").append(msg);
    		}
    	} else if (isRequestReassignment) {
    		if (msg.indexOf("Error") < 0) {
    			msg = new StringBuffer("Your assignment has been reassigned.");
    	    	// append the build msg
    	    	url.append("?msg=").append(msg);
    		}
    	} else if (isNowClosed) {
    		// redirect to the dashboard.
	    	url.append("?organizationId=").append(req.getParameter("organizationId"));
	    	url.append("&msg=").append(msg);
    	} else {
        	// append actionType depending upon the subType requested.
	    	url.append("?actionType=interaction");
	    	url.append("&assignmentId=").append(req.getParameter("assignmentId"));
	    	if (isAdmin) {
	    		url.append("&logs=true");
	    	} else {
		    	if (StringUtil.checkVal(req.getParameter("subType")).length() > 0) {
		    		url.append("&subType=").append(req.getParameter("subType"));
		    		if (req.getParameter("subType").equals("patient")) {
		    			url.append("&fromType=patient");
		    		} else if (req.getParameter("assigneeId") != null) {
	    	    		url.append("&assigneeId=");
	    	    		url.append(req.getParameter("assigneeId"));
		    		}
		        	if (req.getParameter("patientId") != null) {
		        		url.append("&patientId=");
		        		url.append(req.getParameter("patientId"));
		        	}
		    	}
	    	}
	    	url.append("&organizationId=").append(req.getParameter("organizationId"));
	    	// append the build msg
	    	url.append("&msg=").append(msg);
    	}
    	
    	log.debug("SJMPatientInteractionManager redirect URL: " + url);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	/**
	 * Merges interactions with the associated assignment
	 * @param tdc
	 * @param livo
	 */
	private void mergeInteractions(TrackerDataContainer tdc, List<PatientInteractionVO> livo) {
		// merge interactions with assignments
		if (! livo.isEmpty()) {
			List<AssignmentVO> assignments = tdc.getAssignments();
			for (AssignmentVO asn : assignments) {
				for (PatientInteractionVO p : livo) {
					if (asn.getAssignmentId().equals(p.getAssignmentId())) {
						asn.addInteraction(p);
						break;
					}
				}
			}
		}
		tdc.setInteractions(livo);
	}
	
	/**
	 * Processes interaction review flags, sets up the redirect
	 * that will be used upon return
	 * @param req
	 */
	private void processReviews(SMTServletRequest req, boolean isAdmin, StringBuffer msg) {
		if (! isAdmin || ! StringUtil.checkVal(req.getParameter("processReviews")).equalsIgnoreCase("true")) return;
		log.debug("processing reviews");
		// retrieve the interaction IDs from the request
		String[] interactionIds = req.getParameterValues("reviewed");
		//String msg = "No reviews were marked for processing.";
		// if there are reviews to process, process them
		if (interactionIds != null && interactionIds.length > 0) {
			log.debug("interactionIds to be marked as reviewed: " + interactionIds.length);
			StringBuffer stubSql = new StringBuffer();
			stubSql.append("update pt_interaction set review_flg = 1 ");
			stubSql.append("where pt_interaction_id = ?");
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(stubSql.toString());
				for (String id : interactionIds) {
					ps.setString(1, id);
					ps.addBatch();
				}	
				ps.executeBatch();
				msg.append("The interaction review status was updated successfully. ");
			} catch (SQLException sqle) {
				msg.append("Error: Unable to update the interaction review status. ");
				log.error("Error updating review flags for interactions, ", sqle);
			} finally {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Checks for request parameters that will trigger an assignment update 
	 * @param req
	 * @throws ActionException
	 */
	private String checkAssignment(SMTServletRequest req, boolean isAdmin, boolean isComplaint) throws ActionException {
		log.debug("checking for assignment update...");
		// check to see if we need to update the assignment also
		boolean processAssignment = Convert.formatBoolean(req.getParameter("processAssignment"));
		if (isComplaint) processAssignment = true;
		log.debug("processAssignment|isComplaint: " + processAssignment + "|" + isComplaint);
		String msg = "";
		if (processAssignment) {
			// we need to remove the formSubmittalId from the request in case we also need to write
			// an assignment log entry...otherwise the formSubmittalId from the 'interaction' build op will be reused.
			req.setParameter("formSubmittalId", null);
			if (isComplaint) {
				// set assignment status to 'complaint', override any pre-existing value
				req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_COMPLAINT.toString(), true);
				log.debug("assignment status changed as result of interaction: " + req.getParameter("assignmentStatusId"));
			}
			
			// update the assignment
			SMTActionInterface sai = new SJMAssignmentFacade(this.actionInit);
			sai.setDBConnection(dbConn);
			sai.setAttributes(attributes);
			sai.build(req);
			msg = "You have successfully updated this assignment.";
		} 
		return msg;
	}
	
	/**
	 * Retrieves the interaction summary.
	 * @param req
	 * @param isAdmin
	 * @return
	 */
	private TrackerDataContainer retrieveInteractionSummary(SMTServletRequest req, boolean isAdmin) {
		log.debug("checking for retrieval of interaction summary");
		TrackerDataContainer tdc = null;
		if (StringUtil.checkVal(req.getParameter("emailFieldRep")).length() == 0 &&
				StringUtil.checkVal(req.getParameter("emailTechSupport")).length() == 0) {
			return null;
		}
		log.debug("retrieving of interaction summary");
		// override certain IDs on the request to make sure we retrieve all interactions for this assignment
		String tmp = null;
		// preserve certain values on request
		String origAssigneeId = req.getParameter("assigneeId");
		String origStatusId = req.getParameter("assignmentStatusId");

		// if admin, reset the assignee ID to get full summary
		if (isAdmin) req.setParameter("assigneeId", tmp, true);
		
		// reset certain parameters
		req.setParameter("interactionId", tmp, true);
		req.setParameter("formSubmittalId", tmp, true);
		req.setParameter("assignmentStatusId", tmp, true);
				
		// retrieve the full summary since we have at least one send to process
		try {
			this.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving assignment data for email to tech support, ", ae);
			msg.append("Error: Unable to retrieve assignment data for email send(s). ");
			return null;
		} finally {
			// restore preserved req values regardless of outcome of try/catch
			req.setParameter("assigneeId", origAssigneeId, true);
			req.setParameter("assignmentStatusId", origStatusId, true);
		}
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tempTdc = (TrackerDataContainer)mod.getActionData();
		
		tdc = new TrackerDataContainer();
		tdc.setInteractions(tempTdc.getInteractions());
		// everything we need for the assignment data is rolled up in the assignment
		tdc.setAssignments(tempTdc.getAssignments());
		tdc.addAssignee(tdc.getAssignments().get(0).getAssignee());
		tdc.addPatient(tdc.getAssignments().get(0).getPatient());
		
		return tdc;
	}
	
	/**
	 * Checks to see if summary email to rep was requested and/or if email to tech support was
	 * requested.  In either case, retrieves summary data and calls method to send email based 
	 * on the appropriate email type.
	 * @param req
	 * @param tdc
	 * @return
	 */
	private StringBuffer processSummaryEmailSends(SMTServletRequest req, TrackerDataContainer tdc) {
		log.debug("checking for summary email sends");
		StringBuffer msg = new StringBuffer();
		if (StringUtil.checkVal(req.getParameter("emailFieldRep")).length() == 0 &&
				StringUtil.checkVal(req.getParameter("emailTechSupport")).length() == 0) {
			return msg;
		}
		
		if (StringUtil.checkVal(req.getParameter("emailFieldRep")).length() > 0) {
			log.debug("sending field rep summary email...");
			try {
				this.sendFullSummaryEmail(tdc, req.getParameter("emailFieldRep"), SJMTrackerConstants.EMAIL_TYPE_REP);
			} catch (MailException me) {
				log.error("Error sending full assignment/interaction summary to field rep, ", me);
				msg.append("Error: Unable to send summary to patient's field representative. ");
			}
		}
		
		if (StringUtil.checkVal(req.getParameter("emailTechSupport")).length() > 0) {
			log.debug("sending tech support email...");
			try {
				this.sendFullSummaryEmail(tdc, req.getParameter("emailTechSupport"), SJMTrackerConstants.EMAIL_TYPE_TECH_SUPPORT);
			} catch (MailException me) {
				log.error("Error sending full assignment/interaction summary to technical support, ", me);
				msg.append("Error: Unable to send summary to technical support. ");
			}
		}
		return msg;
	}
	
	/**
	 * Formats and sends email based on the data, recipient, and email type passed in.
	 * @param tdc
	 * @param recipient
	 * @param emailType
	 */
	private void sendFullSummaryEmail(TrackerDataContainer tdc, String recipient, Integer emailType) throws MailException {
		log.debug("sending full summary for email type/recipient: " + emailType + "/" + recipient);
		TrackerMailFormatter mf = new TrackerMailFormatter(tdc);
		mf.setAttributes(attributes);
		mf.setType(emailType);
		String[] recipients = new String[] { recipient };
		mf.setRecipients(recipients);
		mf.sendEmail();
	}
	
}
