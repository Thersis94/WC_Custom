package com.biomed.smarttrak.vo;

//Java 7
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

import com.siliconmtn.gis.Location;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: AccountVO.java</p>
 <p><b>Description: </b>VO representing a Smarttrak account.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="BIOMEDGPS_ACCOUNT")
public class AccountVO {
	private String accountId;
	private String companyId;
	private String accountName;
	private String typeId;
	private String ownerProfileId;
	private Location location;
	private String statusNo;
	private Date startDate;
	private Date expirationDate;
	private Date createDate;
	private Date updateDate;
	private String firstName;
	private String lastName;

	public AccountVO() {
		super();
		location = new Location();
	}

	public AccountVO(ResultSet rs) {
		this();
		DBUtil db = new DBUtil();
		setAccountId(db.getStringVal("account_id", rs));
		setAccountName(db.getStringVal("account_nm", rs));
		setCompanyId(db.getStringVal("company_id", rs));
		setTypeId(db.getStringVal("type_id", rs));
		setOwnerProfileId(db.getStringVal("owner_profile_id", rs));
		setStatusNo(db.getStringVal("status_no", rs));
		setStartDate(db.getDateVal("start_dt", rs));
		setExpirationDate(db.getDateVal("expiration_dt", rs));
		setCreateDate(db.getDateVal("create_dt", rs));
		setUpdateDate(db.getDateVal("update_dt", rs));
		setAddress(db.getStringVal("address_txt", rs));
		setAddress2(db.getStringVal("address2_txt", rs));
		setCity(db.getStringVal("city_nm", rs));
		setState(db.getStringVal("state_cd", rs));
		setZipCode(db.getStringVal("zip_cd", rs));
		setCountry(db.getStringVal("country_cd", rs));
	}

	public AccountVO(ActionRequest req) {
		this();
		setAccountId(req.getParameter("accountId"));
		setAccountName(req.getParameter("accountName"));
		setCompanyId(req.getParameter("companyId"));
		setTypeId(req.getParameter("typeId"));
		setOwnerProfileId(req.getParameter("ownerProfileId"));
		setStatusNo(req.getParameter("statusNo"));
		setStartDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate")));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expirationDate")));
		setAddress(req.getParameter("addressText"));
		setAddress2(req.getParameter("address2Text"));
		setCity(req.getParameter("cityName"));
		setState(req.getParameter("stateCode"));
		setZipCode(req.getParameter("zipCode"));
		setCountry(req.getParameter("countryCode"));
	}


	/**
	 * @return the accountId
	 */
	@Column(name="account_id", isPrimaryKey=true)
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the accountName
	 */
	@Column(name="account_nm")
	public String getAccountName() {
		return accountName;
	}

	/**
	 * @param accountName the accountName to set
	 */
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @return the typeId
	 */
	@Column(name="type_id")
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	/**
	 * @param stringVal
	 */
	public void setAddress(String stringVal) {
		getLocation().setAddress(stringVal);		
	}


	/**
	 * @param stringVal
	 */
	public void setAddress2(String stringVal) {
		getLocation().setAddress2(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setCity(String stringVal) {
		getLocation().setCity(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setState(String stringVal) {
		getLocation().setState(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setZipCode(String stringVal) {
		getLocation().setZipCode(stringVal);
	}

	/**
	 * @param stringVal
	 */
	public void setCountry(String stringVal) {
		getLocation().setCountry(stringVal);
	}

	/**
	 * @return the ownerProfileId
	 */
	@Column(name="owner_profile_id")
	public String getOwnerProfileId() {
		return ownerProfileId;
	}

	/**
	 * @param ownerProfileId the ownerProfileId to set
	 */
	public void setOwnerProfileId(String ownerProfileId) {
		this.ownerProfileId = ownerProfileId;
	}

	/**
	 * @return the location
	 *NOTE no setter for location - we don't want outsiders to be able to nullify it, or our setter/getters around address will fail
	 */
	public Location getLocation() {
		return location;
	}

	@Column(name="address_txt")
	public String getAddress() {
		return getLocation().getAddress();
	}

	@Column(name="address2_txt")
	public String getAddress2() {
		return getLocation().getAddress2();
	}

	@Column(name="city_nm")
	public String getCity() {
		return getLocation().getCity();
	}

	@Column(name="state_cd")
	public String getState() {
		return getLocation().getState();
	}

	@Column(name="zip_cd")
	public String getZipCode() {
		return getLocation().getZipCode();
	}

	@Column(name="country_cd")
	public String getCountry() {
		return getLocation().getCountry();
	}

	/**
	 * @return the statusNo
	 */
	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}

	public String getStatusName() {
		switch (StringUtil.checkVal(getStatusNo())) {
			case "A":
				return "Active";
			case "S":
				return "Staff";
			case "T":
				return "Trial";
			case "U":
				return "Updates";
			default:
				return "Inactive";
		}
	}

	/**
	 * @param statusNo the statusNo to set
	 */
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}

	/**
	 * @return the startDate
	 */
	@Column(name="start_dt")
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the expirationDate
	 */
	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}