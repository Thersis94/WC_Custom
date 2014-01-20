package com.depuy.events_v2;

//JDK 1.5.0
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.depuy.events.AbstractPostcardEmailer;
import com.depuy.events.LeadsDataTool;
// SMT BaseLibs
import com.depuy.events.vo.DePuyEventEntryVO;
import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: EventPostcardEmailer.java<p/>
 * <b>Description: </b> pulls the leads data from the patient data 
 * (profile tables) for this postcard/mailing
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 16, 2006
 ****************************************************************************/
public class DePuyPostcardEmailerV2 extends AbstractPostcardEmailer {
			
	/**
	 * @param attrs
	 */
	public DePuyPostcardEmailerV2(Map<String, Object> attrs, Connection conn) {
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
		List<DePuyEventEntryVO> events = postcard.getDePuyEvents();
		Iterator<DePuyEventEntryVO> iter = events.iterator();

		StringBuffer msg = new StringBuffer();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" (").append(user.getEmailAddress()).append(") has pulled ");
		msg.append("leads data for the postcard containing these ");
		msg.append(postcard.getProductName()).append(" seminars:\r");
		
		while (iter.hasNext()) { //append events list to the message body
			DePuyEventEntryVO event = iter.next();
			msg.append(event.getEventName()).append(" (").append(event.getCityName());
			msg.append(", ").append(event.getStateCode()).append(")\r");
		}
		
		if (type == LeadsDataTool.POSTCARD_SUMMARY_PULL) {
			msg.append("\r").append(user.getFirstName());
			msg.append(" downloaded the postcard summary ");
			msg.append("ONLY, not patient data (at this time).\r");
			
		} else if (type == LeadsDataTool.MAILING_LIST_BY_DATE_PULL) {
			msg.append("\r").append(user.getFirstName());
			msg.append(" downloaded the complete spreadsheet containing the names ");
			msg.append("and addresses of the postcard recipients for a follow-up mailing.\r");
			
		} else if (type == LeadsDataTool.EMAIL_ADDRESS_PULL) {
			msg.append("\r").append(user.getFirstName());
			msg.append(" downloaded the email addresses of the postcard recipients (only).\r");
			
		} else {
			msg.append("\r").append(user.getFirstName());
			msg.append(" downloaded the complete spreadsheet containing the names ");
			msg.append("and addresses of the postcard recipients.\r");
		}
		
		msg.append("\rIf this person should not have accessed this report please take ");
		msg.append("action to ensure DePuy's patient data is kept secure.\r");
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
    		mail.addRecipient(site.getAdminEmail());
    		mail.setSubject(postcard.getProductName() + " Community Education data download (" + postcard.getRSVPCodes() + ")");
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("notifyAdminOfListPull Email Sent");
    	} catch (Exception me) {
    		log.error("EventPostcardListPullEmail, type=" + type, me);
    	}
	}
	
	
	/**
	 * sends event approval request to the site administrator
	 * @param req
	 */
	public void sendApprovalRequest(SMTServletRequest req) {
		//send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		StringBuffer subject = new StringBuffer();
		subject.append(req.getParameter("productName")).append(" Community Education Seminar(s) Submitted by ");
		subject.append(user.getFirstName()).append(" ").append(user.getLastName());
		
		StringBuffer msg = new StringBuffer();
		msg.append(user.getFirstName()).append(" ").append(user.getLastName());
		msg.append(" has submitted a new ").append(req.getParameter("productName"));
		msg.append(" postcard request on ");
		msg.append(site.getSiteAlias()).append("\r\rPlease visit the website ");
		msg.append("to review the information provided and approve this request\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");

		//build the attachment
		AbstractSBReportVO rpt = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(site.getAdminEmail());
    		mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		mail.addAttachment(rpt.getFileName(), rpt.generateReport()); //add the Excel report to the email
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		log.debug("EventPostcardSubmit Admin Email Sent");
    	} catch (Exception me) {
    		log.error("EventPostcardSubmitEmail", me);
    	}
    	return;
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
		msg.append(" DePuy Community Education Postcard and Seminar(s).\r\r");
		msg.append("Please visit our website to access valuable materials that ");
		msg.append("can help you organize and manage your seminar(s).  ");
		msg.append("You will recieve a series of emails that will guide you through ");
		msg.append("the event-hosting process as the seminar(s) approach.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rcptEmail);
    		mail.setSubject("DePuy Community Education Seminar(s) Approved");
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
		msg.append("A new DePuy ").append(postcard.getProductName());
		msg.append(" postcard has been scheduled for mailing.  ");
		msg.append("Please use the information attached as a reference ");
		msg.append("and visit the Community Education website for complete, ");
		msg.append("up-to-date information and list pulls.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		
		//get the recipients from the config file
		StringTokenizer st = new StringTokenizer((String)getAttribute("eventAnnounceEmails"),",");
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
    		mail.setSubject("DePuy Community Education; New Postcard Announcement " + postcard.getRSVPCodes());
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
		msg.append("<p>").append(postcard.getProductName().toUpperCase()).append(" Seminar(s) ").append(postcard.getRSVPCodes());
		msg.append(" submitted by ").append(postcard.getOwner().getFirstName());
		msg.append(" ").append(postcard.getOwner().getLastName());
		msg.append(" has been approved by <i>DePuy Synthes Joint Reconstruction</i> Communications Team. ");
		msg.append("It will be reviewed by SRC at their next meeting. ");
		msg.append("Enclosed please find the details of your submission.</p>");
		
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
    		mail.setSubject("DePuy Synthes Joint Reconstruction Community Education; New Postcard Announcement " + postcard.getRSVPCodes());
    		mail.setFrom(site.getMainEmail());
    		mail.setHtmlBody(msg.toString());
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
		msg.append("The attached DePuy ").append(postcard.getProductName());
		msg.append(" postcard has been canceled.  ");
		msg.append("Please use the information attached as a reference ");
		msg.append("and visit the Community Education website for complete, ");
		msg.append("up-to-date information.\r\r");
		msg.append("https://").append(site.getSiteAlias()).append("\r");
		
		//get the recipients from the config file
		String[] emails = new String[6];
		emails[0] = site.getAdminEmail();  //the DePuy intern in charge
		emails[1] = "KAlfano@its.jnj.com"; //Kristi Alfano
		emails[2] = "amy.zimmerman@hmktgroup.com";
		emails[3] = "admgt@hmktgroup.com"; //Barb Goley & Shari Slough
		emails[4] = "Jenn.Davis@hmktgroup.com"; //Jenn Parrish-Davis
		emails[5] = "kelly.westafer@hmktgroup.com"; 
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(emails);
    		mail.setSubject("DePuy Community Education; Postcard Canceled " + postcard.getRSVPCodes());
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
