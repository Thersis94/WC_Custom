package com.fastsigns.action;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
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
 * <b>Description: facades the call to ContactSubmttal to save the user data, then distributes 
 * email confirmations. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 19, 2014
 ****************************************************************************/
public class TVSpotDlrContactAction extends SimpleActionAdapter {
	private static final String DLR_LOCN_FIELD_ID = "con_c0a802374be51c9177a78a7b7677ea5c";
	
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
		String contactActGrpId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		req.setParameter("actionGroupId", contactActGrpId);
		actionInit.setActionId(contactActGrpId);
		
		SMTActionInterface cfa = new ContactFacadeAction(actionInit);
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
		
		// this is a hook to load the dealer locations dropdown.  Invoked via PublicAjaxServlet
		if (req.hasParameter("view")) {
			listDealerLocations(mod, req);
			return;
		}
		
		//load the dealer info using the dealerLocationId passed on the request
		req.setParameter(DLR_LOCN_FIELD_ID,  req.getParameter(DLR_LOCN_FIELD_ID).substring(4), true);
		DealerLocationVO dealer = loadDesiredDealer(req);
		
		//the contact us portlet will send the email to the FranchiseOwner and Center for us;
		//configure the email as needed before we invoke that class.
		addCenterEmailParamsToReq(req, dealer);
		
		//invoke the Contact Us portlet to save the data and send the Franchise email
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		SMTActionInterface cfa = new ContactFacadeAction(actionInit);
		cfa.setAttributes(attributes);
		cfa.setDBConnection(dbConn);
		cfa.build(req);
		
		//send a confirmation email to the user
		emailUserConfirmation(req, dealer);
		
		//the browser redirect occurring here was setup for us by the ContactSubmittalAction
	}
	
	
	/**
	 * Call DealerLocatorAction to retrieve a list of dealers (Centers) nearby the
	 * passed zipcode.  The module data is turned into JSON in the view and returned
	 * to the browser via an AJAX call.
	 * @param mod
	 * @param req
	 */
	private void listDealerLocations(ModuleVO mod, SMTServletRequest req) {
		//set the actionId of the locator portlet we're facading
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2)); 
		SMTActionInterface dla = new DealerLocatorAction(actionInit);
		dla.setAttributes(attributes);
		dla.setDBConnection(dbConn);
		try {
			dla.retrieve(req);
		} catch (ActionException ae) {
			log.error("could not load dealer list", ae);
		}
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
			dealers = dla.getDealerInfo(req, new String[] { req.getParameter(DLR_LOCN_FIELD_ID) }, null);
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
		//email message recipients
		Set<String> emails = new HashSet<String>();
		if (dealer.getEmailAddress() != null && dealer.getEmailAddress().contains(",")) {
			emails.addAll(Arrays.asList(dealer.getEmailAddress().split(",")));
		} else if (StringUtil.isValidEmail(dealer.getEmailAddress())) {
			emails.add(dealer.getEmailAddress());
		}
		if (StringUtil.isValidEmail(dealer.getOwnerEmail())) 
			emails.add(dealer.getOwnerEmail());
		
		req.setParameter("contactEmailAddress", emails.toArray(new String[emails.size()]), true);
		req.setParameter("dealerLocationId", req.getParameter(DLR_LOCN_FIELD_ID));
		
		//set the status to 'initiated'
		req.setParameter("con_" + TVSpotUtil.ContactField.status.id(), TVSpotUtil.Status.initiated.toString());
		
		
		//email header
		StringBuilder msg = new StringBuilder();
		msg.append("<p>As part of our 2014 TV Spot, there is an option at the end ");
		msg.append("of the commercial for customers to request a free consultation ");
		msg.append("from FASTSIGNS.  The request is sent to the closest location ");
		msg.append("based on the prospect's provided zip code.<br/>");
		msg.append("Please be aware that a consultation request has been submitted for your center.<br/>");
		msg.append("Review the information provided and follow up with the customer as soon as possible:</p>");
		req.setParameter("contactEmailHeader", msg.toString());
		
		//email footer
		msg = new StringBuilder();
		msg.append("<p>It's important that you update the Consultation Request ");
		msg.append("report at <a href=\"http://www.fastsigns.com/webedit\">http://www.fastsigns.com/webedit</a>.  ");
		msg.append("Login to the system, select TV spot from the menu, then select \"Consultation Request\".  ");
		msg.append("Here you will find a list of all of your consultation requests.  ");
		msg.append("Please review and update the \"status\" column to indicate the status ");
		msg.append("of contacting the customer.  If you do not update the status of this ");
		msg.append("request, you will receive a second notification tomorrow.</p>");
		msg.append("<p>For more information about the TV Spot and the process, please ");
		msg.append("refer to the following resources or consult with your Franchise ");
		msg.append("Business Consultant and/or your Marketing Services Manager.</p>");
		//msg.append("<p>Watch the webinar:  support.fastsigns.com#######<br/>");
		//msg.append("Read the overview document:  DOC ID ###<br/>"); 
		//msg.append("Watch the TV spot: www.fastsings.com/#####</p>");
		req.setParameter("contactEmailFooter", msg.toString());
		
		//email subject
		req.setValidateInput(false);
		req.setParameter("contactEmailSubject","TV Spot Consultation Request, " + req.getParameter("pfl_combinedName"));
		req.setValidateInput(true);
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
			mail.addRecipient(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
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
