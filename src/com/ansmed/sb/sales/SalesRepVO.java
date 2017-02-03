package com.ansmed.sb.sales;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: SalesRepVO.java</p>
 <p>Description: <b/>Stores data for a sales rep object</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 5, 2007
 Last Updated:
 ***************************************************************************/

public class SalesRepVO extends AbstractSiteBuilderVO {
	public static final long serialVersionUID = 1l;
	private String firstName = null;
	private String lastName = null;
	private String emailAddress = null;
	private String regionId = null;
	private String areaId = null;
	private String areaName = null;
	private String regionName = null;
	private String roleId = null;
	private String roleName = null;
	private String loginId = null;
	private String phoneNumber = null;
	private String profileId = null;
	private String atmRepId = null;
	
	/**
	 * 
	 */
	public SalesRepVO() {
		super();
	}
	
	/**
	 * Initializes the object using a Result Set row element
	 * @param rs
	 */
	public SalesRepVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the object using a browser request element
	 * @param rs
	 */
	public SalesRepVO(ActionRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Stores the data from a servlet request into the appropriate variable values
	 * @param req
	 */
	public void setData(ActionRequest req) {
		StringEncoder se = new StringEncoder();
		actionId = req.getParameter("salesRepId");
		profileId = req.getParameter("profileId");
		firstName = se.decodeValue(req.getParameter("firstName"));
		lastName = se.decodeValue(req.getParameter("lastName"));
		emailAddress = se.decodeValue(req.getParameter("emailAddress"));
		regionId = req.getParameter("regionId");
		areaId = req.getParameter("areaId");
		roleId = req.getParameter("roleId");
		loginId = req.getParameter("loginId");
		phoneNumber = req.getParameter("phoneNumber");
		atmRepId = req.getParameter("atmRepId");
	}
	
	/**
	 * Stores the data form a database row into the appropriate variable values
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		StringEncoder se =  new StringEncoder();
		actionId = db.getStringVal("sales_rep_id", rs);
		profileId = db.getStringVal("profile_id", rs);
		firstName = se.decodeValue(db.getStringVal("first_nm", rs));
		lastName = se.decodeValue(db.getStringVal("last_nm", rs));
		emailAddress = se.decodeValue(db.getStringVal("email_address_txt", rs));
		regionId = db.getStringVal("region_id", rs);
		regionName = db.getStringVal("region_nm", rs);
		areaId = db.getStringVal("area_id", rs);
		areaName = db.getStringVal("area_nm", rs);
		roleId = db.getStringVal("role_id", rs);
		roleName = db.getStringVal("role_nm", rs);
		loginId = db.getStringVal("ans_login_id", rs);
		phoneNumber = db.getStringVal("phone_number_txt", rs);
		atmRepId = db.getStringVal("atm_rep_id", rs);
	}
	
	/**
	 * Reurns an SB user data object form the rep info
	 * @return
	 */
	public UserDataVO getUserData() {
		UserDataVO user = new UserDataVO();
		user.setProfileId(profileId);
		user.setEmailAddress(emailAddress);
		user.setAuthenticationId(loginId);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.addPhone(new PhoneVO(phoneNumber));
		
		return user;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @return the regionId
	 */
	public String getRegionId() {
		return regionId;
	}

	/**
	 * @param regionId the regionId to set
	 */
	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	/**
	 * @return the regionName
	 */
	public String getRegionName() {
		return regionName;
	}

	/**
	 * @param regionName the regionName to set
	 */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	/**
	 * @return the roleId
	 */
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @return the loginId
	 */
	public String getLoginId() {
		return loginId;
	}

	/**
	 * @param loginId the loginId to set
	 */
	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}


	/**
	 * @return the roleName
	 */
	public String getRoleName() {
		return roleName;
	}


	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
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
	 * @return the areaId
	 */
	public String getAreaId() {
		return areaId;
	}

	/**
	 * @param areaId the areaId to set
	 */
	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	/**
	 * @return the areaName
	 */
	public String getAreaName() {
		return areaName;
	}

	/**
	 * @param areaName the areaName to set
	 */
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getAtmRepId() {
		return atmRepId;
	}

	public void setAtmRepId(String atmRepId) {
		this.atmRepId = atmRepId;
	}

}
