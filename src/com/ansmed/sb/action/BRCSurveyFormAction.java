package com.ansmed.sb.action;

//SMT Base Libs 2.0
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.survey.SurveyAction;
import com.smt.sitebuilder.action.survey.SurveyDataAction;
import com.smt.sitebuilder.action.survey.SurveyDataContainer;
import com.smt.sitebuilder.action.survey.SurveyDataModuleVO;
import com.smt.sitebuilder.action.survey.SurveyResponseAction;
import com.smt.sitebuilder.action.survey.SurveyVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: BRCSurveyFormAction.java</p>
 <p>Post-processing action to send email to additional recipients based on 
 the fields selected.</p>
 <p>Copyright: Copyright (c) 2008 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Oct 27, 2008
 Code Updates
 Dave Bargerhuff, Oct 27, 2008 - Creating Initial Class File
 ***************************************************************************/

public class BRCSurveyFormAction extends SimpleActionAdapter {
	
    /**
     * 
     */
    public BRCSurveyFormAction() {
        super();
    }

    /**
     * @param arg0
     */
    public BRCSurveyFormAction(ActionInitVO arg0) {
        super(arg0);
    }
    
    public void list(SMTServletRequest req) throws ActionException {
    	super.retrieve(req);    	
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
    	log.debug("Starting build...");
    	
    	String oldInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
    	
    	Integer nextPgNo = Convert.formatInteger(req.getParameter("page"));

    	SMTActionInterface ee = new SurveyResponseAction(this.actionInit);
    	ee.setAttributes(this.attributes);
    	ee.setDBConnection(dbConn);
    	ee.build(req);
    	
		Map<String, String> surveys = (Map)req.getSession().getAttribute(SurveyResponseAction.SURVEY_CHART_DATA);
		if (surveys == null) surveys = new HashMap<String, String>();
		
		// Get the response text and add it to the session 
		//(This is displayed instead of the chart)
		SMTActionInterface sa = new SurveyAction(this.actionInit);
		sa.setAttributes(this.attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
		SurveyVO srvy = (SurveyVO)req.getAttribute(SurveyAction.RETRV_SURVEY);
    	
    	// Add the surveys to the session
		surveys.put("brcFormId", actionInit.getActionId());
		surveys.put(actionInit.getActionId(), srvy.getResponseText());
		req.getSession().setAttribute(SurveyResponseAction.SURVEY_CHART_DATA, surveys);
    	
    	// Generate email to the appropriate contacts.
    	log.debug("Build complete; generating email.");
    	this.sendEmail(req);

    	// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?surveySubmitted=true");
    	url.append("&page=").append(nextPgNo);
    	url.append("&transId=").append(req.getAttribute(SurveyResponseAction.TRANSACTION_ID));
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	actionInit.setActionId(oldInitId);
    		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		
		String flag = (String) req.getParameter("surveySubmitted");

    	// See if this survey has been completed
		log.debug("Checking for complete survey.");
		Map<String, String> surveys = (Map)req.getSession().getAttribute(SurveyResponseAction.SURVEY_CHART_DATA);
		
		if (surveys == null) {
			log.debug("surveys map is null...retrieving new survey.");
			surveys = new HashMap<String, String>();
		} else if (flag != null && flag.toLowerCase().equals("false")) {
			log.debug("Flag value for surveySubmitted is: " + flag + ", retrieving new survey.");
			surveys = new HashMap<String, String>();
		}
		
		if (surveys.get(actionInit.getActionId()) != null){
			log.debug("surveys.actionInit.getActionId() is NOT null...exiting the Action.");
			return;
		}
		
		// Retrieve the Survey Data
		log.debug("Starting retrieve...");
		
		// Since this is a new survey, set the "chart data" to the empty map.
		req.getSession().setAttribute(SurveyResponseAction.SURVEY_CHART_DATA, surveys);
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String oldInitId = actionInit.getActionId();
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		SMTActionInterface eg = new SurveyAction(this.actionInit);
    	eg.setAttributes(this.attributes);
    	eg.setDBConnection(dbConn);
    	eg.retrieve(req);
		log.debug("Retrieved survey data for actionID: " + this.actionInit.getActionId());
    	
		// Put the retrieved data in the ModuleVO.actionData and put into the Map
    	log.debug("Putting the data on the map.");
		mod.setActionData((SurveyVO) req.getAttribute(SurveyAction.RETRV_SURVEY));
		attributes.put(Constants.MODULE_DATA, mod);
		actionInit.setActionId(oldInitId);
	}
	
	/**
    * Send a copy of the form submission to the designated recipient.
    * @param req
    */
	private void sendEmail(SMTServletRequest req) throws ActionException {
    	
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	List<String> emailTo = new ArrayList<String>();
    	String adminTo = null;
    	
    	// Determine if we need to send admin email.
    	if (Convert.formatInteger(req.getParameter("notifyAdmin")) == 1) {
    		adminTo = StringUtil.checkVal(req.getParameter("emailAddressText"));
    		if (adminTo.length() == 0) {
    			emailTo.add(site.getAdminEmail());
    		} else {
    			emailTo.add(adminTo);
    		}
    	}

    	String[] mailTo = (String[])req.getParameterValues("SURVEY_c0a8021e7c6d5e16a12012d8b6678366");
    	String otherTo = (String)req.getParameter("SURVEY_c0a8021e8aac9829359a808fbf4ea0c");
    	
    	if (mailTo != null && mailTo.length > 0) {
    		for (int i = 0; i < mailTo.length; i++) {
        		emailTo.add(mailTo[i]);
    		}
    	}

    	if (otherTo != null && otherTo.length() > 0) {
    		if (StringUtil.isValidEmail(otherTo)) {
    			emailTo.add(otherTo);
    		}
    	}
    	
	    if (!emailTo.isEmpty()) {	
	    	
	    	String[] rcpts = emailTo.toArray(new String[0]);
	    	
	    	log.debug("Starting notify email");
	    	site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	    	String subject = "BRC Survey Submission";
	    	String bodyHeader = "BRC Survey Submission";
	    	StringBuffer body = new StringBuffer();
	    	
	    	//call surveyDataAction to retrieve the completed survey
	    	// this action runs against "transId", which was put on the request by SurveyResponseAction
	    	attributes.put(AdminConstants.ADMIN_MODULE_DATA, new ModuleVO());
	    	SMTActionInterface ai = new SurveyDataAction(this.actionInit);
	    	ai.setAttributes(this.attributes);
	    	ai.setDBConnection(this.dbConn);
	    	ai.update(req);
	    	ModuleVO mod = (ModuleVO) this.getAttribute(AdminConstants.ADMIN_MODULE_DATA);
	    	SurveyDataContainer sdc = (SurveyDataContainer) mod.getActionData();
	    	
	    	//build the message body using the survey data obj
	    	SurveyDataModuleVO core = sdc.getCoreData().get(0);
	    	int i = 0;
	
	    	body.append(bodyHeader);    	
	    	
	    	body.append("<br/><br/><style>\r");
	    	body.append("tr.row1 td { border-collapse:collapse; background-color: #E1EAFE; vertical-align: top; padding-left:10px; } \r");
	    	body.append("tr.row2 td { border-collapse:collapse; background-color: #c0d2ec; vertical-align: top; padding-left:10px; } \r");
	    	body.append("</style>\r ");
	    	body.append("<table style=\"width:750px; border:solid 1px black;\">\r ");
	    	body.append("<tr class='").append(style(i++)).append("'><td>Website</td><td>").append(site.getSiteName()).append("</td></tr>\r");
	    	body.append("<tr class='").append(style(i++)).append("'><td>Form Name</td><td>").append(core.getActionName()).append("</td></tr>\r");
	    	body.append("<tr class='").append(style(i++)).append("'><td>Date Submitted</td><td>").append(Convert.formatDate(core.getSubmittalDate(),Convert.DATE_TIME_SLASH_PATTERN)).append("</td></tr>\r");
	    	body.append("<tr class='").append(style(i++)).append("'><td>Transaction Id</td><td>").append(core.getTransactionId()).append("</td></tr>\r");
	
			List<String> fields = sdc.getFields();
			log.debug("fields=" + fields.toString());
			String[] data = sdc.getExtData().get(core.getTransactionId());
			
	    	if (fields.size() == data.length) {
	    		for (int x=0; x < fields.size(); x++) {
		    		try {
		    			body.append("<tr class='").append(style(i++)).append("'><td style=\"width: 40%;\">").append(fields.get(x)).append("</td>\r");
		    			body.append("<td style=\"width: 60%;\">").append(data[x]).append("</td></tr>\r");
		    		} catch (Exception e) {
		    			log.warn("Error adding BRC Survey form question to email notifications.", e);
		    			body.append("<tr class='row1'><td colspan='2'>...error loading this row of data...</td></tr>\r");
		    		}
		    	}
	    	} else {
	    		body.append("<tr class='row1'><td colspan='2'>...error loading survey data...</td></tr>\r");
	    	}
			body.append("</table><br/>\r\r");
			
	    	try {
	    		// Create the mail object and send
	    		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
	    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
	    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
	    		mail.setPort(new Integer((String)getAttribute(Constants.CFG_SMTP_PORT)));
	    		mail.setRecpt(rcpts);
	    		mail.setFrom(site.getMainEmail());
	    		mail.setSubject(subject);
	    		mail.setHtmlBody(body.toString());
	    		mail.postMail();
	    	} catch (MailException me) {
	    		log.error("Could not send BRC Survey form email notifications.", me);
	    	}
    	}

    }
  
    //helper for formating the notifyAdmin email
    private String style(int i) {
    	return (i%2 == 0) ? "row1" : "row2";
    }

}
