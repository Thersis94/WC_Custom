package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.PersonVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
public class ICPExpressEmailer extends MedstreamEmailer {
	
	private static List<String> adminEmails = new ArrayList<String>(){
		private static final long serialVersionUID = 1l; 
		{
			add("acayssio@its.jnj.com");
			add("efournie@ITS.JNJ.com");
			add("cskelto@ITS.JNJ.com");
			add("SStefan1@its.jnj.com");
		}
	};

	/**
	 * @param arg0
	 */
	public ICPExpressEmailer(ActionInitVO arg0) {
		super(arg0);
	}

	public void sendTransactionMessage(ActionRequest req, TransactionVO trans) {
		AccountVO acct = retrieveAccount(trans.getAccountId());

		//lookup rep information
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<String> pid = new ArrayList<String>();
		pid.add(acct.getRep().getProfileId());
		UserDataVO usr = null;

		try {
			usr = pm.searchProfile(dbConn, pid).get(0);
		} catch (DatabaseException | NullPointerException e) {
			log.error(e);
			return;
		}
		PersonVO rep = acct.getRep();
		rep.setName( usr.getFullName() );
		rep.setFirstName( usr.getFirstName() );
		acct.setRep(rep);
		
		this.sendTransactionMessage(req, trans, acct);
	}
	
	/**
	 * Send email for new unit requests
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param trans
	 * @param stat new ICP status
	 * @param acct
	 */
	@SuppressWarnings("incomplete-switch")
	public void sendTransactionMessage(ActionRequest req, TransactionVO trans, AccountVO acct) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		PersonVO rep = acct.getRep();

		switch (trans.getStatus()) {
			case PENDING:
				newOrderInit(req,adminEmails,rep,trans,site,acct);
				break;
			case COMPLETE:
				newOrderShipped(req,adminEmails,rep,trans,site,acct);
				break;
			case SVC_REQ:
				this.repairInitiated(req,adminEmails,rep,trans,site,acct);
				break;
			case SVC_REQ_RCVD:
				this.repairReceived(req,adminEmails,rep,trans,site,acct);
				break;
			case SVC_REQ_COMPL:
				this.repairComplete(req,adminEmails,rep,trans,site,acct);
				break;
			case SVC_REQ_SENT_EDC:
				this.repairSentToEDC(req,adminEmails,rep,trans,site,acct);
				break;
			case SVC_REQ_SENT_REP:
				this.repairSentToRep(req,adminEmails,rep,trans,site,acct);
				break;
			case RTRN_REQ:
				this.repairInitiated(req,adminEmails,rep,trans,site,acct);
				break;
			case RTRN_REQ_RCVD:
				this.repairPartialRefund(req, adminEmails, rep, trans, site, acct);
				break;
		}
	}
	
	
	/**
	 * When a new order gets placed
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void newOrderInit(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans, SiteVO site, AccountVO acct){

		StringBuilder subject = new StringBuilder();
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("A new ICP Express order has been placed.\r\n\r\n");
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
			for (String eml : admins)
				mail.addCC(eml);
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
	 * When a new order is fulfilled
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void newOrderShipped(ActionRequest req, List<String> admins, PersonVO rep,
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
			for (String eml : admins)
				mail.addCC(eml);
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
	 * When a repair order is placed
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairInitiated(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans, SiteVO site, AccountVO acct){

		if (trans.getCreateDate() == null)
			trans.setCreateDate(Convert.getCurrentTimestamp());
		
		StringBuilder subject = new StringBuilder(200);
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("This email confirms you will be shipping a unit back to the Repair Center.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Order Reference Number: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Unit Serial#: ").append(trans.getUnitSerialNos()).append("\r\n\r\n");
		msg.append("Cc: ").append(site.getAdminName());

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rep.getEmailAddress());
			for (String eml : admins)
				mail.addCC(eml);
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
	 * When the repair center receives the box.
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairReceived(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans, SiteVO site, AccountVO acct) {

		StringBuilder subject = new StringBuilder(200);
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
			for (String eml : admins)
				mail.addCC(eml);
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
	 * When the partial credits have been confirmed by GMED
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairPartialRefund(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans, SiteVO site, AccountVO acct) {

		StringBuilder subject = new StringBuilder(200);
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder();
		msg.append("Dear ").append(rep.getFirstName()).append(",\r\n");
		msg.append("Partial credits have been applied to the account for the ICP Express box you recently returned.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Credit Amount: $").append(trans.getCreditText()).append("\r\n\r\n");
		msg.append("Cc: ").append(site.getAdminName());

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(rep.getEmailAddress());
			for (String eml : admins)
				mail.addCC(eml);
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
	 * When the refurbishment has been completed
	 * @param req
	 * @param alloc
	 * @param rep
	 * @param phys
	 * @param trans
	 */
	private void repairComplete(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans, SiteVO site, AccountVO acct) {

		StringBuilder subject = new StringBuilder(200);
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
			for (String eml : admins)
				mail.addCC(eml);
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
	private void repairSentToEDC(ActionRequest req, List<String> admins, 
			PersonVO rep, TransactionVO trans,  SiteVO site, AccountVO acct) {

		StringBuilder subject = new StringBuilder(80);
		subject.append("Information for your ICP Express Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder(200);
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
			for (String eml : admins)
				mail.addCC(eml);
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
	private void repairSentToRep(ActionRequest req, List<String> admins, PersonVO rep,
			TransactionVO trans, SiteVO site, AccountVO acct) {

		StringBuilder subject = new StringBuilder(200);
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
			for (String eml : admins)
				mail.addCC(eml);
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
	

	/**
	 * Fetch a single account by id
	 * @param acctId
	 * @return
	 */
	private AccountVO retrieveAccount(String acctId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select a.*,p.* from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("codman_cu_account a ");
		sql.append("left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("codman_cu_person p on a.person_id=p.person_id ");
		sql.append("where a.account_id=? ");
		AccountVO vo = null;

		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, StringUtil.checkVal(acctId));
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				vo = new AccountVO(rs);
		} catch (SQLException e){
			log.error("Failed to get account,",e);
			vo = new AccountVO();
		}

		return vo;
	}
}