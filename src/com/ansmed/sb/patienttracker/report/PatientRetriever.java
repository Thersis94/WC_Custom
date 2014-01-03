package com.ansmed.sb.patienttracker.report;

// JDK 6
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SB_ANS_Medical libs
import com.ansmed.sb.patienttracker.PatientContactDataRetriever;
import com.ansmed.sb.patienttracker.SJMTrackerConstants;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

// WC 2.0 libs
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO.ColumnName;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
* <b>Title</b>PatientRetriever.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Jan 17, 2013
* <b>Changes: </b>
****************************************************************************/
public class PatientRetriever {
	
	private Logger log = Logger.getLogger(PatientRetriever.class);
	private String assigneeId;
	private Map<String, AssignmentVO> assignments;
	private List<String> profileIds;
	private Map<String, UserDataVO> profiles;
	private Map<String,String> cSubmittalIds;
	private Map<String,ContactDataContainer> cData;
	private Map<String,ContactVO> contactForms;
	private List<String> fSubmittalIds;
	private Map<String,String> formFieldMap;
	private Map<String,FormTransactionVO> fData;
	private SMTDBConnection dbConn;
	private Map<String,Object> config;
	private String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private String dbUrl = "jdbc:sqlserver://192.168.3.130:1433;selectMethod=cursor;responseBuffering=adaptive";
	private String dbUser = "webcrescendo_sb_user";
	private String dbPassword = "sqll0gin";
	private String reportFile = "C:\\SMT\\accounts\\SJM\\Laura\\Tracker_Report_Lauras_Pending\\report.csv";
	private StringEncoder se;
	
	public PatientRetriever() {
		config = new HashMap<String,Object>();
		config.put(Constants.ENCRYPT_KEY, "s1l1c0nmtnT3chm0l0g13$JC");
		PropertyConfigurator.configure("scripts/tracker_log4j.properties");
		assignments = new HashMap<String,AssignmentVO>();
		cSubmittalIds = new HashMap<String,String>();
		contactForms = new HashMap<String,ContactVO>();
		cData = new HashMap<String,ContactDataContainer>();
		fSubmittalIds = new ArrayList<String>();
		formFieldMap = new HashMap<String,String>();
		profileIds = new ArrayList<String>();
		se = new StringEncoder();
		this.loadPatientFieldMap();
	}
	
	public static void main(String[] args) {
		PatientRetriever pr = new PatientRetriever();
		try {
			pr.getDBConnection();
		} catch (Exception e) {
			pr.log.debug("Cannot obtain db connection, " + e);
			System.exit(-1);
		}
		pr.setAssigneeId("c0a80241c491d93fad4439c9e94bdc9");
		pr.retrievePatients();
		pr.closeDBConnection();
	}
	
	/**
	 * Retrieves patient data
	 */
	public void retrievePatients() {
		this.retrievePatientBaseRecords();
		this.retrieveUserProfiles();
		this.retrieveFormSubmittals();
		this.retrieveContactSubmittals();
		this.buildReportFile();
	}
	
	/**
	 * Retrieves base patient records
	 */
	public void retrievePatientBaseRecords() {
		log.debug("retrieving base records...");
		StringBuilder sql = new StringBuilder();
		sql.append("select a.pt_assignment_id, a.assign_dt, b.patient_id, b.patient_profile_id, b.form_submittal_id, ");
		sql.append("b.contact_submittal_id, b.create_dt from pt_assignment a ");
		sql.append("inner join pt_patient b on a.patient_id = b.patient_id ");
		sql.append("where a.assignee_id = ? and a.assignment_status_id = ?");
		log.debug("base record retrieval: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, assigneeId);
			ps.setInt(2, SJMTrackerConstants.STATUS_PENDING);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssignmentVO avo = new AssignmentVO();
				avo.setData(rs);
				PatientVO pvo = new PatientVO();
				pvo.setData(rs);
				avo.setPatient(pvo);
				profileIds.add(rs.getString("patient_profile_id"));
				cSubmittalIds.put(pvo.getPatientProfileId(), rs.getString("contact_submittal_id"));
				fSubmittalIds.add(rs.getString("form_submittal_id"));
				assignments.put(pvo.getPatientProfileId(), avo);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving patient data, ", sqle);
		}
		
		log.debug("Patients: " + assignments.size());
		log.debug("ProfileIds: " + profileIds.size());
		log.debug("cSubmittalIds: " + cSubmittalIds.size());
		log.debug("fSubmittalIds: " + fSubmittalIds.size());
				
	}
	
	/**
	 * Retrieves patient profiles via ProfileManager
	 */
	public void retrieveUserProfiles() {
		log.debug("Retrieving user profiles...");
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		try {
			profiles = pm.searchProfileMap(dbConn, profileIds);
		} catch(DatabaseException de) {
			log.error("Error retrieving user profiles, ", de);
		}
	}
	
	/**
	 * Retrieves data based on the form type and form submittal Ids passed in.
	 * @return
	 */
	public void retrieveFormSubmittals() {
		log.debug("Retrieving user form submittals...");
		if (fSubmittalIds.isEmpty()) return;
		DataContainer dc = new DataContainer();
		
		// lookup the specified form actionId
		String formId = this.retrieveFormActionId("patient");
		if (formId == null) return;
		
		// set up the query for use by the data manager
		GenericQueryVO gqv = new GenericQueryVO(formId);
		gqv.setOrganizationId(SJMTrackerConstants.TRACKER_ORG_ID);
		QueryParamVO vo = new QueryParamVO();
		vo.setColumnNm(ColumnName.FORM_SUBMITTAL_ID);
		vo.setOperator(Operator.in);
		vo.setValues(fSubmittalIds.toArray(new String[0]));
		gqv.addConditional(vo);
		
		// retrieve the data
		dc.setQuery(gqv);
		DataManagerFacade dfm = new DataManagerFacade(config, dbConn);
		dc = dfm.loadTransactions(dc);
		fData = dc.getTransactions();
		log.debug("number of transactions retrieved: " + fData.size());
	}
	
	/**
	 * Retrieves patient contact submittal data if it exists.
	 */
	public void retrieveContactSubmittals() {
		log.debug("Retrieving user contact submittals...");
		PatientContactDataRetriever pcd = new PatientContactDataRetriever();
		pcd.setDbConn(dbConn);
		TrackerDataContainer tdc = null;
		String contactFormId = null;
		try {
			for (String pId : cSubmittalIds.keySet()) {
				if (cSubmittalIds.get(pId) == null) continue;
				pcd.setContactSubmittalId(cSubmittalIds.get(pId));
				// retrieve the tracker data container
				tdc = pcd.retrievePatientContactData();
				// add the contact data container to the map
				cData.put(pId, tdc.getContactData());
				contactFormId = tdc.getContactForm().getActionId();
				// add the contact form to the map if it's not there already
				if (! contactForms.containsKey(contactFormId)) {
					contactForms.put(contactFormId, tdc.getContactForm());
				}
				// set the patient's source contact form ID
				assignments.get(pId).getPatient().setPatientSourceFormId(contactFormId);
			}
		} catch (SQLException sqle) {
			//logged downstream
		}
		log.debug("cData containers: " + cData.size());
	}
	
	/**
	 * Builds the report file
	 */
	public void buildReportFile() {
		log.debug("building report data file...");
		StringBuilder rep = new StringBuilder();
		this.buildHeaders(rep);
		this.buildRows(rep);
		log.debug(rep);
		try {
			this.writeReport(rep, reportFile);
		} catch (FileNotFoundException fnfe) {
			log.error("File not found, ", fnfe);
		} catch (IOException ioe) {
			log.error("IOException, ", ioe);
		}
	}
	
	
	/**
	 * Builds headers for file
	 * @param rep
	 */
	public void buildHeaders(StringBuilder rep) {
		rep.append("Date of Request").append(",");
		rep.append("First Name").append(",");
		rep.append("Middle Initial").append(",");
		rep.append("Last Name").append(",");
		rep.append("Age Range").append(",");
		rep.append("Location of Pain").append(",");
		rep.append("State").append(",");
		rep.append("Most Important Match Factor").append(",");
		rep.append("Preferred Method of Contact").append(",");
		rep.append("Email Address").append(",");
		rep.append("Phone Number").append(",");
		rep.append("Alternative Phone Number").append(",");
		rep.append("Best Time to Contact Patient").append(",");
		rep.append("Summary of Comments").append(",");
		rep.append("Doctor's Name (if provided)").append(",");
		rep.append("Is Interaction Shareable With Patient's Physician?").append(",");
		rep.append("Urgency of Request").append(",");
		rep.append("Requested By");
		rep.append("\n");
	}
	
	public void buildRows(StringBuilder rep) {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		for (String profId : assignments.keySet()) {
			AssignmentVO avo = assignments.get(profId);
			PatientVO pat = avo.getPatient();
			rep.append(Convert.formatDate(avo.getAssignDate())).append(",");
			// append profile data
			rep.append(profiles.get(profId).getFirstName()).append(",");
			if (StringUtil.checkVal(profiles.get(profId).getMiddleName()).length() > 0) {
				rep.append(profiles.get(profId).getMiddleName().substring(0,1)).append(",");
			} else {
				rep.append(",");
			}
			rep.append(profiles.get(profId).getLastName()).append(",");
			// append form data
			this.buildPatientFormData(pnf, rep, pat);
			rep.append("\n");
		}
	}
	
	/**
	 * Appends additional patient data to the report.
	 * @param pnf
	 * @param rep
	 * @param patient
	 */
	private void buildPatientFormData(PhoneNumberFormat pnf, StringBuilder rep, PatientVO patient) {
		FormTransactionVO fvo = fData.get(patient.getSubmittalId());
		Map<String,FormFieldVO> fields = fvo.getCustomData();
		Map<String,String> cVals = this.formatPatientContactValues(patient);
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("ageRange")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("painLocation")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("stateCode")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("matchFactor")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("contactMethod")))).append(",");
		rep.append(StringUtil.checkVal(profiles.get(patient.getProfileId()).getEmailAddress(),"n/a")).append(",");
		
		pnf.setPhoneNumber(profiles.get(patient.getPatientProfileId()).getMainPhone());
		rep.append(StringUtil.checkVal(pnf.getFormattedNumber(),"n/a")).append(",");
				
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("phoneAlternate")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("contactTime")))).append(",");
		
		rep.append(this.parseSummary(cVals.get("summary"))).append(",");
		
		rep.append(se.decodeValue(cVals.get("doctor"))).append(",");
		rep.append(cVals.get("shareInfo")).append(",");
		rep.append(cVals.get("urgency")).append(",");
		rep.append(cVals.get("requestedBy")); // last field in report...NO COMMA
	}
	
	/**
	 * Parses a field response, returning either the response value or an empty String.
	 * @param field
	 * @return
	 */
	private String parseFieldResponse(FormFieldVO field) {
		if (field != null) {
			if (field.getResponses() != null && ! field.getResponses().isEmpty()) {
				// we have a value
				return StringUtil.checkVal(field.getResponses().get(0));
			} else {
				return "";
			}
		} else {
			return "";
		}
	}
	
	/**
	 * Examines the summary field text and replaces carriage returns and line-feed chars with a space.
	 * Wraps the summary in double quotes so that the text is treated as a single field and commas
	 * in the text are not treated as column separators.  Also decodes HTML values and replaces
	 * them with their textual equivalent.
	 * @param summary
	 * @return
	 */
	private String parseSummary(String summary) {
		StringBuilder tmp = new StringBuilder();
		tmp.append("\"");
		for (int i = 0; i < summary.length(); i++) {
			if (summary.charAt(i) == (char)10 || summary.charAt(i) == (char)13) {
				tmp.append(" ");
			} else {
				tmp.append(summary.charAt(i));
			}
		}
		tmp.append("\"");
		return se.decodeValue(tmp.toString());
	}
	
	/**
	 * 
	 * @param rep
	 * @param target
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeReport(StringBuilder rep, String target) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(target);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		bos.write(rep.toString().getBytes());
		bos.flush();
		bos.close();
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
		sql.append("where organization_id = ? and key_nm = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			//ps.setString(1, actionId);
			ps.setString(1, SJMTrackerConstants.TRACKER_ORG_ID);
			ps.setString(2, formKey);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				formId = rs.getString("form_id");
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving action ID for patient tracker form, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		return formId;
	}
	
	// TODO Copied this from TrackerMailFormatter...need to centralize this as a util.
	/**
	 * Returns a map of certain patient contact form values for use in 'new assignment' emails.
	 * @return
	 */
	// TODO 
	private Map<String, String> formatPatientContactValues(PatientVO patient) {
		ContactVO contactForm = contactForms.get(patient.getPatientSourceFormId());
		ContactDataContainer contactData = cData.get(patient.getPatientProfileId());
		
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
					//log.debug("list size is " + data.size());
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
		}
		//if (summary == null && assignment != null) summary = assignment.getAssignmentNotes();
		cVals.put("doctor", doctor);
		cVals.put("shareInfo", shareInfo);
		cVals.put("urgency", urgency);
		cVals.put("summary", summary);
		cVals.put("requestedBy", requestedBy);
		return cVals;
	}
	
	/**
	 * loads map of keys to form_field_id values
	 */
	private void loadPatientFieldMap() {
		// key, form_field_id
		formFieldMap.put("ageRange","c0a80237662b11dbb5c0326460296ed1");
		formFieldMap.put("ambQuestion","c0a802376631104dd8f9dec9ad1e83a8");
		formFieldMap.put("ambRequested","c0a802376626c3b5588a4c246c0cb039");
		formFieldMap.put("contactMethod","c0a8023766250c46418ec22b852e7838");
		formFieldMap.put("contactTime","c0a80237665cd05bce9bc8c2b200318a");
		formFieldMap.put("genderPreference","c0a8023766290edfaabaeaee10fd075c");
		formFieldMap.put("matchFactor","c0a80237662d0a7775f2231c9c7c3743");
		formFieldMap.put("painLocation","c0a8023766282af6d33fce93e7d2d285");
		formFieldMap.put("phoneAlternate","c0a8023766379ebc9feb6e1ae7f358f7");
		formFieldMap.put("physicianName","c0a80237662fef282f3c4e368846f14c");
		formFieldMap.put("shareInfo","c0a802376630a3a7ad52730e42c72ac3");
		formFieldMap.put("stateCode","c0a80241ad8ec5b1902f1fc5b9f6ce42");
		formFieldMap.put("treatmentPhase","c0a802376636bc5a8422a4fbc1157895");
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
	 * 
	 */
	private void closeDBConnection() {
		if (dbConn != null) {
			try {
				dbConn.close();
				log.debug("closed db connection.");
			} catch (Exception e) {}
		}
	}

	/**
	 * @return the assigneeId
	 */
	public String getAssigneeId() {
		return assigneeId;
	}

	/**
	 * @param assigneeId the assigneeId to set
	 */
	public void setAssigneeId(String assigneeId) {
		this.assigneeId = assigneeId;
	}

}
