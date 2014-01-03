package com.venture.cs.action;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 *<b>Title</b>: OwnerVO<p/>
 * Stores the information related to the owner of a vehicle <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class OwnerVO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String ownerId;
	private String firstName;
	private String lastName;
	private String address;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String phone;
	private String email;
	private Date purchaseDate;

	public OwnerVO() {
		
	}
	
	public OwnerVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setOwnerId(db.getStringVal("VENTURE_OWNER_ID", rs));
		this.setFirstName(db.getStringVal("FIRST_NM", rs));
		this.setLastName(db.getStringVal("LAST_NM", rs));
		this.setAddress(db.getStringVal("ADDRESS_TXT", rs));
		this.setAddress2(db.getStringVal("ADDRESS2_TXT", rs));
		this.setCity(db.getStringVal("CITY_NM", rs));
		this.setState(db.getStringVal("STATE_CD", rs));
		this.setZip(db.getStringVal("ZIP_CD", rs));
		this.setPhone(db.getStringVal("PRIMARY_PHONE_NO", rs));
		this.setEmail(db.getStringVal("EMAIL_ADDRESS_TXT", rs));
		this.setPurchaseDate(db.getDateVal("PURCHASE_DT", rs));
	}
	
	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getName() {
		return firstName  + " " + lastName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address1) {
		this.address = address1;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(Date date) {
		this.purchaseDate = date;
	}
	
	
}
