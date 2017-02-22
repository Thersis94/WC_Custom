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

	public enum InsightSection {
		EXTREMITIES(5, "Extremities"),
		TOTAL_JOINT(10, "Total Joint"),
		TRAUMA(15, "Trauma"),
		EU_TRAUMA(20, "EU Trauma"),
		SPINE(25, "Spine"),
		ORTHOBIO(30, "Ortho-Bio"),
		SOFT_TISSUE(35, "Soft Tissue"),
		ADV_WOUND_CARE(40, "Adv. Wound Care"),
		EU_ADV_WOUND_CARE(45, "EU Adv. Wound Care"),
		SURGICAL_MATRICIES(505, "Surgical Matricies"),
		INF_PREV(55, "Inf Prev"),
		GLUES_AND_SEALANTS(60, "Glues & Sealants"),
		WND_MGMT_STD_OF_CARE(65, "Wnd Mgmt - Std of Care"),
		REGEN_MED(70, "Regen Med"),
		NEUROVASCULAR(75, "Neurovascular"),
		NEUROMODULATION(80, "Neuromodulation");

		private int val;
		private String text;

		InsightSection(int val, String text) {
			this.val = val;
			this.text = text;
		}

		public int getVal() {
			return this.val;
		}
		public String getText() {
			return this.text;
		}
	}

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

	/**
	 * Helper method gets the InsightSection for the internal typeCd.
	 * @return
	 */
	public InsightSection getInsightSection() {

		for(InsightSection sec : InsightSection.values()){
			if (sec.getText().equals("sectionId")){
				return sec;
			}
		}
		return null;
	}
}