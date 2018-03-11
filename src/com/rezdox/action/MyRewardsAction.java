package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.MemberRewardVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MyRewardsAction.java<br/>
 * <b>Description:</b> Displays the user's redeemable points (by default), and allows them to cash points in
 * for Rez-Rewards.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 9, 2018
 ****************************************************************************/
public class MyRewardsAction extends SimpleActionAdapter {

	private static final String MY_POINTS = "rezdoxMemberRewardPoints";


	public MyRewardsAction() {
		super();
	}

	public MyRewardsAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * Display the user's points, and a list of available rewards if the Widget is in focus
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTSession ses = req.getSession();

		// Note: if the user's points change during their visit, simply apss this parameter to refresh them.
		if (!req.hasParameter(Constants.PAGE_MODULE_ID) && !req.hasParameter("refreshPoints") && ses.getAttribute(MY_POINTS) != null) {
			//if we're not on the points page, not refreshing, and the session tally is already loaded...we're done here.
			return;
		}

		// Load a list of rewards - inclusive of the ones this user has cashed in.
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		req.setAttribute("allRewards", ra.loadRewards(null));
		ra.retrieve(req);

		//get the list of rewards off the attributes map and build a tally to put onto their session
		ModuleVO mod = (ModuleVO) ra.getAttribute(Constants.MODULE_DATA);
		List<MemberRewardVO> rewards  = (List<MemberRewardVO>) mod.getActionData();
		if (rewards == null) rewards = new ArrayList<>();

		int points = 0;
		for (MemberRewardVO vo : rewards)
			points += vo.getPointsNo();

		ses.setAttribute(MY_POINTS, points);
	}


	/**
	 * form-submittal - the user is cashing in points for a reward.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession ses = req.getSession();
		MemberVO member = (MemberVO) ses.getAttribute(Constants.USER_DATA);
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());

		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			ra.applyReward(req.getParameter("rewardId"), member.getMemberId());
		} catch (ActionException ae) {
			log.error(ae.getMessage(), ae);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		//flush the session value so it's reloaded after the redirect
		ses.removeAttribute(MY_POINTS);

		//redirect the user
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String url = StringUtil.join(page.getFullPath(), "?pmid=", req.getParameter(Constants.PAGE_MODULE_ID));
		sendRedirect(url, (String)msg, req);
	}
}