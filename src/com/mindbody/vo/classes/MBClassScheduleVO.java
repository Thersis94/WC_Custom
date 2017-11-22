package com.mindbody.vo.classes;

import java.util.Date;

/****************************************************************************
 * <b>Title:</b> MBClassScheduleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mindbody ClassSchedule Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 21, 2017
 ****************************************************************************/
public class MBClassScheduleVO {

	private int semesterId;
	private int id;
	private boolean daySunday;
	private boolean dayMonday;
	private boolean dayTuesday;
	private boolean dayWednesday;
	private boolean dayThursday;
	private boolean dayFriday;
	private boolean daySaturday;
	private Date startTime;
	private Date endTime;
	private Date startDate;
	private Date endDate;

	private MBLocationVO location;
	private MBStaffVO staff;
	private MBClassDescriptionVO classDescription;

	public MBClassScheduleVO() {
		//Default Constructor
	}

	/**
	 * @return the semesterId
	 */
	public int getSemesterId() {
		return semesterId;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the daySunday
	 */
	public boolean isDaySunday() {
		return daySunday;
	}

	/**
	 * @return the dayMonday
	 */
	public boolean isDayMonday() {
		return dayMonday;
	}

	/**
	 * @return the dayTuesday
	 */
	public boolean isDayTuesday() {
		return dayTuesday;
	}

	/**
	 * @return the dayWednesday
	 */
	public boolean isDayWednesday() {
		return dayWednesday;
	}

	/**
	 * @return the dayThursday
	 */
	public boolean isDayThursday() {
		return dayThursday;
	}

	/**
	 * @return the dayFriday
	 */
	public boolean isDayFriday() {
		return dayFriday;
	}

	/**
	 * @return the daySaturday
	 */
	public boolean isDaySaturday() {
		return daySaturday;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @return the location
	 */
	public MBLocationVO getLocation() {
		return location;
	}

	/**
	 * @return the staff
	 */
	public MBStaffVO getStaff() {
		return staff;
	}

	/**
	 * @return the classDescription
	 */
	public MBClassDescriptionVO getClassDescription() {
		return classDescription;
	}

	/**
	 * @param semesterId the semesterId to set.
	 */
	public void setSemesterId(int semesterId) {
		this.semesterId = semesterId;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param daySunday the daySunday to set.
	 */
	public void setDaySunday(boolean daySunday) {
		this.daySunday = daySunday;
	}

	/**
	 * @param dayMonday the dayMonday to set.
	 */
	public void setDayMonday(boolean dayMonday) {
		this.dayMonday = dayMonday;
	}

	/**
	 * @param dayTuesday the dayTuesday to set.
	 */
	public void setDayTuesday(boolean dayTuesday) {
		this.dayTuesday = dayTuesday;
	}

	/**
	 * @param dayWednesday the dayWednesday to set.
	 */
	public void setDayWednesday(boolean dayWednesday) {
		this.dayWednesday = dayWednesday;
	}

	/**
	 * @param dayThursday the dayThursday to set.
	 */
	public void setDayThursday(boolean dayThursday) {
		this.dayThursday = dayThursday;
	}

	/**
	 * @param dayFriday the dayFriday to set.
	 */
	public void setDayFriday(boolean dayFriday) {
		this.dayFriday = dayFriday;
	}

	/**
	 * @param daySaturday the daySaturday to set.
	 */
	public void setDaySaturday(boolean daySaturday) {
		this.daySaturday = daySaturday;
	}

	/**
	 * @param startTime the startTime to set.
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * @param endTime the endTime to set.
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * @param startDate the startDate to set.
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate the endDate to set.
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @param location the location to set.
	 */
	public void setLocation(MBLocationVO location) {
		this.location = location;
	}

	/**
	 * @param staff the staff to set.
	 */
	public void setStaff(MBStaffVO staff) {
		this.staff = staff;
	}

	/**
	 * @param classDescription the classDescription to set.
	 */
	public void setClassDescription(MBClassDescriptionVO classDescription) {
		this.classDescription = classDescription;
	}
}