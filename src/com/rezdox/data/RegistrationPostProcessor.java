package com.rezdox.data;

import java.util.List;

// WC_Custom
import com.rezdox.action.MembershipAction;
import com.rezdox.action.PromotionAction;
import com.rezdox.action.RewardsAction;
import com.rezdox.action.RezDoxUtils;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.PromotionVO;
import com.rezdox.vo.SubscriptionVO;

// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;

// WebCrescendo 3
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <p><b>Title</b>: RegistrationPostProcessor</p>
 * <p><b>Description: </b>Creates the member record, default (free) subscription
 * record(s), and Rez Rewards after initial user registration.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 25, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class RegistrationPostProcessor extends SimpleActionAdapter {

	public RegistrationPostProcessor() {
		super();
	}

	/**
	 * @param arg0
	 */
	public RegistrationPostProcessor(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Running RezDox registration post-processor.");

		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		MemberVO member = new MemberVO();

		// Set user data onto the member
		member.setData(user.getDataMap());
		member.setAttributes(user.getAttributes());
		member.setAuthenticated(user.isAuthenticated());

		// Set data for new members
		member.setRegisterSubmittalId((String) req.getAttribute("registerSubmittalId"));
		member.setStatusFlg(1);
		member.setPrivacyFlg(0);

		// Get default member subscription... the only default right now is "100 Connections".
		// Free business and residence subscriptions are added by member selection after signing up.
		SubscriptionVO subscription = new SubscriptionVO();

		DBProcessor dbp = new DBProcessor(dbConn);
		MembershipVO membership = retrieveDefaultMembership();
		PromotionVO promotion = retrieveFreePromotion();

		subscription.setMember(member);
		subscription.setMembership(membership);
		subscription.setPromotion(promotion);
		subscription.setCostNo(membership.getCostNo());
		subscription.setDiscountNo(membership.getCostNo() * promotion.getDiscountPctNo() * -1);
		subscription.setQuantityNo(membership.getQuantityNo());

		// Save member/subscription data
		try {
			dbp.save(member);
			dbp.save(subscription);
		} catch (Exception e) {
			throw new ActionException(e);
		}

		//apply the default reward give to all new users at first login
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		ra.applyReward(RezDoxUtils.NEW_REGISTRANT_REWARD, member.getMemberId());
	}


	/**
	 * Retrieves the default membership for signing up.
	 * 
	 * @return
	 */
	private MembershipVO retrieveDefaultMembership() {
		ActionRequest membershipReq = new ActionRequest();
		membershipReq.setParameter("getNewMemberDefault", "true");
		membershipReq.setParameter("groupCode", Group.CO.name());

		MembershipAction ma = new MembershipAction();
		ma.setAttributes(this.attributes);
		ma.setDBConnection(dbConn);

		List<MembershipVO> membership = ma.retrieveMemberships(membershipReq);
		return membership.get(0);
	}

	/**
	 * Retrieves the promotion used for signing up.
	 * 
	 * @return
	 */
	private PromotionVO retrieveFreePromotion() {
		ActionRequest promotionReq = new ActionRequest();
		promotionReq.setParameter("promotionCode", PromotionAction.SIGNUP_PROMOTION_CD);

		PromotionAction pa = new PromotionAction();
		pa.setAttributes(this.attributes);
		pa.setDBConnection(dbConn);

		List<PromotionVO> promotion = pa.retrievePromotions(promotionReq);
		return promotion.get(0);
	}
}
