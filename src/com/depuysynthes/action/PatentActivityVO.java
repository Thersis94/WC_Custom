package com.depuysynthes.action;

// Java 8
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.db.DBUtil;

/*****************************************************************************
 <b>Title: </b>PatentActivityVO.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Apr 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentActivityVO extends PatentVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2924274442117682384L;

	private int activityId;
	private int activityTypeId;
	private String activityTypeName;
	private Date activityDate;
	
	/**
	* Constructor
	*/
	public PatentActivityVO() {
		super();
	}

	/**
	* Constructor
	 * @throws SQLException 
	*/
	public PatentActivityVO(ResultSet rs) {
		super(rs);
		setData(rs);
	}
	
	/**
	 * Helper method for setting fields from a result set.
	 * @param rs
	 * @throws SQLException
	 */
	private void setData(ResultSet rs) {
		DBUtil dbUtil = new DBUtil();
		activityId = dbUtil.getIntVal("activity_id", rs);
		activityTypeId = dbUtil.getIntVal("activity_type_id", rs);
		activityDate = dbUtil.getDateVal("activity_dt", rs);
		updateById = dbUtil.getStringVal("profile_id", rs);
	}

	/**
	 * @return the activityId
	 */
	public int getActivityId() {
		return activityId;
	}

	/**
	 * @param activityId the activityId to set
	 */
	public void setActivityId(int activityId) {
		this.activityId = activityId;
	}

	/**
	 * @return the activityTypeId
	 */
	public int getActivityTypeId() {
		return activityTypeId;
	}

	/**
	 * @param activityTypeId the activityTypeId to set
	 */
	public void setActivityTypeId(int activityTypeId) {
		this.activityTypeId = activityTypeId;
	}

	/**
	 * @return the activityTypeName
	 */
	public String getActivityTypeName() {
		return activityTypeName;
	}

	/**
	 * @param activityTypeName the activityTypeName to set
	 */
	public void setActivityTypeName(String activityTypeName) {
		this.activityTypeName = activityTypeName;
	}

	/**
	 * @return the activityDate
	 */
	public Date getActivityDate() {
		return activityDate;
	}

	/**
	 * @param activityDate the activityDate to set
	 */
	public void setActivityDate(Date activityDate) {
		this.activityDate = activityDate;
	}

}
