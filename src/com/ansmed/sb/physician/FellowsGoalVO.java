package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;

import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: FellowsGoalVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Feb 17, 2009
 Last Updated:
 ***************************************************************************/

public class FellowsGoalVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String fellowsId = null;
	private String fellowsGoalId = null;
	private String fellowsGoal = null;
	private String fellowsAction = null;
	private Integer fellowsGoalMonth = null;
	private Integer fellowsGoalYear = null;
	private String programNeeds = null;
	
	/**
	 * 
	 */
	public FellowsGoalVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public FellowsGoalVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public FellowsGoalVO(SMTServletRequest req) {
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
		fellowsId = db.getStringVal("fellows_id", rs);
		fellowsGoalId = db.getStringVal("fellows_goal_id", rs);
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
	public void setData(SMTServletRequest req) {
		fellowsId = req.getParameter("fellowsId");
		fellowsGoalId = req.getParameter("fellowsGoalId");
		fellowsGoal = req.getParameter("fellowsGoal");
		fellowsAction = req.getParameter("fellowsAction");
		fellowsGoalMonth = Convert.formatInteger(req.getParameter("fellowsGoalMonth"));
		fellowsGoalYear = Convert.formatInteger(req.getParameter("fellowsGoalYear"));
		programNeeds = req.getParameter("programNeeds");
	}

	/**
	 * @return the fellowsId
	 */
	public String getFellowsId() {
		return fellowsId;
	}

	/**
	 * @param fellowsId the fellowsId to set
	 */
	public void setFellowsId(String fellowsId) {
		this.fellowsId = fellowsId;
	}

	/**
	 * @return the fellowsGoalId
	 */
	public String getFellowsGoalId() {
		return fellowsGoalId;
	}

	/**
	 * @param fellowsGoalId the fellowsGoalId to set
	 */
	public void setFellowsGoalId(String fellowsGoalId) {
		this.fellowsGoalId = fellowsGoalId;
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
