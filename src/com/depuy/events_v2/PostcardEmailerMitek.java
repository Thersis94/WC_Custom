package com.depuy.events_v2;

//JDK 1.6.3
import java.sql.Connection;
import java.util.Map;



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
public class PostcardEmailerMitek extends PostcardEmailer {
	
	/**
	 * @param attrs
	 */
	public PostcardEmailerMitek(Map<String, Object> attrs, Connection conn) {
		super(attrs, conn);
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
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			if (sem.getTgmEmail() != null && sem.getTgmEmail().length() > 0)
				mail.addCC(sem.getTgmEmail().split(", "));
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
			mail.addRecipient("ksmith49@its.jnj.com"); // the DePuy intern in charge
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC("lisa.maiers@novusmediainc.com");
			mail.addCC("Amy.Zimmerman@hmktgroup.com");
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
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
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
		//Mitek does not use SRC approval, this email is not needed.
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
		subject.append("Seminar Approval - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append("This email confirms that your ").append(event.getEventTypeDesc());
		msg.append(" ").append(sem.getJointLabel()).append(" Seminar #").append(event.getRSVPCode());
		msg.append(" as detailed below, has now been Approved by EI Marketing and your ");
		msg.append("Speaker contract will be sent shortly.\r\r");

		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("\r").append(event.getLocationDesc()).append("\rSpeaker: ");
		msg.append(surg.getSurgeonName()).append("\r\r");
		msg.append(event.getEventName()).append("\r");
		msg.append(event.getAddressText()).append("\r");
		if (StringUtil.checkVal(event.getAddress2Text()).length() > 0) 
			msg.append(event.getAddress2Text()).append("\r");
		msg.append(event.getCityName()).append(" ").append(event.getStateCode());
		msg.append(", ").append(event.getZipCode()).append("\r\r");
		msg.append("If you have any questions please contact Kristen Smith ");
		msg.append("at  (508) 977-3873 or ksmith49@its.jnj.com\r\r");
		
		// build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("lisa.maiers@novusmediainc.com");
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC("Amy.Zimmerman@hmktgroup.com");
			for (PersonVO p : sem.getPeople()) { 
				//add only the sales reps
				if (p.getRoleCode() == Role.TGM)
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
		msg.append("The EISC for DePuy ").append(sem.getJointLabel());
		msg.append(" Seminar #").append(sem.getEvents().get(0).getRSVPCode());
		msg.append(" has submitted a request for a Patient Education Seminar Kit.  ");
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
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC(ownerEmail);
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
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
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
			//mail.addRecipient("sterling.hoham@hmktgroup.com"); // Sterling Hoham
			//mail.addCC("amy.zimmerman@hmktgroup.com");
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.addCC("mroderic@its.jnj.com");
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
		EventEntryVO event = sem.getEvents() != null ? sem.getEvents().get(0) : null;
		String name = "EI Marketing";
		if (event != null && "MITEK-PEER".equals(event.getEventTypeCd())) name = "Contracting Department";
		
		StringBuilder subject = new StringBuilder();
		subject.append("Speaker Contract Received - Seminar " + sem.getRSVPCodes());

		StringBuilder msg = new StringBuilder();
		msg.append(name).append(" has received and approved the Speaker's signed contract for Seminar #").append(sem.getRSVPCodes());
		msg.append(".  This seminar is now fully approved.  Please proceed with ");
		msg.append("ad purchases, postcard mailings, flyer/poster distribution, ");
		msg.append("and other necessary tasks to prepare for the seminar.\r\r");
		msg.append("More information can be found on the website.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("lisa.maiers@novusmediainc.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC("Amy.Zimmerman@hmktgroup.com");
			
			for (PersonVO p : sem.getPeople()) {
				if (! StringUtil.isValidEmail(p.getEmailAddress())) continue;
				//Add only the sales rep
				else if ( p.getRoleCode() == Role.TGM ){
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
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
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
		msg.append("The sample Peer to Peer Invitation for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has been uploaded to the website for approval. Please review ");
		msg.append("the sample (PDF) and approve using the url below.\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Peer to Peer Invitation Uploaded - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			
			//Recipients
			mail.addRecipient( sem.getOwner().getEmailAddress() );
			mail.addCC( "ksmith49@its.jnj.com" );
			mail.addCC("mroderic@its.jnj.com");
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
		msg.append("The EISC for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has approved the sample Peer to Peer Invitation.\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Peer to Peer Invitation Approved - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//Recipients
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
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
		msg.append("Peer to Peer Invitations for Seminar #").append(sem.getRSVPCodes());
		msg.append(" have been sent.");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Peer to Peer Invitation Mailing Confirmation - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//recipients
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			
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
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addRecipient("mroderic@its.jnj.com");
//			mail.addCC("amy.zimmerman@hmktgroup.com");
//			mail.addCC("sterling.hoham@hmktgroup.com");
			
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("notifyPostcardSent Sent");
			
		} catch (Exception e){
			log.error("notifyPostcardSent",e);
		}
	}
	
	
	/**
	 * notifies Harmony when a PEER coordinator uploads their leads
	 * @param req
	 */
	protected void inviteFileUploaded(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		StringBuilder msg = new StringBuilder(100);
		msg.append("The PEER Invite List has been uploaded and is now available on the portal. ");
		msg.append("Please click the following link below to view and download the file.\r\r");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=");
		msg.append(sem.getEventPostcardId()).append("\r\r");
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("PEER Invite List Now Available - Seminar "+ sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			//recipients
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addRecipient("mroderic@its.jnj.com");
			
			MessageSender mailer = new MessageSender(attributes,dbConn);
			mailer.sendMessage(mail);
			log.debug("inviteFileUploaded Sent");
			
		} catch (Exception e) {
			log.error("inviteFileUploaded",e);
		}
	}
}
