package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> ProjectMaterialAttributeVO.java<br/>
 * <b>Description:</b> RezDox project material attribute - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2018
 ****************************************************************************/
@Table(name="REZDOX_PROJECT_MATERIAL_ATTRIBUTE")
public class ProjectMaterialAttributeVO {

	private String attributeId;
	private String projectMaterialId;
	private String slugTxt;
	private String valueTxt;

	public ProjectMaterialAttributeVO() {
		super();
	}


	@Column(name="attribute_id", isPrimaryKey=true)
	public String getAttributeId() {
		return attributeId;
	}

	@Column(name="project_material_id")
	public String getProjectMaterialId() {
		return projectMaterialId;
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

	public void setProjectMaterialId(String projectMaterialId) {
		this.projectMaterialId = projectMaterialId;
	}

	public void setSlugTxt(String slugTxt) {
		this.slugTxt = slugTxt;
	}

	public void setValueTxt(String valueTxt) {
		this.valueTxt = valueTxt;
	}
}