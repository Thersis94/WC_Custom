package com.rezdox.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> RewardsAction.java<br/>
 * <b>Description:</b> 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 23, 2018
 ****************************************************************************/
public class RewardsAction extends SimpleActionAdapter {

	protected static final String REQ_REWARD_ID = "rewardId";

	public RewardsAction() {
		super();
	}

	public RewardsAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.list(req);
	}

	@Override
	public void update(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.update(req);
	}

	@Override
	public void delete(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.delete(req);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.retrieve(req);
	}

	
	/**
	 * Attach the desired reward to the user.
	 * @param rewardId
	 * @param userId
	 * @throws ActionException
	 */
	public void applyReward(String rewardId, String userId) throws ActionException {
		//TODO - need to load Reward using slug or ID, create a VO around the member_reward table, then push to DBProcessor for insertion.
	}
	
}
