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
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
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
	 * returns an instance of PostcardEmailer, giving us abstract support for Mitek events
	 * @param sem
	 * @param attrs
	 * @param conn
	 * @return
	 */
	public static PostcardEmailer newInstance(DePuyEventSeminarVO sem, Map<String, Object> attrs, Connection conn) {
		//test for Mitek Seminar.  If so, return the Mitek emailer instead of 'this' class
		if (sem != null && sem.isMitekSeminar()) {
			return new PostcardEmailerMitek(attrs, conn);
		} else {
			return new PostcardEmailer(attrs, conn);
		}
	}
	
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
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
			mail.addRecipient("Kwebb1@its.jnj.com"); 
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addRecipient("admgt@hmktgroup.com");
			mail.addRecipient("sterling.hoham@hmktgroup.com");
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			if (postcard.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
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
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addRecipient(site.getAdminEmail());
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
		msg.append(" Seminar has been submitted to SRC for review. ");
		msg.append("Please use the information attached as a reference and visit ");
		msg.append("the Community Education website for complete, up-to-date information and list pulls.\r\r");
		msg.append(site.getFullSiteAlias()).append("\r\r");

		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		AbstractSBReportVO compliance = (AbstractSBReportVO) req.getAttribute("complianceForm");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.spencerman@hmktgroup.com");			
			//Additional CC recipients
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
			mail.addCC(site.getAdminEmail());
			
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
		StringBuilder subject = new StringBuilder();
		subject.append("SRC Approval - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("This email confirms that your ").append(event.getEventTypeDesc());
		msg.append(" ").append(sem.getJointLabel()).append(" Seminar #").append(event.getRSVPCode());
		msg.append(" as detailed below, has now been Approved by SRC and your ");
		msg.append("Speaker contract(s) will be sent shortly.\r\r");

		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("\r").append(event.getLocationDesc()).append("\rSpeaker(s): ");
		boolean isFirst = true;
		for (DePuyEventSurgeonVO vo : sem.getSurgeonList()) {
			if (!isFirst) msg.append(", ");
			msg.append(vo.getSurgeonName());
			isFirst = false;
		}
		msg.append("\r\r");
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
			mail.addRecipient(sem.getOwner().getEmailAddress()); //Coordinator
			//mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addCC("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addCC("amy.spencerman@hmktgroup.com");
			//mail.addCC("WWilder@its.jnj.com");
			//mail.addCC("RSmith68@its.jnj.com");
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC(site.getAdminEmail());
			for (PersonVO p : sem.getPeople()) { 
				//add only the sales reps
				if (p.getRoleCode() == Role.REP)
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
		boolean isHosp = sem.isHospitalSponsored();
		StringBuilder subject = new StringBuilder();
		String ownerEmail = StringUtil.checkVal(sem.getOwner().getEmailAddress());
		if (isHosp) {
			subject.append("Hospital Sponsored Consumable Box request - Seminar " + sem.getRSVPCodes());
		} else {
			subject.append("Consumable Box request - Seminar " + sem.getRSVPCodes());
		}
		
		StringBuilder msg = new StringBuilder(500);
		msg.append("The Seminar Coordinator for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has submitted a request for a ");
		if (isHosp) msg.append("Hospital Sponsored ");
		msg.append("Consumable Box.  ");
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
			mail.addCC("kshull@ITS.JNJ.com"); //Jill
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
	
	
	protected void requestPostcardApproval(SMTServletRequest req) {
		// send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		StringBuilder subject = new StringBuilder();
		subject.append("Postcard uploaded - Seminar " + sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder();
		msg.append("The sample postcard for DePuy Synthes ").append(sem.getJointLabel());
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
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
			//mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
		subject.append("Speaker Contract Received - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("Medical Affairs has received and approved the Speaker's signed contract for Seminar #").append(sem.getRSVPCodes());
		msg.append(".  This seminar is now fully approved.  Please proceed with ");
		msg.append("ad purchases, postcard mailings, flyer/poster distribution, ");
		msg.append("and other necessary tasks to prepare for the seminar.\r\r");
		msg.append("More information can be found on the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			//mail.addRecipient("Jenn.Davis@hmktgroup.com"); // Jenn Parrish-Davis);
			mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) 
				mail.addRecipient("rita.harman@hmktgroup.com");
			
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
			
			for (PersonVO p : sem.getPeople()) {
				if (! StringUtil.isValidEmail(p.getEmailAddress())) continue;
				//Add only the sales rep
				else if ( p.getRoleCode() == Role.REP ){
					mail.addCC(p.getEmailAddress());
					break;
				}
			}
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
	
	/**
	 * Sent when the coordinator has declined the postcard.
	 * @param req
	 */
	protected void sendPostcardDeclined(SMTServletRequest req){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		String reason = StringUtil.checkVal( req.getParameter("notesText") );
		
		//Create the message body
		StringBuilder msg = new StringBuilder(340);
		msg.append(sem.getOwner().getFullName()).append(" (");
		msg.append(sem.getOwner().getEmailAddress()).append(") has declined ");
		msg.append("the postcard for Seminar #").append(sem.getRSVPCodes()).append(".\r");
		if ( ! reason.isEmpty() ){
			msg.append("The coordinator commented:\r\t").append(reason).append("\r");
		}
		msg.append("\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("Sterling.Hoham@hmktgroup.com");
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.setSubject("Postcard Declined - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//Send the email
			MessageSender ms = new MessageSender(attributes,dbConn);
			ms.sendMessage(mail);
			log.debug("sendPostcardDeclined Sent");
			
		} catch (Exception e ){
			log.error("sendPostcardDeclined",e);
		}
	}
	
	/**
	 * Sent when the PCP invitation is ready for approval.
	 * @param req
	 */
	protected void sendInvitationApprovalRequest( SMTServletRequest req ){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		//Create message body
		StringBuilder msg = new StringBuilder(415);
		msg.append("The sample PCP Invitation for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has been uploaded to the website for approval. Please review ");
		msg.append("the sample (PDF) and approve using the url below.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("PCP Invitation Uploaded - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			
			//Recipients
			mail.addRecipient( sem.getOwner().getEmailAddress() );
			mail.addCC( site.getAdminEmail() );
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			//set the email content
			mail.setTextBody(msg.toString());
			
			//Send the message
			MessageSender mailer = new MessageSender(attributes, dbConn);
			mailer.sendMessage(mail);
			log.debug("sendInvitationApprovalRequest Sent");
			
		} catch (Exception e){
			log.error("sendInvitationApprovalRequest",e);
		}
	}

	/**
	 * Notification that the coordinator has approved the PCP invitation
	 * @param req
	 */
	protected void sendInvitationApproved( SMTServletRequest req ){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		//Create the message
		StringBuilder msg = new StringBuilder(130);
		msg.append("The seminar coordinator for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has approved the sample PCP Invitation.\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("PCP Invitation Approved - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//Recipients
			mail.addRecipient("sterling.hoham@hmktgroup.com");
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			
			//Send Message
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("sendInvitationApproved Sent");
			
		}catch (Exception e){
			log.error("sendInvitationApproved",e);
		}
	}
	
	/**
	 * Notification that the PCP Invitations have been sent.
	 * @param req
	 */
	protected void notifyInvitationSent( SMTServletRequest req ){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		StringBuilder msg = new StringBuilder(100);
		msg.append("PCP Invitations for Seminar #").append(sem.getRSVPCodes());
		msg.append(" have been sent.");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("PCP Invitation Mailing Confirmation - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//recipients
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC("sterling.hoham@hmktgroup.com");
			
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("notifyInvitationSent Sent");
			
		} catch (Exception e){
			log.error("notifyInvitationSent",e);
		}
	}
	
	/**
	 * Notification that the Postcards have been sent.
	 * @param req
	 */
	protected void notifyPostcardSent( SMTServletRequest req ){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		StringBuilder msg = new StringBuilder(100);
		msg.append("Postcards for Seminar #").append(sem.getRSVPCodes());
		msg.append(" have been sent.");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Postcard Mailing Confirmation - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//recipients
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC("sterling.hoham@hmktgroup.com");
			
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("notifyPostcardSent Sent");
			
		} catch (Exception e){
			log.error("notifyPostcardSent",e);
		}
	}
	
	/**
	 * notifies Harmony when a PCP coordinator uploads their leads
	 * @param req
	 */
	protected void inviteFileUploaded(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		StringBuilder msg = new StringBuilder(100);
		msg.append("The PCP Invite List has been uploaded and is now available on the portal. ");
		msg.append("Please click the following link below to view and download the file.\r\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("PCP Invite List Now Available - Seminar "+ sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//recipients
			mail.addRecipient("sterling.hoham@hmktgroup.com");
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("inviteFileUploaded Sent");
			
		} catch (Exception e) {
			log.error("inviteFileUploaded",e);
		}
	}
}
