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

import com.ansmed.sb.patienttracker.PatientContactDataRetriever;
// SB_ANS_Medical libs
import com.ansmed.sb.patienttracker.SJMTrackerConstants;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
// WC 2.0 libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientInteractionVO;
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
* <b>Title</b>AssignmentRetriever.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2013<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Dec 11, 2013
* <b>Changes: </b>
* Dec 11, 2013: DBargerhuff: created class
****************************************************************************/
public class AssignmentRetriever {
	
	private Logger log = Logger.getLogger(AssignmentRetriever.class);
	private String START_QUERY_DT = "2013-01-01";
	private String END_QUERY_DT = "2013-11-01";

	private List<String> profileIds;
	private Map<String, UserDataVO> profiles;
	private Map<String, AssignmentVO> assignments;
	//private List<AssignmentVO> assignments;
	private Map<String,String> cSubmittalIds;
	private List<String> iSubmittalIds;
	private List<String> pSubmittalIds;
	private Map<String,ContactVO> contactForms;
	private Map<String,ContactDataContainer> cData;
	private Map<String,FormTransactionVO> iData;
	private Map<String,FormTransactionVO> pData;
	
	private Map<String,String> formFieldMap;
	
	private SMTDBConnection dbConn;
	private Map<String,Object> config;
	private String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	//private String dbUrl = "jdbc:sqlserver://192.168.3.130:1433;selectMethod=cursor;responseBuffering=adaptive";
	//private String dbUser = "webcrescendo_sb_user";
	private String dbUrl = "jdbc:sqlserver://10.0.20.242:2007;selectMethod=cursor;responseBuffering=adaptive";
	private String dbUser = "sitebuilder_sb_user";
	private String dbPassword = "sqll0gin";
	private String reportFile = "C:\\SMT\\accounts\\SJM\\Laura\\reports\\2013-12-12\\patient_history_report.csv";
	private StringEncoder se;
	
	public AssignmentRetriever() {
		config = new HashMap<String,Object>();
		config.put(Constants.ENCRYPT_KEY, "s1l1c0nmtnT3chm0l0g13$JC");
		PropertyConfigurator.configure("scripts/tracker_log4j.properties");
		//assignments = new ArrayList<AssignmentVO>();
		assignments = new HashMap<String, AssignmentVO>();
		cSubmittalIds = new HashMap<String,String>();
		iSubmittalIds = new ArrayList<String>();
		pSubmittalIds = new ArrayList<String>();
		cData = new HashMap<String,ContactDataContainer>();
		contactForms = new HashMap<String,ContactVO>();
		formFieldMap = new HashMap<String,String>();
		profileIds = new ArrayList<String>();
		se = new StringEncoder();
		this.loadPatientFieldMap();
	}
	
	public static void main(String[] args) {
		AssignmentRetriever pr = new AssignmentRetriever();
		try {
			pr.getDBConnection();
		} catch (Exception e) {
			pr.log.debug("Cannot obtain db connection, " + e);
			System.exit(-1);
		}
		pr.retrieveReport();
		pr.closeDBConnection();
	}
	
	public void retrieveReport() {
		// retrieve main query
		retrieveBaseData();
		
		// get interaction data
		retrieveInteractions();
		
		// retrieve user profiles
		retrieveUserProfiles();
		
		// retrieve form data
		retrieveFormData();
		
		// retrieve contact data
		retrieveContactSubmittals();
		
		// merge patient profiles and form data
		mergeData();
						
		// build report.
		this.buildReportFile();
	}
	
	/**
	 * Retrieves patient data
	 */
	public void retrieveFormData() {
		pData = this.retrieveFormSubmittals(pSubmittalIds, "patient");
		iData = this.retrieveFormSubmittals(iSubmittalIds, "interaction");
	}
	
	/**
	 * Merge patient and interaction data
	 */
	private void mergeData() {
		PatientVO pvo = null;
		UserDataVO profile = null;
		AssignmentVO avo = null;
		for (String key : assignments.keySet()) {
			avo = assignments.get(key);
			// merge patient profile
			pvo = avo.getPatient();
			profile = profiles.get(pvo.getPatientProfileId());
			if (profile != null) {
				pvo.setFirstName(profile.getFirstName());
				pvo.setMiddleName(profile.getMiddleName());
				pvo.setLastName(profile.getLastName());
			}
			// merge patient form data
			pvo.setTransaction(pData.get(pvo.getSubmittalId()));
			
			// merge interaction form data
			for (PatientInteractionVO pivo : avo.getInteractions()) {
				pivo.setTransaction(iData.get(pivo.getSubmittalId()));
			}
		}
	}
		
	/**
	 * Retrieve initial patient/assignment/interaction data
	 */
	public void retrieveBaseData() {
		StringBuilder sql = new StringBuilder();
		sql.append("select a.PATIENT_ID, a.PATIENT_PROFILE_ID, a.FORM_SUBMITTAL_ID, a.CONTACT_SUBMITTAL_ID, "); 
		sql.append("b.PT_ASSIGNMENT_ID, b.ASSIGN_DT, b.ACCEPT_DT, b.UPDATE_DT  ");
		sql.append("from PT_PATIENT a  ");
		sql.append("inner join PT_ASSIGNMENT b on a.PATIENT_ID = b.PATIENT_ID ");
		sql.append("where (b.ASSIGNMENT_STATUS_ID = ? or b.ASSIGNMENT_STATUS_ID = ?) ");
		//sql.append("and b.CREATE_DT < ? ");
		sql.append("and (b.CREATE_DT > ? and b.CREATE_DT < ?) ");
		sql.append("order by b.PATIENT_ID, b.PT_ASSIGNMENT_ID ");
					
		log.debug("base query SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, SJMTrackerConstants.STATUS_COMPLETED);
			ps.setInt(2, SJMTrackerConstants.STATUS_CLOSED);
			ps.setString(3, START_QUERY_DT);
			ps.setString(4, END_QUERY_DT);
			ResultSet rs = ps.executeQuery();
			
			AssignmentVO avo = null;

			while (rs.next()) {
				// add new assignment
				avo = new AssignmentVO();
				avo.setAssignmentId(rs.getString("pt_assignment_id"));
				avo.setAssignDate(rs.getDate("assign_dt"));
				avo.setAcceptDate(rs.getDate("accept_dt"));
				avo.setUpdateDate(rs.getDate("update_dt"));				
				
				// add patient data
				PatientVO pvo = new PatientVO();
				pvo.setPatientId(rs.getString("patient_id"));
				pvo.setPatientProfileId(rs.getString("patient_profile_id"));
				pvo.setSubmittalId(rs.getString("form_submittal_id"));
				pvo.setPatientSourceFormId(rs.getString("contact_submittal_id"));
				avo.setPatient(pvo);
			
				// add to profile lookup list
				if (! profileIds.contains(pvo.getPatientProfileId())) {
					profileIds.add(pvo.getPatientProfileId());	
				}
									
				if (pvo.getPatientSourceProfileId() != null) {
					if (! profileIds.contains(pvo.getPatientSourceProfileId())) {
						profileIds.add(pvo.getPatientSourceProfileId());
					}
				}
				
				// patient form data IDs
				cSubmittalIds.put(pvo.getPatientProfileId(), rs.getString("contact_submittal_id"));
				pSubmittalIds.add(pvo.getSubmittalId());
				
				assignments.put(avo.getAssignmentId(), avo);
			}
			
		} catch (SQLException sqle) {
			log.error("Error retrieving base query data, ", sqle);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {};
		}
		
		log.debug("retrieved " + assignments.size() + " assignments.");

	}
	
	/**
	 * Retrieve interactions
	 */
	public void retrieveInteractions() {
		StringBuilder sql = new StringBuilder();
		sql.append("select b.PT_ASSIGNMENT_ID, b.PT_INTERACTION_ID, b.FORM_SUBMITTAL_ID, b.CREATE_DT "); 
		sql.append("from PT_ASSIGNMENT a ");
		sql.append("inner join PT_INTERACTION b on a.PT_ASSIGNMENT_ID = b.PT_ASSIGNMENT_ID ");
		sql.append("where (a.ASSIGNMENT_STATUS_ID = ? or ASSIGNMENT_STATUS_ID = ?) ");
		//sql.append("and a.CREATE_DT < ? ");
		sql.append("and (a.CREATE_DT > ? and a.CREATE_DT < ?) ");
		sql.append("order by b.PT_ASSIGNMENT_ID, b.CREATE_DT ");
		
		log.debug("interaction SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, SJMTrackerConstants.STATUS_COMPLETED);
			ps.setInt(2, SJMTrackerConstants.STATUS_CLOSED);
			ps.setString(3, START_QUERY_DT);
			ps.setString(4, END_QUERY_DT);
			ResultSet rs = ps.executeQuery();
			
			PatientInteractionVO pivo = null;
			while (rs.next()) {
				pivo = new PatientInteractionVO();
				pivo.setAssignmentId(rs.getString("pt_assignment_id"));
				pivo.setInteractionId(rs.getString("pt_interaction_id"));
				pivo.setSubmittalId(rs.getString("form_submittal_id"));
				
				// add form submittal value to the list
				iSubmittalIds.add(pivo.getSubmittalId());
				
				// add interaction to the appropriate assignment
				assignments.get(pivo.getAssignmentId()).addInteraction(pivo);				
			}
			
		} catch (Exception e) {
			log.error("Error retrieving interactions...", e);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {}
		}
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
	 * Retrieves patient contact submittal data if it exists.
	 */
	public void retrieveContactSubmittals() {
		log.debug("Retrieving user contact submittals...");
		PatientContactDataRetriever pcd = new PatientContactDataRetriever();
		pcd.setDbConn(dbConn);
		TrackerDataContainer tdc = null;
		String contactFormId = null;
		try {
			for (String profileId : cSubmittalIds.keySet()) {
				if (cSubmittalIds.get(profileId) == null) continue;
				pcd.setContactSubmittalId(cSubmittalIds.get(profileId));
				// retrieve the tracker data container
				tdc = pcd.retrievePatientContactData();
				// add the contact data container to the map
				cData.put(profileId, tdc.getContactData());
				contactFormId = tdc.getContactForm().getActionId();
				log.debug("contactFormId is: " + contactFormId);
				// add the contact form to the map if it's not there already
				//if (! contactForms.containsKey(contactFormId)) {
				contactForms.put(profileId, tdc.getContactForm());
				//}
				
			}
		} catch (SQLException sqle) {
			//logged downstream
		}
		log.debug("cData containers: " + cData.size());
	}
	
	/**
	 * Retrieves data based on the form type and form submittal Ids passed in.
	 * @return
	 */
	public Map<String, FormTransactionVO> retrieveFormSubmittals(List<String> fSubs, String type) {
		log.debug("Retrieving form submittals of type: " + type);
		Map<String, FormTransactionVO> fData = null;
		if (fSubs.isEmpty()) return fData;
	
		DataContainer dc = new DataContainer();
		
		// lookup the specified form actionId
		String formId = this.retrieveFormActionId(type);
		if (formId == null) return fData;
		
		fData = new HashMap<String, FormTransactionVO>();
		
		// set up the query for use by the data manager
		GenericQueryVO gqv = new GenericQueryVO(formId);
		gqv.setOrganizationId(SJMTrackerConstants.TRACKER_ORG_ID);
		QueryParamVO vo = new QueryParamVO();
		vo.setColumnNm(ColumnName.FORM_SUBMITTAL_ID);
		vo.setOperator(Operator.in);
		vo.setValues(fSubs.toArray(new String[0]));
		gqv.addConditional(vo);
		
		// retrieve the data
		dc.setQuery(gqv);
		DataManagerFacade dfm = new DataManagerFacade(config, dbConn);
		dc = dfm.loadTransactions(dc);
		fData = dc.getTransactions();
		log.debug("number of transactions retrieved: " + fData.size());
		return fData;
	}
	
	/**
	 * Builds the report file
	 */
	public void buildReportFile() {
		log.debug("building report data file...");
		StringBuilder rep = new StringBuilder();
		this.buildHeaders(rep);
		this.buildRows(rep);
		//log.debug(rep);
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
		rep.append("First Name").append(",");
		rep.append("Middle Initial").append(",");
		rep.append("Last Name").append(",");
		rep.append("State").append(",");
		rep.append("Preferred Method of Contact").append(",");
		rep.append("Phone").append(",");
		rep.append("Email Address").append(",");
		rep.append("Alternative Phone Number").append(",");
		rep.append("Preferred Ambassador").append(",");
		rep.append("Patient's Location of Pain").append(",");
		rep.append("Preferred Gender of Ambassador").append(",");
		rep.append("Patient's Age Range").append(",");
		rep.append("Most Important Match Factor").append(",");
		rep.append("Best Time to Contact Patient").append(",");
		rep.append("Doctor's Name").append(",");
		rep.append("Is Interaction Shareable With Patient's Physician?").append(",");
		rep.append("Current Phase of SCS Treatment Continuum").append(",");
		rep.append("Email address of SJM Representative").append(",");
		rep.append("Date Assigned").append(",");
		rep.append("Date Accepted").append(",");
		rep.append("Date Completed").append(",");
		rep.append("Interaction Date").append(",");
		rep.append("Method of Interaction").append(",");
		rep.append("Duration of Interaction").append(",");
		rep.append("Submitted By");
		rep.append("\n");
	}
	
	/**
	 * 
	 * @param rep
	 */
	public void buildRows(StringBuilder rep) {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		AssignmentVO avo = null;
		for (String key : assignments.keySet()) {
			// build a row for each interaction
			avo = assignments.get(key);
			for (PatientInteractionVO pivo : avo.getInteractions()) {				
				this.buildSingleRow(avo, pivo, pnf, rep);
			}
		}
	}
	
	/**
	 * Appends additional patient data to the report.
	 * @param pnf
	 * @param rep
	 * @param patient
	 */
	private void buildSingleRow(AssignmentVO asgn, PatientInteractionVO pivo, PhoneNumberFormat pnf, StringBuilder rep) {
		
		/* PATIENT DATA */
		PatientVO patient = asgn.getPatient();
		String tmpVal = se.decodeValue(patient.getFirstName());
		tmpVal = tmpVal.replace(",","");
		rep.append(tmpVal).append(",");
		if (StringUtil.checkVal(patient.getMiddleName()).length() > 0) {
			rep.append(patient.getMiddleName().substring(0,1)).append(",");
		} else {
			rep.append(",");
		}
		tmpVal = se.decodeValue(patient.getLastName());
		tmpVal.replace(",","");
		rep.append(tmpVal).append(",");
		
		FormTransactionVO fvo = pData.get(patient.getSubmittalId());
		Map<String,FormFieldVO> fields = fvo.getCustomData();
		
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("stateCode")))).append(",");		
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("contactMethod")))).append(",");
		pnf.setPhoneNumber(profiles.get(patient.getPatientProfileId()).getMainPhone());
		rep.append(StringUtil.checkVal(pnf.getFormattedNumber(),"n/a")).append(",");
		rep.append(StringUtil.checkVal(profiles.get(patient.getPatientProfileId()).getEmailAddress(),"n/a")).append(",");
		tmpVal = se.decodeValue(this.parseFieldResponse(fields.get(formFieldMap.get("phoneAlternate"))));
		tmpVal.replace(",","");
		rep.append(tmpVal).append(",");
		
		// requested ambassador is in last initial comma first name format (e.g. S, Joe).  we have
		// to remove the comma or the field will be mis-positioned.
		String amb = this.parseFieldResponse(fields.get(formFieldMap.get("ambRequested")));
		if (amb.length() > 0) {
			int comma = amb.indexOf(",");
			if (comma > -1) {
				String first = "";
				if (amb.length() > (comma + 1)) {
					first = amb.substring(comma + 1).trim(); // first name
				}
				String last = amb.substring(0, comma);
				amb = first + " " + last;
			}
		}
		rep.append(amb).append(",");
		
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("painLocation")))).append(",");		
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("genderPreference")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("ageRange")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("matchFactor")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("contactTime")))).append(",");
		
		String doctor = this.parseFieldResponse(fields.get(formFieldMap.get("physicianName")));
		doctor = se.decodeValue(doctor);
		if (doctor.indexOf(",") > -1) {
			doctor = doctor.replace(",", "");
		}
		rep.append(doctor).append(",");
		
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("shareInfo")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("treatmentPhase")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("patientSJMRep")))).append(",");
	
		/* ASSIGNMENT DATA */
		rep.append(asgn.getAssignDate()).append(",");
		rep.append(asgn.getAcceptDate()).append(",");
		rep.append(asgn.getUpdateDate()).append(",");
		
		/* INTERACTION DATA */
		fvo = iData.get(pivo.getSubmittalId());
		fields = fvo.getCustomData();
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("interactDate")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("interactMethod")))).append(",");
		rep.append(this.parseFieldResponse(fields.get(formFieldMap.get("interactDuration")))).append(",");
		
		/* SUBMITTED BY */
		// capture submittedBy for later use
		String submittedBy = this.retrieveSubmittedByInfo(patient);
		rep.append(submittedBy); // LAST field in row, NO COMMA
		
		// add end of row marker
		rep.append("\n");
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
	private String retrieveSubmittedByInfo(PatientVO patient) {
		ContactVO contactForm = contactForms.get(patient.getPatientProfileId());
		ContactDataContainer contactData = cData.get(patient.getPatientProfileId());
		
		String submittedBy = "n/a";
		if (patient.getPatientSourceFormId() != null) {
			// patient was created via a Contact Us form
			// retrieve the form data
			log.debug("patient source contact form ID is: " + patient.getPatientSourceFormId());
			if (contactForm != null) {
				String formId = contactForm.getActionId();
				log.debug("patient's source contact form actionId: " + formId);
				List<ContactDataModuleVO> data = contactData.getData();
				if (data != null) {
					log.debug("patient data list size is " + data.size());
					if (! data.isEmpty()) {
						ContactDataModuleVO vo = data.get(0);
						if (vo != null) {
							if (vo.getExtData() != null && ! vo.getExtData().isEmpty()) {
								log.debug("found contact form data for contact form ID: " + formId);
								if(formId.equals("c0a80241203d1bbac210bd5923703429")) { // POYP
									submittedBy = "Patient";
								} else if (formId.equals("c0a80237aebf15939fc972a3f8b15540")) { // SalesNet CS form
									submittedBy = StringUtil.checkVal(vo.getExtData().get("c0a80241210724aa91486e27c72d644"));
									if (submittedBy.length() == 0) {
										submittedBy = "Customer Service";
									}									
								} else if (formId.equals("c0a8024121202bfe526d209c678fdd5b")) { // SalesNet Field Rep form
									submittedBy = StringUtil.checkVal(vo.getExtData().get("c0a80241210724aa91486e27c72d644"));
									if (submittedBy.length() == 0) {
										submittedBy = "SJM Representative";
									}									
								}
							}
						} 
					} 
				} 
			}
		}
		return submittedBy;
	}
	
	/**
	 * loads map of keys to form_field_id values
	 */
	private void loadPatientFieldMap() {
		// key, form_field_id
		// patient fields
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
		formFieldMap.put("patientSJMRep", "c0a802378d49c279918a2402e41e03c0");
		formFieldMap.put("submittedBy", "c0a80241210724aa91486e27c72d644");
		// interaction fields
		formFieldMap.put("interactDate", "c0a802376667680bf183e3a0a8e46e24");
		formFieldMap.put("interactMethod", "c0a802375f992483764c41aada43daa");
		formFieldMap.put("interactDuration", "c0a80237666be04d71bc888acf98847c");
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

}
