package com.fastsigns.action.tvspot;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.action.contact.ContactVO;
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
 * @Updates
 * 	JM 07.01.14
 * 		refactored to ".tvspot" package.  Added TVSpotConfig interface for country abstraction.
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
		

//		if (req.hasParameter("reprocessEmails")) {
//			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
//			TVSpotConfig config =TVSpotConfig.getInstance(site.getCountryCode());
//			req.setParameter("reprocessDealerKey", config.getDealerLocnField());
//			ReprocessConsultationRequestEmails rep = new ReprocessConsultationRequestEmails(this.actionInit);
//			rep.setDBConnection(dbConn);
//			rep.setAttributes(attributes);
//			rep.procEmails(req);
//			rep = null;
//			return;
//		}
		
		if (req.hasParameter("isSurvey")) return; //we don't need anything addtl to render this.
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String contactActGrpId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		req.setParameter(Constants.ACTION_GROUP_ID, contactActGrpId);
		actionInit.setActionId(contactActGrpId);
		
		SMTActionInterface cfa = new ContactFacadeAction(actionInit);
		cfa.setAttributes(attributes);
		cfa.setDBConnection(dbConn);
		cfa.retrieve(req);
		ModuleVO m = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.addCacheGroup(((ContactVO)m.getActionData()).getActionId());
	}

	
	/**
	 * submit the contact us portlet, then process the custom outgoing emails.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		TVSpotConfig config =TVSpotConfig.getInstance(site.getCountryCode());
		
		// this is a hook to load the dealer locations dropdown.  Invoked via PublicAjaxServlet
		if (req.hasParameter("view")) {
			listDealerLocations(mod, req);
			return;
			
		} else if (req.hasParameter("contactSubmittalId") && req.hasParameter("isSurvey")) {
			//these are the feedback surveys.  Update the existing record with the data for these two contact fields.
			try {
				updateSurvey(config, req);
			} catch (SQLException sqle) {
				log.error("could not update survey response", sqle);
				//nothing we can do about this, so let the user continue on unbeknownst.
			}
			return;
		}
		
		//load the dealer info using the dealerLocationId passed on the request
		//remove the "dlr-" prefix we needed when we built the dropdown (ensured proper ordering, by distance)
		//only if it is present.  If this is coming in through a reprocess the dlr- will not be present.
		String dealerId;
		if (req.getParameter(config.getDealerLocnField()).contains("dlr-")) {
			dealerId = req.getParameter(config.getDealerLocnField()).substring(4);
		} else {
			dealerId = req.getParameter(config.getDealerLocnField());
		}
		req.setParameter(config.getDealerLocnField(),  dealerId, true);
		DealerLocationVO dealer = loadDesiredDealer(config, req);
		
		//the contact us portlet will send the email to the FranchiseOwner and Center for us;
		//configure the email as needed before we invoke that class.
		addCenterEmailParamsToReq(config, req, dealer);
		
		//invoke the Contact Us portlet to save the data and send the Franchise email
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		SMTActionInterface cfa = new ContactFacadeAction(actionInit);
		cfa.setAttributes(attributes);
		cfa.setDBConnection(dbConn);
		cfa.build(req);
		
		//send a confirmation email to the user
		emailUserConfirmation(config, req, dealer);
		
		//the browser redirect occurring here was setup for us by the ContactSubmittalAction
	}
	
	
	/**
	 * updates the users original contact submittal and completes the two empty
	 * fields for feedback and rating.  These are asked on a survey attached to an email campaign.
	 * @param req
	 * @throws SQLException
	 */
	private void updateSurvey(TVSpotConfig config, SMTServletRequest req) throws SQLException {
		SubmittalDataAction sda = new SubmittalDataAction(actionInit);
		sda.setAttributes(attributes);
		sda.setDBConnection(dbConn);
		String csi = req.getParameter("contactSubmittalId");
		
		//update survey rating
		String key = config.getContactId(ContactField.rating);
		//log.debug("rating=" + req.getParameter(CON_ + key));
		sda.updateField(req.getParameter(TVSpotConfig.CON_ + key), csi, key);
		
		//update survey feedback
		key = config.getContactId(ContactField.feedback);
		//log.debug("feedback=" + req.getParameter(CON_ + key));
		sda.updateField(req.getParameter(TVSpotConfig.CON_ + key), csi, key);
		
		//update whether the consultation has been completed or not
		key = config.getContactId(ContactField.consultation);
		//log.debug("complete=" + req.getParameter(CON_ + key));
		sda.updateField(req.getParameter(TVSpotConfig.CON_ + key), csi, key);
		
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
	private DealerLocationVO loadDesiredDealer(TVSpotConfig config, SMTServletRequest req) throws ActionException {
		DealerLocatorAction dla = new DealerLocatorAction(actionInit);
		dla.setAttributes(attributes);
		dla.setDBConnection(dbConn);
		List<DealerLocationVO> dealers = null;
		try {
			dealers = dla.getDealerInfo(req, new String[] { req.getParameter(config.getDealerLocnField()) }, null);
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
	private void addCenterEmailParamsToReq(TVSpotConfig config, SMTServletRequest req, DealerLocationVO dealer) {
		//email message recipients
		Set<String> emails = new HashSet<String>();
		if (dealer.getEmailAddress() != null && dealer.getEmailAddress().contains(",")) {
			emails.addAll(Arrays.asList(dealer.getEmailAddress().split(",")));
		} else if (StringUtil.isValidEmail(dealer.getEmailAddress())) {
			emails.add(dealer.getEmailAddress());
		}
		
		if (dealer.getOwnerEmail() != null && dealer.getOwnerEmail().contains(",")) {
			emails.addAll(Arrays.asList(dealer.getOwnerEmail().split(",")));
		} else if (StringUtil.isValidEmail(dealer.getOwnerEmail())) {
			emails.add(dealer.getOwnerEmail());
		}
		
		req.setParameter("contactEmailAddress", emails.toArray(new String[emails.size()]), true);
		req.setAttribute("senderEmail", config.getDefaultSenderEmail());
		req.setParameter("dealerLocationId", req.getParameter(config.getDealerLocnField()));
		
		//set the status to 'initiated'
		req.setParameter(TVSpotConfig.CON_ + config.getContactId(ContactField.status), Status.initiated.toString());
		
		//set the users state based on their zip code
		String state = getStateFromZip(config, req);
		req.setParameter(TVSpotConfig.CON_ + config.getContactId(ContactField.state), state);
		
		//email header from country implentation
		req.setParameter("contactEmailHeader", config.getDealerEmailHeader());
		
		//email footer from country implentation
		req.setParameter("contactEmailFooter", config.getDealerEmailFooter());
		
		//email subject from country implentation
		req.setValidateInput(false);
		req.setParameter("contactEmailSubject", config.getDealerEmailSubject(req.getParameter("pfl_combinedName")));
		req.setValidateInput(true);
	}
	
	
	/**
	 * Does a quick geocode for the user's state using the passed zip code.
	 * Assumes country code to be that of the Site, since it's not asked of the user..
	 * @param zip
	 * @return
	 */
	private String getStateFromZip(TVSpotConfig config, SMTServletRequest req) {
		Location loc = null;
		DealerLocatorAction dla = new DealerLocatorAction();
		dla.setAttributes(attributes);
		
		try {
			req.setParameter("zip", req.getParameter(TVSpotConfig.CON_ + config.getContactId(ContactField.zipcode)));
			req.setParameter("country", config.getCountryCode());
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
	private void emailUserConfirmation(TVSpotConfig config, SMTServletRequest req, DealerLocationVO dealer) {
		try {
			EmailMessageVO msg = config.buildUserConfirmationEmail(req, dealer);
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(msg);
		} catch (Exception e) {
			log.error("could not send email to user", e);
		}
	}

	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
