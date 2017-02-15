package com.biomed.smarttrak.vo;

// Java 7
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

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
public class UserVO extends UserDataVO implements HumanNameIntfc {
	private static final long serialVersionUID = -8619730513300299951L;
	private String accountId;
	private String userId;
	private String registerSubmittalId;
	private String statusCode;
	private List<TeamVO> teams;
	private Date expirationDate;
	private Date loginDate;
	private Date createDate;
	private Date updateDate;

	public UserVO() {
		teams = new ArrayList<>();
	}

	public UserVO(ActionRequest req) {
		super(req);
		setUserId(req.getParameter("userId"));
		setAccountId(req.getParameter("accountId"));
		this.setProfileId(req.getParameter("profileId"));
		setRegisterSubmittalId(req.getParameter("registerSubmittalId"));
		setStatusCode(req.getParameter("statusCode"));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expirationDate")));
		teams = new ArrayList<>();
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id", isPrimaryKey=true)
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

	@Column(name="status_cd")
	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	@Column(name="login_dt", isReadOnly=true)
	public Date getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(Date loginDate) {
		this.loginDate = loginDate;
	}

	/*********************
	 *  SOME OVERRIDES FOR ORM TO WORK PROPERLY WITH SUPERCLASS FIELDS
	 *********************/

	@Column(name="profile_id")
	public String getProfileId() {
		return super.getProfileId();
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return super.getFirstName();
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return super.getLastName();
	}

	@Column(name="email_address_txt", isReadOnly=true)
	public String getEmailAddress() {
		return super.getEmailAddress();
	}
}