package com.fastsigns.action;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.action.contact.SubmittalDataAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
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
	
	public static final String CON_ = "con_";  //comes from ContactUs
	
	private final String dlrLocnField;  //runtime constant
	
	public TVSpotDlrContactAction() {
		super();
		dlrLocnField = CON_ + TVSpotUtil.ContactField.preferredLocation.id();
	}

	public TVSpotDlrContactAction(ActionInitVO arg0) {
		super(arg0);
		dlrLocnField = CON_ + TVSpotUtil.ContactField.preferredLocation.id();
	}
	
	
	/**
	 * retrieve the default ContactUs form/portlet using the actionId defined in 
	 * attribute1
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("isSurvey")) return; //we don't need anything addtl to render this.
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String contactActGrpId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		req.setParameter(Constants.ACTION_GROUP_ID, contactActGrpId);
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
			
		} else if (req.hasParameter("contactSubmittalId") && req.hasParameter("isSurvey")) {
			//these are the feedback surveys.  Update the existing record with the data for these two contact fields.
			try {
				updateSurvey(req);
			} catch (SQLException sqle) {
				log.error("could not update survey response", sqle);
				//nothing we can do about this, so let the user continue on unbeknownst.
			}
			return;
		}
		
		//load the dealer info using the dealerLocationId passed on the request
		//remove the "dlr-" prefix we needed when we built the dropdown (ensured proper ordering, by distance)
		req.setParameter(dlrLocnField,  req.getParameter(dlrLocnField).substring(4), true);
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
	 * updates the users original contact submittal and completes the two empty
	 * fields for feedback and rating.  These are asked on a survey attached to an email campaign.
	 * @param req
	 * @throws SQLException
	 */
	private void updateSurvey(SMTServletRequest req) throws SQLException {
		SubmittalDataAction sda = new SubmittalDataAction(actionInit);
		sda.setAttributes(attributes);
		sda.setDBConnection(dbConn);
		String csi = req.getParameter("contactSubmittalId");
		
		//update survey rating
		String key = TVSpotUtil.ContactField.rating.id();
		//log.debug("rating=" + req.getParameter(CON_ + key));
		sda.updateField(req.getParameter( CON_ + key), csi, key);
		
		//update survey feedback
		key = TVSpotUtil.ContactField.feedback.id();
		//log.debug("feedback=" + req.getParameter(CON_ + key));
		sda.updateField(req.getParameter(CON_ + key), csi, key);
		
		//redirect the browser to the thank you page
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder();
		url.append(page.getRequestURI()).append("?isSurveyComplete=true");
		url.append("&contactSubmittalId=").append(csi);
		super.sendRedirect(url.toString(), null, req);
	}
	
	
	/**
	 * Call DealerLocatorAction to retrieve a list of dealers (Centers) nearby the
	 * passed zipcode.  The module data is turned into JSON in the view and returned
	 * to the browser via an AJAX call.
	 * @param mod
	 * @param req
	 */
	private void listDealerLocations(ModuleVO mod, SMTServletRequest req) {
		//this tells the locator we only want records that are active for promotions.
		req.setAttribute("promotionsFlg", "1");
		
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
			dealers = dla.getDealerInfo(req, new String[] { req.getParameter(dlrLocnField) }, null);
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
		req.setAttribute("senderEmail","consultation@fastsigns.com");
		req.setParameter("dealerLocationId", req.getParameter(dlrLocnField));
		
		//set the status to 'initiated'
		req.setParameter(CON_ + TVSpotUtil.ContactField.status.id(), TVSpotUtil.Status.initiated.toString());
		
		//set the users state based on their zip code
		String state = getStateFromZip(req);
		req.setParameter(CON_ + TVSpotUtil.ContactField.state.id(), state);
		
		//email header
		StringBuilder msg = new StringBuilder();
		msg.append("<p><b>Please contact the prospect below using the information provided within 24 hours; ");
		msg.append("he or she will receive an email survey in seven business days asking them to rate their ");
		msg.append("experience with your center and their FASTSIGNS&reg; consultation.</b><br/>This prospect has chosen ");
		msg.append("your location and completed a form requesting a consultation after seeing our \"Operation ");
		msg.append("Consultation\" commercial on television, online or on our website.  We recommend that you ");
		msg.append("call and then follow up with an email if you are unable to connect with them on your initial ");
		msg.append("attempt. You can determine whether the actual consultation is via phone or in-person.</p><br/>");
		req.setParameter("contactEmailHeader", msg.toString());
		
		//email footer
		msg = new StringBuilder();
		msg.append("<b>Here are six important things for you to know:</b></br>");
		msg.append("<ol>");
		msg.append("<li>This prospect chose you from nearby locations; we have provided he/she with your ");
		msg.append("center contact information and have told them that someone would be in touch.</li>");
		msg.append("<li>This email is being sent to both your center and Franchise Partner email accounts; a ");
		msg.append("second email reminding you to contact this prospect will be automatically sent to these ");
		msg.append("addresses at the end of the next business day.</li>");
		msg.append("<li>We will track your consultation requests and survey feedback in the Web Edit tool ");
		msg.append("(<a href=\"http://www.fastsigns.com/webedit\">www.fastsigns.com/webedit</a>); you'll get an email ");
		msg.append("each day you have activity (consultation requests, surveys answered, etc.).</li>");
		msg.append("<li>Periodically we will send you a request to tell us if the leads generated sales, and if ");
		msg.append("so, the sale amount.  If you would like to proactively provide this information, you can update the ");
		msg.append("\"Consultation Request\" section at <a href=\"http://www.fastsigns.com/webedit\">www.fastsigns.com/webedit</a>. ");
		msg.append("If you choose to, you can review and update the \"status\" column to indicate the status of contacting ");
		msg.append("the prospect and view survey results.</li>");
		msg.append("<li>This survey question will be automatically emailed to the prospect seven business days after ");
		msg.append("their initial consultation request:<div style=\"margin:30px 0 30px 40px;\">");
		msg.append("<i>Thank you for your recent request for a consultation from ");
		msg.append("FASTSIGNS&reg;. Please take a moment to rate your satisfaction level with the consultation and tell ");
		msg.append("us about your experience.<ul><li>How satisfied were you with your consultation? Please select a ");
		msg.append("ranking between 1 (not satisfied at all) and 10 (extremely satisfied)</li>");
		/*
		msg.append("<table  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width: 300px;\"><tbody>");
		msg.append("<tr><td>O1<input type=\"radio\" name=\"num\" value=\"1\"></td>");
		msg.append("<td>O2<input type=\"radio\" name=\"num\" value=\"2\"></td>");
		msg.append("<td>O3<input type=\"radio\" name=\"num\" value=\"3\"></td>");
		msg.append("<td>O4<input type=\"radio\" name=\"num\" value=\"4\"></td>");
		msg.append("<td>O5<input type=\"radio\" name=\"num\" value=\"5\"></td></tr>");
		msg.append("<tr><td>O6<input type=\"radio\" name=\"num\" value=\"6\"></td>");
		msg.append("<td>O7<input type=\"radio\" name=\"num\" value=\"7\"></td>");
		msg.append("<td>O8<input type=\"radio\" name=\"num\" value=\"8\"></td>");
		msg.append("<td>O9<input type=\"radio\" name=\"num\" value=\"9\"></td>");
		msg.append("<td>10<input type=\"radio\" name=\"num\" value=\"10\"></td></tr>");
		msg.append("</tbody></table><br/>");
		*/
		msg.append("<li>If desired, please tell us more about your experience (open-ended with space for at least 250 words).</li></ul></i></div></li>");
		msg.append("<li>For more information about \"Operation Consultation\", please refer to the following resources or ");
		msg.append("consult with your Franchise Business Consultant and/or your Marketing Services Manager:<br/>");
		msg.append("<ul>");
		msg.append("<li>Watch the TV spot: <a href=\"http://www.fastsigns.com/#####\">www.fastsigns.com/###</a></li>");
		msg.append("<li>Review the overview document: DOC ID ###</li>");
		msg.append("<li>View the webinar:  <a href=\"http://support.fastsigns.com#######\">support.fastsigns.com######</a></li>");
		msg.append("</ul>");
		msg.append("</li>");
		msg.append("</ol>");
		req.setParameter("contactEmailFooter", msg.toString());
		
		//email subject
		req.setValidateInput(false);
		req.setParameter("contactEmailSubject","LEAD FROM TV:  Enact \"Operation Consultation\" within 24 hours: " + req.getParameter("pfl_combinedName"));
		req.setValidateInput(true);
	}
	
	
	/**
	 * Does a quick geocode for the user's state using the passed zip code.
	 * Assumes country code to be that of the Site, since it's not asked of the user..
	 * @param zip
	 * @return
	 */
	private String getStateFromZip(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		Location loc = null;
		DealerLocatorAction dla = new DealerLocatorAction();
		dla.setAttributes(attributes);
		
		try {
			req.setParameter("zip", req.getParameter(CON_ + TVSpotUtil.ContactField.zipcode.id()));
			req.setParameter("country", site.getCountryCode());
			loc = dla.getGeocode(req);
		} catch (Exception e) {
			log.error("could not geocode zip", e);
			loc = new Location();
		}
		
		return loc.getState();
	}
	
	
	/**
	 * send the user confirmation of their inquiry.
	 * @param req
	 */
	private void emailUserConfirmation(SMTServletRequest req, DealerLocationVO dealer) {
		PhoneNumberFormat phone = new PhoneNumberFormat(dealer.getPhone(), PhoneNumberFormat.PAREN_FORMATTING);
		
		StringBuilder msg = new StringBuilder();
		msg.append("<p>Thank you for submitting your consultation request to FASTSIGNS&reg;.</p>");
		msg.append("<p>Your request has been submitted to:</p>");
		
		msg.append("<p style='margin-left:40px;'>FASTSIGNS of ").append(dealer.getLocationName()).append("<br/>");
		msg.append(dealer.getAddress()).append("<br/>");
		if (dealer.getAddress2() != null && dealer.getAddress2().length() > 0) 
			msg.append(dealer.getAddress2()).append("<br/>");
		msg.append(dealer.getCity()).append(", ").append(dealer.getState()).append(" ").append(dealer.getZipCode()).append("<br/>");
		msg.append(phone.getFormattedNumber()).append("<br/>");
		msg.append(dealer.getEmailAddress()).append("</p>");
		
		msg.append("<p>FASTSIGNS of ").append(dealer.getLocationName());
		msg.append(" will contact you to discuss your visual communications needs and challenges.  You will receive a one-question survey in seven business days asking you about your experience.</p>");
		
		msg.append("<p>In the meantime, if you would like to learn more about the comprehensive ");
		msg.append("solutions that FASTSIGNS provides, download or view our guide online at ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf\">www.fastsigns.com/binary/org/FTS/PDF/CSG-2013_2.pdf</a> ");
		msg.append("or visit the FASTSIGNS online <a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter\">Learning Center</a> to access our ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/WhitePapers\">white papers</a>, and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/DesignTips\">helpful tips</a> and ");
		msg.append("<a target=\"_blank\" href=\"http://www.fastsigns.com/LearningCenter/SignInformation\">information</a>.</p>");
		
		msg.append("<p>Thank you for contacting FASTSIGNS.</p>");
		msg.append("<p><br/>Each FASTSIGNS&reg; location is independently owned and operated.</p><br/>");
		
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
			mail.setSubject("Thank you for your consultation request from FASTSIGNS");
			mail.setFrom("consultation@fastsigns.com");
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
