package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Apache Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT base libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB II libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentLogVO;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.exception.AssignmentException;
import com.smt.sitebuilder.action.tracker.exception.AssignmentLogException;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.MapTransactionParser;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormPageVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
* <b>Title</b>UtilityAssignmentLogger.java<p/>
* <b>Description: Assignment logger utility class used by SJMAssignmentProcessor script classes for 
* logging assignment updates.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since May 21, 2012
* <b>Change Log:</b> May 21, 2012: Created class.
****************************************************************************/
public class UtilityAssignmentLogger {
	
	private Logger log = Logger.getLogger("UtilityAssignmentLogger");
	private List<String> statusMessages = null;
	private boolean errorCondition = false;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private SMTDBConnection dbConn = null;
	private FormVO logForm = null;
	private AssignmentVO assignment = null;
	private String baseAssigneeId = null; // allows for override of assigneeID on assignment
	private Integer baseStatusId = null; // allows for override of statusID on assignment
	private Integer baseResponseId = null; // allows for override of responseID on assignment
	private Map<String, String[]> logData = null;
	private Map<String, String> logFieldMap = null;
	
	public UtilityAssignmentLogger() {
		statusMessages = new ArrayList<String>();
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		logData = new HashMap<String, String[]>();
		logFieldMap = new HashMap<String, String>();
	}
	
	/**
	 * Public method for 
	 * @throws AssignmentLogException
	 */
	public void processAssignmentLogging() throws AssignmentLogException {
		if (dbConn == null || assignment == null || logForm == null || logData.isEmpty()) {
			throw new AssignmentLogException("Missing dbConn or assignment or logForm or logData");
		}
		//this.initializeDataMap();
		log.debug("processing assignment logging...");
		this.createAssignmentLogEntry();
	}

	/**
	 * Inserts an assignment log record for this assignment to reflect that the 
	 * assignment notify date has been reset.
	 * @param avo
	 * @return
	 * @throws AssignmentException
	 */
	private void createAssignmentLogEntry() throws AssignmentLogException {
		String formSubmittalId = null;
		// retrieve assignment log form
		DataManagerFacade dmf = new DataManagerFacade(attributes, dbConn);
		DataContainer dc  = 	new DataContainer();
		dc.setForm(logForm);
		
		MapTransactionParser mtp = new MapTransactionParser(logData);

		// load data and write transaction
		try {
			dc.loadData(mtp);
			dmf.writeTransactions(dc);
			addStatusMessage("Success: Wrote assignment log transaction record for assignment ID: " + assignment.getAssignmentId());
		} catch (Exception e) {
			addStatusMessage("Error: Failed to write assignment log transaction records; assignment ID: " + assignment.getAssignmentId());
			setErrorCondition(true);
			log.error("Error writing assignment log transaction, ", e);
			throw new AssignmentLogException(e.getMessage());
		}
				
		//retrieve form_submittal_id for this insert
		Collection<FormTransactionVO> f = dc.getTransactions().values();
		Iterator<FormTransactionVO> iter = f.iterator();
		if (iter.hasNext()) {
			FormTransactionVO vo = iter.next();
			log.debug("found an assignment log transaction VO: " + vo.getFormSubmittalId());
			formSubmittalId = vo.getFormSubmittalId();	
		}
		
		if (StringUtil.checkVal(formSubmittalId).length() == 0) {
			log.debug("form submittal ID has zero length");
			addStatusMessage("Error: Assignment log form submittal ID has zero length; assignment ID: " + assignment.getAssignmentId());
			setErrorCondition(true);
			throw new AssignmentLogException("Error writing assignment log transaction data.");
		}
				
		// insert assignment log base record, logging is done within method
		try {
			this.createAssignmentLogBaseRecord(assignment, formSubmittalId);
		} catch (AssignmentLogException ale) {
			throw new AssignmentLogException(ale);
		}
	}
	
	/**
	 * Inserts assignment log base record
	 * @param avo
	 * @throws AssignmentLogException
	 */
	private void createAssignmentLogBaseRecord(AssignmentVO avo, String formSubmittalId) throws AssignmentLogException {
		log.debug("inserting assignment log base record");
		StringBuffer sql = new StringBuffer();
		sql.append("insert into pt_assignment_log(pt_assignment_id, assignee_id, ");
		sql.append("assignment_status_id, assignment_response_id, form_submittal_id, ");
		sql.append("create_dt, pt_assignment_log_id) values (?,?,?,?,?,?,?) ");
		
		AssignmentLogVO aLog = new AssignmentLogVO();
		aLog.setSubmittalId(formSubmittalId);
		aLog.setAssignmentLogId(new UUIDGenerator().getUUID());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, avo.getAssignmentId());
			if (baseAssigneeId != null) {
				ps.setString(2, baseAssigneeId);
			} else {
				ps.setString(2, avo.getAssigneeId());
			}
			if (baseStatusId != null) {
				ps.setInt(3, baseStatusId);
			} else {
				ps.setInt(3, avo.getAssignmentStatusId());
			}
			if (baseResponseId != null) {
				ps.setInt(4, baseResponseId);
			} else {
				ps.setInt(4, avo.getAssignmentResponseId());
			}
			ps.setString(5, aLog.getSubmittalId());
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, aLog.getAssignmentLogId());
			ps.executeUpdate();
			addStatusMessage("Success: Inserted assignment log base record.");
		} catch (SQLException sqle) {
			log.error("Error inserting or updating assignment log base record(s), ", sqle);
			addStatusMessage("Error: Unable to insert the assignment log data.");
			setErrorCondition(true);
			throw new AssignmentLogException(sqle.getMessage());
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		// add the log to the assignment
		//assignment.addAssignmentLog(aLog);
	}

	/**
	 * Parses the log form fields into a map.  The map keys are the names of the fields.
	 */
	private void parseLogFormFields() {
		// iterate the form pages
		Collection<FormPageVO> pages = logForm.getPages();
		Iterator<FormPageVO> iter = pages.iterator();
		while (iter.hasNext()) {
			FormPageVO page = iter.next();
			Collection<FormFieldVO> fields = page.getFields();
			Iterator<FormFieldVO> fIter = fields.iterator();
			// iterate the page fields to find the fields we need
			FormFieldVO field = null;
			while (fIter.hasNext()) {
				field = fIter.next();
				logFieldMap.put(field.getFieldName(), field.getFormFieldId());
			}
		}
	}
	
	/**
	 * Initializes the data map that will be used to write the assignment log record to the 'black box'
	 * @return
	 */
	public void initializeDataMap() {
		// initialize the transaction parser map
		logData.put("formSubmittalId", new String[] {null});
		logData.put("profileId", new String[] {assignment.getAssignee().getProfileId()});
		logData.put("siteId", new String[] {SJMAssignmentProcessor.SITE_ID});
		logData.put("formId", new String[] {logForm.getFormId()});
		logData.put("emailAddressText", new String[] {assignment.getAssignee().getEmailAddress()});
		logData.put("acceptPrivacyFlag", new String[] {"0"});
		logData.put("sessionId", new String[]{"SYSTEM script"});
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
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}

	public FormVO getLogForm() {
		return logForm;
	}

	public void setLogForm(FormVO logForm) {
		this.logForm = logForm;
		this.parseLogFormFields();
	}

	public Map<String, String[]> getLogData() {
		return logData;
	}

	public void setLogData(Map<String, String[]> logData) {
		this.logData = logData;
	}
	
	public void addLogData(String key, String[] val) {
		this.logData.put(key, val);
	}

	public Map<String, String> getLogFieldMap() {
		return logFieldMap;
	}

	public void setAssignment(AssignmentVO assignment) {
		this.assignment = assignment;
	}

	public String getBaseAssigneeId() {
		return baseAssigneeId;
	}

	public void setBaseAssigneeId(String baseAssigneeId) {
		this.baseAssigneeId = baseAssigneeId;
	}

	public Integer getBaseStatusId() {
		return baseStatusId;
	}

	public void setBaseStatusId(Integer baseStatusId) {
		this.baseStatusId = baseStatusId;
	}

	public Integer getBaseResponseId() {
		return baseResponseId;
	}

	public void setBaseResponseId(Integer baseResponseId) {
		this.baseResponseId = baseResponseId;
	}

	public boolean isErrorCondition() {
		return errorCondition;
	}

	public void setErrorCondition(boolean errorCondition) {
		this.errorCondition = errorCondition;
	}
	
}
