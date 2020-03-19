package com.rezdox.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.data.ProjectFormProcessor.FormSlug;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	private String projectName;
	private String descriptionText;
	private String residenceId;
	private String residenceName;
	private String roomId;
	private String roomName;
	private String businessId;
	private BusinessVO business;
	private String projectCategoryCd;
	private String projectCategoryName;
	private String projectTypeCd;
	private String projectTypeName;
	private double laborNo;
	private double totalNo;
	private double projectDiscountNo;
	private double projectTaxNo;
	private double materialDiscountNo;
	private double materialTaxNo;
	private UserDataVO homeowner;
	private Date endDate;
	private int residenceViewFlg;
	private int businessViewFlg;
	private List<ProjectMaterialVO> materials;
	private List<ProjectAttributeVO> attributes;
	private Map<String, String> attributeMap;
	private int photoCnt;

	private double productSubtotalNo;
	private String mainPhone;
	private double projectValuation;


	public ProjectVO() {
		super();
		attributes = new ArrayList<>();
	}

	/**
	 * @param req
	 * @return
	 */
	public static ProjectVO instanceOf(ActionRequest req) {
		ProjectVO vo = new ProjectVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
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

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="end_dt")
	public Date getEndDate() {
		return endDate;
	}

	@Column(name="desc_txt")
	public String getDescriptionText() {
		return descriptionText;
	}

	public List<ProjectMaterialVO> getMaterials() {
		return materials;
	}

	public List<ProjectAttributeVO> getAttributes() {
		return attributes;
	}

	@BeanSubElement
	public void addAttribute(ProjectAttributeVO attr) {
		attributes.add(attr);
	}

	public void addAttribute(String slug, String value) {
		ProjectAttributeVO attr = new ProjectAttributeVO();
		attr.setSlugTxt(slug);
		attr.setValueTxt(value);
		attributes.add(attr);
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
		for (ProjectAttributeVO vo : attributes)
			attributeMap.put(vo.getSlugTxt(),  vo.getValueTxt());
	}

	@Column(name="room_nm", isReadOnly=true)
	public String getRoomName() {
		return roomName;
	}

	@Column(name="residence_nm", isReadOnly=true)
	public String getResidenceName() {
		return StringUtil.checkVal(residenceName, getAttribute(FormSlug.PROJECT_NAME.name()));
	}

	@Column(name="category_nm", isReadOnly=true)
	public String getProjectCategoryName() {
		return projectCategoryName;
	}

	@Column(name="type_nm", isReadOnly=true)
	public String getProjectTypeName() {
		return projectTypeName;
	}




	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setResidenceId(String residenceId) {
		this.residenceId = StringUtil.checkVal(residenceId, null);
	}

	public void setRoomId(String roomId) {
		this.roomId = StringUtil.checkVal(roomId, null);
	}

	public void setBusinessId(String businessId) {
		this.businessId = StringUtil.checkVal(businessId, null);
	}

	public void setProjectCategoryCd(String projectCategoryCd) {
		this.projectCategoryCd = StringUtil.checkVal(projectCategoryCd, null);
	}

	public void setProjectTypeCd(String projectTypeCd) {
		this.projectTypeCd = StringUtil.checkVal(projectTypeCd, null);
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

	@BeanSubElement
	public void addMaterial(ProjectMaterialVO mvo) {
		if (this.materials == null)
			this.materials = new ArrayList<>();
		
		if (mvo != null)
			this.materials.add(mvo);
	}
	
	public void setMaterials(List<ProjectMaterialVO> materials) {
		this.materials = materials;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public void setResidenceName(String residenceName) {
		this.residenceName = residenceName;
	}

	public void setProjectCategoryName(String projectCategoryName) {
		this.projectCategoryName = projectCategoryName;
	}

	public void setProjectTypeName(String projectTypeName) {
		this.projectTypeName = projectTypeName;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public UserDataVO getHomeowner() {
		return homeowner;
	}

	public void setHomeowner(UserDataVO homeowner) {
		this.homeowner = homeowner;
	}

	public void setHomeownerProfileId(String profileId) {
		if (getHomeowner() == null) setHomeowner(new UserDataVO());
		getHomeowner().setProfileId(profileId);
	}

	@Column(name="homeowner_profile_id", isReadOnly=true)
	public String getHomeownerProfileId() {
		return getHomeowner() != null ? getHomeowner().getProfileId() : null;
	}

	public void setDescriptionText(String descriptionText) {
		this.descriptionText = descriptionText;
	}


	/**
	 * used in JSPs - homogenizes Connected and Non-Connected customer contact
	 * @return
	 */
	public UserDataVO getOwner() {
		if (homeowner != null && !StringUtil.isEmpty(homeowner.getProfileId())) return homeowner;

		homeowner = new UserDataVO();
		homeowner.setName(getAttribute(FormSlug.PROJECT_OWNER.name()));
		homeowner.setEmailAddress(getAttribute(FormSlug.PROJECT_EMAIL.name()));
		homeowner.setMainPhone(getAttribute(FormSlug.PROJECT_PHONE.name()));
		homeowner.setAddress(getAttribute(FormSlug.PROJECT_ADDRESS.name()));
		homeowner.setCity(getAttribute(FormSlug.PROJECT_CITY.name()));
		homeowner.setState(getAttribute(FormSlug.PROJECT_STATE.name()));
		homeowner.setZipCode(getAttribute(FormSlug.PROJECT_ZIP_CODE.name()));
		return homeowner;
	}


	/**
	 * used in JSPs - homogenizes Connected and Non-Connected Business (aka Provider, aka Vendor) contact.
	 * @return
	 */
	public BusinessVO getProvider() {
		if (business != null) return business;

		business = new BusinessVO();
		business.setBusinessName(getAttribute(FormSlug.PROJECT_OWNER.name()));
		business.setMainPhoneText(getMainPhone());
		business.setEmailAddressText(getAttribute(FormSlug.PROJECT_EMAIL.name()));
		business.setMainPhoneText(getAttribute(FormSlug.PROJECT_PHONE.name()));
		return business;
	}

	public BusinessVO getBusiness() {
		return business;
	}

	public void setBusiness(BusinessVO business) {
		this.business = business;
	}

	/**
	 * tax & discounts get applied on the invoice page, not the Edit form, so these fours fields are annotated read-only
	 * @return
	 */
	@Column(name="proj_discount_no", isReadOnly=true)
	public double getProjectDiscountNo() {
		return projectDiscountNo >= 1 ? projectDiscountNo/100 : projectDiscountNo;
	}

	@Column(name="proj_tax_no", isReadOnly=true)
	public double getProjectTaxNo() {
		return projectTaxNo >= 1 ? projectTaxNo/100 : projectTaxNo;
	}

	@Column(name="mat_discount_no", isReadOnly=true)
	public double getMaterialDiscountNo() {
		return materialDiscountNo >= 1 ? materialDiscountNo/100 : materialDiscountNo;
	}

	@Column(name="mat_tax_no", isReadOnly=true)
	public double getMaterialTaxNo() {
		return materialTaxNo >= 1 ? materialTaxNo/100 : materialTaxNo;
	}

	public void setProjectDiscountNo(double projectDiscountNo) {
		this.projectDiscountNo = projectDiscountNo;
	}

	public void setProjectTaxNo(double projectTaxNo) {
		this.projectTaxNo = projectTaxNo;
	}

	public void setMaterialDiscountNo(double materialDiscountNo) {
		this.materialDiscountNo = materialDiscountNo;
	}

	public void setMaterialTaxNo(double materialTaxNo) {
		this.materialTaxNo = materialTaxNo;
	}

	public double getAppliedDiscount() {
		return Convert.round(getTotalNo() * getProjectDiscountNo(), 2);
	}

	public double getAppliedTax() {
		return Convert.round((getTotalNo() - getAppliedDiscount()) * getProjectTaxNo(), 2);
	}

	public double getAppliedProjectTotal() {
		return Convert.round(getTotalNo() - getAppliedDiscount() + getAppliedTax(), 2);
	}

	@Column(name="material_cost", isReadOnly=true)
	public double getMaterialSubtotal() {
		return productSubtotalNo;
	}

	public void setMaterialSubtotal(double d) {
		this.productSubtotalNo = d;
	}

	public double getAppliedMaterialDiscount() {
		return Convert.round(getMaterialSubtotal() * getMaterialDiscountNo(), 2);
	}

	public double getAppliedMaterialTax() {
		return Convert.round((getMaterialSubtotal() - getAppliedMaterialDiscount()) * getMaterialTaxNo(), 2);
	}

	public double getAppliedMaterialTotal() {
		return Convert.round(getMaterialSubtotal() - getAppliedMaterialDiscount() + getAppliedMaterialTax(), 2);
	}

	public double getInvoiceTotal() {
		return getAppliedProjectTotal() + getAppliedMaterialTotal();
	}

	public double getInvoiceSubTotal() {
		return getTotalNo() + getMaterialSubtotal();
	}

	@Column(name="main_phone_txt", isReadOnly=true)
	public String getMainPhone() {
		return mainPhone;
	}

	public void setMainPhone(String mainPhone) {
		this.mainPhone = mainPhone;
	}

	@Column(name="photo_cnt", isReadOnly=true)
	public int getPhotoCnt() {
		return photoCnt;
	}

	public void setPhotoCnt(int photoCnt) {
		this.photoCnt = photoCnt;
	}

	@Column(name="project_valuation", isReadOnly=true)
	public double getProjectValuation() {
		
		
		return projectValuation;
	}

	public void setProjectValuation(double projectValuation) {
		this.projectValuation = projectValuation;
	}
}