package com.biomed.smarttrak.vo;

// Java 7
import java.util.Date;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
 <p><b>Title</b>: TeamMemberVO.java</p>
 <p><b>Description: </b>VO that encapsulates a Smarttrak team->user relationship (XR table binding).</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 11, 2017
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="BIOMEDGPS_USER_TEAM_XR")
public class TeamMemberVO implements HumanNameIntfc {
	private String teamId;
	private String userId;
	private String userTeamXrId;
	private String pkId;
	private Date createDate;
	private String firstName;
	private String lastName;

	public TeamMemberVO() {
		super();
	}

	public TeamMemberVO(ActionRequest req) {
		this();
		setTeamId(req.getParameter("teamId"));
		setUserId(req.getParameter("userId"));
		setUserTeamXrId(req.getParameter("userTeamXrId"));
	}

	/**
	 * @return the teamId
	 */
	@Column(name="team_id")
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

	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Column(name="user_team_xr_id", isPrimaryKey=true)
	public String getUserTeamXrId() {
		return userTeamXrId;
	}

	public void setUserTeamXrId(String userTeamXrId) {
		this.userTeamXrId = userTeamXrId;
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * pkid is an overload/override for the primary.  
	 * There's a bug in DBProcessor consolidated beans with null primary keys into a single bean, so TeamAction needed 
	 * to shift the primary key from the _XR table into another field to avoid this consolidation. 
	 * This compliments oddities in formatRetrieveQuery() of TeamMemberAction.  -JM- 02.12.2017
	 * @return
	 */
	@Column(name="pkid", isReadOnly=true)
	public String getPkId() {
		return pkId;
	}

	public void setPkId(String pkId) {
		this.pkId = pkId;
	}
}