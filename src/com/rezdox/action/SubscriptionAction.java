package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
import com.rezdox.action.RezDoxUtils.Product;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.PromotionVO;
import com.rezdox.vo.SubscriptionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

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
		String memberId = RezDoxUtils.getMemberId(req);

		// Check if this is a new member. New members get a free residence or a free business.
		int residenceCount = getUsageQty(memberId, Product.RESIDENCE);
		int businessCount = getUsageQty(memberId, Product.BUSINESS);
		boolean isNewMember = ((residenceCount + businessCount) == 0);
		log.debug(isNewMember);
		req.setAttribute("newMember", isNewMember); //used in JSP conditional
		if (isNewMember) return;

		// Get the possible memberships the member can subscribe to.
		MembershipAction ma = new MembershipAction(dbConn, attributes);
		putModuleData(ma.retrieveMemberships(req));
	}

	/**
	 * Checks if a member needs to purchase a subscription upgrade
	 * 
	 * @param req
	 * @return true if the member needs an upgrade, false if not
	 * @throws ActionException 
	 */
	protected boolean checkUpgrade(ActionRequest req, MemberVO member, Product membershipId) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean needsUpgrade = true;
		int usageQty = getUsageQty(member.getMemberId(), membershipId);

		StringBuilder sql = new StringBuilder(350);
		sql.append("select sum(s.qty_no) ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_subscription s inner join ");
		sql.append(schema).append("rezdox_membership m on s.membership_id = m.membership_id ");
		sql.append("where m.membership_id=? and member_id=? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, membershipId.name());
			ps.setString(2, member.getMemberId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// Check their purchases against their usage
				int purchaseQty = rs.getInt(1);
				needsUpgrade = purchaseQty - usageQty <= 0;
			} else {
				// No purchases were found (free or otherwise), add the free one they get for signing up.
				MembershipAction ma = new MembershipAction(dbConn, attributes);
				PromotionAction pa = new PromotionAction(dbConn, attributes);
				addSubscription(member, ma.retrieveDefaultMembership(req, membershipId.name()), pa.retrieveFreePromotion(), null);
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
	protected int getUsageQty(String memberId, Product membershipId) throws ActionException {
		switch (membershipId) {
			case RESIDENCE:
				return getResidenceUsage(memberId, ResidenceAction.STATUS_ACTIVE);
			case BUSINESS: 
				return getBusinessUsage(memberId, BusinessStatus.ACTIVE.getStatus(), BusinessStatus.PENDING.getStatus());
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
		log.debug(sql + memberId);

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
		log.debug(sql + memberId);

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

		log.debug("cnt=" + usageQty);
		return usageQty;
	}


	/**
	 * Adds a membership subscription for a given member
	 * @param member
	 * @param membership
	 * @param promotion
	 * @throws ActionException 
	 */
	public void addSubscription(MemberVO member, MembershipVO membership, 
			PromotionVO promotion, String businessId) throws ActionException {
		SubscriptionVO subscription = new SubscriptionVO();
		subscription.setMember(member);
		subscription.setMembership(membership);
		subscription.setPromotion(promotion);
		subscription.setCostNo(membership.getCostNo());
		subscription.setDiscountNo(membership.getCostNo() * promotion.getDiscountPctNo() * -1);
		subscription.setQuantityNo(membership.getQuantityNo());
		subscription.setBusinessId(businessId);

		// Save the member's subscription
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		try {
			dbp.save(subscription);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}