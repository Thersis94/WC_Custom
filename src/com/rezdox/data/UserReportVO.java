package com.rezdox.data;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title</b>: UserReportVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jul 2, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class UserReportVO {

	private static PhoneNumberFormat pnf;

	private String guid;
	private String businessName;
	private String firstName;
	private String lastName;
	private String role;
	private String address;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String phone;
	private String fmtPhone;
	private String email;
	private String status;
	private String category;
	private String subCategory;
	private String enrollDateStr;
	private Date enrollDate;
	private String website;

	//static constructor
	public UserReportVO() {
		super();
		if (pnf == null) {
			pnf = new PhoneNumberFormat();
			pnf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
		}
	}


	@Column(name="guid")
	public String getGuid() {
		return guid;
	}

	@Column(name="business_nm")
	public String getBusinessName() {
		return businessName;
	}

	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}

	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
	}

	@Column(name="role_nm")
	public String getRole() {
		return role;
	}

	@Column(name="address_txt")
	public String getAddress() {
		return address;
	}

	@Column(name="address2_txt")
	public String getAddress2() {
		return address2;
	}

	@Column(name="city_nm")
	public String getCity() {
		return city;
	}

	@Column(name="state_cd")
	public String getState() {
		return state;
	}

	@Column(name="zip_cd")
	public String getZip() {
		return zip;
	}

	@Column(name="phone_number_txt")
	public String getPhone() {
		return phone;
	}

	@Column(name="email_address_txt")
	public String getEmail() {
		return email;
	}


	@Column(name="status_nm")
	public String getStatus() {
		return status;
	}

	@Column(name="category_nm")
	public String getCategory() {
		return category;
	}

	@Column(name="sub_category_nm")
	public String getSubCategory() {
		return subCategory;
	}

	@Column(name="enroll_dt")
	public Date getEnrollDate() {
		return enrollDate;
	}

	@Column(name="website_url")
	public String getWebsite() {
		return website;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		this.setFmtPhone(phone);
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	public void setEnrollDate(Date enrollDate) {
		if (enrollDate == null) return;       
		this.enrollDate = enrollDate;
		this.setEnrollDateStr(Convert.formatDate(enrollDate, Convert.DATE_SLASH_PATTERN));
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getEnrollDateStr() {
		return enrollDateStr;
	}

	public void setEnrollDateStr(String enrollDateStr) {
		this.enrollDateStr = enrollDateStr;
	}


	public String getFmtPhone() {
		return fmtPhone;
	}


	public void setFmtPhone(String fmtPhone) {
		if (StringUtil.isEmpty(fmtPhone)) return;
		pnf.setPhoneNumber(fmtPhone);
		this.fmtPhone = pnf.getFormattedNumber();
	}
}