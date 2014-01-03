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

//SMT Base Libs 2.0
import com.ansmed.sb.locator.LocatorFacadeAction;
import com.ansmed.sb.locator.LocatorSearchAction;
import com.ansmed.sb.physician.SurgeonVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: SJMContactPostProcessorAction.java</p>
 <p>Post-processing action that wraps a given Contact Us portlet and performs
 email post-processing on that portlet.</p>
 <p>Copyright: Copyright (c) 2010 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Aug 23, 2010
 Code Updates
 Dave Bargerhuff, Aug 23, 2010 - Created initial class.
 Dave Bargerhuff, Dec 06, 2010 - Refactored to leverage post-process functionality
 in Contact Us portlet that James M. added.
 ***************************************************************************/

public class SJMContactPostProcessorAction extends SimpleActionAdapter {
	
	private final String SJM_FIELD_MANAGER_ROLE = "0a0015078af409d8fba3fe12a79f3487";
	
    /**
     * 
     */
    public SJMContactPostProcessorAction() {
        super();
    }

    /**
     * @param arg0
     */
    public SJMContactPostProcessorAction(ActionInitVO arg0) {
        super(arg0);
    }
    
    public void list(SMTServletRequest req) throws ActionException {
    	super.retrieve(req);    	
    }

    /**
     * Enum of SJM Contact Us action Id's that require post-processing.
     */
    private enum SJMFormId {
    	
    	// enum elements
    	c0a8022834e40d1b8e531bc0e370fc60("PowerOverYourPain Contact Us"),
    	c0a80237d934c65240bb374eb2791e57("PowerOverYourPain Contact Us"),
    	c0a80228178cb3c08c115684c9eafb39("PoderSobreSuDolor Contact Us"),
    	c0a80228d7b30e0f59cf59d3fffdfc22("PowerOverYourPain Information Kit Request"),
    	c0a802281bda5ce6b55e6589832dbcf8("PowerOverYourPain (Spanish) Information Kit Request"),
    	c0a80237cad8198bf3a74c321b39949("PoderSobreSuDolor Information Kit Request"),
    	c0a8024121202bfe526d209c678fdd5b("Patient Ambassador Request Form (Field)");
    	
    	// enum 'class' constructor
    	private SJMFormId(String name) {
    		this.formName = name;
    	}
   		
    	// enum 'instance' var
    	private String formName;
    							
    	//getter returns form name
    	private String getFormName() {
    		return formName;
    	}
    };
    
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
    	log.debug("Starting SJMContactPostProcessorAction build...");
    	String actionId = StringUtil.checkVal(req.getParameter("sbActionGroupId"));
    	if (actionId.length() == 0) actionId = actionInit.getActionId();
    	
    	// loop the enum to retrieve the appropriate form enum
		SJMFormId[] eObj = SJMFormId.values();
		SJMFormId form = null;
		for (SJMFormId temp : eObj) {
			if (actionId.equalsIgnoreCase(temp.name())) {
				form = temp;
				break;
			}
		}
		
		if (form != null) {
	    	// Perform post-processing of email
	    	log.debug("performing Contact Us post-processing for email sends...");
	    	this.postProcess(req, form);
			// override the upstream facade's email send.
			req.setParameter("contactEmailAddress", new String[0], Boolean.TRUE);
		}
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
	private void postProcess(SMTServletRequest req, SJMFormId form)
	throws ActionException {
		
		List<String> mailTo = new ArrayList<String>();
		
		// capture the 'default' administrative email address(es) in case we need it/them
		String[] defaultRcpts = req.getParameterValues("contactEmailAddress");
		log.debug("defaultRcpts size: " + defaultRcpts.length);
		
		// build the mailTo list
		switch(form) {
			// US-English language forms
			case c0a8022834e40d1b8e531bc0e370fc60: //Contact Us: POYP
			case c0a80237d934c65240bb374eb2791e57: //Contact Us: POYP2
			case c0a80228d7b30e0f59cf59d3fffdfc22: //Info Kit: POYP/POYP2
				boolean i18n = false;
				
				// if 'outside US' field was selected, email to specific person
				if (StringUtil.checkVal(req.getParameter("con_c0a80237fbf824eca619c81bfd7a208a")).length() > 0) {
					addRecipient(mailTo,new String[]{"laura.gasch@sjmneuro.com"});
					i18n = true;
				} else {
					//use default recipient
					addRecipient(mailTo, defaultRcpts);
				}
				
				// if not an international request, check for 'contact me'
				if (! i18n) {
					// if 'have a member of the SJM clinical team contact me' was selected, try to find rep
					if (StringUtil.checkVal(req.getParameter("con_c0a80237a7b866f7cd8b6d743bf907e5")).length() > 0) {					
						// Even if visitor did not check 'outside US', let's make sure that this is a US only request
						if (isUSRequest(StringUtil.checkVal(req.getParameter("con_c0a802283493393656c8fd4a909568a9")))) {
							UserDataVO rep = this.retrieveNearestFieldManager(req);
							if (rep != null && rep.getEmailAddress() !=  null) {
								//means we found a rep 
								addRecipient(mailTo, new String[]{rep.getEmailAddress()});
							} else {
								addRecipient(mailTo,new String[]{"LSterling@sjm.com"});
							}
						}
					}
				}
				break;
				
			// US-Spanish language form
			case c0a80228178cb3c08c115684c9eafb39: //Contact Us: PSSD
				if (StringUtil.checkVal(req.getParameter("con_c0a80237fc01dfb6991002f4572d2c24")).length() > 0) {
					addRecipient(mailTo,new String[]{"laura.gasch@sjmneuro.com"});
				} else {
					//use to default recipient
					addRecipient(mailTo, defaultRcpts);
				}
				break;
			
			// US-Spanish language form
			case c0a802281bda5ce6b55e6589832dbcf8: //Info Kit: POYP-ES
			case c0a80237cad8198bf3a74c321b39949: // Info Kit: PSSD
				if (StringUtil.checkVal(req.getParameter("con_c0a80237fc01dfb6991002f4572d2c24")).length() > 0) {
					addRecipient(mailTo,new String[]{"laura.gasch@sjmneuro.com"});
				} else {
					addRecipient(mailTo, new String[]{"mike.mercado@sjmneuro.com"});
				}
				break;
			case c0a8024121202bfe526d209c678fdd5b: // SalesNet Field Ambassador Request form
				// add the default recipient
				addRecipient(mailTo, defaultRcpts);
				// add the 'send copy to' recipient if applicable
				if (StringUtil.checkVal(req.getParameter("con_c0a802412128bdb4508e0afbc2e25fe")).length() > 0) {
					addRecipient(mailTo, new String[]{StringUtil.checkVal(req.getParameter("con_c0a802412128bdb4508e0afbc2e25fe"))});
				}
				break;
			default:
				addRecipient(mailTo, defaultRcpts);
				break;
		}
		// send email
		this.sendEmail(req, form, mailTo);
	}
	
	/**
	 * Adds recipient email address to the list of addresses if it doesn't
	 * already exist in the list.
	 * @param emailAddress
	 * @param recpt
	 */
	private void addRecipient(List<String> emailAddress, String[] recpts) {
		for (String recpt : recpts) {
			// support multiple comma-delimited email addresses
			if (recpt.indexOf(",") > -1) {
				String[] subRecpts = recpt.split(",");
				for (int i = 0; i < subRecpts.length; i++) {
					String emVal = subRecpts[i].trim();
					if (StringUtil.isValidEmail(emVal)) {
						if (! emailAddress.contains(emVal)) {
							emailAddress.add(emVal);
						}
					}
				}
			} else {
				if (! emailAddress.contains(recpt)) {
					emailAddress.add(recpt);
				}
			}
		}
	}
	
	/**
	 * Checks to see if this was a valid US-based request.  Returns boolean
	 * @param country
	 */
	private boolean isUSRequest(String country) {
		if (country != null && country.length() > 0) {
			// Remove leading/trailing spaces, spaces/periods between words/chars.
			country = country.toLowerCase().trim();
			country = StringUtil.replace(country, " ", "");
			country = StringUtil.replace(country, ".", "");
			if (country.length() == 0 || country.equals("us") || country.equals("usa") || 
				country.equals("unitedstates") || country.equals("unitedstatesofamerica") || 
				country.equals("losestadosunidos") || country.equals("estadosunidos") || 
				country.equals("eeuu")) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
	
	/**
	 * Send a copy of the form submission to the designated recipient(s).
	 * @param req
	 * @param contactForm
	 * @param recipients
	 * @throws ActionException
	 */
	private void sendEmail(SMTServletRequest req, SJMFormId contactForm, 
			List<String> recipients) throws ActionException {
		
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	
	    if (! recipients.isEmpty()) {
	    	String[] rcpts = recipients.toArray(new String[0]);

	    	log.debug("Starting administrative notification email send...");
	    	site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	    	String subject = "";
	    	if (contactForm == SJMFormId.c0a8024121202bfe526d209c678fdd5b) {
	    		subject = "Ambassador Request—Field";
	    	} else {
	    		subject = contactForm.getFormName() + " Submission";	
	    	}
	    	String bodyHeader = contactForm.getFormName() + " Submission";
	    	String body = this.buildEmailBody(req, site.getSiteName(), bodyHeader, contactForm);
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
	private String buildEmailBody(SMTServletRequest req, String siteName, String header, SJMFormId contactForm) {
		StringEncoder se = new StringEncoder();
    	StringBuffer body = new StringBuffer();
    	
    	if (contactForm == SJMFormId.c0a8024121202bfe526d209c678fdd5b) {
    		body.append("<p>We have received your request and will notify you when an ambassador has contacted the patient, ");
    		body.append("typically within 24-48 hours, unless otherwise indicated.</p><br/>");
    	}
    	
		body.append("<p><font color=\"blue\"><b>").append(header);
		body.append("</b></font></p>");
		body.append("<table style=\"width:750px;border:solid 1px black;\">");
		body.append("<tr><th colspan='2'>").append(req.getParameter("actionName"));
		body.append("</th></tr>");
		body.append("<tr style=\"background:#e1eafe;\"><td style=\"padding-right:10px;\">Website");
		body.append("</td><td>");
		body.append(siteName).append("</td></tr>");
		
        // Add all of the fields that start with con_ to the message
		Map<String,String[]> data = req.getParameterMap();
		log.debug("map size: " + data.size());
		Map<String,String> fields = this.getFieldNames(actionInit.getActionId());
		Set<String> s = fields.keySet();
		int x=0;
		for (Iterator<String> iter = s.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String[] val = (String[]) data.get(key);
			String color="#e1eafe";
			if ((x++ % 2) == 0) color="#c0d2ec";
			String questionNm = StringUtil.replace(fields.get(key),"#hide#","");
			body.append("<tr style=\"background:").append(color);
			body.append(";\"><td style=\"width: 300px; padding-right:10px;\" nowrap valign=\"top\">").append(questionNm);
			body.append("</td><td>");
    	   
			// Loop all elements for a parameter (multi-selects,check boxes).
			for (int i=0; val != null && i < val.length; i++) {
				// if phone field, format phone number
				if (questionNm.toLowerCase().contains("phone")) {
					body.append(this.formatPhoneNumber(se.decodeValue(val[i])));
				} else {
					body.append(se.decodeValue(val[i]));
				}
				if ((i + 1) < val.length) body.append(", ");
			}
			body.append("&nbsp;</td></tr>");
               
		}
		if (req.getParameter("collectionStatement") != null) {
			Integer cs = Convert.formatInteger(req.getParameter("collectionStatement"));
			body.append("<tr style=\"background:#e1eafe;\"><td style=\"padding-right:10px;\">Collection Statement");
			body.append("</td><td>");
			body.append((cs == 1) ? "Yes":"No").append("</td></tr>");
		}
		if (req.getParameter("orgConsentStatement") != null) {
			Integer orgCs = Convert.formatInteger(req.getParameter("orgConsentStatement"));
			body.append("<tr style=\"background:#e1eafe;\"><td style=\"padding-right:10px;\">Org Opt-In Consent");
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
	 * Retrieves the CFM user data for rep associated with surgeon nearest to visitor 
	 * who requested contact by rep.
	 * @param req
	 */
	private UserDataVO retrieveNearestFieldManager(SMTServletRequest req) {
		log.debug("retrieving nearest field manager");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		// retrieves nearest 'CFM' rep, not just any rep.
		UserDataVO rep = null;		
		String surgeonId = this.retrieveNearestSurgeon(req);
		if (surgeonId == null) return rep;
		
		String regionId = this.retrieveNearestRegion(surgeonId, customDb);
		if (regionId == null) return rep;
		
		String repProfileId = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select b.profile_id from ").append(customDb);
		sql.append("ans_sales_region a inner join ").append(customDb);
		sql.append("ans_sales_rep b on a.region_id = b.region_id ");
		sql.append("where a.region_id = ? and b.role_id = ? ");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, regionId);
			ps.setString(2, SJM_FIELD_MANAGER_ROLE);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				repProfileId = rs.getString(1);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving profile Id for field manager rep.", sqle);
			return rep;
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}		
		
		//get the UserDataVO for the rep
		if (repProfileId != null) {
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			try {
				List<String> repProfile = new ArrayList<String>();
				repProfile.add(repProfileId);
				List<UserDataVO> profile = pm.searchProfile(dbConn, repProfile);
				rep = profile.get(0);
			} catch (DatabaseException de) {
				log.error("Error retrieving rep's user data...",de);
			}
		}
		return rep;
	}
	
	/**
	 * Retrieves region associated with surgeon nearest to visitor
	 * @param surgeonId
	 * @return
	 */
	private String retrieveNearestRegion(String surgeonId, String customDb) {
		String regionId = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.region_id from ").append(customDb);
		sql.append("ans_sales_rep a inner join ").append(customDb);
		sql.append("ans_surgeon b on a.sales_rep_id = b.sales_rep_id ");
		sql.append("where surgeon_id = ? ");
		
		log.debug("sql: " + sql.toString());
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				regionId = rs.getString(1);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving region Id for sales rep.", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return regionId;
	}
	
	/**
	 * Leverages LocatorSearchAction to query for surgeons closest to visitor.
	 * Returns rep ID of rep associated with closest surgeon. 
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String retrieveNearestSurgeon(SMTServletRequest req) {
		
		// set values the locator will need to perform a search		
		this.setLocatorSearchParams(req);
				
		// retrieve the surgeon nearest to this person and get the rep for that surgeon
		try {
			SMTActionInterface sai = new LocatorSearchAction(this.actionInit);
			sai.setAttributes(this.attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		} catch (ActionException ae) {
			log.error("Error locating surgeons near this visitor.  Returning null rep ID.", ae);
			return null;
		}
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		List<SurgeonVO> surgeons = (List<SurgeonVO>) mod.getActionData();
		if (surgeons.size() > 0) {
			return surgeons.get(0).getSurgeonId();
		} else {
			log.debug("no surgeons found, no rep ID returned.");
			return null;
		}		
	}
	
	/**
	 * Sets the parameters on the request that the locator will use to 
	 * perform the search for surgeons
	 * @param req
	 */
	private void setLocatorSearchParams(SMTServletRequest req) {
		//set the 'address' params
		req.setParameter("address", req.getParameter("pfl_ADDRESS_TXT"));
		req.setParameter("city", req.getParameter("pfl_CITY_NM"));
		req.setParameter("state", req.getParameter("pfl_STATE_CD"));
		
		//set zip, lat/long if zip is valid
		String zip = StringUtil.checkVal(req.getParameter("pfl_ZIP_CD"));
		if (zip.length() > 4 && zip.length() < 11) {
			zip = zip.substring(0,5);
			req.setParameter("zipCode", req.getParameter("pfl_ZIP_CD"));
		
			// retrieve and set lat/long params
			StringBuffer sql = new StringBuffer();
			sql.append("select latitude_no, longitude_no from zip_code where zip_cd = ?");
			
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, zip);
				
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					req.setParameter(LocatorFacadeAction.ANS_USER_LATITUDE, rs.getString(1));
					req.setParameter(LocatorFacadeAction.ANS_USER_LONGITUDE, rs.getString(2));
				}
			} catch (SQLException sqle) {
				log.error("Error retrieving lat/long for zipcode: " + zip, sqle);
			}
		}
	}
	
	/**
	 * Returns a formatted phone number or else the original value.
	 * @param val
	 * @return
	 */
	private String formatPhoneNumber(String val) {
		// if length < 7 return original val
		if (val.length() < 7) return val;
		
		String tempVal = StringUtil.removeNonNumeric(val);
		// if length > 10 return original val, otherwise format
		if (tempVal.length() > 10) {
			log.debug("returning: " + val);
			return val;
		} else {
			log.debug("returning: " + (new PhoneNumberFormat(tempVal,3).getFormattedNumber()));
			return (new PhoneNumberFormat(tempVal,3).getFormattedNumber());
		}
	}
	
}
