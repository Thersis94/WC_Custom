package com.fastsigns.action.tvspot;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: TVSpotConfigUS.java<p/>
 * <b>Description: A country-specific implementation of the TVSpot program.
 * This class customizes the email messages and labeling for the "Consult Fastsigns" program 
 * running in the UK.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jul 10, 2014
 ****************************************************************************/
public class TVSpotConfigGB extends TVSpotConfig {

	public TVSpotConfigGB() {
		super(getStatus(), getContactFields());
	}
	
	public String getCountryCode() {
		return "GB";
	}
	
	
	/**
	 * defines the status labels for this country/implementation
	 * @return
	 */
	private static Map<Status, String> getStatus() {
		Map<Status, String> data = new HashMap<Status, String>();
		data.put(Status.initiated, "Awaiting Contact");
		data.put(Status.callNoAnswer, "Phone call made - no answer");
		data.put(Status.callLeftMessage, "Phone call made - left message");
		data.put(Status.callExistingCustomer, "Phone call made - existing customer - had conversation");
		data.put(Status.callSuccess, "Phone call made - sent information");
		data.put(Status.appointmentMade, "Appointment Made");
		data.put(Status.saleMade, "Sale Made");
		data.put(Status.prospect, "Prospect contacted us directly");
		data.put(Status.invalid, "Contact information invalid - bad email and bad phone number");
		return data;
	}
	
	/**
	 * defines the contactFieldIds (Strings) for this country/implementation
	 * @return
	 */
	private static Map<ContactField, String> getContactFields() {
		Map<ContactField, String> data = new HashMap<ContactField, String>();
		data.put(ContactField.state,"0a00141d220adabd32be5b47dde62b07");
		data.put(ContactField.zipcode, "0a00141d220e6334f46e53f5e9436801");
		data.put(ContactField.preferredLocation, "0a00141d22090019e477dbed8d0953ff");
		data.put(ContactField.status, "0a00141d2209b8aa3154e25cd7083eb0");
		data.put(ContactField.transactionNotes, "0a00141d220a1046875a8ae6739cbdd3");
		data.put(ContactField.saleAmount, "0a00141d220b0bdb69379049a6fd66d0");
		data.put(ContactField.feedback, "0a00141d220a956fd383a20b5f1d82b4");
		data.put(ContactField.rating, "0a00141d220a5090444a5cd08a412899");
		data.put(ContactField.inquiry, "0a00141d220c122591d0a3f028690b40");
		data.put(ContactField.consultation, "0a00141d220bdabbdc6e0498f66f631c");
		//data.put(ContactField.visitMethod, "0a00141d6a2339bcc1b10e57708ed185");
		return data;
	}


	/* (non-Javadoc)
	 * @see com.fastsigns.action.tvspot.TVSpotConfig#addCenterEmailParamsToReq(com.siliconmtn.http.SMTServletRequest, com.smt.sitebuilder.action.dealer.DealerLocationVO)
	 */
	@Override
	public void addCenterEmailParamsToReq(SMTServletRequest req, DealerLocationVO dealer) {
		
	}
	
	
	public String getDealerEmailSubject(String name) {
		return "SALES LEAD:  Enact \"Operation Consultation\" within 24 hours: " + name;
	}
	
	
	public String getDealerEmailHeader() {
		StringBuilder msg = new StringBuilder();
		msg.append("<p><b>Please contact the prospect below using the information provided within 24 hours; ");
		msg.append("he or she will receive an email survey in seven business days asking them to rate their ");
		msg.append("experience with your centre and their FASTSIGNS&reg; consultation.</b><br/>This prospect has chosen ");
		msg.append("your location and completed a form requesting a consultation after seeing our \"Operation ");
		msg.append("Consultation\" online or on our website.  We recommend that you ");
		msg.append("call and then follow up with an email if you are unable to connect with them on your initial ");
		msg.append("attempt. You can determine whether the actual consultation is via phone or in-person.</p><br/>");
		return msg.toString();
	}
	
	
	public String getDealerEmailFooter() {
		StringBuilder msg = new StringBuilder();
		msg.append("<b>Here are six important things for you to know:</b></br>");
		msg.append("<ol>");
		msg.append("<li>This prospect chose you from nearby locations; we have provided he/she with your ");
		msg.append("centre contact information and have told them that someone would be in touch.</li>");
		msg.append("<li>This email is being sent to both your centre and Franchise Partner email accounts; a ");
		msg.append("second email reminding you to contact this prospect will be automatically sent to these ");
		msg.append("addresses at the end of the next business day.</li>");
		msg.append("<li>We will track your consultation requests and survey feedback in the Web Edit tool ");
		msg.append("(<a href=\"http://www.fastsigns.co.uk/webedit?mbk=true\">www.fastsigns.co.uk/webedit</a>); you'll get an email ");
		msg.append("each day you have activity (consultation requests, surveys answered, etc.).</li>");
		msg.append("<li>Periodically we will send you a request to tell us if the leads generated sales, and if ");
		msg.append("so, the sale amount.  If you would like to proactively provide this information, you can update the ");
		msg.append("\"Consultation Request\" section at <a href=\"http://www.fastsigns.co.uk/webedit?mbk=true\">www.fastsigns.co.uk/webedit</a>. ");
		msg.append("If you choose to, you can review and update the \"status\" column to indicate the status of contacting ");
		msg.append("the prospect and view survey results.</li>");
		msg.append("<li>This survey question will be automatically emailed to the prospect seven business days after ");
		msg.append("their initial consultation request:<div style=\"margin:30px 0 30px 40px;\">");
		msg.append("<i>Thank you for your recent request for a consultation from ");
		msg.append("FASTSIGNS&reg;. Please take a moment to rate your satisfaction level with the consultation and tell ");
		msg.append("us about your experience.<ul><li>How satisfied were you with your consultation? Please select a ");
		msg.append("ranking between 1 (not satisfied at all) and 10 (extremely satisfied)</li>");
		msg.append("<li>If desired, please tell us more about your experience (open-ended with space for at least 250 words).</li></ul></i></div></li>");
		msg.append("<li>For more information about \"Operation Consultation\", please refer to the following: <br/>");
		msg.append("<ul>");
		msg.append("<li>Review the overview document: <a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=8281'>");
		msg.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=8281</a></li>");
		msg.append("</ul>");
		msg.append("</li>");
		msg.append("</ol>");
		return msg.toString();
	}


	/* (non-Javadoc)
	 * @see com.fastsigns.action.tvspot.TVSpotConfig#emailUserConfirmation(com.siliconmtn.http.SMTServletRequest, com.smt.sitebuilder.action.dealer.DealerLocationVO)
	 */
	@Override
	public EmailMessageVO buildUserConfirmationEmail(SMTServletRequest req, DealerLocationVO dealer) 
			throws InvalidDataException {
		PhoneNumberFormat phone = new PhoneNumberFormat(dealer.getPhone(), PhoneNumberFormat.PAREN_FORMATTING);
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>Thank you for submitting your consultation request to FASTSIGNS&reg;.</p>");
		msg.append("<p>Your request has been submitted to:</p>");
		
		msg.append("<p style='margin-left:40px;'>FASTSIGNS of ").append(dealer.getLocationName()).append("<br/>");
		msg.append(dealer.getAddress()).append("<br/>");
		if (dealer.getAddress2() != null && dealer.getAddress2().length() > 0) 
			msg.append(dealer.getAddress2()).append("<br/>");
		msg.append(dealer.getCity()).append(", ").append(dealer.getState()).append(" ").append(dealer.getZipCode()).append("<br/>");
		msg.append(phone.getFormattedNumber()).append("<br/>");
		msg.append(dealer.getEmailAddress()).append("</p>");
		
		msg.append("<p>FASTSIGNS of ").append(dealer.getLocationName());
		msg.append(" will contact you to discuss your visual communications needs and challenges.  You will receive a one-question survey in seven business days asking you about your experience.</p>");
		
		msg.append("<p>In the meantime, if you would like to learn more about the comprehensive ");
		msg.append("solutions that FASTSIGNS provides, download or view our guide online at ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.co.uk/binary/org/FTS/PDF/CSG-2013_2.pdf\">www.fastsigns.co.uk/binary/org/FTS/PDF/CSG-2013_2.pdf</a> ");
		msg.append("or visit the FASTSIGNS online <a target=\"_blank\" href=\"http://www.fastsigns.co.uk/LearningCenter\">Learning Center</a> to access our ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.co.uk/LearningCenter/WhitePapers\">white papers</a>, and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.co.uk/LearningCenter/DesignTips\">helpful tips</a> and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.co.uk/LearningCenter/SignInformation\">information</a>.</p>");
		
		msg.append("<p>Thank you for contacting FASTSIGNS.</p>");
		msg.append("<p><br/>Each FASTSIGNS&reg; location is independently owned and operated.</p><br/>");
		
		EmailMessageVO mail = new EmailMessageVO();
		mail.addRecipient(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
		mail.setSubject("Thank you for your consultation request from FASTSIGNS");
		mail.setFrom(dealer.getDealerLocationId() + "@fastsigns.com");
		mail.setHtmlBody(msg.toString());
		return mail;
	}
	
	public String getDefaultSenderEmail() {
		return "do_not_reply@fastsigns.com";
	}
	
	public EmailMessageVO buildSurveyEmail(String contactSubmittalId) {
		EmailMessageVO vo = new EmailMessageVO();
		vo.setSubject("Please complete a one question survey about your FASTSIGNS consultation");
		
		//msg body
		StringBuilder body = new StringBuilder(200);
		body.append("<p>Thank you for your recent request for a consultation from FASTSIGNS&reg;.</p>");
		body.append("<p>Please take a moment to rate your satisfaction level with the consultation and tell us about your experience.  ");
		body.append("We will ask you to rate us from 1-10 regarding your satisfaction level with your FASTSIGNS consultation.</p>");
		body.append("<a href=\"http://www.fastsigns.co.uk/consultfastsigns?contactSubmittalId=");
		body.append(contactSubmittalId).append("&amp;isSurvey=true\">Click on this survey link to continue</a>");
		vo.setHtmlBody(body.toString());
		
		return vo;
	}
	
	
	public EmailMessageVO buildFirstNoticeEmail(ContactDataModuleVO vo) throws InvalidDataException {
		EmailMessageVO msg = new EmailMessageVO();
		msg.setSubject("Reminder: Enact \"Operation Consultation\" within 24 hours: " + vo.getFullName());
		
		PhoneNumberFormat pnf = new PhoneNumberFormat(vo.getMainPhone(), PhoneNumberFormat.PAREN_FORMATTING);
		StringBuilder body = new StringBuilder();
		body.append("Dear Franchise Partner,");
		body.append("<p>Yesterday you received an email notifying you about a consultation ");
		body.append("request that included the prospect's contact information.  ");
		body.append("The information that was sent to you is below; if you have not already tried ");
		body.append("to contact the prospect, please do so today.  The prospect will receive a survey* ");
		body.append("in six business days to learn about their experience with your centre.</p>");
		body.append("<p style=\"margin-left:40px;\">");
		body.append("<font color=\"red\">Name: </font>").append(vo.getFullName()).append("<br/>");
		body.append("<font color=\"red\">Email: </font>").append(vo.getEmailAddress()).append("<br/>");
		body.append("<font color=\"red\">Contact phone: </font>").append(pnf.getFormattedNumber()).append("<br/>");
		body.append("<font color=\"red\">Post code: </font>").append(vo.getExtData().get(getContactId(ContactField.zipcode))).append("<br/>");
		body.append("<font color=\"red\">Other information provided: </font>");
		body.append(StringUtil.checkVal(vo.getExtData().get(getContactId(ContactField.feedback)),"<i>none</i>")).append("</p>");
		body.append("<p>For more information about \"Operation Consultation\", please refer to the following resources or ");
		body.append("consult with your Franchise Business Consultant:</p>");
		body.append("<ul>");
		body.append("<li>Review the overview document: <a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=8281'>");
		body.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=8281</a></li>");
		body.append("</ul>");
		body.append("<p>* This survey will be sent to this prospect in six business days: ");
		body.append("Thank you for your recent request for a consultation from FASTSIGNS&reg;.  ");
		body.append("Please take a moment to rate your satisfaction level with the consultation and tell ");
		body.append("us about your experience.  How satisfied were you with your consultation?</p>");
		body.append("<div style=\"margin-left:40px;\">Please select a ");
		body.append("ranking between 1 (not satisfied at all) and 10 (extremely satisfied):<br/>");
		body.append("<table  border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody>");
		body.append("<tr><td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"1\"> 1</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"2\"> 2</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"3\"> 3</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"4\"> 4</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"5\"> 5</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"6\">6</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"7\"> 7</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"8\"> 8</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"9\"> 9</td>");
		body.append("<td nowrap style='padding-right:15px'><input type=\"radio\" name=\"num\" value=\"10\"> 10</td></tr>");
		body.append("</tbody></table>");
		body.append("<p>Is there anything you want to tell us about your consultation experience? (open-ended with space for at least 250 words).</p></div><br/>");
		
		msg.setHtmlBody(body.toString());
		msg.setFrom(getDefaultSenderEmail());
		
		return msg;
	}
	
	
	public EmailMessageVO buildCorpReportEmail() throws InvalidDataException {
		EmailMessageVO msg = new EmailMessageVO();
		msg.addRecipient("Drue.Townsend@fastsigns.com"); 
		msg.addRecipient("E.Team@fastsigns.com");
		msg.addRecipient("david.callister@fastsigns.com");
		msg.addRecipient("Garth.Allison@fastsigns.com");
		msg.setSubject("\"Operation Consultation\" report is attached for your review");
		msg.setHtmlBody(buildReportBody(false));
		msg.setFrom(getDefaultSenderEmail());
		return msg;
	}
	
	public EmailMessageVO buildCenterReportEmail() throws InvalidDataException {
		EmailMessageVO msg = new EmailMessageVO();
		msg.setSubject("\"Operation Consultation\" report is attached for your review");
		msg.setHtmlBody(buildReportBody(true));
		msg.setFrom(getDefaultSenderEmail());
		return msg;
	}
	
	/**
	 * Creates the html body for the corporate email
	 * @return
	 */
	private String buildReportBody(boolean byCenter) {
		StringBuilder body = new StringBuilder();
		
		// Handle the slight verbiage change based on who gets this email
		if (byCenter) {
			body.append("Dear Franchise Partner:");
			body.append("<p>The attached \"Operation Consultation\" report is a record of the consultation ");
			body.append("requests that have been received year to date for your centre. The requests ");
			body.append("are the result of a prospect finding information about ");
			body.append("the consultation option on fastsigns.co.uk.</p>");
		} else {
			body.append("Dear Corporate Employee:");
			body.append("<p>The attached \"Operation Consultation\" report is a record of the consultation ");
			body.append("requests that have been received year to date for all locations. The requests ");
			body.append("are the result of a prospect finding information about ");
			body.append("the consultation option on fastsigns.co.uk.</p>");
		}
		
		body.append("This report includes the following information:<br/>");
		body.append("<ul>");
		body.append("<li>Date and time consultations requested</li>");
		if (byCenter) {
			body.append("<li>Your centre web number</li>");
		} else {
			body.append("<li>centre web number</li>");
		}
		body.append("<li>The prospect's name and contact information (email and phone number)</li>");
		body.append("<li>The prospects location information (Post code and town or city)</li>");
		body.append("<li>Any free form text information entered</li>");
		body.append("<li>The status of the request if <a href=\"http://www.fastsigns.co.uk/webedit?mbk=true\">www.fastsigns.co.uk/webedit</a> is updated (by the Franchise ");
		body.append("Partner or by us after asking the Franchise Partner) </li>");
		body.append("<li>The sale amount if <a href=\"http://www.fastsigns.co.uk/webedit?mbk=true\">www.fastsigns.co.uk/webedit</a> is updated (by the Franchise Partner or ");
		body.append("by us after asking) </li>");
		body.append("<li>Prospect survey status (sent, returned, feedback if provided)</li>");
		body.append("<li>Prospect's survey rating regarding their satisfaction with the consultation (1 is ");
		body.append("low and 10 is high)</li>");
		body.append("</ul>");
		body.append("For information about \"Operation Consultation\" please consult your ");
		body.append("Franchise Business Consultant. Additional ");
		body.append("information is available using the following resources:<br/>");
		body.append("<ul>");
		body.append("<li>Review the overview document: <a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=8281'>");
		body.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=8281</a></li>");
		body.append("</ul>");
		
		return body.toString();
	}
	
	
	public String getDealerLocnField() { 
		return CON_ + getContactId(ContactField.preferredLocation);
	}
}
