package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: InsightXRVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for managing insight xr data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 15, 2017
 ****************************************************************************/
@Table(name="biomedgps_insight_section")
public class InsightXRVO {

	private String insightSectionXrId;
	private String sectionId;
	private String insightId;
	private Date createDt;

	public InsightXRVO() {
		super();
	}

	/**
	 * @param req
	 */
	public InsightXRVO(String insightId, String sectionId) {
		this.insightId = insightId;
		this.sectionId = sectionId;
	}

	/**
	 * @return the insightSectionXrId
	 */
	@Column(name="insight_section_xr_id", isPrimaryKey=true)
	public String getInsightSectionXrId() {
		return insightSectionXrId;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="section_id")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the insightId
	 */
	@Column(name="insight_id")
	public String getInsightId() {
		return insightId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param insightSectionXrId the insightSectionXrId to set.
	 */
	public void setInsightSectionXrId(String insightSectionXrId) {
		this.insightSectionXrId = insightSectionXrId;
	}

	/**
	 * @param sectionId the sectionId to set.
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param insightId the insightId to set.
	 */
	public void setInsightId(String insightId) {
		this.insightId = insightId;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
}