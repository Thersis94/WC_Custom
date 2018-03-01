package com.depuysynthes.srt.vo;

import java.util.Date;

import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Project Milestone Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 23, 2018
 ****************************************************************************/
public class SRTMilestoneVO extends BeanDataVO implements Comparable<SRTMilestoneVO>{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String projectMilestoneXRId;
	private String projectId;
	private String milestoneId;
	private Date milestoneDt;

	public SRTMilestoneVO() {
		//Default Constructor
	}

	/**
	 * @return the projectMilestoneXRId
	 */
	@Column(name="PROJ_MILESTONE_XR_ID", isPrimaryKey= true)
	public String getProjectMilestoneXRId() {
		return projectMilestoneXRId;
	}

	/**
	 * @return the projectId
	 */
	@Column(name="PROJECT_ID")
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the milestoneId
	 */
	@Column(name="MILESTONE_ID")
	public String getMilestoneId() {
		return milestoneId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="MILESTONE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getMilestoneDt() {
		return milestoneDt;
	}

	/**
	 * @param projectMilestoneXRId the projectMilestoneXRId to set.
	 */
	public void setProjectMilestoneXRId(String projectMilestoneXRId) {
		this.projectMilestoneXRId = projectMilestoneXRId;
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param milestoneId the milestoneId to set.
	 */
	public void setMilestoneId(String milestoneId) {
		this.milestoneId = milestoneId;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setMilestoneDt(Date milestoneDt) {
		this.milestoneDt = milestoneDt;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SRTMilestoneVO o) {
		return getMilestoneDt().compareTo(o.getMilestoneDt());
	}

	@Override
	public boolean equals(Object o) {
		boolean same = true;

		if(o instanceof SRTMilestoneVO) {
			SRTMilestoneVO m = (SRTMilestoneVO)o; 
			same = same && StringUtil.checkVal(milestoneId).equals(m.getMilestoneId());
			same = same && StringUtil.checkVal(projectId).equals(m.getProjectId());
		} else {
			same = false;
		}

		return same;
	}
}