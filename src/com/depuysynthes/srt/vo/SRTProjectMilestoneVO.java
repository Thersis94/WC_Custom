package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <b>Title:</b> SRTProjectMilestoneVO.java
 * <b>Project:</b> WebCrescendo
 * <b>Description:</b> Stores SRT Project Milestone XR Records.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 12, 2018
 ****************************************************************************/

public class SRTProjectMilestoneVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String projectMilestoneXRId;
	private String projectId;
	private String milestoneId;
	private Date milestoneDt;

	public SRTProjectMilestoneVO() {
		//Default Constructor
	}

	/**
	 * Overloaded Constructor for easy ProjectMilestone Creation.
	 * @param milestoneId
	 * @param projectId
	 */
	public SRTProjectMilestoneVO(String milestoneId, String projectId) {
		this();
		this.projectId = projectId;
		this.milestoneId = milestoneId; 
	}

	public SRTProjectMilestoneVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public SRTProjectMilestoneVO(ResultSet rs) {
		this();
		populateData(rs);
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
}