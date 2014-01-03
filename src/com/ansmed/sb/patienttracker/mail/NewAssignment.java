package com.ansmed.sb.patienttracker.mail;

// SB ANS Medical
import com.ansmed.sb.patienttracker.SJMTrackerConstants;
import com.ansmed.sb.patienttracker.TrackerMailFormatter;

// WC 2.0
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;

/****************************************************************************
 * <b>Title</b>: NewAssignmentMailFormatter.java<p/>
 * <b>Description: </b>Formats a new assignment notification email.<p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Nov 30, 2012
 * Change Log:
 * 11/30/2012 - Created class due to refactoring of TrackerMailFormatter
 ****************************************************************************/
public class NewAssignment extends TrackerMailFormatter {
	
	public NewAssignment() {
		super();
	}
	
	public NewAssignment(TrackerDataContainer tdc) {
		super(tdc);
	}
	
	public NewAssignment(Integer type) {
		super(type);
	}
	
	/**
	Formats new assignment email.
	*/
	protected void formatStatusNewAssignment() {
		this.setSubject("New Assignment Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("You have been assigned to patient ").append(patient.getFirstName()).append(" ");
		sb.append(patient.getLastName()).append(" and have 48 hours to accept ");
		sb.append("or decline this assignment.  If you do not respond within 48 hours, the patient will be ");
		sb.append("reassigned to another available ambassador.<br/><br/>");
		sb.append("Please use the links below to either accept or decline this assignment:<br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.ASSIGNMENT_RESPONSE_URL).append(assignment.getAssignmentId());
		sb.append("&pid=").append(assignment.getAssignee().getAssigneeProfileId());
		sb.append("&response=").append("accept").append("\">Accept assignment</a><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.ASSIGNMENT_RESPONSE_URL).append(assignment.getAssignmentId());
		sb.append("&pid=").append(assignment.getAssignee().getAssigneeProfileId());
		sb.append("&response=").append("decline").append("\">Decline assignment</a><br/><br/>");
		
		//added verbiage, etc. for PHASE3 #3001
		sb.append(this.getNewAssignmentAdditionalInformation());
		/* removed for Phase3 #3001
		sb.append("Once you have contacted the patient, document your interaction by ");
		sb.append("<a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL).append("?actionType=interaction&assignmentId=");
		sb.append(assignment.getAssignmentId()).append("&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);
		sb.append("\">clicking here</a>.<br/><br/>");
		*/
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}

}
