package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

// WC_Custom
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
		MembershipVO membership = retrieveDefaultMembership(dbp);
		PromotionVO promotion = retrieveFreePromotion(dbp);
		
		subscription.setMember(member);
		subscription.setMembership(membership);
		subscription.setPromotion(promotion);
		subscription.setCostNo(membership.getCostNo());
		subscription.setDiscountNo(membership.getCostNo() * promotion.getDiscountPctNo());
		subscription.setQuantityNo(membership.getQuantityNo());
		
		// Save member/subscription data
		try {
			dbp.save(member);
			dbp.save(subscription);
		} catch (Exception e) {
			log.error("Unable to save new RezDox member/subscription data. ", e);
		}
		
		// TODO: Ticket #RV-72: Add in hook to give the user Rez Rewards for signing up.
		
	}
	
	/**
	 * Retrieves the default membership for signing up.
	 * 
	 * @param dbp
	 * @return
	 */
	private MembershipVO retrieveDefaultMembership(DBProcessor dbp) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(schema).append("rezdox_membership ");
		sql.append("where group_cd = ? and qty_no = 100 ");
		
		List<Object> params = new ArrayList<>();
		params.add(Group.CO.name());
		
		List<MembershipVO> membership = dbp.executeSelect(sql.toString(), params, new MembershipVO());
		
		return membership.get(0);
	}

	/**
	 * Retrieves the promotion used when signing up.
	 * 
	 * @param dbp
	 * @return
	 */
	private PromotionVO retrieveFreePromotion(DBProcessor dbp) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(schema).append("rezdox_promotion ");
		sql.append("where promotion_cd = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(PromotionVO.SIGNUP_PROMOTION_CD);
		
		List<PromotionVO> promotion = dbp.executeSelect(sql.toString(), params, new PromotionVO());
		
		return promotion.get(0);
	}
}
