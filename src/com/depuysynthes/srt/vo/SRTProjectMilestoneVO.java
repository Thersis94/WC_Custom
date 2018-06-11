package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
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
@Table(name="DPY_SYN_SRT_PROJECT_MILESTONE_XR")
public class SRTProjectMilestoneVO extends MilestoneVO {

	public enum MilestoneTypeId {DATE, STATUS}
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String projectMilestoneXRId;
	private String projectId;
	private MilestoneTypeId milestoneTypeId;
	private String campaignInstanceId;
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
		setMilestoneTypeId(util.getEnumVal(MilestoneTypeId.class, "MILESTONE_TYPE_ID", rs));
		setProjectMilestoneXRId(util.getStringVal("PROJ_MILESTONE_XR_ID", rs));
		setMilestoneDt(util.getDateVal("MILESTONE_DT", rs));
		setCampaignInstanceId(util.getStringVal("CAMPAIGN_INSTANCE_ID", rs));
	}

	/**
	 * @return the projectMilestoneXRId
	 */
	@Column(name="PROJ_MILESTONE_XR_ID", isPrimaryKey=true)
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
	 * @return the milestoneTypeId
	 */
	@Column(name="MILESTONE_TYPE_ID")
	public MilestoneTypeId getMilestoneTypeId() {
		return milestoneTypeId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="MILESTONE_DT")
	public Date getMilestoneDt() {
		return milestoneDt;
	}

	/**
	 * @return the campaignInstanceId
	 */
	public String getCampaignInstanceId() {
		return campaignInstanceId;
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
	 * @param milestoneTypeId the milestoneTypeId to set.
	 */
	public void setMilestoneTypeId(MilestoneTypeId milestoneTypeId) {
		this.milestoneTypeId = milestoneTypeId;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setMilestoneDt(Date milestoneDt) {
		this.milestoneDt = milestoneDt;
	}

	/**
	 * @param campaignInstanceId the campaignInstanceId to set.
	 */
	public void setCampaignInstanceId(String campaignInstanceId) {
		this.campaignInstanceId = campaignInstanceId;
	}
}