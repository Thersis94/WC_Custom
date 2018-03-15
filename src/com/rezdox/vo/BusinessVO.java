package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
import com.siliconmtn.action.ActionRequest;
// SMTBaseLibs
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.security.PhoneVO;

/*****************************************************************************
 <p><b>Title</b>: BusinessVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox business.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Mar 8, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_BUSINESS")
public class BusinessVO extends GeocodeLocation implements Serializable {
	private static final long serialVersionUID = -6288149815547303962L;

	private String businessId;
	private String businessName;
	private PhoneVO mainPhone;
	private PhoneVO altPhone;
	private String emailAddressText;
	private String websiteUrl;
	private String photoUrl;
	private String adFileUrl;
	private int privacyFlag;
	private transient Map<String, Object> attributes;
	private String subCategoryCd;
	private String categoryCd;
	private BusinessStatus status;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * Special use keys for values from the attributes table in the attibutes map
	 */
	private static final String SUMMARY = "summary";

	public BusinessVO() {
		super();
		attributes = new HashMap<>();
		mainPhone = new PhoneVO();
		altPhone = new PhoneVO();
	}

	/**
	 * @param req
	 */
	public BusinessVO(ActionRequest req) {
		this();
		populateData(req);
	}
	
	/**
	 * @return the businessId
	 */
	@Column(name="business_id", isPrimaryKey=true)
	public String getBusinessId() {
		return businessId;
	}

	/**
	 * @param businessId the businessId to set
	 */
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	/**
	 * @return the businessName
	 */
	@Column(name="business_nm")
	public String getBusinessName() {
		return businessName;
	}

	/**
	 * @param businessName the businessName to set
	 */
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	/**
	 * @return the mainPhone
	 */
	public PhoneVO getMainPhone() {
		return mainPhone;
	}

	/**
	 * @param mainPhone the mainPhone to set
	 */
	public void setMainPhone(PhoneVO mainPhone) {
		this.mainPhone = mainPhone;
	}

	/**
	 * @return the mainPhoneText
	 */
	@Column(name="main_phone_txt")
	public String getMainPhoneText() {
		return mainPhone.getPhoneNumber();
	}

	/**
	 * @param mainPhoneText the mainPhoneText to set
	 */
	public void setMainPhoneText(String mainPhoneText) {
		mainPhone.setPhoneNumber(mainPhoneText);
	}

	/**
	 * @return the altPhone
	 */
	public PhoneVO getAltPhone() {
		return altPhone;
	}

	/**
	 * @param altPhone the altPhone to set
	 */
	public void setAltPhone(PhoneVO altPhone) {
		this.altPhone = altPhone;
	}

	/**
	 * @return the altPhoneText
	 */
	@Column(name="alt_phone_txt")
	public String getAltPhoneText() {
		return altPhone.getPhoneNumber();
	}

	/**
	 * @param altPhoneText the altPhoneText to set
	 */
	public void setAltPhoneText(String altPhoneText) {
		altPhone.setPhoneNumber(altPhoneText);
	}

	/**
	 * @return the emailAddressText
	 */
	@Column(name="email_address_txt")
	public String getEmailAddressText() {
		return emailAddressText;
	}

	/**
	 * @param emailAddressText the emailAddressText to set
	 */
	public void setEmailAddressText(String emailAddressText) {
		this.emailAddressText = emailAddressText;
	}

	/**
	 * @return the websiteUrl
	 */
	@Column(name="website_url")
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	/**
	 * @param websiteUrl the websiteUrl to set
	 */
	public void setWebsiteUrl(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	/**
	 * @return the photoUrl
	 */
	@Column(name="photo_url")
	public String getPhotoUrl() {
		return photoUrl;
	}

	/**
	 * @param photoUrl the photoUrl to set
	 */
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	/**
	 * @return the adFileUrl
	 */
	@Column(name="ad_file_url")
	public String getAdFileUrl() {
		return adFileUrl;
	}

	/**
	 * @param adFileUrl the adFileUrl to set
	 */
	public void setAdFileUrl(String adFileUrl) {
		this.adFileUrl = adFileUrl;
	}

	/**
	 * @return the privacyFlag
	 */
	@Column(name="privacy_flg")
	public int getPrivacyFlag() {
		return privacyFlag;
	}

	/**
	 * @param privacyFlag the privacyFlag to set
	 */
	public void setPrivacyFlag(int privacyFlag) {
		this.privacyFlag = privacyFlag;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the summaryText
	 */
	@Column(name="summary_txt")
	public String getSummaryText() {
		return (String) attributes.get(SUMMARY);
	}

	/**
	 * @param summaryText the summaryText to set
	 */
	public void setSummaryText(String summaryText) {
		attributes.put(SUMMARY, summaryText);
	}

	/**
	 * @return the subCategoryCd
	 */
	@Column(name="sub_category_cd", isReadOnly=true)
	public String getSubCategoryCd() {
		return subCategoryCd;
	}

	/**
	 * @param subCategoryCd the subCategoryCd to set
	 */
	public void setSubCategoryCd(String subCategoryCd) {
		this.subCategoryCd = subCategoryCd;
	}

	/**
	 * @return the categoryCd
	 */
	@Column(name="category_cd", isReadOnly=true)
	public String getCategoryCd() {
		return categoryCd;
	}

	/**
	 * @param categoryCd the categoryCd to set
	 */
	public void setCategoryCd(String categoryCd) {
		this.categoryCd = categoryCd;
	}

	/**
	 * @return the status
	 */
	public BusinessStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(BusinessStatus status) {
		this.status = status;
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_flg", isReadOnly=true)
	public int getStatusCode() {
		return status == null ? 0 : status.getStatus();
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		for (BusinessStatus businessStatus : BusinessStatus.values()) {
			if (businessStatus.getStatus() == statusCode) {
				this.status = businessStatus;
			}
		}
	}

	/**
	 * @return the latitude
	 */
	@Override
	@Column(name="latitude_no")
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @return the longitude
	 */
	@Override
	@Column(name="longitude_no")
	public Double getLongitude() {
		return longitude;
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
}
