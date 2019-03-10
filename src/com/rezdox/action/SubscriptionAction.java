package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.PromotionVO;
import com.rezdox.vo.SubscriptionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SubscriptionAction.java<p/>
 * <b>Description: Manages member subscriptions, checks for needs to upgrade.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 7, 2018
 ****************************************************************************/
public class SubscriptionAction extends SimpleActionAdapter {

	public SubscriptionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SubscriptionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public SubscriptionAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		String memberId = member.getMemberId();

		// Check if this is a new member. New members get a free residence or a free business.
		int residenceCount = getUsageQty(memberId, Group.HO);
		int businessCount = getUsageQty(memberId, Group.BU);
		req.setAttribute("newMember", residenceCount + businessCount == 0);
		if ((boolean) req.getAttribute("newMember")) return;

		// Get the possible memberships the member can subscribe to.
		req.setParameter(MembershipAction.REQ_EXC_GROUP_CD, getMembershipExclusions(req).toArray(new String[0]), true);
		MembershipAction ma = new MembershipAction(dbConn, attributes);
		putModuleData(ma.retrieveMemberships(req));
	}

	/**
	 * Checks the member's role to determine which subscriptions they don't have access to
	 * 
	 * @param req
	 * @return
	 */
	private List<String> getMembershipExclusions(ActionRequest req) {
		List<String> exclusions = new ArrayList<>();
		SBUserRole role = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA));

		if (RezDoxUtils.REZDOX_BUSINESS_ROLE.equals(role.getRoleId())) {
			// Exclude residence memberships if this is a business-only role
			exclusions.add(Group.HO.name());
		} else if (RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId())) {
			// Exclude business memberships if this is a residence-only role
			exclusions.add(Group.BU.name());
		}

		return exclusions;
	}

	/**
	 * Checks if a member needs to purchase a subscription upgrade
	 * 
	 * @param req
	 * @return true if the member needs an upgrade, false if not
	 * @throws ActionException 
	 */
	protected boolean checkUpgrade(MemberVO member, Group membershipGroup) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean needsUpgrade = true;

		StringBuilder sql = new StringBuilder(350);
		sql.append("select sum(s.qty_no) as purchase_qty ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_subscription s inner join ");
		sql.append(schema).append("rezdox_membership m on s.membership_id = m.membership_id ");
		sql.append("where group_cd = ? and member_id = ? ");
		sql.append("group by group_cd ");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			ps.setString(++idx, membershipGroup.toString());
			ps.setString(++idx, member.getMemberId());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// Check their purchases against their usage
				int purchaseQty = rs.getInt(1);
				int usageQty = getUsageQty(member.getMemberId(), membershipGroup);
				needsUpgrade = purchaseQty - usageQty <= 0;
			} else {
				// No purchases were found (free or otherwise), add the free one they get for signing up.
				MembershipAction ma = new MembershipAction(dbConn, attributes);
				PromotionAction pa = new PromotionAction(dbConn, attributes);
				addSubscription(member, ma.retrieveDefaultMembership(membershipGroup), pa.retrieveFreePromotion());
				needsUpgrade = false;
			}
		} catch (SQLException e) {
			log.error("Unable to validate subscription purchase qty. ", e);
		}

		return needsUpgrade;
	}

	/**
	 * Gets a member's subscription usage for a given membership type.
	 * NOTE: This is different than the total subscriptions paid for.
	 * 
	 * @param memberId
	 * @param membershipGroup
	 * @return
	 * @throws ActionException 
	 */
	protected int getUsageQty(String memberId, Group membershipGroup) throws ActionException {
		switch (membershipGroup) {
			case HO:
				return getResidenceUsage(memberId, ResidenceAction.STATUS_ACTIVE);
			case BU: 
				return getBusinessUsage(memberId, BusinessStatus.ACTIVE.getStatus(), BusinessStatus.PENDING.getStatus());
			case CO: 
				return getConnectionUsage(memberId);
			default:
				throw new ActionException("Unsupported membership group type.");
		}
	}

	/**
	 * Checks a member's usage of residence subscriptions
	 * 
	 * @param memberId
	 * @return
	 */
	protected int getResidenceUsage(String memberId, Integer... statuses) {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(150);
		sql.append("select count(residence_id) as usage_qty from ").append(schema);
		sql.append("rezdox_residence_member_xr where member_id=? and status_flg in ("); 
		DBUtil.preparedStatmentQuestion(statuses.length, sql); 
		sql.append(")");

		int usageQty = 0;
		int x=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++x, memberId);
			for (Integer sts : statuses)
				ps.setInt(++x, sts);

			ResultSet rs = ps.executeQuery();
			if (rs.next())
				usageQty = rs.getInt(1);

		} catch (SQLException e) {
			log.error("Unable to validate member residence usage. ", e);
		}

		return usageQty;
	}

	/**
	 * Checks a member's usage of business subscriptions
	 * 
	 * @param memberId
	 * @return
	 */
	protected int getBusinessUsage(String memberId, Integer... statuses) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select count(business_id) as usage_qty from ").append(schema);
		sql.append("rezdox_business_member_xr where member_id = ? and status_flg in (");
		DBUtil.preparedStatmentQuestion(statuses.length, sql);
		sql.append(")");
		log.debug(sql);

		int usageQty = 0;
		int x=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++x, memberId);
			for (Integer sts : statuses)
				ps.setInt(++x, sts);

			ResultSet rs = ps.executeQuery();
			if (rs.next())
				usageQty = rs.getInt(1);

		} catch (SQLException e) {
			log.error("Unable to validate member business usage. ", e);
		}

		return usageQty;
	}

	/**
	 * Checks a member's usage of connection subscriptions
	 * 
	 * @param memberId
	 * @return
	 */
	private int getConnectionUsage(String memberId) {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(400);
		sql.append("select count(*) as usage_qty from ").append(schema).append("rezdox_connection c ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_member_xr sbm on c.sndr_business_id = sbm.business_id and sbm.status_flg = 1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_member_xr rbm on c.rcpt_business_id = rbm.business_id and rbm.status_flg = 1 ");
		sql.append(DBUtil.WHERE_CLAUSE).append("approved_flg = 1 and (sndr_member_id = ? or rcpt_member_id = ? or sbm.member_id = ? or rbm.member_id = ?) ");

		int usageQty = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, memberId);
			ps.setString(2, memberId);
			ps.setString(3, memberId);
			ps.setString(4, memberId);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				usageQty = rs.getInt(1);
			}
		} catch (SQLException e) {
			log.error("Unable to validate member connection usage. ", e);
		}

		return usageQty;
	}

	/**
	 * Adds a membership subscription for a given member
	 * 
	 * @param member
	 * @param membership
	 * @param promotion
	 * @throws ActionException 
	 */
	public void addSubscription(MemberVO member, MembershipVO membership, PromotionVO promotion) throws ActionException {
		SubscriptionVO subscription = new SubscriptionVO();
		subscription.setMember(member);
		subscription.setMembership(membership);
		subscription.setPromotion(promotion);
		subscription.setCostNo(membership.getCostNo());
		subscription.setDiscountNo(membership.getCostNo() * promotion.getDiscountPctNo() * -1);
		subscription.setQuantityNo(membership.getQuantityNo());

		// Save the member's subscription
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		try {
			dbp.save(subscription);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}