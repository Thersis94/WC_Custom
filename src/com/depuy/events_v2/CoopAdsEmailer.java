package com.depuy.events_v2;

import java.util.Calendar;
import java.util.Date;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
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
public class CoopAdsEmailer extends SBActionAdapter {

	public CoopAdsEmailer() {
		super();
	}

	public CoopAdsEmailer(ActionInitVO arg0) {
		super(arg0);
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
			mail.addCC("rwilkin7@its.jnj.com");
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

	public void requestCoordinatorApproval(DePuyEventSeminarVO sem, SiteVO site, int cnt, CoopAdVO vo) {
		//Allow 5 business days for response
		Date dueDate = addBusinessDays(5);
		boolean isOnline = Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1;
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear Seminar Holder,\r\r");
		msg.append("The cost and proof for ");
		if (isOnline) msg.append("Online ");
		msg.append("Ad #").append(cnt).append(" for Seminar ").append(sem.getRSVPCodes());
		msg.append(" is now ready for your approval.  Please visit ");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=").append(sem.getEventPostcardId()).append(" before ");
		msg.append(Convert.formatDate(dueDate)).append(" to either accept or reject this offer.  ");
		msg.append("If you fail to accept or reject by the given date you will ");
		msg.append("automatically accept the offer and your territory will be billed for the expense.\r\r");
		msg.append("Thank You,\rEvents.DePuySynthes.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC(site.getAdminEmail());
			mail.setSubject(((isOnline) ? "Online" : "Newspaper") + " Ad #" + cnt + " approval required - Seminar " + sem.getRSVPCodes());
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
	
	/**
	 * Helper method that returns a new date, set x number of business days 
	 * away from the current date.
	 * @param addDaysToToday Number of days to add to the current date.
	 * @return
	 */
	private Date addBusinessDays(int addDaysToToday){
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
	
	public void requestAdApprovalOfSurgeon(DePuyEventSeminarVO sem, SiteVO site) {
		EventEntryVO event = sem.getEvents().get(0);
		DePuyEventSurgeonVO surg  = sem.getSurgeon();
		Date approvalDt = addBusinessDays(5);
		Date paymentDt = addBusinessDays(6);

		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(surg.getSurgeonName()).append(",");
		msg.append("<p>The promotional ad(s) for the ").append(sem.getJointLabel());
		msg.append(" patient education seminar scheduled on <i>");
		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("</i> at <i>").append(event.getEventName())
				.append("</i> are ready for your approval.</p>");
		msg.append("<p>Please click the link below to review the ad(s), ");
		msg.append("and your portion of the costs.</p>");
		msg.append("<p>Approval is required by ").append(
				Convert.formatDate(approvalDt, Convert.DATE_LONG));
		msg.append(".<br/>Once you approve, a credit card processing system (managed ");
		msg.append("by Harmony Marketing Group, our third party agency) will appear ");
		msg.append("with step by step instructions on providing payment for your ");
		msg.append("portion of the advertising/seminar expenses. Payment information ");
		msg.append("must be received by ").append(Convert.formatDate(paymentDt, Convert.DATE_LONG));
		msg.append(" or the seminar will be cancelled.</p>");
		msg.append("<p><a href=\"http://").append(site.getSiteAlias())
				.append("/approve-ad")
				.append("?reqType=coopAdsReview&amp;eventPostcardId=")
				.append(sem.getEventPostcardId());
		msg.append("\">Click here to review &amp; approve this ad.</a></p>");
		msg.append("<p>Regards,<br/><i>DePuy Synthes Joint Reconstruction</i></p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(surg.getPractEmail());
			if (StringUtil.isValidEmail(surg.getSecEmail())) mail.addRecipient(surg.getSecEmail());
			mail.addBCC(site.getAdminEmail());
			mail.addBCC("rwilkin7@its.jnj.com");
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
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			
			if (! isCFSEM ){ //Additional recipients for DePuy Funded events
				mail.addRecipient("lisa.maiers@novusmediainc.com");
				mail.addCC(sem.getOwner().getEmailAddress());
				mail.addCC("nicole.olson@novusmediainc.com");
				mail.addCC("carly.lubert@novusmeidainc.com");
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
	public void notifyAdminOfSurgeonsApproval(DePuyEventSeminarVO sem, SiteVO site) {
		DePuyEventSurgeonVO surg  = sem.getSurgeon();
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>").append(surg.getSurgeonName());
		msg.append(" has reviewed the ad(s)/cost and provided payment for Seminar #");
		msg.append(sem.getRSVPCodes()).append(" with the following results:</p>");
		int nCnt=0, oCnt=0;
		boolean appr = false;
		boolean haveUnapproved = false;
		for (CoopAdVO vo : sem.getAllAds()) {
			if (vo.getOnlineFlg() > 0) {
				msg.append("Online Ad #").append(++oCnt);
			} else {
				msg.append("Newspaper Ad #").append(++nCnt);
			}
			appr = (Convert.formatInteger(vo.getStatusFlg()).equals(CoopAdsActionV2.SURG_APPROVED_AD));
			msg.append(": ").append((appr) ? "Approved" : "Not Approved").append("<br/>");
			if (!appr) {
				msg.append("<div style='padding:2px 10px 10px'>Reason: <i>").append(vo.getInstructionsText()).append("</i></div><br/>");
				haveUnapproved = true;
			}
		}
		msg.append("<p>&nbsp;</p>");
		if (!haveUnapproved) {
			msg.append("<p>Harmony, please change the status on the portal to Payment Received ");
			msg.append("by using the link below.</p>");
			String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
			msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
			msg.append("<p>Novus, please move forward with the newspaper ad purchases.</p>");
		} else {
			msg.append("<p>Harmony, please use the surgeon's comments to address any unapproved ads. ");
			msg.append("The link below will take you back to the portal when you're ready to post changes.</p>");
			String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
			msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
		}
		msg.append("<p>Thank You,<br/>Events.DePuySynthes.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addRecipient("lisa.maiers@novusmediainc.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.addCC("nicole.olson@novusmediainc.com");
			mail.addCC("carly.lubert@novusmediainc.com");
			
			mail.setSubject("Ad(s) reviewed by Surgeon for Seminar #" + sem.getRSVPCodes());
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
	 * notification sent when the surgeon views/approves their Ad. (outside of
	 * Events Mgmt system)
	 * 
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfSurgeonsDecline(DePuyEventSeminarVO sem, SiteVO site,
			String notes, int cnt, CoopAdVO vo) {
		String label = (Convert.formatInteger(vo.getOnlineFlg()).intValue() == 1) ? "Online" : "Newspaper";
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>").append(sem.getSurgeon().getSurgeonName());
		msg.append(" has declined ").append(label).append(" Ad #").append(cnt).append(" for Seminar #").append(sem.getRSVPCodes()).append(".</p>");
		msg.append("<p>The surgeon commented:<br/><pre>").append(notes).append("</pre></p>");
		msg.append("<p><br/>If possible, please take the necessary corrective actions and upload a new ad to the website.  ");
		msg.append("Be sure to set the status to \"Pending Client Approval\" when you do.</p>");
		String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
		msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
		msg.append("<p>Thank You,<br/>Events.DePuySynthes.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.setSubject(label + " Ad #" + cnt + " declined by Surgeon for Seminar #" + sem.getRSVPCodes());
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
			UserDataVO user) {
		StringBuilder msg = new StringBuilder();
		msg.append("Harmony has received payment for the newspaper ad and postcard for Seminar #");
		msg.append(sem.getRSVPCodes()).append(".\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.addCC(sem.getOwner().getEmailAddress());
			mail.setSubject("Harmony confirmation for Seminar #" + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad Payment Received Email Sent");
		} catch (Exception me) {
			log.error("Co-Op Ad Payment Received", me);
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
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
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
	protected void notifyAdPlacement( DePuyEventSeminarVO sem, SiteVO site ){
		
		StringBuilder msg = new StringBuilder(140);
		msg.append("Newspaper Advertising for Seminar #").append(sem.getRSVPCodes());
		msg.append(" has been sent and confirmed by the publication(s).\r\r");
		
		try{
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Newspaper Advertising Placement Confirmation - Seminar "+sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("rwilkin7@its.jnj.com");
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("amy.zimmerman@hmktgroup.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			mail.addCC("lisa.maiers@novusmediainc.com");
			mail.addCC("nicole.olson@novusmediainc.com");
			mail.addCC("carly.lubert@novusmediainc.com");
			
			MessageSender sender = new MessageSender(attributes,dbConn);
			sender.sendMessage(mail);
			log.debug("Notify Ad Placement Sent");
			
		} catch (Exception e){
			log.error("Ad Placement Notification",e);
		}
	}
}
