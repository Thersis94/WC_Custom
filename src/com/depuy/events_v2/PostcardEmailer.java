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
		subject.append("Compliance Form - Seminar " + sem.getRSVPCodes());

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
			mail.addRecipient("WWilder@its.jnj.com");
			mail.addRecipient("RSmith68@its.jnj.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("nbeasle@its.jnj.com");
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

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addCC(site.getAdminEmail());
			mail.addCC("nbeasle@its.jnj.com");
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			mail.addAttachment(rpt.getFileName(), rpt.generateReport());

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
		msg.append(event.getAddress2Text()).append("\r");
		msg.append(event.getCityName()).append(" ").append(event.getStateCode());
		msg.append(", ").append(event.getZipCode()).append("\r\r");
		msg.append("If you have any questions please contact ").append(site.getAdminName());
		msg.append(" at 303-945-5184 or ").append(site.getAdminEmail()).append("\r\r");
		
		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("nbeasle@its.jnj.com");
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC("WWilder@its.jnj.com");
			mail.addCC("RSmith68@its.jnj.com");
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
		subject.append("Consumable Box request - Seminar " + sem.getRSVPCodes());

		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		
		StringBuilder msg = new StringBuilder();
		msg.append("The Seminar Coordinator for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has submitted a request for a Consumable Box. ");
		msg.append("Please ship a Consumable Box to the address provided below.\r\r");
		msg.append("Type(s) Needed:\r");
		msg.append(StringUtil.getToString(req.getParameterValues("boxType"), false, true, ", "));
		msg.append("\r\rMailing Address:\r");
		msg.append(req.getParameter("mailingAddress")).append("\r\r");
		msg.append("For more information about this Seminar please visit the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=summary&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");


		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.addCC(site.getAdminEmail());
			mail.addCC("nbeasle@its.jnj.com");
			mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
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
			mail.addCC("nbeasle@its.jnj.com");
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
			mail.addRecipient("nbeasle@its.jnj.com");
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

}
