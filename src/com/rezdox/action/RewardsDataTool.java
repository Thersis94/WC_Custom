package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.rezdox.action.RewardsAction.REQ_REWARD_ID;

import com.rezdox.vo.MemberRewardVO;
import com.rezdox.vo.RewardVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title:</b> RewardsDataTool.java<br/>
 * <b>Description:</b> WC Data Tool for Managing Rewards in the system.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 23, 2018
 ****************************************************************************/
public class RewardsDataTool extends SimpleActionAdapter {

	private static final String REQ_MEMBER_REWARD_ID = "memberRewardId";

	public RewardsDataTool() {
		super();
	}

	public RewardsDataTool(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * Return a list of all the rewards in the system.
	 * This is called from the WC Data Tool for RezDox Administrators to manage the data.
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		//only load data on the ajax call (list pg), or when a pkId is passed (edit pg)
		if (!req.hasParameter("loadData") && !req.hasParameter(REQ_REWARD_ID)) return;

		if (req.hasParameter("listApprovals")) {
			MyRewardsAction mra = new MyRewardsAction(getDBConnection(), getAttributes());
			List<MemberRewardVO> data = mra.loadPendingRewards();
			putModuleData(data);

		} else {
			RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
			List<RewardVO> data = ra.loadRewards(req.getParameter(REQ_REWARD_ID));
			putModuleData(data);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		if (req.hasParameter(REQ_MEMBER_REWARD_ID)) {
			//member reward approval - rejection is a call to delete(req) directly from the browser
			MemberRewardVO mrv = new MemberRewardVO();
			mrv.setApprovalFlg(1);
			mrv.setMemberRewardId(req.getParameter(REQ_MEMBER_REWARD_ID));
			msg = approveMemberReward(mrv);

			//notify the user their reward was approved
			RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
			notifyUtil.sendToMember(RezDoxNotifier.Message.REWARD_APPRVD, null, null, req.getParameter("memberId"));

		} else {
			RewardVO vo = RewardVO.instanceOf(req);
			boolean isNew = StringUtil.isEmpty(vo.getRewardId());
			msg = save(vo, false);

			//tell the world we've added a new reward!
			if (isNew && "REDEEM".equals(vo.getRewardTypeCd()) && vo.getActiveFlg() == 1) {
				RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
				notifyUtil.sendToAllMembers(RezDoxNotifier.Message.REWARD_NEW, null, null);
			}
		}

		adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		Object vo = null;

		if (req.hasParameter(REQ_MEMBER_REWARD_ID)) {
			MemberRewardVO mrv = new MemberRewardVO();
			mrv.setMemberRewardId(req.getParameter(REQ_MEMBER_REWARD_ID));
			vo = mrv;
		} else {
			vo = RewardVO.instanceOf(req);
		}
		String msg = save(vo, true);
		adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/**
	 * Approve the member reward using pkId.  
	 * Seemingly DBProcessor does not support this (partial updates) as cleanly.
	 * @param mrv
	 */
	private String approveMemberReward(MemberRewardVO mrv) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema());
		sql.append("REZDOX_MEMBER_REWARD set ");
		sql.append("approval_flg=1, update_dt=CURRENT_TIMESTAMP ");
		sql.append("where member_reward_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, mrv.getMemberRewardId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not approve member reward", sqle);
			return (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE); 
		}
		return (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
	}


	/**
	 * Reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected String save(Object vo, boolean isDelete) {
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			if (isDelete) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
			return (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		} catch (Exception e) {
			log.error("could not save reward", e);
			return (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
	}
}