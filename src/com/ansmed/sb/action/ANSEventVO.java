package com.ansmed.sb.action;

// JDK 1.6.0
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;

/*****************************************************************************
<p><b>Title</b>: ANSEventVO.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author James Camire
@version 1.0
@since Mar 30, 2009
Last Updated:
 ***************************************************************************/
public class ANSEventVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Member variables
	private Integer statusId = null;
	private Integer waitList = Integer.valueOf(0);
	private String surgeonName = null;
	private String surgeonId = null;
	private String salesRepName = null;
	private String salesRepId = null;
	private String eventId = null;
	private String eventName = null;
	private String profileId = null;
	private String eventSignupId = null;
	private Date eventDate = null;

	/**
	 * 
	 */
	public ANSEventVO() {
		super();
	}

	/**
	 * 
	 * @param rs
	 */
	public ANSEventVO(ResultSet rs) {
		this();
		this.setData(rs);
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		statusId = db.getIntegerVal("event_status_id", rs);
		waitList = db.getIntegerVal("wait_list_no", rs);
		surgeonName = db.getStringVal("first_nm", rs) + " " + db.getStringVal("last_nm", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		profileId = db.getStringVal("profile_id", rs);
		salesRepName = db.getStringVal("rep_nm", rs);
		salesRepId = db.getStringVal("sales_rep_id", rs);
		eventId = db.getStringVal("event_entry_id", rs);
		eventName = db.getStringVal("event_nm", rs);
		eventDate = db.getDateVal("start_dt", rs);
		eventSignupId = db.getStringVal("event_signup_id", rs);
	}

	/**
	 * @return the statusId
	 */
	public Integer getStatusId() {
		return statusId;
	}

	/**
	 * @param statusId the statusId to set
	 */
	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}

	/**
	 * @return the surgeonName
	 */
	public String getSurgeonName() {
		return surgeonName;
	}

	/**
	 * @param surgeonName the surgeonName to set
	 */
	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the salesRepName
	 */
	public String getSalesRepName() {
		return salesRepName;
	}

	/**
	 * @param salesRepName the salesRepName to set
	 */
	public void setSalesRepName(String salesRepName) {
		this.salesRepName = salesRepName;
	}

	/**
	 * @return the salesRepId
	 */
	public String getSalesRepId() {
		return salesRepId;
	}

	/**
	 * @param salesRepId the salesRepId to set
	 */
	public void setSalesRepId(String salesRepId) {
		this.salesRepId = salesRepId;
	}

	/**
	 * @return the eventId
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	/**
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * @param eventName the eventName to set
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * @return the eventDate
	 */
	public Date getEventDate() {
		return eventDate;
	}

	/**
	 * @param eventDate the eventDate to set
	 */
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param eventSignupId the eventSignupId to set
	 */
	public void setEventSignupId(String eventSignupId) {
		this.eventSignupId = eventSignupId;
	}

	/**
	 * @return the eventSignupId
	 */
	public String getEventSignupId() {
		return eventSignupId;
	}

	/**
	 * @param waitlist the waitList to set
	 */
	public void setWaitList(Integer waitlist) {
		this.waitList = waitlist;
	}

	/**
	 * @return the waitList
	 */
	public Integer getWaitList() {
		return waitList;
	}

}
