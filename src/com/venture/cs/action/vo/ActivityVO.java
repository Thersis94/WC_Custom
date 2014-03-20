package com.venture.cs.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 *<b>Title</b>: TicketVO<p/>
 * Stores the information related to actions taken affecting a vehicle <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class ActivityVO implements Serializable {

	private static final long serialVersionUID = 1L;
	private String activityId;
	private String vehicleId;
	/**
	 * This is the profile ID of the person submitting / performing
	 * this activity.
	 */
	private String submissionId;
	private String firstName;
	private String lastName;
	private String comment;
	private Date createDate;
	
	/**
	 * 
	 */
	public ActivityVO() {}
	
	/**
	 * 
	 * @param rs
	 */
	public ActivityVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setVehicleId(db.getStringVal("VENTURE_VEHICLE_ID", rs));
		this.setSubmissionId(db.getStringVal("PROFILE_ID", rs));
		this.setFirstName(db.getStringVal("FIRST_NM", rs));
		this.setLastName(db.getStringVal("LAST_NM", rs));
		this.setComment(db.getStringVal("COMMENT", rs));
		this.setCreateDate(db.getDateVal("CREATE_DT", rs));
		
	}

	/**
	 * @return the activityId
	 */
	public String getActivityId() {
		return activityId;
	}

	/**
	 * @param activityId the activityId to set
	 */
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	/**
	 * @return the vehicleId
	 */
	public String getVehicleId() {
		return vehicleId;
	}

	/**
	 * @param vehicleId the vehicleId to set
	 */
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the submissionId
	 */
	public String getSubmissionId() {
		return submissionId;
	}

	/**
	 * @param submissionId the submissionId to set
	 */
	public void setSubmissionId(String submissionId) {
		this.submissionId = submissionId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date date) {
		this.createDate = date;
	}
	
}
