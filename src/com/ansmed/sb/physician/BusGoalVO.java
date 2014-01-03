package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;

import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: BusAssessVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Feb 11, 2009
 Last Updated:
 ***************************************************************************/

public class BusGoalVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String busGoalId = null;
	private String goal = null;
	private String goalAction = null;
	private String goalTimeline = null;
	private String surgeonId = null;
	private String repId = null;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	
	/**
	 * 
	 */
	public BusGoalVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public BusGoalVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public BusGoalVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		StringEncoder se = new StringEncoder();
		busGoalId = db.getStringVal("bus_assess_goal_id", rs);
		goal = se.decodeValue(db.getStringVal("goal_txt", rs));
		goalAction = se.decodeValue(db.getStringVal("goal_action_txt", rs));
		goalTimeline = se.decodeValue(db.getStringVal("goal_timeline_txt", rs));
		surgeonId = db.getStringVal("surgeon_id", rs);
		repId = db.getStringVal("sales_rep_id", rs);
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		busGoalId = req.getParameter("goalId");
		goal = req.getParameter("goalName");
		goalAction = req.getParameter("goalAction");
		goalTimeline = req.getParameter("goalTimeline");
		surgeonId = req.getParameter("surgeonId");
	}
	
	/**
	 * @return the busGoalId
	 */
	public String getBusGoalId() {
		return busGoalId;
	}

	/**
	 * @param busGoalId the busGoalId to set
	 */
	public void setBusGoalId(String busGoalId) {
		this.busGoalId = busGoalId;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the goal
	 */
	public String getGoal() {
		return goal;
	}

	/**
	 * @param goal the goal to set
	 */
	public void setGoal(String goal) {
		this.goal = goal;
	}

	/**
	 * @return the goalAction
	 */
	public String getGoalAction() {
		return goalAction;
	}

	/**
	 * @param goalAction the goalAction to set
	 */
	public void setGoalAction(String goalAction) {
		this.goalAction = goalAction;
	}

	/**
	 * @return the goalTimeline
	 */
	public String getGoalTimeline() {
		return goalTimeline;
	}

	/**
	 * @param goalTimeline the goalTimeline to set
	 */
	public void setGoalTimeline(String goalTimeline) {
		this.goalTimeline = goalTimeline;
	}
	
	/**
	 * @return the repId
	 */
	public String getRepId() {
		return repId;
	}

	/**
	 * @param repId the repId to set
	 */
	public void setRepId(String repId) {
		this.repId = repId;
	}

	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
	}

	/**
	 * @return the surgeonFirstNm
	 */
	public String getSurgeonFirstNm() {
		return surgeonFirstNm;
	}

	/**
	 * @param surgeonFirstNm the surgeonFirstNm to set
	 */
	public void setSurgeonFirstNm(String surgeonFirstNm) {
		this.surgeonFirstNm = surgeonFirstNm;
	}

	/**
	 * @return the surgeonLastNm
	 */
	public String getSurgeonLastNm() {
		return surgeonLastNm;
	}

	/**
	 * @param surgeonLastNm the surgeonLastNm to set
	 */
	public void setSurgeonLastNm(String surgeonLastNm) {
		this.surgeonLastNm = surgeonLastNm;
	}

}
