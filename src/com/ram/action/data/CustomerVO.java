package com.ram.action.data;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CustomerVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 10, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CustomerVO implements Serializable {
	protected String customerId = null;
	protected String customerName = null;
	protected String locationDesc = null;
	protected String organizationId = null;
	protected String profileId = null;
	protected UserDataVO contact = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public CustomerVO() {
		
	}
	
	/**
	 * 
	 * @param rs
	 */
	public CustomerVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		customerId = db.getStringVal("customer_id", rs);
		customerName = db.getStringVal("customer_nm", rs);
		locationDesc = db.getStringVal("location_desc", rs);
		organizationId = db.getStringVal("organization_id", rs);
		profileId = db.getStringVal("profile_id", rs);
		
		contact = new UserDataVO(rs);
	}
	
	/**
	 * Format the values of the bean and return
	 */
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the customerId
	 */
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @return the customerName
	 */
	public String getCustomerName() {
		return customerName;
	}

	/**
	 * @param customerName the customerName to set
	 */
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	/**
	 * @return the locationDesc
	 */
	public String getLocationDesc() {
		return locationDesc;
	}

	/**
	 * @param locationDesc the locationDesc to set
	 */
	public void setLocationDesc(String locationDesc) {
		this.locationDesc = locationDesc;
	}

	/**
	 * @return the organizationId
	 */
	public String getOrganizationId() {
		return organizationId;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	/**
	 * @return the contact
	 */
	public UserDataVO getContact() {
		return contact;
	}

	/**
	 * @param contact the contact to set
	 */
	public void setContact(UserDataVO contact) {
		this.contact = contact;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * 
	 * @return
	 */
	public boolean hasContactAssigned() {
		if (StringUtil.checkVal(profileId).length() > 0) return true;
		else return false;
	}
}
