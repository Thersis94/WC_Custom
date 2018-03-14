package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.workflow.milestones.MilestoneVO;

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
public class SRTProjectMilestoneVO extends MilestoneVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String projectMilestoneXRId;
	private String projectId;
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
		setMilestoneId(milestoneId);
	}

	public SRTProjectMilestoneVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public SRTProjectMilestoneVO(ResultSet rs) throws SQLException {
		this();
		setData(rs);
	}

	/**
	 * Set data off a ResultSet
	 * @param rs
	 * @throws SQLException
	 */
	private void setData(ResultSet rs) throws SQLException {
		DBUtil util = new DBUtil();
		setMilestoneId(rs.getString("MILESTONE_ID"));
		setOrganizationId(util.getStringVal("OP_CO_ID", rs));
		setParentId(util.getStringVal("PARENT_ID", rs));
		setCreateDt(util.getDateVal("CREATE_DT", rs));
		setProjectId(util.getStringVal("PROJECT_ID", rs));
		setMilestoneNm(util.getStringVal("MILESTONE_NM", rs));
		setProjectMilestoneXRId(util.getStringVal("PROJ_MILESTONE_XR_ID", rs));
		setMilestoneDt(util.getDateVal("MILESTONE_DT", rs));
	}

	/**
	 * @return the projectMilestoneXRId
	 */
	public String getProjectMilestoneXRId() {
		return projectMilestoneXRId;
	}

	/**
	 * @return the projectId
	 */
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the milestoneId
	 */
	@Override
	public String getMilestoneId() {
		return super.getMilestoneId();
	}

	/**
	 * @return the createDt
	 */
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
	 * @param createDt the createDt to set.
	 */
	public void setMilestoneDt(Date milestoneDt) {
		this.milestoneDt = milestoneDt;
	}
}