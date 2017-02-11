package com.biomed.smarttrak.vo;

// Java 7
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: TeamVO.java</p>
 <p><b>Description: </b>VO that encapsulates a Smarttrak team.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="BIOMEDGPS_TEAM")
public class TeamVO {
	private String accountId;
	private String teamId;
	private String teamName;
	private int memberCount;
	private int defaultFlg;
	private int privateFlg;
	private Date createDate;
	private Date updateDate;

	/**
	 * Constructor
	 */
	public TeamVO() {
		super();
	}

	public TeamVO(ActionRequest req) {
		this();
		setAccountId(req.getParameter("accountId"));
		setTeamId(req.getParameter("teamId"));
		setTeamName(req.getParameter("teamName"));
		setDefaultFlg(Convert.formatInteger(req.getParameter("defaultFlag")));
		setPrivateFlg(Convert.formatInteger(req.getParameter("privateFlag")));
	}

	/**
	 * @return the teamId
	 */
	@Column(name="team_id", isPrimaryKey=true)
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
	 * @return the teamName
	 */
	@Column(name="team_nm")
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
	 * @return the defaultFlg
	 */
	@Column(name="default_flg")
	public int getDefaultFlg() {
		return defaultFlg;
	}

	/**
	 * @param defaultFlg the defaultFlg to set
	 */
	public void setDefaultFlg(int defaultFlg) {
		this.defaultFlg = defaultFlg;
	}

	/**
	 * @return the privateFlg
	 */
	@Column(name="private_flg")
	public int getPrivateFlg() {
		return privateFlg;
	}

	/**
	 * @param privateFlg the privateFlg to set
	 */
	public void setPrivateFlg(int privateFlg) {
		this.privateFlg = privateFlg;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
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
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	//@Column(name="members", isReadOnly=true)
	public int getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(int memberCount) {
		this.memberCount = memberCount;
	}
}