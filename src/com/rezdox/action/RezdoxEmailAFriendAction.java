package com.rezdox.action;

import java.util.HashMap;
import java.util.Map;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: RezdoxEmailAFriend.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extends core email a friend action to
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 19, 2018
 * @updates:
 * 	TJ - Mar 28, 2018: Added invitation support.
 ****************************************************************************/
public class RezdoxEmailAFriendAction extends EmailFriendAction{
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override 
	public void build(ActionRequest req)  throws ActionException {
		log.debug("reqbuild in action called");
		
		// Handle logic specific to the type of email a friend
		String emailType = req.getParameter("emailType");
		if ("invitation".equals(emailType)) {
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
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		BusinessVO bvo = new BusinessVO(req);
		String schema = getCustomSchema();
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		
		try {
			db.getByPrimaryKey(bvo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("could not load busines data ",e);
		}
		
		log.debug("business: " + bvo);
		
		Map<String, Object> emailData = new HashMap <>();
		
		//default email a friend only gets the name from the form 
		emailData.put("memberName", member.getFullName());
		emailData.put("businessName", bvo.getBusinessName());
		emailData.put("businessId", bvo.getBusinessId());
		
		sendEmail(req, emailData);
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
		ia.build(req);
		
		Map<String, Object> emailData = ia.getEmailData();
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
