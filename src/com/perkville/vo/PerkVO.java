package com.perkville.vo;

import java.util.Date;

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

	private boolean admin_flagged_as_invisible_to_customers;
	private String business;
	private String classification;
	private boolean completes_referral;
	private String description;
	private boolean eligible_at_all_locations;
	private String external_coupon_code;
	private String external_reward_url;
	private String fine_print;
	private Date from_date;
	private int perk_id;
	private String picture_card;
	private int points;
	private int redemption_limit_count;
	private int redemption_limit_interval;
	private int redemption_limit_interval_count;
	private String resource_uri;
	private String status;
	private String title;
	private Date to_date;
	private String type;

	/**
	 * @return the admin_flagged_as_invisible_to_customers
	 */
	public boolean isAdmin_flagged_as_invisible_to_customers() {
		return admin_flagged_as_invisible_to_customers;
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
	 * @return the completes_referral
	 */
	public boolean isCompletes_referral() {
		return completes_referral;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @return the eligible_at_all_locations
	 */
	public boolean isEligible_at_all_locations() {
		return eligible_at_all_locations;
	}
	/**
	 * @return the external_coupon_code
	 */
	public String getExternal_coupon_code() {
		return external_coupon_code;
	}
	/**
	 * @return the external_reward_url
	 */
	public String getExternal_reward_url() {
		return external_reward_url;
	}
	/**
	 * @return the fine_print
	 */
	public String getFine_print() {
		return fine_print;
	}
	/**
	 * @return the from_date
	 */
	public Date getFrom_date() {
		return from_date;
	}
	/**
	 * @return the perk_id
	 */
	public int getPerk_id() {
		return perk_id;
	}
	/**
	 * @return the picture_card
	 */
	public String getPicture_card() {
		return picture_card;
	}
	/**
	 * @return the points
	 */
	public int getPoints() {
		return points;
	}
	/**
	 * @return the redemption_limit_count
	 */
	public int getRedemption_limit_count() {
		return redemption_limit_count;
	}
	/**
	 * @return the redemption_limit_interval
	 */
	public int getRedemption_limit_interval() {
		return redemption_limit_interval;
	}
	/**
	 * @return the redemption_limit_interval_count
	 */
	public int getRedemption_limit_interval_count() {
		return redemption_limit_interval_count;
	}
	/**
	 * @return the resource_uri
	 */
	public String getResource_uri() {
		return resource_uri;
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
	 * @return the to_date
	 */
	public Date getTo_date() {
		return to_date;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param admin_flagged_as_invisible_to_customers the admin_flagged_as_invisible_to_customers to set.
	 */
	public void setAdmin_flagged_as_invisible_to_customers(boolean admin_flagged_as_invisible_to_customers) {
		this.admin_flagged_as_invisible_to_customers = admin_flagged_as_invisible_to_customers;
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
	 * @param completes_referral the completes_referral to set.
	 */
	public void setCompletes_referral(boolean completes_referral) {
		this.completes_referral = completes_referral;
	}
	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @param eligible_at_all_locations the eligible_at_all_locations to set.
	 */
	public void setEligible_at_all_locations(boolean eligible_at_all_locations) {
		this.eligible_at_all_locations = eligible_at_all_locations;
	}
	/**
	 * @param external_coupon_code the external_coupon_code to set.
	 */
	public void setExternal_coupon_code(String external_coupon_code) {
		this.external_coupon_code = external_coupon_code;
	}
	/**
	 * @param external_reward_url the external_reward_url to set.
	 */
	public void setExternal_reward_url(String external_reward_url) {
		this.external_reward_url = external_reward_url;
	}
	/**
	 * @param fine_print the fine_print to set.
	 */
	public void setFine_print(String fine_print) {
		this.fine_print = fine_print;
	}
	/**
	 * @param from_date the from_date to set.
	 */
	public void setFrom_date(Date from_date) {
		this.from_date = from_date;
	}
	/**
	 * @param perk_id the perk_id to set.
	 */
	public void setPerk_id(int perk_id) {
		this.perk_id = perk_id;
	}
	/**
	 * @param picture_card the picture_card to set.
	 */
	public void setPicture_card(String picture_card) {
		this.picture_card = picture_card;
	}
	/**
	 * @param points the points to set.
	 */
	public void setPoints(int points) {
		this.points = points;
	}
	/**
	 * @param redemption_limit_count the redemption_limit_count to set.
	 */
	public void setRedemption_limit_count(int redemption_limit_count) {
		this.redemption_limit_count = redemption_limit_count;
	}
	/**
	 * @param redemption_limit_interval the redemption_limit_interval to set.
	 */
	public void setRedemption_limit_interval(int redemption_limit_interval) {
		this.redemption_limit_interval = redemption_limit_interval;
	}
	/**
	 * @param redemption_limit_interval_count the redemption_limit_interval_count to set.
	 */
	public void setRedemption_limit_interval_count(int redemption_limit_interval_count) {
		this.redemption_limit_interval_count = redemption_limit_interval_count;
	}
	/**
	 * @param resource_uri the resource_uri to set.
	 */
	public void setResource_uri(String resource_uri) {
		this.resource_uri = resource_uri;
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
	 * @param to_date the to_date to set.
	 */
	public void setTo_date(Date to_date) {
		this.to_date = to_date;
	}
	/**
	 * @param type the type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}
}