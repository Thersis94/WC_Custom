package com.depuy.events;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events.vo.DePuyEventPostcardVO;
import com.depuy.events.vo.report.PostcardSummaryReportVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: CoopAdsEmailer.java<p/>
 * <b>Description: </b> contains all emails fired from the website 
 * based on action/state changes  (status_flg = ??)
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 14, 2008
 ****************************************************************************/
public class CoopAdsEmailer extends SBActionAdapter {
	
	public CoopAdsEmailer() {
		super();
	}

	public CoopAdsEmailer(ActionInitVO arg0) {
		super(arg0);
	}

		
	/**
	 * sends the site admin an email whenever a user downloads patient data/reports
	 * @param postcard
	 * @param user
	 * @param type
	 * @param site
	 */
	public void notifyAdminOfAdSubmittal(CoopAdVO vo, SiteVO site, UserDataVO user, SMTServletRequest req) {
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has submitted ");
		msg.append("a co-op ad request for seminar(s) ");
		msg.append(vo.getEventCodes()).append("\r\r");
		
		//get the postcard data to generate the summary report
		AbstractSBReportVO rpt = new PostcardSummaryReportVO();
		try {
			SMTActionInterface ai = new PostcardSelect(actionInit);
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
			ai.retrieve(req);
			List<?> data = (List<?>) req.getAttribute(PostcardSelect.RETR_EVENTS);
			DePuyEventPostcardVO eventVo = (DePuyEventPostcardVO) data.get(0);
			eventVo.setCoopAd(vo);
			rpt.setData(eventVo);
		} catch (ActionException ae) {
			log.error("can't load postcard data for emailed report", ae);
		}
	
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
    		mail.setSubject("Co-Op Ad request for Seminar(s) " + vo.getEventCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		mail.addAttachment("Postcard Summary.xls", rpt.generateReport());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("Co-Op Ad request Email Sent");
    		
    	} catch (Exception me) {
    		log.error("Co-Op Ad request", me);
    	}
	}
	
	
	public void notifyAdminOfAdDeclined(CoopAdVO vo, SiteVO site, UserDataVO user) {
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has declined ");
		msg.append("the co-op ad offered for seminar(s) ");
		msg.append(vo.getEventCodes()).append("\r\r");
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
    		mail.setSubject("Co-Op Ad Declined for Seminar(s) " + vo.getEventCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("Co-Op Ad declined Email Sent");
    		
    	} catch (Exception me) {
    		log.error("Co-Op Ad declined", me);
    	}
	}
	
	
	public void requestClientApproval(CoopAdVO vo, SiteVO site, String rcptEmail) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 4);  //give them 4 days from today to approve the Ad
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear Seminar Holder,\r\r");
		msg.append("The cost and proof for your ad for seminar ").append(vo.getEventCodes());
		msg.append(" is now ready for your approval.  Please visit http://").append(site.getSiteAlias()).append("/ before ");
		msg.append(Convert.formatDate(cal.getTime())).append(" to either accept or reject this offer.  ");
		msg.append("If you fail to accept or reject by the given date you will ");
		msg.append("automatically accept the offer and your territory will be billed for the expense.\r\r");
		msg.append("Thank You,\rEvents.depuy.com Administrator\r\r");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rcptEmail);
	    		mail.setSubject("Co-Op Ad approval required for Seminar(s) " + vo.getEventCodes());
	    		mail.setFrom(site.getMainEmail());
	    		mail.setTextBody(msg.toString());
	    		
	    		MessageSender ms = new MessageSender(attributes, dbConn);
	    		ms.sendMessage(mail);
	    		
	    		log.debug("Co-Op Ad requestClientApproval Email Sent");
	    	} catch (Exception me) {
	    		log.error("Co-Op Ad requestClientApproval", me);
	    	}
	}
	
	
	public void requestAdApprovalOfSurgeon(CoopAdVO vo, SiteVO site, String coordinatorsEmail, String productNm, String eventNm, String strEventDt) {
		Date eventDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, strEventDt);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 5);  //give them 5 days from today to approve the Ad
		//do not allow the deadline to fall on a weekend
		if (cal.get(Calendar.DAY_OF_WEEK) == 7) cal.add(Calendar.DATE, 2); //Saturday
		else if (cal.get(Calendar.DAY_OF_WEEK) == 1) cal.add(Calendar.DATE, 1); //Sunday
		
		Calendar cal2 = Calendar.getInstance();
		cal2.add(Calendar.DATE, 6);  //give them 6 days from today to submit payment
		//do not allow the deadline to fall on a weekend
		if (cal2.get(Calendar.DAY_OF_WEEK) == 7) cal2.add(Calendar.DATE, 2); //Saturday
		else if (cal2.get(Calendar.DAY_OF_WEEK) == 1) cal2.add(Calendar.DATE, 1); //Sunday
		
		String surgeonName = (vo.getSurgeonName().toLowerCase().startsWith("dr") ? "" : "Dr. ") + vo.getSurgeonName();
				
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(surgeonName).append(",");
		msg.append("<p>The newspaper ad for the ").append(productNm);
		msg.append(" patient education seminar scheduled on <i>");
		msg.append(Convert.formatDate(eventDt, Convert.DATE_LONG));
		msg.append("</i> at <i>").append(eventNm).append("</i> is ready for your approval.</p>");
		msg.append("<p>Please click the link below to view and approve the newspaper ad ");
		msg.append("and your portion of the cost for the ad");
		if (!"SHOULDER".equalsIgnoreCase(productNm)) msg.append(" and postcards.  The cost of the postcards is $90");
		msg.append(". Approval is required by ").append(Convert.formatDate(cal.getTime(), Convert.DATE_LONG));
		msg.append(". Approval and payment information (credit card information given to our third party agency) ");
		msg.append("must be received by ").append(Convert.formatDate(cal2.getTime(), Convert.DATE_LONG));
		msg.append(" or the seminar will be cancelled.</p>");
		msg.append("<p><a href=\"http://").append(site.getSiteAlias()).append("/approve-").append(productNm).append("?reqType=coopAdsReview&amp;eventPostcardId=").append(vo.getEventPostcardId());
		msg.append("\">Click here to review &amp; approve this ad.</a></p>");
		msg.append("<p>Regards,<br/><i>DePuy Synthes Joint Reconstruction</i></p><br/>");

		String[] surgeonEmails = new String[] { vo.getSurgeonEmail() };
		if (vo.getSurgeonEmail().contains(","))
			surgeonEmails = vo.getSurgeonEmail().split(",");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(surgeonEmails);  //surgeonEmail could also include the Office Manager
	    		mail.addCC(site.getAdminEmail());
	    		mail.addCC(coordinatorsEmail);
	    		mail.addCC("kelly.westafer@hmktgroup.com");
	    		mail.addCC("amy.zimmerman@hmktgroup.com");
	    		mail.addCC("admgt@hmktgroup.com");
	    		mail.setSubject("Approval Required: Promotion for " + productNm + " seminar on " + Convert.formatDate(eventDt, Convert.DATE_SLASH_PATTERN));
	    		mail.setFrom(site.getMainEmail(), "DePuy Synthes Joint Reconstruction patient education seminars");
	    		mail.setHtmlBody(msg.toString());
	
	    		MessageSender ms = new MessageSender(attributes, dbConn);
	    		ms.sendMessage(mail);    		
	    		log.debug("Co-Op Ad requestClientApprovalOfSurgeon Email Sent");
	    	} catch (Exception me) {
	    		log.error("Co-Op Ad requestClientApprovalOfSurgeon", me);
	    	}
	}
	
	
	public void notifyAdminOfAdApproval(CoopAdVO vo, SiteVO site, UserDataVO user) {
		StringBuilder msg = new StringBuilder();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has approved ");
		msg.append("the co-op ad for seminar(s) ");
		msg.append(vo.getEventCodes()).append("\r\r");
		if (StringUtil.checkVal(vo.getSurgeonEmail()).length() > 0 && vo.getSurgeonStatusFlg() == 0) {
			msg.append("The surgeon has yet to review this ad.\r\n");
		}
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.addRecipient("dfox@hmktgroup.com");
			mail.addRecipient("barb.goley@hmktgroup.com");
			mail.addRecipient("kelly.westafer@hmktgroup.com");
	    		mail.addRecipient("amy.zimmerman@hmktgroup.com");
	    		mail.setSubject("Co-Op Ad Approved for Seminar(s) " + vo.getEventCodes());
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
	 * notification sent when the surgeon views/approves their Ad. (outside of Events Mgmt system)
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfSurgeonsApproval(CoopAdVO vo, SiteVO site) {
		String surgeonName = (vo.getSurgeonName().toLowerCase().startsWith("dr") ? "" : "Dr. ") + vo.getSurgeonName();
		StringBuilder msg = new StringBuilder();
		msg.append("<p>").append(surgeonName);
		msg.append(" has approved the newspaper ad and cost for Event ").append(vo.getEventCodes()).append("</p>");
		msg.append("<p>Once you've collected payment, please enter that information ");
		msg.append("on the website and change the status to Payment Received.</p>");
		msg.append("<p>Thank You,<br/>Events.depuy.com Administrator</p><br/>");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("kelly.westafer@hmktgroup.com");
			mail.addRecipient("admgt@hmktgroup.com");
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.setSubject("Surgeon approval of newspaper ad and cost for " + vo.getEventCodes());
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
	 * notification sent when the surgeon views/approves their Ad. (outside of Events Mgmt system)
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfSurgeonsDecline(CoopAdVO vo, SiteVO site, String notes) {
		String surgeonName = (vo.getSurgeonName().toLowerCase().startsWith("dr") ? "" : "Dr. ") + vo.getSurgeonName();
		StringBuilder msg = new StringBuilder();
		msg.append("<p>").append(surgeonName);
		msg.append(" has declined the newspaper ad for Event ").append(vo.getEventCodes()).append(".</p>");
		msg.append("<p>The surgeon commented:<br/><pre>").append(notes).append("</pre></p>");
		msg.append("<p><br/>If possible, please take the necessary corrective actions and upload a new ad to the website.  ");
		msg.append("Be sure to set the status to \"Pending Client Approval\" when you do.</p>");
		msg.append("<p>Thank You,<br/>Events.depuy.com Administrator</p><br/>");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient("kelly.westafer@hmktgroup.com");
			mail.addRecipient("admgt@hmktgroup.com");
			mail.addRecipient("amy.zimmerman@hmktgroup.com");
			mail.addCC(site.getAdminEmail());
			mail.setSubject("Surgeon approval of newspaper ad and cost for " + vo.getEventCodes());
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
	 * this use-case only triggered for co-funded (CFSEM), once a surgeon has paid for their Ad.
	 * @param vo
	 * @param site
	 * @param user
	 */
	public void notifyAdminOfAdPaymentRecd(CoopAdVO vo, SiteVO site, UserDataVO user) {
		StringBuilder msg = new StringBuilder();
		msg.append("Harmony has received payment for the newspaper ad and postcard for seminar ");
		msg.append(vo.getEventCodes()).append(".\r\r");
		
		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
			mail.setSubject("Harmony confirmation for seminar(s) " + vo.getEventCodes());
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
