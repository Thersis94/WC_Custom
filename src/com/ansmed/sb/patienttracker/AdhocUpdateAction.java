package com.ansmed.sb.patienttracker;

// SMT Base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC libs
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>AdhocUpdateAction.java<p/>
* <b>Description: Updates a pre-existing (completed/closed) assignment with an interaction.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 23, 2012
* <b>Changes: </b>
* Oct 23, 2012: David Bargerhuff - Phase 3 - #3007
****************************************************************************/
public class AdhocUpdateAction extends TrackerAction {
	
	public AdhocUpdateAction () {}
	
	public AdhocUpdateAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("AdhocUpdateAction build...");
		// need patientId and assignmentId from the request
		AssignmentVO avo = new AssignmentVO();
		avo.setData(req);
				
		 //1. retrieve assignment data for patient chosen on form.
		AssignmentVO fullVO = this.retrieveLatestAssignmentData(req, avo);
		boolean isValid = this.checkAssignmentStatus(fullVO);
		if (isValid) {
			boolean isOwner = this.checkAssignmentOwner(avo, fullVO);
			if (isOwner) {
				this.processOwnersAssignment(req, fullVO);
			} else {
				this.processNonOwnersAssignment(req, avo, fullVO);
			}
			msg.append("You have successfully added an interaction for this patient.");
		} else {
			msg.append("The assignment to this patient is still open and could not be updated.  Please contact the Program Manager.");
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
	 * Re-opens a 'completed' or 'closed' assignment belonging to this ambassador, adds the interaction,
	 * and sets the assignment back to 'completed'
	 * @param req
	 * @param fullVO
	 * @throws ActionException
	 */
	private void processOwnersAssignment(ActionRequest req, AssignmentVO fullVO) 
		throws ActionException {
		log.debug("processing 'owner' assignment...");
		// re-open to 'in progress', log to 'in progress'
		fullVO.setCurrentStatusId(fullVO.getAssignmentStatusId()); // set 'current' status to existing status
		fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
		fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
		this.updateAssignment(req, fullVO);
		// save interaction
		this.createInteraction(req, fullVO);
		
		boolean isComplaint = Convert.formatBoolean(req.getParameter("productComplaint"));
		if (! isComplaint) {
			// update to 'completed', log to 'completed'
			fullVO.setCurrentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
			fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_COMPLETED);
			fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			this.updateAssignment(req, fullVO);
		}
	}
	
	/**
	 * Processes a 'closed' assignment belonging to another ambassador.  The assignment is 'virtually'
	 * reassigned to this ambassador, the interaction is logged, and the assignment is then set to 'completed'.
	 * @param req
	 * @param fullVO
	 */
	private void processNonOwnersAssignment(ActionRequest req, AssignmentVO avo, AssignmentVO fullVO) 
		throws ActionException {
		log.debug("processing 'non-owner' assignment...");
		if (fullVO.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLETED)) {
			// DENY, assignment is in a 'completed' state and should be reviewed by an ambassador.
			msg.append("Unable to add this interaction. ");
			msg.append("Another ambassador has an open assignment with this patient. ");
			msg.append("Please contact the Program Manager. ");
		} else {
			/*
			// reassign, log 'reassign'
			log.debug("updating assignment to 'reassigned'");
			fullVO.setCurrentStatusId(SJMTrackerConstants.STATUS_CLOSED);
			fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_REASSIGNED);
			fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			this.updateAssignment(req, fullVO);
			*/
			
			fullVO.setAssigneeId(avo.getAssigneeId());
			// replace the assigneeId on the request
			req.setParameter("assigneeId", fullVO.getAssigneeId(), true);
			
			// pending, log pending
			log.debug("updating assignment to 'pending'");
			fullVO.setCurrentStatusId(0);
			fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_PENDING);
			fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			this.updateAssignment(req, fullVO);
			
			// update to 'in progress', log 'in progress'
			log.debug("updating assignment to 'in progress'");
			fullVO.setCurrentStatusId(SJMTrackerConstants.STATUS_PENDING);
			fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
			fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
			this.updateAssignment(req, fullVO);
			
			// save interaction
			this.createInteraction(req, fullVO);
			
			boolean isComplaint = Convert.formatBoolean(req.getParameter("productComplaint"));
			if (! isComplaint) {
				// update to 'completed', log to 'completed'
				log.debug("updating assignment to 'completed'");
				fullVO.setCurrentStatusId(SJMTrackerConstants.STATUS_IN_PROGRESS);
				fullVO.setAssignmentStatusId(SJMTrackerConstants.STATUS_COMPLETED);
				fullVO.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_ADHOC);
				this.updateAssignment(req, fullVO);
			}
		}
	}
	
	/**
	 * Retrieves 
	 * @param avo
	 */
	private AssignmentVO retrieveLatestAssignmentData(ActionRequest req, AssignmentVO avo) 
		throws ActionException {
		log.debug("retrieving latest assignment data...");
		log.debug("assignment ID is: " + req.getParameter("assignmentId"));
		//req.setParameter("logs", "true"); // include the assignment logs
		req.setParameter("assigneeId", req.getParameter("currAssigneeId"), true);
		log.debug("orig/req assigneeId: " + avo.getAssigneeId() + "/" + req.getParameter("assigneeId"));
		ActionInterface sai = new SJMAssignmentManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		try {
			sai.retrieve(req);
		} catch (Exception e) {
			throw new ActionException(e.getMessage());
		} finally {
			// clean up the req param
			req.setParameter("assigneeId", avo.getAssigneeId(), true);
		}
		log.debug("assigneeId after retrieve: " + req.getParameter("assigneeId"));
		AssignmentVO vo = null;
		TrackerDataContainer tdc = null;
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			tdc = (TrackerDataContainer)mod.getActionData();
			if (tdc != null) {
				if (tdc.getAssignments() != null && tdc.getAssignments().size() > 0) {
					vo = tdc.getAssignments().get(0);
				}
			}
		}
		return vo;
	}
	
	/**
	 * Checks the assignment status.
	 * @param rvo
	 * @return Returns 'true' if assignment status is 'completed' or 'closed', otherwise returns 'false'.
	 */
	private boolean checkAssignmentStatus(AssignmentVO rvo) {
		log.debug("checking assignment status...");
		if (rvo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_COMPLETED) ||
				rvo.getAssignmentStatusId().equals(SJMTrackerConstants.STATUS_CLOSED)) {
			return true; 
		} else {
			return false;
		}
	}

	/**
	 * Checks to see if the assigneeID of each AssignmentVO are equal.
	 * @param avo
	 * @param rvo
	 * @return Returns 'true' if the assignee IDs match, otherwise returns 'false'.
	 */
	private boolean checkAssignmentOwner(AssignmentVO avo, AssignmentVO rvo) {
		if (avo.getAssigneeId().equals(rvo.getAssigneeId())) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Updates the assignment
	 * @param req
	 * @param avo
	 * @throws ActionException
	 */
	private void updateAssignment(ActionRequest req, AssignmentVO avo) 
			throws ActionException {
		// update the assignment
		req.setParameter("currentAssignmentStatusId", avo.getCurrentStatusId().toString(), true);
		req.setParameter("assignmentStatusId", avo.getAssignmentStatusId().toString(), true);
		req.setParameter("assignmentResponseId", avo.getAssignmentResponseId().toString(), true);
		ActionInterface sai = null;
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
	private void createInteraction(ActionRequest req, AssignmentVO avo) 
		throws ActionException {
		log.debug("creating adhoc interaction entry...");
		req.setParameter("processInteraction", "true");
		req.setParameter("formSubmittalId", null); // prep
		ActionInterface sai = null;
		sai = new SJMPatientInteractionManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		req.setParameter("formSubmittalId", null); // clean-up
	}
	
	private void resetLoggingParameters(ActionRequest req) {
		// clear logging params
		req.setParameter(AssignmentLogManager.LOG_STATUS_ID, null);
		req.setParameter(AssignmentLogManager.LOG_RESPONSE_ID, null);
		req.setParameter(AssignmentLogManager.LOG_ASSIGN_TEXT, null);
		req.setParameter(AssignmentLogManager.LOG_SYSTEM_TEXT, null);
	}
}
