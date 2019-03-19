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
public class WarrantyReportVO {

	private static PhoneNumberFormat pnf;

	private String guid;
	private String firstName;
	private String lastName;
	private String address;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String phone;
	private String fmtPhone;
	private String email;
	private String model;
	private String serial;
	private String purchaseLocation;
	private Date purchaseDate;
	private String purchaseDateStr;
	private String manufacturer;
	private String brand;
	private String source;

	//static constructor
	public WarrantyReportVO() {
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

	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}

	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
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

	@Column(name="model")
	public String getModel() {
		return model;
	}

	@Column(name="serial")
	public String getSerial() {
		return serial;
	}

	@Column(name="retailer")
	public String getPurchaseLocation() {
		return purchaseLocation;
	}

	@Column(name="purchase_dt")
	public Date getPurchaseDate() {
		return purchaseDate;
	}

	@Column(name="manufacturer")
	public String getManufacturer() {
		return manufacturer;
	}

	@Column(name="brand")
	public String getBrand() {
		return brand;
	}

	@Column(name="source")
	public String getSource() {
		return source;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
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

	public void setPurchaseDate(Date purchaseDate) {
		if (purchaseDate == null) return;       
		this.purchaseDate = purchaseDate;
		setPurchaseDateStr(Convert.formatDate(purchaseDate, Convert.DATE_SLASH_PATTERN));
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getPurchaseDateStr() {
		return purchaseDateStr;
	}

	public void setPurchaseDateStr(String purchaseDateStr) {
		this.purchaseDateStr = purchaseDateStr;
	}

	public String getFmtPhone() {
		return fmtPhone;
	}

	public void setFmtPhone(String fmtPhone) {
		if (StringUtil.isEmpty(fmtPhone)) return;
		pnf.setPhoneNumber(fmtPhone);
		this.fmtPhone = pnf.getFormattedNumber();
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public void setPurchaseLocation(String purchaseLocation) {
		this.purchaseLocation = purchaseLocation;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}
}