package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ConnectionReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This value object is used to store one connection when reading data
 * due connections complexity readind from the data base uses connection report vo and saving a connectgion
 * should us connection VO.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 27, 2018
 * @updates:
 ****************************************************************************/

@Table(name="REZDOX_CONNECTION")
public class ConnectionReportVO extends BeanDataVO implements Serializable {

	private static final long serialVersionUID = 2287098865714793552L;

	private String connectionId;
	private String sndrId;
	private String rcptId;
	private String directionCode;
	private String firstName;
	private String lastName;
	private String sortableName;
	private String initials;
	private String profilePicPath;
	private String cityName;
	private String stateCode;
	private String businessSummary;
	private double rating;
	private Date createDate;
	private String categoryCode;
	private String subCategoryCode;
	private int approvedFlag;
	private int privacyFlag;
	private String type;
	
	
	
	public ConnectionReportVO() {
		super();
	}
	/**
	 * @param req
	 */
	public ConnectionReportVO(ActionRequest req) {
		this();
		populateData(req);
	}
	
	/**
	 * @return the connectionId
	 */
	@Column(name="connection_id", isPrimaryKey=true)
	public String getConnectionId() {
		return connectionId;
	}
	/**
	 * @return the sndrId
	 */
	@Column(name="sndr_id")
	public String getSndrId() {
		return sndrId;
	}
	/**
	 * @return the rcptId
	 */
	@Column(name="rcpt_id")
	public String getRcptId() {
		return rcptId;
	}
	/**
	 * @return the directionCode
	 */
	@Column(name="direction_cd")
	public String getDirectionCode() {
		return directionCode;
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
	 * @return the sortableName
	 */
	@Column(name="sortable_nm")
	public String getSortableName() {
		return sortableName;
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
	 * @return the approvedFlag
	 */
	@Column(name="approved_flg")
	public int getApprovedFlag() {
		return approvedFlag;
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
	 * @param approvedFlag the approvedFlag to set
	 */
	public void setApprovedFlag(int approvedFlag) {
		this.approvedFlag = approvedFlag;
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
	}
	/**
	 * @param cityName the cityName to set
	 */
	public void setCityName(String cityName) {
		this.cityName = cityName;
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
	 * Sets user's initials based on first/last name
	 * There could only be one name in the case of a business
	 */
	private void setInitials() {
		setInitials(StringUtil.abbreviate(getSortableName(), 2).toUpperCase());
	}
	/**
	 * @param sortableName the sortableName to set
	 */
	public void setSortableName(String sortableName) {
		this.sortableName = sortableName;
		setInitials();
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @param directionCode the directionCode to set
	 */
	public void setDirectionCode(String directionCode) {
		this.directionCode = directionCode;
	}
	/**
	 * @param rcpt_id the rcpt_id to set
	 */
	public void setRcptId(String rcptId) {
		this.rcptId = rcptId;
	}
	/**
	 * @param sndr_id the sndr_id to set
	 */
	public void setSndrId(String sndrId) {
		this.sndrId = sndrId;
	}
	/**
	 * @param connectionId the connectionId to set
	 */
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	@Column(name="sub_category_cd", isReadOnly=true)
	public String getSubCategoryCode() {
		return subCategoryCode;
	}
	public void setSubCategoryCode(String subCategoryCode) {
		this.subCategoryCode = subCategoryCode;
	}
	@Column(name="type", isReadOnly=true)
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

}
