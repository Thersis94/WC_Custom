package com.mts.subscriber.action;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;

// MTS Libs
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.SubmittalAction;


/****************************************************************************
 * <b>Title</b>: TrialSubScriptionPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Adds the user data to the MTS_USER table and 
 * sets up a 7 day trial period
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 28, 2020
 * @updates:
 ****************************************************************************/
public class TrialSubScriptionPostProcessor extends SBActionAdapter {

	/**
	 * 
	 */
	public TrialSubScriptionPostProcessor() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TrialSubScriptionPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO)req.getAttribute(SubmittalAction.REGISTRATION_USER_DATA);
		log.info("Adding MTS info for trial: " + user);
		
		// Look for an existing account
		MTSUserVO mtsUser = checkMTSUser(user.getProfileId());
		
		// What to do if exists
		
		// Add MTS User
		mtsUser = new MTSUserVO(user);
		mtsUser.setActiveFlag(1);
		mtsUser.setRoleId("SUBSCRIBER");
		mtsUser.setSubscriptionType(SubscriptionType.USER);
		
		// Get existing MTS Subscription info 
		List<SubscriptionUserVO> subs = getExistingSubscriptions(mtsUser.getUserId());
		
		// What to do if exists
		
		
		// Add subscription
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 */
	public List<SubscriptionUserVO> getExistingSubscriptions(String userId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_subscription_publication_xr where user_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(userId), new SubscriptionUserVO());
	}
	
	/**
	 * Check for an existing user id
	 * @param profileId
	 * @return
	 */
	public MTSUserVO checkMTSUser(String profileId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_user where profile_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<MTSUserVO> users = db.executeSelect(sql.toString(), Arrays.asList(profileId), new MTSUserVO());
		if (! users.isEmpty()) return users.get(0);
		else return null;
	}

}
