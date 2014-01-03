package com.ansmed.sb.report;

import java.util.Date;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: SmartBriefReportVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since May 13, 2009
 Last Updated:
 ***************************************************************************/

public class SmartBriefReportVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String instanceId = null;
	private String profileId = null;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String regionNm = null;
	private Date sendDate = null;
	private Date responseDate = null;


	/**
	 * 
	 */
	public SmartBriefReportVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public SmartBriefReportVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		instanceId = db.getStringVal("campaign_instance_id", rs);
		profileId = db.getStringVal("profile_id", rs);
		repFirstNm = se.decodeValue(db.getStringVal("first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("last_nm", rs));
		regionNm = se.decodeValue(db.getStringVal("region_nm", rs));
		sendDate = db.getDateVal("send_dt", rs);
		responseDate = db.getDateVal("response_dt", rs);

	}
	
	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
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
	 * @return the regionNm
	 */
	public String getRegionNm() {
		return regionNm;
	}

	/**
	 * @param regionNm the regionNm to set
	 */
	public void setRegionNm(String regionNm) {
		this.regionNm = regionNm;
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
	
}
