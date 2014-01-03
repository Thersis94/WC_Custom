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
import org.apache.log4j.PropertyConfigurator;

// SMT base libs
import com.siliconmtn.db.pool.SMTDBConnection;

// SB II libs
import com.smt.sitebuilder.action.tracker.AssignmentLogManager;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.exception.AssignmentException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentLogException;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
* <b>Title</b>SJMFollowUpProcessor.java<p/>
* <b>Description: Processes follow-up notifications to ambassadors who have designated a follow-up
* date on an assignment.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Dec 14, 2011
* <b>Change Log:</b> Dec 14, 2011: Created class.
****************************************************************************/
public class FollowUpProcessor {
	
	private Logger log = Logger.getLogger("SJMFollowUpProcessor");
	private AssignmentVO toNotify = null;
	private List<String> statusMessages = null;
	private boolean errorCondition = false;
	private int followUpCount = 0;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private SMTDBConnection dbConn = null;
	private FormVO logForm = null;
	private String formId = null;
	
	public FollowUpProcessor() {
		statusMessages = new ArrayList<String>();
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
	}
			
	/**
	 * 	Processes ambassador 'follow-up' notifications.  If a 'follow-up' date of 'today' is set 
	 * on an assignment, the ambassador is sent a reminder to follow-up with the assigned patient.
	 */
	public void processNotifyDates() throws AssignmentException {
		log.debug("processing follow-up updates of type " + SJMTrackerConstants.EMAIL_TYPE_FOLLOW_UP);
		if (toNotify == null) {
			addStatusMessage("Found no follow-up record to process.");
			return;
		}

		boolean success = false;		
		// reset the notify date field
		try {
			this.resetFollowUpDate(toNotify);
			log.debug("successful date reset");
			addStatusMessage("Successfully reset follow-up date");
			success = true;
		} catch (SQLException sqle) {
			success = false;
			addStatusMessage("Error resetting follow-up date, exiting process" + sqle);
			setErrorCondition(true);
			log.debug("failed date reset");
			throw new AssignmentException(sqle.getMessage());
		}
		
		// update the assignment log
		if (success) {
			// add field/val for the 'notify date' form field
			UtilityAssignmentLogger ual = new UtilityAssignmentLogger();
			ual.setDbConn(dbConn);
			ual.setAttributes(attributes);
			ual.setLogForm(logForm);
			ual.setAssignment(toNotify);
			ual.initializeDataMap();
			ual.addLogData(ual.getLogFieldMap().get(AssignmentLogManager.LOG_NOTIFY_DATE), new String[] {"SYSTEM"});
			try {
				ual.processAssignmentLogging();
				log.debug("successful log insertion");
				addStatusMessage("Successfully inserted follow-up log entry.");
			} catch (AssignmentLogException ale) {
				log.debug("failed log insertion", ale);
				addStatusMessage("Error inserting assignment log entry, exiting process, " + ale);
				setErrorCondition(true);
				throw new AssignmentException(ale.getMessage());
			}
			followUpCount++;
		}
	}
	
	/**
	 * Sets the notify_dt field value to null for each assignment in the list passed in.
	 * @param toNotify
	 */
	private void resetFollowUpDate(AssignmentVO avo) throws SQLException {
		log.debug("Resetting follow-up date...");
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_assignment set notify_dt = null, ");
		sql.append("notify_notes_txt = null where pt_assignment_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, avo.getAssignmentId());
			int updateCount = ps.executeUpdate();
			addStatusMessage("Success: " + updateCount + " follow-up date reset: ");
			log.debug("records updated: " + updateCount);
		} catch (SQLException sqle) {
			log.error("Error: Failed to update follow-up date, ", sqle);
			addStatusMessage("Error: Failed to reset follow-up date for assignment: " + avo.getAssignmentId());
			setErrorCondition(true);
			throw new SQLException(sqle.getMessage());
		}
	}

	/**
	 * @return the statusMessages
	 */
	public List<String> getStatusMessages() {
		return statusMessages;
	}

	/**
	 * @param statusMessages the statusMessages to set
	 */
	public void setStatusMessages(List<String> statusMessages) {
		this.statusMessages = statusMessages;
	}
	
	public void addStatusMessage(String msg) {
		this.statusMessages.add(msg);
	}

	/**
	 * @param toNotify the toNotify to set
	 */
	public void setToNotify(AssignmentVO toNotify) {
		this.toNotify = toNotify;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @return the formId
	 */
	public String getFormId() {
		return formId;
	}

	/**
	 * @param formId the formId to set
	 */
	public void setFormId(String formId) {
		this.formId = formId;
	}

	public void setLogForm(FormVO logForm) {
		this.logForm = logForm;
	}

	/**
	 * @return the followUpCount
	 */
	public int getFollowUpCount() {
		return followUpCount;
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
	
}
