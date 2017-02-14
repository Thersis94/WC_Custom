package com.biomed.smarttrak.vo;

// Java 7
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
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
@Table(name="BIOMEDGPS_USER")
public class UserVO extends UserDataVO {

	private static final long serialVersionUID = -8619730513300299951L;
	private String accountId;
	private String userId;
	private String registerSubmittalId;
	private List<TeamVO> teams;
	private Date createDate;
	private Date updateDate;
	
	public UserVO() {
		teams = new ArrayList<>();
	}

	public UserVO(SMTServletRequest req) {
		super(req);
		setAccountId(req.getParameter("accountId"));
		setUserId(req.getParameter("userId"));
		setRegisterSubmittalId(req.getParameter("registerSubmittalId"));
		teams = new ArrayList<>();
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
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
	@Column(name="account_id")
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
	@Column(name="register_submittal_id")
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
	 * Helper method for adding a team to the List of teams
	 * @param team
	 */
	public void addTeam(TeamVO team) {
		teams.add(team);
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
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
	@Column(name="update_dt", isUpdateOnly=true)
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