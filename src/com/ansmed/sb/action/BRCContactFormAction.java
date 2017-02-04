package com.ansmed.sb.action;

// Java libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.contact.ContactAction;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.contact.SubmittalAction;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: BRCContactFormAction.java</p>
 <p>Post-processing action that wraps the Contact Us portlet in order to process
 BRC contact card information.  Action creates a user profile based on the 
 submitted form information.  The person submitting the user information is the 
 'proxy' user, that is, they are not the user who's profile is being created. 
 The action also sends email to designated recipients based on the fields 
 selected.</p>
 <p>Copyright: Copyright (c) 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Mar 05, 2009
 Code Updates
 Dave Bargerhuff, Mar 05, 2009 - Creating Initial Class File
 ***************************************************************************/

public class BRCContactFormAction extends SimpleActionAdapter {
	
    /**
     * 
     */
    public BRCContactFormAction() {
        super();
    }

    /**
     * @param arg0
     */
    public BRCContactFormAction(ActionInitVO arg0) {
        super(arg0);
    }
    
    public void list(ActionRequest req) throws ActionException {
    	super.retrieve(req);    	
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Starting BRC build...");
    	
    	String oldInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String newInitId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
    	log.debug("Setting actionId to : " + newInitId);
    	actionInit.setActionId(newInitId);
    	req.setParameter("actionGroupId", newInitId);
    	
    	// Check custom "opt-in" field, set opt-in/privacy parameters.
    	log.debug("Setting collection/consent parameters.");
    	String optIn = StringUtil.checkVal(req.getParameter("con_c0a8021eed165a0e2d0a52b2581ab092"));
    	//DEV String optIn = StringUtil.checkVal(req.getParameter("con_0a004602d82ccf618b5bf925caaa8d9"));
    	if (optIn.equalsIgnoreCase("yes")) {
    		req.setParameter("collectionStatement","1");
    		req.setParameter("orgConsentStatement","1");
    	}
    	
    	//Pull the logged in user's data off of the session for now.
    	log.debug("Removing proxy user's session data temporarily.");
    	UserDataVO proxyUser = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
    	req.getSession().removeAttribute(Constants.USER_DATA);

		// Process the contact form information to create profile based on the  
    	// submitted information.
    	ActionInterface eg = new SubmittalAction(this.actionInit);
    	eg.setAttributes(this.attributes);
    	eg.setDBConnection(dbConn);
    	eg.build(req);
    	
    	// Make sure the newly created user's data is not on the session before
    	// we put the proxy user's data back on the session.
    	req.getSession().removeAttribute(Constants.USER_DATA);
    	
    	//Put the proxy user's data back on the session
    	log.debug("Adding 'proxy' user's data back onto the session.");
    	req.getSession().setAttribute(Constants.USER_DATA, proxyUser);

    	// Generate email to the appropriate contacts.
    	log.debug("BRC Contact build complete... generating email.");
    	this.sendEmail(req);
    	// Setup the redirect.
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI()).append("?contactSubmitted=true");
    	url.append("&responseId=").append(req.getParameter("pmid"));
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	actionInit.setActionId(oldInitId);
    	
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String oldInitId = actionInit.getActionId();
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
    	
		// Retrieve the Contact Data
    	ActionInterface eg = new ContactAction(this.actionInit);
    	eg.setAttributes(this.attributes);
    	eg.setDBConnection(dbConn);
    	eg.retrieve(req);

		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		mod.setActionData((ContactVO) req.getAttribute(ContactAction.RETRV_CONTACT));
		attributes.put(Constants.MODULE_DATA, mod);
		actionInit.setActionId(oldInitId);
    }
	
	/**
    * Send a copy of the form submission to the designated recipient.
    * @param req
    */
	private void sendEmail(ActionRequest req) throws ActionException {
		
		StringEncoder se = new StringEncoder();
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	List<String> emailTo = new ArrayList<String>();
    	
    	// Process custom email 'send to' fields.
    	String nonUSMailTo = StringUtil.checkVal(req.getParameter("con_c0a8021eed0e1d86d104529c9e094d8c"));
    	String[] sendMeTo = (String[])req.getParameterValues("con_c0a8021eed1d03252e9bd8a873a902d");
    	String[] sendAlso = {"con_c0a802281463d10d4d6238022c275bb","con_c0a80228145b0fcb98549ded1c3739d"};
    	String[] sendSpecific = (String[])req.getParameterValues("con_c0a80228f3d7f17dc3d2b9afab021e9a");
    	
    	// Check for international submission.
    	if (nonUSMailTo.equalsIgnoreCase("yes")) {
    		emailTo.add("dana.lindsey@sjmneuro.com");
    	}
    	
    	if (sendMeTo != null && sendMeTo.length > 0) {
    		emailTo.add("delisa.cash@sjmneuro.com");
    	}
    	
    	// Process the additional 'send to' fields
    	for (int i = 0; i < sendAlso.length; i++) {
    		String addr = StringUtil.checkVal(req.getParameter(sendAlso[i]));
    		if (StringUtil.isValidEmail(addr)) {
    			emailTo.add(addr);
    		}
    	}
    	
    	// Process the "I am interested in...' field for email values.
    	if (sendSpecific != null && sendSpecific.length > 0) {
    		String val = "";
    		for (int i = 0; i < sendSpecific.length; i++) {
    			val = StringUtil.checkVal(sendSpecific[i]);
    			if (val.equalsIgnoreCase("share my story or photo") 
    					|| val.equalsIgnoreCase("success story video") 
    					|| val.equalsIgnoreCase("patient ambassador")) {
    				emailTo.add("Sherri.Drury@sjmneuro.com");
    			} else if (val.equalsIgnoreCase("poyp newsletter") 
    					|| val.equalsIgnoreCase("customer service rating")) {
    				emailTo.add("laura.crandell@sjmneuro.com");
    			}
    		}
    	}

	    if (!emailTo.isEmpty()) {	
	    	
	    	String[] rcpts = emailTo.toArray(new String[0]);
	    	
	    	log.debug("Starting notification email(s)...");
	    	site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	    	String subject = "BRC Contact Submission";
	    	String bodyHeader = "BRC Contact Submission";
	    	StringBuffer body = new StringBuffer();
	    	
			body.append("<p><font color=\"blue\"><b>").append(bodyHeader);
			body.append("</b></font></p>");
			body.append("<table style=\"width:750px;border:solid 1px black;\">");
			body.append("<tr><th colspan='2'>").append(req.getParameter("actionName"));
			body.append("</th></tr>");
			body.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\">Website");
			body.append("</td><td>");
			body.append(site.getSiteName()).append("</td></tr>");
			
	        // Add all of the fields that start with con_ to the message
			Map<String,String[]> data = req.getParameterMap();
			Map<String,String> fields = this.getFieldNames(actionInit.getActionId());
			Set<String> s = fields.keySet();
			int x=0;
			for (Iterator<String> iter = s.iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				String[] val = (String[]) data.get(key);
				String color="#E1EAFE";
				if ((x++ % 2) == 0) color="#c0d2ec";
				String questionNm = StringUtil.replace(fields.get(key),"#hide#","");
				body.append("<tr style=\"background:").append(color);
				body.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">").append(questionNm);
				body.append("</td><td>");
	    	   
				// Loop all elements for a parameter (multi-selects,check boxes).
				for (int i=0; val != null && i < val.length; i++) {
					body.append(se.decodeValue(val[i]));
					if ((i + 1) < val.length) body.append(", ");
				}
				body.append("&nbsp;</td></tr>");
	               
			}
			if (req.getParameter("collectionStatement") != null) {
				Integer cs = Convert.formatInteger(req.getParameter("collectionStatement"));
				body.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\">Collection Statement");
				body.append("</td><td>");
				body.append((cs == 1) ? "Yes":"No").append("</td></tr>");
			}
			if (req.getParameter("orgConsentStatement") != null) {
				Integer orgCs = Convert.formatInteger(req.getParameter("orgConsentStatement"));
				body.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\">Org Opt-In Consent");
				body.append("</td><td>");
				body.append((orgCs == 1) ? "Yes":"No").append("</td></tr>");
			}
			
			body.append("</table>");
			body.append("<br>");
	    	
			SMTMail mail = null;
	    	try {
	    		// Create the mail object and send
	    		mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
	    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
	    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
	    		mail.setPort(new Integer((String)getAttribute(Constants.CFG_SMTP_PORT)));
	    		mail.setRecpt(rcpts);
	    		mail.setFrom(site.getMainEmail());
	    		mail.setSubject(subject);
	    		mail.setHtmlBody(body.toString());
	    		
				// Add any attachments that were submitted
				List<FilePartDataBean> files = req.getFiles();
				for (int i=0; i < files.size(); i++) {
					FilePartDataBean file = files.get(i);
					mail.addAttachment(file.getFileName(), file.getFileData());
				}
				log.debug("Mail Info: " + mail.toString());	    		
	    		mail.postMail();
	    	} catch (MailException me) {
	    		log.error("Could not send BRC Contact form email notifications.", me);
	    	}
    	}
    }
	
	/**
	 * 
	 * @param contactId
	 * @return
	 */
	private Map<String, String> getFieldNames(String contactId) {
		StringBuffer sb = new StringBuffer();
		sb.append("select a.contact_field_id, field_nm, order_no, profile_column_nm ");
		sb.append("from contact_field a inner join contact_assoc b ");
		sb.append("on a.contact_field_id = b.contact_field_id and action_id = ? ");
		sb.append("order by order_no");
		log.debug("SQL: " + sb.toString() + "|" + contactId);
		
		PreparedStatement ps = null;
		Map<String, String> fields = new LinkedHashMap<String, String>();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, contactId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String key = "con_" + rs.getString(1);
				String pfl = rs.getString(4);
				String val = StringUtil.checkVal(rs.getString(2)).replace("#hide# ", "");
				if (pfl != null && pfl.length() > 0)
					key = "pfl_" + pfl;
				
				fields.put(key, val);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving Contact form fields for email: " + contactId, sqle);
		} finally {
		    if (ps != null) {
		        try {
		            ps.close();
		        } catch(Exception ex) {}
		    }
		}
		return fields;
	}
  
}
