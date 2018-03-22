package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.rezdox.vo.MemberRewardVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
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
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public MyRewardsAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/*
	 * Display the user's points, and a list of available rewards if the Widget is in focus
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
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

		//set the total in a cookie.  This may be excessive for repeat calls to the rewards page, but ensures cached data is flushed
		HttpServletResponse resp = (HttpServletResponse) getAttribute(GlobalConfig.HTTP_RESPONSE);
		CookieUtil.add(resp, MY_POINTS, String.valueOf(points), "/", -1);

		//if this is the ajax call, we only want to return a point count, not reward data
		if (req.hasParameter("pointsOnly")) {
			putModuleData(points);
		} else {
			mod.setAttribute(MY_POINTS, points); //for JSP, which won't see the cookie if just created
		}
	}


	/*
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


	/**
	 * Returns a list of pending rewards for the entire system - called from the WC Data Tool UI for management 
	 * @return
	 * @throws ActionException
	 */
	protected List<MemberRewardVO> loadPendingRewards() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.member_reward_id, a.create_dt, a.member_id, b.reward_nm, b.currency_value_no, b.point_value_no, ");
		sql.append("p.first_nm, p.last_nm, p.email_address_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_MEMBER_REWARD a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_REWARD b on a.reward_id=b.reward_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_MEMBER m on a.member_id=m.member_id ");
		sql.append(DBUtil.INNER_JOIN).append("PROFILE p on m.profile_id=p.profile_id ");
		sql.append("where b.reward_type_cd='REDEEM' and a.approval_flg=0 order by a.create_dt");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<MemberRewardVO> data = dbp.executeSelect(sql.toString(), null, new MemberRewardVO());
		if (data == null || data.isEmpty()) return Collections.emptyList();

		//decrypt member names and tally memberIds for the next step
		List<String> memberIds = new ArrayList<>();
		StringEncrypter se = StringEncrypter.getInstance((String)getAttribute(Constants.ENCRYPT_KEY));
		for (MemberRewardVO vo : data) {
			vo.setFirstName(decrypt(se, vo.getFirstName()));
			vo.setLastName(decrypt(se, vo.getLastName()));
			vo.setEmailAddress(decrypt(se, vo.getEmailAddress()));

			memberIds.add(vo.getMemberId());
		}

		//get point totals for each of these members.
		Map<String, Integer> pointTotals = getPointTotals(memberIds);
		for (MemberRewardVO vo : data) {
			vo.setMyPointsNo(Convert.formatInteger(pointTotals.get(vo.getMemberId())));
		}
		return data;
	}


	/**
	 * loads point totals for the given users
	 * @param memberIds
	 * @return
	 */
	private Map<String, Integer> getPointTotals(List<String> memberIds) {
		Map<String, Integer> data = new HashMap<>(memberIds.size());
		StringBuilder sql = new StringBuilder(150);
		sql.append("select member_id, sum(point_value_no) from ").append(getCustomSchema());
		sql.append("REZDOX_MEMBER_REWARD where member_id in (");
		DBUtil.preparedStatmentQuestion(memberIds.size(), sql);
		sql.append(") and approval_flg=1 "); //only approved rewards counts towards their available total
		sql.append("group by member_id");
		log.debug(sql);

		int x = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String s : memberIds)
				ps.setString(++x, s);

			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString(1), rs.getInt(2));

		} catch (SQLException sqle) {
			log.error("could not load user point totals", sqle);
		}

		log.debug("loaded point totals for " + data.size() + " members");
		return data;
	}

	/**
	 * helper method to trap thrown exception from String decryption
	 * @param se
	 * @param s
	 * @return
	 */
	private String decrypt(StringEncrypter se, String s) {
		try {
			return se.decrypt(s);
		} catch (Exception e) {
			return s;
		}
	}
}