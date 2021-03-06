package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
import com.rezdox.data.BusinessFormProcessor;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
// SMTBaseLibs
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.StringUtil;

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

	public static final String AD_FILE_KEY = "BUSINESS_AD";

	private String businessId;
	private String businessName;
	private PhoneVO mainPhone;
	private PhoneVO altPhone;
	private String emailAddressText;
	private String websiteUrl;
	private String photoUrl;
	private String adFileUrl;
	private Map<String, PhotoVO> carouselFileUrls;
	private int privacyFlag;
	private Map<String, MemberVO> members;
	private Map<String, String> attributes;
	private String subCategoryCd;
	private String categoryCd;
	private String categoryName;
	private String subCategoryName;
	private int totalReviewsNo;
	private double avgRatingNo;
	private BusinessStatus status;
	private String initials;
	private Date createDate;
	private Date updateDate;

	private MemberVO owner; // serialized to JSON and used in the Business Data Tool

	public BusinessVO() {
		super();
		members = new HashMap<>();
		attributes = new HashMap<>();
		carouselFileUrls = new HashMap<>();
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
		setInitials();
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
	 * @return the first photoUrl if available.
	 */
	@Column(name="photo_url", isReadOnly=true)
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
	 * @return the first adFileUrl if available.
	 */
	@Column(name="ad_file_url", isReadOnly=true)
	public String getAdFileUrl() {
		return adFileUrl; 
	}

	/**
	 * @param adFileUrl the adFileUrl to set
	 */
	public void setAdFileUrl(String adFileUrl) {
		this.adFileUrl = adFileUrl;
	}

	public Collection<PhotoVO> getCarouselFileUrls() {
		return carouselFileUrls.values();
	}

	/**
	 * Helper method for loading Photo Lists.
	 * @param p
	 */
	@BeanSubElement
	public void addPhoto(PhotoVO p) {
		if(p != null && AD_FILE_KEY.equals(p.getDescriptionText()) && !carouselFileUrls.containsKey(p.getPhotoId())) {
			carouselFileUrls.put(p.getPhotoId(), p);
		}
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
	 * @return the members
	 */
	public Map<String, MemberVO> getMembers() {
		return members;
	}

	/**
	 * @param members the members to set
	 */
	public void setMembers(Map<String, MemberVO> members) {
		this.members = members;
		setOwner();
	}

	/**
	 * @param member
	 */
	@BeanSubElement
	public void addMember(MemberVO member) {
		this.members.put(member.getMemberId(), member);
		setOwner();
	}

	/**
	 * Presume owner is first member and peel them off the Map if not previously set.  
	 * This is only used in the Data Tool for approving Businesses - its a workaround for Javascript/JSON pain.
	 */
	private void setOwner() {
		if (owner != null || members == null || members.isEmpty()) 
			return;
		owner = new ArrayList<>(members.values()).get(0);
	}

	public MemberVO getOwner() {
		return owner;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(BusinessAttributeVO attribute) {
		this.attributes.put(attribute.getSlugText(), attribute.getValueText());
	}

	/**
	 * Adds a collection of attributes to the map
	 * @param attrs
	 */
	public void addAttributes(List<BusinessAttributeVO> attrs) {
		for (BusinessAttributeVO attr : attrs) addAttribute(attr);
	}

	/**
	 * @return the summaryText
	 */
	public String getSummaryText() {
		return attributes.get(BusinessFormProcessor.SLUG_BUSINESS_SUMMARY);
	}

	/**
	 * @param summaryText the summaryText to set
	 */
	public void setSummaryText(String summaryText) {
		attributes.put(BusinessFormProcessor.SLUG_BUSINESS_SUMMARY, summaryText);
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
	 * @return the categoryName
	 */
	@Column(name="category_nm", isReadOnly=true)
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * @return the totalReviewsNo
	 */
	@Column(name="total_reviews_no", isReadOnly=true)
	public int getTotalReviewsNo() {
		return totalReviewsNo;
	}

	/**
	 * @param totalReviewsNo the totalReviewsNo to set
	 */
	public void setTotalReviewsNo(int totalReviewsNo) {
		this.totalReviewsNo = totalReviewsNo;
	}

	/**
	 * @return the avgRatingNo
	 */
	@Column(name="avg_rating_no", isReadOnly=true)
	public double getAvgRatingNo() {
		return avgRatingNo;
	}

	/**
	 * @param avgRatingNo the avgRatingNo to set
	 */
	public void setAvgRatingNo(double avgRatingNo) {
		this.avgRatingNo = avgRatingNo;
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
				break;
			}
		}
	}

	/**
	 * @return the initials
	 */
	public String getInitials() {
		return initials;
	}

	/**
	 * @param initials the initials to set
	 */
	public void setInitials(String initials) {
		this.initials = initials;
	}

	/**
	 * Sets the businesses initials based on the business name set in the VO
	 */
	private void setInitials() {
		if (getBusinessName() != null) {
			setInitials(StringUtil.abbreviate(getBusinessName().trim(), 2).toUpperCase());
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

	@Column(name="sub_category_nm", isReadOnly=true)
	public String getSubCategoryName() {
		return subCategoryName;
	}

	public void setSubCategoryName(String subCategoryName) {
		this.subCategoryName = subCategoryName;
	}

	public boolean isShared() {
		return BusinessStatus.SHARED == getStatus();
	}
}
