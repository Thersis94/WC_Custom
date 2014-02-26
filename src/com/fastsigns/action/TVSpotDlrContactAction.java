package com.fastsigns.action;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.db.DatabaseException;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: TVSpotDlrContactAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 19, 2014
 ****************************************************************************/
public class TVSpotDlrContactAction extends SimpleActionAdapter {

	public TVSpotDlrContactAction() {
		super();
	}

	public TVSpotDlrContactAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	
	/**
	 * retrieve the default ContactUs form/portlet using the actionId defined in 
	 * attribute1
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		ContactFacadeAction cfa = new ContactFacadeAction(actionInit);
		cfa.setAttributes(attributes);
		cfa.setDBConnection(dbConn);
		cfa.retrieve(req);
	}

	
	/**
	 * submit the contact us portlet, then process the custom outgoing emails.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		DealerLocationVO dealer = loadDesiredDealer(req);
		
		//the contact us portlet will send the email to the FranchiseOwner and Center for us;
		//configure the email as needed before we call it.
		addCenterEmailParamsToReq(req, dealer);
		
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		ContactFacadeAction cfa = new ContactFacadeAction(actionInit);
		cfa.setAttributes(attributes);
		cfa.setDBConnection(dbConn);
		cfa.build(req);
		
		//send confirmation email to the user
		emailUserConfirmation(req, dealer);
		
	}
	
	
	/**
	 * loads the desired dealer (details VO) based on the pre-selected dealerLocationId passed to us
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private DealerLocationVO loadDesiredDealer(SMTServletRequest req) throws ActionException {
		DealerLocatorAction dla = new DealerLocatorAction(actionInit);
		dla.setAttributes(attributes);
		dla.setDBConnection(dbConn);
		List<DealerLocationVO> dealers = null;
		try {
			dealers = dla.getDealerInfo(req, new String[] { req.getParameter("dealerLocationId") }, null);
		} catch (DatabaseException de) {
			log.error("could not load dealer list", de);
		}
		
		if (dealers != null && dealers.size() > 0) return dealers.get(0);
		else throw new ActionException("no dealer found");
	}
	
	
	/**
	 * adds some values to the request to be injected into the outgoing center/
	 * owner-bound email.
	 * @param req
	 */
	private void addCenterEmailParamsToReq(SMTServletRequest req, DealerLocationVO dealer) {
		//email recipients
		req.setParameter("contactEmailAddress", new String[]{ dealer.getEmailAddress(), dealer.getOwnerEmail() }, true);
		
		//email header
		req.setParameter("contactEmailHeader", "<p></p>");
		
		//email subject
		req.setParameter("contactEmailSubject","");
	}
	
	
	/**
	 * send the user confirmation of their inquiry.
	 * @param req
	 */
	private void emailUserConfirmation(SMTServletRequest req, DealerLocationVO dealer) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		PhoneNumberFormat phone = new PhoneNumberFormat(dealer.getPhone(), PhoneNumberFormat.PAREN_FORMATTING);
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>Thank you for submitting your Consultation Request to FASTSIGNS&reg;.</p>");
		msg.append("<p>Your request has been submitted to:</p>");
		
		msg.append("<p>FASTSIGNS of ").append(dealer.getLocationName()).append("<br/>");
		msg.append(dealer.getAddress()).append("<br/>");
		if (dealer.getAddress2() != null && dealer.getAddress2().length() > 0) 
			msg.append(dealer.getAddress2()).append("<br/>");
		msg.append(dealer.getCity()).append(", ").append(dealer.getState()).append(" ").append(dealer.getZipCode()).append("<br/>");
		msg.append(phone.getFormattedNumber()).append("</p>");
		
		msg.append("<p>FASTSIGNS of ").append(dealer.getLocationName());
		msg.append(" will contact you within the next few days to talk with you about your request.  ");
		msg.append("In the meantime, if you would like to learn more about comprehensive solutions ");
		msg.append("for your communications challenges, download or view our guide online at ");
		msg.append("<a href=\"http://www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf\">");
		msg.append("http://www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf</a> or visit ");
		msg.append("the <a href=\"http://www.fastsigns.com/LearningCenter\">FASTSIGNS ");
		msg.append("online Learning Center</a> for access to our white papers, ");
		msg.append("and helpful tips and information.</p>");
		
		msg.append("<p>Thank you for contacting FASTSIGNS.  We look forward to ");
		msg.append("talking with you about your communications challenges.</p>");
		msg.append("<img src=\"http://www.fastsigns.com/binary/org/FTS/FTS_2012/images/IMAGES-11.png\" alt=\"More ways to bring your communications to life.\" /><br/>");
		
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(req.getParameter("emailAddress"));
			mail.setSubject("Thank you for submitting your Consultation Request to FASTSIGNS.");
			mail.setFrom(site.getMainEmail());
			mail.setHtmlBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (InvalidDataException ide) {
			log.error("could not send confirmation email", ide);
		}
	}

	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
