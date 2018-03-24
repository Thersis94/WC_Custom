package com.rezdox.data;

// WC_Custom
import com.rezdox.action.MembershipAction;
import com.rezdox.action.PromotionAction;
import com.rezdox.action.RewardsAction;
import com.rezdox.action.RezDoxUtils;
import com.rezdox.action.SubscriptionAction;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.PromotionVO;

// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
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

		// Save member data
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(member);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		// Get default member subscription... the only default right now is "100 Connections".
		// Free business and residence subscriptions are added by member selection after signing up.
		MembershipAction ma = new MembershipAction(dbConn, attributes);
		MembershipVO membership = ma.retrieveDefaultMembership(Group.CO);

		// Get the "Free" promotion used when signing up
		PromotionAction pa = new PromotionAction(dbConn, attributes);
		PromotionVO promotion = pa.retrieveFreePromotion();

		// Give the member their free subscription
		SubscriptionAction sa = new SubscriptionAction(dbConn, attributes);
		sa.addSubscription(member, membership, promotion);

		//apply the default reward give to all new users at first login
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		ra.applyReward(RezDoxUtils.NEW_REGISTRANT_REWARD, member.getMemberId());
		//set a member vo on the session so other rezdox actions have the right class
		SMTSession session = req.getSession();
		//set the new member so other actions can find its data
		session.setAttribute(Constants.USER_DATA, member);
		req.setParameter(Constants.REDIRECT_URL, RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH);
	}
}
