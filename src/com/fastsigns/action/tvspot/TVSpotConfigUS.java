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
 * running in the US.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 1, 2014
 ****************************************************************************/
public class TVSpotConfigUS extends TVSpotConfig {

	public TVSpotConfigUS() {
		super(getStatus(), getContactFields());
	}
	
	public String getCountryCode() {
		return "US";
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
		//data.put(ContactField.businessChallenge, "c0a8023727e92d4c94ee061a529c7d3c");
		//data.put(ContactField.companyNm, "c0a80237b0c703fd4020174ce3a74dfd");
		//data.put(ContactField.industry, "c0a8022d4aa7a83def1d1f05458cc933");
		//data.put(ContactField.department, "c0a802374ae4a1823f8e3f128a806466");
		//data.put(ContactField.title, "c0a802374af32fa435952a608c8c3946");
		data.put(ContactField.state,"6a000001567b18842a834a598cdeafa");
		data.put(ContactField.zipcode, "c0a8022d4af41a7fa75a85ccdfdb1b37");
		data.put(ContactField.preferredLocation, "c0a802374be51c9177a78a7b7677ea5c");
		data.put(ContactField.status, "7f0001019c4932bc3629f3987f43b5ec");
		data.put(ContactField.transactionNotes, "7f000101ed12428e6f503d8d58e4ef90");
		data.put(ContactField.saleAmount, "6d000001567b18842a834a598cdeafa");
		data.put(ContactField.feedback, "6c000001567b18842a834a598cdeafa");
		data.put(ContactField.rating, "6b000001567b18842a834a598cdeafa");
		data.put(ContactField.inquiry, "6e000001567b18842a834a598cdeafa");
		data.put(ContactField.consultation, "c0a8022d42b780adb6b4539af9f91878");
		data.put(ContactField.visitMethod, "0a00141d5e92b12310fa5f6b2d4c9ccc");
		data.put(ContactField.seenCommercial, "0a00141d5e92b12310fa5f6b2d4c9ccd");
		return data;
	}


	/* (non-Javadoc)
	 * @see com.fastsigns.action.tvspot.TVSpotConfig#addCenterEmailParamsToReq(com.siliconmtn.http.SMTServletRequest, com.smt.sitebuilder.action.dealer.DealerLocationVO)
	 */
	@Override
	public void addCenterEmailParamsToReq(SMTServletRequest req, DealerLocationVO dealer) {
		
	}
	
	
	public String getDealerEmailSubject(String name) {
		return "LEAD FROM TV:  Enact \"Operation Consultation\" within 24 hours: " + name;
	}
	
	
	public String getDealerEmailHeader() {
		StringBuilder msg = new StringBuilder();
		msg.append("<p><b>Please contact the prospect below using the information provided within 24 hours; ");
		msg.append("he or she will receive an email survey in seven business days asking them to rate their ");
		msg.append("experience with your center and their FASTSIGNS&reg; consultation.</b><br/>This prospect has chosen ");
		msg.append("your location and completed a form requesting a consultation after seeing our \"Operation ");
		msg.append("Consultation\" commercial on television, online or on our website.  We recommend that you ");
		msg.append("call and then follow up with an email if you are unable to connect with them on your initial ");
		msg.append("attempt. You can determine whether the actual consultation is via phone or in-person.</p><br/>");
		return msg.toString();
	}
	
	
	public String getDealerEmailFooter() {
		StringBuilder msg = new StringBuilder();
		msg.append("<b>Here are six important things for you to know:</b></br>");
		msg.append("<ol>");
		msg.append("<li>This prospect chose you from nearby locations; we have provided he/she with your ");
		msg.append("center contact information and have told them that someone would be in touch.</li>");
		msg.append("<li>This email is being sent to both your center and Franchise Partner email accounts; a ");
		msg.append("second email reminding you to contact this prospect will be automatically sent to these ");
		msg.append("addresses at the end of the next business day.</li>");
		msg.append("<li>We will track your consultation requests and survey feedback in the Web Edit tool ");
		msg.append("(<a href=\"http://www.fastsigns.com/webedit?mbk=true\">www.fastsigns.com/webedit</a>); you'll get an email ");
		msg.append("each day you have activity (consultation requests, surveys answered, etc.).</li>");
		msg.append("<li>Periodically we will send you a request to tell us if the leads generated sales, and if ");
		msg.append("so, the sale amount.  If you would like to proactively provide this information, you can update the ");
		msg.append("\"Consultation Request\" section at <a href=\"http://www.fastsigns.com/webedit?mbk=true\">www.fastsigns.com/webedit</a>. ");
		msg.append("If you choose to, you can review and update the \"status\" column to indicate the status of contacting ");
		msg.append("the prospect and view survey results.</li>");
		msg.append("<li>This survey question will be automatically emailed to the prospect seven business days after ");
		msg.append("their initial consultation request:<div style=\"margin:30px 0 30px 40px;\">");
		msg.append("<i>Thank you for your recent request for a consultation from ");
		msg.append("FASTSIGNS&reg;. Please take a moment to rate your satisfaction level with the consultation and tell ");
		msg.append("us about your experience.<ul><li>How satisfied were you with your consultation? Please select a ");
		msg.append("ranking between 1 (not satisfied at all) and 10 (extremely satisfied)</li>");
		msg.append("<li>If desired, please tell us more about your experience (open-ended with space for at least 250 words).</li></ul></i></div></li>");
		msg.append("<li>For more information about \"Operation Consultation\", please refer to the following resources or ");
		msg.append("consult with your Franchise Business Consultant and/or your Marketing Services Manager:<br/>");
		msg.append("<ul>");
		msg.append("<li>Watch the TV spot: (15 sec) <a href='https://www.youtube.com/watch?v=BDu-yuOnpmQ'>https://www.youtube.com/watch?v=BDu-yuOnpmQ</a> ");
		msg.append("and (30 sec) <a href='https://www.youtube.com/watch?v=QhvPTqOkhZM'>https://www.youtube.com/watch?v=QhvPTqOkhZM</a></li>");
		msg.append("<li>Review the overview document: ");
		msg.append("<a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=7430'>");
		msg.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=7430</a> (PowerPoint presentation)</li>");
		msg.append("<li>View the webinar: ");
		msg.append("<a href='http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html'>");
		msg.append("http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html</a> ");
		msg.append("(Connect with Catherine)</li>");
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
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf\">www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf</a> ");
		msg.append("or visit the FASTSIGNS online <a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter\">Learning Center</a> to access our ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/WhitePapers\">white papers</a>, and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/DesignTips\">helpful tips</a> and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/SignInformation\">information</a>.</p>");
		
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
	
	public String getCorpConsultationEmail(){
		return "consultation@fastsigns.com";
	}
	
	public EmailMessageVO buildSurveyEmail(String contactSubmittalId) {
		EmailMessageVO vo = new EmailMessageVO();
		vo.setSubject("Please complete a one question survey about your FASTSIGNS consultation");
		
		//msg body
		StringBuilder body = new StringBuilder(200);
		body.append("<p>Thank you for your recent request for a consultation from FASTSIGNS&reg;.</p>");
		body.append("<p>Please take a moment to rate your satisfaction level with the consultation and tell us about your experience.  ");
		body.append("We will ask you to rate us from 1-10 regarding your satisfaction level with your FASTSIGNS consultation.</p>");
		body.append("<a href=\"http://www.fastsigns.com/consultfastsigns?contactSubmittalId=");
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
		body.append("request from our TV campaign that included the prospect's contact information.  ");
		body.append("The information that was sent to you is below; if you have not already tried ");
		body.append("to contact the prospect, please do so today.  The prospect will receive a survey* ");
		body.append("in six business days to learn about their experience with your center.</p>");
		body.append("<p style=\"margin-left:40px;\">");
		body.append("<font color=\"red\">Name: </font>").append(vo.getFullName()).append("<br/>");
		body.append("<font color=\"red\">Email: </font>").append(vo.getEmailAddress()).append("<br/>");
		body.append("<font color=\"red\">Contact phone: </font>").append(pnf.getFormattedNumber()).append("<br/>");
		body.append("<font color=\"red\">Zip/Postal code: </font>").append(vo.getExtData().get(getContactId(ContactField.zipcode))).append("<br/>");
		body.append("<font color=\"red\">Other information provided: </font>");
		body.append(StringUtil.checkVal(vo.getExtData().get(getContactId(ContactField.feedback)),"<i>none</i>")).append("</p>");
		body.append("<p>For more information about \"Operation Consultation\", please refer to the following resources or ");
		body.append("consult with your Franchise Business Consultant and/or your Marketing Services Manager:</p>");
		body.append("<ul>");
		body.append("<li>Watch the TV spot: (15 sec) <a href='https://www.youtube.com/watch?v=BDu-yuOnpmQ'>https://www.youtube.com/watch?v=BDu-yuOnpmQ</a> ");
		body.append("and (30 sec) <a href='https://www.youtube.com/watch?v=QhvPTqOkhZM'>https://www.youtube.com/watch?v=QhvPTqOkhZM</a></li>");
		body.append("<li>Review the overview document: ");
		body.append("<a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=7430'>");
		body.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=7430</a> (PowerPoint presentation)</li>");
		body.append("<li>View the webinar: ");
		body.append("<a href='http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html'>");
		body.append("http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html</a> ");
		body.append("(Connect with Catherine)</li>");
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
		body.append("<p>If desired, please tell us more about your experience (open-ended with space for at least 250 words).</p></div><br/>");
		
		msg.setHtmlBody(body.toString());
		msg.setFrom(getDefaultSenderEmail());
		
		return msg;
	}
	
	
	public EmailMessageVO buildCorpReportEmail() throws InvalidDataException {
		EmailMessageVO msg = new EmailMessageVO();
		msg.addRecipient("operationconsultation@fastsigns.com");
		msg.setSubject("\"Operation Consultation\" report is attached for your review");
		msg.setHtmlBody(buildReportBody(false));
		msg.setFrom(getCorpConsultationEmail());
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
			body.append("requests that have been received year to date for your center. The requests ");
			body.append("are the result of a prospect seeing our TV spot or finding information about ");
			body.append("the consultation option on fastsigns.com, and selecting your center.</p>");
		} else {
			body.append("Dear Corporate Employee:");
			body.append("<p>The attached \"Operation Consultation\" report is a record of the consultation ");
			body.append("requests that have been received year to date for all locations. The requests ");
			body.append("are the result of a prospect seeing our TV spot or finding information about ");
			body.append("the consultation option on fastsigns.com, and selecting a center.</p>");
		}
		
		body.append("This report includes the following information:<br/>");
		body.append("<ul>");
		body.append("<li>Date and time consultations requested</li>");
		if (byCenter) {
			body.append("<li>Your center web number</li>");
		} else {
			body.append("<li>Center web number</li>");
		}
		body.append("<li>The prospect's name and contact information (email and phone number)</li>");
		body.append("<li>The prospects location information (Zip/postal code and state/province)</li>");
		body.append("<li>Any free form text information entered</li>");
		body.append("<li>The status of the request if <a href=\"http://www.fastsigns.com/webedit?mbk=true\">www.fastsigns.com/webedit</a> is updated (by the Franchise ");
		body.append("Partner or by us after asking the Franchise Partner) </li>");
		body.append("<li>The sale amount if <a href=\"http://www.fastsigns.com/webedit?mbk=true\">www.fastsigns.com/webedit</a> is updated (by the Franchise Partner or ");
		body.append("by us after asking) </li>");
		body.append("<li>Prospect survey status (sent, returned, feedback if provided)</li>");
		body.append("<li>Prospect's survey rating regarding their satisfaction with the consultation (1 is ");
		body.append("low and 10 is high)</li>");
		body.append("</ul>");
		body.append("For information about \"Operation Consultation\" and the TV spot, please consult your ");
		body.append("Franchise Business Consultant and/or your Marketing Services Manager. Additional ");
		body.append("information is available using the following resources:<br/>");
		body.append("<ul>");
		body.append("<li>Watch the TV spot: (15 sec) <a href='https://www.youtube.com/watch?v=BDu-yuOnpmQ'>https://www.youtube.com/watch?v=BDu-yuOnpmQ</a> ");
		body.append("and (30 sec) <a href='https://www.youtube.com/watch?v=QhvPTqOkhZM'>https://www.youtube.com/watch?v=QhvPTqOkhZM</a></li>");
		body.append("<li>Review the overview document: ");
		body.append("<a href='http://support.fastsigns.com/search/FileViewer.aspx?ki=7430'>");
		body.append("http://support.fastsigns.com/search/FileViewer.aspx?ki=7430</a> (PowerPoint presentation)</li>");
		body.append("<li>View the webinar: ");
		body.append("<a href='http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html'>");
		body.append("http://support.fastsigns.com/Stream/MonthlyRecording/ConnectWithCatherine/lib/playback.html</a> ");
		body.append("(Connect with Catherine)</li>");
		body.append("</ul>");
		
		return body.toString();
	}
	
	
	public String getDealerLocnField() { 
		return CON_ + getContactId(ContactField.preferredLocation);
	}
}
