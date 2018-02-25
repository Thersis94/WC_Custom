package com.rezdox.vo;

import java.util.List;

import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> ProjectVO.java<br/>
 * <b>Description:</b> RezDox Project data container - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2018
 ****************************************************************************/
@Table(name="REZDOX_PROJECT")
public class ProjectVO {

	private String projectId;
	private String residenceId;
	private String roomId;
	private String businessId;
	private String projectCategoryCd;
	private String projectTypeCd;
	private String projectName;
	private double laborNo;
	private double totalNo;
	private int residenceViewFlg;
	private int businessViewFlg;
	private List<ProjectMaterialVO> materials;
	private List<ProjectAttributeVO> attributes;


	public ProjectVO() {
		super();
	}

	@Column(name="project_id", isPrimaryKey=true)
	public String getProjectId() {
		return projectId;
	}

	@Column(name="residence_id")
	public String getResidenceId() {
		return residenceId;
	}

	@Column(name="room_id")
	public String getRoomId() {
		return roomId;
	}

	@Column(name="business_id")
	public String getBusinessId() {
		return businessId;
	}

	@Column(name="project_category_cd")
	public String getProjectCategoryCd() {
		return projectCategoryCd;
	}

	@Column(name="project_type_cd")
	public String getProjectTypeCd() {
		return projectTypeCd;
	}

	@Column(name="project_nm")
	public String getProjectName() {
		return projectName;
	}

	@Column(name="labor_no")
	public double getLaborNo() {
		return laborNo;
	}

	@Column(name="total_no")
	public double getTotalNo() {
		return totalNo;
	}

	@Column(name="residence_view_flg")
	public int getResidenceViewFlg() {
		return residenceViewFlg;
	}

	@Column(name="business_view_flg")
	public int getBusinessViewFlg() {
		return businessViewFlg;
	}

	//@BeanSubElement  - This method is NOT annotated because it's not part of the SQL query that populates this VO.
	public List<ProjectMaterialVO> getMaterials() {
		return materials;
	}

	@BeanSubElement
	public List<ProjectAttributeVO> getAttributes() {
		return attributes;
	}


	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setResidenceId(String residenceId) {
		this.residenceId = residenceId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}


	public void setProjectCategoryCd(String projectCategoryCd) {
		this.projectCategoryCd = projectCategoryCd;
	}

	public void setProjectTypeCd(String projectTypeCd) {
		this.projectTypeCd = projectTypeCd;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setLaborNo(double laborNo) {
		this.laborNo = laborNo;
	}

	public void setTotalNo(double totalNo) {
		this.totalNo = totalNo;
	}

	public void setResidenceViewFlg(int residenceViewFlg) {
		this.residenceViewFlg = residenceViewFlg;
	}

	public void setBusinessViewFlg(int businessViewFlg) {
		this.businessViewFlg = businessViewFlg;
	}

	public void setMaterials(List<ProjectMaterialVO> materials) {
		this.materials = materials;
	}
}