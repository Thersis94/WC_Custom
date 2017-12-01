package com.perkville.vo;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/****************************************************************************
 * <b>Title:</b> PerkVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Perkville Perk Endpoint Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class PerkVO {

	@SerializedName("admin_flagged_as_invisible_to_customers")
	private boolean adminFlaggedAsInvisibleToCustomers;
	private String business;
	private String classification;
	@SerializedName("completes_referral")
	private boolean completesReferral;
	private String description;
	@SerializedName("eligible_at_all_locations")
	private boolean eligibleAtAllLocations;
	@SerializedName("external_coupon_code")
	private String externalCouponCode;
	@SerializedName("external_reward_url")
	private String externalRewardUrl;
	@SerializedName("fine_print")
	private String finePrint;
	@SerializedName("from_date")
	private Date fromDate;
	@SerializedName("perk_id")
	private int perkId;
	@SerializedName("picture_card")
	private String pictureCard;
	private int points;
	@SerializedName("redemption_limit_count")
	private int redemptionLimitCount;
	@SerializedName("redemption_limit_interval")
	private int redemptionLimitInterval;
	@SerializedName("redemption_limit_interval_count")
	private int redemptionLimitIntervalCount;
	@SerializedName("resource_uri")
	private String resourceUri;
	private String status;
	private String title;
	@SerializedName("to_date")
	private Date toDate;
	private String type;
	/**
	 * @return the adminFlaggedAsInvisibleToCustomers
	 */
	public boolean isAdminFlaggedAsInvisibleToCustomers() {
		return adminFlaggedAsInvisibleToCustomers;
	}
	/**
	 * @return the business
	 */
	public String getBusiness() {
		return business;
	}
	/**
	 * @return the classification
	 */
	public String getClassification() {
		return classification;
	}
	/**
	 * @return the completesReferral
	 */
	public boolean isCompletesReferral() {
		return completesReferral;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @return the eligibleAtAllLocations
	 */
	public boolean isEligibleAtAllLocations() {
		return eligibleAtAllLocations;
	}
	/**
	 * @return the externalCouponCode
	 */
	public String getExternalCouponCode() {
		return externalCouponCode;
	}
	/**
	 * @return the externalRewardUrl
	 */
	public String getExternalRewardUrl() {
		return externalRewardUrl;
	}
	/**
	 * @return the finePrint
	 */
	public String getFinePrint() {
		return finePrint;
	}
	/**
	 * @return the fromDate
	 */
	public Date getFromDate() {
		return fromDate;
	}
	/**
	 * @return the perkId
	 */
	public int getPerkId() {
		return perkId;
	}
	/**
	 * @return the pictureCard
	 */
	public String getPictureCard() {
		return pictureCard;
	}
	/**
	 * @return the points
	 */
	public int getPoints() {
		return points;
	}
	/**
	 * @return the redemptionLimitCount
	 */
	public int getRedemptionLimitCount() {
		return redemptionLimitCount;
	}
	/**
	 * @return the redemptionLimitInterval
	 */
	public int getRedemptionLimitInterval() {
		return redemptionLimitInterval;
	}
	/**
	 * @return the redemptionLimitIntervalCount
	 */
	public int getRedemptionLimitIntervalCount() {
		return redemptionLimitIntervalCount;
	}
	/**
	 * @return the resourceUri
	 */
	public String getResourceUri() {
		return resourceUri;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @return the toDate
	 */
	public Date getToDate() {
		return toDate;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param adminFlaggedAsInvisibleToCustomers the adminFlaggedAsInvisibleToCustomers to set.
	 */
	public void setAdminFlaggedAsInvisibleToCustomers(boolean adminFlaggedAsInvisibleToCustomers) {
		this.adminFlaggedAsInvisibleToCustomers = adminFlaggedAsInvisibleToCustomers;
	}
	/**
	 * @param business the business to set.
	 */
	public void setBusiness(String business) {
		this.business = business;
	}
	/**
	 * @param classification the classification to set.
	 */
	public void setClassification(String classification) {
		this.classification = classification;
	}
	/**
	 * @param completesReferral the completesReferral to set.
	 */
	public void setCompletesReferral(boolean completesReferral) {
		this.completesReferral = completesReferral;
	}
	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @param eligibleAtAllLocations the eligibleAtAllLocations to set.
	 */
	public void setEligibleAtAllLocations(boolean eligibleAtAllLocations) {
		this.eligibleAtAllLocations = eligibleAtAllLocations;
	}
	/**
	 * @param externalCouponCode the externalCouponCode to set.
	 */
	public void setExternalCouponCode(String externalCouponCode) {
		this.externalCouponCode = externalCouponCode;
	}
	/**
	 * @param externalRewardUrl the externalRewardUrl to set.
	 */
	public void setExternalRewardUrl(String externalRewardUrl) {
		this.externalRewardUrl = externalRewardUrl;
	}
	/**
	 * @param finePrint the finePrint to set.
	 */
	public void setFinePrint(String finePrint) {
		this.finePrint = finePrint;
	}
	/**
	 * @param fromDate the fromDate to set.
	 */
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	/**
	 * @param perkId the perkId to set.
	 */
	public void setPerkId(int perkId) {
		this.perkId = perkId;
	}
	/**
	 * @param pictureCard the pictureCard to set.
	 */
	public void setPictureCard(String pictureCard) {
		this.pictureCard = pictureCard;
	}
	/**
	 * @param points the points to set.
	 */
	public void setPoints(int points) {
		this.points = points;
	}
	/**
	 * @param redemptionLimitCount the redemptionLimitCount to set.
	 */
	public void setRedemptionLimitCount(int redemptionLimitCount) {
		this.redemptionLimitCount = redemptionLimitCount;
	}
	/**
	 * @param redemptionLimitInterval the redemptionLimitInterval to set.
	 */
	public void setRedemptionLimitInterval(int redemptionLimitInterval) {
		this.redemptionLimitInterval = redemptionLimitInterval;
	}
	/**
	 * @param redemptionLimitIntervalCount the redemptionLimitIntervalCount to set.
	 */
	public void setRedemptionLimitIntervalCount(int redemptionLimitIntervalCount) {
		this.redemptionLimitIntervalCount = redemptionLimitIntervalCount;
	}
	/**
	 * @param resourceUri the resourceUri to set.
	 */
	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}
	/**
	 * @param status the status to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @param title the title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @param toDate the toDate to set.
	 */
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	/**
	 * @param type the type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}
}