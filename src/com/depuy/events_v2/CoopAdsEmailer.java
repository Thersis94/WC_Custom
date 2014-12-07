package com.depuy.events_v2;

import java.util.Calendar;
import java.util.Date;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

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

	public void notifyAdminOfAdDeclined(DePuyEventSeminarVO sem, SiteVO site, UserDataVO user) {
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has declined ");
		msg.append("the newspaper ad offered for Seminar ");
		msg.append(sem.getRSVPCodes()).append("\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.setSubject("Newspaper Ad declined for Seminar " + sem.getRSVPCodes());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			log.debug("Co-Op Ad declined Email Sent");

		} catch (Exception me) {
			log.error("Co-Op Ad declined", me);
		}
	}

	public void requestCoordinatorApproval(DePuyEventSeminarVO sem, SiteVO site) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 4); // give them 4 days from today to approve the Ad
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear Seminar Holder,\r\r");
		msg.append("The cost and proof for your ad for Seminar ").append(sem.getRSVPCodes());
		msg.append(" is now ready for your approval.  Please visit ");
		msg.append(site.getFullSiteAlias()).append("/?reqType=promote&eventPostcardId=").append(sem.getEventPostcardId()).append(" before ");
		msg.append(Convert.formatDate(cal.getTime())).append(" to either accept or reject this offer.  ");
		msg.append("If you fail to accept or reject by the given date you will ");
		msg.append("automatically accept the offer and your territory will be billed for the expense.\r\r");
		msg.append("Thank You,\rEvents.depuy.com Administrator\r\r");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(sem.getOwner().getEmailAddress());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC(site.getAdminEmail());
			mail.setSubject("Newspaper Ad approval required - Seminar " + sem.getRSVPCodes());
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
		msg.append("<p>The newspaper ad for the ").append(sem.getJointLabel());
		msg.append(" patient education seminar scheduled on <i>");
		msg.append(Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		msg.append("</i> at <i>").append(event.getEventName())
				.append("</i> is ready for your approval.</p>");
		msg.append("<p>Please click the link below to view and approve the newspaper ad ");
		msg.append("and your portion of the cost for the ad");
//		if (!"shoulder".equalsIgnoreCase(sem.getJointLabel()))
//			msg.append(" and postcards.  The cost of the postcards is $90");
		msg.append(". Approval is required by ").append(
				Convert.formatDate(approvalDt, Convert.DATE_LONG));
		msg.append(". Approval and payment information (credit card information given to our third party agency) ");
		msg.append("must be received by ").append(
				Convert.formatDate(paymentDt, Convert.DATE_LONG));
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
			UserDataVO user) {
		//Determine if it's co-funded or DePuy funded (used for subject line and recipients)
		String eventType = StringUtil.checkVal(sem.getEvents().get(0).getEventTypeCd());
		boolean isCFSEM = ( eventType.toUpperCase().startsWith("CFSEM") );
		
		//Build the subject text
		StringBuilder subject = new StringBuilder();
		subject.append("Newspaper Ad approved by Coordinator for ");
		subject.append( (isCFSEM ? "Co-Funded" : "DePuy Funded") );
		subject.append(" Seminar #").append(sem.getRSVPCodes());
		
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has approved ");
		msg.append("the newspaper ad for Seminar #").append(sem.getRSVPCodes()).append("\r\r");
		if (sem.getNewspaperAd().getSurgeonStatusFlg() == 0)
			msg.append("The surgeon has yet to review this ad.\r\n");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			//mail.addRecipient("becca.burton@hmktgroup.com");
			//mail.addRecipient("barb.goley@hmktgroup.com");
			// mail.addRecipient("kelly.westafer@hmktgroup.com");
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
		msg.append(" has approved the newspaper ad and cost for Seminar #");
		msg.append(sem.getRSVPCodes()).append("</p>");
		msg.append("<p>Once you've collected payment, please enter that information ");
		msg.append("on the website and change the status to Payment Received.</p>");
		String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
		msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
		msg.append("<p>Thank You,<br/>Events.depuy.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			//mail.addRecipient("admgt@hmktgroup.com");
			//mail.addRecipient("rita.harman@hmktgroup.com");
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.addCC("Sterling.Hoham@hmktgroup.com");
			
			mail.setSubject("Newspaper Ad approved by Surgeon for Seminar #" + sem.getRSVPCodes());
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
			String notes) {
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>").append(sem.getSurgeon().getSurgeonName());
		msg.append(" has declined the newspaper ad for Seminar #")
				.append(sem.getRSVPCodes()).append(".</p>");
		msg.append("<p>The surgeon commented:<br/><pre>").append(notes)
				.append("</pre></p>");
		msg.append("<p><br/>If possible, please take the necessary corrective actions and upload a new ad to the website.  ");
		msg.append("Be sure to set the status to \"Pending Client Approval\" when you do.</p>");
		String url = site.getFullSiteAlias() + "/?reqType=promote&eventPostcardId=" + sem.getEventPostcardId();
		msg.append("<p><a href=\"").append(url).append("\">").append(url).append("</a></p>");
		msg.append("<p>Thank You,<br/>Events.depuy.com Administrator</p><br/>");

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			// mail.addRecipient("kelly.westafer@hmktgroup.com");
			//mail.addRecipient("admgt@hmktgroup.com");
			//mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addRecipient(site.getAdminEmail());
			mail.addCC("rwilkin7@its.jnj.com");
			mail.setSubject("Newspaper Ad declined by Surgeon for Seminar #" + sem.getRSVPCodes());
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

}
