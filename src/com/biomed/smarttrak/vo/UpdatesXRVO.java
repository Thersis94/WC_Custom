/**
 *
 */
package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: UpdatesXRVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for managing udpate xr data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 15, 2017
 ****************************************************************************/
@Table(name="biomedgps_update_section")
public class UpdatesXRVO {

	private String updateSectionXrId;
	private String sectionId;
	private String updateId;
	private Date createDt;

	public UpdatesXRVO() {
		super();
	}

	/**
	 * @param req
	 */
	public UpdatesXRVO(String updateId, String sectionId) {
		this.updateId = updateId;
		this.sectionId = sectionId;
	}

	/**
	 * @return the updateSectionXrId
	 */
	@Column(name="update_section_xr_id", isPrimaryKey=true)
	public String getUpdateSectionXrId() {
		return updateSectionXrId;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="section_id")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the updateId
	 */
	@Column(name="update_id")
	public String getUpdateId() {
		return updateId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param updateSectionXrId the updateSectionXrId to set.
	 */
	public void setUpdateSectionXrId(String updateSectionXrId) {
		this.updateSectionXrId = updateSectionXrId;
	}

	/**
	 * @param sectionId the sectionId to set.
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param updateId the updateId to set.
	 */
	public void setUpdateId(String updateId) {
		this.updateId = updateId;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}