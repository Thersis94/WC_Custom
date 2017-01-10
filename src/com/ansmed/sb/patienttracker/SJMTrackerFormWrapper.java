package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// Sitebuilder II libs
import com.smt.sitebuilder.action.contact.ContactAction;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.contact.SubmittalAction;
import com.smt.sitebuilder.action.contact.SubmittalDataAction;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.PatientAction;
import com.smt.sitebuilder.action.tracker.PatientManager;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.tracker.data.ProfileNameComparator;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
* <b>Title</b>SJMTrackerFormWrapper.java<p/>
* <b>Description: </b> Used to wrap certain SJM forms so that patient data can be ingested into the
* SJM Patient Tracker.
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Aug 23, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMTrackerFormWrapper extends TrackerAction {
	
	public static final String PATIENT_FORM_ID = "c0a80237661f0eb4563341864fd8a52a";
	public static final String ASSIGNEE_FORM_ID = "c0a80237661feb561b100221e0a9c7f6";
	private final String FIELD_REQUEST_FORM_ID = "c0a8024121202bfe526d209c678fdd5b";
	private final String FIELD_FORM_SJM_EMAIL_FIELD_ID = "con_c0a802412128bdb4508e0afbc2e25fe";
	private final Integer AMB_TYPE_AMB = 0;
	private Map<String, String> formFieldMap = null;
	
	public SJMTrackerFormWrapper() {
		super();
		formFieldMap = new HashMap<String, String>();
	}
	
	public SJMTrackerFormWrapper(ActionInitVO actionInit) {
		super(actionInit);
		formFieldMap = new HashMap<String, String>();
	}
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.ActionController#delete(com.siliconmtn.http.SMTServletRequest)
     */
	public void delete(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		super.delete(req);
        
        // Redirect the user
		SiteBuilderUtil util = new SiteBuilderUtil();
        util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("SJMTrackerFormWrapper retrieve...");
		
		// 1. retrieve the wrapped form
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String oldInitId = actionInit.getActionId();
    	String newInitId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
    	actionInit.setActionId(newInitId);
    	// set the action group ID so that the retrieval works properly
    	req.setParameter("actionGroupId", newInitId);
    	log.debug ("oldInitId/attribute_1 actionId: " + oldInitId + "/" + actionInit.getActionId());
    	
    	SMTActionInterface eg = new ContactAction(this.actionInit);
    	eg.setAttributes(this.attributes);
    	eg.setDBConnection(dbConn);
    	eg.retrieve(req);

		TrackerDataContainer tdc = new TrackerDataContainer();
		tdc.setContactForm((ContactVO)(req.getAttribute(ContactAction.RETRV_CONTACT)));
		
		// 2. retrieve "today's" ambassadors, place on container
		List<AssigneeVO> assignees = this.retrieveTodaysAmbassadors();
		tdc.setAssignees(assignees);
		
		// 3. put the data onto the module
		mod.setActionData(tdc);
		attributes.put(Constants.MODULE_DATA, mod);
		actionInit.setActionId(oldInitId);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("SJMTrackerFormWrapper build...");
		
		// 1. preserve the actionIds from the build request.
    	String wrapperInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String contactInitId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
    	mod = null;
    	log.debug("processing Contact Us with action ID of: " + contactInitId);
    	
    	// 2. populate the request with the 'tracker-equivalent' field values
    	this.loadPatientFieldMap();
    	this.loadPatientRequestFields(req, contactInitId);
		
    	//3. Check to see if patient already exists and is currently not assigned to anyone
    	//10/12/2012 PHASE3 #3006
    	String profileId = this.checkForProfile(req);
    	log.debug("profileId from lookup: " + profileId);
    	boolean isDuplicatePatient = false;
    	boolean doNotAssign = false;
    	
    	PatientVO thePatient = new PatientVO();
    	thePatient.setProfileId(profileId);
    	//#3006
    	if (thePatient.getProfileId() != null) {
        	isDuplicatePatient = this.isDuplicatePatient(thePatient);
        	if (isDuplicatePatient) {
        		doNotAssign = this.checkRecentPatientInteractions(thePatient);
        	}
    	}
    	AssigneeVO assignee = null;
    	// Setup the redirect stub
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?contactSubmitted=true");
    	url.append("&responseId=").append(req.getParameter("pmid"));
    	if (doNotAssign) { // patient already exists and has been interacted with in past 7 days.
    		url.append("&ambassadorAssigned=false");
    		url.append("&cause=preexisting");
    	} else {
	    	// proxy the org ID so that we read/write tracker data correctly
			req.setParameter("wrapperOrganizationId", "SJM_AMBASSADORS");
			//PatientVO patient = null;
			if (! isDuplicatePatient) { // new patient, create patient records
				// 3a. create the patient tracker patient records from the submitted form data
				this.createPatient(req);
	    		// retrieve newly created patient from request
				thePatient = (PatientVO)req.getAttribute(PatientAction.PATIENT_DATA);
			    	
				// 3b. Process the submitted contact form data.
		        actionInit.setActionId(contactInitId); // set the action ID to the ID of the Contact Us form
		    	this.writeWrappedFormData(req, contactInitId, thePatient);
		    	actionInit.setActionId(wrapperInitId); // reset the action ID
		    	
		    	// 3c. update the patient base record with the contact submittal id as the source id
		    	this.updatePatientBaseRecord(thePatient);
			}
	    	
	    	// 4. now retrieve the entire patient record
			 thePatient = this.retrieveFullPatientRecord(req, thePatient, isDuplicatePatient);
	    	
	    	// 5 retrieve assignee form VO so that we have knowledge of field names
	    	// when assigning an ambassador
	    	log.debug("retrieving assigneeForm");
	    	FormVO assigneeForm = this.retrieveForm(req, "assignee");
	
	    	// 6. assign the patient to an ambassador
	    	assignee = this.assignAmbassador(req, assigneeForm, thePatient);
	    	
	    	//If this is the field form, email rep and append ambassador's name as parameter to the redirect
	    	if (contactInitId.equals(FIELD_REQUEST_FORM_ID)) {
	    		log.debug("this is the field form...");
		    	if (assignee != null) {
	    			this.sendRepEmail(req, assignee);
		    		url.append("&ambassadorAssigned=true");
		    		//url.append(assignee.getFirstName()).append(" ").append(assignee.getLastName());
		    	}
		    	String teamEmailAddr = StringUtil.checkVal(req.getParameter(FIELD_FORM_SJM_EMAIL_FIELD_ID));
		    	if (StringUtil.isValidEmail(teamEmailAddr)) {
		    		log.debug("team email address: " + req.getParameter(FIELD_FORM_SJM_EMAIL_FIELD_ID));
		    		this.sendSJMMemberEmail(req, assignee, thePatient);
		    	}
	    	}
    	}
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());    	
    	log.debug("SJMTrackerFormWrapper redirect URL: " + url);
	}
	
	/**
	 * Performs checks to see if a patient already exists in the Patient table.
	 * @param req
	 * @return True if the patient exists.  False if the patient does not exist.
	 */
	private String checkForProfile(ActionRequest req) {
		log.debug("checking for duplicate patient profile...");
		String profileId = null;
		UserDataVO user = new UserDataVO(req);
		user.setFirstName(req.getParameter("pfl_FIRST_NM"));
		user.setLastName(req.getParameter("pfl_LAST_NM"));
		user.setEmailAddress(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		if (StringUtil.checkVal(user.getEmailAddress()).length() > 0) {
			// check for profile using email address
			profileId = this.findProfileUsingEmail(pm, user);
		} else {
			PhoneVO userPhone = new PhoneVO();
			userPhone.setPhoneNumber(StringUtil.checkVal(req.getParameter("pfl_MAIN_PHONE_TXT")));
			userPhone.setTypeName(PhoneVO.HOME_PHONE);
			//reset the phone list because UserDataVO creates null phones when instantiated
			user.setPhoneNumbers(new ArrayList<PhoneVO>());
			user.addPhone(userPhone);
			profileId = this.findProfileUsingPhone(pm, user);
		}
		return profileId;
	}
	
	/**
	 * Checks to see if a duplicate patient registrant has been interacted with in the past 7 days.  If so,
	 * the patient will not be registered again and will not be assigned to an ambassador. 
	 * @param thePatient
	 * @return
	 */
	private boolean checkRecentPatientInteractions(PatientVO thePatient) {
		log.debug("checking recent patient interactions...");
		boolean doNotAssign = false;
		StringBuilder s = new StringBuilder();
		s.append("select a.patient_profile_id from PT_PATIENT a ");
		s.append("inner join PT_ASSIGNMENT b on a.PATIENT_ID = b.PATIENT_ID ");
		s.append("left join PT_ASSIGNMENT_LOG c on b.PT_ASSIGNMENT_ID = c.PT_ASSIGNMENT_ID ");
		s.append("where a.PATIENT_PROFILE_ID = ? and c.create_dt >= ? and c.assignment_status_id <= ?");
		log.debug("check recent interactions SQL: " + s.toString());
		log.debug("patient profile id: " + thePatient.getProfileId());
		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		Date start = cal.getTime();
		start = Convert.formatStartDate(start);
		log.debug("comm date: " + start.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, thePatient.getProfileId());
			ps.setDate(2, Convert.formatSQLDate(start));
			ps.setInt(3, SJMTrackerConstants.STATUS_COMPLETED);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				doNotAssign = true;
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving patient's recent interaction count, ",sqle);
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
			} catch(Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.debug("do not assign? : " + doNotAssign);
		return doNotAssign;
	}
	
	/**
	 * Performs a profile ID lookup based on first/last name and email address on the UserDataVO.
	 * @param pm
	 * @param user
	 * @return Profile ID of the profile that is associated with the email address on the user data bean.
	 */
	private String findProfileUsingEmail(ProfileManager pm, UserDataVO user) {
		log.debug("checking for existing profile via email address...");
		String profileId = null;
		try {
			profileId = pm.checkProfile(user, dbConn);
		} catch (DatabaseException dbe) {
			log.error("Error while attempting to retrieve profile data, ", dbe);
		}
		return profileId;
	}
	
	/**
	 * Performs a profile ID look-up based on first/last name and phone number on the UserDataVO.
	 * @param pm
	 * @param user
	 * @return
	 */
	private String findProfileUsingPhone(ProfileManager pm, UserDataVO user) {
		// check for profile using phone number
		log.debug("checking for existing profile via phone...");
		String profileId = null;
		StringBuilder s = new StringBuilder();
		s.append("select a.profile_id from profile a ");
		s.append("inner join phone_number b on a.profile_id = b.profile_id ");
		s.append("where b.phone_number_txt = ? and b.phone_country_cd = ?");
		log.debug("Profile by phone SQL: " + s.toString());
		log.debug("first/last/phone: " + user.getFirstName() + "/" + user.getLastName() + "/" + user.getPhoneNumbers().get(0).getPhoneNumber());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, pm.getEncValue("PHONE_NUMBER_TXT", user.getPhoneNumbers().get(0).getPhoneNumber()));
			ps.setString(2, "US");
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				profileId = rs.getString("profile_id");
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving profile ID via phone number, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return profileId;
	}
	
	/**
	 * Performs a patient lookup based on the profile_id value passed in.
	 * @param thePatient
	 * @return True if the profile ID is found in the patient table, otherwise false.
	 */
	private boolean isDuplicatePatient(PatientVO thePatient) {
		log.debug("checking patient table for duplicate patient profile ID...");
		boolean found = false;
		String sql = "select * from pt_patient where patient_profile_id = ?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, thePatient.getProfileId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				thePatient.setData(rs);
				found = true;
			}
		} catch (SQLException sqle) {
			log.error("Error looking up patient profile id, ", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		log.debug("duplicate? : " + found);
		return found;
	}
	
	/**
	 * Creates patient and assigns ambassador to patient
	 * @param req
	 * @throws ActionException
	 */
	private void createPatient(ActionRequest req) throws ActionException {
		log.debug("creating patient base record, extended data, and profile...");
		// set request parameters needed downstream to create patient
		req.setParameter("sbActionId", PATIENT_FORM_ID, true);
		// patient base record and extended data records
		SMTActionInterface sai = new PatientManager(this.actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
	}
	
	/**
	 * Writes the wrapped contact form's data.
	 * @param req
	 * @param patient
	 * @throws ActionException
	 */
	private void writeWrappedFormData(ActionRequest req, String contactInitId, PatientVO patient) throws ActionException {
    	// pull the logged in user's data off of the session for now.
    	log.debug("Removing proxy user's session data temporarily.");
    	UserDataVO proxyUser = null;
    	if (req.getSession().getAttribute(Constants.USER_DATA) != null) {
    		proxyUser = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
    	}
    	req.getSession().removeAttribute(Constants.USER_DATA);
    	
		String contactSubmittalId = new UUIDGenerator().getUUID();
		String profileId = null;
		String emailAddress = null;
		
		// try to set profileId and emailAddress
		if (patient != null) {
			profileId = patient.getProfileId() != null ? patient.getProfileId() : null;
			emailAddress = patient.getEmailAddress() != null ? patient.getEmailAddress() : null;
		}
		
		log.debug("profileId from patientVO is: " + profileId);
		
		// Build the insert statement
		StringBuffer sql = new StringBuffer();
		sql.append("insert into contact_submittal (contact_submittal_id, ");
		sql.append("profile_id, site_id, action_id, email_address_txt, ");
		sql.append("create_dt, accepted_privacy_flg, dealer_location_id) ");
		sql.append("values(?,?,?,?,?,?,?,?)");
		
		String siteId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteId();
		log.debug("SJMTrackerFormWrapper contact Submital sql: " + sql.toString());

        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, contactSubmittalId);
            ps.setString(2, profileId);
            ps.setString(3, siteId);
            ps.setString(4, actionInit.getActionId());
            ps.setString(5, emailAddress);
            ps.setTimestamp(6, Convert.getCurrentTimestamp());
            ps.setInt(7, 1);
            ps.setString(8, req.getParameter(Constants.DEALER_LOCATION_ID_KEY));
            ps.execute();
        } catch (SQLException sqle) {
            log.debug("Error inserting contact submittal via SJMTrackerFormWrapper, ", sqle);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // put the profileId on the request.
		req.setParameter("profileId", profileId);
        // put the Contact Submittal Id on the request object
        req.setAttribute(SubmittalAction.CONTACT_SUBMITTAL_ID, contactSubmittalId);
        
        // Call the action to set the specific field data
        SMTActionInterface aac = new SubmittalDataAction(this.actionInit);
        aac.setAttributes(this.attributes);
        aac.setDBConnection(dbConn);
        aac.build(req);

        // set contact submittal id as patient's source id
        if (patient != null) patient.setPatientSourceFormId(contactSubmittalId);
        // set proxy user's profile ID as patient's source profile id if applicable.
        if (proxyUser != null) {
        	patient.setPatientSourceProfileId(proxyUser.getProfileId());
        }
        
    	// Make sure any newly created user data is not on session before
    	// putting proxy user's data back on session.
    	req.getSession().removeAttribute(Constants.USER_DATA);
    	
    	// put proxy user's data back on session
    	log.debug("Adding 'proxy' user's data back onto the session.");
    	req.getSession().setAttribute(Constants.USER_DATA, proxyUser);
	}
	
	/**
	 * Updates patient base record with source submittal id
	 * @param req
	 * @param patientId
	 */
	private void updatePatientBaseRecord(PatientVO patient) {
		if (patient == null || StringUtil.checkVal(patient.getPatientSourceFormId()).length() == 0) return;
		
		StringBuffer sql = new StringBuffer();
		sql.append("update pt_patient set contact_submittal_id = ? where patient_id = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, patient.getPatientSourceFormId());
			ps.setString(2, patient.getPatientId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error updating patient record with source ID, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	private PatientVO retrieveFullPatientRecord(ActionRequest req, PatientVO patient, boolean isDuplicate) {
		log.debug("retrieving new patient's full data");
		PatientVO newPatient = null;
		// set patient's patient ID and form submittal ID on request
		req.setParameter("patientId", patient.getPatientId(), true);
		req.setParameter("formSubmittalId", patient.getSubmittalId(), true);
		
		// we need to proxy the retrieval as an admin role so the retrieval doesn't fail
    	SBUserRole proxyRole = new SBUserRole();
    	proxyRole.setOrganizationId(AmbassadorRetriever.ORGANIZATION_ID);
    	proxyRole.setRoleId("100");
    	proxyRole.setRoleLevel(100);
    	
    	// preserver user's original role, if any 
    	SBUserRole origRole = null;
    	if (req.getSession().getAttribute(Constants.ROLE_DATA) != null) {
    		origRole = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
    	}
    	// remove user's original role
    	req.getSession().removeAttribute(Constants.ROLE_DATA);    	
    	// place proxy role on request
    	req.getSession().setAttribute(Constants.ROLE_DATA, proxyRole);
		
		// retrieve patient
		SMTActionInterface sai = new PatientManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		try {
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error retrieving new patient data, ", ae);
			return patient;
		} finally {
			// remove proxy role
			req.getSession().removeAttribute(Constants.ROLE_DATA);
			// restore user's original role, if any
			if (origRole != null) {
				req.getSession().setAttribute(Constants.ROLE_DATA, origRole);
			}	
		}
	
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		
		if (tdc.getPatients() != null && !tdc.getPatients().isEmpty()) {
			newPatient = tdc.getPatients().get(0);
		} else {
			// nothing retrieved, return what we started with.
			newPatient = patient;
		}
		return newPatient;
	}
	
	/**
	 * 
	 * @param req
	 * @param assigneeForm
	 * @param patient
	 * @throws ActionException
	 */
	private AssigneeVO assignAmbassador(ActionRequest req, FormVO assigneeForm, 
			PatientVO patient) throws ActionException {
		log.debug("choosing and assigning ambassador...");
		AssigneeVO assignee = null;
		if (patient == null) return assignee;
		String wrapperActionId = StringUtil.checkVal(req.getParameter("sbActionId"));
		log.debug("wrapperActionId: " + wrapperActionId);
		// 1. retrieve an ambassador to assign to this patient
		assignee = chooseAmbassador(req, assigneeForm, patient);
		if (assignee != null) {
			if (assignee.getAssigneeId().length() > 0) {
				// 2. remove any formSubmittalId from the request so it doesn't
				// get re-used in subsequent 'black box' ops (assignment, assignment logging).
				String fsi = null;
				req.setParameter("formSubmittalId", fsi, true);
				// 3. make assignment if the match succeeded
				req.setParameter("assigneeId", assignee.getAssigneeId(), true);
				req.setParameter("patientId", patient.getPatientId());		
				req.setParameter("assignmentStatusId", SJMTrackerConstants.STATUS_PENDING.toString());
				
				// assignment logging parameters
				StringBuffer toText = new StringBuffer("Patient assigned to ambassador ");
				toText.append(assignee.getFirstName()).append(" ").append(assignee.getLastName().substring(0,1));
				req.setParameter("logSystemText", toText.toString());
				
				SMTActionInterface sai = new SJMAssignmentFacade(this.actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.build(req);
			}
		}
			
		// replace sbActionId with original value
		req.setParameter("sbActionId", wrapperActionId, true);
		return assignee;
	}
	
	/**
	 * Determines the ambassador assigneeId to pass to the assignment manager
	 * @param req
	 * @param assigneeForm
	 * @param patient
	 * @return
	 */
	private AssigneeVO chooseAmbassador(ActionRequest req, FormVO assigneeForm, PatientVO patient) {
		AssigneeVO match = null;
		// retrieve today's active ambassadors, including admins
		List<AssigneeVO> ambassadors = this.retrieveAmbassadors();
		// check to see if an ambassador was specified
		String assigneeId = StringUtil.checkVal(req.getParameter("assigneeId"));
		// if assigneeId does not exist on request, find an ambassador match
		if (assigneeId.length() == 0 || assigneeId.equalsIgnoreCase("unknown")) {
			log.debug("choosing an ambassador...");
			// find a match
			AmbassadorMatcher am = new AmbassadorMatcher();
			am.setDbConn(dbConn);
			am.setPatient(patient);
			am.setAmbassadors(ambassadors);
			am.setAmbassadorForm(assigneeForm);
			match = am.findAmbassadorMatch();
			assigneeId = match.getAssigneeId();
		} else {
			for (AssigneeVO assignee : ambassadors) {
				if (assigneeId.equals(assignee.getAssigneeId())) {
					match = assignee;
					break;
				}
			}
		}
		return match;
	}
	
	/**
	 * Retrieves all ambassadors available today
	 * @return
	 */
	private List<AssigneeVO> retrieveAmbassadors() {
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setAttributes(attributes);
		sar.setDbConn(dbConn);
		// make sure we filter out non-admins who are at max assignment count
		sar.setCheckAssignmentLimit(true);
		List<AssigneeVO> ambassadors = null;
		try {
			ambassadors = sar.retrieveAmbassadors();	
		} catch (SQLException sqle) {
			log.error("Error retrieving base records for ambassadors, ", sqle);
		}
		log.debug("ambassador list size: " + (ambassadors != null ? ambassadors.size() : "null"));
		return ambassadors;
	}

	/**
	 * Retrieves all ambassadors available today and filters out the admins
	 * @return
	 */
	private List<AssigneeVO> retrieveTodaysAmbassadors() {
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setAttributes(attributes);
		sar.setDbConn(dbConn);
		// make sure we filter out ambs who are at max assignment count
		sar.setCheckAssignmentLimit(Boolean.TRUE);
		// retrieve only non-admins
		sar.setAmbassadorType(AMB_TYPE_AMB);
		List<AssigneeVO> ambassadors = null;
		
		try {
			ambassadors = sar.retrieveAmbassadors();	
		} catch (SQLException sqle) {
			log.error("Error retrieving base records for ambassadors, ", sqle);
		}
		log.debug("ambassador list size: " + (ambassadors != null ? ambassadors.size() : "null"));
		// filter out the admins
		List<AssigneeVO> returnList = null;
		log.debug("filtering out admins");
		if (ambassadors != null) {
			returnList = new ArrayList<AssigneeVO>();
			for (AssigneeVO amb : ambassadors) {
				if (amb.getTypeId() < AssigneeManager.MIN_ADMIN_TYPE_ID) returnList.add(amb);
			}
			log.debug("returnList size: " + returnList.size());
			// sort alphabetically
			Collections.sort(returnList, new ProfileNameComparator());
		}
		return returnList;
	}
	
	/**
	 * Loads map of keys to contact form field IDs.
	 * @param req
	 * @param formActionId
	 */
	private void loadPatientRequestFields(ActionRequest req, String formActionId) {
		log.debug("loading patient request fields");
		Map<String, String> contactFieldMap = new HashMap<String, String>();
		// key, contact field id
		if (formActionId.equals("c0a80241203d1bbac210bd5923703429")) {
			// POYP patient form
			log.debug("creating contactFieldMap for contact action ID of: " + formActionId);
			contactFieldMap.put("ageRange","con_c0a80241205021fe476c5167b6cb6543");
			contactFieldMap.put("ambQuestion","con_c0a802412056828f448e263bd09b9754");
			contactFieldMap.put("ambRequested","assigneeName");
			contactFieldMap.put("contactMethod","con_c0a8024120426e936d7928f95bd7ecfa");
			contactFieldMap.put("contactTime","con_c0a802412043f876fa18fe526c170871");
			contactFieldMap.put("genderPreference","con_c0a80241204d6c138002b66d9750357b");
			contactFieldMap.put("matchFactor","con_c0a802412052252aad5da4b8de929ce1");
			contactFieldMap.put("painLocation","con_c0a80241204c9048ef7bb4e0c1fe2b2d");
			contactFieldMap.put("phoneAlternate","con_c0a802412042cbef1951af39bb0a823a");
			contactFieldMap.put("physicianFirstName","con_c0a80241a67319f124069def29053020");
			contactFieldMap.put("physicianLastName","con_c0a802373bb5f6983157b5a79f3487");
			contactFieldMap.put("shareInfo","con_c0a8024120560922764fb70a08e934");
			contactFieldMap.put("treatmentPhase","con_c0a80241205ad8535e4139cdfb920964");
			contactFieldMap.put("stateCode","pfl_STATE_CD");
			
		} else if (formActionId.equals("c0a8024121202bfe526d209c678fdd5b")) {
			// SalesNet rep form
			log.debug("creating contactFieldMap for contact action ID of: " + formActionId);
			contactFieldMap.put("ageRange","con_c0a8024120ea9c005e14c53093e087b0");
			contactFieldMap.put("ambQuestion","con_c0a802412106e1fce34ca96fe26ae5a4");
			contactFieldMap.put("contactMethod","con_c0a8024120e4240cef3941a3bdf65011");
			contactFieldMap.put("contactTime","con_c0a8024120e56b9a4d42e0bd1b128b94");
			contactFieldMap.put("genderPreference","con_c0a8024120e9ed8f51a30800c24fcb9c");
			contactFieldMap.put("matchFactor","con_c0a8024120ee9e43d89c79693278999");
			contactFieldMap.put("painLocation","con_c0a8024120e98fbc41b4c0dcffe03b8d");
			contactFieldMap.put("phoneAlternate","con_c0a8024120e50ff7c9076df1835f823e");
			contactFieldMap.put("stateCode","pfl_STATE_CD");
			contactFieldMap.put("patientsRepEmailAddress","con_c0a802418c7a78681c6861289102cdbb");
			
		} else if (formActionId.equals("c0a80237aebf15939fc972a3f8b15540")) {
			// SalesNet customer service(CS) form
			log.debug("creating contactFieldMap for contact action ID of: " + formActionId);
			contactFieldMap.put("ageRange","con_c0a8024120ea9c005e14c53093e087b0");
			contactFieldMap.put("ambQuestion","con_c0a802412106e1fce34ca96fe26ae5a4");
			contactFieldMap.put("ambRequested","assigneeName");
			contactFieldMap.put("contactMethod","con_c0a8024120e4240cef3941a3bdf65011");
			contactFieldMap.put("contactTime","con_c0a8024120e56b9a4d42e0bd1b128b94");
			contactFieldMap.put("genderPreference","con_c0a8024120e9ed8f51a30800c24fcb9c");
			contactFieldMap.put("matchFactor","con_c0a8024120ee9e43d89c79693278999");
			contactFieldMap.put("painLocation","con_c0a8024120e98fbc41b4c0dcffe03b8d");
			contactFieldMap.put("phoneAlternate","con_c0a8024120e50ff7c9076df1835f823e");
			contactFieldMap.put("physicianFirstName","con_c0a8024120f1d7ba2c6bbd09581ab092");
			contactFieldMap.put("physicianLastName","con_c0a802373bb3622f825876e29def1b3c");
			contactFieldMap.put("shareInfo","con_c0a8024120f279b3148f9bf14d3e4315");
			contactFieldMap.put("stateCode","pfl_STATE_CD");
			contactFieldMap.put("treatmentPhase","con_c0a8024121010b8ea1e1f1626dd6d326");
			contactFieldMap.put("patientsRepEmailAddress","con_c0a802418c7a78681c6861289102cdbb");
			
		}
		this.addPatientRequestParameters(req, contactFieldMap, formActionId);
	}
	
	/**
	 * loads map of keys to form_field_id values
	 */
	private void loadPatientFieldMap() {
		// key, form_field_id
		formFieldMap.put("ageRange","frm_c0a80237662b11dbb5c0326460296ed1");
		formFieldMap.put("ambQuestion","frm_c0a802376631104dd8f9dec9ad1e83a8");
		formFieldMap.put("ambRequested","frm_c0a802376626c3b5588a4c246c0cb039");
		formFieldMap.put("contactMethod","frm_c0a8023766250c46418ec22b852e7838");
		formFieldMap.put("contactTime","frm_c0a80237665cd05bce9bc8c2b200318a");
		formFieldMap.put("genderPreference","frm_c0a8023766290edfaabaeaee10fd075c");
		formFieldMap.put("matchFactor","frm_c0a80237662d0a7775f2231c9c7c3743");
		formFieldMap.put("painLocation","frm_c0a8023766282af6d33fce93e7d2d285");
		formFieldMap.put("phoneAlternate","frm_c0a8023766379ebc9feb6e1ae7f358f7");
		formFieldMap.put("physicianName","frm_c0a80237662fef282f3c4e368846f14c");
		formFieldMap.put("shareInfo","frm_c0a802376630a3a7ad52730e42c72ac3");
		formFieldMap.put("stateCode","frm_c0a80241ad8ec5b1902f1fc5b9f6ce42");
		formFieldMap.put("treatmentPhase","frm_c0a802376636bc5a8422a4fbc1157895");
		formFieldMap.put("patientsRepEmailAddress","frm_c0a802378d49c279918a2402e41e03c0");
		
	}
	
	/**
	 * Compares contact field keys to form field keys.  When a match is found the form field key value is
	 * added to the request for use downstream.
	 * @param req
	 * @param contactFieldMap
	 */
	private void addPatientRequestParameters(ActionRequest req, Map<String, String> contactFieldMap, String formActionId) {
		log.debug("adding patient request parameters for 'black box' field equivalents...");
		// loop the field map for the given contact form and look for a corresponding field
		// in the black box form
		for (String key : contactFieldMap.keySet()) {
			String formFieldId = formFieldMap.get(key);
			if (StringUtil.checkVal(formFieldId).length() > 0) {
				// we found a corresponding black box form field so add it to the request
				req.setParameter(formFieldId, req.getParameter(contactFieldMap.get(key)));
				log.debug("added form field id to request: " + formFieldId);
			}
		}
		
		// make sure the physician name is set for certain forms
		String firstName = null;
		String lastName = null;
		if (formActionId.equals("c0a80241203d1bbac210bd5923703429")) {
			//firstName = contactFieldMap.get("con_c0a80241a67319f124069def29053020");
			//lastName = contactFieldMap.get("con_c0a802373bb5f6983157b5a79f3487");
			firstName = req.getParameter(contactFieldMap.get("physicianFirstName"));
			lastName = req.getParameter(contactFieldMap.get("physicianLastName"));
			// replace the amb requested value on the original form
			req.setParameter("con_c0a80241204607518c754f39ec1eff30", req.getParameter("assigneeName"), true);
		} else if (formActionId.equals("c0a80237aebf15939fc972a3f8b15540")) {
			//firstName = contactFieldMap.get("con_c0a8024120f1d7ba2c6bbd09581ab092");
			//lastName = contactFieldMap.get("con_c0a802373bb3622f825876e29def1b3c");
			firstName = req.getParameter(contactFieldMap.get("physicianFirstName"));
			lastName = req.getParameter(contactFieldMap.get("physicianLastName"));
			// replace the amb requested value on the original form
			req.setParameter("con_c0a8024120f11e8f3841daa3e51adf30", req.getParameter("assigneeName"), true);
		}
		StringBuffer physName = new StringBuffer();
		if (StringUtil.checkVal(firstName).length() > 0) {
			physName.append(firstName);
		}
		if (StringUtil.checkVal(lastName).length() > 0) {
			if (physName.length() > 0) {
				physName.append(" ");
			} 
			physName.append(lastName);
		}
		if (physName.length() > 0) req.setParameter("frm_c0a80237662fef282f3c4e368846f14c", physName.toString());
		log.debug("physician name: " + physName);
		// set gender based on 'genderPreference'
		String genderPref = StringUtil.checkVal(req.getParameter(formFieldMap.get("genderPreference"))).toLowerCase();
		if (genderPref.indexOf("male") > -1) { // works for both 'male' and 'female'
			req.setParameter("pfl_GENDER_CD", genderPref);
		}
		
		// set 'share info' value if it is not set
		if (StringUtil.checkVal(req.getParameter(formFieldMap.get("shareInfo"))).length() == 0) {
			req.setParameter(formFieldMap.get("shareInfo"), "No");
		}
	}
	
	/**
	 * Sends an email to the rep who submitted the field form to notify the rep of
	 * whom was assigned to the patient.
	 * @param req
	 */
	private void sendRepEmail(ActionRequest req, AssigneeVO assignee) {
		log.debug("sending ambassador assignment email notification to rep...");
		String repEmail = StringUtil.checkVal(req.getParameter("submittingRepEmailAddress"));
		log.debug("submittingRepEmailAddress param: " + req.getParameter("submittingRepEmailAddress"));
		if (repEmail.length() == 0) return;
		TrackerMailFormatter mf = new TrackerMailFormatter();
		mf.setAttributes(attributes);
		mf.setAmbassador(assignee);
		mf.setType(SJMTrackerConstants.EMAIL_TYPE_REP_FIELD_FORM_ASSIGN);
		mf.setRecipients(new String[] {repEmail});
		try {
			mf.sendEmail();
		} catch (MailException me) {
			log.error("Error sending email notification to SJM rep regarding ambassador assignment to patient, ", me);
		} 
	}
	
	/**
	 * Sends a copy of this field form submission to the specified SJM team member.
	 * @param req
	 * @param assignee
	 * @param patient
	 */
	private void sendSJMMemberEmail(ActionRequest req, AssigneeVO assignee, PatientVO patient) {
		log.debug("sending copy of field form submission to team member");
		TrackerDataContainer tdc = null;
		try {
			tdc = this.retrieveSourceFormData(req, patient);
		} catch (Exception ae) {
			log.error("Error retrieving patient contact submittal data for Field form email, ", ae);
		}
		
		if (tdc != null) {
			TrackerMailFormatter mf = new TrackerMailFormatter(tdc);
			mf.setAttributes(attributes);
			mf.setAmbassador(assignee);
			mf.setPatient(patient);
			mf.setType(SJMTrackerConstants.EMAIL_TYPE_FIELD_SJM_MEMBER);
			mf.setRecipients(new String[] {req.getParameter(FIELD_FORM_SJM_EMAIL_FIELD_ID)});
			try {
				mf.sendEmail();
			} catch (MailException me) {
				log.error("Error sending field form submission to SJM team member: " + req.getParameter(FIELD_FORM_SJM_EMAIL_FIELD_ID), me);
			}
		}
	}
	
	/**
	 * Retrieves the source contact form data for use in the team member email.
	 * @param req
	 * @param patient
	 * @return
	 */
	private TrackerDataContainer retrieveSourceFormData(ActionRequest req, PatientVO patient) 
		throws Exception {
		log.debug("retrieving source form data.");
		PatientContactDataRetriever pcd = new PatientContactDataRetriever();
		pcd.setDbConn(dbConn);
		pcd.setContactSubmittalId(patient.getPatientSourceFormId());
		TrackerDataContainer tdc = pcd.retrievePatientContactData();
		return tdc;
	}
}
