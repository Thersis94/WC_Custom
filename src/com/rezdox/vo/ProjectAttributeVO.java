package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> ProjectAttributeVO.java<br/>
 * <b>Description:</b> RezDox project attribute.  See data model
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2018
 ****************************************************************************/
@Table(name="REZDOX_PROJECT_ATTRIBUTE")
public class ProjectAttributeVO {

	private String attributeId;
	private String projectId;
	private String slugTxt;
	private String valueTxt;

	public ProjectAttributeVO() {
		super();
	}


	@Column(name="attribute_id", isPrimaryKey=true)
	public String getAttributeId() {
		return attributeId;
	}

	@Column(name="projectId")
	public String getProjectId() {
		return projectId;
	}

	@Column(name="slug_txt")
	public String getSlugTxt() {
		return slugTxt;
	}

	@Column(name="value_txt")
	public String getValueTxt() {
		return valueTxt;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}


	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setSlugTxt(String slugTxt) {
		this.slugTxt = slugTxt;
	}

	public void setValueTxt(String valueTxt) {
		this.valueTxt = valueTxt;
	}	
}