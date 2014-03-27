package com.fastsigns.scripts;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.fastsigns.action.TVSpotReportVO;
import com.fastsigns.action.TVSpotUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.contact.ContactDataAction;
import com.smt.sitebuilder.action.contact.ContactDataActionVO;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: TVSpotEmailer.java<p/>
 * <b>Description: Handles the nightly email notifications sent out-of-band in 
 * relation to the Q2 2014 TV spot commercial/campaign.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 3, 2014
 ****************************************************************************/
public class TVSpotEmailer extends CommandLineUtil {
	
	private boolean isSurveyRun = false;
	private Map<String, Object> attributes;

	public TVSpotEmailer(String[] args) {
		super(args);
		loadProperties("scripts/fts_TVSpot.properties");
		loadDBConnection(props);
		isSurveyRun =  (args != null && args.length > 0 && "survey".equals(args[0]));
		makeAttribMap();
	}

	/**
	 * Set up the arguments for creating the Message Sender
	 * @throws ApplicationException
	 */
	private void makeAttribMap() {
		attributes = new HashMap<String, Object>();
		attributes.put("defaultMailHandler", new SMTMailHandler(props));
		attributes.put("instanceName", props.get("instanceName"));
		attributes.put("appName", props.get("appName"));
		attributes.put("adminEmail", props.get("adminEmail"));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TVSpotEmailer emailer = new TVSpotEmailer(args);
		emailer.run();
	}
	
		
	/**
	 * handles script invocation, to ensure all steps are executed in the proper sequence.
	 */
	public void run() {
		//load all the inquiries that have a status of pending
		ContactDataContainer cdc;
		try {
			//Attempt to create the message sender
			cdc = loadContactData();
			
			//only send the 7-day surveys if that's what we were invoked to do. 
			if (isSurveyRun) {
				sendSurveys(cdc);
				return;
			}
			
			//for the ones that are 1 day old, send the 1-day notification
			sendFirstNotice(cdc);
			
			//send reports to corporate
			sendCorpReport(cdc);
			
			// Send indivudual center reports
			sendCenterReport(cdc);
		} catch (ActionException e) {
			log.error("Could not create ContactDataContainer. ", e);
		}
		
	}
	
	/**
	 * Creates the ContactDataContainer via the ContactDataAction
	 * @return
	 * @throws ActionException
	 */
	private ContactDataContainer loadContactData() throws ActionException {
		ContactDataContainer cdc = null;
		
		// Create the VO that contains the information we need to send
		// to the ContactDataAction
		ContactDataActionVO vo = new ContactDataActionVO();
		vo.setContactId(props.getProperty("contactFormId"));
		vo.setEnd(Convert.getCurrentTimestamp().toString());
		
		// Create and set up the ContactDataAction
		ContactDataAction cda = new ContactDataAction();
		cda.setDBConnection(new SMTDBConnection(dbConn));
		cda.setAttribute("encryptKey", props.get("encryptKey"));
		cda.setAttribute("binaryDirectory", props.get("binaryDirectory"));
		
		cdc = cda.createCDC(vo, props.getProperty("hostName"));
		log.debug(cdc.getData().size());
		return cdc;
	}
	
	/**
	 * Send out any surveys for requests that are seven days old.  
	 * Eight and nine days as well on Mondays since the script
	 * will not be run on the weekends.
	 * @param cdc
	 */
	private void sendSurveys(ContactDataContainer cdc) {
		Calendar now = Calendar.getInstance();
		EmailMessageVO msg;
		boolean isMonday = now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
		int daysBetween;

		MessageSender ms = new MessageSender(attributes, dbConn);
		for (ContactDataModuleVO vo : cdc.getData()) {
			//calculate the days between today and the date the submission was created.
			daysBetween = (int) ((vo.getSubmittalDate().getTime() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24));
			switch (daysBetween) {
				case 8:
				case 9: if (!isMonday) 	continue;
				case 7:
					try {
						msg = new EmailMessageVO();
						msg.addRecipient(vo.getEmailAddress());
						msg.setSubject("Please complete a one question survey about your FASTSIGNS consultation");
						msg.setHtmlBody(buildSurveyBody(vo.getContactSubmittalId()));
						msg.setFrom(props.getProperty("senderEmailAddr"));

						ms.sendMessage(msg);
					} catch (InvalidDataException e) {
						log.error("Could not create email for submittal: " + vo.getContactSubmittalId(), e);
					}
			}
		}
	}
	
	/**
	 * Build the html body for the survey email
	 * @return
	 */
	private String buildSurveyBody(String contactSubmittalId) {
		StringBuilder body = new StringBuilder();
		body.append("Thank you for your recent request for a consultation from FASTSIGNS&reg;.<br/>");
		body.append("Please take a moment to rate your satisfaction level with the consultation and tell us about your experience.  ");
		body.append("We will ask you to rate us from 1-10 regarding your satisfaction level with your FASTSIGNS consultation.<br/>");
		body.append("<a href=http:/www.fastsigns.com/consultfastsigns?contactSubmittalId=");
		body.append(contactSubmittalId).append("&isSurvey=true\">Click on this survey link to continue</a>");
		return body.toString();
	}

	/**
	 * Run through the data we received and send first notice emails
	 * to all franchisees that have not responded to a day old submission
	 * @param cdc
	 */
	private void sendFirstNotice(ContactDataContainer cdc) {
		Calendar now = Calendar.getInstance();
		EmailMessageVO msg;
		TVSpotUtil.Status status;
		int daysBetween;
		MessageSender ms = new MessageSender(attributes, dbConn);
		
		for (ContactDataModuleVO vo : cdc.getData()) {
			// Make sure that this is a submittal that requires a first notice to be sent out.
			daysBetween = (int) ((vo.getSubmittalDate().getTime() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24));
			status = TVSpotUtil.Status.valueOf(vo.getExtData().get(TVSpotUtil.ContactField.status.id()));
			
			if (daysBetween != 1 || status != TVSpotUtil.Status.initiated)
				continue;

			msg = new EmailMessageVO();
			try {
				msg.addRecipient(vo.getDealerLocation().getOwnerEmail());
				msg.setSubject("Reminder: Enact \"Operation Consultation\" within 24 hours: " + vo.getFullName());
				msg.setHtmlBody(buildFirstNoticeBody(vo));
				msg.setFrom(props.getProperty("senderEmailAddr"));
				ms.sendMessage(msg);
			} catch (InvalidDataException e) {
				log.error("Could not create email for submittal: " + vo.getContactSubmittalId(), e);
			}
		}
	}

	/**
	 * Creates the html body for the first notice email
	 * @param vo
	 * @return
	 */
	private String buildFirstNoticeBody(ContactDataModuleVO vo) {
		StringBuilder body = new StringBuilder();
		body.append("Dear Franchise Partner,");
		body.append("<p>Yesterday you received an email notifying you about a consultation request from our ");
		body.append("TV campaign that included the prospect's contact information. The information that ");
		body.append("was sent to you is below; if you have not already tried to contact the prospect, ");
		body.append("please do so today. The prospect will receive a survey* in six business days to ");
		body.append("learn about their experience with your center.</p>");
		body.append("<p style=\"margin-left:40px;\">");
		body.append("<font color=\"red\">Name: </font>").append(vo.getFullName()).append("<br/>");
		body.append("<font color=\"red\">Email: </font>").append(vo.getEmailAddress()).append("<br/>");
		body.append("<font color=\"red\">Contact Phone: </font>").append(vo.getMainPhone()).append("<br/>");
		body.append("<font color=\"red\">Zip/Postal code: </font>").append(vo.getZipCode()).append("<br/>");
		body.append("<font color=\"red\">Other information provided: </font>");
		body.append(vo.getExtData().get(TVSpotUtil.ContactField.feedback.id())).append("</p>");
		body.append("<p>For more information about \"Operation Consultation\", please refer to the following resources or ");
		body.append("consult with your Franchise Business Consultant and/or your Marketing Services Manager:</p>");
		body.append("<ul>");
		body.append("<li>Watch the TV spot: <a href=\"http://www.fastsigns.com/#####\">www.fastsigns.com/###</a></li>");
		body.append("<li>Review the overview document: DOC ID ###</li>");
		body.append("<li>View the webinar: <a href=\"http://support.fastsigns.com#######\">support.fastsigns.com######</a></li>");
		body.append("</ul>");
		body.append("<p>* This survey question will be automatically emailed to the prospect seven business days after ");
		body.append("their initial consultation request:<br/>Thank you for your recent request for a consultation from ");
		body.append("FASTSIGNS&reg;. Please take a moment to rate your satisfaction level with the consultation and tell ");
		body.append("us about your experience. How satisfied were you with your consultation?</p>");
		body.append("<div style=\"margin-left:40px;width:100%;\">Please select a ");
		body.append("ranking between 1 (not satisfied at all) and 10 (extremely satisfied):<br/>");
		body.append("<table  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width='100%'><tbody>");
		body.append("<tr><td width='10%'><input type=\"radio\" name=\"num\" value=\"1\"> 1</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"2\"> 2</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"3\"> 3</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"4\"> 4</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"5\"> 5</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"6\">6</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"7\"> 7</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"8\"> 8</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"9\"> 9</td>");
		body.append("<td width='10%'><input type=\"radio\" name=\"num\" value=\"10\"> 10</td></tr>");
		body.append("</tbody></table>");
		body.append("<p>If desired, please tell us more about your experience (open-ended with space for at least 250 words).</p></div><br/>");
		
		return body.toString();
	}

	/**
	 * Send an excel document containing a summary of all the information 
	 * gathered by this contact us form to date to corporate
	 * @param cdc
	 */
	private void sendCorpReport(ContactDataContainer cdc) {
		TVSpotReportVO report = new TVSpotReportVO();
		report.setData(cdc);
		
		EmailMessageVO msg = new EmailMessageVO();
		MessageSender ms = new MessageSender(attributes, dbConn);
		try {
			msg.addRecipient(props.getProperty("operationsMailbox"));
			msg.setSubject("\"Operation Consultation\" report is attached for your review");
			msg.setHtmlBody(buildReportBody(false));
			msg.setFrom(props.getProperty("senderEmailAddr"));
			msg.addAttachment("Consultation Report.xls", report.generateReport());
			
			ms.sendMessage(msg);
		} catch (InvalidDataException e) {
			log.error("Could not create email for submittal: ", e);
		}
	}
	
	private void sendCenterReport(ContactDataContainer cdc) {
		TVSpotReportVO report = new TVSpotReportVO();
		report.setData(cdc);
		Map<String, StringBuilder> byCenter = report.generateCenterReport();
		
		EmailMessageVO msg;
		MessageSender ms = new MessageSender(attributes, dbConn);
		for (String email : byCenter.keySet()) {
			try {
				msg = new EmailMessageVO();
				msg.addRecipient(email);
				msg.setSubject("\"Operation Consultation\" report is attached for your review");
				msg.setHtmlBody(buildReportBody(true));
				msg.setFrom(props.getProperty("senderEmailAddr"));
				msg.addAttachment("Consultation Report.xls", byCenter.get(email).toString().getBytes());
				
				ms.sendMessage(msg);
			} catch (InvalidDataException e) {
				log.error("Could not create email for submittal: ", e);
			}
		}
	}
	
	/**
	 * Creates the html body for the corporate email
	 * @return
	 */
	private String buildReportBody(boolean byCenter) {
		StringBuilder body = new StringBuilder();
		
		// Handle the slight verbiage change based on who gets this email
		if (byCenter) {
			body.append("Dear Francise Partner:");
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
		body.append("<li>The status of the request if fastsigns.com/webedit is updated (by the Franchise ");
		body.append("Partner or by us after asking the Franchise Partner) </li>");
		body.append("<li>The sale amount if fastsigns.com/webedit is updated (by the Franchise Partner or ");
		body.append("by us after asking) </li>");
		body.append("<li>Prospect survey status (sent, returned, feedback if provided)</li>");
		body.append("<li>Prospect's survey rating regarding their satisfaction with the consultation (1 is ");
		body.append("low and 10 is high)</li>");
		body.append("</ul>");
		body.append("For information about \"Operation Consultation\" and the TV spot, please consult your ");
		body.append("Franchise Business Consultant and/or your Marketing Services Manager. Additional ");
		body.append("information is available using the following resources:<br/>");
		body.append("<ul>");
		body.append("<li>Watch the TV spot: <a href=\"http://www.fastsigns.com/#####\">www.fastsigns.com/#####</a></li>");
		body.append("<li>Review the overview document:  DOC ID ###</li>");
		body.append("<li>View the webinar:  <a href=\"http://support.fastsigns.com#######\">support.fastsigns.com#######</a></li>");
		body.append("</ul>");
		
		return body.toString();
	}
}
