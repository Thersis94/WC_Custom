package com.biomed.smarttrak.vo;

// Java 7
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;

/*****************************************************************************
 <p><b>Title</b>: SmarttrakTeamVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a Smarttrak team.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamVO {

	private String accountId;
	private String teamId;
	private String teamName;
	private int defaultFlag;
	private int privateFlag;
	private List<UserVO> members;
	private Date assignedDate; // XR table create_dt value
	private Date createDate;
	private Date updateDate;
	
	/**
	* Constructor
	*/
	public TeamVO() {
		members = new ArrayList<>();
	}
	
	/**
	* Constructor
	 */
	public TeamVO(ResultSet rs) {
		setData(rs);
	}
	
	/**
	 * Helper method to populate bean based on result set
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setAccountId(db.getStringVal("account_id", rs));
		setTeamId(db.getStringVal("team_id", rs));
		setTeamName(db.getStringVal("team_nm", rs));
		setDefaultFlag(db.getIntVal("default_flg", rs));
		setPrivateFlag(db.getIntVal("private_flg", rs));
		setAssignedDate(db.getDateVal("assigned_dt", rs));
		setCreateDate(db.getDateVal("create_dt", rs));
		setUpdateDate(db.getDateVal("update_dt", rs));
	}

	/**
	 * @return the teamId
	 */
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = teamId;
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
	 * @return the teamName
	 */
	public String getTeamName() {
		return teamName;
	}

	/**
	 * @param teamName the teamName to set
	 */
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	/**
	 * @return the defaultFlag
	 */
	public int getDefaultFlag() {
		return defaultFlag;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
	public void setDefaultFlag(int defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * @return
	 */
	public boolean isDefault() {
		if (defaultFlag == 1) return true;
		return false;
	}
	
	/**
	 * @return the privateFlag
	 */
	public int getPrivateFlag() {
		return privateFlag;
	}

	/**
	 * @param privateFlag the privateFlag to set
	 */
	public void setPrivateFlag(int privateFlag) {
		this.privateFlag = privateFlag;
	}

	/**
	 * @return
	 */
	public boolean isPrivate() {
		if (privateFlag == 1) return true;
		return false;
	}

	/**
	 * @return the members
	 */
	public List<UserVO> getMembers() {
		return members;
	}

	/**
	 * @param members the members to set
	 */
	public void setMembers(List<UserVO> members) {
		this.members = members;
	}
	
	/**
	 * Helper method for adding members to a team.
	 * @param member
	 */
	public void addMember(UserVO member) {
		if (members == null) members = new ArrayList<>();
		members.add(member);
	}

	/**
	 * @return the assignedDate
	 */
	public Date getAssignedDate() {
		return assignedDate;
	}

	/**
	 * @param assignedDate the assignedDate to set
	 */
	public void setAssignedDate(Date assignedDate) {
		this.assignedDate = assignedDate;
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
	
}
