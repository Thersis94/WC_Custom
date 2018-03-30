package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.InvitationVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: InvitationAction.java<p/>
 * <b>Description: Manages invitations sent by a member to a potential new member.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 29, 2018
 ****************************************************************************/
public class InvitationAction extends SBActionAdapter {
	
	private static final String REQ_RECIPIENT_EMAIL = "rcptEml";
	private Map<String, Object> emailData;

	public InvitationAction() {
		super();
		emailData = new HashMap<>();
	}

	/**
	 * @param actionInit
	 */
	public InvitationAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public InvitationAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override 
	public void build(ActionRequest req)  throws ActionException {
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		if (req.hasParameter("statusFlag")) {
			updateInvitation(member, req);
		} else {
			saveInvitation(member, req);
		}
	}
	
	/**
	 * Saves a new invitation, builds params that can be used for email sending
	 * 
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	protected void saveInvitation(MemberVO member, ActionRequest req) {
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
		
		// Put params onto the email data map
		emailData.put("memberName", member.getFullName());
	}
	
	/**
	 * Updates the invitation status, building email params only if the status is "re-sent".
	 * An invitationId must be passed on the request.
	 * 
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	protected void updateInvitation(MemberVO member, ActionRequest req) throws ActionException {
		// Update the status on the invitation record
		updateStatus(new InvitationVO(req));
		
		// Build email params if the status is set to "re-send"
		if (Convert.formatInteger(req.getParameter("statusFlag")) == InvitationVO.Status.RESENT.getCode()) {
			InvitationVO invitation = retrieveInvitations(req).get(0);
			req.setParameter(REQ_RECIPIENT_EMAIL, invitation.getEmailAddressText());
			emailData.put("memberName", member.getFullName());
		}
	}
	
	/**
	 * Updates the status for an invitation.
	 * Must pass the invitationId and statusFlg in the VO.
	 * 
	 * @param invite
	 * @throws ActionException
	 */
	private void updateStatus(InvitationVO invite) throws ActionException {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_invitation ");
		sql.append("set status_flg = ?, update_dt = current_timestamp where invitation_id = ? ");
		
		List<String> fields = Arrays.asList("status_flg", "invitation_id");
		
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), invite, fields);
		} catch (Exception e) {
			throw new ActionException("Could not update status on invitation record", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<InvitationVO> invitations = retrieveInvitations(req);
		putModuleData(invitations, invitations.size(), false);
	}
	
	/**
	 * Gets the base query for returning invitation data
	 * 
	 * @return
	 */
	private StringBuilder getBaseRetrieveSql() {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(200);
		sql.append("select invitation_id, member_id, email_address_txt, status_flg, create_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_invitation ");
		
		return sql;
	}

	/**
	 * Retrieves invitations sent by the member
	 * 
	 * @param req
	 * @return
	 */
	protected List<InvitationVO> retrieveInvitations(ActionRequest req) {
		StringBuilder sql = getBaseRetrieveSql();
		sql.append(DBUtil.WHERE_CLAUSE).append("member_id = ? and status_flg >= ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		
		// Filter as required
		if (req.hasParameter(REQ_RECIPIENT_EMAIL)) {
			sql.append("and lower(email_address_txt) = ? ");
			params.add(InvitationVO.Status.DELETED.getCode());
			params.add(req.getParameter(REQ_RECIPIENT_EMAIL).trim().toLowerCase());
			
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
	
	/**
	 * Checks the current user against the invitation records,
	 * and gives rewards to the person(s) who invited them.
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	public void applyInviterRewards(ActionRequest req, String rewardSlug) throws ActionException {
		List<InvitationVO> invites = retrieveRewardInvites(RezDoxUtils.getMember(req).getEmailAddress());
		
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		for (InvitationVO invite : invites) {
			invite.setStatusFlag(InvitationVO.Status.JOINED.getCode());
			updateStatus(invite);
			
			ra.applyReward(rewardSlug, invite.getMemberId());
		}
	}

	/**
	 * Retrieves list of invites to reward members for inviting the given email address
	 * 
	 * @param emailAddress
	 * @return
	 */
	private List<InvitationVO> retrieveRewardInvites(String emailAddress) {
		StringBuilder sql = getBaseRetrieveSql();
		sql.append(DBUtil.WHERE_CLAUSE).append("lower(email_address_txt) = ?  ");
		
		List<Object> params = new ArrayList<>();
		params.add(emailAddress.trim().toLowerCase());
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new InvitationVO());
	}

	/**
	 * @return the emailData
	 */
	public Map<String, Object> getEmailData() {
		return emailData;
	}
}
