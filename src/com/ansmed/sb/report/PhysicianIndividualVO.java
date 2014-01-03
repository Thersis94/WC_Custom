package com.ansmed.sb.report;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

import com.ansmed.sb.physician.BusAssessVO;
import com.ansmed.sb.physician.BusGoalVO;

/****************************************************************************
 * <b>Title</b>: PhysicianIndividualVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr 30, 2009
 ****************************************************************************/
public class PhysicianIndividualVO extends AbstractSiteBuilderVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<BusAssessVO> swot = new ArrayList<BusAssessVO>();
	private List<BusGoalVO> objectives = new ArrayList<BusGoalVO>();
	
	/**
	 * 
	 */
	public PhysicianIndividualVO() {		
	}

	/**
	 * @return the swot
	 */
	public List<BusAssessVO> getSwot() {
		return swot;
	}

	/**
	 * @param swot the swot to set
	 */
	public void setSwot(List<BusAssessVO> swot) {
		this.swot = swot;
	}

	/**
	 * @return the objectives
	 */
	public List<BusGoalVO> getObjectives() {
		return objectives;
	}

	/**
	 * @param objectives the objectives to set
	 */
	public void setObjectives(List<BusGoalVO> objectives) {
		this.objectives = objectives;
	}
	
	
}
