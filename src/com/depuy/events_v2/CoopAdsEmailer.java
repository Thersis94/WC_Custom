package com.depuy.events_v2;

import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.ConsigneeVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
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
public class CoopAdsEmailer {

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
	public static CoopAdsEmailer newInstance(DePuyEventSeminarVO sem, Map<String, Object> attrs, Connection conn) {
		//test for Mitek Seminar.  If so, return the Mitek emailer instead of 'this' class
		if (sem != null && sem.isMitekSeminar()) {
			return new CoopAdsEmailerMitek(attrs, conn);
		} else {
			return new CoopAdsEmailer(attrs, conn);
		}
	}
	
	public CoopAdsEmailer(Map<String, Object> attrs, Connection conn) {
		log = Logger.getLogger(getClass());
		this.attributes = attrs;
		this.dbConn = conn;
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
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
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
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("educationalseminars@dpyus.jnj.com");
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
		String eventType = StringUtil.checkVal(sem.getEvents().get(0).getEventTypeCd());
		boolean isCFSEM = ( eventType.toUpperCase().startsWith("CFSEM") );
		
		StringBuilder msg = new StringBuilder(500);
		msg.append("The seminar coordinator has chosen their Newspaper Ad Options and they are listed below:\r\r\r");
		msg.append(vo.getOptionFeedbackText()).append("\r\r\r");
		msg.append("Harmony, please upload the chosen Newspaper Ad Details to the portal along with the final Ad Files ");
		msg.append("and change status to \"Pending Coordinator Approval\".\r\r");
		msg.append("Thank You,\rEvents.DePuySynthes.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (! isCFSEM ){ //Additional recipients for DePuy Funded events
				if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
					mail.addCC("Francisco.Gonzalez@umj3.com");
					mail.addCC("Evan.Pring@umj3.com");
					mail.addCC("lisav@metrosn.com");
				} else {
					mail.addCC("lisa.maiers@novusmediainc.com");
					mail.addCC("anna.schwanz@novusmediainc.com");
					mail.addCC("taylor.larson@novusmediainc.com");
				}
			}
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
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("educationalseminars@dpyus.jnj.com");
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

	/**
	 * Helper method that returns a new date, set x number of business days 
	 * away from the current date.
	 * @param addDaysToToday Number of days to add to the current date.
	 * @return
	 */
	protected Date addBusinessDays(int addDaysToToday) {
		Calendar cal = Calendar.getInstance();
		int daysToAdd = 0; //Actual applied
        
        //if it's the weekend right now, move to Monday
		if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY )
        	cal.add(Calendar.DATE, 2);
		else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			cal.add(Calendar.DATE, 1);
		
		//Today. Offset by 1 so that Monday is the beginning of the week instead of Sunday
		int initial = cal.get(Calendar.DAY_OF_WEEK) - 1;
		for ( int i = initial; i < addDaysToToday+initial; i++){ //for each day to be added
			if ( i%5 == 0 ) //If we're adding on a Friday, increase the increment to 3, so we skip weekends
				daysToAdd+=3;
			else
				daysToAdd++;
			}

		//increment Calendar date with new number
		cal.add(Calendar.DATE, daysToAdd);

		return cal.getTime();
	}
	
	public void requestAdApprovalOfConsignee(DePuyEventSeminarVO sem, SiteVO site, boolean isHospital) {
		EventEntryVO event = sem.getEvents().get(0);
		ConsigneeVO consignee = sem.getConsignees().get((isHospital ? Long.valueOf(2) : Long.valueOf(1)));
		if (consignee == null) consignee = new ConsigneeVO();
		DePuyEventSurgeonVO surgeon = sem.getSurgeon();
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
		
		msg.append("<p>Ad approval and payment must be received by ");
		msg.append(Convert.formatDate(approvalDt, Convert.DATE_LONG));
		msg.append(" or the seminar will be canceled.</p>");
		msg.append("<p><a href=\"http://").append(site.getSiteAlias());
		msg.append("/approve-ad?reqType=coopAdsReview&amp;eventPostcardId=");
		msg.append(sem.getEventPostcardId());
		if (isHospital) msg.append("&isHosp=1");
		msg.append("\">Click here to review &amp; approve this ad.</a></p>");
		msg.append("<p>Regards,<br/><i>DePuy Synthes Joint Reconstruction</i></p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			if (consignee.getEmail() != null) mail.addRecipient(consignee.getEmail());
			mail.addBCC(site.getAdminEmail());
			mail.addBCC("kshull@ITS.JNJ.com");
			mail.addBCC("educationalseminars@dpyus.jnj.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			
			//add the surgeon's secondary contact, if they have one
			if (surgeon != null && StringUtil.isValidEmail(surgeon.getSecEmail()))
				mail.addCC(surgeon.getSecEmail());
			
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
				mail.addRecipient("amy.spencerman@hmktgroup.com");
				mail.addCC("Sterling.Hoham@hmktgroup.com");
				mail.addCC(sem.getOwner().getEmailAddress());
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
			} else {
				//CFSEM recipients
				mail.addRecipient("amy.spencerman@hmktgroup.com");
				mail.addCC(site.getAdminEmail());
				mail.addCC("kshull@ITS.JNJ.com");
				mail.addCC("Sterling.Hoham@hmktgroup.com");
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
		ConsigneeVO consignee = sem.getConsignees().get((isHospital ? Long.valueOf(2) : Long.valueOf(1)));
		if (consignee == null) consignee = new ConsigneeVO();
		int nCnt=0, oCnt=0, statusFlg=0;
		boolean appr = false, haveUnapproved = false;
		String subject;
		StringBuilder msg = new StringBuilder(1000);
		msg.append("<p>").append(consignee.getContactName());
		msg.append(" has reviewed the ad(s)/cost for Seminar #");
		msg.append(sem.getRSVPCodes()).append(" with the following results:</p>");
		
		for (CoopAdVO vo : sem.getAllAds()) {
			if (isHospital) {
				statusFlg = Convert.formatInteger(vo.getHospitalStatusFlg()).intValue();
				appr = (statusFlg == CoopAdsActionV2.HOSP_APPROVED_AD);
			} else {
				statusFlg = Convert.formatInteger(vo.getSurgeonStatusFlg()).intValue();
				appr = (statusFlg == CoopAdsActionV2.SURG_APPROVED_AD);
			}
//			log.debug("sts=" + statusFlg + ", appr=" + appr);
//			log.debug(StringUtil.getToString(vo));
			
			if (vo.getOnlineFlg() > 0) {
				msg.append("Online Ad #").append(++oCnt);
			} else {
				msg.append("Newspaper Ad #").append(++nCnt);
			}
			
			msg.append(": ").append((appr) ? "Approved" : "Not Approved").append("<br/>");
			if (!appr) {
				msg.append("<div style='padding:2px 10px 10px'>Reason: <i>").append(vo.getInstructionsText()).append("</i></div><br/>");
				haveUnapproved = true;
			}
		}
		msg.append("<p>&nbsp;</p>");
		
		if (!haveUnapproved) {
			subject = "Ad(s) approved by " + (isHospital ? "Hospital" : "Speaker") + " for Seminar #" + sem.getRSVPCodes();
			msg.append("<p>Harmony, the speaker has approved all ads for Seminar ").append(sem.getRSVPCodes());
			msg.append(" and is in the process of making their payment.  Once you confirm that you've received payment ");
			msg.append("please change status in the portal to \"Payment Received\" for each ad.</p>");
		} else {
			subject = "Ad Changes Requested by Speaker for Seminar #" + sem.getRSVPCodes();
			msg.append("<p>Harmony, please use the speaker's comments to address any unapproved ads. ");
			msg.append("The link below will take you back to the portal when you're ready to post changes.</p>");
			String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
			msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
		}
		msg.append("<p>Thank You,<br/>Events.DePuySynthes.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject(subject);
			mail.setFrom(site.getMainEmail());
			mail.setHtmlBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad Approved Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad Approved", me);
		}
	}


	/**
	 * this use-case only triggered for co-funded (CFSEM), once a surgeon has
	 * paid for their Ad.
	 * 
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfAdPaymentRecd(DePuyEventSeminarVO sem, SiteVO site,
			UserDataVO user, boolean isHospital) {
		ConsigneeVO consignee = sem.getConsignees().get((isHospital ? Long.valueOf(2) : Long.valueOf(1)));
		if (consignee == null) consignee = new ConsigneeVO();
		int nCnt=0, oCnt=0, statusFlg=0;
		boolean appr = false;//, haveUnapproved = false;
		
		StringBuilder msg = new StringBuilder(1000);
		msg.append("<p>").append(consignee.getContactName());
		msg.append(" has reviewed the ad(s)/cost for Seminar #");
		msg.append(sem.getRSVPCodes()).append(" with the following results:</p>");
		
		for (CoopAdVO vo : sem.getAllAds()) {
			if (isHospital) {
				statusFlg = Convert.formatInteger(vo.getHospitalStatusFlg()).intValue();
				appr = (statusFlg == CoopAdsActionV2.HOSP_APPROVED_AD);
			} else {
				statusFlg = Convert.formatInteger(vo.getSurgeonStatusFlg()).intValue();
				appr = (statusFlg == CoopAdsActionV2.SURG_APPROVED_AD ||  statusFlg == CoopAdsActionV2.SURG_PAID_AD);
			}
			log.debug("sts=" + statusFlg + ", appr=" + appr);
			log.debug(StringUtil.getToString(vo));
			
			if (vo.getOnlineFlg() > 0) {
				msg.append("Online Ad #").append(++oCnt);
			} else {
				msg.append("Newspaper Ad #").append(++nCnt);
			}
			
			msg.append(": ").append((appr) ? "Approved" : "Not Approved").append("<br/>");
			if (!appr) {
				msg.append("<div style='padding:2px 10px 10px'>Reason: <i>").append(vo.getInstructionsText()).append("</i></div><br/>");
				//haveUnapproved = true;
			}
		}
		msg.append("<p>Novus, please move forward with the newspaper ad purchases.</p>");
		msg.append("<p>Thank You,<br/>Events.DePuySynthes.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
			
			mail.setSubject("Payment Received from Speaker for Seminar #" + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setHtmlBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad payment rcvd Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad payment rcvd", me);
		}
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
			mail.addRecipient("amy.spencerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("kshull@ITS.JNJ.com");
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
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
			
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("kshull@ITS.JNJ.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("educationalseminars@dpyus.jnj.com");
			mail.addCC("amy.spencerman@hmktgroup.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			if (sem.getEarliestEventDate().after(Convert.formatDate(Convert.DATE_SLASH_PATTERN,"01/01/2016"))) {
				mail.addCC("Francisco.Gonzalez@umj3.com");
				mail.addCC("Evan.Pring@umj3.com");
				mail.addCC("lisav@metrosn.com");
			} else {
				mail.addCC("lisa.maiers@novusmediainc.com");
				mail.addCC("anna.schwanz@novusmediainc.com");
				mail.addCC("taylor.larson@novusmediainc.com");
			}
			
			MessageSender sender = new MessageSender(attributes,dbConn);
			sender.sendMessage(mail);
			log.debug("Notify Ad Placement Sent");
			
		} catch (Exception e){
			log.error("Ad Placement Notification",e);
		}
	}
}