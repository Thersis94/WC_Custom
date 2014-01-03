package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Apache Log4J
import org.apache.log4j.Logger;

// SMT Baselibs 2.0
import com.siliconmtn.exception.MailException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SitebuilderII libs
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentLogVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientInteractionVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
 * <b>Title</b>: SJMTrackerMailFormatter.java<p/>
 * <b>Description: </b>Formats the appropriate email based on the requested type.<p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Aug 23, 2011
 * Change Log:
 * 01/2012 - Updated email templates per Michelle Revello's edits
 * 02/13/2012 - Updated email templates per Laura Crandell's edits
 ****************************************************************************/
public class TrackerMailFormatter extends TrackerAction {
	
	protected final Logger log = Logger.getLogger(getClass());
	
	protected Integer type = new Integer(0);
	private String subject = null;
	private StringBuffer body = null;
	private String fromSender = SJMTrackerConstants.FROM_SENDER;
	protected List<String> emailTo = null;
	private String[] recipients = null;
	protected Map<String, String> fields = null;
	private TrackerDataContainer container = null;
	protected AssignmentVO assignment = null;
	private PatientInteractionVO interaction = null;
	protected AssigneeVO ambassador = null;
	protected PatientVO patient = null;
	private FormVO form = null;
	private ContactVO contactForm = null;
	private ContactDataContainer contactData = null;
	private Map<String, String> patientFieldMap = null;
	protected Map<String, String> patientDataMap = null;
	
	public TrackerMailFormatter() {
		fields = new HashMap<String, String>();
		emailTo = new ArrayList<String>();
		this.loadPatientFieldMap();
		patientDataMap = new LinkedHashMap<String, String>();
	}
	
	public TrackerMailFormatter(TrackerDataContainer tdc) {
		this();
		this.setContainer(tdc);
	}
	
	public TrackerMailFormatter(Integer type) {
		this();
		this.type = type;
	}
	
	/**
	 * Overloaded method.  Formats email based on the email type that has been
	 * set via the 'setType' public setter method.
	 */
	public void formatEmail() {
		this.formatEmail(this.type);
	}
	
	/**
	 * Overloaded method.  Formats email based on email type passed in.  Passes
	 * request obect as param to provide access to request parameters.
	 * @param req
	 * @param emailType
	 */
	public void formatEmail(Integer emailType) {
		this.setType(emailType);
		log.debug("formatting for email type: " + emailType);
		switch (emailType) {
			case 10: { //status pending
					if (ambassador.getTypeId() >= AssigneeManager.MIN_ADMIN_TYPE_ID) {
						this.formatStatusNewAdminAssignment();
					} else {
						this.formatStatusNewAssignment();
					}
				}
				break;
			case 20: //status accepted
				// removed per Phase 3 item #3001
				//this.formatAssignmentAccepted();
				log.error("Error, should not reach here.");
				break;
			case 40: // status 'request assistance'
				this.formatStatusRequestAssistance();
				break;
			case 50: //status 'request reassign'
				this.formatStatusRequestReassignment();
				break;
			case 60: //status complaint
				this.formatStatusComplaint();
				break;
			case 100: // status completed
				this.formatAssignmentCompleted();
				break;
			case 110: //response accepted
				// removed per Phase 3 item #3001
				//this.formatAssignmentAccepted();
				log.error("Error, should not reach here.");
				break;
			case 120://response declined
				this.formatAssignmentDeclined();
				break;
			case 130://no response 48
				this.formatOriginalAmbassadorResponseNone48();
				break;
			case 140://no response 96
				this.formatResponseNone96();
				break;
			case 150://admin assigned
				break;
			case 160: //response - reassigned
				this.formatResponseReassigned();
				break;
			case 170: // response - complaint
				break;
			case 900: // expired
				this.formatAssignmentExpired();
				break;
			case 910: // reassigned
				break;
			case 999: // closed
				break;
			case 1000: // used for follow-up notifications
				this.formatFollowUpReminder();
				break;
			case 1100: // used when in a no response 48 scenario and 
				// we need to email the newly assigned ambassador
				this.formatNextAvailableAmbassadorResponseNone48();
				break;
			case 1110: // email copy of assignment/interaction summary to rep
				this.formatSummaryCopyToFieldRep();
				break;
			case 1115: // email new assignment notification to ambassador's alternate email address
				this.formatAlternateEmailNotification();
				break;
			case 1120: // email to tech support
				this.formatSummaryCopyToTechSupport();
				break;
			case 1125: // email to amb after assignment has been reset to 'In Progress' from 'Request Assistance'
				this.formatRequestAssistanceCompleted();
				break;
			case 1130: // email of copy of field form submission to additional SJM team member.
				this.formatFieldFormSubmissionCopy();
				break;
			case 1135: // email to rep upon assignment via field form
				this.formatRepAmbassadorAssignment();
				break;
			case 1140: // email of copy of admin new assignment creation to SJM team member
				this.formatNewAssignmentCopySJMTeam();
				break;
			default:
				this.setBody(new StringBuffer(""));
				break;
		}
	}
	
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
		
	private void formatStatusNewAdminAssignment() {
		this.setSubject("New Assignment: No Match Found");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear Patient Ambassador Program Manager,<br/><br/>");
		sb.append("No ambassador was matched to patient ").append(patient.getFirstName()).append(" ");
		sb.append(patient.getLastName()).append(" so the patient was assigned to your account.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);		
	}
	
	private void formatStatusRequestAssistance() {
		this.setSubject("Assistance is Needed");
		StringBuffer sb = new StringBuffer();
		sb.append(ambassador.getFirstName()).append(" ").append(ambassador.getLastName()).append(" is requesting ");
		sb.append("your assistance with their assignment to patient ");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(". Please see ");
		sb.append("the interaction summary below.<br/><br/>");
		sb.append("Interaction Summary<br/>");
		sb.append(StringUtil.checkVal(this.retrieveInteractionSummary(), "No summary found.")).append("<br/><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL).append("?actionType=interaction&assignmentId=");
		sb.append(assignment.getAssignmentId()).append("&logs=true").append("&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);
		sb.append("\">Click here</a> to read the interaction history.<br/><br/>");
		sb.append(getThankYouLetterFooter());
		this.setBody(sb);
	}
	
	private void formatStatusRequestReassignment() {
		this.setSubject("Reassignment is Needed");
		StringBuffer sb = new StringBuffer();
		sb.append(ambassador.getFirstName()).append(" ").append(ambassador.getLastName()).append(" is requesting that their ");
		sb.append("assignment to patient ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(" is ");
		sb.append("reassigned to another ambassador. Please see the interaction summary below.<br/><br/>");
		sb.append("Interaction Summary<br/>");
		sb.append(StringUtil.checkVal(this.retrieveInteractionSummary(), "No summary found.")).append("<br/><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL).append("?actionType=interaction&assignmentId=");
		sb.append(assignment.getAssignmentId()).append("&logs=true").append("&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);
		sb.append("\">Click here</a> to read the interaction history and reassign this patient.<br/><br/>");
		sb.append(getThankYouLetterFooter());
		this.setBody(sb);
	}
	
	private void formatStatusComplaint() {
		this.setSubject("URGENT: Product Complaint Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear Patient Ambassador Program Manager,<br/><br/>");
		sb.append("The following assignment has been flagged as a product complaint: <br/><br/>");
		sb.append("<table>");
		sb.append("<tr><td>Ambassador Assigned:</td><td>").append(ambassador.getFirstName()).append(" ");
		sb.append(ambassador.getLastName()).append("</td></tr>\n");
		sb.append("<tr><td>Patient:</td><td>").append(patient.getFirstName()).append(" ");
		sb.append(patient.getLastName()).append("</td></tr></table><br/><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL).append("?actionType=interaction");
		sb.append("&assignmentId=").append(assignment.getAssignmentId()).append("&logs=true");
		sb.append("&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);		
		sb.append("\">Click here</a> to read the interaction.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);		
	}
	
	private void formatAssignmentCompleted() {
		this.setSubject("Assignment Completed");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear Patient Ambassador Program Manager,<br/><br/>");
		sb.append("The following assignment has been completed and is ready for your review:<br/>");
		sb.append("Patient Ambassador: ").append(ambassador.getFirstName()).append(" ").append(ambassador.getLastName()).append("<br/>");
		sb.append("Patient: ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("<br/><br/>");
		sb.append("Click here to review the assignment, <a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL);
		sb.append("?actionType=interaction&assignmentId=").append(assignment.getAssignmentId()).append("&logs=true");
		sb.append("&startView=reviewAssignment").append("&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);
		sb.append("\">Completed Assignment</a><br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);	
	}
	
	private void formatOriginalAmbassadorResponseNone48() {
		this.setSubject("Notification of Expired Assignment");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("Your assignment to ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(" ");
		sb.append("has now expired and has been reassigned to another available ambassador.  No additional follow up is needed.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	private void formatNextAvailableAmbassadorResponseNone48() {
		this.setSubject("New Assignment Notification: Urgent");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("You have been assigned to patient ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(".  ");
		sb.append("This patient was previously assigned to a different ambassador on ");
		String assignmentCreateDate = StringUtil.checkVal(Convert.formatDate(assignment.getPreviousAssignDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		sb.append(assignmentCreateDate).append(" and has not been contacted.  This assignment is now <b>urgent</b> and requires a response ");
		sb.append("within 24 hours.  If you do not respond within 24 hours, the patient will be reassigned to the Patient Ambassador Program Manager.<br/><br/>");
		sb.append("Please use the links below to either accept or decline this assignment:<br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.ASSIGNMENT_RESPONSE_URL).append(assignment.getAssignmentId());
		sb.append("&pid=").append(assignment.getAssignee().getAssigneeProfileId());
		sb.append("&response=").append("accept").append("\">Accept assignment</a><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.ASSIGNMENT_RESPONSE_URL).append(assignment.getAssignmentId());
		sb.append("&pid=").append(assignment.getAssignee().getAssigneeProfileId());
		sb.append("&response=").append("decline").append("\">Decline assignment</a><br/><br/>");
		
		//added verbiage, etc. for PHASE3 #3001
		sb.append(this.getNewAssignmentAdditionalInformation());
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}

	private void formatResponseNone96() {
		this.setSubject("URGENT: Reassignment Notification for Expired Assignment");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear Patient Ambassador Program Manager,<br/><br/>");
		sb.append("The following assignment has expired (no response after 96 hours) and has been reassigned to you:<br/><br/>");
		sb.append("<table>");
		sb.append("<tr><td>Original assignment date:</td><td>");
		String assignmentCreateDate = StringUtil.checkVal(Convert.formatDate(assignment.getCreateDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		sb.append(assignmentCreateDate).append("</td></tr>\n");
		sb.append("<tr><td>Patient:</td><td>").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("</td></tr>");
		sb.append("</table><br/><br/>");
		sb.append("<a href=\"").append(SJMTrackerConstants.BASE_TRACKER_URL).append("?actionType=interaction&assignmentId=");
		sb.append(assignment.getAssignmentId()).append("&logs=true&organizationId=").append(SJMTrackerConstants.TRACKER_ORG_ID);
		sb.append("\">Click here</a> to review the original request.<br/><br/>");		
		sb.append(getStandardLetterFooter());
		this.setBody(sb);	
	}
	
	private void formatResponseReassigned() {
		this.setSubject("Patient Reassignment Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("Your assignment to patient ").append(patient.getFirstName()).append(" ");
		sb.append(patient.getLastName()).append(" has been set to \"completed\" and has been ");
		sb.append("reassigned to another ambassador.<br/><br/>");
		sb.append(getThankYouLetterFooter());
		this.setBody(sb);		
	}
	
	
	private void formatAssignmentExpired() {
		this.setSubject("Notification of Expired Assignment");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear Ambassador Manager,<br/>");
		sb.append("The following assignment has expired:<br/><br/>");
		sb.append("Ambassador: ").append(ambassador.getFirstName()).append(" ").append(ambassador.getLastName()).append("<br/>");
		sb.append("Patient: ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
		
	private void formatAssignmentDeclined() {
		this.setSubject("Confirmation of Declined Assignment");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("Thank you for considering your assignment to patient ");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(".<br/><br/>");
		sb.append("This email confirms that you have declined the assignment and that your assignment to this patient ");
		sb.append("has been deleted.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	private void formatFollowUpReminder() {
		this.setSubject("Patient Follow-Up Reminder");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("This is a reminder to follow up with your patient, ");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(" who was originally ");
		sb.append("assigned to you on ");
		String assignmentCreateDate = StringUtil.checkVal(Convert.formatDate(assignment.getAssignDate(), Convert.DATE_SLASH_PATTERN));
		sb.append(assignmentCreateDate).append("<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	public void formatRepAmbassadorReassignment(String ambassadorName) {
		this.setSubject("Patient Tracker: Ambassador Reassignment Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("	Thank you for your submission to contact a patient by the Patient Ambassador Program. ");
		sb.append("Your request was reassigned to Ambassador ");
		sb.append(ambassadorName).append(". If you requested to receive updates, you will be notified when contact ");
		sb.append("is made.  The typical turnaround time is 24-48 hours, unless otherwise indicated.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	// 1110
	private void formatSummaryCopyToFieldRep() {
		this.parsePatientResponses();
		this.setSubject("Notification: Patient Ambassador Interaction Summary");
		StringBuffer sb = new StringBuffer();
		// append the HTML header
		sb.append(this.getSummaryHtmlHeader());
		// append email-specific verbiage
		sb.append("Below is a summary of interactions between a St. Jude Medical Patient Ambassador and patient ");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(". If you have any ");
		sb.append("questions and/or concerns regarding these interactions, please contact Laura Sterling, ");
		sb.append("Patient Ambassador Program Manager.<br/><br/>");
		sb.append("<b>Patient Profile</b><br/>");
		sb.append("<table>");
		sb.append("<tr><td>Patient's Name:</td><td>");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName());
		sb.append("</td></tr>\n");
		sb.append("<tr><td>Doctor's Name (if provided):</td><td>");
		sb.append(StringUtil.checkVal(patientDataMap.get("managingDoctor"))).append("</td></tr>\n");
		sb.append("</table><br/><br/>\n");
		// append the assignment/interaction summary data
		sb.append(this.getFullSummaryHtmlBody());
		this.setBody(sb);
	}
	
	// 1115
	private void formatAlternateEmailNotification() {
		this.setSubject("SJM Ambassador Assignment Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("You have received an assignment to contact a patient on behalf of the St. Jude Medical ");
		sb.append("Patient Ambassador Program.  Please ");
		sb.append("<a href=\"").append(SJMTrackerConstants.DEFAULT_EMAIL_TYPE_ALTERNATE_LINK);
		sb.append("\">click here</a> to log into your St. Jude Medical email account to view ");
		sb.append("the request. You have 48 hours to accept or decline this assignment.  If you do not respond within 48 hours, ");
		sb.append("the patient will be reassigned to another available ambassador.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	// 1120
	private void formatSummaryCopyToTechSupport() {
		this.setSubject("URGENT: Patient Ambassador Product Complaint Notification");
		StringBuffer sb = new StringBuffer();
		// append the HTML header
		sb.append(this.getSummaryHtmlHeader());
		// append email-specific verbiage
		sb.append("Dear Tech Support,<br/><br/>");
		sb.append("The following interaction has been reported as a complaint.  Below is a summary of interactions ");
		sb.append("between a St. Jude Medical Patient Ambassador and patient ");
		sb.append(patient.getFirstName()).append(" ").append(patient.getLastName()).append(".<br/><br/>");
		sb.append("If you have any questions and/or concerns regarding this interaction, please contact Laura Sterling, ");
		sb.append("Patient Ambassador Program Manager.<br/><br/>");
		// append the patient profile header block
		sb.append(this.getPatientProfileHtmlHeader());		
		// append the assignment/interaction summary data
		sb.append(this.getFullSummaryHtmlBody());
		this.setBody(sb);
	}
	
	// 1125 - email sent to ambassador after admin has set 'request assistance' back to 'in progress'
	private void formatRequestAssistanceCompleted() {
		this.setSubject("Notification: Assignment Status Change");
		StringBuffer sb = new StringBuffer();
		sb.append("Dear ").append(ambassador.getFirstName()).append(",<br/><br/>");
		sb.append("I have reviewed your request for assistance with ").append(patient.getFirstName()).append(" ");
		sb.append(patient.getLastName()).append(". This assignment is now ready for you to complete.<br/><br/>");
		sb.append("To complete your assignment, log into the Assignment Tracker by <a href=\"");
		sb.append(SJMTrackerConstants.BASE_TRACKER_URL).append("\">clicking here</a>.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);		
	}
	
	// 1130 - email of copy of field form submittal to SJM team member
	private void formatFieldFormSubmissionCopy() {
		this.setSubject("SalesNet - Patient Ambassador Request Form Submission");
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>");
		sb.append("<title>Patient Tracker Notification</title>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\" />");
		sb.append("<meta http-equiv=\"content-language\" content=\"en\" />");
		sb.append("<meta name=\"description\" content=\"Patient Tracker notification email\" />");
		sb.append("<meta name=\"keywords\" content=\"Patient Tracker notification\" />");
		sb.append("</head>");
		sb.append("<body style=\"background-color: #fff;\">");
		sb.append("<p>Dear St. Jude team member,</p>");
		sb.append("<p>A field representative has submitted a Patient Ambassador Request form and a copy of the ");
		sb.append("submission is listed below.</p>");
		sb.append("<table style=\"border:solid 1px black;\">");
		sb.append("<tr><th colspan='3'>").append("Patient Ambassador Request Form (Field) Submission").append("</th></tr>");
		
		fields = contactData.getFields();
		Map<String, String> data = contactData.getData().get(0).getExtData();
		
		sb.append("<tr style=\"background:").append("#C0D2EC");
		sb.append(";\"><td style=\"width: 575px; padding-right:10px;\" valign=\"top\">").append("First Name");
		sb.append("</td><td>").append(patient.getFirstName()).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append("#E1EAFE");
		sb.append(";\"><td style=\"padding-right:10px;\" valign=\"top\">").append("Middle Initial");
		sb.append("</td><td>").append(patient.getMiddleName()).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append("#C0D2EC");
		sb.append(";\"><td style=\"padding-right:10px;\" valign=\"top\">").append("Last Name");
		sb.append("</td><td>").append(patient.getLastName()).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append("#E1EAFE");
		sb.append(";\"><td style=\"padding-right:10px;\" valign=\"top\">").append("State");
		sb.append("</td><td>").append(patient.getState()).append("&nbsp;</td></tr>");
		
		int x=0;
		for (String key : fields.keySet()) {
			String color = ((x++ % 2) == 0) ? "#C0D2EC" : "#E1EAFE";
			String questionNm = StringUtil.replace(fields.get(key),"#hide#","");
			String value = StringUtil.checkVal(data.get(key));
			sb.append("<tr style=\"background:").append(color);
			sb.append(";\"><td style=\"padding-right:10px;\" valign=\"top\">").append(questionNm);
			sb.append("</td><td>").append(value).append("&nbsp;</td></tr>");
		}
		sb.append("</table>");
		sb.append("<br>");
		sb.append(getStandardLetterFooter());
		sb.append("</body></html>");
		this.setBody(sb);
	}
	
	// 1135
	public void formatRepAmbassadorAssignment() {
		this.setSubject("Patient Tracker: Ambassador Assignment Notification");
		StringBuffer sb = new StringBuffer();
		sb.append("	Thank you for your submission to contact a patient by the Patient Ambassador Program.  ");
		sb.append("If you requested to receive updates, you will be notified when contact is made.  ");
		sb.append("The typical turnaround time is 24-48 hours, unless otherwise indicated.<br/><br/>");
		sb.append(getStandardLetterFooter());
		this.setBody(sb);
	}
	
	//1140
	public void formatNewAssignmentCopySJMTeam() {
		this.parsePatientResponses();
		this.setSubject("Patient Ambassador Request Form Submission");
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>");
		sb.append("<title>Patient Tracker New Assignment Notification</title>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\" />");
		sb.append("<meta http-equiv=\"content-language\" content=\"en\" />");
		sb.append("<meta name=\"description\" content=\"Patient Tracker notification email\" />");
		sb.append("<meta name=\"keywords\" content=\"Patient Tracker notification\" />");
		sb.append("</head>");
		sb.append("<body style=\"background-color: #fff;\">");
		sb.append("<p>Dear St. Jude team member,</p>");
		sb.append("<p>The St. Jude Medical Patient Ambassador Program Manager has submitted a ");
		sb.append("Patient Ambassador Request form and a copy of the submission is listed below.</p>");
		sb.append("<table style=\"border:solid 1px black;\">");
		sb.append("<tr><th colspan='3'>").append("Patient Ambassador Request Form (Admin) Submission").append("</th></tr>");
		
		String codeBlue = "#C0D2EC";
		String codeWhite = "#E1EAFE";
		String tdBlock = ";\"><td style=\"padding-right:10px;\" valign=\"top\">";
		
		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(";\"><td style=\"width: 575px; padding-right:10px;\" valign=\"top\">").append("First Name");
		sb.append("</td><td>").append(patient.getFirstName()).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Middle Initial");
		sb.append("</td><td>").append(StringUtil.checkVal(patient.getMiddleName())).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Last Name");
		sb.append("</td><td>").append(patient.getLastName()).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("State");
		sb.append("</td><td>").append(patientDataMap.get("state")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Preferred Method of Contact");
		sb.append("</td><td>").append(patientDataMap.get("contactMethod")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Phone");
		sb.append("</td><td>").append(patient.getMainPhone()).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Alternative Phone Number");
		sb.append("</td><td>").append(patientDataMap.get("altPhoneNumber")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Email Address");
		sb.append("</td><td>").append(StringUtil.checkVal(patient.getEmailAddress())).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Assign Patient to Specific Ambassador");
		sb.append("</td><td>").append(patientDataMap.get("ambRequested")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Patient's Location of Pain");
		sb.append("</td><td>").append(patientDataMap.get("pain")).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Preferred Gender of Ambassador");
		sb.append("</td><td>").append(patientDataMap.get("ambGenderPreferred")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Patient's Age Range");
		sb.append("</td><td>").append(patientDataMap.get("age")).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Most Important Factor About Ambassador");
		sb.append("</td><td>").append(patientDataMap.get("matchFactor")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Best Time of Day for Contact");
		sb.append("</td><td>").append(patientDataMap.get("bestContactTime")).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Doctor's Name, If Applicable");
		sb.append("</td><td>").append(patientDataMap.get("managingDoctor")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Is Interaction Shareable With Patient's Physician?");
		sb.append("</td><td>").append(patientDataMap.get("shareInfo")).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Current Phase of SCS Treatment Continuum");
		sb.append("</td><td>").append(patientDataMap.get("treatmentPhase")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Urgency of Request");
		sb.append("</td><td>").append(StringUtil.checkVal(attributes.get("urgencyRequest"))).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Specific Questions for Ambassador");
		sb.append("</td><td>").append(patientDataMap.get("ambSpecificQuestion")).append("&nbsp;</td></tr>");
		
		sb.append("<tr style=\"background:").append(codeWhite);
		sb.append(tdBlock).append("Email address of patient's SJM representative");
		sb.append("</td><td>").append(patientDataMap.get("patientSJMRepEmail")).append("&nbsp;</td></tr>");

		sb.append("<tr style=\"background:").append(codeBlue);
		sb.append(tdBlock).append("Email address of SJM team member to copy on this request");
		sb.append("</td><td>").append(attributes.get("teamMemberEmailAddress")).append("&nbsp;</td></tr>");

		sb.append("</table>");
		sb.append("<br>");
		sb.append(getStandardLetterFooter());
		sb.append("</body></html>");
		this.setBody(sb);
	}

	protected StringBuffer getSummaryHtmlHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>");
		sb.append("<title>Patient Tracker Notification</title>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\" />");
		sb.append("<meta http-equiv=\"content-language\" content=\"en\" />");
		sb.append("<meta name=\"description\" content=\"Patient Tracker notification email\" />");
		sb.append("<meta name=\"keywords\" content=\"Patient Tracker notification\" />");
		sb.append("<link href=\"http://amb.sb.siliconmtn.com/binary/themes/CUSTOM/SJM/AMB_COMM/scripts/modules.css\" type=\"text/css\" rel=\"stylesheet\">");
		sb.append("<link href=\"http://amb.sb.siliconmtn.com/sb/script/AMB_COMM/SJM_AMBASSADORS_1_2.css\" type=\"text/css\" rel=\"stylesheet\">");
		sb.append("</head>");
		sb.append("<body style=\"background-color: #fff;\">");
		sb.append("<div style=\"width: 715px; padding: 25px;\">");
		return sb;
	}
	
	protected StringBuffer getPatientProfileHtmlHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table class=\"pHeaderTable\" border=\"0\"><tr>");
		sb.append("<td class=\"ambTitle\">Patient Profile:&nbsp;</td>");
		sb.append("<td colspan=\"2\" class=\"ambPatientTitle\">");
		sb.append(patient.getLastName()).append(", ").append(patient.getFirstName());
		sb.append("</td></tr></table>");
		sb.append("<table class=\"adminManagerTable\" border=\"0\">");
		sb.append("<tr class=\"greyHeader\"><td colspan=\"5\" style=\"padding: 0 0 0 5px;\">Current Status</td></tr>");
		sb.append("<tr class=\"adminManagerTableHeader\">");
		sb.append("<td style=\"width: 177px; padding: 0 0 0 5px;\">Ambassador</td>");
		sb.append("<td style=\"width: 160px;\">Patient</td>");
		sb.append("<td style=\"width: 120px;\">Date Accepted</td>");
		sb.append("<td style=\"width: 100px;\">Status</td>");
		sb.append("<td>Review Needed</td></tr>");
		sb.append("<tr><td style=\"width: 173px; padding: 0 0 0 5px;\">");
		sb.append(ambassador.getLastName()).append(", ").append(ambassador.getFirstName()).append("</td>");
		sb.append("<td style=\"width: 160px;\">").append(patient.getLastName()).append(", ").append(patient.getFirstName()).append("</td>");
		sb.append("<td style=\"width: 122px;\">");
		sb.append(Convert.formatDate(assignment.getAcceptDate(), Convert.DATE_SLASH_PATTERN)).append("</td>");
		sb.append("<td style=\"width: 132px;\">").append(this.getTypeName(assignment.getAssignmentStatusId())).append("</td>");
		sb.append("<td>&nbsp;</td></tr></table><br/>");		
		return sb;
	}
	
	protected StringBuffer getFullSummaryHtmlBody() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table style=\"border-collapse: collapse; border: solid 1px #d4d4d4;\">");
		sb.append("<tr><td>");
		sb.append("<div id=\"viewHistory\">");
		sb.append("<table class=\"ambFormTable\" border=\"0\" style=\"width: 715px;\">");
		sb.append("<tr><td class=\"ambFieldSpacerNarrow\">&nbsp;</td><td colspan=\"2\" class=\"ambFormTableHeader\">INTERACTION SUMMARY</td></tr>");
		if (container.getInteractions() != null && ! container.getInteractions().isEmpty()) {
			int aCount = 0;
			for(PatientInteractionVO ivo : container.getInteractions()) {
				for (AssignmentVO avo : container.getAssignments()) {
					if (ivo.getAssignmentId().equals(avo.getAssignmentId())) {
						aCount++;
						if (aCount > 1) sb.append("<tr><td colspan=\"3\">&nbsp;</td></tr>");
						sb.append("<tr>");
						sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
						sb.append("<td class=\"ambFormFieldLeft\">Interaction Date</td>");
						sb.append("<td class=\"ambFormFieldRight\">");
						sb.append(ivo.getTransaction().getCustomData().get("c0a802376667680bf183e3a0a8e46e24").getResponses().get(0));
						sb.append("</td></tr>");
						sb.append("<tr>");
						sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
						sb.append("<td class=\"ambFormFieldLeft\">Method of Interaction</td>");
						sb.append("<td class=\"ambFormFieldRight\">");
						sb.append(ivo.getTransaction().getCustomData().get("c0a80237666be04d71bc888acf98847c").getResponses().get(0));
						sb.append("</td>");
						sb.append("</tr>");
						sb.append("<tr>");
						sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
						sb.append("<td class=\"ambFormFieldLeft\">Duration of Interaction (Minutes)</td>");
						sb.append("<td class=\"ambFormFieldRight\">");
						if (ivo.getTransaction().getCustomData().get("c0a802375f992483764c41aada43daa").getResponses().get(0) != null) {
							sb.append(ivo.getTransaction().getCustomData().get("c0a802375f992483764c41aada43daa").getResponses().get(0));
						} else {
							sb.append("not specified");
						}
						sb.append("</td>");
						sb.append("</tr>");
						sb.append("<tr>");
						sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
						sb.append("<td class=\"ambFormFieldLeft\">Was this a product complaint?</td>");
						sb.append("<td class=\"ambFormFieldRight\">");
						sb.append(ivo.getTransaction().getCustomData().get("c0a802376668dcb46e565486647d81d2").getResponses().get(0));
						sb.append("</tr>");
						sb.append("<tr>");
						sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
						sb.append("<td class=\"ambFormFieldLeft\">Interaction Summary</td>");
						sb.append("<td class=\"ambFormFieldRight\">");
						String intSummary = ivo.getTransaction().getCustomData().get("c0a802376667f35a1ced22dabaacd93b").getResponses().get(0);
						intSummary.replace("&amp;#39;", "'");
						sb.append(intSummary).append("</td></tr>");
					}
				}
			}
		} else {
			sb.append("<tr><td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
			sb.append("<td colspan=\"2\" class=\"ambFormFieldLeft\">No interactions with this patient have been documented.</td>");
			sb.append("</tr>");
		}

		sb.append("<tr><td colspan=\"3\">&nbsp;</td></tr>");
		sb.append("<tr><td class=\"ambFieldSpacerNarrow\">&nbsp;</td><td colspan=\"2\" class=\"ambFormTableHeader\">ASSIGNMENT SUMMARY</td></tr>");

		if (assignment.getAssignmentLog() !=null && ! assignment.getAssignmentLog().isEmpty()) {
			for (AssignmentLogVO alvo : assignment.getAssignmentLog()) {
				sb.append("<tr >");
				sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
				sb.append("<td class=\"ambFormFieldLeft\" style=\"width: 300px;\">Update Date</td>");
				sb.append("<td class=\"ambFormFieldRight\">");
				sb.append(Convert.formatDate(alvo.getCreateDate(), Convert.DATE_SLASH_PATTERN));
				sb.append("</td>");
				sb.append("</tr>");
				String logResponse = this.retrieveLogResponse(alvo, "c0a80241a58b0bd7db29c376a9280740");
				if (logResponse != null) {
					// try to convert the status ID String to an Integer val and then to a type name
					Integer statusVal = null;
					try {
						statusVal = Integer.valueOf(logResponse);
					} catch (NumberFormatException nfe) {
						// leave statusVal equal to null
					}
					String statusText = "Status changed to";
					if (statusVal != null && statusVal.equals(SJMTrackerConstants.STATUS_PENDING)) {
						statusText = "Status set to";
					}
					sb.append("<tr>");
					sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
					sb.append("<td class=\"ambFormFieldLeft\">").append(statusText).append("</td>");
					sb.append("<td class=\"ambFormFieldRight\">");
					if (statusVal != null) {
						sb.append(this.getTypeName(statusVal));
					} else {
						sb.append(logResponse);
					}
					sb.append("</td></tr>");
				}
				logResponse = this.retrieveLogResponse(alvo, "c0a80241a58bee48c14c0605232cf5ff");
				if (logResponse != null) {
					sb.append("<tr>");
					sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
					if (logResponse.contains("SYSTEM")) {
						sb.append("<td class=\"ambFormFieldLeft\">Follow-up date</td>");
						sb.append("<td class=\"ambFormFieldRight\">Follow-up notification sent, date reset.</td>");
					} else if (logResponse.contains("USER_RESET")) {
						sb.append("<td class=\"ambFormFieldLeft\">Follow-up date reset by ambassador.</td>");
						sb.append("<td class=\"ambFormFieldRight\">&nbsp;</td>");
					} else {
						sb.append("<td class=\"ambFormFieldLeft\">Follow-up date set to</td>");
						sb.append("<td class=\"ambFormFieldRight\">").append(logResponse).append("</td>");
					}
					sb.append("</tr>");
				}
				logResponse = this.retrieveLogResponse(alvo, "c0a80241fbb9dab5895d7d534c79a56");
				if (logResponse != null) {
					sb.append("<tr>");
					sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
					if (logResponse.contains("USER_RESET")) {
						sb.append("<td class=\"ambFormFieldLeft\">Follow-up notes reset by ambassador.</td>");
						sb.append("<td class=\"ambFormFieldRight\">&nbsp;</td>");
					} else {
						sb.append("<td class=\"ambFormFieldLeft\">Follow-up notes set to</td>");
						sb.append("<td class=\"ambFormFieldRight\">").append(logResponse).append("</td>");
					}
					sb.append("</tr>");
				}
				logResponse = this.retrieveLogResponse(alvo, "c0a80241a58b9b378623001ecb1cddf4");
				if (logResponse != null) {
					sb.append("<tr>");
					sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
					sb.append("<td class=\"ambFormFieldLeft\">Added Notes</td>");
					sb.append("<td class=\"ambFormFieldRight\">");
					sb.append(logResponse.replace("&amp;#39;","'"));
					sb.append("</td>");
					sb.append("</tr>");
				}
				logResponse = this.retrieveLogResponse(alvo, "c0a80241a58c3df6debb45c9b84dc3");
				if (logResponse != null) {
					sb.append("<tr>");
					sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
					sb.append("<td class=\"ambFormFieldLeft\">System Notes</td>");
					sb.append("<td class=\"ambFormFieldRight\">");
					sb.append(logResponse.replace("&amp;#39;","'"));
					sb.append("</td>");
					sb.append("</tr>");
				}
				sb.append("<tr>");
				sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
				sb.append("<td class=\"ambFormFieldLeft\">&nbsp;</td>");
				sb.append("<td class=\"ambFormFieldRight\">&nbsp;</td>");
				sb.append("</tr>");
			}
		} else {
			sb.append("<tr>");
			sb.append("<td class=\"ambFieldSpacerNarrow\">&nbsp;</td>");
			sb.append("<td class=\"ambFormFieldLeft\">Assignment created</td>");
			sb.append("<td class=\"ambFormFieldRight\">");
			sb.append(Convert.formatDate(assignment.getCreateDate(), Convert.DATE_SLASH_PATTERN));
			sb.append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("</div>");
		sb.append("</td></tr></table>");
		sb.append("<br/><br/>");
		sb.append(getThankYouLetterFooter());
		sb.append("</div>");
		sb.append("</body></html>");
		return sb;
	}
	
	/**
	 * Formats additional 'new assignment' information needed for the 'new assignment' emails.
	 * @return
	 */
	protected String getNewAssignmentAdditionalInformation() {
		this.parsePatientResponses();
		PhoneNumberFormat phf = new PhoneNumberFormat();
		phf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
		StringBuffer sb = new StringBuffer();
		sb.append("Additional Information About this Assignment");
		sb.append("<table border=\"1\">");
		sb.append("<tr><td colspan=\"2\"><b>Patient Information</b></td></tr>\n");
		sb.append("<tr><td>Name:</td><td>").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("</td></tr>\n");
		sb.append("<tr><td>Age Range:</td><td>").append(StringUtil.checkVal(patientDataMap.get("age"))).append("</td></tr>\n");
		sb.append("<tr><td>Location of Pain:</td><td>").append(StringUtil.checkVal(patientDataMap.get("pain"))).append("</td></tr>\n");
		sb.append("<tr><td>State:</td><td>").append(StringUtil.checkVal(patientDataMap.get("state"))).append("</td></tr>");
		sb.append("<tr><td>Most Important Match Factor to the Patient:</td><td>");
		sb.append(StringUtil.checkVal(patientDataMap.get("matchFactor"), "Unknown"));
		sb.append("</td></tr>");
		sb.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
		sb.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
		sb.append("<tr><td colspan=\"2\"><b>Contact Information</b></td></tr>\n");
		sb.append("<tr><td>Preferred Method of Contact:</td><td>").append(StringUtil.checkVal(patientDataMap.get("contactMethod"))).append("</td></tr>\n");
		sb.append("<tr><td>Email Address:</td><td>").append(StringUtil.checkVal(patient.getEmailAddress(), "n/a")).append("</td></tr>\n");
		sb.append("<tr><td>Phone Number:</td><td>");
		if (StringUtil.checkVal(patient.getMainPhone()).length() > 0) {
			phf.setPhoneNumber(patient.getMainPhone());
			sb.append(phf.getFormattedNumber());
		} else {
			sb.append("- none specified -");
		}
		sb.append("</td></tr>\n");
		sb.append("<tr><td>Alternative Phone Number:</td><td>");
		if (StringUtil.checkVal(patientDataMap.get("altPhoneNumber"), "n/a").equals("n/a")) {
			sb.append("n/a");
		} else {
			phf.setPhoneNumber(patientDataMap.get("altPhoneNumber"));
			sb.append(phf.getFormattedNumber());
		}
		sb.append("</td></tr>\n");
		sb.append("<tr><td>Best Time to Contact Patient:</td><td>").append(StringUtil.checkVal(patientDataMap.get("bestContactTime"), "n/a")).append("</td></tr>");
		sb.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
		sb.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
		sb.append("<tr><td colspan=\"2\"><b>Additional Information</b></td></tr>\n");
		
		Map<String, String> cVals = this.retrievePatientContactValues();
		
		sb.append("<tr><td>Summary of Comments</td><td>");
		// retrieve summary from form response or from assignment notes.
		String summary = cVals.get("summary");
		if (StringUtil.checkVal(summary).length() == 0) summary = "No summary available.";
		sb.append(summary).append("</td></tr>");
		//Doctor's name
		sb.append("<tr><td>Doctor's Name (if provided):</td><td>").append(cVals.get("doctor")).append("</td></tr>");
		// share interaction data with doctor?
		sb.append("<tr><td>Can you share information about this interaction with the patient's pain management doctor?</td><td>");
		sb.append(cVals.get("shareInfo")).append("</td></tr>");
		// Urgency?
		sb.append("<tr><td>Urgency of Request:</td><td>").append(cVals.get("urgency")).append("</td></tr>");
		// Requested by (based on form submitted);
		sb.append("<tr><td>Requested By:</td><td>").append(cVals.get("requestedBy")).append("</td></tr>");
		sb.append("</table><br/><br/>");
		return sb.toString();
	}
	
	protected String getStandardLetterFooter() {
		StringBuffer sb = new StringBuffer();
		sb.append("Sincerely,<br/><br/>The St. Jude Medical Patient Ambassador Program Manager");
		sb.append("<br/><br/>");
		sb.append(getStandardDisclaimer());
		return sb.toString();
	}
	
	private String getThankYouLetterFooter() {
		StringBuffer sb = new StringBuffer();
		sb.append("Thank you,<br/><br/>The St. Jude Medical Patient Ambassador Program Manager");
		sb.append("<br/><br/>");
		sb.append(getStandardDisclaimer());
		return sb.toString();
	}
	
	private String getStandardDisclaimer() {
		StringBuffer sb = new StringBuffer();
		sb.append("<p style=\"color: #414141; font-size: 12px; font-style: italic;\">This communication, including any attachments, may contain information that is ");
		sb.append("proprietary, privileged, confidential or legally exempt from disclosure. If you are not a named ");
		sb.append("addressee, you are hereby notified that you are not authorized to read, print, retain a copy ");
		sb.append("of or disseminate any portion of this communication without the consent of the sender and that ");
		sb.append("doing so may be unlawful. If you have received this communication in error, please immediately ");
		sb.append("notify the sender via return e-mail and delete it from your system.</p>");
		return sb.toString();
	}
		
	private void parseContainer(TrackerDataContainer tdc) {
		if (tdc.getAssignments() != null && ! tdc.getAssignments().isEmpty()) {
			assignment = tdc.getAssignments().get(0);
		}
		if (tdc.getAssignees() != null && ! tdc.getAssignees().isEmpty()) {
			ambassador = tdc.getAssignees().get(0);
		}
		if (tdc.getPatients() != null && ! tdc.getPatients().isEmpty()) {
			patient = tdc.getPatients().get(0);
		}
		if (tdc.getInteractions() != null && ! tdc.getInteractions().isEmpty()) {
			interaction = tdc.getInteractions().get(0);
		}
		if (tdc.getForm() != null) form = tdc.getForm();
		if (tdc.getContactForm() != null) contactForm = tdc.getContactForm();
		if (tdc.getContactData() != null) contactData = tdc.getContactData();
	}
	
	/**
	 * Map of comparison attributes and their corresponding form field IDs
	 */
	protected void loadPatientFieldMap() {
		patientFieldMap = new LinkedHashMap<String, String>();
		patientFieldMap.put("state", "c0a80241ad8ec5b1902f1fc5b9f6ce42");
		patientFieldMap.put("contactMethod", "c0a8023766250c46418ec22b852e7838");
		patientFieldMap.put("altPhoneNumber", "c0a8023766379ebc9feb6e1ae7f358f7");
		patientFieldMap.put("ambRequested", "c0a802376626c3b5588a4c246c0cb039");
		patientFieldMap.put("pain","c0a8023766282af6d33fce93e7d2d285");
		patientFieldMap.put("ambGenderPreferred", "c0a8023766290edfaabaeaee10fd075c");		
		patientFieldMap.put("age", "c0a80237662b11dbb5c0326460296ed1");
		patientFieldMap.put("matchFactor", "c0a80237662d0a7775f2231c9c7c3743");
		patientFieldMap.put("bestContactTime", "c0a80237665cd05bce9bc8c2b200318a");
		patientFieldMap.put("managingDoctor", "c0a80237662fef282f3c4e368846f14c");
		patientFieldMap.put("shareInfo", "c0a802376630a3a7ad52730e42c72ac3");
		patientFieldMap.put("treatmentPhase", "c0a802376636bc5a8422a4fbc1157895");		
		patientFieldMap.put("gender", "7f000001537b18842a834a598cdeafa");
		patientFieldMap.put("ambSpecificQuestion", "c0a802376631104dd8f9dec9ad1e83a8");		
		patientFieldMap.put("patientSJMRepEmail", "c0a802378d49c279918a2402e41e03c0");
	}
	
	/**
	 * Parses patient attributes (response data) into a map
	 */
	private void parsePatientResponses() {
		FormTransactionVO ft = patient.getTransaction();
		if (ft != null) {
			Map<String, FormFieldVO> fMap = ft.getCustomData();
			if (fMap != null && ! fMap.isEmpty()) {
				// loop the fieldMap
				String[] value = null;
				for (String key : patientFieldMap.keySet()) {
					if (fMap.get(patientFieldMap.get(key)) != null) {
						FormFieldVO keyField = fMap.get(patientFieldMap.get(key));
						if (keyField.getResponses() != null && ! keyField.getResponses().isEmpty()) {
							value = keyField.getResponses().toArray(new String[] {});
							patientDataMap.put(key, StringUtil.getDelimitedList(value, false, ","));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the interaction summary.
	 * @return
	 */
	private String retrieveInteractionSummary() {
		String summary = null;
		if (interaction != null && interaction.getTransaction() != null) {
			log.debug("retrieving interaction summary");
			if (interaction.getTransaction().getCustomData() != null) {
				if (! interaction.getTransaction().getCustomData().isEmpty()) {
					FormFieldVO mf = interaction.getTransaction().getCustomData().get(SJMTrackerConstants.INTERACTION_SUMMARY_FIELD_ID);
					if (mf != null) {
						if (mf.getResponses() != null) {
							if (! mf.getResponses().isEmpty()) {
								summary = mf.getResponses().get(0);
							}
						}
					}
				}
			}
		} 
		return summary;
	}
	
	/**
	 * Returns a map of certain patient contact form values for use in 'new assignment' emails.
	 * @return
	 */
	private Map<String, String> retrievePatientContactValues() {
		Map<String, String> cVals = new HashMap<String, String>();
		String doctor = "n/a";
		String shareInfo = "n/a";
		String urgency = "Normal—to be completed within 3-5 business days";
		String summary = "n/a";
		String requestedBy = "n/a";
		if (patient.getPatientSourceFormId() != null) {
			// patient was created via a Contact Us form
			// retrieve the form data
			if (contactForm != null) {
				String formId = contactForm.getActionId();
				log.debug("patient's source contact form actionId: " + formId);
				List<ContactDataModuleVO> data = contactData.getData();
				if (data != null) {
					log.debug("list size is " + data.size());
					if (! data.isEmpty()) {
						ContactDataModuleVO vo = data.get(0);
						if (vo != null) {
							if (vo.getExtData() != null && ! vo.getExtData().isEmpty()) {
								if(formId.equals("c0a80241203d1bbac210bd5923703429")) { // POYP
									doctor = StringUtil.checkVal(vo.getExtData().get("c0a80241a67319f124069def29053020")) + " " 
											+ StringUtil.checkVal(vo.getExtData().get("c0a802373bb5f6983157b5a79f3487"));
									if (doctor.trim().length() == 0) doctor = "None specified";
									shareInfo = StringUtil.checkVal(vo.getExtData().get("c0a8024120560922764fb70a08e934"));
									if (shareInfo.length() == 0) shareInfo = "No";
									summary = vo.getExtData().get("c0a802412056828f448e263bd09b9754");
									requestedBy = "Patient";
								} else if (formId.equals("c0a80237aebf15939fc972a3f8b15540")) { // SalesNet CS form
									doctor = StringUtil.checkVal(vo.getExtData().get("c0a8024120f1d7ba2c6bbd09581ab092")) + " " 
											+ StringUtil.checkVal(vo.getExtData().get("c0a802373bb3622f825876e29def1b3c"));
									if (doctor.trim().length() == 0) doctor = "None specified";
									shareInfo = StringUtil.checkVal(vo.getExtData().get("c0a8024120f279b3148f9bf14d3e4315"));
									if (shareInfo.length() == 0) shareInfo = "No";
									urgency = StringUtil.checkVal(vo.getExtData().get("c0a802412105ba75916efe71c9f6576"));
									if (urgency.length() == 0) urgency = "Normal—to be completed within 3-5 business days";
									summary = vo.getExtData().get("c0a802412106e1fce34ca96fe26ae5a4");
									requestedBy = "Customer Service";
								} else if (formId.equals("c0a8024121202bfe526d209c678fdd5b")) { // SalesNet Field Rep form
									doctor = "n/a";
									shareInfo = "n/a";
									urgency = StringUtil.checkVal(vo.getExtData().get("c0a802412105ba75916efe71c9f6576"));
									if (urgency.length() == 0) urgency = "Normal—to be completed within 3-5 business days";
									summary = vo.getExtData().get("c0a802412106e1fce34ca96fe26ae5a4");
									requestedBy = "SJM Representative";
								}
							}
						} 
					} 
				} 
			}
		} else {
			if (patientDataMap != null && ! patientDataMap.isEmpty()) {
				doctor = patientDataMap.get("managingDoctor");
				shareInfo = patientDataMap.get("shareInfo");
				if (StringUtil.checkVal(attributes.get("urgencyRequest")).length() > 0) {
					urgency = StringUtil.checkVal(attributes.get("urgencyRequest"));	
				}
				summary = patientDataMap.get("ambSpecificQuestion");
			}
		}
		if (summary == null) {
			if (assignment != null) summary = assignment.getAssignmentNotes();
		}
		cVals.put("doctor", doctor);
		cVals.put("shareInfo", shareInfo);
		cVals.put("urgency", urgency);
		cVals.put("summary", summary);
		cVals.put("requestedBy", requestedBy);
		return cVals;
	}
	
	/**
	 * Safely unpacks an assignment's assignment logs and retrieves the response value
	 * for the specified field ID.
	 * @param logVO
	 * @param fieldId
	 * @return
	 */
	private String retrieveLogResponse(AssignmentLogVO logVO, String fieldId) {
		String response = null;
		if (logVO.getTransaction() != null) {
			if (logVO.getTransaction().getCustomData() != null) {
				if (! logVO.getTransaction().getCustomData().isEmpty()) {
					FormFieldVO mf = logVO.getTransaction().getCustomData().get(fieldId);
					if (mf != null) {
						if (mf.getResponses() != null) {
							if (! mf.getResponses().isEmpty()) {
								response = mf.getResponses().get(0);
							}
						}
					} 
				} 
			} 
		}
		return response;
	}
	
	/**
	 * Overloaded method that formats and sends email based on data container and email type passed in
	 * @param emailType
	 * @param tdc
	 * @throws MailException
	 */
	public void sendEmail(TrackerDataContainer tdc, Integer emailType) throws MailException {
		this.setContainer(tdc);
		this.setType(emailType);
		this.sendEmail();
	}
	
	/**
	 * Overloaded method that sends email based on data already set on mail object
	 * @throws MailException
	 */
	public void sendEmail() throws MailException {
		// if there is no data container of if the email type is not set, return
		if (type == 0) return;
		this.formatEmail();
		
		log.debug("sending email...");		
		if (log.isDebugEnabled()) {
			log.debug("\n********** START of MESSAGE **********");
			log.debug("\nfrom: " + this.getFromSender());
			log.debug("\nto: " + StringUtil.getDelimitedList(this.getRecipients(), false, ","));
			log.debug("\nsubject: " + this.getSubject());
			log.debug("\nbody: " + this.getBody());
			log.debug("\n**********  END of MESSAGE **********");
		}

		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setRecpt(recipients);
		mail.setSubject(this.getSubject());
		mail.setFrom(this.getFromSender());
		mail.setHtmlBody(this.getBody().toString());
		mail.postMail();
		mail = null;
	}
	
	/**
	 * Overloaded method that sends email based on data already set on mail object using
	 * SMTP properties specified by a properties file
	 * @throws MailException
	 */
	public void sendEmail(Properties props) throws MailException {
		// if there is no data container of if the email type is not set, return
		if (type == 0) return;
		this.formatEmail();
		
		log.debug("sending email...");	
		if (log.isDebugEnabled()) {
			log.debug("\n********** START of MESSAGE **********");
			log.debug("\nfrom: " + this.getFromSender());
			log.debug("\nto: " + StringUtil.getDelimitedList(this.getRecipients(), false, ","));
			log.debug("\nsubject: " + this.getSubject());
			log.debug("\nbody: " + this.getBody());
			log.debug("\n**********  END of MESSAGE **********");
		}
		
		SMTMail mail = new SMTMail(props.getProperty(Constants.CFG_SMTP_SERVER));
		mail.setUser(props.getProperty(Constants.CFG_SMTP_USER));
		mail.setPassword(props.getProperty(Constants.CFG_SMTP_PASSWORD));
		mail.setRecpt(recipients);
		mail.setSubject(this.getSubject());
		mail.setFrom(this.getFromSender());
		mail.setHtmlBody(this.getBody().toString());
		mail.postMail();
		mail = null;
	}
	
	private String getTypeName(Integer type) {
		switch(type.intValue()) {
		case 10: return "Pending";
		case 30: return "In Progress";
		case 40: return "Request Assistance";
		case 50: return "Request Reassignment";
		case 60: return "Complaint";
		case 70: return "Dr. Name";
		case 100: return "Completed";
		case 900: return "Expired";
		case 910: return "Reassigned";
		case 999: return "Closed";
		default: return "n/a";
		}
	}
	
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the fromSender
	 */
	public String getFromSender() {
		return fromSender;
	}

	/**
	 * @param fromSender the fromSender to set
	 */
	public void setFromSender(String fromSender) {
		this.fromSender = fromSender;
	}

	public StringBuffer getBody() {
		return body;
	}

	public void setBody(StringBuffer body) {
		this.body = body;
	}

	public List<String> getEmailTo() {
		return emailTo;
	}

	public void setEmailTo(List<String> emailTo) {
		this.emailTo = emailTo;
	}
	
	public void addEmailTo(String to) {
		this.emailTo.add(to);
	}
	
	public String[] getRecipients() {
		return recipients;
	}

	public void setRecipients(String[] recipients) {
		this.recipients = recipients;
	}

	/**
	 * @return the fields
	 */
	public Map<String, String> getFields() {
		return fields;
	}

	/**
	 * @param fields the fields to set
	 */
	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}

	/**
	 * @return the container
	 */
	public TrackerDataContainer getContainer() {
		return container;
	}

	/**
	 * @param container the container to set
	 */
	public void setContainer(TrackerDataContainer container) {
		this.container = container;
		this.parseContainer(this.container);
	}

	/**
	 * @return the assignment
	 */
	public AssignmentVO getAssignment() {
		return assignment;
	}

	/**
	 * @param assignment the assignment to set
	 */
	public void setAssignment(AssignmentVO assignment) {
		this.assignment = assignment;
	}
	
	/**
	 * @return the interaction
	 */
	public PatientInteractionVO getInteraction() {
		return interaction;
	}

	/**
	 * @param interaction the interaction to set
	 */
	public void setInteraction(PatientInteractionVO interaction) {
		this.interaction = interaction;
	}

	/**
	 * @return the ambassador
	 */
	public AssigneeVO getAmbassador() {
		return ambassador;
	}

	/**
	 * @param ambassador the ambassador to set
	 */
	public void setAmbassador(AssigneeVO ambassador) {
		this.ambassador = ambassador;
	}

	/**
	 * @return the patient
	 */
	public PatientVO getPatient() {
		return patient;
	}

	/**
	 * @param patient the patient to set
	 */
	public void setPatient(PatientVO patient) {
		this.patient = patient;
	}

	/**
	 * @return the form
	 */
	public FormVO getForm() {
		return form;
	}

	/**
	 * @param form the form to set
	 */
	public void setForm(FormVO form) {
		this.form = form;
	}

	/**
	 * @return the contactForm
	 */
	public ContactVO getContactForm() {
		return contactForm;
	}

	/**
	 * @param contactForm the contactForm to set
	 */
	public void setContactForm(ContactVO contactForm) {
		this.contactForm = contactForm;
	}

	/**
	 * @return the contactData
	 */
	public ContactDataContainer getContactData() {
		return contactData;
	}

	/**
	 * @param contactData the contactData to set
	 */
	public void setContactData(ContactDataContainer contactData) {
		this.contactData = contactData;
	}

}
