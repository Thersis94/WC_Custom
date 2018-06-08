package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

//SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MyProVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO returned in the JSON response when we load My Pros dropdown.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * @author James McKain
 * @version 1.0
 * @since Apr 10, 2018
 * @updates:
 ****************************************************************************/
@Table(name="rezdox_my_pro")
public class MyProVO implements Serializable {

	private static final long serialVersionUID = 2364416015206367281L;

	private String myProId;
	private String memberId;
	private String businessId;
	private String categoryId;
	private Date createDate;
	private Date updateDate;

	//display values
	private String businessName;
	private String categoryName;
	private String specialtyName;
	private String phoneNumber;
	private String profileImgPath;
	private String logoImgPath;
	private String ownerFirstName;
	private String ownerLastName;

	public MyProVO() {
		super();
	}


	/**
	 * Create a new VO using data auto-filled off the request.
	 * Request parameter names must match setter method names, sans the "set".
	 * e.g. setFirstName -> req.getParameter("firstName"); 
	 * @param req
	 * @return
	 */
	public static MyProVO instanceOf(ActionRequest req) {
		MyProVO vo = new MyProVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}

	@Column(name="my_pro_id", isPrimaryKey=true)
	public String getMyProId() {
		return myProId;
	}

	@Column(name="business_id")
	public String getBusinessId() {
		return businessId;
	}

	@Column(name="member_id")
	public String getMemberId() {
		return memberId;
	}

	@Column(name="business_category_cd")
	public String getCategoryId() {
		return categoryId;
	}

	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name="business_nm", isReadOnly=true)
	public String getBusinessName() {
		return businessName;
	}

	@Column(name="category_nm", isReadOnly=true)
	public String getCategoryName() {
		return categoryName;
	}

	@Column(name="specialty_nm", isReadOnly=true)
	public String getSpecialtyName() {
		return specialtyName;
	}

	@Column(name="main_phone_txt", isReadOnly=true)
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Column(name="profile_pic_pth", isReadOnly=true)
	public String getProfileImgPath() {
		return profileImgPath;
	}

	@Column(name="photo_url", isReadOnly=true)
	public String getLogoImgPath() {
		return logoImgPath;
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getOwnerFirstName() {
		return ownerFirstName;
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getOwnerLastName() {
		return ownerLastName;
	}

	public String getImage() {
		return !StringUtil.isEmpty(logoImgPath) ? logoImgPath : profileImgPath; 
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public void setSpecialtyName(String specialtyName) {
		this.specialtyName = specialtyName;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setProfileImgPath(String profileImgPath) {
		this.profileImgPath = profileImgPath;
	}

	public void setLogoImgPath(String logoImgPath) {
		this.logoImgPath = logoImgPath;
	}

	public void setOwnerFirstName(String ownerFirstName) {
		this.ownerFirstName = ownerFirstName;
	}

	public void setOwnerLastName(String ownerLastName) {
		this.ownerLastName = ownerLastName;
	}

	public void setMyProId(String myProId) {
		this.myProId = myProId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}
}