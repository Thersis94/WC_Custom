package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// SB ANS Medical libs
import com.ansmed.sb.physician.SurgeonVO;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SBII Libs
import com.smt.sitebuilder.action.contact.ContactAction;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: EpiducerRegistrationAction.java</p>
 <p>Pre/post-processing action that wraps the Contact Us portlet in order to process
 Epiducer registration submittals. The action sends email to designated recipients.</p>
 <p>Copyright: Copyright (c) 2011 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since May 12, 2011
 Code Updates
 Dave Bargerhuff, May 12, 2011 - Creating Initial Class File
 ***************************************************************************/
public class EpiducerRegistrationAction extends SimpleActionAdapter {
	
	private static final String FROM_SENDER = "EpiducerTraining@sjmneuro.com";
	//private static final String FROM_SENDER = "dave@siliconmtn.com";
	
	public static final String APPROVED = "approved";
	public static final String NOT_APPROVED = "notApproved";
	public static final String NOT_GROUP_APPROVED = "notGroupApproved";
	public static final String WAIT_LIST = "waitList";
	public static final String COURSE_FULL = "courseFull";
	public static final String ERROR = "error";
	private static final String COURSE_SELECTION_FIELD_ID = "c0a8023751111e8c8200f52c7920a8ab";
	private static final String LICENSE_FIELD_ID = "c0a802374c78c71f3040d1a3a22a122c";
	private static final String CUSTOM_SCHEMA = "WebCrescendo_custom.dbo.";
	private String courseRequested = null;
	private Map<String, List<String>> courseMap = new HashMap<String, List<String>>();
	private Map<String, EventEntryVO> courseNames = new HashMap<String, EventEntryVO>();
	private Map<String, Integer> maxAttendees = new HashMap<String, Integer>();
	private List<String> availableCourses = new ArrayList<String>();
	private Map<String, Integer> actionGroupMap = new HashMap<String, Integer>();
	private List<String> groupUrl = new ArrayList<String>();
	private SurgeonVO surgeon = null;
	
    public EpiducerRegistrationAction() {
        super();
        loadCollections();
    }

    public EpiducerRegistrationAction(ActionInitVO arg0) {
        super(arg0);
        loadCollections();
    }
     
    public void list(SMTServletRequest req) throws ActionException {
    	super.retrieve(req);    	
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
    	log.debug("Starting EpiducerRegistrationAction build...");
    	String courseFieldId = COURSE_SELECTION_FIELD_ID;
    	StringEncoder se = new StringEncoder();
    	courseRequested = se.decodeValue(StringUtil.checkVal(req.getParameter("con_" + courseFieldId)));
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String oldInitId = actionInit.getActionId();
    	String contactFormId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
    	log.debug("actionId | formId: " + oldInitId + " | " + contactFormId);
    	String responseType = null;
    	actionInit.setActionId(contactFormId);
    	
    	loadEvents(req);
    	surgeon = this.findSurgeon(req, se);
   		try {
   			// determine responseType
   			responseType = this.processResponseType(req, surgeon, oldInitId, contactFormId, courseRequested);
   		} catch (SQLException sqle) {
   			log.error("Error determining responseType, ", sqle);
   			responseType = ERROR;
   		}
    	// process the form based on responseType
    	processForm(req, surgeon, responseType);
    	// send email based on responseType
    	this.sendEmail(req, oldInitId, responseType, contactFormId);
    	// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	url = buildRedirect(req, oldInitId, responseType, courseRequested);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	actionInit.setActionId(oldInitId);
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Starting EpiducerRegistrationAction retrieve...");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String oldInitId = actionInit.getActionId();
    	String contactFormId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
    	actionInit.setActionId(contactFormId);
    	log.debug("actionId|contactFormId: " + oldInitId + "|" + contactFormId);
    	// load events
    	loadEvents(req);
    	loadMaxAttendees();
    	retrieveAvailableCourses(oldInitId, contactFormId);
		// Retrieve the Contact Data
    	SMTActionInterface eg = new ContactAction(this.actionInit);
    	eg.setAttributes(this.attributes);
    	eg.setDBConnection(dbConn);
    	eg.retrieve(req);
		// Store the retrieved data in ModuleVO.actionData, set available courses as attribute on moduleVO
    	mod.setAttribute("availableCourses", availableCourses);
    	//mod.setAttribute("courseList", courseList);
		mod.setActionData((ContactVO) req.getAttribute(ContactAction.RETRV_CONTACT));
		attributes.put(Constants.MODULE_DATA, mod);
		actionInit.setActionId(oldInitId);
    }
	
	/**
	 * 	Returns a response type based on whether or not a surgeon is approved for a course.
	 * @param req
	 * @return
	 */
	private SurgeonVO findSurgeon(SMTServletRequest req, StringEncoder se) {
		SurgeonVO surgeon = findSurgeonByLicense(req);
		if (surgeon == null) {
			surgeon = findSurgeonByName(req, se);
		}

		if (surgeon != null) {
			// if we found a surgeon, check the email address on the surgeon VO
			log.debug("email address on SurgeonVO: " + surgeon.getEmailAddress());
			if (StringUtil.checkVal(surgeon.getEmailAddress()).length() == 0) {
				surgeon.setEmailAddress(StringUtil.checkVal(req.getParameter("pfl_EMAIL_ADDRESS_TXT")));
				log.debug("email address NOW on surgeonVO: " + surgeon.getEmailAddress());
			}
		} else {
			// no surgeon found, let's populate a vo off of the request for emailing purposes
			log.debug("no surgeon found");
			surgeon = new SurgeonVO(req);
			surgeon.setFirstName(req.getParameter("pfl_FIRST_NAME"));
			surgeon.setLastName(req.getParameter("pfl_LAST_NAME"));
			surgeon.setEmailAddress(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
		}
		return surgeon;
	}
	
	/**
	 * Searches for surgeon based on submitted license number, returns SurgeonVO if found
	 * @param req
	 * @return
	 */
	private SurgeonVO findSurgeonByLicense(SMTServletRequest req) {
		// if no license number submitted, return
		log.debug("checking surgeon by license");
		SurgeonVO surgeon = null;
		String licenseFieldId = "con_" + LICENSE_FIELD_ID;
		String licenseFieldVal = StringUtil.checkVal(req.getParameter(licenseFieldId));
		if (licenseFieldVal.length() == 0) return surgeon;
		log.debug("found license number: " + licenseFieldVal);
		
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(CUSTOM_SCHEMA).append("ans_surgeon ");
		sql.append("where prod_approval_flg > 0 and prod_group_no > 0 ");
		sql.append("and medical_license_no = ? ");
		log.debug("checkSurgeonByLicense SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, licenseFieldVal);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				surgeon = new SurgeonVO(rs);
			}
		} catch (SQLException sqle) {
			log.error("Error performing surgeon lookup by license number, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		return surgeon;
	}
	
	/**
	 * Searches for surgeon based on submitted first/last name and state.
	 * Returns response type based on the results of the search.
	 * @param req
	 * @return
	 */
	private SurgeonVO findSurgeonByName(SMTServletRequest req, StringEncoder se) {
		log.debug("checking surgeon by name");
		SurgeonVO surgeon = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.city_nm, b.state_cd ");
		sql.append("from ").append(CUSTOM_SCHEMA).append("ans_surgeon a ");
		sql.append("inner join ").append(CUSTOM_SCHEMA).append("ans_clinic b ");
		sql.append("on a.surgeon_id = b.surgeon_id where b.location_type_id = 1 ");
		sql.append("and a.first_nm = ? and a.last_nm = ? ");
		sql.append("and b.state_cd = ? and a.prod_approval_flg > 0 ");
		log.debug("checkSurgeonByName SQL: " + sql.toString());
		log.debug("first/last name (decoded): " + se.decodeValue(req.getParameter("pfl_FIRST_NM")) + "|" + se.decodeValue(req.getParameter("pfl_LAST_NM")));
		log.debug("state: " + req.getParameter("pfl_STATE_CD"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, se.decodeValue(req.getParameter("pfl_FIRST_NM")));
			ps.setString(2, se.decodeValue(req.getParameter("pfl_LAST_NM")));
			ps.setString(3, req.getParameter("pfl_STATE_CD"));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				surgeon = new SurgeonVO(rs);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving surgeon by name, ", sqle);
		}
		return surgeon;
	}
	
	/**
	 * Determines appropriate response type to return based on the result set passed in.
	 * @param req
	 * @param rs
	 * @param courseRequested
	 * @return
	 * @throws SQLException
	 */
	private String processResponseType(SMTServletRequest req, SurgeonVO surgeon, 
			String oldInitId, String contactFormId, String courseRequested)	throws SQLException {
		String responseType = NOT_APPROVED;
		if (surgeon == null || surgeon.getProductApprovalFlag() < 1) return responseType;
		if (courseRequested.equalsIgnoreCase("Wait-List")) return WAIT_LIST;
		log.debug("surgeon group number: " + surgeon.getProductGroupNumber());
		log.debug("actionGroupMap number: " + actionGroupMap.get(oldInitId));
		if (!surgeon.getProductGroupNumber().equals(actionGroupMap.get(oldInitId))) 
			return NOT_GROUP_APPROVED;
		
		// if course is not 'other' check to see if requested course is full
		loadMaxAttendees();
		retrieveAvailableCourses(oldInitId, contactFormId);
		log.debug("courseRequested: ----->" + courseRequested + "<-----");
		if (availableCourses.contains(courseRequested)) {
			responseType = APPROVED;
		} else {
			responseType = COURSE_FULL;
		}
		return responseType;
	}
	
	/**
	 * Retrieves map of max attendees for each event associated with this actionId
	 * @param actionId
	 * @return
	 */
	private void loadMaxAttendees() {
		for (String s : courseNames.keySet()) {
			EventEntryVO e = courseNames.get(s);
			// EventEntryVO actionId is the event_entry_id
			maxAttendees.put(e.getActionId(), e.getMaxNumberUsers());
			log.debug("eventActionId | course name | max users: " + e.getActionId() + " | " + e.getEventName() + " | " + e.getMaxNumberUsers());
		}
		log.debug("maxAttendees size: " + maxAttendees.size());
	}
	
	/**
	 * Builds map of available courses for this actionId.  The map is used to build the 
	 * options in the select list for the 
	 * @param actionId
	 */
	private void retrieveAvailableCourses(String actionId, String formId) {
		StringEncoder se = new StringEncoder();
		// loop the action/event map
		int count = 0;
		StringBuffer fullCourseName = null;
		for (String s : courseMap.get(actionId)) {
			fullCourseName = new StringBuffer(courseNames.get(s).getEventName());
			// append the decoded course name so that the comparison is accurate
			fullCourseName.append(", ").append(Convert.formatDate(courseNames.get(s).getStartDate(), Convert.DATE_SLASH_SHORT_PATTERN));
			count = retrieveCourseSubmissions(formId, se.encodeValue(fullCourseName.toString()));
			log.debug("course | max | current count: " + s + " | " + maxAttendees.get(s) + " | " + count);
			if (count < maxAttendees.get(s)) {
				log.debug("adding " + fullCourseName.toString() + " to available courses");
				availableCourses.add(fullCourseName.toString());
				//courseList.put(s, fullCourseName.toString());
			} 
		}
	}
	
	/**
	 * Retrieves the count of submissions for the given actionId and field value
	 * @param actionId
	 * @param value
	 * @return
	 */
	private int retrieveCourseSubmissions(String formId, String value) {
		int count = 0;
		StringBuffer sql = new StringBuffer();
		sql.append("select count(*) from contact_submittal a ");
		sql.append("inner join contact_data b on a.contact_submittal_id = b.contact_submittal_id ");
		sql.append("where action_id = ? and contact_field_id = ? ");
		sql.append("and (cast(b.value_txt as nvarchar(max)) = ?) ");
		log.debug("retrieveCourseSubmissions SQL: " + sql.toString());
		log.debug("actionId|field|value: " + formId + "|" + COURSE_SELECTION_FIELD_ID + "|" + value);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, formId);
			ps.setString(2, COURSE_SELECTION_FIELD_ID);
			ps.setString(3, value);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving count for contact submittals, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		return count;
	}
	
	/**
	 * Processes submission
	 * @param req
	 * @throws ActionException
	 */
	private void processForm(SMTServletRequest req, SurgeonVO surgeon, String responseType) throws ActionException {
		// if surgeon object is not null and responseType is either 'approved' or 'data other' then we persist the form data
		if ( surgeon != null && (responseType.equalsIgnoreCase(APPROVED) || responseType.equalsIgnoreCase(WAIT_LIST)) ) {
			// set the surgeon's profileId on the request
			req.setParameter("pfl_PROFILE_ID", surgeon.getProfileId());
			log.debug("surgeon's profileId: " + req.getParameter("pfl_PROFILE_ID"));
	    	// Check custom "opt-in" field, set opt-in/privacy parameters.
	    	log.debug("Setting collection/consent parameters.");
	    	String collectionStatement = StringUtil.checkVal(req.getParameter("collectionStatement"));
	    	if (StringUtil.checkVal(collectionStatement).equals("1")) {
	    		req.setParameter("collectionStatement","1");
	    	}
	    	
			// Process the contact form information.  User's profile is NOT updated by 
	    	// the action.  This is on purpose.
	    	SMTActionInterface eg = new EpiducerSubmittalAction(this.actionInit);
	    	eg.setAttributes(this.attributes);
	    	eg.setDBConnection(dbConn);
	    	eg.build(req);
		} else {
			return;
		}
	}
	
	/**
	 * Helper method to determine under what circumstances to send an email and to whom
	 * @param req
	 * @param type
	 * @throws ActionException
	 */
	private void sendEmail(SMTServletRequest req, String actionId, String type, String formId) throws ActionException {
		log.debug("type is: " + type);
		if (type.equals(APPROVED) || 
				type.equals(NOT_APPROVED) || 
				type.equals(WAIT_LIST)) {
			sendEmail(req, actionId, type, formId, "sjm");
			sendEmail(req, actionId, type, formId, "physician");
		} else if (type.equals(ERROR)) {
			sendEmail(req, actionId, type, formId, "sjm");
		}
	}
	
	/**
    * Send a copy of the form submission to the designated recipient.
    * @param req
    */
	private void sendEmail(SMTServletRequest req, String actionId, String type, String formId, String target) throws ActionException {
		EpiducerMailFormatter emf = this.loadEmailFormatter(req, actionId, formId, type, target);
		
		if (emf == null) return;
		
		String[] rcpts = emf.getEmailTo().toArray(new String[0]);
		//String[] rcpts = {"dave@siliconmtn.com"};
		
    	log.debug("Starting notification email to " + target + " for responseType: " + type);
		SMTMail mail = null;
    	try {
    		// Create the mail object and send
    		mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
    		mail.setPort(new Integer((String)getAttribute(Constants.CFG_SMTP_PORT)));
    		mail.setRecpt(rcpts);
    		mail.setFrom(FROM_SENDER);
    		mail.setSubject(emf.getSubject());
    		mail.setHtmlBody(emf.getBody());
    		//mail.setTextBody(emf.getBody());
			log.debug("Mail body: " + mail.getHtmlBody());	    		
    		mail.postMail();
    	} catch (MailException me) {
    		log.error("Could not send Epiducer registration form email notification to " + target, me);
    	}
    }
	
	/**
	 * Initializes an EpiducerMailFormatter object
	 * @param req
	 * @param responseType
	 * @param target
	 * @return
	 */
	private EpiducerMailFormatter loadEmailFormatter(SMTServletRequest req, String actionId, String formId, String responseType, String target) {
		EpiducerMailFormatter emf = null;
		emf = new EpiducerMailFormatter(req, responseType);
		log.debug("responseType/target is: " + emf.getType() + "/" + target);
		emf.setDbConn(dbConn);
		emf.setFormId(formId);
		log.debug("formId is: " + emf.getFormId());
		try {
			if (target.equals("sjm")) {
				emf.setEvent(retrieveEventVO(actionId, courseRequested));
				emf.formatSjmEmail();
			} else if (target.equals("physician")) {
				log.debug("surgeon VO is: " + (surgeon != null ? "not null" : "null"));
				emf.setSurgeon(surgeon);
				emf.setEvent(retrieveEventVO(actionId, courseRequested));
				emf.formatPhysicianEmail();
			}
			log.debug("formatted email is: " + emf.getBody());
		} catch (SQLException sqle) {
			log.error("Error formatting email for " + target, sqle);
			emf = null;
		}
		return emf;
	}
	
	/**
	 * Formats the redirect URL based on the parameters passed in
	 * @param req
	 * @param responseType
	 * @param courseRequested
	 * @return
	 */
	private StringBuffer buildRedirect(SMTServletRequest req, String actionId, String responseType, String courseRequested) {
		StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?contactSubmitted=true");
    	url.append("&responseId=").append(req.getParameter("pmid"));
    	url.append("&responseType=").append(responseType);

    	if (responseType.equalsIgnoreCase(NOT_GROUP_APPROVED)) {
        	// add a 'group url' if physician accessed wrong reg url.
    		//String groupUrl = (surgeon.getSurgeonTypeId().equals("0") ? "sjm" : "comp") + surgeon.getProductGroupNumber(); 
    		url.append("&groupUrl=");
    		url.append(groupUrl.get((surgeon.getProductGroupNumber() - 1)));
    	} else if (responseType.equalsIgnoreCase("approved")) {
        	// add event location and date if approved
    		StringBuffer fullCourseName = null;
    		for (String s : courseMap.get(actionId)) {
    			fullCourseName = new StringBuffer(courseNames.get(s).getEventName());
    			fullCourseName.append(", ").append(Convert.formatDate(courseNames.get(s).getStartDate(), Convert.DATE_SLASH_SHORT_PATTERN));
    			if (fullCourseName.toString().equalsIgnoreCase(courseRequested)) {
    	    		url.append("&courseLoc=").append(courseNames.get(s).getLocationDesc());
    	    		url.append("&courseDate=").append(Convert.formatDate(courseNames.get(s).getStartDate(), Convert.DATE_SLASH_PATTERN));
    				break;
    			}
    		}
    	}
		return url;
	}
		
	/**
	 * Loads action ID / course map
	 */
	private void loadCollections() {		
		// load standard events
		// 'al' ids are event entry ids
		// 'courseMap' maps the wrapper portlet action id to the list of events (event entry ids) displayed on that portlet
		List<String> al = new ArrayList<String>();
		// SJM group 1 (FA)
		al.add("c0a802374bbffd7deee5ce70beafa4de");
		courseMap.put("c0a8023746786a17cadd78ab46c08bb",al);

		// SJM group 2 (FB)
		al = new ArrayList<String>();
		al.add("c0a802374bc272aebb5b0298b176101");
		al.add("c0a802374bc32345adaf859f8bf48c3b");
		//al.add("c0a802374bc3d9bbaad7511b517716a4"); - moved to Group 2 on 08/04/2011
		//al.add("c0a802374bc45e74d353f6f64c9462d9"); - moved to Group 2 on 08/04/2011
		al.add("c0a802374bc4bf8dd48cde116256b074");
		//al.add("c0a802374bc53e9d321c0061f43a9ba5"); - moved to Group 2 on 08/04/2011
		courseMap.put("c0a802374678e826a9f14e8243e2890a",al);

		// SJM group 3 (FC)
		al = new ArrayList<String>();
		al.add("c0a802374bc5bb7b608acd6cb5107802");
		courseMap.put("c0a80237467981ae57336ce2ae0327de",al);
		
		// SJM group 4 (FD)
		al = new ArrayList<String>();
		al.add("c0a802374bc3d9bbaad7511b517716a4"); // moved from Group 1B on 08/04/2011
		al.add("c0a802374bc45e74d353f6f64c9462d9"); // moved from Group 1B on 08/04/2011
		al.add("c0a802374bc53e9d321c0061f43a9ba5"); // moved from Group 1B on 08/04/2011
		al.add("c0a802374bc6614a50ffcc4dbeb8f707");
		al.add("c0a80241e7ad960a79995ce41c3c0df7");
		al.add("c0a80241e7ae031a6642987a6fa33049");
		al.add("c0a80241e7ae6c214744f15c16a63d2a");
		al.add("c0a80241e7af09aeb9d29483611476ff");
		al.add("c0a80241e7af83f5b80c076046595156");
		courseMap.put("c0a80241ebf590a36e86b540370a6308",al);
		
		// SJM group 5 (FE)
		al = new ArrayList<String>();
		al.add("c0a80241e7affd4fb0fb338ace2a99c");
		al.add("c0a80241e7b06f628c0242ccd8ced251");
		al.add("c0a80241e7b0e88fc2edd4efbcbe8484");
		al.add("c0a80241e7b15c2ffdc3ca9994e4caa0");
		courseMap.put("c0a80241ebf64b75d6169d6febad268b",al);
		
		// SJM group 6 (FF)
		al = new ArrayList<String>();
		al.add("c0a802378d00985471b2c15fbabad840");
		courseMap.put("c0a802378d4dc454886243c2db54df44",al);
		
		actionGroupMap.put("c0a8023746786a17cadd78ab46c08bb", 1);
		actionGroupMap.put("c0a802374678e826a9f14e8243e2890a", 2);
		actionGroupMap.put("c0a80237467981ae57336ce2ae0327de", 3);
		actionGroupMap.put("c0a80241ebf590a36e86b540370a6308", 4);
		actionGroupMap.put("c0a80241ebf64b75d6169d6febad268b", 5);
		actionGroupMap.put("c0a802378d4dc454886243c2db54df44", 6);
		
		groupUrl.add("fa");
		groupUrl.add("fb");
		groupUrl.add("fc");
		groupUrl.add("fd");
		groupUrl.add("fe");
		groupUrl.add("ff");
	}
	
	/**
	 * loads Epiducer events
	 * @param req
	 */
	private void loadEvents(SMTServletRequest req) {
		StringBuffer sql = new StringBuffer();
		sql.append("select * from event_entry where event_type_id = 'c0a802374bbe61db5cc63cd3ebd5f37f' ");
		sql.append("order by start_dt");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				courseNames.put(rs.getString("event_entry_id"), new EventEntryVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving event entries, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}		
	}
	
	private EventEntryVO retrieveEventVO(String actionId, String courseRequested) {
		log.debug("retrieving course requested: " + courseRequested);
		EventEntryVO evo = null;
		StringBuffer fullCourseName = null;
		for (String s : courseMap.get(actionId)) {
			fullCourseName = new StringBuffer(courseNames.get(s).getEventName());
			fullCourseName.append(", ").append(Convert.formatDate(courseNames.get(s).getStartDate(), Convert.DATE_SLASH_SHORT_PATTERN));
			if (fullCourseName.toString().equalsIgnoreCase(courseRequested)) {
				log.debug("found eventVO for: " + courseRequested);
				evo = courseNames.get(s);
				break;
			}
		}
		return evo;
	}

}
