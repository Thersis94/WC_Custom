package com.perkville.vo;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/****************************************************************************
 * <b>Title:</b> UserVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Perkville User Endpoint Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class UserVO {

	@SerializedName("user_id")
	private int userId;
	@SerializedName("first_name")
	private String firstName;
	@SerializedName("last_mod_dt")
	private Date lastModDt;
	@SerializedName("last_name")
	private String lastName;
	@SerializedName("phone_number")
	private String phoneNumber;
	@SerializedName("birthday")
	private Date birthday;
	private Object [] emails;
	private Object [] connections;
	/**
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}
	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @return the lastModDt
	 */
	public Date getLastModDt() {
		return lastModDt;
	}
	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}
	/**
	 * @return the birthday
	 */
	public Date getBirthday() {
		return birthday;
	}
	/**
	 * @return the emails
	 */
	public Object[] getEmails() {
		return emails;
	}
	/**
	 * @return the connections
	 */
	public Object[] getConnections() {
		return connections;
	}
	/**
	 * @param userId the userId to set.
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}
	/**
	 * @param firstName the firstName to set.
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @param lastModDt the lastModDt to set.
	 */
	public void setLastModDt(Date lastModDt) {
		this.lastModDt = lastModDt;
	}
	/**
	 * @param lastName the lastName to set.
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @param phoneNumber the phoneNumber to set.
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	/**
	 * @param birthday the birthday to set.
	 */
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	/**
	 * @param emails the emails to set.
	 */
	public void setEmails(Object[] emails) {
		this.emails = emails;
	}
	/**
	 * @param connections the connections to set.
	 */
	public void setConnections(Object[] connections) {
		this.connections = connections;
	}
}