package com.depuy.events_v2;

import java.sql.Connection;
import java.util.Date;
import java.util.Map;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.ConsigneeVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.PersonVO.Role;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: CoopAdsEmailer.java
 * <p/>
 * <b>Description: </b> contains all emails fired from the website based on
 * action/state changes (status_flg = ??)
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
public class CoopAdsEmailerMitek extends CoopAdsEmailer {

	public CoopAdsEmailerMitek(Map<String, Object> attrs, Connection conn) {
		super(attrs, conn);
	}


	public void notifyAdminOfAdDeclined(DePuyEventSeminarVO sem, SiteVO site, UserDataVO user, String reason, int cnt, CoopAdVO vo) {
		String label = (Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1) ? "Online" : "Newspaper";
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has declined ");
		msg.append(label).append(" Ad #").append(cnt).append(" offered for Seminar ");
		msg.append(sem.getRSVPCodes()).append("\r\n\r\n");
		if ( ! StringUtil.checkVal(reason).isEmpty() ){
			msg.append("The Coordinator commented:\r\n").append(reason).append("\r\n");
		}
		msg.append("\r\n");
		

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC("mroderic@its.jnj.com");
//			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Justin.Reyes@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
			}

			mail.setSubject(label + " Ad #" + cnt + " declined for Seminar " + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad declined Email Sent");

		} catch (Exception me) {
			log.error("Co-Op Ad declined", me);
		}
	}
	
	/**
	 * sends a notification to the coordinator about their ad options. (an uploaded file)
	 * @param sem
	 * @param site
	 * @param cnt
	 * @param vo
	 */
	public void reviewAdOptions(DePuyEventSeminarVO sem, SiteVO site, CoopAdVO vo) {
		boolean isOnline = false;//Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1;
		
		StringBuilder msg = new StringBuilder(500);
		msg.append("Dear Seminar Holder,\r\r");
		msg.append("The Newspaper Ad Options are now ready for your review/approval. ");
		msg.append("Please visit ");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=").append(sem.getEventPostcardId());
		msg.append(" to review all options and select newspaper(s), run dates, and pricing.  Once you are ready to submit, ");
		msg.append("click the \"Submit Ad Options\" button at the bottom of the page.  Within 24-48hrs you'll be receiving ");
		msg.append("a Final Newspaper Ad Approval Email with the run dates and pricing you chose and the final ad file for your review.\n\n");
		msg.append("Thank You,\rEvents.DePuySynthes.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC("ksmith49@its.jnj.com");
			mail.setSubject(((isOnline) ? "Online" : "Newspaper") + " Options - Seminar " + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);

			log.debug("Co-Op Ad options Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad options", me);
		}
	}
	
	
	/**
	 * sends a notification to Harmony with Ad option feedback from the coordinator
	 * @param sem
	 * @param site
	 * @param cnt
	 * @param vo
	 */
	public void feedbackAdOptions(DePuyEventSeminarVO sem, SiteVO site, CoopAdVO vo) {
		boolean isOnline = false; //Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1;
		//String eventType = StringUtil.checkVal(sem.getEvents().get(0).getEventTypeCd());
		//boolean isCFSEM = ( eventType.toUpperCase().startsWith("CFSEM") );
		
		StringBuilder msg = new StringBuilder(500);
		msg.append("The seminar coordinator has chosen their Newspaper Ad Options and they are listed below:\r\r\r");
		msg.append(vo.getOptionFeedbackText()).append("\r\r\r");
		msg.append("Harmony, please upload the chosen Newspaper Ad Details to the portal along with the final Ad Files ");
		msg.append("and change status to \"Pending Client Approval\".\r\r");
		msg.append("Thank You,\rEvents.DePuySynthes.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			//mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			//mail.addCC("Sterling.Hoham@hmktgroup.com");
			//if (! isCFSEM ){ //Additional recipients for DePuy Funded events
				//mail.addRecipient("lisa.maiers@novusmediainc.com");
				//mail.addCC("anna.schwanz@novusmediainc.com");
				//mail.addCC("taylor.larson@novusmediainc.com");
			//}
			mail.setSubject(((isOnline) ? "Online" : "Newspaper") + " Options Confirmed - Seminar " + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);

			log.debug("Co-Op Ad options feedback email sent");
		} catch (Exception me) {
			log.error("Co-Op Ad options feedback", me);
		}
	}
	

	public void requestCoordinatorApproval(DePuyEventSeminarVO sem, SiteVO site, CoopAdVO vo) {
		//Allow 5 business days for response
		Date dueDate = addBusinessDays(5);
		boolean isOnline = Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1;
		
		StringBuilder msg = new StringBuilder(500);
		msg.append("Dear Seminar Holder,\r\r");
		msg.append("The cost and proof for your ");
		if (isOnline) msg.append("Online ");
		msg.append("Ad(s) for Seminar ").append(sem.getRSVPCodes());
		msg.append(" are now ready for your approval.  Please visit ");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=").append(sem.getEventPostcardId()).append(" before ");
		msg.append(Convert.formatDate(dueDate)).append(" to either accept or reject this offer.  ");
		msg.append("If you fail to accept or reject by the given date you will ");
		msg.append("automatically accept the offer and your territory will be billed for the expense.\r\r");
		msg.append("Thank You,\rEvents.DePuySynthes.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("mroderic@its.jnj.com");
			mail.addCC("ksmith49@its.jnj.com");

			for (PersonVO p : sem.getPeople()) {
				if (! StringUtil.isValidEmail(p.getEmailAddress())) continue;
				//Add only the sales rep
				else if (p.getRoleCode() == Role.TGM ) {
					mail.addCC(p.getEmailAddress());
					break;
				}
			}
			mail.setSubject(((isOnline) ? "Online" : "Newspaper") + " Ad approval required - Seminar " + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);

			log.debug("Co-Op Ad requestClientApproval Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad requestClientApproval", me);
		}
	}

	/* Replacing this method with one that adds business days instead of days, to avoid 
	 * odd results (i.e. Fri 12/5/2014 + 3 business days should be Wed 12/10/2014
	 * instead of Mon 12/8/2014.  -Wingo 12/5/14
	 * 
	 * simple date wrapper that adds business days to today + #days passed.
	 * works around weekends.
	 * @param addDaysToToday
	 * @return
	private Date addBusinessDays(int addDaysToToday) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, addDaysToToday); // give them 5 days from today to approve the Ad
		
		// do not allow the deadline to fall on a weekend
		if (cal.get(Calendar.DAY_OF_WEEK) == 7)
			cal.add(Calendar.DATE, 2); // Saturday
		else if (cal.get(Calendar.DAY_OF_WEEK) == 1)
			cal.add(Calendar.DATE, 1); // Sunday
	
		return cal.getTime();
	}*/
	
	public void requestAdApprovalOfConsignee(DePuyEventSeminarVO sem, SiteVO site, boolean isHospital) {
		EventEntryVO event = sem.getEvents().get(0);
		ConsigneeVO consignee = sem.getConsignees().get((isHospital ? Long.valueOf(2) : Long.valueOf(1)));
		if (consignee == null) consignee = new ConsigneeVO();
		Date approvalDt = addBusinessDays(6);

		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(consignee.getContactName()).append(",");
		msg.append("<p>The promotional ad(s) for the ").append(sem.getJointLabel());
		msg.append(" patient education seminar scheduled on <i>");
		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("</i> at <i>").append(event.getEventName())
				.append("</i> are ready for your approval.</p>");
		
		msg.append("<p>Please click the link below to review the ad(s), ");
		msg.append("and your portion of the costs.  Once you approve, you will click on a link taking you to a secure PayPal account ");
		msg.append("set-up and contracted by DePuy Synthes.  Your credit card information will NOT ");
		msg.append("be provided to any party other than PayPal to ensure the privacy and security of your personal information.</p>");
		
		msg.append("<p>Ad approval and payment must be received by ").append(
				Convert.formatDate(approvalDt, Convert.DATE_LONG));
		msg.append(" or the seminar will be canceled.</p>");
		msg.append("<p><a href=\"http://").append(site.getSiteAlias())
				.append("/approve-ad?reqType=coopAdsReview&amp;eventPostcardId=")
				.append(sem.getEventPostcardId());
		if (isHospital) msg.append("&isHosp=1");
		msg.append("\">Click here to review &amp; approve this ad.</a></p>");
		msg.append("<p>Regards,<br/><i>DePuy Synthes Joint Reconstruction</i></p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(consignee.getEmail());
			mail.addBCC("ksmith49@its.jnj.com");
			mail.addBCC("mroderic@its.jnj.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject("Approval Required: Promotion for Seminar #" + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setHtmlBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad requestClientApprovalOfSurgeon Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad requestClientApprovalOfSurgeon", me);
		}
	}

	public void notifyAdminOfAdApproval(DePuyEventSeminarVO sem, SiteVO site,
			UserDataVO user, int cnt, CoopAdVO vo) {
		//Determine if it's co-funded or DePuy funded (used for subject line and recipients)
		String eventType = StringUtil.checkVal(sem.getEvents().get(0).getEventTypeCd());
		boolean isCFSEM = ( eventType.toUpperCase().startsWith("CFSEM") );
		String label = (Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1) ? "Online" : "Newspaper";
		
		//Build the subject text
		StringBuilder subject = new StringBuilder();
		subject.append(label + " Ad #" + cnt + " approved by Coordinator for ");
		subject.append( (isCFSEM ? "Co-Funded" : "DePuy Funded") );
		subject.append(" Seminar #").append(sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder(425);
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has approved ");
		msg.append(label).append(" Ad #").append(cnt).append(" for Seminar #").append(sem.getRSVPCodes()).append("\r\n\r\n");
		if (isCFSEM){
			msg.append("Harmony, please upload the final invoice and ");
			msg.append("amount the surgeon and/or hospital is responsible for to ");
			msg.append("the portal for their approval.\r\n");
		}
		msg.append("\r\n");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			if (! isCFSEM ){ //different recipients for DePuy Funded events
				//mail.addRecipient("amy.zimmerman@hmktgroup.com");
				if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
					mail.addCC("Justin.Reyes@umj3.com");
					mail.addCC("Evan.Pring@umj3.com");
					mail.addCC("lisav@metrosn.com");
				} else {
					mail.addCC("lisa.maiers@novusmediainc.com");
				}
				//mail.addCC("Sterling.Hoham@hmktgroup.com");
				mail.addCC(sem.getOwner().getEmailAddress());
				mail.addCC("ksmith49@its.jnj.com");
				mail.addCC("mroderic@its.jnj.com");
			} else {
				//CFSEM recipients
//				mail.addRecipient("amy.zimmerman@hmktgroup.com");
//				mail.addCC("ksmith49@its.jnj.com");
//				mail.addCC("mroderic@its.jnj.com");
//				mail.addCC("Sterling.Hoham@hmktgroup.com");
			}
			
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad Approved Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad Approved", me);
		}
	}

	/**
	 * notification sent when the surgeon views/approves their Ad. (outside of
	 * Events Mgmt system)
	 * 
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfSurgeonsApproval(DePuyEventSeminarVO sem, SiteVO site, boolean isHospital) {
		//Mitek does not do co-funded seminars
	}

	
	/**
	 * Notification sent out after Novus has uploaded ad details to the portal.
	 * @param sem
	 * @param site
	 */
	public void notifyNovusUpload( DePuyEventSeminarVO sem, SiteVO site ){
		//Message body
		StringBuilder msg = new StringBuilder(250);
		msg.append("Novus has uploaded all Newspaper Advertising options into ");
		msg.append("the portal for Seminar #").append(sem.getRSVPCodes()).append(". ");
		msg.append("Detailed information is now available for Harmony to begin ");
		msg.append("ad creation.\r\r");
		
		//Email subject
		StringBuilder subject = new StringBuilder(100);
		subject.append("Newspaper Advertising Options Available in Portal - Seminar ");
		subject.append(sem.getRSVPCodes());
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.addCC("Amy.Zimmerman@hmktgroup.com");
			mail.addCC("ksmith49@its.jnj.com");
			mail.addCC("mroderic@its.jnj.com");
			mail.setTextBody(msg.toString());
			
			//Send message
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Novus Upload Notification Sent");
		} catch (Exception e){
			log.error("Novus Upload Mailer", e);
		}
	}

	/**
	 * Notification that the ads have been placed in the newspapers.
	 * @param sem
	 * @param site
	 */
	protected void allAdsComplete(DePuyEventSeminarVO sem, SiteVO site) {
		StringBuilder msg = new StringBuilder(140);
		msg.append("Newspaper Advertising for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has been sent and confirmed by the publication(s).\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Newspaper Advertising Placement Confirmation - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			mail.addRecipient("ksmith49@its.jnj.com");
			mail.addRecipient("mroderic@its.jnj.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
//			mail.addCC("amy.zimmerman@hmktgroup.com");
//			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Justin.Reyes@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
			}
			
			MessageSender sender = new MessageSender(attributes,dbConn);
			sender.sendMessage(mail);
			log.debug("Notify Ad Placement Sent");
			
		} catch (Exception e){
			log.error("Ad Placement Notification",e);
		}
	}
}