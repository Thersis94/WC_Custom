package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.MemberRewardVO;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.RewardVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RewardsAction.java<br/>
 * <b>Description:</b> Interacts with RezDox Rewards - for Data Tool as well as for a single user.
 * Also applies rewards to a user's account.
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

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public RewardsAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/**
	 * List the rewards available for the logged-in user.  
	 * Note: "rewards available for a specific user" can be achived for administrative 
	 * 		purposes by passing the request Attribute instead of relying on session data for memberId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.*, b.reward_type_cd, b.reward_nm, c.type_nm");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_MEMBER_REWARD a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_REWARD b on a.reward_id=b.reward_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_REWARD_TYPE c on b.reward_type_cd=c.reward_type_cd ");
		sql.append("where a.member_id=? ");
		sql.append("order by coalesce(a.update_dt, a.create_dt, CURRENT_TIMESTAMP) desc, c.type_nm, b.order_no, b.reward_nm");
		log.debug(sql);

		String memberId = StringUtil.checkVal(req.getAttribute("member_id"), null);
		if (memberId == null) {
			MemberVO user = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
			memberId = user.getMemberId();
		}
		List<Object> params = new ArrayList<>();
		params.add(memberId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<Object> myRewards = db.executeSelect(sql.toString(), params, new MemberRewardVO());
		putModuleData(myRewards);
	}


	/**
	 * Attach the given reward to the given user.
	 * @param rewardIdOrSlug
	 * @param memberId
	 * @throws ActionException
	 */
	public void applyReward(String rewardIdOrSlug, String memberId) throws ActionException {
		if (StringUtil.isEmpty(rewardIdOrSlug) || StringUtil.isEmpty(memberId))
			throw new ActionException("Missing data");

		//find the desired reward
		List<RewardVO> data = loadRewards(rewardIdOrSlug);
		if (data == null || data.size() != 1) throw new ActionException("Could not find reward");

		//create a VO we can throw at the rezdox_member_reward table
		MemberRewardVO vo = MemberRewardVO.instanceOf(data.get(0), memberId);

		//save the record
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(vo);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException("Could not apply reward", e);
		}
	}


	/**
	 * Returns a list of rewards - possibly matching the given criteria.  
	 * Used in the WC Data Tool as well as for listing a user's rewards and/or applying them. 
	 * @param rewardIdOrSlug
	 * @return
	 */
	protected List<RewardVO> loadRewards(String rewardIdOrSlug) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.*, b.type_nm from ").append(schema).append("REZDOX_REWARD a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_REWARD_TYPE b ");
		sql.append("on a.reward_type_cd=b.reward_type_cd ");

		if (!StringUtil.isEmpty(rewardIdOrSlug)) {
			sql.append("where a.action_slug_txt=? or a.reward_id=? ");
			params.add(rewardIdOrSlug);
			params.add(rewardIdOrSlug);
		}
		sql.append("order by b.type_nm, a.order_no, a.reward_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new RewardVO());
	}
}