package com.biomed.smarttrak.vo;

// Java 7
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;
// SMTBaseLibs
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: SmarttrakUserVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a Smarttrak user.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserVO extends UserDataVO {

	private static final long serialVersionUID = -8619730513300299951L;
	private String accountId;
	private String userId;
	private String registerSubmittalId;
	private List<TeamVO> teams;
	private Date createDate;
	private Date updateDate;

	/**
	* Constructor
	*/
	public UserVO() {
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public UserVO(SMTServletRequest req) {
		super(req);
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public UserVO(ResultSet rs) {
		super(rs);
		this.setData(rs);
		teams = new ArrayList<>();
	}
	
	/**
	 * 
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setAccountId(db.getStringVal("account_id", rs));
		setUserId(db.getStringVal("user_id", rs));
		setProfileId(db.getStringVal("profile_id", rs));
		setRegisterSubmittalId(db.getStringVal("register_submittal_id", rs));
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
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
	 * @return the registerSubmittalId
	 */
	public String getRegisterSubmittalId() {
		return registerSubmittalId;
	}

	/**
	 * @param registerSubmittalId the registerSubmittalId to set
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
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
	 * Helper method for adding a team to the List of teams
	 * @param team
	 */
	public void addTeam(TeamVO team) {
		teams.add(team);
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
