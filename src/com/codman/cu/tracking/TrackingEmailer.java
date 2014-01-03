package com.codman.cu.tracking;


// SMT BaseLibs

import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.PersonVO;
import com.codman.cu.tracking.vo.PhysicianVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: TrackingEmailer.java<p/>
 * <b>Description: </b> handles all email transactions for the CU Tracking tool
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 10, 2010
 ****************************************************************************/
public class TrackingEmailer extends SBActionAdapter {
	
	/**
	 * @param arg0
	 */
	public TrackingEmailer(ActionInitVO arg0) {
		super(arg0);
	}

		
	/**
	 * Sends email when rep requests a new control unit
	 * @param req
	 * @param vo
	 */
	public void submitRequest(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans, AccountVO acct) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
					
		StringBuffer subject = new StringBuffer();
		subject.append("MedStream Control Unit Request# ").append(trans.getRequestNo());
		
		StringBuffer msg = new StringBuffer();
		msg.append("Dear ").append(site.getAdminName()).append(",\r\n");
		msg.append("There is a request for a MedStream Control Unit pending in your inbox.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(phys.getFirstName()).append(" ").append(phys.getLastName()).append("\r\n\r\n");
		msg.append("Please review and approve this request.\r\n");
		msg.append("http://www.codmanpumps.com/cu");
		msg.append("\r\n\r\nCc: Eva Casamento");
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.addRecipient(site.getAdminEmail());
    		mail.addCC("ecasamen@its.jnj.com");
    		mail.addCC("tflynn@its.jnj.com");
    		mail.setTextBody(msg.toString());
    		    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		
    		log.debug("Request Submit email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("CU Request Submit Email, ", me);
    	}
    	return;
	}
	
	
	/**
	 * sends request approval to the customer service rep and admin
	 * @param req
	 */
	public void approveRequestCS(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans, AccountVO acct) {
		//send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuffer subject = new StringBuffer();
		subject.append("Request for MedStream Control Unit no-charge order: Request# ").append(trans.getRequestNo());
		
		StringBuffer msg = new StringBuffer();
		msg.append("Dear Customer Service Rep,\r\n");
		msg.append("Please ship quantity ").append(trans.getUnitCount());
		msg.append(" of MedStream Control Unit (91-4205US) and quantity ").append(trans.getUnitCount());
		msg.append(" of MedStream Programming Guide (91-4282US) billing $0 to sample account# ");
		msg.append(StringUtil.checkVal(rep.getSampleAccountNo())).append("\r\n\r\n");
		msg.append("Please ship to:\r\n");
		
		//ship to address
		msg.append(trans.getShipToName()).append("\r\n");
		msg.append(trans.getShippingAddress().getAddress()).append("\r\n");
		if (StringUtil.checkVal(trans.getShippingAddress().getAddress2()).length() > 0) 
			msg.append(trans.getShippingAddress().getAddress2()).append("\r\n");
		msg.append(trans.getShippingAddress().getCity()).append(", ");
		msg.append(trans.getShippingAddress().getState()).append("  ");
		msg.append(trans.getShippingAddress().getZipCode()).append(", ");
		msg.append(trans.getShippingAddress().getCountry()).append("\r\n\r\n");
			
		msg.append("Please forward the Order Reference Number to me for my records.\r\n\r\n");
		
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getApprovalDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Requested By: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(trans.getPhysician().getFirstName()).append(" ").append(trans.getPhysician().getLastName()).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append("\r\n\r\n");
		
		msg.append("Thank you,\r\n");
		msg.append(site.getAdminName()).append("\r\n");
		msg.append(site.getAdminEmail()).append("\r\n\r\n");
		msg.append("Cc: Eva Casamento, Tim Flynn, ").append(site.getAdminName());
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.addRecipient("RA-DPYUS-dpyryopsvc@its.jnj.com");
    		mail.addCC(new String[] { site.getAdminEmail(), "tflynn@its.jnj.com", "ecasamen@its.jnj.com" });
    		mail.setTextBody(msg.toString());
    		    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		
    		log.debug("Request approval email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Request Approval Email, ", me);
    	}
    	return;
	}
	
	
	/**
	 * sends request approval to the rep and admin
	 * @param req
	 */
	public void approveRequestRep(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans) {
		//send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuffer subject = new StringBuffer();
		subject.append("Information for your MedStream Control Unit: Request# ").append(trans.getRequestNo());
		
		StringBuffer msg = new StringBuffer();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Your MedStream Control Unit request has been approved.").append("\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ");
		msg.append(Convert.formatDate(trans.getApprovalDate(), Convert.DATE_SLASH_PATTERN));
		msg.append("\r\n");
		msg.append("Requested By: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(phys.getFirstName()).append(" ").append(phys.getLastName()).append("\r\n\r\n");
		msg.append("Sincerely,\r\nThe ").append(site.getSiteName()).append(" Team\r\n\r\n");
		msg.append("Cc: ").append(site.getAdminName());
	
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.addRecipient(rep.getEmailAddress());
    		for (UserDataVO vo : admins)
    			mail.addRecipient(vo.getEmailAddress());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		
    		log.debug("Request approval email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Request Approval Email, ", me);
    	}
    	return;
	}
	
	
	/**
	 * Email sent when unit ships (after serial number of unit is
	 * specified in order.
	 * @param req
	 * @param vo
	 */
	public void unitShipped(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans, AccountVO acct) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		StringBuffer subject = new StringBuffer();
		subject.append("Information for your MedStream Control Unit: Request# ").append(trans.getRequestNo());
		
		StringBuffer msg = new StringBuffer();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Your MedStream Control Unit has shipped per your request.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(phys.getFirstName()).append(" ").append(phys.getLastName()).append("\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n\r\n");
		msg.append("Shipped to:\r\n");
		
		//ship to address
		msg.append(trans.getShipToName()).append("\r\n");
		msg.append(trans.getShippingAddress().getAddress()).append("\r\n");
		if (StringUtil.checkVal(trans.getShippingAddress().getAddress2()).length() > 0) 
			msg.append(trans.getShippingAddress().getAddress2()).append("\r\n");
		msg.append(trans.getShippingAddress().getCity()).append(", ");
		msg.append(trans.getShippingAddress().getState()).append("  ");
		msg.append(trans.getShippingAddress().getZipCode()).append(", ");
		msg.append(trans.getShippingAddress().getCountry()).append("\r\n\r\n");
		
		msg.append("Please call Customer Support if the Control Unit has not arrived within a few days.\r\n\r\n");
		msg.append("Cc: ").append(site.getAdminName());

		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rep.getEmailAddress());
    		for (UserDataVO vo : admins)
    			mail.addRecipient(vo.getEmailAddress());
    		mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		
    		log.debug("Request Unit Shipped email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("CU Request Unit Shipped Email, ", me);
    	}
    	return;
	}
	
	
	public void requestDeclined(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, PhysicianVO phys) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		StringBuffer subject = new StringBuffer();
		subject.append("Request for MedStream Control Unit no-charge order");
		
		StringBuffer msg = new StringBuffer();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("We're sorry but your request for a MedStream Control Unit has been declined.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(phys.getFirstName()).append(" ");
		msg.append(phys.getLastName()).append("\r\n\r\n");
		
		if (StringUtil.checkVal(trans.getNotesText()).length() > 0)
			msg.append(trans.getNotesText()).append("\r\n\r\n");
		msg.append("Cc: ").append(site.getAdminName());
		
		try {
    		// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rep.getEmailAddress());
    		for (UserDataVO vo : admins)
    			mail.addRecipient(vo.getEmailAddress());
    		mail.setSubject(subject.toString());
    		mail.setFrom(site.getMainEmail());
    		mail.setTextBody(msg.toString());
    		
    		MessageSender ms = new MessageSender(attributes, dbConn);
    		ms.sendMessage(mail);
    		
    		log.debug("Request declined email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("CU Request Declined Email, ", me);
    	}
    	return;
	}
	
	/**
	 * Sends account information to user
	 * @param vo
	 * @param site
	 * @throws MailException
	 */
	public void sendAccountInfo(PersonVO vo, SiteVO site) throws MailException {
		log.debug("send notification email to " + vo.getEmailAddress());
		StringBuffer msg = new StringBuffer();
		msg.append("<p>A login account has been created for you at<br><a href='http://");
		msg.append(site.getSiteAlias()).append("/'>http://");
		msg.append(site.getSiteAlias()).append("/</a></p>");
		msg.append("<p>Login using this email address and the password \"").append(vo.getPassword());
		msg.append("\".  You may be required to change your password if this is your first login</p>");
		msg.append("<p>Sincerely,<br>The ").append(site.getSiteName()).append(" Team</p>");
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.setFrom(site.getMainEmail());
			mail.setSubject(site.getSiteName() + " Login Information");
			mail.addRecipient(vo.getEmailAddress());
			mail.setHtmlBody(msg.toString());
			   		
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
			ms = null;
		} catch (Exception e) {
			throw new MailException(e.getCause());
		}
	}

}
