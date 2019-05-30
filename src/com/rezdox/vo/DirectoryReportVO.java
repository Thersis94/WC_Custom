package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DirectoryReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds member or business data, including connection 
 * id if they are connected to the logged in member or selected business.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since Apr 11, 2018
 * @updates:
 ****************************************************************************/

@Table(name="REZDOX_CONNECTION")
public class DirectoryReportVO extends BeanDataVO implements Serializable {

	private static final long serialVersionUID = 2287098865714793552L;

	private String userId;
	private String connectionId;
	private String firstName;
	private String lastName;
	private String initials;
	private String profilePicPath;
	private String cityName;
	private String stateCode;
	private String businessSummary;
	private double rating;
	private Date createDate;
	private String categoryCode;
	private String categoryLvl2Code;
	private int privacyFlag;
	private String uniqueId;

	private String myProId;
	private String combinedSearch = "";



	public DirectoryReportVO() {
		super();
	}
	/**
	 * @param req
	 */
	public DirectoryReportVO(ActionRequest req) {
		this();
		populateData(req);
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id", isPrimaryKey=true)
	public String getUserId() {
		return userId;
	}
	/**
	 * @return the connectionId
	 */
	@Column(name="connection_id")
	public String getConnectionId() {
		return connectionId;
	}
	/**
	 * @return the firstName
	 */
	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @return the lastName
	 */
	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
	}
	/**
	 * @return the initials
	 */
	public String getInitials() {
		return initials;
	}
	/**
	 * @return the profilePicPath
	 */
	@Column(name="profile_pic_pth")
	public String getProfilePicPath() {
		return profilePicPath;
	}
	/**
	 * @return the cityName
	 */
	@Column(name="city_nm")
	public String getCityName() {
		return cityName;
	}
	/**
	 * @return the stateName
	 */
	@Column(name="state_cd")
	public String getStateCode() {
		return stateCode;
	}
	/**
	 * @return the businessSummary
	 */
	@Column(name="business_summary")
	public String getBusinessSummary() {
		return businessSummary;
	}
	/**
	 * @return the rating
	 */
	@Column(name="rating")
	public double getRating() {
		return rating;
	}
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	/**
	 * @return the categoryCode
	 */
	@Column(name="category_cd")
	public String getCategoryCode() {
		return categoryCode;
	}
	/**
	 * @return the privacyFlag
	 */
	@Column(name="privacy_flg")
	public int getPrivacyFlag() {
		return privacyFlag;
	}
	/**
	 * @return the uniqueId
	 */
	@Column(name="unique_id")
	public String getUniqueId() {
		return uniqueId;
	}
	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	/**
	 * @param privacyFlag the privacyFlag to set
	 */
	public void setPrivacyFlag(int privacyFlag) {
		this.privacyFlag = privacyFlag;
	}
	/**
	 * @param categoryCode the categoryCode to set
	 */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	/**
	 * @param rating the rating to set
	 */
	public void setRating(double rating) {
		this.rating = rating;
	}
	/**
	 * @param businessSummary the businessSummary to set
	 */
	public void setBusinessSummary(String businessSummary) {
		this.businessSummary = businessSummary;
	}
	/**
	 * @param stateName the stateName to set
	 */
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
		addCombinedSearch(stateCode);
	}
	/**
	 * @param cityName the cityName to set
	 */
	public void setCityName(String cityName) {
		this.cityName = cityName;
		addCombinedSearch(cityName);
	}
	/**
	 * @param profilePicPath the profilePicPath to set
	 */
	public void setProfilePicPath(String profilePicPath) {
		this.profilePicPath = profilePicPath;
	}
	/**
	 * @param initials the initials to set
	 */
	public void setInitials(String initials) {
		this.initials = initials;
	}
	/**
	 * Sets user's initials based on first/last name in the VO
	 * There could only be one name in the case of business categories
	 */
	private void setInitials() {
		String name = StringUtil.checkVal(getFirstName()) + ' ' + StringUtil.checkVal(getLastName());
		setInitials(StringUtil.abbreviate(name.trim(), 2).toUpperCase());
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
		setInitials();
		addCombinedSearch(lastName);
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
		setInitials();
		addCombinedSearch(firstName);
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @param connectionId the connectionId to set
	 */
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	@Column(name="category_lvl2_cd")
	public String getCategoryLvl2Code() {
		return categoryLvl2Code;
	}
	public void setCategoryLvl2Code(String categoryLvl2Code) {
		this.categoryLvl2Code = categoryLvl2Code;
	}

	@Column(name="my_pro_id", isReadOnly=true)
	public String getMyProId() {
		return myProId;
	}
	public void setMyProId(String myProId) {
		this.myProId = myProId;
	}
	public String getCombinedSearch() {
		return combinedSearch;
	}
	private void addCombinedSearch(String combinedSearch) {
		this.combinedSearch += " " + combinedSearch;
	}
}
