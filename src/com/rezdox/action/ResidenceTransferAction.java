package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <p><b>Title</b>: ResidenceTransferAction.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 30, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class ResidenceTransferAction extends SimpleActionAdapter {

	private static final String RES_NAME = "resName";


	public ResidenceTransferAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ResidenceTransferAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public ResidenceTransferAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/**
	 * Send notification email to the recipient to initiate the process.
	 * This is phase 1 of the transfer.
	 * @param req
	 */
	protected void initateResidenceTransfer(ActionRequest req) {
		MemberVO sender = RezDoxUtils.getMember(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		int cnt = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(getInitiateSql())) {
			ps.setInt(1, ResidenceAction.STATUS_INACTIVE);
			ps.setString(2, req.getParameter(ResidenceAction.RESIDENCE_ID));
			ps.setString(3,  sender.getMemberId()); //cross-check to make sure I'm authorized to enact on this residence
			cnt = ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not disable member's residence access", sqle);
		}

		// If the above query doesn't update any rows then stop processing.  
		// (Most likely this is a malicious request & we should ignore it.)
		if (cnt == 0) return;

		//load the recipient member.  Done here and not passed on the request for security reasons.
		MemberAction ma = new MemberAction(getDBConnection(), getAttributes());
		MemberVO rcpt = ma.retrieveMemberData(req.getParameter("toMemberId"), null);

		sendInitiateEmail(req, sender, rcpt);

		//post a notification to the recipient
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), null);
		notifyUtil.send(RezDoxNotifier.Message.RESIDENCE_TRANS_PENDING, null, null, rcpt.getProfileId());

		sendRedirect(req);
	}


	/**
	 * finish the transfer of a residence to the new member.
	 * This is phase 2 of the transaction - the recipient has proved their identity already.
	 * @param req
	 */
	protected void transferResidence(ActionRequest req) {
		//verify the user logged in matches the desired target (of the email)
		if (!RezDoxUtils.getMemberId(req).equals(req.getParameter("memberId")))
			return;

		String residenceId = req.getParameter("residenceId");
		ResidenceAction ra = new ResidenceAction(getDBConnection(), getAttributes());
		try {
			ra.saveResidenceMemberXR(req, true);
		} catch (DatabaseException de) {
			log.error("could not create new residence_member_xr", de);
		}

		//detach any personal items/inventory from the old residence.  (They'll still remain with the owner)
		InventoryAction ia = new InventoryAction(getDBConnection(), getAttributes());
		ia.detachResidence(residenceId);

		//need the residence address for the email
		String schema = getCustomSchema();
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		String sql = StringUtil.join("select residence_nm as key from ", schema, "REZDOX_RESIDENCE where residence_id=?");
		List<GenericVO> data = db.executeSelect(sql, Arrays.asList(residenceId), new GenericVO());
		if (!data.isEmpty())
			req.setParameter(RES_NAME, (String) data.get(0).getKey());

		//load the recipient member.  Done here and not passed on the request for security reasons.
		MemberAction ma = new MemberAction(getDBConnection(), getAttributes());
		MemberVO prevOwner = ma.retrieveMemberData(req.getParameter("senderId"), null);

		sendCompleteEmail(req, prevOwner, RezDoxUtils.getMember(req));

		//post a notification to the recipient
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), null);
		notifyUtil.send(RezDoxNotifier.Message.RESIDENCE_TRANS_COMPLETE, null, null, prevOwner.getProfileId());

		sendRedirect(req);
	}


	/**
	 * 
	 * @param req
	 * @param preOwner - Yee who use to live there
	 * @param newOwner - Yee who lives there now
	 */
	private void sendInitiateEmail(ActionRequest req, MemberVO preOwner, MemberVO newOwner) {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("senderName", preOwner.getFirstName() + " " + preOwner.getLastName());
		dataMap.put("address", StringEncoder.urlDecode(req.getParameter(RES_NAME)));
		dataMap.put("memberId", newOwner.getMemberId());
		dataMap.put("senderId", preOwner.getMemberId());
		dataMap.put(ResidenceAction.RESIDENCE_ID, req.getParameter(ResidenceAction.RESIDENCE_ID));

		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(newOwner.getProfileId(), newOwner.getEmailAddress(), EmailRecipientVO.TO));

		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		util.sendMessage(dataMap, rcpts, RezDoxUtils.EmailSlug.TRANSFER_WAITING.name());
	}


	/**
	 * 
	 * @param req
	 * @param preOwner - Yee who use to live there
	 * @param newOwner - Yee who lives there now
	 */
	private void sendCompleteEmail(ActionRequest req, MemberVO preOwner, MemberVO newOwner) {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("rcptName", newOwner.getFirstName() + " " + newOwner.getLastName());
		dataMap.put("address", StringEncoder.urlDecode(req.getParameter(RES_NAME)));

		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(preOwner.getProfileId(), preOwner.getEmailAddress(), EmailRecipientVO.TO));

		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		util.sendMessage(dataMap, rcpts, RezDoxUtils.EmailSlug.TRANSFER_COMPLETE.name());
	}


	/**
	 * Returns the SQL for initiating transfer (by suspecting the active user's access to this residence)
	 * @return
	 */
	private String getInitiateSql() {
		// Make sure the user is authorized to make this transfer by disabling their access to the residence.
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR ");
		sql.append("set status_flg=?, update_dt=CURRENT_TIMESTAMP where residence_id=? ");
		sql.append("and residence_id in (select residence_id from ").append(schema);
		sql.append("REZDOX_RESIDENCE_MEMBER_XR where member_id=? and status_flg=1)");
		log.debug(sql);
		return sql.toString();
	}


	/**
	 * redirect the browser after this action completes
	 * @param req
	 */
	private void sendRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		sendRedirect(page.getFullPath(), null, req);
	}
}
