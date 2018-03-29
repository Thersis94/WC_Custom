package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.InvitationVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
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
 ****************************************************************************/
public class RezdoxEmailAFriendAction extends EmailFriendAction{
	
	private static final String MEMBER_NAME = "memberName";
	private static final String REQ_RECIPIENT_EMAIL = "rcptEml";

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override 
	public void build(ActionRequest req)  throws ActionException {
		log.debug("reqbuild in action called");
		
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		// Handle logic specific to the email a friend widget instance
		String emailType = req.getParameter("emailType");
		if ("invitation".equals(emailType)) {
			sendInvitation(member, req);
		} else {
			referBusiness(member, req);
		}
	}
	
	/**
	 * Mananges getting data required for sending a business referall
	 * 
	 * @param member
	 * @param req
	 * @throws ActionException
	 */
	protected void referBusiness(MemberVO member, ActionRequest req) throws ActionException {
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
		emailData.put(MEMBER_NAME, member.getFullName());
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
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	protected void sendInvitation(MemberVO member, ActionRequest req) throws ActionException {
		if (req.hasParameter("statusFlag")) {
			updateInvitation(member, req);
		} else {
			saveInvitation(member, req);
		}
	}
	
	/**
	 * Saves a new invitation, sends the email
	 * 
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	protected void saveInvitation(MemberVO member, ActionRequest req) throws ActionException {
		// Check if member has invited this email before, if so, then don't proceed
		List<InvitationVO> invitations = retrieveInvitations(req);
		if (!invitations.isEmpty()) {
			return;
		}
		
		// Set the data on the VO
		InvitationVO invite = new InvitationVO(req);
		invite.setMemberId(RezDoxUtils.getMemberId(req));
		invite.setStatusFlag(InvitationVO.Status.SENT.getCode());
		invite.setEmailAddressText(req.getParameter(REQ_RECIPIENT_EMAIL));
		
		// Save the invitation record
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(invite);
		} catch(Exception e) {
			log.error("Could not save invitation", e);
		}
		putModuleData(invite.getInvitationId(), 1, false);
		
		// Send the email
		Map<String, Object> emailData = new HashMap<>();
		emailData.put(MEMBER_NAME, member.getFullName());
		sendEmail(req, emailData);
	}
	
	/**
	 * Updates the invitation status, sending the email only if the status is "re-sent"
	 * An invitationId must be passed on the request
	 * 
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	protected void updateInvitation(MemberVO member, ActionRequest req) throws ActionException {
		String schema = getCustomSchema();
		
		// Update the status on the invitation record
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_invitation ");
		sql.append("set status_flg = ?, update_dt = current_timestamp where invitation_id = ? ");
		
		List<String> fields = Arrays.asList("status_flg", "invitation_id");
		
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), new InvitationVO(req), fields);
		} catch (Exception e) {
			throw new ActionException("Could not update status on invitation record", e);
		}
		
		// Send an email if the status is set to "re-send"
		if (Convert.formatInteger(req.getParameter("statusFlag")) == InvitationVO.Status.RESENT.getCode()) {
			InvitationVO invitation = retrieveInvitations(req).get(0);
			req.setParameter(REQ_RECIPIENT_EMAIL, invitation.getEmailAddressText());

			Map<String, Object> emailData = new HashMap<>();
			emailData.put(MEMBER_NAME, member.getFullName());
			sendEmail(req, emailData);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("json") && req.hasParameter("invitation")) {
			List<InvitationVO> invitations = retrieveInvitations(req);
			putModuleData(invitations, invitations.size(), false);
		} else {
			super.retrieve(req);
		}
	}
	
	/**
	 * Retrieves invitations sent by the member
	 * 
	 * @param req
	 * @return
	 */
	protected List<InvitationVO> retrieveInvitations(ActionRequest req) {
		String schema = getCustomSchema();
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("select invitation_id, email_address_txt, status_flg, create_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_invitation ");
		sql.append(DBUtil.WHERE_CLAUSE).append("member_id = ? and status_flg >= ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		
		// Filter as required
		if (req.hasParameter(REQ_RECIPIENT_EMAIL)) {
			sql.append("and email_address_txt = ? ");
			params.add(InvitationVO.Status.DELETED.getCode());
			params.add(req.getParameter(REQ_RECIPIENT_EMAIL).trim());
			
		} else if (req.hasParameter("invitationId")) {
			sql.append("and invitation_id = ? ");
			params.add(InvitationVO.Status.DELETED.getCode());
			params.add(req.getParameter("invitationId"));
			
		} else {
			params.add(InvitationVO.Status.SENT.getCode());
		}

		sql.append(DBUtil.ORDER_BY).append(" coalesce(update_dt, create_dt) desc ");

		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new InvitationVO());
	}
}
