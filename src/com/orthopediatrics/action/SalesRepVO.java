package com.orthopediatrics.action;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
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

	private static final long serialVersionUID = -9214824029248624596L;
	private String firstName = null;
	private String lastName = null;
	private String emailAddress = null;
	private String roleId = null;
	private String profileRoleId = null;
	private String roleName = null;
	private String loginId = null;
	private String classId = null;
	private String phoneNumber = null;
	private String password = null;
	private String profileId = null;
	private Map<String, String> regions = new LinkedHashMap<String, String>();
	private Map<String, String> territories = new LinkedHashMap<String, String>();
	private String regionId = null;
	private String territoryId = null;
	
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
	public SalesRepVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Stores the data from a servlet request into the appropriate variable values
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		actionId = req.getParameter("salesRepId");
		profileId = req.getParameter("profileId");
		firstName = se.decodeValue(req.getParameter("firstName"));
		lastName = se.decodeValue(req.getParameter("lastName"));
		emailAddress = se.decodeValue(req.getParameter("emailAddress"));
		roleId = req.getParameter("roleId");
		loginId = req.getParameter("loginId");
		classId = req.getParameter("classId");
		phoneNumber = req.getParameter("phoneNumber");
		profileRoleId = req.getParameter("profileRoleId");
		password = req.getParameter("password");
		regionId = req.getParameter("regionId");
		territoryId = req.getParameter("territoryId");
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
		roleId = db.getStringVal("role_id", rs);
		roleName = db.getStringVal("role_nm", rs);
		loginId = db.getStringVal("op_login_id", rs);
		classId = db.getStringVal("class_id", rs);
		phoneNumber = db.getStringVal("phone_number_txt", rs);
		profileRoleId = db.getStringVal("profile_role_id", rs);
		regionId = db.getStringVal("region_id", rs);
		territoryId = db.getStringVal("territory_id", rs);
		
		// Add the region
		this.addRegion(db.getStringVal("region_id", rs), db.getStringVal("region_nm", rs));
		// Add the territory
		this.addTerritory(db.getStringVal("territory_id", rs), db.getStringVal("territory_nm", rs));
	}
	
	/**
	 * Adds a region to the collection
	 * @param key - Region ID
	 * @param value - Region Name
	 */
	public void addRegion(String key, String value) {
		regions.put(key, value);
	}
	
	/**
	 * Adds a territory to the collection
	 * @param key - territory ID
	 * @param value - territory Name
	 */
	public void addTerritory(String key, String value) {
		territories.put(key, value);
	}	
	
	/**
	 * Reurns an SB user data object form the rep info
	 * @return
	 */
	public UserDataVO getUserData() {
		UserDataVO user = new UserDataVO();
		user.setProfileId(profileId);
		user.setEmailAddress(emailAddress);
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
	 * @return the regions
	 */
	public Map<String, String> getRegions() {
		return regions;
	}

	/**
	 * @param regions the regions to set
	 */
	public void setRegions(Map<String, String> regions) {
		this.regions = regions;
	}

	/**
	 * @return the territories
	 */
	public Map<String, String> getTerritories() {
		return territories;
	}

	/**
	 * @param territories the territories to set
	 */
	public void setTerritories(Map<String, String> territories) {
		this.territories = territories;
	}

	/**
	 * @return the profileRoleId
	 */
	public String getProfileRoleId() {
		return profileRoleId;
	}

	/**
	 * @param profileRoleId the profileRoleId to set
	 */
	public void setProfileRoleId(String profileRoleId) {
		this.profileRoleId = profileRoleId;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the classId
	 */
	public String getClassId() {
		return classId;
	}

	/**
	 * @param classId the classId to set
	 */
	public void setClassId(String classId) {
		this.classId = classId;
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
	 * @return the territoryId
	 */
	public String getTerritoryId() {
		return territoryId;
	}

	/**
	 * @param territoryId the territoryId to set
	 */
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

}
