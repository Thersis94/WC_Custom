package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Apache Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT base libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SB II libs
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.tracker.exception.AssigneeException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentException;
import com.smt.sitebuilder.action.tracker.exception.PatientException;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO.ColumnName;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
* <b>Title</b>SJMAssignmentProcessor.java<p/>
* <b>Description: Evaluates existing SJM assignments with 'Pending' status and processes each 
* according to SJM assignment business rules.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Mar 21, 2011
* <b>Changes:</b>
*  Dec 06, 2011 - Refactored code.
*  Feb 29, 2012 - Refactored code.
****************************************************************************/
public class SJMAssignmentProcessor {
	
	public static final String ORGANIZATION_ID = "SJM_AMBASSADORS";
	public static final String SITE_ID = "SJM_AMBASSADORS_1";
	public static final String LOGGING_FORM_ID = "c0a80241a58a227d3134d0df9dbe033c";
	public static final long HOURS_48_IN_MILLIS = 48*60*60*1000;
	public static final long HOURS_96_IN_MILLIS = 96*60*60*1000;
	private String lineBreak = "<br/>"; 
	
	private SMTDBConnection dbConn = null;
	private Properties props = new Properties();
	private String configPath = "scripts/ans_config.properties";
	private Logger log = Logger.getLogger("SJMAssignmentProcessor");
	
	private String dbDriver = null;
	private String dbUser = null;
	private String dbPassword = null;
	private String dbUrl = null;
	private String adminEmailFrom = null;
	private String adminEmailTo = null;
	private List<String> statusMessages = null;
	private boolean errorCondition = false;
	
	// helper members for email notifications
	private AssignmentVO newAssignment = null;
	private AssigneeVO newAssignee = null;
	private Map<String, Integer> reassignMail = null;
	private Map<String, Integer> deassignMail = null;
	
	private int expiredCount = 0;
	private int reassignedCount = 0;
	private int followUpCount = 0;
	
	private Map<String, Object> attributes = new HashMap<String, Object>();
	
	public SJMAssignmentProcessor() {
		statusMessages = new ArrayList<String>();
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		// load the config properties file or exit
		try {
			this.loadConfig();
		} catch (Exception e) {
			log.error("Error loading configuration file, exiting, ", e);
			StringBuffer msg = new StringBuffer("Could not load configuration file.  " + e.getClass());
			try {
				this.sendAdminEmail(false, msg);
			} catch (Exception e1) {
				log.error("Error sending admin email, ", e1);
			}
			System.exit(-1);
		}
		attributes.put(Constants.ENCRYPT_KEY, props.getProperty("encryptKey"));
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		SJMAssignmentProcessor ap = new SJMAssignmentProcessor();
		ap.addStatusMessage("Starting assignment processor.");
		// obtain dbConnection
		try {
			ap.getDBConnection();
		} catch (Exception e) {
			ap.exitProcessor("No database connection.," + e, true, false);
		}
		
		// retrieve the logging form for assignment logging
		FormVO logForm = null;
		try {
			logForm = ap.retrieveLoggingForm(LOGGING_FORM_ID);
		} catch (Exception e) {
			ap.exitProcessor("Could not retrieve assignment logging form., " + e, true, true);
		}
		
		// retrieve the assignment data and ambassador/patient data associated
		// with each assignment.
		List<AssignmentVO> assignments = null;
		try {
			assignments = ap.retrieveAssignments();
		} catch (AssignmentException ae) {
			ap.exitProcessor("Could not retrieve assignments., " + ae, true, true);
		}
		
		if (assignments.isEmpty()) {
			ap.exitProcessor("No assignments to process.", false, true);
		}
		
		// retrieve the admins for this run
		// retrieve admin ambassadors
		List<AssigneeVO> admins = ap.retrieveAdmins();
		if (admins == null || admins.isEmpty()) {
			ap.exitProcessor("No admins found for processing.", true, true);
		}
		/*
		int x = 1;
		if (x == 1) {
			ap.closeDBConnection();
			System.exit(-1);
		}
		*/
		// process assignments
		ap.processAssignments(assignments, admins, logForm);
		
		// email script results to the SMT admin
		ap.sendAdminEmail();
		//close db connection
		ap.closeDBConnection();
	}
	
	/**
	 * Processes the exiting of the processor for certain error conditions
	 * @param body
	 * @param errorCondition
	 */
	private void exitProcessor(String message, boolean errorCondition, boolean closeDBConnection) {
		if (closeDBConnection) {
			this.closeDBConnection();
		}
		addStatusMessage(message);
		setErrorCondition(errorCondition);
		sendAdminEmail();
		System.exit(-1);
	}
	
	/**
	 * Retrieves assignments and assignee/patient data for each assignment
	 * @return
	 * @throws AssignmentException
	 */
	public List<AssignmentVO> retrieveAssignments() throws AssignmentException {
		// retrieve assignment base records: pending with certain response codes, or with notify date of today.
		List<AssignmentVO> assignments = null;
		try {
			assignments = this.retrieveBaseAssignments();
		} catch (AssignmentException ae) {
			throw new AssignmentException(ae.getMessage());
		}
		
		// retrieve assignment 'people' data (assignees, patients, etc.)
		try {
			this.retrieveAssignmentPeopleData(assignments);
		} catch (AssigneeException ane) {
			this.addStatusMessage(ane.getMessage());
			this.setErrorCondition(true);
			throw new AssignmentException(ane.getMessage());
		} catch (PatientException pe) {
			this.addStatusMessage(pe.getMessage());
			this.setErrorCondition(true);
			throw new AssignmentException(pe.getMessage());
		}
		if (assignments == null) {
			assignments = new ArrayList<AssignmentVO>();
		}
		
		addStatusMessage("assignments retrieved: " + assignments.size());
		log.debug("assignments retrieved: " + assignments.size());		
		return assignments;
	}
	
	/**
	 * Retrieves base assignment records
	 * @return
	 * @throws AssignmentException
	 */
	public List<AssignmentVO> retrieveBaseAssignments() throws AssignmentException {
		log.debug("retrieving assignments...");
		StringBuffer sql = this.buildSQLQuery();
		List<AssignmentVO> lavo = new ArrayList<AssignmentVO>();
    	Date start = Convert.formatStartDate(new Date());
    	Calendar cal = GregorianCalendar.getInstance();
    	cal.setTime(start);
    	cal.set(Calendar.HOUR_OF_DAY, 11);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
    	Date end = cal.getTime();
		String startEndDates = "Start/End dates used for query: " + start + "/" + end;
		log.debug(startEndDates);
		addStatusMessage(startEndDates);
			
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssignmentVO avo = new AssignmentVO();
				avo.setData(rs);
				lavo.add(avo);
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving base assignment records, " + sqle);
			throw new AssignmentException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("assignments initially retrieved: " + lavo.size());
		return lavo;
	}
	
	/**
	 * Builds SQL query based on type.
	 * @param type
	 * @return
	 */
	private StringBuffer buildSQLQuery() {
		StringBuffer sql = new StringBuffer();
		sql.append("select b.* from pt_assignment b where 1 = 1 ");
		sql.append("and (");
		sql.append("(b.assignment_status_id = 10 AND ");
		sql.append("(b.assignment_response_id is null OR b.assignment_response_id in (1,130,150,160)) ");
		sql.append("OR (b.assignment_status_id < 100 AND b.notify_dt between ? AND ?)) ");
		sql.append(") order by create_dt");
		log.debug("SQL: " + sql.toString());
		return sql;
	}
	
	/**
	 * Reassigns a patient to another ambassador (not the original assignee) or to an admin.
	 * @param toReassign
	 * @throws PatientException
	 * @throws AssigneeException
	 */
	public void retrieveAssignmentPeopleData(List<AssignmentVO> assignments) 
		throws PatientException, AssigneeException {
		log.debug("retrieving 'people' data...");
		if (assignments.isEmpty()) return;
		// loop the assignments and get the assignment data
		List<String> patientIds = new ArrayList<String>();
		List<String> assigneeIds = new ArrayList<String>();
		List<String> profileIds = new ArrayList<String>();
		for (AssignmentVO a : assignments) {
			// add IDs, no duplicates
			if (! patientIds.contains(a.getPatientId())) patientIds.add(a.getPatientId());
			if (! assigneeIds.contains(a.getAssigneeId())) assigneeIds.add(a.getAssigneeId());
		}
		
		Map<String, PatientVO> patients = this.retrievePatientBaseRecords(patientIds, profileIds);
		Map<String, AssigneeVO> ambs = this.retrieveAmbassadorBaseRecords(assigneeIds, profileIds);
		// retrieve the patient/assignee transaction data
		Map<String, FormTransactionVO> patientData = retrieveData("patient", patientIds).getTransactions();
		//Map<String, FormTransactionVO> assigneeData = retrieveData("assignee", assigneeIds).getTransactions();
		
		// merge data
		this.mergePeopleData(ambs, patients, patientData, profileIds);
		this.mergeAssignmentPeopleData(assignments, ambs, patients);
	}
	
	/**
	 * Processes assignments and creates lists based on certain criteria
	 * @param assignments
	 * @param admins
	 * @throws AssignmentException
	 */
	private void processAssignments (List<AssignmentVO> assignments, List<AssigneeVO> admins, FormVO logForm) {
		log.debug("processing assignments...");
		addStatusMessage("Starting assignment processing.");
		long now = Calendar.getInstance().getTimeInMillis();
		
		for (AssignmentVO origAvo : assignments) {
			Integer responseToProcess = this.checkAssignmentProcessType(origAvo, now);
			log.debug("responseToProcess type: " + responseToProcess);
			addStatusMessage("responseToProcess type: " + responseToProcess);
			if (responseToProcess.equals(SJMTrackerConstants.EMAIL_TYPE_FOLLOW_UP)) {
				try {
					this.processFollowUpReminders(origAvo, logForm);
				} catch (AssignmentException ae) {
					// already logged message downstream, continue on.
					continue;
				} 
			} else if (responseToProcess.equals(SJMTrackerConstants.RESPONSE_NONE_48) || 
				responseToProcess.equals(SJMTrackerConstants.RESPONSE_NONE_96)) {
				// skip assignments assigned to admins
				if (origAvo.getAssignee().getTypeId() >= AssigneeManager.MIN_ADMIN_TYPE_ID) {
					log.debug("Skipping this assignment - is assigned to an admin.");
					addStatusMessage("Skipping this assignment - is assigned to an admin.");
					continue;
				}
				
				try {
					this.processExpiredAssignment(origAvo, responseToProcess, admins, logForm);
				} catch (AssignmentException e) {
					// already logged message downstream, continue on.
					continue;
				}
				// now process notifications for expired (reassigned/deassigned) assignments
				try {
					this.sendAmbassadorEmail(origAvo);
				} catch (MailException me) {
					addStatusMessage("Error sending ambassador notification email...");
					this.setErrorCondition(true);
					continue;
				}
			} else {
				log.debug("skipping this assignment...");
				addStatusMessage("Skipping this assignment...no processing required.");
			}
		}
		addStatusMessage("Finished assignment processing.");
	}
	
/**
 * Determines what type of assignment processing needs to be done.
 * @param avo
 * @param now
 * @return
 */
	private Integer checkAssignmentProcessType(AssignmentVO avo, long now) {
		// sort the assignment
		log.debug("processing assignmentId/status/response: " + avo.getAssignmentId() + "/" + avo.getAssignmentStatusId() + "/" + avo.getAssignmentResponseId());
		addStatusMessage("processing assignmentId/status/response: " + avo.getAssignmentId() + "/" + avo.getAssignmentStatusId() + "/" + avo.getAssignmentResponseId());
		Integer status = avo.getAssignmentStatusId();
		Integer responseToProcess = new Integer(0);
		switch(status) {
			case 10: { // if status 'pending', check response values
				Integer response = avo.getAssignmentResponseId();
				long assignDate = avo.getAssignDate().getTime();
				switch(response) {
					case 1: // corresponds to SJMTrackerConstants.RESPONSE_DEFAULT
					case 130: // corresponds to SJMTrackerConstants.RESPONSE_NONE_48
					case 150: // corresponds to SJMTrackerConstants.RESPONSE_ADMIN_ASSIGNED
					case 160: // corresponds to SJMTrackerConstants.RESPONSE_REASSIGNED
						if (isTimeExpired(assignDate, now, HOURS_48_IN_MILLIS)) {
							if (response.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
								// assignment has reached the 96-hour no-response threshold
								responseToProcess = SJMTrackerConstants.RESPONSE_NONE_96;
							} else {
								// assignment has reached the initial 48-hour no-response threshold
								responseToProcess = SJMTrackerConstants.RESPONSE_NONE_48;
							}
						}
						break;
					default:
						// ignore all other responses associated with a status of 10.
						break;
				} // end of inner switch
				break;
			} // end of outer case: 10
			default: // if any other status, add to follow-up list
				responseToProcess = SJMTrackerConstants.EMAIL_TYPE_FOLLOW_UP;
				break;
		} // end of outer switch
		return responseToProcess;
	}

	/**
	 * 	Processes assignments that need to be expired/reassigned due to expiration
	 * of a 48-hour or 96-hour response time limit.
	 * @param toExpire
	 * @param newResponseId
	 * @param admins
	 * @throws AssignmentException
	 */
	private void processExpiredAssignment(AssignmentVO toExpire, Integer newResponseId, 
			List<AssigneeVO> admins, FormVO logForm) throws AssignmentException {
		try {
			dbConn.setAutoCommit(false);
			this.processDeassignFrom(toExpire, newResponseId, admins, logForm);
			this.processAssignTo(toExpire, newResponseId, admins, logForm);
			dbConn.commit();
		} catch (AssignmentException ae) {
			// rollback transaction
			this.addStatusMessage("Attempting to roll back expired assignment process.");
			this.setErrorCondition(true);
			try {
				dbConn.rollback();
				log.error("...rolling back expired assignment transaction");
				this.addStatusMessage("Successfully rolled back the expired assignment process.");
			} catch (SQLException sqle) {}
			throw new AssignmentException(ae.getMessage());
		} catch (SQLException sqle) {
			log.error("Error setting autocommit on dbConn., ", sqle);
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e) { log.error("Error setting autocommit to 'true', ", e);	}
		}
	}

	/**
	 * Processes the reassignment of an 'expired' assignment.  The 'expired' assignment is
	 * reassigned either to an admin or to the next available ambassador.
	 * @param toExpire
	 * @param newResponseId
	 * @param admins
	 * @throws AssignmentException
	 */
	@SuppressWarnings("unchecked")
	private void processDeassignFrom (AssignmentVO toExpire, Integer newResponseId, 
			List<AssigneeVO> admins, FormVO logForm) throws AssignmentException {
		log.debug("starting processDeassignFrom...");
		ExpiredAssignmentProcessor eap = new ExpiredAssignmentProcessor();
		eap.setDbConn(dbConn);
		eap.setAttributes(attributes);
		eap.processExpiredAssignment(toExpire, newResponseId, admins, logForm);
		statusMessages.addAll(eap.getStatusMessages());
		if (eap.isErrorCondition()) this.setErrorCondition(true);
		if (eap.getAttributes() != null && eap.getAttributes().get("deassignEmailTo") != null) {
			deassignMail = (Map<String, Integer>) eap.getAttributes().get("deassignEmailTo");
		}
		this.expiredCount += eap.getExpiredCount();
		eap = null;
	}
	
	/**
	 * Processes the de-assignment of an 'expired' assignment.  The 'expired' assignment is
	 * de-assigned from the previously assigned ambassador.
	 * @param toReassign
	 * @param newResponseId
	 * @param ambMatch
	 * @param admins
	 * @throws AssignmentException
	 */
	@SuppressWarnings("unchecked")
	private void processAssignTo(AssignmentVO toReassign, Integer newResponseId, 
			List<AssigneeVO> admins, FormVO logForm) throws AssignmentException {
		log.debug("starting processReassignTo...");
		NewAssignmentProcessor nsm = new NewAssignmentProcessor();
		nsm.setDbConn(dbConn);
		nsm.setAttributes(attributes);
		nsm.processReassignAssignment(toReassign, newResponseId, admins, logForm);
		newAssignee = nsm.getNewAmbassador();
		newAssignment = nsm.getReAssignment();
		statusMessages.addAll(nsm.getStatusMessages());
		if (nsm.isErrorCondition()) this.setErrorCondition(true);
		if (nsm.getAttributes() != null && nsm.getAttributes().get("reassignEmailTo") != null) {
			reassignMail = (Map<String, Integer>) nsm.getAttributes().get("reassignEmailTo");
		}
		this.reassignedCount += nsm.getReassignCount();
		nsm = null;
	}
	
	/**
	 * Retrieves admin ambassador VOs
	 * @return
	 */
	private List<AssigneeVO> retrieveAdmins() {
		// retrieve admin ambassadors
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setDbConn(dbConn);
		sar.setAttributes(attributes);
		sar.setAmbassadorType(AssigneeManager.MIN_ADMIN_TYPE_ID);
		List<AssigneeVO> admins = null;
		try {
			admins = sar.retrieveAmbassadors();
		} catch (SQLException sqle) {
			log.error("Error retrieving admin ambassadors, ", sqle);
		}
		if (admins == null) admins = new ArrayList<AssigneeVO>();
		log.debug("admins retrieved: " + admins.size());
		return admins;
	}
	
	/**
	 * Processes ambassador 'follow-up' notifications.  If a 'follow-up' date of 'today' is set 
	 * on an assignment, the ambassador is sent a reminder to follow-up with the assigned patient.
	 * @param toNotify
	 * @throws AssignmentException
	 * @throws MailException
	 */
	private void processFollowUpReminders(AssignmentVO toNotify, FormVO logForm) throws AssignmentException {
		// process assignments that have a follow-up date of today
		log.debug("processing follow-up reminder...");
		// 1. send the follow-up notification email
		TrackerMailFormatter mf = new TrackerMailFormatter(SJMTrackerConstants.EMAIL_TYPE_FOLLOW_UP);
		mf.setAssignment(toNotify);
		mf.setAmbassador(toNotify.getAssignee());
		mf.setPatient(toNotify.getPatient());
		String[] to = new String[] { toNotify.getAssignee().getEmailAddress() };
		mf.setRecipients(to);
		addStatusMessage("Processing notification for assignment ID: " + toNotify.getAssignmentId());
		try {
			mf.sendEmail(props);
			log.debug("successfully sent follow-up reminder email...");
			addStatusMessage("Success: Sent follow-up notification to: " + StringUtil.getDelimitedList(to, false, ","));
		} catch (MailException me) {
			String error = "Error: Failed sending follow-up email to: " + StringUtil.getDelimitedList(to, false, ",");
			log.error(error, me);
			addStatusMessage(error);
			this.setErrorCondition(true);
		}
		
		// 2. reset the follow-up date and insert an assignment log entry
		String formId = this.retrieveFormActionId("assignmentLog");
		log.debug("formId is: " + formId);
		if (formId == null) {
			addStatusMessage("Error retrieving assignmentLog form ID value.");
			throw new AssignmentException("Error retrieving assignmentLog formId value.");
		}
		
		FollowUpProcessor sfp = null;
		try {
			dbConn.setAutoCommit(false);
			// process notify dates
			sfp = new FollowUpProcessor();
			sfp.setDbConn(dbConn);
			sfp.setAttributes(attributes);
			sfp.setToNotify(toNotify);
			sfp.setFormId(formId);
			sfp.setLogForm(logForm);
			sfp.processNotifyDates();
			statusMessages.addAll(sfp.getStatusMessages());	
			dbConn.commit();
		} catch (AssignmentException ae) {
			this.setErrorCondition(true);
			log.error("...rolling back transaction");
			this.addStatusMessage("Attempting to roll back follow-up notification process.");
			// rollback transaction
			statusMessages.addAll(sfp.getStatusMessages());
			
			try {
				dbConn.rollback();
				this.addStatusMessage("Successfully rolled back follow-up notification process.");
				log.debug("Successfully rolled back follow-up notification process.");
			} catch (SQLException sqle) {}
			throw new AssignmentException(ae.getMessage());
		} catch (SQLException sqle) {
			log.error("Error setting autocommit on dbConn., ", sqle);
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e) { log.error("Error setting autocommit to 'true', ", e);	}
		}
		// if follow-up successfully processed, set the count
		if (sfp != null) followUpCount += sfp.getFollowUpCount();
	}
	
	/**
	 * Retrieves patient base records and adds patient profile id to the profile IDs list.
	 * @param patientIds
	 * @param profileIds
	 * @return
	 * @throws PatientException
	 */
	private Map<String, PatientVO> retrievePatientBaseRecords(List<String> patientIds, List<String> profileIds) 
		throws PatientException {
		Map<String, PatientVO> patients = new HashMap<String, PatientVO>();
		StringBuffer sql = new StringBuffer();
		sql.append("select * from pt_patient where patient_id in (");
		sql.append(StringUtil.getDelimitedList(patientIds.toArray(new String[] {}), true, ","));
		sql.append(")");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PatientVO p = new PatientVO();
				p.setData(rs);
				patients.put(p.getPatientId(), p);
				profileIds.add(p.getPatientProfileId());
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving patient base records, ", sqle);
			throw new PatientException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("patients retrieved: " + patients.size());
		return patients;
	}

	/**
	 * Retrieves ambassador base records and adds ambassador profile id to the profile IDs list.
	 * @param assigneeIds
	 * @param profileIds
	 * @return
	 * @throws AssigneeException
	 */
	private Map<String, AssigneeVO> retrieveAmbassadorBaseRecords(List<String> assigneeIds, List<String> profileIds) 
		throws AssigneeException {
		Map<String, AssigneeVO> assignees = new HashMap<String, AssigneeVO>();
		StringBuffer sql = new StringBuffer();
		sql.append("select * from pt_assignee where assignee_id in (");
		sql.append(StringUtil.getDelimitedList(assigneeIds.toArray(new String[] {}), true, ","));
		sql.append(")");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssigneeVO a = new AssigneeVO();
				a.setData(rs);
				assignees.put(a.getAssigneeId(), a);
				profileIds.add(a.getAssigneeProfileId());
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving base records, ", sqle);
			throw new AssigneeException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("ambassadors retrieved: " + assignees.size());
		return assignees;
	}
	
	/**
	 * Merges profile data for ambassadors and patients.
	 * @param ambs
	 * @param patients
	 * @param profileIds
	 */
	private void mergePeopleData(Map<String, AssigneeVO> ambs, Map<String, PatientVO> patients, 
			Map<String, FormTransactionVO> patientData, List<String> profileIds) {
		// retrieve profile data
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> users = null;
		try {
			users = pm.searchProfileMap(dbConn, profileIds);
		} catch (DatabaseException de) {
			log.debug("Error retrieving ambassador and patient profile data, ", de);
		}
		
		// merge patient profile data and patient extended data
		if (users != null && ! users.isEmpty()) {
			for (String s : patients.keySet()) {
				PatientVO p = patients.get(s);
				if (p != null) {
					p.setData(users.get(p.getProfileId()).getDataMap());
					if (patientData != null && ! patientData.isEmpty()) {
						p.setTransaction(patientData.get(p.getPatientId()));
					}
				}
			}
			// merge assignee profile data
			for (String sa : ambs.keySet()) {
				AssigneeVO a = ambs.get(sa);
				if (a != null) {
					try {
						a.setData(users.get(a.getProfileId()).getDataMap());
					} catch (Exception ite) {
						// this catch only exists to catch a TargetInvocationException 
						// thrown by the setDisplayValue(String, Object) method 
						// inherited from UserDataVO.
					}
				}
			}
		}		
	}
	
	/**
	 * Merges ambassador and patient data with their associated assignment
	 * @param toReassign
	 * @param ambs
	 * @param patients
	 */
	private void mergeAssignmentPeopleData(List<AssignmentVO> toReassign, Map<String, AssigneeVO> ambs, 
			Map<String, PatientVO> patients) {
		for (AssignmentVO avo : toReassign) {
			avo.setAssignee(ambs.get(avo.getAssigneeId()));
			avo.setPatient(patients.get(avo.getPatientId()));
		}
	}
	
	/**
	 * Retrieves data based on the form type and form submittal Ids passed in.
	 * @param formType
	 * @param formSubmittalId
	 * @return
	 */
	private DataContainer retrieveData(String formType, List<String> ids) {
		DataContainer dc = new DataContainer();
		
		// lookup the specified form actionId
		String formId = this.retrieveFormActionId(formType);
		if (formId == null) return dc;
		
		// set up the query for use by the data manager
		GenericQueryVO gqv = new GenericQueryVO(formId);
		gqv.setOrganizationId(ORGANIZATION_ID);
		QueryParamVO vo = new QueryParamVO();
		vo.setColumnNm(ColumnName.FORM_SUBMITTAL_ID);
		vo.setOperator(Operator.in);
		vo.setValues(ids.toArray(new String[0]));
		gqv.addConditional(vo);
		
		// retrieve the data
		dc.setQuery(gqv);
		DataManagerFacade dfm = new DataManagerFacade(attributes, dbConn);
		dc = dfm.loadTransactions(dc);
		if (dc != null && dc.getTransactions() != null) {
			addStatusMessage(formType + " transactions retrieved: " + dc.getTransactions().size());
		}
		return dc;
	}
	
	/**
	 * Retrieves actionId for given form key
	 * @param formKey
	 * @return
	 */
	private String retrieveFormActionId(String formKey) {
		String formId = null;
		StringBuffer sql = new StringBuffer(); 
		sql.append("select form_id from pt_action_xr ");
		sql.append("where organization_id = '").append(ORGANIZATION_ID).append("' and key_nm = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, formKey);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				formId = rs.getString("form_id");
				addStatusMessage("Success: retrieved form ID for form key value of '" + formKey + "'");
			} else {
				addStatusMessage("Failure: could not find form ID for form key value of '" + formKey + "'");
				this.setErrorCondition(true);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving action ID for patient tracker form, ", sqle);
			addStatusMessage("Error: Error retrieving form ID for form key value of '" + formKey + "'");
			this.setErrorCondition(true);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		return formId;
	}
	
	/**
	 * Returns true if a date is older than the limit specified.  Otherwise returns false.
	 * @param dateToCheck
	 * @param now
	 * @param limit
	 * @return
	 */
	private boolean isTimeExpired(long dateToCheck, long now, long limit) {
		if ((now - dateToCheck) > limit) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Sends email to ambassadors/admins
	 * @param origAvo
	 * @throws MailException
	 */
	private void sendAmbassadorEmail(AssignmentVO origAvo) 
		throws MailException {
		log.debug("Sending ambassador email...");
		if (reassignMail != null && deassignMail != null) {
			if (! reassignMail.isEmpty() && ! deassignMail.isEmpty()) {
				// mail the deassignment notices first.
				TrackerMailFormatter mf = new TrackerMailFormatter();
				// send the deassigns first
				for (String key : deassignMail.keySet()) {
					this.loadMailFormatter(mf, origAvo, deassignMail.get(key));
					mf.setRecipients(new String[] { key });
					mf.sendEmail(props);
				}
				// now send the reassigns
				for (String key : reassignMail.keySet()) {
					this.loadMailFormatter(mf, origAvo, reassignMail.get(key));
					mf.setRecipients(new String[] { key });
					mf.sendEmail(props);
				}
			} else {
				addStatusMessage("ATTENTION: reassign/deassign email maps are EMPTY(not null)!");
				this.setErrorCondition(true);
			}
		} else {
			addStatusMessage("ATTENTION: reassign/deassign email maps are NULL!");
		}
	}
	
	/**
	 * Sets properties of the mail formatter based on the mailType passed in.
	 * @param mf
	 * @param origAvo
	 * @param mailType
	 */
	private void loadMailFormatter(TrackerMailFormatter mf, AssignmentVO origAvo, Integer mailType) {
		log.debug("formatting mail object for type: " + mailType);
		// common to all
		mf.setType(mailType);
		mf.setPatient(origAvo.getPatient());
		// set other properties based on the mailType
		switch(mailType.intValue()) {
		case 130:
			mf.setAmbassador(origAvo.getAssignee());
			break;
		case 140:
			mf.setAssignment(origAvo);
			break;
		case 900:
			mf.setAmbassador(origAvo.getAssignee());
			break;
		case 1100:
			mf.setAmbassador(newAssignee);
			mf.setAssignment(newAssignment);
			break;
		}
	}
		
	/**
	 * Overloaded method.  Determines whether or not errors occurred during script
	 * processing and calls overloaded method with appropriate parameters.
	 */
	private void sendAdminEmail() {
		if (errorCondition || expiredCount > 0 || reassignedCount > 0 || followUpCount > 0) {
			StringBuffer status = new StringBuffer();
			for (String s : getStatusMessages()) {
				status.append(s).append(lineBreak);
			}
			sendAdminEmail(!errorCondition, status);
		}
	}
	
	/**
	 * Sends script results email to admin
	 * @param success
	 * @param message
	 */
	private void sendAdminEmail(boolean success, StringBuffer message) {
		log.debug("sending admin email");
		StringBuffer subject = new StringBuffer("SJMAssignmentProcessor : ");
		subject.append(success ? "SUCCESS: " : "FAILURE: ");
		subject.append("expired | reassigned | followUp: ").append(" | ").append(expiredCount);
		subject.append(" | ").append(reassignedCount).append(" | ").append(followUpCount);
		try {
			SMTMail mail = new SMTMail(props.getProperty("smtpServer"), Convert.formatInteger(props.getProperty("smtpPort")), 
					props.getProperty("smtpUser"), props.getProperty("smtpPassword"));
			mail.setFrom(adminEmailFrom);
			mail.setRecpt(new String[] {adminEmailTo});
			mail.setSubject(subject.toString());
			mail.setHtmlBody(message.toString());
			mail.postMail();
			mail = null;
		} catch (MailException me) {
			log.error("Error sending admin email, ", me);
		}
	}
	
	/**
	 * Retrieves the assignment logging form.
	 * @param formActionId
	 * @return
	 * @throws Exception
	 */
	private FormVO retrieveLoggingForm(String formActionId) throws Exception {
		DataManagerFacade dmf = new DataManagerFacade(attributes, dbConn);
		DataContainer dc = dmf.loadForm(formActionId);
		if (dc.hasErrors()) {
			for (String key : dc.getErrors().keySet()) {
				this.addStatusMessage(dc.getErrors().get(key).getMessage());
			}
			throw new Exception("Error retrieving assignment logging form.");
		}
		return dc.getForm();
	}
	
	/**
	 * Gets a db connection
	 * @throws Exception
	 */
	private void getDBConnection() throws Exception {
		log.debug("getting db connection...");
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		dbConn = new SMTDBConnection(dbc.getConnection());
	}
	
	/**
	 * Closes db connection
	 */
	private void closeDBConnection() {
		if (dbConn != null) {
			try {
				dbConn.close();
			} catch (Exception e) {
				addStatusMessage("Error: could not close db connection, " + e);
				log.error("could not close db connection, ", e);
			}
		}
	}
	
	/**
	 * loads configuration file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadConfig() throws FileNotFoundException, IOException {
		// Load the config file.  Taken from com.depuy.datafeed.DataFeedUtil
		log.debug("loading config properties");
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(configPath);
			props.load(inStream);
			log.info("Successfully loaded config file");
		} catch (FileNotFoundException fnfe){
			log.error("Unable to find configuration file.");
			throw new FileNotFoundException();
		} catch (IOException ioe) {
			log.error("Unable to read/load configuration file.");
			throw new IOException();
		}
		finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {}
			}
		}
		// initialize certain vars
		dbDriver = props.getProperty("dbDriver");
		dbUser =  props.getProperty("dbUser");
		dbPassword =  props.getProperty("dbPassword");
		dbUrl =  props.getProperty("dbUrl");
		adminEmailFrom = props.getProperty("smtpFrom");
		adminEmailTo = props.getProperty("smtpTo");
	}

	public List<String> getStatusMessages() {
		return statusMessages;
	}

	public void setStatusMessages(List<String> statusMessages) {
		this.statusMessages = statusMessages;
	}
	
	public void addStatusMessage(String msg) {
		this.statusMessages.add(msg);
	}

	public boolean isErrorCondition() {
		return errorCondition;
	}

	public void setErrorCondition(boolean errorCondition) {
		this.errorCondition = errorCondition;
	}

}
