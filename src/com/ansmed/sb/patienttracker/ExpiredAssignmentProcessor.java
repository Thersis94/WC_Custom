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
import com.smt.sitebuilder.action.tracker.exception.AssigneeException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentLogException;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
* <b>Title</b>ExpiredAssignmentProcessor.java<p/>
* <b>Description: Processes expired assignments.  Expired assignments are assignments that have 
* reached the 48 hour or 96 hour response time limit according to SJM assignment business rules.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Feb 27, 2012
****************************************************************************/
public class ExpiredAssignmentProcessor {
	
	private SMTDBConnection dbConn = null;
	Logger log = Logger.getLogger("ExpiredAssignmentProcessor");
	
	private List<String> statusMessages = null;
	private boolean errorCondition = false;
	private Map<String, Object> attributes = null;
	private int expiredCount = 0;
	
	public ExpiredAssignmentProcessor() {
		statusMessages = new ArrayList<String>();
		attributes = new HashMap<String, Object>();
	}
	
	
	/**
	 * Processes assignments that need to be expired/reassigned due to expiration
	 * of 96-hour response time limit.
	 * @param toExpire
	 * @param newResponseId
	 * @throws AssignmentException
	 * @throws AssigneeException
	 */
	protected void processExpiredAssignment(AssignmentVO toExpire, Integer newResponseId, 
			List<AssigneeVO> admins, FormVO logForm) throws AssignmentException {
		log.debug("processing expired assignment of type: " + newResponseId);
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		// reassign expired assignments to an admin
		this.expireAssignment(toExpire, newResponseId, countMap);
		
		// log assignment updates
		this.logAssignmentUpdate(logForm, toExpire, newResponseId);
		
		// update assignment counts for the original ambs assigned to the now-expired assignments
		this.updateAssignmentCount(countMap);
		
		// set up a map of who should receive email and of what type
		Map<String, Integer> deassignEmailTo = new HashMap<String, Integer>();
		if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
			deassignEmailTo.put(toExpire.getAssignee().getEmailAddress(), SJMTrackerConstants.RESPONSE_NONE_48);
			deassignEmailTo.put(admins.get(0).getEmailAddress(), SJMTrackerConstants.STATUS_EXPIRED);
		} else {
			deassignEmailTo.put(toExpire.getAssignee().getEmailAddress(), SJMTrackerConstants.RESPONSE_NONE_48);
		}
		this.attributes.put("deassignEmailTo", deassignEmailTo);
	}
	
		
	/**
	 * 	Updates the 'expired' assignment, setting the response to the new response ID passed in
	 * and setting the assignment's status to 'expired.
	 * @param toExpire
	 * @param newResponseId
	 * @throws AssignmentException
	 */
	private void expireAssignment(AssignmentVO toExpire, Integer newResponseId, Map<String, Integer> countMap) 
		throws AssignmentException {
		log.debug("expiring assignment of type: " + newResponseId);
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_assignment set assignment_status_id = ?, ");
		sql.append("assignment_response_id = ?, update_dt = ? where pt_assignment_id = ? ");
		log.debug("expireAssignments sql: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, SJMTrackerConstants.STATUS_REASSIGNED);
			ps.setInt(2, newResponseId);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, toExpire.getAssignmentId());
			expiredCount = ps.executeUpdate();
			if (expiredCount > 0) {
				// enter a negative number for the increment or subtract from the 
				// number already on the map for this ambassador
				if (countMap.containsKey(toExpire.getAssigneeId())) {
					countMap.put(toExpire.getAssigneeId(), (countMap.get(toExpire.getAssigneeId()) - expiredCount));
				} else {
					countMap.put(toExpire.getAssigneeId(), new Integer(-expiredCount));
				}
			}
		} catch (SQLException sqle) {
			log.error("Error expiring the assignment, ", sqle);
			addStatusMessage("Error setting the expired assignment to 'reassigned'.");
			setErrorCondition(true);
			throw new AssignmentException("Error changing status of 'expired' assignment to 'reassigned'. " + sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("Successfully reset status of expired assignment to 'reassigned'...");
	}
	
	/**
	 * Creates assignment log entries for this assignment expiration
	 * @param logForm
	 * @param avo
	 * @param newResponseId
	 */
	private void logAssignmentUpdate(FormVO logForm, AssignmentVO avo, Integer newResponseId) 
		throws AssignmentException {
		log.debug("creating assignment log entry");
		UtilityAssignmentLogger ual = new UtilityAssignmentLogger();
		ual.setDbConn(dbConn);
		ual.setAttributes(attributes);
		ual.setLogForm(logForm);
		ual.setAssignment(avo);
		ual.initializeDataMap();
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_STATUS_ID), new String[] {SJMTrackerConstants.STATUS_REASSIGNED.toString()});
		StringBuffer toText = new StringBuffer();
		if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_48)) {
			toText.append("Ambassador has not responded within 48 hours of initial assignment.");	
		} else if (newResponseId.equals(SJMTrackerConstants.RESPONSE_NONE_96)) {
			toText.append("No ambassador responded to initial assignment; 96 hours have elapsed since initial assignment was made.");
		}
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_RESPONSE_ID), new String[] {newResponseId.toString()});
		ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_SYSTEM_TEXT), new String[] {toText.toString()});
		try {
			ual.processAssignmentLogging();
			log.debug("successful log insertion");
			addStatusMessage("Successfully inserted reassignment log entry.");
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
			addStatusMessage("Error updating ambassador assignment count");
			setErrorCondition(true);
			log.error("Error updating ambassador assignment counts, ", sqle);
			throw new AssignmentException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		addStatusMessage("Successfully updated assignment count: " + updateCount.length);
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
	 * @return the expiredCount
	 */
	public int getExpiredCount() {
		return expiredCount;
	}

}
