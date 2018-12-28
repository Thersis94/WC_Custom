package com.rezdox.action;

import java.util.HashMap;
import java.util.Map;

import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: RezdoxEmailAFriend.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extends core email a friend action to include loading the Business' profile data, to include in the email.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 19, 2018
 * @updates:
 * 	TJ - Mar 28, 2018: Added invitation support.
 ****************************************************************************/
public class RezdoxEmailAFriendAction extends EmailFriendAction {

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override 
	public void build(ActionRequest req)  throws ActionException {
		// Handle logic specific to the type of email a friend
		if ("invitation".equals(req.getParameter("emailType"))) {
			sendInvitation(req);
		} else {
			referBusiness(req);
		}
	}

	/**
	 * Mananges getting data required for sending a business referall
	 * 
	 * @param req
	 * @throws ActionException
	 */
	protected void referBusiness(ActionRequest req) throws ActionException {
		MemberVO member = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		BusinessVO bvo = new BusinessVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.getByPrimaryKey(bvo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("could not load business data", e);
		}
		log.debug("business: " + bvo);

		//default email a friend only gets the name from the form
		Map<String, Object> emailData = new HashMap<>();
		emailData.put("senderName", member.getFullName());
		emailData.put("businessName", bvo.getBusinessName());
		emailData.put("businessId", bvo.getBusinessId());
		sendEmail(req, emailData);

		//notify the business owner
		notifyBusinessOwner(bvo, req);
	}


	/**
	 * Put a browser notification up for the business owner(s), so they see this good news
	 * @param bvo
	 */
	private void notifyBusinessOwner(BusinessVO vo, ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		String[] profileIds = notifyUtil.getProfileIds(vo.getBusinessId(), false);

		//quit while we're ahead if there's nobody to inform
		if (profileIds == null || profileIds.length == 0) return;

		notifyUtil.send(Message.REFFERAL, null, null, profileIds);
	}


	/**
	 * Sends an email with the given email parameters
	 * 
	 * @param req
	 * @param emailData
	 * @throws ActionException
	 */
	private void sendEmail(ActionRequest req, Map<String, Object> emailData) throws ActionException {
		attributes.put(EmailFriendAction.MESSAGE_DATA_MAP, emailData);
		super.build(req);
	}

	/**
	 * Mananges data & logging required when sending an invitation to join RezDox
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void sendInvitation(ActionRequest req) throws ActionException {
		InvitationAction ia = new InvitationAction(dbConn, attributes);
		Map<String, Object> emailData = ia.saveInvite(req);
		if (!emailData.isEmpty()) {
			sendEmail(req, emailData);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("json") && req.hasParameter("invitation")) {
			InvitationAction ia = new InvitationAction(dbConn, attributes);
			ia.retrieve(req);
		} else {
			super.retrieve(req);
		}
	}
}
