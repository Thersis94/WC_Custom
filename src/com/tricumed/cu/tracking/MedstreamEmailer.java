package com.tricumed.cu.tracking;

import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;
import com.tricumed.cu.tracking.vo.AccountVO;
import com.tricumed.cu.tracking.vo.PersonVO;
import com.tricumed.cu.tracking.vo.PhysicianVO;
import com.tricumed.cu.tracking.vo.TransactionVO;

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
public class MedstreamEmailer extends SimpleActionAdapter {

	protected MessageSender ms = null; //shared with ICPExpressEmailer, a subclass


	/**
	 * @param dbConn
	 * @param attributes
	 */
	public MedstreamEmailer(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);

		//TODO temporary overrides for Mark to evaluate the app in staging -JM- 09.30.19
		attributes.put(AdminConstants.APP_NM, "DEVELOP");
		attributes.put(GlobalConfig.KEY_ADMIN_EMAIL, "Marc.Hoffmeister@tricumed.de");
		ms = new MessageSender(attributes, dbConn);
	}


	/**
	 * Sends email when rep requests a new control unit
	 * @param req
	 * @param vo
	 */
	public void submitRequest(ActionRequest req, List<UserDataVO> admins, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans, AccountVO acct) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder subject = new StringBuilder(100);
		subject.append("MedStream Control Unit Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder(500);
		msg.append("Dear Administrator,\r\n");
		msg.append("There is a request for a MedStream Control Unit pending in your inbox.\r\n\r\n");
		msg.append("Request #: ").append(trans.getRequestNo()).append("\r\n");
		msg.append("Date: ").append(Convert.formatDate(trans.getCreateDate(), Convert.DATE_SLASH_PATTERN)).append("\r\n");
		msg.append("Account: ").append(acct.getAccountName()).append(" (#").append(acct.getAccountNo()).append(")\r\n");
		msg.append("Requesting Rep: ").append(trans.getRequestorName()).append("\r\n");
		msg.append("Physician: ").append(phys.getFirstName()).append(" ").append(phys.getLastName()).append("\r\n\r\n");
		msg.append("Please review and approve this request.\r\n");
		msg.append("http://").append(site.getSiteAlias());
		msg.append("\r\n\r\nCc: ").append(rep.getFirstName()).append(" ").append(rep.getLastName());

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.addRecipient(rep.getEmailAddress());
			for (UserDataVO vo : admins)
				mail.addRecipient(vo.getEmailAddress());
			mail.setTextBody(msg.toString());
			ms.sendMessage(mail);
			log.debug("Request Submit email sent");

		} catch (Exception me) {
			log.error("CU Request Submit Email, ", me);
		}
		return;
	}

	/**
	 * sends request approval to the customer service rep and admin
	 * @param req
	 */
	public void approveRequestCS(ActionRequest req, List<UserDataVO> admins, 
			PersonVO rep, TransactionVO trans, AccountVO acct) {
		//send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder subject = new StringBuilder(100);
		subject.append("Request for MedStream Control Unit no-charge order: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder(500);
		msg.append("Dear Customer Service Rep,\r\n");
		msg.append("Please ship quantity ").append(trans.getUnitCount());
		msg.append(" of product code 91-4205 to account#: ").append(StringUtil.checkVal(rep.getSampleAccountNo())).append("\r\n\r\n");
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
		msg.append("Cc: ").append(rep.getFirstName()).append(" ");
		msg.append(rep.getLastName()).append(", ").append(site.getAdminName());

		try {
			// Create the mail object and send
			EmailMessageVO mail = new EmailMessageVO();
			for (UserDataVO vo : admins)
				mail.addRecipient(vo.getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			ms.sendMessage(mail);
			log.debug("Request approval email sent");

		} catch (Exception me) {
			log.error("Request Approval Email, ", me);
		}
		return;
	}

	/**
	 * sends request approval to the rep and alloc
	 * @param req
	 */
	public void approveRequestRep(ActionRequest req, List<UserDataVO> alloc, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans) {
		//send email to site admin
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder subject = new StringBuilder(100);
		subject.append("Information for your MedStream Control Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder(500);
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
			mail.addRecipient(rep.getEmailAddress());
			for (UserDataVO vo : alloc)
				mail.addRecipient(vo.getEmailAddress());
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			ms.sendMessage(mail);
			log.debug("Request approval email sent");

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
	public void unitShipped(ActionRequest req, PersonVO rep, 
			PhysicianVO phys, TransactionVO trans, AccountVO acct) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder subject = new StringBuilder(100);
		subject.append("Information for your MedStream Control Unit: Request# ").append(trans.getRequestNo());

		StringBuilder msg = new StringBuilder(500);
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
			mail.setSubject(subject.toString());
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			ms.sendMessage(mail);
			log.debug("Request Unit Shipped email sent");

		} catch (Exception me) {
			log.error("CU Request Unit Shipped Email, ", me);
		}
		return;
	}


	public void requestDeclined(ActionRequest req, PersonVO rep, 
			TransactionVO trans, AccountVO acct, PhysicianVO phys) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		String subject = "Request for MedStream Control Unit no-charge order";
		StringBuilder msg = new StringBuilder(500);
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
			mail.setSubject(subject);
			mail.setFrom(site.getMainEmail());
			mail.setTextBody(msg.toString());
			ms.sendMessage(mail);
			log.debug("Request declined email sent");

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
		StringBuilder msg = new StringBuilder(500);
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
			ms.sendMessage(mail);

		} catch (Exception e) {
			throw new MailException(e.getCause());
		}
	}
}
