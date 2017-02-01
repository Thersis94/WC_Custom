package com.ansmed.sb.report;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;

import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: FellowsDetailVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since May 05, 2009
 Last Updated:
 ***************************************************************************/

public class FellowsDetailVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	private String fellowsGoal = null;
	private String fellowsAction = null;
	private String programNeeds = null;
	private Integer fellowsGoalMonth = null;
	private Integer fellowsGoalYear = null;
	
	/**
	 * 
	 */
	public FellowsDetailVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public FellowsDetailVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public FellowsDetailVO(ActionRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
		fellowsGoal = se.decodeValue(db.getStringVal("program_goal_txt", rs));
		fellowsAction = se.decodeValue(db.getStringVal("program_action_txt", rs));
		fellowsGoalMonth = db.getIntegerVal("program_month_no", rs);
		fellowsGoalYear = db.getIntegerVal("program_yr_no", rs);
		programNeeds = se.decodeValue(db.getStringVal("program_needs_txt",rs));
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(ActionRequest req) {
		StringEncoder se = new StringEncoder();
		repFirstNm = se.decodeValue(req.getParameter("repFirstNm"));
		repLastNm = se.decodeValue(req.getParameter("repLastNm"));
		surgeonFirstNm = se.decodeValue(req.getParameter("physFirstNm"));
		surgeonLastNm = se.decodeValue(req.getParameter("physLastNm"));
		fellowsGoal = se.decodeValue(req.getParameter("fellowsGoal"));
		fellowsAction = se.decodeValue(req.getParameter("fellowsAction"));
		fellowsGoalMonth = Convert.formatInteger(req.getParameter("fellowsGoalMonth"));
		fellowsGoalYear = Convert.formatInteger(req.getParameter("fellowsGoalYear"));
		programNeeds = se.decodeValue(req.getParameter("programNeeds"));
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

	/**
	 * @return the fellowsGoal
	 */
	public String getFellowsGoal() {
		return fellowsGoal;
	}

	/**
	 * @param fellowsGoal the fellowsGoal to set
	 */
	public void setFellowsGoal(String fellowsGoal) {
		this.fellowsGoal = fellowsGoal;
	}

	/**
	 * @return the fellowsAction
	 */
	public String getFellowsAction() {
		return fellowsAction;
	}

	/**
	 * @param fellowsAction the fellowsAction to set
	 */
	public void setFellowsAction(String fellowsAction) {
		this.fellowsAction = fellowsAction;
	}

	/**
	 * @return the fellowsGoalMonth
	 */
	public Integer getFellowsGoalMonth() {
		return fellowsGoalMonth;
	}

	/**
	 * @param fellowsGoalMonth the fellowsGoalMonth to set
	 */
	public void setFellowsGoalMonth(Integer fellowsGoalMonth) {
		this.fellowsGoalMonth = fellowsGoalMonth;
	}

	/**
	 * @return the fellowsGoalYear
	 */
	public Integer getFellowsGoalYear() {
		return fellowsGoalYear;
	}

	/**
	 * @param fellowsGoalYear the fellowsGoalYear to set
	 */
	public void setFellowsGoalYear(Integer fellowsGoalYear) {
		this.fellowsGoalYear = fellowsGoalYear;
	}
	/**
	 * @return the programNeeds
	 */
	public String getProgramNeeds() {
		return programNeeds;
	}

	/**
	 * @param programNeeds the programNeeds to set
	 */
	public void setProgramNeeds(String programNeeds) {
		this.programNeeds = programNeeds;
	}
}
