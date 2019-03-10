package com.rezdox.action;

//Java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
//WC Custom
import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.action.RezDoxUtils.EmailSlug;
import com.rezdox.vo.MemberVO;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.sb.email.vo.EmailRecipientVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/***************************************************************************
 * <p><b>Title:</b> SharingAction.java</p>
 * <p><b>Description:</b> Governs sharing of Residences and Businesses between members.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jul 20, 2018
 * <b>Changes:</b>
 ***************************************************************************/
public class SharingAction extends SimpleActionAdapter {

	public SharingAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SharingAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public SharingAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/** 
	 * List people I've shared 'this' residence or business with.
	 * Note "Who can I share this with?" gets proxied over to the ConnectionsAction.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("shares")) {
			putModuleData(loadMembers(req));

		} else if (req.hasParameter("search")) {
			String lookup = req.getParameter("sendingId"); //stuffing businessId into sendingId reuses a bunch of JS code.
			if (!StringUtil.isEmpty(lookup)) {
				//find users connected to this business to share it with.
				lookup = "b_" + lookup;
			} else {
				//find users connected to me that I can share my Residence with.
				lookup = "m_" + RezDoxUtils.getMemberId(req);
			}
			ConnectionAction ca = new ConnectionAction(getDBConnection(), getAttributes());
			putModuleData(ca.searchMembers(req.getParameter("search"), lookup, true, false));
		}
	}


	/**
	 * Revokes shared, or creates it and posts notification/email accordingly
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("revoke")) {
			deshareResource(req);
			downgradeUserRole(req);
		} else {
			shareResource(req);
			verifyUserRole(req);
			notifyReciever(req);
		}
	}


	/**
	 * When revoking a shared resource, downgrade the user from hybrid to residencial 
	 * if they no longer have any shared businesses.
	 * @param req
	 */
	private void downgradeUserRole(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String profileId = req.getParameter(MemberAction.REQ_PROFILE_ID);
		String memberId = req.getParameter(MemberAction.REQ_MEMBER_ID);
		String downgradedRoleId = null;
		SubscriptionAction sa = new SubscriptionAction(getDBConnection(), getAttributes());
		try {
			int count = 0;
			if (req.hasParameter(BusinessAction.REQ_BUSINESS_ID)) {
				downgradedRoleId = RezDoxUtils.REZDOX_RESIDENCE_ROLE; //from hybrid to residence - if they have no more buisnesses
				count = sa.getBusinessUsage(memberId, BusinessStatus.ACTIVE.getStatus(), BusinessStatus.PENDING.getStatus(), BusinessStatus.SHARED.getStatus());
			} else {
				downgradedRoleId = RezDoxUtils.REZDOX_BUSINESS_ROLE; //from hybrid to business - if they have no more residences
				count = sa.getResidenceUsage(memberId, ResidenceAction.STATUS_ACTIVE, ResidenceAction.STATUS_SHARED);
			}

			if (count == 0) {
				ProfileRoleManager prm = new ProfileRoleManager();
				//don't change a role that isn't specifically in the hybrid state
				SBUserRole role = prm.getRole(profileId, StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId()), RezDoxUtils.REZDOX_RES_BUS_ROLE, null, dbConn);
				if (!StringUtil.isEmpty(role.getProfileRoleId())) {
					role.setRoleId(downgradedRoleId);
					prm.addRole(role, dbConn);
				}
			}

		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
		}
	}


	/**
	 * Confirm the recieving user's role permits them to see the asset we've just shared with them.
	 * If not, move them to the hybrid role so they can see what they had before plus this.
	 * @param req
	 */
	private void verifyUserRole(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String profileId = req.getParameter(MemberAction.REQ_PROFILE_ID);
		String requiredRoleId = req.hasParameter(BusinessAction.REQ_BUSINESS_ID) ? 
				RezDoxUtils.REZDOX_BUSINESS_ROLE : RezDoxUtils.REZDOX_RESIDENCE_ROLE;

		//get the users current roleId
		ProfileRoleManager prm = new ProfileRoleManager();
		try {
			//get the users current role (parent siteId - holds user accounts)
			SBUserRole role = prm.getRole(profileId, StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId()), null, null, dbConn);

			//if the user's current roleId isn't the one we need them to have, move them into the Hybrid role - which gives them both.
			if (!requiredRoleId.equals(role.getRoleId())) {
				role.setRoleId(RezDoxUtils.REZDOX_RES_BUS_ROLE);
				prm.addRole(role, dbConn);
			}

		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Delete a sharing _xr record to break the bond
	 * @param req
	 * @param isBusiness
	 */
	private void deshareResource(ActionRequest req) {
		String pkId;
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.DELETE_CLAUSE).append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		if (req.hasParameter(BusinessAction.REQ_BUSINESS_ID)) {
			pkId = req.getParameter(BusinessAction.REQ_BUSINESS_ID);
			sql.append("REZDOX_BUSINESS_MEMBER_XR where business_member_xr_id=?");
		} else {
			pkId = req.getParameter(ResidenceAction.RESIDENCE_ID);
			sql.append("REZDOX_RESIDENCE_MEMBER_XR where residence_member_xr_id=?");
		}
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, pkId);
			int cnt = ps.executeUpdate();
			log.debug(String.format("deleted %d sharing records", cnt));

		} catch (SQLException sqle) {
			log.error("could not delete sharing record", sqle);
		}
	}


	/**
	 * Create a sharing _xr record between the given user and resource
	 * @param req
	 * @param isBusiness
	 */
	private void shareResource(ActionRequest req) throws ActionException {
		String resourceId;
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.INSERT_CLAUSE).append(getCustomSchema());
		if (req.hasParameter(BusinessAction.REQ_BUSINESS_ID)) {
			resourceId = req.getParameter(BusinessAction.REQ_BUSINESS_ID);
			sql.append("REZDOX_BUSINESS_MEMBER_XR (business_member_xr_id, member_id, ");
			sql.append("business_id, status_flg, create_dt) values (?,?,?,?,?)");
		} else {
			resourceId = req.getParameter(ResidenceAction.RESIDENCE_ID);
			sql.append("REZDOX_RESIDENCE_MEMBER_XR (residence_member_xr_id, member_id, ");
			sql.append("residence_id, status_flg, create_dt) values (?,?,?,?,?)");
		}
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter(MemberAction.REQ_MEMBER_ID));
			ps.setString(3, resourceId);
			ps.setInt(4, 2);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			int cnt = ps.executeUpdate();
			log.debug(String.format("created %d sharing records", cnt));

		} catch (SQLException sqle) {
			throw new ActionException("could not create sharing record", sqle);
		}
	}


	/**
	 * Return a list of member I've shared this resource with.
	 * @param req
	 * @return
	 */
	private List<MemberVO> loadMembers(ActionRequest req) {
		boolean isBusiness = req.hasParameter(BusinessAction.REQ_BUSINESS_ID);
		String resourceId = isBusiness ? req.getParameter(BusinessAction.REQ_BUSINESS_ID) : req.getParameter(ResidenceAction.RESIDENCE_ID);
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select m.member_id, m.profile_id, m.first_nm, m.last_nm, m.email_address_txt, ");
		sql.append("m.profile_pic_pth, pa.city_nm, pa.state_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_MEMBER m ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE_ADDRESS pa on m.profile_id=pa.profile_id ");
		if (isBusiness) {
			sql.insert(7, "bm.business_member_xr_id as register_submittal_id, 1 as status_flg, ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR bm on m.member_id=bm.member_id ");
			sql.append("and bm.status_flg=2 and bm.business_id=? ");
		} else {
			sql.insert(7, "rm.residence_member_xr_id as register_submittal_id, 2 as status_flg, ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on m.member_id=rm.member_id ");
			sql.append("and rm.status_flg=2 and rm.residence_id=? ");
		}
		sql.append("where m.email_address_txt is not null ");
		sql.append("order by m.last_nm, m.first_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(resourceId), new MemberVO());
	}


	/**
	 * Post a website notification to the member who recieved the share, so they know.
	 * Trigger an email to them as well.
	 * @param req
	 */
	private void notifyReciever(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		boolean isBusiness = req.hasParameter(BusinessAction.REQ_BUSINESS_ID);
		MemberVO sender = RezDoxUtils.getMember(req);
		String targetMemberId = req.getParameter(MemberAction.REQ_MEMBER_ID);
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("senderName", sender.getFirstName() + " " + sender.getLastName());
		dataMap.put("firstName", req.getParameter("firstName"));
		dataMap.put("lastName", req.getParameter("lastName"));
		if (isBusiness) {
			dataMap.put("businessName", req.getParameter("businessName"));
		} else {
			dataMap.put("residenceName", req.getParameter("residenceName"));
		}

		//post website notification
		Message msg = isBusiness ? Message.SHARED_BUSINESS : Message.SHARED_RESIDENCE;
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		notifyUtil.sendToMember(msg, dataMap, null, targetMemberId);

		//send email too
		sendEmail(req, isBusiness, dataMap);
	}


	/**
	 * Send an email to the member who recieved the share, so they know
	 * @param req
	 */
	private void sendEmail(ActionRequest req, boolean isBusiness, Map<String, Object> dataMap) {
		if (!StringUtil.isValidEmail(req.getParameter("emailAddress"))) 
			return; //quit before things get ugly.  This should never happen though.

		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(req.getParameter(MemberAction.REQ_PROFILE_ID), req.getParameter("emailAddress"), EmailRecipientVO.TO));

		EmailCampaignBuilderUtil emailer = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		EmailSlug slug = isBusiness ? EmailSlug.BUSINESS_SHARED : EmailSlug.RESIDENCE_SHARED;
		emailer.sendMessage(dataMap, rcpts, slug.name());
	}
}