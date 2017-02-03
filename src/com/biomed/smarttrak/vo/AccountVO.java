package com.biomed.smarttrak.vo;

//Java 7
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;
// SMTBaseLibs
import com.siliconmtn.gis.Location;

/*****************************************************************************
 <p><b>Title</b>: SmarttrakAccountVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a Smarttrak account.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountVO {

	private String accountId;
	private String accountName;
	private String companyId;
	private String typeId;
	private String ownerProfileId;
	private String ownerName;
	private Location location;
	private String statusNo;
	private Date startDate;
	private Date expirationDate;
	private List<TeamVO> teams;
	private List<UserVO> users;
	private Date createDate;
	private Date updateDate;
	
	/**
	* Constructor
	*/
	public AccountVO() {
		// constructor stub
		teams = new ArrayList<>();
		users = new ArrayList<>();
	}
	
	/**
	* Constructor
	 */
	public AccountVO(ResultSet rs) {
		this();
		setData(rs);
	}

	/**
	 * Helper method for populating bean properties from
	 * a result set.
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setAccountId(db.getStringVal("account_id", rs));
		setAccountName(db.getStringVal("account_nm", rs));
		setCompanyId(db.getStringVal("company_id", rs));
		setTypeId(db.getStringVal("type_id", rs));
		setOwnerProfileId(db.getStringVal("owner_profile_id", rs));
		setStatusNo(db.getStringVal("status_no", rs));
		setStartDate(db.getDateVal("start_dt", rs));
		setExpirationDate(db.getDateVal("expiration_dt", rs));
		setCreateDate(db.getDateVal("create_dt", rs));
		setUpdateDate(db.getDateVal("update_dt", rs));
		Location loc = new Location();
		loc.setAddress(db.getStringVal("address_txt", rs));
		loc.setAddress2(db.getStringVal("address2_txt", rs));
		loc.setCity(db.getStringVal("city_nm", rs));
		loc.setState(db.getStringVal("state_cd", rs));
		loc.setZipCode(db.getStringVal("zip_cd", rs));
		loc.setCountry(db.getStringVal("country_cd", rs));
		setLocation(loc);
	}
	
	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the accountName
	 */
	public String getAccountName() {
		return accountName;
	}

	/**
	 * @param accountName the accountName to set
	 */
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	/**
	 * @return the ownerProfileId
	 */
	public String getOwnerProfileId() {
		return ownerProfileId;
	}

	/**
	 * @param ownerProfileId the ownerProfileId to set
	 */
	public void setOwnerProfileId(String ownerProfileId) {
		this.ownerProfileId = ownerProfileId;
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * @return the statusNo
	 */
	public String getStatusNo() {
		return statusNo;
	}

	/**
	 * @param statusNo the statusNo to set
	 */
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the expirationDate
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @return the teams
	 */
	public List<TeamVO> getTeams() {
		return teams;
	}

	/**
	 * @param teams the teams to set
	 */
	public void setTeams(List<TeamVO> teams) {
		this.teams = teams;
	}
	
	/**
	 * @param team
	 */
	public void addTeam(TeamVO team) {
		if (team != null) teams.add(team);
	}

	/**
	 * @return the users
	 */
	public List<UserVO> getUsers() {
		return users;
	}

	/**
	 * @param users the users to set
	 */
	public void setUsers(List<UserVO> users) {
		this.users = users;
	}
	
	/**
	 * @param user
	 */
	public void addUser(UserVO user) {
		if (user != null) users.add(user);
	}
	
	/**
	 * Helper method to return account owner's name (rep).
	 * @return
	 */
	public String getOwnerName() {
		return ownerName;
	}
	
	public void setOwnerName() {
		if (ownerProfileId ==  null || 
				users == null) return;
		for (UserVO user : users) {
			if (user.getProfileId().equals(ownerProfileId)) {
				ownerName = user.getFirstName();
			}
		}
	}

	
	/**
	 * Helper field for JSTL view.
	 * @param ownerName
	 */
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

}
