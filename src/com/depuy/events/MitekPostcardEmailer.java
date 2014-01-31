package com.depuy.events;

// JDK Libs
import java.sql.Connection;
import java.util.Map;
import java.util.StringTokenizer;

// WC Libs
import com.depuy.events.vo.DePuyEventPostcardVO;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: MitekPostcardEmailer.java<p/>
 * <b>Description: </b> responsible for outgoing emails
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 22, 2012
 ****************************************************************************/
public class MitekPostcardEmailer extends AbstractPostcardEmailer {
	
	/**
	 * @param attrs
	 */
	public MitekPostcardEmailer(Map<String, Object> attrs, Connection conn) {
		super(attrs, conn);
	}


	/**
	 * sends the site admin an email whenever a user downloads patient data/reports
	 * @param postcard
	 * @param user
	 * @param type
	 * @param site
	 */
	public void notifyAdminOfListPull(DePuyEventPostcardVO postcard, UserDataVO user, int type, SiteVO site) {
		AbstractPostcardEmailer ape = new DePuyPostcardEmailer(super.attributes, dbConn);
		ape.notifyAdminOfListPull(postcard, user, type, site);
	}
	
	
	/**
	 * sends event approval request to the site administrator
	 * @param req
	 */
	public void sendApprovalRequest(SMTServletRequest req) {
		AbstractPostcardEmailer ape = new DePuyPostcardEmailer(super.attributes, dbConn);
		ape.sendApprovalRequest(req);
	}
	
	/**
	 * sends event owner notification their event/postcard was approved
	 * @param req
	 */
	public void sendApprovedResponse(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String rcptEmail = site.getAdminEmail();
		DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) req.getAttribute("postcard");
		try {
			if (postcard != null && postcard.getOwner() != null) 
				rcptEmail = postcard.getOwner().getEmailAddress();
		} catch (Exception e) {
			 //ignore failure, just send the email to the site admin instead
		}
		
		StringBuffer msg = new StringBuffer();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" has approved your ").append(postcard.getProductName());
		msg.append(" Community Education Postcard and Seminar(s).\r\r");
		msg.append("Please visit our website to access valuable materials that ");
		msg.append("can help you organize and manage your seminar(s).  ");
		msg.append("You will recieve a series of emails that will guide you through ");
		msg.append("the event-hosting process as the seminar(s) approach.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rcptEmail);
    		mail.setSubject("ORTHOVISC Community Education Seminar(s) Approved");
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("EventPostcardApproved Email Sent");
    	} catch (Exception me) {
    		log.error("EventPostcardApprovedEmail", me);
    	}
    	
    	return;
	}
	
	/**
	 * send vendor and DePuy PMs an email with postcard info/layout and critical dates
	 * @param req
	 * @param eventPostcardId
	 */
	public void sendVendorSummary(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) req.getAttribute("postcard");
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		
		StringBuffer msg = new StringBuffer();
		msg.append("A new ").append(postcard.getProductName());
		msg.append(" postcard has been scheduled for mailing.  ");
		msg.append("Please use the information attached as a reference ");
		msg.append("and visit the Community Education website for complete, ");
		msg.append("up-to-date information and list pulls.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		
		//get the recipients from the config file
		StringTokenizer st = new StringTokenizer((String)getAttribute("mitekEventAnnounceEmails"),",");
		String[] emails = new String[st.countTokens()];
		try {
			for (int i=0; st.hasMoreTokens(); i++) {
				emails[i] = st.nextToken();
				log.debug("rcpts[" + i + "]=" + emails[i]);
			}
		} catch (Exception e) {
			log.error("error getting configFile addresses", e);
		}
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(emails);
    		mail.setSubject("ORTHOVISC Community Education; New Postcard Announcement " + postcard.getRSVPCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		mail.addAttachment(rpt.getFileName(), rpt.generateReport()); //add the Excel report to the email
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("EventPostcardVendorSummary Email Sent");
    	} catch (Exception me) {
    		log.error("EventPostcardVendorSummary Email", me);
    	}
    	
    	log.debug("done sending vendor summary");
		return;
	}
	
	
	/**
	 * send DePuy TGMs and sales rep an email with postcard info and authorization approvals
	 * @param req
	 */
	public void sendPreAuthPaperwork(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) req.getAttribute("postcard");
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		
		StringBuffer msg = new StringBuffer();
		msg.append(postcard.getProductName().toUpperCase()).append(" Seminar(s) ").append(postcard.getRSVPCodes());
		msg.append(" submitted by ").append(postcard.getOwner().getFirstName());
		msg.append(" ").append(postcard.getOwner().getLastName());
		msg.append(" has been approved by DePuy Mitek. It will be reviewed by SRC ");
		msg.append("within the next two weeks. Enclosed please find the pre-authorization ");
		msg.append("form which has been read and signed by the seminar organizer ");
		msg.append("and the surgeon speaker.");
		
		//get the recipients from the TGM field
		StringTokenizer st = new StringTokenizer(postcard.getPcAttribute1(),",");
		String[] emails = new String[st.countTokens()+1];
		int i=0;
		try {
			for (; st.hasMoreTokens(); i++) {
				emails[i] = st.nextToken();
				log.debug("rcpts[" + i + "]=" + emails[i]);
			}
		} catch (Exception e) {
			log.error("error getting configFile addresses", e);
		}
		emails[i] = postcard.getPcAttribute2();

		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(emails);
    		mail.setSubject("ORTHOVISC Community Education; New Postcard Announcement " + postcard.getRSVPCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		mail.addAttachment(rpt.getFileName(), rpt.generateReport()); //add the Excel report to the email
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("sendPreAuthPaperwork Email Sent");
    	} catch (Exception me) {
    		log.error("sendPreAuthPaperwork Email", me);
    	}
    	
    	log.debug("done sending vendor summary");
		return;
	}
	
	
	/**
	 * send certain admins a notice that a postcard was canceled
	 * @param req
	 * @param eventPostcardId
	 */
	public void sendPostcardCancellation(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) req.getAttribute("postcard");
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		
		StringBuffer msg = new StringBuffer();
		msg.append("The attached ").append(postcard.getProductName());
		msg.append(" postcard has been canceled.  ");
		msg.append("Please use the information attached as a reference ");
		msg.append("and visit the Community Education website for complete, ");
		msg.append("up-to-date information.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		
		//get the recipients from the config file
		String[] emails = new String[4];
		emails[0] = site.getAdminEmail();  //the DePuy intern in charge
		emails[1] = "dfox@hmktgroup.com"; //Debra Fox
		emails[2] = "admgt@hmktgroup.com"; //Barb Goley & Shari Slough
		emails[3] = "Jenn.Davis@HmktGroup.com"; //Jenn Parrish-Davis
		//emails[5] = "mgibson2@its.jnj.com"; //Monika Gibson 
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(emails);
    		mail.setSubject("ORTHOVISC Community Education; Postcard Canceled " + postcard.getRSVPCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		mail.addAttachment(rpt.getFileName(), rpt.generateReport()); //add the Excel report to the email
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("EventPostcardCanceled Email Sent");
    	} catch (Exception me) {
    		log.error("EventPostcardCanceled Email", me);
    	}
    	
    	log.debug("done sending cancellation email");
		return;
	}

}
