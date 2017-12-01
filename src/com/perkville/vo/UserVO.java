package com.perkville.vo;

import java.util.Date;

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

	private int user_id;
	private String first_name;
	private Date last_mod_dt;
	private String last_name;
	private String phone_number;
	private Date birthday;
	private Object [] emails;
	private Object [] connections;

	/**
	 * @return the user_id
	 */
	public int getUser_id() {
		return user_id;
	}
	/**
	 * @return the first_name
	 */
	public String getFirst_name() {
		return first_name;
	}
	/**
	 * @return the last_mod_dt
	 */
	public Date getLast_mod_dt() {
		return last_mod_dt;
	}
	/**
	 * @return the last_name
	 */
	public String getLast_name() {
		return last_name;
	}
	/**
	 * @return the phone_number
	 */
	public String getPhone_number() {
		return phone_number;
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
	 * @param user_id the user_id to set.
	 */
	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}
	/**
	 * @param first_name the first_name to set.
	 */
	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}
	/**
	 * @param last_mod_dt the last_mod_dt to set.
	 */
	public void setLast_mod_dt(Date last_mod_dt) {
		this.last_mod_dt = last_mod_dt;
	}
	/**
	 * @param last_name the last_name to set.
	 */
	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}
	/**
	 * @param phone_number the phone_number to set.
	 */
	public void setPhone_number(String phone_number) {
		this.phone_number = phone_number;
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