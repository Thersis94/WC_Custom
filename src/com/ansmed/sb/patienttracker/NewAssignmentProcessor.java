package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache Log4J
import org.apache.log4j.Logger;

// SMT base libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;

// SB II libs
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.exception.AssignmentException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentLogException;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
* <b>Title</b>NewAssignmentProcessor.java<p/>
* <b>Description: Processes the reassignment of expired assignments according to SJM assignment 
* business rules.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Feb 27, 2012
****************************************************************************/
public class NewAssignmentProcessor {
	
	//private static final String DEFAULT_ORG_ID = "SJM_AMBASSADORS";
	private SMTDBConnection dbConn = null;
	private Logger log = Logger.getLogger("NewAssignmentProcessor");
	
	private List<String> statusMessages = null;
	private boolean errorCondition = false;
	private Map<String, Object> attributes = null;
	private AssigneeVO newAmbassador = null;
	private String newAssignmentId = null;
	private AssignmentVO reAssignment = null;
	private int reassignCount = 0;
	
	public NewAssignmentProcessor() {
		statusMessages = new ArrayList<String>();
		attributes = new HashMap<String, Object>();
	}
	
	protected void processReassignAssignment(AssignmentVO toReassign, Integer newResponseId, 
			List<AssigneeVO> admins, FormVO logForm) throws AssignmentException {
		log.debug("starting processReassignAssignment for 'expired' assignment of type: " + newResponseId);
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		AssigneeVO ambMatch = null;
		if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
			// choose an ambassador
			ambMatch = this.retrieveAmbassadorMatch(toReassign);
		} else {
			// choose an admin
			ambMatch = admins.get(0);
		}
		
		if (ambMatch == null) {
			addStatusMessage("Error retrieving an ambassador match");
			setErrorCondition(true);
			throw new AssignmentException("Error retrieving ambassador match.  Null match returned.");
		}
		// set the vo of the ambassador chosen as a match.
		this.setNewAmbassador(ambMatch);
		
		// reassign expired assignment
		this.createReassignment(toReassign, ambMatch, newResponseId, countMap);
		
		// set logging parameters
		this.logAssignmentUpdate(toReassign, ambMatch, newResponseId, logForm);
		
		// update assignment counts for the original amb assigned to the now-expired assignment
		this.updateAssignmentCount(countMap);
		
		// notify the admin(s) of the expired assignments
		//this.processReassignmentNotification(admins.get(0), toReassign, newResponseId);
		
		// set up a map of who should receive email and of what type
		Map<String, Integer> reassignEmailTo = new HashMap<String, Integer>();
		if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
			reassignEmailTo.put(ambMatch.getEmailAddress(), SJMTrackerConstants.EMAIL_TYPE_NONE_48_NEXT_AMB);
			if (ambMatch.getUseAlternateEmail() == 1) {
				reassignEmailTo.put(ambMatch.getAlternateEmailAddress(), SJMTrackerConstants.EMAIL_TYPE_ALTERNATE_ADDRESS);
			}
		} else {
			reassignEmailTo.put(ambMatch.getEmailAddress(), SJMTrackerConstants.RESPONSE_NONE_96);
		}
		this.attributes.put("reassignEmailTo", reassignEmailTo);
	}

	/**
	 * Assigns each patient from each expired assignment to an admin.
	 * @param toReassign
	 * @param admins
	 * @param countMap
	 */
	protected void createReassignment(AssignmentVO toReassign, AssigneeVO matchAssignee, Integer newResponseId, 
			Map<String, Integer> countMap) throws AssignmentException {
		log.debug("updating assignment record for type: " + newResponseId);
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_assignment set assignee_id = ?, assignment_status_id = ?, ");
		sql.append("assignment_response_id = ?, assign_dt = ?, accept_dt = ?, update_dt = ? where pt_assignment_id = ?");
		log.debug("reassignment update sql: " + sql.toString());
		
		// format the new assignment VO
		reAssignment = new AssignmentVO();
		reAssignment.setAssignee(matchAssignee);
		reAssignment.setAssignmentId(toReassign.getAssignmentId());
		reAssignment.setAssigneeId(matchAssignee.getAssigneeId());
		reAssignment.setPatientId(toReassign.getPatientId());
		reAssignment.setAssignmentStatusId(SJMTrackerConstants.STATUS_PENDING);
		reAssignment.setAssignmentResponseId(newResponseId);
		reAssignment.setCreateDate(toReassign.getCreateDate()); // keep the original create date unchanged
		reAssignment.setPreviousAssignDate(toReassign.getAssignDate());
		reAssignment.setAssignDate(new java.util.Date()); // set assign date to 'now'
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, matchAssignee.getAssigneeId());
			ps.setInt(2, SJMTrackerConstants.STATUS_PENDING);
			if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48) || 
				newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_96)) {
				ps.setInt(3, newResponseId);
			} else {
				ps.setInt(3, SJMTrackerConstants.RESPONSE_DEFAULT);
				reAssignment.setAssignmentResponseId(SJMTrackerConstants.RESPONSE_DEFAULT);
			}
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setDate(5, null);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, toReassign.getAssignmentId());
			ps.addBatch();
			reassignCount = ps.executeUpdate();
			if (reassignCount > 0) {
				if (countMap.containsKey(matchAssignee.getAssigneeId())) {
					countMap.put(matchAssignee.getAssigneeId(), countMap.get(matchAssignee.getAssigneeId()) + reassignCount);
				} else {
					countMap.put(matchAssignee.getAssigneeId(), new Integer(reassignCount));
				}
			}
		} catch (SQLException sqle) {
			log.error("Error creating new assignment, ", sqle);
			addStatusMessage("Error creating new assignment");
			setErrorCondition(true);
			throw new AssignmentException("Error creating/assigning new assignments. " + sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		addStatusMessage("Successfully created new assignment");
		this.setNewAssignmentId(newAssignmentId);
	}
	
	/**
	 * Creates log entry 
	 * @param toReassign
	 * @param ambMatch
	 * @param newResponseId
	 * @param logForm
	 */
	private void logAssignmentUpdate(AssignmentVO toReassign, AssigneeVO ambMatch, Integer newResponseId, 
			FormVO logForm) throws AssignmentException {
		log.debug("creating reassignment log entry");
		UtilityAssignmentLogger ual = new UtilityAssignmentLogger();
		ual.setDbConn(dbConn);
		ual.setAttributes(attributes);
		ual.setLogForm(logForm);
		ual.setAssignment(toReassign);
		ual.initializeDataMap();
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_STATUS_ID), new String[] {SJMTrackerConstants.STATUS_PENDING.toString()});
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_RESPONSE_ID), new String[] {newResponseId.toString()});
		// override certain values from the original assignment
		ual.setBaseAssigneeId(ambMatch.getAssigneeId());
		ual.setBaseStatusId(reAssignment.getAssignmentStatusId());
		ual.setBaseResponseId(newResponseId);
		StringBuffer toText = new StringBuffer();
		if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
			toText.append("Patient reassigned to ");	
		} else if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_96)) {
			toText.append("Patient reassigned to admin ");
		}
		toText.append(ambMatch.getFirstName()).append(" ").append(ambMatch.getLastName().substring(0,1)).append(".");
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_SYSTEM_TEXT), new String[] {toText.toString()});
		try {
			ual.processAssignmentLogging();
			log.debug("successful log insertion");
			addStatusMessage("Successfully inserted pending reassignment log entry.");
		} catch (AssignmentLogException ale) {
			log.debug("failed log insertion", ale);
			addStatusMessage("Error inserting assignment log entry, exiting process, " + ale);
			setErrorCondition(true);
			throw new AssignmentException(ale.getMessage());
		}
		ual = null;
	}
	
	/**
	 * Updates ambassador assignment counts.  Map keys are assignee IDs, values are the number
	 * of assignments expired for each assignee.  Values are either positive or negative reflecting
	 * an increase or decrease in the number of assignments assigned to each ambassador.
	 * @param countMap
	 * @throws AssignmentException
	 */
	private void updateAssignmentCount(Map<String, Integer> countMap) throws AssignmentException {
		log.debug("updating ambassador assignment count...");
		if (countMap.isEmpty()) return;
		
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_assignee set curr_assign_no += ? where assignee_id = ? ");
		log.debug("assignment count update sql: " + sql.toString());
		int[] updateCount = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String key : countMap.keySet()) {
				ps.setInt(1, countMap.get(key));
				ps.setString(2, key);
				ps.addBatch();
			}
			updateCount = ps.executeBatch();
			log.debug("Updated ambassador assignment counts; records updated: " + updateCount.length);
		} catch (SQLException sqle) {
			log.error("Error updating ambassador assignment counts, ", sqle);
			addStatusMessage("Error updating assignment count");
			setErrorCondition(true);
			throw new AssignmentException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("Successfully updated assignment count: " + updateCount.length);
	}
	
	/**
	 * Retrieves today's available ambassadors and matches one with a patient.
	 * @param avo
	 * @return
	 */
	private AssigneeVO retrieveAmbassadorMatch(AssignmentVO avo) {
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setDbConn(dbConn);
		sar.setAttributes(attributes);
		sar.setCheckAssignmentLimit(true);
		List<AssigneeVO> ambs = null;
		try {
			ambs = sar.retrieveAmbassadors();
		} catch (SQLException sqle) {
			log.error("Error retrieving ambassadors, ", sqle);
			addStatusMessage("Unable to retrieve today's ambassadors");
			setErrorCondition(true);
		}
		
		AmbassadorMatcher am = new AmbassadorMatcher();
		am.setDbConn(dbConn);
		am.setAmbassadors(ambs);
		am.setPatient(avo.getPatient());
		am.addToExcludeList(avo.getAssigneeId());
		AssigneeVO asvo = am.findAmbassadorMatch();
		if (asvo != null) { 
			return asvo;
		} else {
			return null;
		}
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
	
	/**
	 * @return the errorCondition
	 */
	public boolean isErrorCondition() {
		return errorCondition;
	}

	/**
	 * @param errorCondition the errorCondition to set
	 */
	public void setErrorCondition(boolean errorCondition) {
		this.errorCondition = errorCondition;
	}

	/**
	 * @return the dbConn
	 */
	public SMTDBConnection getDbConn() {
		return dbConn;
	}


	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the newAssignmentId
	 */
	public String getNewAssignmentId() {
		return newAssignmentId;
	}

	/**
	 * @param newAssignmentId the newAssignmentId to set
	 */
	public void setNewAssignmentId(String newAssignmentId) {
		this.newAssignmentId = newAssignmentId;
	}

	/**
	 * @return the newAmbassador
	 */
	public AssigneeVO getNewAmbassador() {
		return newAmbassador;
	}

	/**
	 * @param newAmbassador the newAmbassador to set
	 */
	public void setNewAmbassador(AssigneeVO newAmbassador) {
		this.newAmbassador = newAmbassador;
	}

	/**
	 * @return the ReAssignment
	 */
	public AssignmentVO getReAssignment() {
		return reAssignment;
	}

	/**
	 * @param reAssignment the reAssignment to set
	 */
	public void reAssignment(AssignmentVO reAssignment) {
		this.reAssignment = reAssignment;
	}

	/**
	 * @return the reassignCount
	 */
	public int getReassignCount() {
		return reassignCount;
	}

}
