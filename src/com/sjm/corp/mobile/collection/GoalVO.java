package com.sjm.corp.mobile.collection;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: GoalVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM related to Goals and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class GoalVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private boolean newPractice;
	private boolean rebrandPractice;
	private boolean consolidation;
	private boolean overallPatients;
	private boolean interventionalPatients;
	private boolean hcpPatients;
	private String goalId;
	
	public GoalVO(){
		super();
	}

	public boolean isNewPractice() {
		return newPractice;
	}

	public void setNewPractice(boolean newPractice) {
		this.newPractice = newPractice;
	}

	public boolean isRebrandPractice() {
		return rebrandPractice;
	}

	public void setRebrandPractice(boolean rebrandPractice) {
		this.rebrandPractice = rebrandPractice;
	}

	public boolean isConsolidation() {
		return consolidation;
	}

	public void setConsolidation(boolean consolidation) {
		this.consolidation = consolidation;
	}

	public boolean isOverallPatients() {
		return overallPatients;
	}

	public void setOverallPatients(boolean overallPatients) {
		this.overallPatients = overallPatients;
	}

	public boolean isInterventionalPatients() {
		return interventionalPatients;
	}

	public void setInterventionalPatients(boolean interventionalPatients) {
		this.interventionalPatients = interventionalPatients;
	}

	public boolean isHcpPatients() {
		return hcpPatients;
	}

	public void setHcpPatients(boolean hcpPatients) {
		this.hcpPatients = hcpPatients;
	}

	public String getGoalId() {
		return goalId;
	}

	public void setGoalId(String goalId) {
		this.goalId = goalId;
	}
}