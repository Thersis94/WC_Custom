package com.ansmed.sb.util;

import java.util.Date;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: EmailCampaignReportVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2011 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Apr 15, 2011
 Last Updated:
 ***************************************************************************/

public class EmailCampaignReportVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String instanceId = null;
	private String instanceName = null;
	private String profileId = null;
	private Date sendDate = null;
	private Date responseDate = null;
	private String responseType = null;
	private UserDataVO user = new UserDataVO();

	/**
	 * 
	 */
	public EmailCampaignReportVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public EmailCampaignReportVO(ResultSet rs) {
		setData(rs);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		instanceId = db.getStringVal("campaign_instance_id", rs);
		instanceName = db.getStringVal("instance_nm", rs);
		profileId = db.getStringVal("profile_id", rs);
		sendDate = db.getDateVal("send_dt", rs);
		responseDate = db.getDateVal("response_dt", rs);
		responseType = db.getStringVal("response_type_id", rs);
		db = null;
	}
	
	/**
	 * @return the instanceId
	 */
	public String getInstanceId() {
		return instanceId;
	}

	/**
	 * @param instanceId the instanceId to set
	 */
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	/**
	 * @return the instanceName
	 */
	public String getInstanceName() {
		return instanceName;
	}

	/**
	 * @param instanceName the instanceName to set
	 */
	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
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
	 * @return the sendDate
	 */
	public Date getSendDate() {
		return sendDate;
	}

	/**
	 * @param sendDate the sendDate to set
	 */
	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}

	/**
	 * @return the responseDate
	 */
	public Date getResponseDate() {
		return responseDate;
	}

	/**
	 * @param responseDate the responseDate to set
	 */
	public void setResponseDate(Date responseDate) {
		this.responseDate = responseDate;
	}

	public String getResponseType() {
		return responseType;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public UserDataVO getUser() {
		return user;
	}

	public void setUser(UserDataVO user) {
		this.user = user;
	}
	
}
