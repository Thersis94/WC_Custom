package com.ansmed.sb.action.postprocess;

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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: PAPrContactPPAction.java</p>
 <p>Post-processing action that wraps a given Contact Us portlet and performs
 email post-processing on that portlet.</p>
 <p>Copyright: Copyright (c) 2010 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Jan 24, 2011
 Code Updates
 Dave Bargerhuff, Jan 24, 2011 - Created initial class.
 ***************************************************************************/

public class PAPrContactPPAction extends SimpleActionAdapter {
	
	
    /**
     * 
     */
    public PAPrContactPPAction() {
        super();
    }

    /**
     * @param arg0
     */
    public PAPrContactPPAction(ActionInitVO arg0) {
        super(arg0);
    }
    
    public void list(SMTServletRequest req) throws ActionException {
    	super.retrieve(req);    	
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
    	log.debug("Starting PAPrContactPPAction build...");
    	this.postProcess(req);
    	
    	// override the upstream facade's email send.
		req.setParameter("contactEmailAddress", new String[0], Boolean.TRUE);
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {}
	
	/**
	 * Processes specific Contact Us portlet form fields to determine who should 
	 * receive administrative email for the form.
	 * @param req
	 * @param formId
	 * @return
	 */
	private void postProcess(SMTServletRequest req)
	throws ActionException {
		
		List<String> mailTo = new ArrayList<String>();
		
		// capture the 'default' administrative email address(es) in case we need it/them
		String[] defaultRcpts = req.getParameterValues("contactEmailAddress");
		this.addRecipient(mailTo, defaultRcpts);
		
		// determine which forms were submitted
		String[] forms = req.getParameterValues("con_c0a80241a6719fb58f61d70c46c08bb");
		
		if (forms != null && forms.length > 0) {
			for (String s : forms) {
				if (s.toLowerCase().contains("ambassador")) {
					this.addRecipient(mailTo, new String[]{"michelle.revello@sjmneuro.com"});
				} else if (s.toLowerCase().contains("story") || s.toLowerCase().contains("information")) {
					this.addRecipient(mailTo, new String[]{"sherri.drury@sjmneuro.com"});
				}
			}
		}
		// send email
		this.sendEmail(req, mailTo);
		// set a temporary cookie so that we can retrieve submitter's name and use it in the response text.
		this.setNameCookie(req);	
	}
	
	/**
	 * Adds recipient email address to the list of addresses if it doesn't
	 * already exist in the list.
	 * @param emailAddress
	 * @param recpt
	 */
	private void addRecipient(List<String> emailAddress, String[] recpts) {
		if (recpts == null || recpts.length == 0) return;
		for (String recpt : recpts) {
			if (! emailAddress.contains(recpt)) {
				emailAddress.add(recpt);
			}
		}
	}
	
	/**
	 * Send a copy of the form submission to the designated recipient(s).
	 * @param req
	 * @param contactForm
	 * @param recipients
	 * @throws ActionException
	 */
	private void sendEmail(SMTServletRequest req, List<String> recipients) throws ActionException {
		
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	
	    if (! recipients.isEmpty()) {
	    	String[] rcpts = recipients.toArray(new String[0]);

	    	log.debug("Starting administrative notification email send...");
	    	site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	    	String subject = "'I Would Like to Share My Story' Form Submission";
	    	String bodyHeader = "'I Would Like to Share My Story' Form Submission";
	    	
	    	String body = this.buildEmailBody(req, site.getSiteName(), bodyHeader);
	    	
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
	    		mail.setHtmlBody(body);
	    		
				// Add any attachments that were submitted
				List<FilePartDataBean> files = req.getFiles();
				for (int i=0; i < files.size(); i++) {
					FilePartDataBean file = files.get(i);
					mail.addAttachment(file.getFileName(), file.getFileData());
				}
				log.debug("Mail Info: " + mail.toString());	    		
	    		mail.postMail();
	    	} catch (MailException me) {
	    		log.error("Error sending SJM email notification for ", me);
	    	}
    	}
    }
	
	/**
	 * Builds administrative email body
	 * @param req
	 * @param siteName
	 * @param header
	 * @return
	 */
	private String buildEmailBody(SMTServletRequest req, String siteName, String header) {
		StringEncoder se = new StringEncoder();
    	StringBuffer body = new StringBuffer();
    	
		body.append("<p><font color=\"blue\"><b>").append(header);
		body.append("</b></font></p>");
		body.append("<table style=\"width:750px;border:solid 1px black;\">");
		body.append("<tr style=\"background:#c0d2ec;\"><th colspan='2'>").append(req.getParameter("actionName"));
		body.append("</th></tr>");
		body.append("<tr style=\"background:#c0d2ec;\"><td style=\"width: 250px; padding-right:10px;\">Website");
		body.append("</td><td>").append(siteName).append("</td></tr>");
		
        // Add all of the fields that start with con_ to the message
		Map<String,String[]> data = req.getParameterMap();
		log.debug("map size: " + data.size());
		Map<String,String> fields = this.getFieldNames(actionInit.getActionId());
		Set<String> s = fields.keySet();
		int x=0;
		for (Iterator<String> iter = s.iterator(); iter.hasNext();) {
			x++;
			String key = (String) iter.next();
			String[] val = (String[]) data.get(key);
			String questionNm = StringUtil.replace(fields.get(key),"#hide#","");
			body.append("<tr style=\"background:").append(setRowColor(x, (x % 2 == 0)));
			body.append(";\"><td style=\"width: 250px; padding-right:10px;\" valign=\"top\">").append(questionNm);
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
			body.append("<tr style=\"background:#c0d2ec;\"><td style=\"width: 250px; padding-right:10px;\">Collection Statement");
			body.append("</td><td>");
			body.append((cs == 1) ? "Yes":"No").append("</td></tr>");
		}
		if (req.getParameter("orgConsentStatement") != null) {
			Integer orgCs = Convert.formatInteger(req.getParameter("orgConsentStatement"));
			body.append("<tr style=\"background:#c0d2ec;\"><td style=\"width: 250px; padding-right:10px;\">Org Opt-In Consent");
			body.append("</td><td>");
			body.append((orgCs == 1) ? "Yes":"No").append("</td></tr>");
		}
		body.append("</table>");
		body.append("<br>");
		return body.toString();
	}
	
	/**
	 * Retrieves fields for given contact Id
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
	
	/**
	 * Sets a temporary cookie to hold submitter's name for use in JSTL
	 * @param req
	 */
	private void setNameCookie(SMTServletRequest req) {
        try {
			// Create/set temporary cookie with lifespan of 120 seconds.
			Cookie cookie = new Cookie("SJM_TEMP_VISITOR", StringUtil.checkVal(req.getParameter("pfl_FIRST_NM")));
	        HttpServletResponse res = (HttpServletResponse) this.getAttribute(GlobalConfig.HTTP_RESPONSE);
	        cookie.setMaxAge(120);
			res.addCookie(cookie);
			log.debug("Cookie Set: name/value: " + cookie.getName() + " / " + cookie.getValue());
		} catch (Exception e) {
			log.debug("Unable to set user cookie", e);
		}
    }
	
	/**
	 * Returns color style value for row being rendered.
	 * @param x
	 * @param alt
	 * @return
	 */
	private String setRowColor(int x, boolean alt) {
		String color = "";
		if (x > 22) { // share story section
			if (alt) color = "#c0d7c8";
			else color = "#c0d7c8";
		} else if (x > 14) { // ambassador section
			if (alt) color = "#e7e2bf";
			else color = "#cfcfb1";
		} else { // main form info
			//if (alt) color = "#c0d2ec";
			//else color = "#e1eafe";
			if (alt) color = "#ced2e5";
			else color = "#e6e3f2";
		}
		return color;
	}
}
