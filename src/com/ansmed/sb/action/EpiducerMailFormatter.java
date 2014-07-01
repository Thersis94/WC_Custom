package com.ansmed.sb.action;

// JDK 1.6
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// SB ANS Medical libs
import com.ansmed.sb.physician.SurgeonVO;

// SMT Baselibs 2.0
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SitebuilderII libs
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EpiducerMailFormatter.java<p/>
 * <b>Description: </b><p>Formats the appropriate email response to either in-house SJM and/or 
 * the physician who is attempting to register for training.<p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since June 09, 2011
 ****************************************************************************/
public class EpiducerMailFormatter {
	
	private static final String FROM_SENDER = "EpiducerTraining@sjmneuro.com";
	private static final String CC_NAME = "john.astorga@sjmneuro.com";
	//private static final String FROM_SENDER = "dave@siliconmtn.com";
	//private static final String CC_NAME = "dave@siliconmtn.com";
	
	private SMTServletRequest req = null;
	private String type = null;
	private String subject = null;
	private String body = null;
	private List<String> emailTo = null;
	private Map<String, String> fields = null;
	private String formId = null;
	private StringBuffer responseText = null;
	private Connection dbConn = null;
	private EventEntryVO event = null;
	private SurgeonVO surgeon = null;
	private List<SurgeonVO> surgeons = null;
	private UserDataVO rep = null;
	
	public EpiducerMailFormatter() {
		fields = new HashMap<String, String>();
		emailTo = new ArrayList<String>();
	}
	
	public EpiducerMailFormatter(SMTServletRequest req, String type) {
		this();
		this.req = req;
		this.type = type;
	}
	
	public void formatSjmEmail() throws SQLException {
    	//System.out.println("type is: " + type);
    	if (type.equals(EpiducerRegistrationAction.APPROVED)) {
    		emailTo.add(FROM_SENDER);
    		emailTo.add(CC_NAME);
    		subject = "Epiducer training registration - Approved";
    	} else if (type.equals(EpiducerRegistrationAction.NOT_APPROVED)) {
    		emailTo.add(FROM_SENDER);
    		emailTo.add(CC_NAME);
    		subject = "Epiducer training registration - Target list";
    	} else if (type.equals(EpiducerRegistrationAction.WAIT_LIST)) {
    		emailTo.add(FROM_SENDER);
    		emailTo.add(CC_NAME);
    		subject = "Epiducer training registration - Wait List";
    	} else if (type.equalsIgnoreCase(EpiducerRegistrationAction.ERROR)) {
    		emailTo.add("dave@siliconmtn.com");
    		subject = "ERROR during Epiducer Registration submission";
    	} 

    	try {
    		body = this.formatSjmEmailBody(req);
    	} catch (SQLException sqle) {
    		throw new SQLException(sqle.getMessage());
    	}
		
		return;
	}
	
	public void formatPhysicianEmail() throws SQLException {
		if (surgeon != null) {
			if (StringUtil.checkVal(surgeon.getEmailAddress()).length() == 0) {
				surgeon.setEmailAddress(StringUtil.checkVal(req.getParameter("pfl_EMAIL_ADDRESS_TXT")));
				// surgeon.setEmailAddress(FROM_SENDER);
			}
		} else {
			return;
		}
   		emailTo.add(surgeon.getEmailAddress());
    	body = this.formatPhysicianEmailBody();
		return;
	}
	
	/**
	 * Builds the email body to be sent to SJM
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	private String formatSjmEmailBody(SMTServletRequest req)  throws SQLException {
		StringEncoder se = new StringEncoder();
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuffer body = new StringBuffer();
		if (type.equals(EpiducerRegistrationAction.APPROVED)	|| 
			type.equals(EpiducerRegistrationAction.NOT_APPROVED)  || 
			type.equals(EpiducerRegistrationAction.WAIT_LIST)) {
			
	    	body.append("<p><font color=\"blue\"><b>");
			body.append("</b></font></p>");
			body.append("<table style=\"width:750px;border:solid 1px black;\">");
			body.append("<tr><th colspan='2'>").append(req.getParameter("actionName"));
			body.append("</th></tr>");
			body.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\">Website");
			body.append("</td><td>");
			body.append(site.getSiteName()).append("</td></tr>");
			
	        // Add all of the fields that start with con_ to the message
			Map<String,String[]> data = req.getParameterMap();
			try {
				fields = this.getFieldNames(formId);
			} catch (SQLException sqle) {
				throw new SQLException(sqle.getMessage());
			}
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
		}
		//System.out.println("sjm body is: " + body.toString());
		return body.toString();
	}
	
	/**
	 * Builds the email body to be sent to SJM
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	private String formatPhysicianEmailBody() {
		StringBuffer letter = new StringBuffer(this.getWrapperBodyTop());
		StringBuffer body = new StringBuffer();
		if (type.equals(EpiducerRegistrationAction.APPROVED)) {
    		StringBuffer sub = new StringBuffer("Epiducer System Training | ");
    		sub.append(Convert.formatDate(event.getStartDate(), Convert.DATE_SLASH_PATTERN)).append(" | ");
    		sub.append(surgeon.getLastName()).append(" | ");
    		sub.append("Training Confirmation");
			subject = sub.toString();
			body.append(this.getApprovedBody());
			body.append(this.getCommittment());
			
		} else if (type.equals(EpiducerRegistrationAction.NOT_APPROVED)) {
			subject = "Epiducer System Training | St. Jude Medical Neuromodulation";
			body.append(this.getNotApprovedBody());
			body.append(this.getCommittment());
			
		} else if (type.equals(EpiducerRegistrationAction.NOT_GROUP_APPROVED)) {
			body.append("<p>It appears that you have accessed an incorrect URL for Epiducer training registration for your pre-approved group status.<br/>");
			body.append("Please go <a href=\"http://").append(this.getGroupUrl(surgeon)).append(">here</a> to register for Epiducer training.</p>");
			
		} else if (type.equals(EpiducerRegistrationAction.WAIT_LIST)) {
			subject = "Epiducer System Training | St. Jude Medical Neuromodulation";
			body.append(this.getOtherBody());
			body.append(this.getCommittment());
			
		} else if (type.equals(EpiducerRegistrationAction.COURSE_FULL)) {
			body.append("<p>We're sorry.  The course you have requested is full.  Please return to the <a href=\"http://");
			body.append(this.getGroupUrl(surgeon)).append(">Epiducer registration page</a> and select a different course.</p>");
			
		}
		//System.out.println("phys body is: " + body.toString());
		letter.append(body);
		letter.append(this.getWrapperBodyBottom(surgeon.getEmailAddress()));
		return letter.toString();
	}
	
	/**
	 * 
	 * @param contactId
	 * @return
	 */
	private Map<String, String> getFieldNames(String contactId) throws SQLException {
		StringBuffer sb = new StringBuffer();
		sb.append("select a.contact_field_id, field_nm, order_no, profile_column_nm ");
		sb.append("from contact_field a inner join contact_assoc b ");
		sb.append("on a.contact_field_id = b.contact_field_id and action_id = ? ");
		sb.append("order by order_no");
		//log.debug("SQL: " + sb.toString() + "|" + contactId);
		
		PreparedStatement ps = null;
		Map<String, String> fields = new LinkedHashMap<String, String>();
		ps = dbConn.prepareStatement(sb.toString());
		ps.setString(1, contactId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			String key = "con_" + rs.getString(1);
			String pfl = rs.getString(4);
			String val = StringUtil.checkVal(rs.getString(2)).replace("#hide# ", "");
			if (pfl != null && pfl.length() > 0) key = "pfl_" + pfl;
			fields.put(key, val);
		}
        try {
            ps.close();
        } catch(Exception e) {}

		return fields;
	}
	
	/**
	 * Returns Epiducer training url based on physician's surgeon type and group
	 * @param surgeon
	 * @return
	 */
	private String getGroupUrl(SurgeonVO surgeon) {
		String url = "#";
		switch(surgeon.getProductGroupNumber()) {
			case 1:
				url = "www.epiducer.com/fa";
				break;
			case 2:
				url = "www.epiducer.com/fb";
				break;
			case 3:
				url = "www.epiducer.com/fc";
				break;
			case 4:
				url = "www.epiducer.com/fd";
				break;
			case 5:
				url = "www.epiducer.com/fe";
				break;
			case 6:
				url = "www.epiducer.com/ff";
				break;
		}
		return url;
	}

	public void setReq(SMTServletRequest req) {
		this.req = req;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<String> getEmailTo() {
		return emailTo;
	}

	public void setEmailTo(List<String> emailTo) {
		this.emailTo = emailTo;
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}

	public String getFormId() {
		return formId;
	}

	public void setFormId(String formId) {
		this.formId = formId;
	}

	public StringBuffer getResponseText() {
		return responseText;
	}
	
	public String getPhysResponseText() {
		StringBuffer text = new StringBuffer();
		text.append("Thank you for interest in attending the <i>Epiducer <sup>TM</sup> Lead Delivery System</i> ");
		text.append("hands-on product training workshop on DATE in LOCATION. We are in receipt of your registration ");
		text.append("and will be in contact with you within five business days to confirm your attendance. If you have ");
		text.append("any questions regarding this training course or if you would like to speak to one of St. Jude Medical ");
		text.append("Professional Education team members, please call 877-756-6441 or email us at ");
		text.append("<a href=\"mailto:professional.education@Epiducertraining.com\">professional.education@Epiducertraining.com</a>.");
		return text.toString();
	}
	public void setResponseText(StringBuffer responseText) {
		this.responseText = responseText;
	}

	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	public void setEvent(EventEntryVO event) {
		this.event = event;
	}

	public void setSurgeon(SurgeonVO surgeon) {
		this.surgeon = surgeon;
	}

	public void setSurgeons(List<SurgeonVO> surgeons) {
		this.surgeons = surgeons;
	}

	public void setRep(UserDataVO rep) {
		this.rep = rep;
	}

	public StringBuffer getApprovedBody() {
		StringBuffer letter = new StringBuffer();
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Thank you for accepting our invitation to participate in the ");
		letter.append("<strong>Epiducer lead delivery system</strong> hands-on product training workshop on ");
		letter.append("<strong>").append(Convert.formatDate(event.getStartDate(), Convert.DATE_SLASH_PATTERN)).append("</strong> in ");
		letter.append("<strong>").append(event.getLocationDesc()).append("</strong>. ");
		letter.append("Plaza Travel will contact you within two business days to arrange your travel. If you have any questions regarding this ");
		letter.append("training course, please contact Stacey Vess, St. Jude Medical Neuromodulation Division Professional Education Manager, at 972-309-8125.</p>");
		return letter;
	}
	
	public StringBuffer getNotApprovedBody() {
		StringBuffer letter = new StringBuffer();
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Thank you for your interest in attending the <strong>Epiducer lead delivery system</strong> ");
		letter.append("hands-on product training workshop. We have received your registration and will contact you ");
		letter.append("within five business days to confirm your attendance. If you have any questions regarding this ");
		letter.append("training course or if you would like to speak with a St. Jude Medical Professional Education team ");
		letter.append("member, please call 877-756-6441 or email us at ");
		letter.append("<a href=\"mailto:professional.education@epiducertraining.com\">professional.education@epiducertraining.com</a>.</p>");
		return letter;
	}
	
	public StringBuffer getOtherBody() {
		StringBuffer other = new StringBuffer();
		other.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		other.append("We appreciate your interest in attending a future <strong>Epiducer lead delivery system</strong> ");
		other.append("hands-on product training workshop. A member of the St. Jude Medical Professional Education ");
		other.append("team will contact you as soon as new dates and locations become available. If you have any ");
		other.append("questions regarding this training course or if you would like to speak with a St. Jude Medical ");
		other.append("Professional Education team member, please call 877-756-6441 or email us at ");
		other.append("<a href=\"mailto:professional.education@epiducertraining.com\">professional.education@epiducertraining.com</a>.</p>");
		return other;
	}
	
	public StringBuffer getWrapperBodyTop() {
		StringBuffer wrapper = new StringBuffer();
		wrapper.append("<div align=\"center\">");
		wrapper.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\">");
		wrapper.append("<tbody>");
		wrapper.append("<tr><td colspan=\"3\"><img height=\"183\" src=\"http://www.epiducer.com/binary/org/ANS-MEDICAL/images/epiducer-email-header.png\" width=\"600\" /></td></tr>");
		wrapper.append("<tr style=\"height:3px\"><td style=\"height:3px\" width=\"10\">&nbsp;</td><td style=\"height:3px\" width=\"580\">&nbsp;</td><td style=\"height:3px\" width=\"10\">&nbsp;</td></tr>");
		wrapper.append("<tr><td width=\"10\">&nbsp;</td>");
		wrapper.append("<td align=\"left\" width=\"580\">");
		return wrapper;
	}
	
	public StringBuffer getWrapperBodyBottom(String optOutEmail) {
		StringBuffer wrapper = new StringBuffer();
		wrapper.append("</td><td width=\"10\">&nbsp;</td></tr>");
		wrapper.append("<tr><td style=\"height:10px\" width=\"10\">&nbsp;</td><td style=\"height:10px\" width=\"580\">&nbsp;</td><td style=\"height:10px\" width=\"10\">&nbsp;</td></tr></tbody></table>");
		wrapper.append("</div>");
		wrapper.append("<p class=\"footer\">You have received this email communication on behalf of St. Jude Medical, which is solely responsible for its content.  ");
		wrapper.append("6901 Preston Rd Plano TX 75024</p><p class=\"footer\">If you are no longer interested in receiving these communications, please ");
		wrapper.append("<a href=\"http://www.epiducer.com/sb/emailPermissions?emailAddress=");
		wrapper.append(optOutEmail);
		wrapper.append("&organizationId=ANS-MEDICAL\">click here to opt out</a>.</p>");
		return wrapper;
	}
	
	public StringBuffer getWrapperBodyBottomWithoutOptOut(String optOutEmail) {
		StringBuffer wrapper = new StringBuffer();
		wrapper.append("</td><td width=\"10\">&nbsp;</td></tr>");
		wrapper.append("<tr><td style=\"height:10px\" width=\"10\">&nbsp;</td><td style=\"height:10px\" width=\"580\">&nbsp;</td><td style=\"height:10px\" width=\"10\">&nbsp;</td></tr></tbody></table>");
		wrapper.append("</div>");
		return wrapper;
	}
	
	public StringBuffer getCommittment() {
		StringBuffer com = new StringBuffer();
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Our Commitment to Excellence and Integrity</strong><br />");
		com.append("St. Jude Medical has adopted the AdvaMed Code and adheres to all laws, rules, and regulations regarding appropriate interactions with healthcare professionals. ");
		com.append("As part of this commitment to ethical behavior, we provide the following information:</p>");
		
		com.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<li>As a registered participant, you must attend the entire course. If you fail to do so, you will be required to pay or reimburse us for your hotel and travel expenditures.</li>");
		com.append("<li>St. Jude Medical cannot extend business courtesies to persons who do not have a legitimate business reason to attend the event, including spouses and significant others.</li>");
		com.append("<li>If you change your approved travel itinerary, you may incur the costs associated with rebooking.</li>");
		com.append("<li>St. Jude Medical may only provide you meals associated with this educational meeting. You are responsible for any meals or snacks that St. Jude Medical has not coordinated.</li>");
		com.append("<li>For participants, St. Jude Medical will arrange pre-paid ground transportation from the airport to the hotel, from the hotel to the training facility, and from the training facility to the airport. You are responsible for any additional ground transportation costs.</li>");
		com.append("<li>St. Jude Medical will not reimburse you for baggage fees, extended stay costs, personal expenses, gifts, or entertainment costs.</li>");
		com.append("</ul>");
		
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Notice for Customers, Potential Customers, and Respective Associates and Agents Licensed to Practice Medicine in the Commonwealth of Massachusetts</strong><br />");
		com.append("St. Jude Medical agrees to pay for reasonable expenses (including travel, meals, and lodging) associated with the product demonstration and training you will ");
		com.append("receive at this Epiducer system training workshop and lab. Please be aware that Massachusetts law requires St. Jude Medical to publicly disclose the amount of such ");
		com.append("payments and the identity of the recipients annually.</p>");
		
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("By attending this training event, you acknowledge that you or a facility with which you are associated either has an existing agreement to purchase product with ");
		com.append("St. Jude Medical or is considering entering into an agreement to purchase product after evaluating the appropriate use and functionality of the product. ");
		com.append("Massachusetts law requires this in order for our company to pay the training costs of healthcare practitioners licensed in Massachusetts.</p>");
		
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Notice for Healthcare Professionals Licensed to Practice Medicine in the State of Vermont</strong><br />");
		com.append("St. Jude Medical agrees to pay for reasonable expenses (including travel, meals, and lodging) associated with the product training you will receive at this ");
		com.append("Epiducer system training workshop and lab. Vermont law requires St. Jude Medical to publicly disclose the amount of such payments and the identity of the ");
		com.append("recipients annually.</p>");
		com.append("<br />");
		return com;
	}
	
	public StringBuffer getSocietyCommittment() {
		StringBuffer com = new StringBuffer();
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Our Commitment to Excellence and Integrity</strong><br />");
		com.append("St. Jude Medical has adopted the AdvaMed Code and adheres to all laws, rules, and regulations regarding appropriate interactions with healthcare professionals. ");
		com.append("As part of this commitment to ethical behavior, we provide the following information:</p>");
		com.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<li>As a registered participant, you must attend the entire course. If you fail to do so, you will be required to pay or reimburse us for your hotel.</li>");
		com.append("<li>St. Jude Medical cannot extend business courtesies to persons who do not have a legitimate business reason to attend the event, including spouses and significant others.</li>");
		com.append("<li>St. Jude Medical may only provide you meals associated with this educational meeting. You are responsible for any meals or snacks that St. Jude Medical has not coordinated.</li>");
		com.append("<li>For participants, St. Jude Medical will arrange pre-paid ground transportation between the hotel and the training facility. You are responsible for any additional ground transportation costs.</li>");
		com.append("<li>St. Jude Medical will not reimburse you for baggage fees, extended stay costs, personal expenses, gifts, or entertainment costs.</li>");
		com.append("</ul>");
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Notice for Customers, Potential Customers, and Respective Associates and Agents Licensed to Practice Medicine in the Commonwealth of Massachusetts</strong><br />");
		com.append("St. Jude Medical agrees to pay for reasonable expenses (including meals, and lodging) associated with the product demonstration and training you will ");
		com.append("receive at this Epiducer system training workshop and lab. Please be aware that Massachusetts law requires St. Jude Medical to publicly disclose the amount of such ");
		com.append("payments and the identity of the recipients annually.</p>");
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("By attending this training event, you acknowledge that you or a facility with which you are associated either has an existing agreement to purchase product with ");
		com.append("St. Jude Medical or is considering entering into an agreement to purchase product after evaluating the appropriate use and functionality of the product. ");
		com.append("Massachusetts law requires this in order for our company to pay the training costs of healthcare practitioners licensed in Massachusetts.</p>");
		com.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		com.append("<strong>Notice for Healthcare Professionals Licensed to Practice Medicine in the State of Vermont</strong><br />");
		com.append("St. Jude Medical agrees to pay for reasonable expenses (including meals, and lodging) associated with the product training you will receive at this ");
		com.append("Epiducer system training workshop and lab. Vermont law requires St. Jude Medical to publicly disclose the amount of such payments and the identity of the ");
		com.append("recipients annually.</p>");
		com.append("<br />");
		return com;
	}
	
	/**
	 * Initial rep notification of the physicians selected to attend training.
	 * @return
	 */
	public StringBuffer formatRepGroupNotification() {
		StringBuffer letter = new StringBuffer(this.getWrapperBodyTop());
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Dear <strong>");
		letter.append(rep.getFirstName()).append(" ").append(rep.getLastName());
		letter.append("</strong>:</p>");
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("This email is to notify you that the following physicians have been selected to attend an exclusive <strong>Epiducer lead delivery system</strong> hands-on product training workshop:</p>");
		
		letter.append("<table border=\"0\" width=\"550\">");
		letter.append("<tr><th>Physician</th><th>Group</th></tr>");
		
		int rowCount = 0;
		for (SurgeonVO s : surgeons) {
			rowCount++;
			if (rowCount % 2 == 0) {
				letter.append("<tr style=\"background-color: #e1eafe;\">");
			} else {
				letter.append("<tr style=\"background-color: #c0d2ec;\">");
			}
			letter.append("<td style=\"text-align: center;\">Dr. ").append(s.getFirstName()).append(" ").append(s.getLastName()).append("</td>");
			letter.append("<td style=\"text-align: center;\">").append(findGroupCode(s.getProductGroupNumber())).append("</td>");
			letter.append("</tr>");
		}
		
		letter.append("</table><br/><br/>");
		
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Thank you,<br />");
		letter.append("Stacey Vess</p>");
		letter.append(this.getWrapperBodyBottom(rep.getEmailAddress()));
		return letter;
	}
	
	/**
	 * This is the notification sent to a rep for each physician that has been selected to attend training, one
	 * email to the rep per each physician selected.
	 * @return
	 */
	public StringBuffer formatRepFirstNotification() {
		StringBuffer letter = new StringBuffer(this.getWrapperBodyTop());
		StringBuffer info = new StringBuffer();
		StringBuffer physName = new StringBuffer(StringUtil.capitalizePhrase(surgeon.getFirstName()));
		physName.append(" ").append(StringUtil.capitalizePhrase(surgeon.getLastName()));
		info.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		info.append("Dear <strong>");
		info.append(StringUtil.capitalizePhrase(rep.getFirstName())).append(" ").append(StringUtil.capitalizePhrase(rep.getLastName()));
		info.append("</strong>:</p>");
		info.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		info.append("This email is to notify you that your physician, ").append(physName).append(", has been ");
		info.append("selected to attend an exclusive <strong>Epiducer lead delivery system</strong> ");
		info.append("hands-on product training workshop. Within three business days, you will receive a personalized ");
		info.append("invitation for your physician to attend this event (this correspondence will be sent to you and ");
		info.append("should be forwarded to your physician).</p>");
		info.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		info.append("<strong>").append(physName).append("</strong> will need to use the link included in the email ");
		info.append("to select the course date and location of the chosen meeting. If the available dates will not work, ");
		info.append("the physician will be given the option of being notified of future events when they become available. ");
		info.append("Upon acceptance of the invitation, the physician will be contacted by Plaza Travel to make travel arrangements.</p>");
		info.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		info.append("Please take this opportunity to personally notify your physician of this invitation. Epiducer system invitation cards have ");
		info.append("been sent to you for use when notifying the physician. If you have any questions regarding the registration process for ");
		info.append("the Epiducer lead delivery system course, please feel free to contact me at 972-309-8125.</p>");
		info.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		info.append("Thank you,<br />");
		info.append("Stacey Vess</p>");
		letter.append(info);
		letter.append(this.getWrapperBodyBottom(rep.getEmailAddress()));
		return letter;
	}
	
	/**
	 * This email is the reminder to the reps that they will be receiving physician invitations
	 * @return
	 */
	public StringBuffer formatRepSecondNotification() {
		StringBuffer letter = new StringBuffer(this.getWrapperBodyTop());
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Dear <strong>");
		letter.append(rep.getFirstName()).append(" ").append(rep.getLastName());
		letter.append("</strong>:</p>");
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("This email is to remind you that tomorrow you will be receiving the personalized invitation for your ");
		letter.append("physician to attend the Epiducer lead delivery system training. Before forwarding the email invitation ");
		letter.append("to your physician please confirm the following:</p>");
		letter.append("<ul>");
		letter.append("<li style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">The physician&rsquo;s name is spelled correctly</li>");
		letter.append("<li style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">You are sending the correct invitation to the correct physician</li>");
		letter.append("</ul>");
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		letter.append("Please remember that the invitations are personalized for each physician and should not be forwarded to ");
		letter.append("anyone other than the physician listed in the email. If you have any questions regarding the registration ");
		letter.append("process for the Epiducer lead delivery system course, please feel free to contact me at 972-309-8125.</p>");
		letter.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">Thank you,<br />Stacey Vess</p>");
		letter.append(this.getWrapperBodyBottom(rep.getEmailAddress()));
		return letter;
	}
	
	/**
	 * Returns the appropriate physician invitation based on the product group requested
	 * @param group
	 * @return
	 */
	public StringBuffer getPhysicianInvitation(Integer group) {
		StringBuffer invite = new StringBuffer();
		switch (group) {
			case 1:
			case 2:
			case 4:
				invite = getPhysicianInvitation();
				break;
			case 3:
				invite = getPhysicianSocietyInvitation();
				break;
			case 5:
				invite = getGroupFivePhysicianInvitation();
				break;
			case 6:
				invite = getGroupSixPhysicianInvitation();
				break;
		}
		return invite;
	}
	
	/**
	 * This is the personalized physician invitation that is sent to a rep, one invitation to the rep for each physician invited.
	 * @return
	 */
	public StringBuffer getPhysicianInvitation() {
		StringBuffer invite = new StringBuffer(this.getWrapperBodyTop());
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Dear Dr. <strong>");
		invite.append(StringUtil.capitalizePhrase(surgeon.getFirstName())).append(" ").append(StringUtil.capitalizePhrase(surgeon.getLastName()));
		invite.append("</strong>:</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("St. Jude Medical Neuromodulation Division cordially invites you to attend the <strong>Epiducer lead delivery system</strong> ");
		invite.append("hands-on product training workshop.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("The Epiducer lead delivery system is a revolutionary device that will enable multiple options with one needle stick, including the ");
		invite.append("introduction of up to three percutaneous leads or the percutaneous introduction of a slimline paddle lead in combination with two ");
		invite.append("percutaneous leads. This interactive, hands-on training will provide participants with the opportunity to learn about the device ");
		invite.append("from faculty implanters experienced with its use. The course agenda includes, but is not limited to, the following:</p>");
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<li>Product features and benefits</li>");
		invite.append("<li>Clinical implantation technique</li>");
		invite.append("<li>Hands-on cadaver lab</li>");
		invite.append("<li>Clinical research summary</li>");
		invite.append("<li>Epiducer system case studies</li>");
		invite.append("<li>Practical application of the Epiducer system in your neuromodulation practice</li>");
		invite.append("<li>Reimbursement</li>");
		invite.append("</ul>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Please visit <a href=\"http://").append(this.getGroupUrl(surgeon)).append("\" style=\"text-decoration:none;\"><strong><font color=\"#006c56\">");
		invite.append(this.getGroupUrl(surgeon)).append("</font></strong></a>");
		invite.append("&nbsp;to view the available course dates and locations. ");
		invite.append("These courses have a limited number of spaces and are by invitation only. St. Jude Medical will cover ");
		invite.append("the cost of your hotel stay and round-trip coach airfare. All travel must be arranged by Plaza Travel in ");
		invite.append("accordance with our travel and expense policy. If you have any questions, please contact the ");
		invite.append("St. Jude Medical Professional Education Team at 877-756-6441. I look forward to your participation at this meeting.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Sincerely,<br />");
		invite.append("<strong>Chris Chavez</strong></p>");
		invite.append(this.getCommittment());
		invite.append(this.getWrapperBodyBottomWithoutOptOut(surgeon.getEmailAddress()));
		return invite;
	}
	
	/**
	 * This is the personalized physician invitation that is sent to a rep, one invitation to the rep for each physician invited.
	 * @return
	 */
	public StringBuffer getGroupFivePhysicianInvitation() {
		StringBuffer invite = new StringBuffer(this.getWrapperBodyTop());
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Dear Dr. <strong>");
		invite.append(StringUtil.capitalizePhrase(surgeon.getFirstName())).append(" ").append(StringUtil.capitalizePhrase(surgeon.getLastName()));
		invite.append("</strong>:</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("St. Jude Medical Neuromodulation Division cordially invites you to attend the <strong>Epiducer lead delivery system</strong> ");
		invite.append("hands-on product training workshop.</p>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("The Epiducer lead delivery system is a revolutionary device that will enable multiple options with one needle stick, including the ");
		invite.append("introduction of up to three percutaneous leads or the percutaneous introduction of a slimline paddle lead in combination with two ");
		invite.append("percutaneous leads. This interactive, hands-on training will provide participants with the opportunity to learn about the device ");
		invite.append("from faculty implanters experienced with its use. The course agenda includes, but is not limited to, the following:</p>");
		
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<li>Product features and benefits</li>");
		invite.append("<li>Clinical implantation technique</li>");
		invite.append("<li>Hands-on cadaver lab</li>");
		invite.append("<li>Clinical research summary</li>");
		invite.append("<li>Epiducer system case studies</li>");
		invite.append("<li>Practical application of the Epiducer system in your neuromodulation practice</li>");
		invite.append("<li>Reimbursement</li>");
		invite.append("</ul>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Please visit <a href=\"http://").append(this.getGroupUrl(surgeon)).append("\" style=\"text-decoration:none;\"><strong><font color=\"#006c56\">");
		invite.append(this.getGroupUrl(surgeon)).append("</font></strong></a>");
		invite.append("&nbsp;to view the available course dates and locations. ");
		invite.append("These courses have a limited number of spaces and are by invitation only.</p>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<strong>November 5, 6, and December 17 Course Dates</strong>: St. Jude Medical will cover the cost of your hotel stay and ");
		invite.append("round-trip coach airfare. All travel must be arranged by Plaza Travel in accordance with our travel and expense policy. If you ");
		invite.append("have any questions, please contact the St. Jude Medical Professional Education Team at 877-756-6441.</p>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<strong>December 8, 2011 Course Date</strong>: As a participant in the NANS meeting, St. Jude Medical Neuromodulation Division ");
		invite.append("is unable to provide airfare to a conference that a physician would otherwise attend, regardless of the St. Jude Medical product training. ");
		invite.append("St. Jude Medical, however, may pay for lodging preceding product training that lasts six or more hours. Your hotel for the night of ");
		invite.append("December 7, 2011, must be arranged by Plaza Travel in accordance with our travel and expense policy. If you have any questions, please ");
		invite.append("contact the St. Jude Medical Professional Education Team at 877-756-6441.</p>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("I look forward to your participation at this meeting.</p>");
		
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Sincerely,<br />");
		invite.append("<strong>Chris Chavez</strong></p>");
		invite.append(this.getCommittment());
		invite.append(this.getWrapperBodyBottomWithoutOptOut(surgeon.getEmailAddress()));
		return invite;
	}
	
	public StringBuffer getPhysicianSocietyInvitation() {
		StringBuffer invite = new StringBuffer(this.getWrapperBodyTop());
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Dear Dr. <strong>");
		invite.append(StringUtil.capitalizePhrase(surgeon.getFirstName())).append(" ").append(StringUtil.capitalizePhrase(surgeon.getLastName()));
		invite.append("</strong>:</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("As a participant in the Napa Pain Conference, St. Jude Medical Neuromodulation Division cordially invites ");
		invite.append("you to attend a special pre-conference product training workshop on the Epiducer lead delivery system ");
		invite.append("scheduled for Thursday, September 15, 2011.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("The Epiducer lead delivery system is a revolutionary device that will enable multiple options with one needle stick, ");
		invite.append("including the introduction of up to three percutaneous leads or the percutaneous introduction of a slimline paddle lead ");
		invite.append("in combination with two percutaneous leads. This interactive, hands-on training will provide participants with the ");
		invite.append("opportunity to learn about the device from faculty implanters experienced with its use. The course agenda includes, ");
		invite.append("but is not limited to, the following:</p>");
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<li>Product features and benefits</li>");
		invite.append("<li>Clinical implantation technique</li>");
		invite.append("<li>Hands-on cadaver lab</li>");
		invite.append("<li>Clinical research summary</li>");
		invite.append("<li>Epiducer system case studies</li>");
		invite.append("<li>Practical application of the Epiducer system in your neuromodulation practice</li>");
		invite.append("<li>Reimbursement</li>");
		invite.append("</ul>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Please visit <a href=\"http://").append(this.getGroupUrl(surgeon)).append("\" style=\"text-decoration:none;\"><strong><font color=\"#006c56\">");
		invite.append(this.getGroupUrl(surgeon)).append("</font></strong></a>");
		invite.append("&nbsp;to register.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("St. Jude Medical Neuromodulation Division is unable to provide airfare to a conference that a physician would otherwise attend, ");
		invite.append("regardless of the St. Jude Medical product training. St. Jude Medical, however, may pay for lodging preceding product training ");
		invite.append("that lasts six or more hours. Your hotel for the night of September 14, 2011, must be arranged by Plaza Travel in accordance with ");
		invite.append("our travel and expense policy. If you have any questions, please contact the St. Jude Medical Professional Education Team ");
		invite.append("at 877-756-6441. I look forward to your participation at this meeting.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Sincerely,<br />");
		invite.append("<strong>Chris Chavez</strong></p>");
		invite.append(this.getSocietyCommittment());
		invite.append(this.getWrapperBodyBottomWithoutOptOut(surgeon.getEmailAddress()));
		return invite;		
	}
	
	public StringBuffer getGroupSixPhysicianInvitation() {
		StringBuffer invite = new StringBuffer(this.getWrapperBodyTop());
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Dear Dr. <strong>");
		invite.append(StringUtil.capitalizePhrase(surgeon.getFirstName())).append(" ").append(StringUtil.capitalizePhrase(surgeon.getLastName()));
		invite.append("</strong>:</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("St. Jude Medical Neuromodulation Division cordially invites you to attend the <strong>Epiducer lead delivery system</strong> ");
		invite.append("hands-on product training workshop on October 29, 2011.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("The Epiducer lead delivery system is a revolutionary device that will enable multiple options with one needle stick, including the ");
		invite.append("introduction of up to three percutaneous leads or the percutaneous introduction of a slimline paddle lead in combination with two ");
		invite.append("percutaneous leads. This interactive, hands-on training will provide participants with the opportunity to learn about the device ");
		invite.append("from faculty implanters experienced with its use. The course agenda includes, but is not limited to, the following:</p>");
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<li>Product features and benefits</li>");
		invite.append("<li>Clinical implantation technique</li>");
		invite.append("<li>Hands-on cadaver lab</li>");
		invite.append("<li>Clinical research summary</li>");
		invite.append("<li>Epiducer system case studies</li>");
		invite.append("<li>Practical application of the Epiducer system in your neuromodulation practice</li>");
		invite.append("<li>Reimbursement</li>");
		invite.append("</ul>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Please visit <a href=\"http://").append(this.getGroupUrl(surgeon)).append("\" style=\"text-decoration:none;\"><strong><font color=\"#006c56\">");
		invite.append(this.getGroupUrl(surgeon)).append("</font></strong></a>");
		invite.append("&nbsp;to register for the meeting. ");
		invite.append("This course has a limited number of seats and is by invitation only. If required, all travel must be arranged by Plaza Travel in ");
		invite.append("accordance with our travel and expense policy. If you have any questions, please contact the ");
		invite.append("St. Jude Medical Professional Education Team at 877-756-6441. I look forward to your participation at this meeting.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Sincerely,<br />");
		invite.append("<strong>Paul Hanchin</strong></p>");
		invite.append(this.getCommittment());
		invite.append(this.getWrapperBodyBottomWithoutOptOut(surgeon.getEmailAddress()));
		return invite;
	}
		
	public String getLongDate(Date d, int adj) {
		GregorianCalendar gCal = new GregorianCalendar();
		if (d == null) {
			// set calendar ahead by adj amount
			gCal.add(GregorianCalendar.DAY_OF_MONTH, adj);			
		} else {
			gCal.setTimeInMillis(d.getTime());
		}
		// return the date in text format (e.g. June 7, 2011)
		StringBuffer s = new StringBuffer();
		s.append(gCal.getDisplayName(GregorianCalendar.MONTH, 2, new Locale("en")));
		s.append(" ");
		s.append(gCal.get(GregorianCalendar.DAY_OF_MONTH));
		s.append(", ");
		s.append(gCal.get(GregorianCalendar.YEAR));
		return s.toString();
	}
	
	/**
	 * Returns the display value for the physician's group number
	 * @param groupNumber
	 * @return
	 */
	private String findGroupCode(Integer groupNumber) {
		String code = null;
		switch(groupNumber) {
		case 1:
			code = "1A";
			break;
		case 2:
			code = "1B";
			break;
		case 3:
			code = "S";
			break;
		case 4:
			code = "2";
			break;
		case 5:
			code = "3";
			break;
		}
		return code;
	}
		
}
