package com.rezdox.vo;

import java.io.Serializable;

//SMTBaseLibs
import com.siliconmtn.db.orm.Column;
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
public class MyProVO implements Serializable {

	private static final long serialVersionUID = 2364416015206367281L;

	private String businessId;
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

	@Column(name="business_id", isPrimaryKey=true)
	public String getBusinessId() {
		return businessId;
	}

	@Column(name="business_nm")
	public String getBusinessName() {
		return businessName;
	}

	@Column(name="category_nm")
	public String getCategoryName() {
		return categoryName;
	}

	@Column(name="specialty_nm")
	public String getSpecialtyName() {
		return specialtyName;
	}

	@Column(name="main_phone_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Column(name="profile_pic_pth")
	public String getProfileImgPath() {
		return profileImgPath;
	}

	@Column(name="photo_url")
	public String getLogoImgPath() {
		return logoImgPath;
	}

	@Column(name="first_nm")
	public String getOwnerFirstName() {
		return ownerFirstName;
	}

	@Column(name="last_nm")
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

}