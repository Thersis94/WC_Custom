package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

// SMT base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

//SiteBuilder II libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
* <b>Title</b>AmbassadorResponseManager.java<p/>
* <b>Description: </b> Manages the accept/reject response sent by an ambassador in response to 
* an assignment notification.
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 05, 2011
* <b>Changes: </b>
****************************************************************************/
public class AmbassadorResponseManager extends TrackerAction {
	
	//private static final String ASSIGNMENT_OLD = "assignmentOld"; 
	private Integer responseType = 0;
	private String redirectVal = "";
	
	public AmbassadorResponseManager() {
		super();
	}
	
	public AmbassadorResponseManager(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) {
		log.debug("Starting AmbassadorResponseManager build...");
		
		// check for a valid response value on the request
		if (this.checkResponse(req)) {
			//if the response is valid, process the assignment
			this.processAssignment(req);
		}
		
	   	// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
    	
    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
    	url.append(page.getFullPath()).append("?redirectVal=").append(redirectVal);
    	if (msg != null) {
    		url.append("&msg=");
    		if (req.getAttribute(TrackerAction.TRACKER_BUILD_MSG) != null) {
    			msg.append(req.getAttribute(TrackerAction.TRACKER_BUILD_MSG));

    		}
    		url.append(msg);
    	}
    	log.debug("AmbassadorResponseManager redirect URL: " + url);
    	
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) {
		log.debug("Starting AmbassadorResponseManager retrieve...");
		String process = StringUtil.checkVal(req.getParameter("process"));
		if (process.equalsIgnoreCase("true")) {
			log.debug("calling 'build' to process response");
			this.build(req);
		}
	}
	
	/**
	 * Checks response val passed in on request for validity
	 * @param req
	 * @return
	 */
	private boolean checkResponse(ActionRequest req) {
		String response = StringUtil.checkVal(req.getParameter("response"));
		if (response.length() > 0) {
			if (response.equalsIgnoreCase("accept")) {
				responseType = SJMTrackerConstants.RESPONSE_ACCEPTED;
				return true;
			} else if (response.equalsIgnoreCase("decline")) {
				responseType = SJMTrackerConstants.RESPONSE_DECLINED;
				return true;
			} else {
				redirectVal = "responseInvalid";
				return false;
			}
		} else {
			redirectVal = "responseInvalid";
			return false;
		}
	}
	
	/**
	 * Retrieves and processes assignment
	 * @param req
	 */
	private void processAssignment(ActionRequest req) {
		AssignmentVO avo = null;
		String assignmentId = StringUtil.checkVal(req.getParameter("assignmentId"));
		String assigneeProfileId = StringUtil.checkVal(req.getParameter("pid"));
		
		try {
			// try to retrieve the base record for this assignment
			avo = this.validateAssignment(assignmentId, assigneeProfileId);
		} catch (SQLException sqle) {
			redirectVal = "processError";
			log.error("Error validating assignment, ", sqle);
			return;
		}
		
		// if we retrieved a valid AssignmentVO, check the assignment status
		if (avo != null && this.hasValidAssignmentStatus(avo)) {
			// retrieve the full AssignmentVO
			try {
				avo = this.retrieveFullAssignment(req, avo);
			} catch (ActionException sqle) {
				redirectVal = "processError";
				log.error("Error retrieving assignment, ", sqle);
				return;
			}
			
			if (avo != null) {
				try {
					this.updateAssignment(req, avo);
				} catch (ActionException ae) {
					redirectVal = "processError";
					log.error("Error updating assignment response and status values, ", ae);
					return;
				}
				
				if (responseType.equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
					redirectVal = "assignmentDeclined";
				} else {
					redirectVal = "assignmentAccepted";
				}
			} else {
				redirectVal = "assignmentInvalid";
			}
		} else if (avo == null) {
			redirectVal = "assignmentInvalid";
		} else {
			redirectVal = "assignmentOld";
		}
	}
	
	/**
	 * Retrieves assignment record based on assignment ID passed in
	 * @param assignmentId
	 * @return
	 */
	private AssignmentVO validateAssignment(String assignmentId, String assigneeProfileId) throws SQLException {
		log.debug("validating assignment by retrieving base record for this assignment");
		AssignmentVO avo = null;
		if (assignmentId.length() == 0 || assigneeProfileId.length() == 0) return avo;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.* from pt_assignment a inner join pt_assignee b ");
		sql.append("on a.assignee_id = b.assignee_id where a.pt_assignment_id = ? ");
		sql.append("and b.assignee_profile_id = ? ");
		log.debug("assignment retrieve SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, assignmentId);
			ps.setString(2, assigneeProfileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				avo = new AssignmentVO();
				avo.setData(rs);
			}
		} catch (SQLException sqle) {
			throw new SQLException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		return avo;
	}
	
	/**
	 * Checks assignment status to make sure it is 'pending' and of a valid response type.
	 * @param avo
	 * @return
	 */
	private boolean hasValidAssignmentStatus(AssignmentVO avo) {
		log.debug("checking assignment status...");
		// determine if we need to update the assignment
		// only consider 'pending' assignments with certain response codes
		log.debug("considering assignment status/response ids: " + avo.getAssignmentStatusId() + "/" + avo.getAssignmentResponseId());
		if (avo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_PENDING)) {
			// if the assignment status is 'Pending', check the response code
			// if the response code is not blank, 'no reponse 48', or 'admin assigned' then return false
			if (! avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_DEFAULT) && 
					! avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_NONE_48) &&
					! avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED) &&
					! avo.getAssignmentResponseId().equals(SJMTrackerConstants.RESPONSE_REASSIGNED)) {
				redirectVal = "assignmentOld";
				return false;
			} else {
				return true;
			}
		} else {
			redirectVal = "assignmentOld";
			return false;
		}
	}
	
	/**
	 * Retrieves a full assignment VO (assignment, assignee, patient data)
	 * @param req
	 * @param avo
	 * @return
	 * @throws ActionException
	 */
	private AssignmentVO retrieveFullAssignment(ActionRequest req, AssignmentVO avo) throws ActionException {
		log.debug("retrieving full assignment (assignment, assignee, patient): " + req.getParameter("assignmentId"));
		AssignmentVO fullVo = null;
		// add role data to impersonate a 'registered user' so that 
		//we restrict the retrieval of assignment, assignee, and patient data
		SBUserRole role = new SBUserRole();
		role.setRoleLevel(SecurityController.ADMIN_ROLE_LEVEL);
		req.getSession().setAttribute(Constants.ROLE_DATA, role);
		
		// populate the request with the appropriate values for use by the action
		// assignmentId is already on the request
		req.setParameter("assigneeId", avo.getAssigneeId());
		req.setParameter("patientId", avo.getPatientId());
		
		// retrieve the assignment.
		SMTActionInterface sai = new SJMAssignmentFacade(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		// If exception thrown, catch it here so that we can ensure that the role impersonation 
		// data on the session is removed before returning from this method.
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving assignment for evaluating ambassador response, ", ae);
			throw new ActionException(ae.getMessage());
		} finally {
			// remove role data
			req.getSession().removeAttribute(Constants.ROLE_DATA);			
		}
		
		// get the assignment data
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		if (tdc.getAssignments() != null && tdc.getAssignments().size() > 0) {
			fullVo = tdc.getAssignments().get(0);
		}
		
		// return the vo
		return fullVo;
	}
	
	/**
	 * Updates status and response IDs of the given assignment
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void updateAssignment(ActionRequest req, AssignmentVO avo) throws ActionException {
		log.debug("updating original assignment");
		// set the current status
		avo.setCurrentStatusId(avo.getAssignmentStatusId());
		
		// set the new status and response IDs based on response type
		if (responseType.equals(SJMTrackerConstants.RESPONSE_ACCEPTED)) {
			// assignment accepted
			avo.setCurrentStatusId(SJMTrackerConstants.STATUS_PENDING);
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
			avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ACCEPTED);
			avo.setAcceptDate(new Date());
			req.setParameter("acceptDate", Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
			req.setParameter("assigneeProfileId", avo.getAssignee().getAssigneeProfileId());
			// set logging param
			req.setParameter(AssignmentLogManager.LOG_STATUS_ID, SJMTrackerConstants.STATUS_IN_PROGRESS.toString());
			req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, SJMTrackerConstants.RESPONSE_ACCEPTED.toString());
			
		} else if (responseType.equals(SJMTrackerConstants.RESPONSE_DECLINED)) {
			// assignment declined
			avo.setCurrentStatusId(SJMTrackerConstants.STATUS_PENDING);
			avo.setAssignmentStatusId(SJMTrackerConstants.STATUS_EXPIRED);
			avo.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_DECLINED);
		}
		
		// populate the request object with the VO values so that the update can be made
		// assignmentId is already on request.
		req.setParameter("assigneeId", avo.getAssigneeId(), true);
		req.setParameter("patientId", avo.getPatientId(), true);
		req.setParameter("currentAssignmentStatusId", avo.getCurrentStatusId().toString());
		req.setParameter("assignmentStatusId", avo.getAssignmentStatusId().toString());
		req.setParameter("assignmentResponseId", avo.getAssignmentResponseId().toString());
		req.setParameter("assignmentNotes", avo.getAssignmentNotes());
		req.setParameter("organizationId", SJMTrackerConstants.TRACKER_ORG_ID);
		// update the assignment
		SMTActionInterface sai = new SJMAssignmentFacade(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
	}
	
}
