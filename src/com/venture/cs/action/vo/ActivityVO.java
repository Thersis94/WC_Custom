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
	private String firstName;
	private String lastName;
	private String submitterId; // typically, the profileId of the person performing the activity
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
		this.setFirstName(db.getStringVal("FIRST_NM", rs));
		this.setLastName(db.getStringVal("LAST_NM", rs));
		this.setComment(db.getStringVal("COMMENT", rs));
		this.setCreateDate(db.getDateVal("CREATE_DT", rs));
		
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the submitterId
	 */
	public String getSubmitterId() {
		return submitterId;
	}

	/**
	 * @param submitterId the submitterId to set
	 */
	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
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
