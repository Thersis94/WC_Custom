package com.rezdox.security;

// Java 8
import java.util.Map;

import com.rezdox.action.BusinessReviewAction;
import com.rezdox.action.ConnectionAction;
import com.rezdox.action.MemberAction;
import com.rezdox.action.MyNotificationsAction;
import com.rezdox.action.MyRewardsAction;
//WC_Custom libs
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionRequest;
//SMTBaseLibs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
//WebCrescendo libs
import com.smt.sitebuilder.security.DBLoginModule;

/*****************************************************************************
 <p><b>Title</b>: RezDoxLoginModule</p>
 <p><b>Description: </b>Custom login module for RezDox user login.  Authenticates 
 user against WebCrescendo core login, then retrieves RezDox member data.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 25, 2018
 <b>Changes:</b>
 ***************************************************************************/
public class RezDoxLoginModule extends DBLoginModule {

	public RezDoxLoginModule() {
		super();
	}

	public RezDoxLoginModule(Map<String, Object> config) {
		super(config);
	}

	/**
	 * After authentication, gets RezDox member data for the user who just logged in and
	 * merges the user data into the MemberVO.
	 * 
	 * @param profileId
	 * @param authenticationId
	 * @return
	 */
	@Override
	public MemberVO loadUserData(String profileId, String authenticationId) {
		UserDataVO user = super.loadUserData(profileId, authenticationId);
		SMTDBConnection dbConn = (SMTDBConnection) getAttribute(GlobalConfig.KEY_DB_CONN);
		ActionRequest req = (ActionRequest) getAttribute(GlobalConfig.ACTION_REQUEST);

		// Get the member data by calling the member action
		MemberAction ma = new MemberAction(dbConn, getAttributes());
		MemberVO member = ma.retrieveMemberData(null, user.getProfileId());
		if (StringUtil.isEmpty(member.getMemberId())) return null;

		// Populate the member/user data
		member.setData(user.getDataMap());
		member.setAttributes(user.getAttributes());
		member.setAuthenticated(user.isAuthenticated());

		// Get the count of reviews to display in the left menu badge
		BusinessReviewAction br = new BusinessReviewAction(dbConn, getAttributes());
		CookieUtil.add(req, BusinessReviewAction.COOKIE_REVIEW_COUNT, String.valueOf(br.getReviewCount(member.getMemberId())), "/", -1);

		//Flush the connections cookie - it can't be loaded until after the user's Role is, so it'll have to wait.
		CookieUtil.remove(req, ConnectionAction.CONNECTION_COOKIE);

		//load a count of the user's RezRewards into a cookie for display in the left menu
		MyRewardsAction rewards = new MyRewardsAction(dbConn, getAttributes());
		int pts = rewards.getAvailablePoints(member.getMemberId());
		CookieUtil.add(req, MyRewardsAction.MY_POINTS, String.valueOf(pts), "/", -1);

		//load a count of the user's Notifications into a cookie for display in the left menu
		MyNotificationsAction notifs = new MyNotificationsAction(dbConn, getAttributes());
		pts = notifs.getCount(member.getProfileId());
		CookieUtil.add(req, MyNotificationsAction.MY_NOTIFS, String.valueOf(pts), "/", -1);

		return member;
	}
}