/**
 *
 */
package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
@Table(name="BIOMEDGPS_INSIGHT_SECTION")
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
	@Column(name="INSIGHT_SECTION_XR_ID", isPrimaryKey=true)
	public String getInsightSectionXrId() {
		return insightSectionXrId;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="SECTION_ID")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the insightId
	 */
	@Column(name="INSIGHT_ID")
	public String getInsightId() {
		return insightId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
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
}