package com.perkville.vo;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/****************************************************************************
 * <b>Title:</b> ConnectionVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Perkville Connection Endpoint Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class ConnectionVO {

	private String business;
	@SerializedName("connection_id")
	private int connectionId;
	@SerializedName("external_cancel_dt")
	private Date externalCancelDt;
	@SerializedName("external_join_dt")
	private Date externalJoinDt;
	@SerializedName("external_membership_status")
	private String externalMembershipStatus;
	@SerializedName("external_membership_type")
	private String externalMembershipType;
	@SerializedName("home_location")
	private String homeLocation;
	@SerializedName("last_mod_dt")
	private Date lastModDt;
	@SerializedName("last_transaction_dt")
	private Date lastTransactionDt;
	@SerializedName("last_visited_location")
	private String lastVisitedLocation;
	private String level;
	@SerializedName("lifetime_earned_points")
	private int lifetimeEarnedPoints;
	@SerializedName("point_balance")
	private int pointBalance;
	@SerializedName("resource_uri")
	private String resourceUri;
	@SerializedName("referral_offer_url")
	private String referralOfferUrl;
	private String status;
	private String user;
	private Object [] vouchers;

	/**
	 * @return the business
	 */
	public String getBusiness() {
		return business;
	}

	/**
	 * @return the connectionId
	 */
	public int getConnectionId() {
		return connectionId;
	}

	/**
	 * @return the externalCancelDt
	 */
	public Date getExternalCancelDt() {
		return externalCancelDt;
	}

	/**
	 * @return the externalJoinDt
	 */
	public Date getExternalJoinDt() {
		return externalJoinDt;
	}

	/**
	 * @return the externalMembershipStatus
	 */
	public String getExternalMembershipStatus() {
		return externalMembershipStatus;
	}

	/**
	 * @return the externalMembershipType
	 */
	public String getExternalMembershipType() {
		return externalMembershipType;
	}

	/**
	 * @return the homeLocation
	 */
	public String getHomeLocation() {
		return homeLocation;
	}

	/**
	 * @return the lastModDt
	 */
	public Date getLastModDt() {
		return lastModDt;
	}

	/**
	 * @return the lastTransactionDt
	 */
	public Date getLastTransactionDt() {
		return lastTransactionDt;
	}

	/**
	 * @return the lastVisitedLocation
	 */
	public String getLastVisitedLocation() {
		return lastVisitedLocation;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @return the lifetimeEarnedPoints
	 */
	public int getLifetimeEarnedPoints() {
		return lifetimeEarnedPoints;
	}

	/**
	 * @return the pointBalance
	 */
	public int getPointBalance() {
		return pointBalance;
	}

	/**
	 * @return the resourceUri
	 */
	public String getResourceUri() {
		return resourceUri;
	}

	/**
	 * @return the referralOfferUrl
	 */
	public String getReferralOfferUrl() {
		return referralOfferUrl;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the vouchers
	 */
	public Object[] getVouchers() {
		return vouchers;
	}

	/**
	 * @param business the business to set.
	 */
	public void setBusiness(String business) {
		this.business = business;
	}

	/**
	 * @param connectionId the connectionId to set.
	 */
	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	/**
	 * @param externalCancelDt the externalCancelDt to set.
	 */
	public void setExternalCancelDt(Date externalCancelDt) {
		this.externalCancelDt = externalCancelDt;
	}

	/**
	 * @param externalJoinDt the externalJoinDt to set.
	 */
	public void setExternalJoinDt(Date externalJoinDt) {
		this.externalJoinDt = externalJoinDt;
	}

	/**
	 * @param externalMembershipStatus the externalMembershipStatus to set.
	 */
	public void setExternalMembershipStatus(String externalMembershipStatus) {
		this.externalMembershipStatus = externalMembershipStatus;
	}

	/**
	 * @param externalMembershipType the externalMembershipType to set.
	 */
	public void setExternalMembershipType(String externalMembershipType) {
		this.externalMembershipType = externalMembershipType;
	}

	/**
	 * @param homeLocation the homeLocation to set.
	 */
	public void setHomeLocation(String homeLocation) {
		this.homeLocation = homeLocation;
	}

	/**
	 * @param lastModDt the lastModDt to set.
	 */
	public void setLastModDt(Date lastModDt) {
		this.lastModDt = lastModDt;
	}

	/**
	 * @param lastTransactionDt the lastTransactionDt to set.
	 */
	public void setLastTransactionDt(Date lastTransactionDt) {
		this.lastTransactionDt = lastTransactionDt;
	}

	/**
	 * @param lastVisitedLocation the lastVisitedLocation to set.
	 */
	public void setLastVisitedLocation(String lastVisitedLocation) {
		this.lastVisitedLocation = lastVisitedLocation;
	}

	/**
	 * @param level the level to set.
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * @param lifetimeEarnedPoints the lifetimeEarnedPoints to set.
	 */
	public void setLifetimeEarnedPoints(int lifetimeEarnedPoints) {
		this.lifetimeEarnedPoints = lifetimeEarnedPoints;
	}

	/**
	 * @param pointBalance the pointBalance to set.
	 */
	public void setPointBalance(int pointBalance) {
		this.pointBalance = pointBalance;
	}

	/**
	 * @param resourceUri the resourceUri to set.
	 */
	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	/**
	 * @param referralOfferUrl the referralOfferUrl to set.
	 */
	public void setReferralOfferUrl(String referralOfferUrl) {
		this.referralOfferUrl = referralOfferUrl;
	}

	/**
	 * @param status the status to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @param user the user to set.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @param vouchers the vouchers to set.
	 */
	public void setVouchers(Object[] vouchers) {
		this.vouchers = vouchers;
	}
}