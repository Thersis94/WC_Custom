/**
 * 
 */
package com.codman.cu.tracking;

import java.util.List;

import com.codman.cu.tracking.TransIcpAction.Status;
import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.PersonVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: TrackingEmailerICP.java<p/>
 * <b>Description: </b> Handles emails for ICP Express
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Oct 31, 2014
 ****************************************************************************/
public class TrackingEmailerICP extends TrackingEmailer {

	/**
	 * @param arg0
	 */
	public TrackingEmailerICP(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * Send email based on the new ICP status
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param trans
	 * @param stat new ICP status
	 * @param acct
	 */
	public void sendICPMessage(SMTServletRequest req, List<UserDataVO> alloc, PersonVO rep, 
			TransactionVO trans, AccountVO acct){
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		Status stat = Status.valueOf( StringUtil.checkVal(trans.getStatusName()));
		
		switch( stat ){
		case OLD_SENT:
			oldIcp(req,alloc,rep,trans, site, acct);
			break;
		case CREDIT_CONF:
			creditConfirmed(req,alloc,rep,trans, acct,site);
			break;
		case NEW_ORDER:
			newICP(req,alloc,rep,trans,acct,site);
			break;
		case REPAIR_RECEIVED:
			repairReceived(req,alloc,rep,trans,acct,site);
			break;
		case REPAIR_SERVICED:
			repairComplete(req,alloc,rep,trans,acct,site);
			break;
		case REPAIR_SENT:
			repairSent(req,alloc,rep,trans,acct,site);
			break;
		case NEW_SENT:
			edcSent(req,alloc,rep,trans,acct,site);
			break;
		}
	}

	/**
	 * When the old country sends the box out
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void oldIcp(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep,
			TransactionVO trans, SiteVO site, AccountVO acct){
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Your ICP Express box has shipped per your request.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requested By: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n\r\n");
		msg.append("Please call Customer Support if the box has not arrived within a few days.\r\n\r\n");
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
    		
    		log.debug("ICP Express Box Shipped email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("ICP Express Box Shipped Email, ", me);
    	}
	}
	
	/**
	 * When the partial credits have been confirmed by GMED
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void creditConfirmed(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Partial credits have been applied to the account for the ICP Express box.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Credit Amount: $").append(trans.getCreditText()).append("\r\n");
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
    		
    		log.debug("Credit Confirmed email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Credit Confirmed Email, ", me);
    	}
	}
	
	/**
	 * When a new order has been placed
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void newICP(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("A new order has been placed.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n\r\n");
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
    		
    		log.debug("New Icp Order email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("New Icp Order Email, ", me);
    	}
	}
	
	/**
	 * When the repair center receives the box.
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairReceived(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("The Repair Center has received the unit and the refurbishing process has begun.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n\r\n");
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
    		
    		log.debug("Repair Received Email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Repair Received Email, ", me);
    	}
	}
	
	/**
	 * When the refurbishment has been completed
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairComplete(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Refurbishment of the ICP unit has been completed. It will soon be shipped back to the EDC.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
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
    		
    		log.debug("Repairs Complete Email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Repairs Complete Email, ", me);
    	}
	}
	
	/**
	 * When the unit has been sent back to EDC
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairSent(SMTServletRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("The unit has been sent out to the EDC from the Repair Center.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n\r\n");
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
    		
    		log.debug("Shipped From Repair Center Email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("Shipped From Repair Center Email, ", me);
    	}
	}
	
	/**
	 * When the box has been sent to the new country
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void edcSent(SMTServletRequest req, List<UserDataVO> admins, PersonVO rep,
			TransactionVO trans, AccountVO acct, SiteVO site){
		
		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());
		
		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Your ICP Unit has shipped from the EDC.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
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
		
		msg.append("Please call Customer Support if the Unit has not arrived within a few days.\r\n\r\n");
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
    		
    		log.debug("ICP Unit Shipped email sent");
    		ms = null;
    	} catch (Exception me) {
    		log.error("ICP Unit Shipped Email, ", me);
    	}
    	return;
	}
}
