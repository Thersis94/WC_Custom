package com.bmg.admin.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: LocationVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores infromation about
 * a location managed by a company.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 16, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_COMPANY_LOCATION")
public class LocationVO {
	private String locationId;
	private String companyId;
	private String locationName;
	private String addressText;
	private String address2Text;
	private String cityName;
	private String stateCode;
	private String zipCode;
	private String countryCode;
	private String phoneText;
	private String altPhoneText;
	private int primaryLocFlg;
	
	public LocationVO() {
		// Default constructor created to allow creation of this
		// vo without needing a servlet request.
	}
	
	public LocationVO(SMTServletRequest req) {
		setData(req);
	}
	
	public void setData(SMTServletRequest req) {
		locationId = req.getParameter("locationId");
		companyId = req.getParameter("companyId");
		locationName = req.getParameter("locationName");
		addressText = req.getParameter("addressText");
		address2Text = req.getParameter("address2Text");
		cityName = req.getParameter("cityName");
		stateCode = req.getParameter("stateCode");
		zipCode = req.getParameter("zipCode");
		countryCode = req.getParameter("countryCode");
		phoneText = req.getParameter("phoneText");
		altPhoneText = req.getParameter("altPhoneText");
		primaryLocFlg = Convert.formatInteger(req.getParameter("primaryLocFlg"));
	}

	@Column(name="location_id", isPrimaryKey=true)
	public String getLocationId() {
		return locationId;
	}
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	@Column(name="location_nm")
	public String getLocationName() {
		return locationName;
	}
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	@Column(name="address_txt")
	public String getAddressText() {
		return addressText;
	}
	public void setAddressText(String addressText) {
		this.addressText = addressText;
	}
	@Column(name="address2_txt")
	public String getAddress2Text() {
		return address2Text;
	}
	public void setAddress2Text(String address2Text) {
		this.address2Text = address2Text;
	}
	@Column(name="city_nm")
	public String getCityName() {
		return cityName;
	}
	public void setCityName(String cityName) {
		this.cityName = cityName;
	}
	@Column(name="state_cd")
	public String getStateCode() {
		return stateCode;
	}
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}
	@Column(name="zip_cd")
	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	@Column(name="country_cd")
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	@Column(name="phone_txt")
	public String getPhoneText() {
		return phoneText;
	}
	public void setPhoneText(String phoneText) {
		this.phoneText = phoneText;
	}
	@Column(name="alt_phone_txt")
	public String getAltPhoneText() {
		return altPhoneText;
	}
	public void setAltPhoneText(String altPhoneText) {
		this.altPhoneText = altPhoneText;
	}
	@Column(name="primary_locn_flg")
	public int getPrimaryLocFlg() {
		return primaryLocFlg;
	}
	public void setPrimaryLocFlg(int primaryLocFlg) {
		this.primaryLocFlg = primaryLocFlg;
	}
	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

}
