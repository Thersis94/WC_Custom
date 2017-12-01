package com.perkville.vo;

import java.util.Date;

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
	private int connection_id;
	private Date external_cancel_dt;
	private Date external_join_dt;
	private String external_membership_status;
	private String external_membership_type;
	private String home_location;
	private Date last_mod_dt;
	private Date last_transaction_dt;
	private String last_visited_location;
	private String level;
	private int lifetime_earned_points;
	private int point_balance;
	private String resource_uri;
	private String referral_offer_url;
	private String status;
	private String user;
	private Object [] vouchers;

	public ConnectionVO() {
		
	}

	/**
	 * @return the business
	 */
	public String getBusiness() {
		return business;
	}
	/**
	 * @return the connection_id
	 */
	public int getConnection_id() {
		return connection_id;
	}
	/**
	 * @return the external_cancel_dt
	 */
	public Date getExternal_cancel_dt() {
		return external_cancel_dt;
	}
	/**
	 * @return the external_join_dt
	 */
	public Date getExternal_join_dt() {
		return external_join_dt;
	}
	/**
	 * @return the external_membership_status
	 */
	public String getExternal_membership_status() {
		return external_membership_status;
	}
	/**
	 * @return the external_membership_type
	 */
	public String getExternal_membership_type() {
		return external_membership_type;
	}
	/**
	 * @return the home_location
	 */
	public String getHome_location() {
		return home_location;
	}
	/**
	 * @return the last_mod_dt
	 */
	public Date getLast_mod_dt() {
		return last_mod_dt;
	}
	/**
	 * @return the last_transaction_dt
	 */
	public Date getLast_transaction_dt() {
		return last_transaction_dt;
	}
	/**
	 * @return the last_visited_location
	 */
	public String getLast_visited_location() {
		return last_visited_location;
	}
	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}
	/**
	 * @return the lifetime_earned_points
	 */
	public int getLifetime_earned_points() {
		return lifetime_earned_points;
	}
	/**
	 * @return the point_balance
	 */
	public int getPoint_balance() {
		return point_balance;
	}
	/**
	 * @return the resource_uri
	 */
	public String getResource_uri() {
		return resource_uri;
	}
	/**
	 * @return the referral_offer_url
	 */
	public String getReferral_offer_url() {
		return referral_offer_url;
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
	 * @param connection_id the connection_id to set.
	 */
	public void setConnection_id(int connection_id) {
		this.connection_id = connection_id;
	}
	/**
	 * @param external_cancel_dt the external_cancel_dt to set.
	 */
	public void setExternal_cancel_dt(Date external_cancel_dt) {
		this.external_cancel_dt = external_cancel_dt;
	}
	/**
	 * @param external_join_dt the external_join_dt to set.
	 */
	public void setExternal_join_dt(Date external_join_dt) {
		this.external_join_dt = external_join_dt;
	}
	/**
	 * @param external_membership_status the external_membership_status to set.
	 */
	public void setExternal_membership_status(String external_membership_status) {
		this.external_membership_status = external_membership_status;
	}
	/**
	 * @param external_membership_type the external_membership_type to set.
	 */
	public void setExternal_membership_type(String external_membership_type) {
		this.external_membership_type = external_membership_type;
	}
	/**
	 * @param home_location the home_location to set.
	 */
	public void setHome_location(String home_location) {
		this.home_location = home_location;
	}
	/**
	 * @param last_mod_dt the last_mod_dt to set.
	 */
	public void setLast_mod_dt(Date last_mod_dt) {
		this.last_mod_dt = last_mod_dt;
	}
	/**
	 * @param last_transaction_dt the last_transaction_dt to set.
	 */
	public void setLast_transaction_dt(Date last_transaction_dt) {
		this.last_transaction_dt = last_transaction_dt;
	}
	/**
	 * @param last_visited_location the last_visited_location to set.
	 */
	public void setLast_visited_location(String last_visited_location) {
		this.last_visited_location = last_visited_location;
	}
	/**
	 * @param level the level to set.
	 */
	public void setLevel(String level) {
		this.level = level;
	}
	/**
	 * @param lifetime_earned_points the lifetime_earned_points to set.
	 */
	public void setLifetime_earned_points(int lifetime_earned_points) {
		this.lifetime_earned_points = lifetime_earned_points;
	}
	/**
	 * @param point_balance the point_balance to set.
	 */
	public void setPoint_balance(int point_balance) {
		this.point_balance = point_balance;
	}
	/**
	 * @param resource_uri the resource_uri to set.
	 */
	public void setResource_uri(String resource_uri) {
		this.resource_uri = resource_uri;
	}
	/**
	 * @param referral_offer_url the referral_offer_url to set.
	 */
	public void setReferral_offer_url(String referral_offer_url) {
		this.referral_offer_url = referral_offer_url;
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