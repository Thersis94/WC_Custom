package com.rezdox.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> ProjectMaterialVO.java<br/>
 * <b>Description:</b> RezDox Project Material - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2018
 ****************************************************************************/
@Table(name="REZDOX_PROJECT_MATERIAL")
public class ProjectMaterialVO {

	private String projectMaterialId;
	private String projectId;
	private String materialName;
	private int quantityNo;
	private double costNo;
	private List<ProjectMaterialAttributeVO> attributes;
	private Map<String, String> attributeMap;

	public ProjectMaterialVO() {
		super();
		attributes = new ArrayList<>();
	}

	/**
	 * @param req
	 * @return
	 */
	public static ProjectMaterialVO instanceOf(ActionRequest req) {
		ProjectMaterialVO vo = new ProjectMaterialVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}

	@Column(name="project_material_id", isPrimaryKey=true)
	public String getProjectMaterialId() {
		return projectMaterialId;
	}

	@Column(name="project_id")
	public String getProjectId() {
		return projectId;
	}

	@Column(name="material_nm")
	public String getMaterialName() {
		return materialName;
	}

	@Column(name="quantity_no")
	public int getQuantityNo() {
		return quantityNo;
	}

	@Column(name="cost_no")
	public double getCostNo() {
		return costNo;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}

	public List<ProjectMaterialAttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param projectOwner
	 * @return
	 */
	public String getAttribute(String slug) {
		if (attributeMap == null) buildAttributeMap();
		return attributeMap.get(slug);
	}

	/**
	 * one-time builder of the attributes map
	 */
	private void buildAttributeMap() {
		attributeMap = new HashMap<>();
		if (attributes == null) return;
		for (ProjectMaterialAttributeVO vo : attributes)
			attributeMap.put(vo.getSlugTxt(),  vo.getValueTxt());
	}


	public void setProjectMaterialId(String projectMaterialId) {
		this.projectMaterialId = projectMaterialId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public void setQuantityNo(int quantityNo) {
		this.quantityNo = quantityNo;
	}

	public void setCostNo(double costNo) {
		this.costNo = costNo;
	}

	public void setAttributes(List<ProjectMaterialAttributeVO> attributes) {
		this.attributes = attributes;
	}

	@BeanSubElement
	public void addAttribute(ProjectMaterialAttributeVO attr) {
		attributes.add(attr);
	}


	/**
	 * total cost, as a calculation of qnty * unit cost
	 * @return
	 */
	public double getTotalCostNo() {
		return quantityNo * costNo;
	}
}