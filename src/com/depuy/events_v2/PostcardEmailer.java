package com.depuy.events_v2;

//JDK 1.6.3
import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;


// SMT BaseLibs
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.PersonVO.Role;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: PostcardEmailer.java
 * <p/>
 * <b>Description: </b> pulls the leads data from the patient data (profile
 * tables) for this postcard/mailing
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author James McKain
 * @version 1.0
 * @since Jan 31, 2014
 ****************************************************************************/
public class PostcardEmailer {

	protected static Logger log = null;
	protected Map<String, Object> attributes = null;
	protected Connection dbConn = null;
	
	/**
	 * @param attrs
	 */
	public PostcardEmailer(Map<String, Object> attrs, Connection conn) {
		this.attributes = attrs;
		log = Logger.getLogger(getClass());
		this.dbConn = conn;
	}

	

	/**
	 * sends event approval request to the site administrator
	 * 
	 * @param req
	 */
	public void sendApprovalRequest(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append(sem.getJointLabel()).append(" Community Education Seminar(s) Submitted by ");
		subject.append(user.getFirstName()).append(" ").append(user.getLastName());

		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" has submitted a new ").append(sem.getJointLabel());
		msg.append(" seminar request on ");
		msg.append(site.getSiteAlias()).append("\r\rPlease visit the website ");
		msg.append("to review the information provided and approve this request\r\r");
		msg.append(site.getFullSiteAlias()).append("\r");

		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("EventPostcardSubmit Admin Email Sent");
		} catch (Exception me) {
			log.error("EventPostcardSubmitEmail", me);
		}
		return;
	}


	/**
	 * send certain admins a notice that a postcard was canceled
	 * 
	 * @param req
	 * @param eventPostcardId
	 */
	public void sendPostcardCancellation(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO postcard = (DePuyEventSeminarVO) req.getAttribute("postcard");
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		StringBuilder msg = new StringBuilder();
		msg.append("The attached DePuy ").append(postcard.getJointLabel());
		msg.append(" postcard has been canceled.  ");
		msg.append("Please use the information attached as a reference ");
		msg.append("and visit the Community Education website for complete, ");
		msg.append("up-to-date information.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail()); // the DePuy intern in charge
			mail.addRecipient("KAlfano@its.jnj.com"); // Kristi Alfano
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addRecipient("admgt@hmktgroup.com"); // Barb Goley & Shari Slough
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com");
			mail.setSubject("DePuy Community Education; Postcard Canceled " + postcard.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport()); 

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("EventPostcardCanceled Email Sent");
		} catch (Exception me) {
			log.error("EventPostcardCanceled Email", me);
		}

		log.debug("done sending cancellation email");
		return;
	}
	

	/* (non-Javadoc)
	 * @see com.depuy.events.AbstractPostcardEmailer#sendSRCApprovalRequest(com.siliconmtn.http.SMTServletRequest)
	 */
	public void sendAdvApprovalRequest(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Territory " + sem.getTerritoryNumber() + " - Compliance Form for Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("A new DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar has been submitted on the Community Education website. ");
		msg.append("Please review the attached compliance form and approve within 24 hours.\r\r");
		msg.append("Follow the link below to review and approve this request.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=summary&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");

		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			//mail.addRecipient("WWilder@its.jnj.com");
			//mail.addRecipient("RSmith68@its.jnj.com");
			mail.addRecipient("rwilkin7@ITS.JNJ.COM");
			mail.addCC(site.getAdminEmail());
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("EventPostcardSubmit Admin Email Sent");
		} catch (Exception me) {
			log.error("EventPostcardSubmitEmail", me);
		}
		return;
	}
	
	
	/**
	 * announcement email triggered when the ADV team member reviews the
	 * compliance form (PDF) and clicks to approve their portion of the seminar.
	 * @param req
	 */
	protected void sendAdvApproved(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("New Seminar Annoucement - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("A new DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar has been submitted to Medical Affairs for review. ");
		msg.append("Please use the information attached as a reference and visit ");
		msg.append("the Community Education website for complete, up-to-date information and list pulls.\r\r");
		msg.append(site.getFullSiteAlias()).append("\r\r");

		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		AbstractSBReportVO compliance = (AbstractSBReportVO) req.getAttribute("complianceForm");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addCC(site.getAdminEmail());
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) 
				mail.addRecipient("rita.harman@hmktgroup.com");
			
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport());
			mail.addAttachment(compliance.getFileName(), compliance.generateReport());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("sendSrcApproved Email Sent");
		} catch (Exception me) {
			log.error("sendSrcApprovedEmail", me);
		}
		return;
	}

	
	/**
	 * announcement email triggered by Site Admin once SRC approves the Seminar
	 * @param req
	 */
	protected void sendSrcApproved(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		EventEntryVO event = sem.getEvents().get(0);
		DePuyEventSurgeonVO surg = sem.getSurgeon();
		StringBuilder subject = new StringBuilder();
		subject.append("SRC Approval - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("This email confirms that your ").append(event.getEventTypeDesc());
		msg.append(" ").append(sem.getJointLabel()).append(" Seminar #").append(event.getRSVPCode());
		msg.append(" as detailed below, has now been Approved by SRC and your ");
		msg.append("Surgeon Speaker contract will be sent shortly.\r\r");

		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("\r").append(event.getLocationDesc()).append("\rSpeaker: ");
		msg.append(surg.getSurgeonName()).append("\r\r");
		msg.append(event.getEventName()).append("\r");
		msg.append(event.getAddressText()).append("\r");
		if (StringUtil.checkVal(event.getAddress2Text()).length() > 0) 
			msg.append(event.getAddress2Text()).append("\r");
		msg.append(event.getCityName()).append(" ").append(event.getStateCode());
		msg.append(", ").append(event.getZipCode()).append("\r\r");
		msg.append("If you have any questions please contact Stefanie Sax ");
		msg.append("at 303-945-5184 or stef@siliconmtn.com\r\r");
		
		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			//mail.addCC("WWilder@its.jnj.com");
			//mail.addCC("RSmith68@its.jnj.com");
			mail.addCC("rwilkin7@ITS.JNJ.COM");
			for (PersonVO p : sem.getPeople()) {
				//add only the sales reps
				if (p.getRoleCode() == Role.TGM) continue;
				mail.addCC(p.getEmailAddress());
			}
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("sendAdvApproved Email Sent");
		} catch (Exception me) {
			log.error("sendAdvApprovedEmail", me);
		}
		return;
	}
	
	protected void orderConsumableBox(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		String ownerEmail = StringUtil.checkVal(sem.getOwner().getEmailAddress());
		subject.append("Consumable Box request - Seminar " + sem.getRSVPCodes());
		
		//logged-in user:
		//UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		StringBuilder msg = new StringBuilder();
		msg.append("The Seminar Coordinator for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has submitted a request for a Consumable Box.  ");
		msg.append("Please ship the requested seminar supplies to the person listed below. \r\r");
		msg.append("Seminar #: ").append(sem.getRSVPCodes()).append("\r");
		msg.append("Seminar Date: ").append(Convert.formatDate(sem.getEarliestEventDate(), Convert.DATE_FULL_MONTH)).append("\r");
		msg.append("Seminar Type: ").append(sem.getEvents().get(0).getEventTypeDesc()).append("\r\r");
		msg.append("Consumable Type(s) Requested:\r");
		String[] vals = req.getParameterValues("boxType");
		for (int i=0; i < vals.length; i++) {
			if (i > 0) msg.append(", ");
			msg.append(vals[i]);
		}
		msg.append("\r\rQuantity Needed:\r");
		msg.append(req.getParameter("qnty"));
		msg.append("\r\rMailing Address:\r");
		msg.append(req.getParameter("mailingAddress")).append("\r\r");
		msg.append("For more information about this Seminar please visit the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=summary&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("tkumfer@printlinc.net"); // Terrie
			mail.addRecipient("rwilkin7@its.jnj.com"); //Rachel Wilkinson
			mail.addCC(ownerEmail);
			//if (StringUtil.isValidEmail(user.getEmailAddress()) && ownerEmail != user.getEmailAddress())
			//	mail.addCC(user.getEmailAddress());
			mail.addCC(site.getAdminEmail());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("EventPostcardSubmit Admin Email Sent");
		} catch (Exception me) {
			log.error("EventPostcardSubmitEmail", me);
		}
		return;
	}
	
	/*
	 * This confirmation email was turned off by Rachel Wilkerson, 06-17-14,
	 * the recipient was added to the CC on the email to the admins (above)
	 * 
	protected void orderConsumableBoxConfirmation(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Consumable Box request confirmation - Seminar " + sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Your order for Seminar Supplies has been sent. You should ");
		msg.append("plan to receive your order within 7-10 days. Should you need ");
		msg.append("it before then, please contact Terrie Kumfer at Lincoln Printing; ");
		msg.append("tkumfer@printlinc.net or 260-414-8368.\r\r");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("orderConsumableBoxConfirmation Email Sent");
		} catch (Exception me) {
			log.error("orderConsumableBoxConfirmationEmail", me);
		}
		return;
	}
	*/
	
	protected void requestPostcardApproval(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Postcard uploaded - Seminar " + sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder();
		msg.append("The sample postcard for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has been uploaded to the website. ");
		msg.append("Please review the sample (PDF) and approve it using the URL below.\r\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC(site.getAdminEmail());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("requestPostcardApproval Admin Email Sent");
		} catch (Exception me) {
			log.error("requestPostcardApprovalEmail", me);
		}
		return;
	}
	
	
	protected void sendPostcardApproved(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Postcard approved - Seminar " + sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder();
		msg.append("The Seminar Coordinator for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has approved the sample postcard.\r\r");
		msg.append("More information can be found on the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addRecipient(site.getAdminEmail());
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("EventPostcardSubmit Admin Email Sent");
		} catch (Exception me) {
			log.error("EventPostcardSubmitEmail", me);
		}
		return;
	}
	
	
	protected void sendMedicalAffairsApprovedNotice(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Surgeon Speaker Contract Received - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("Medical Affairs has received and approved the Surgeon Speaker's signed contract for Seminar #").append(sem.getRSVPCodes());
		msg.append(".  This seminar is now fully approved.  Please proceed with ad negotiation, postcard creation, and ");
		msg.append("other necessary tasks to prepare for the seminar.\r\r");
		msg.append("More information can be found on the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) 
				mail.addRecipient("rita.harman@hmktgroup.com");
			
			mail.addCC(site.getAdminEmail());

			for (PersonVO p : sem.getPeople()) {
				if (! StringUtil.isValidEmail(p.getEmailAddress())) continue;
				mail.addCC(p.getEmailAddress());
			}
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("sendMedicalAffairsApprovedNotice Admin Email Sent");
		} catch (Exception me) {
			log.error("sendMedicalAffairsApprovedNotice", me);
		}
		return;
	}

}
