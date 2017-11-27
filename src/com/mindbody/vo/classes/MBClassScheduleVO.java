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
	private boolean [] days;
	private Date startTime;
	private Date endTime;
	private Date startDate;
	private Date endDate;

	private MBLocationVO location;
	private MBStaffVO staff;
	private MBClassDescriptionVO classDescription;

	public MBClassScheduleVO() {
		//Default Constructor
		days = new boolean[7];
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
		return days[0];
	}

	/**
	 * @return the dayMonday
	 */
	public boolean isDayMonday() {
		return days[1];
	}

	/**
	 * @return the dayTuesday
	 */
	public boolean isDayTuesday() {
		return days[2];
	}

	/**
	 * @return the dayWednesday
	 */
	public boolean isDayWednesday() {
		return days[3];
	}

	/**
	 * @return the dayThursday
	 */
	public boolean isDayThursday() {
		return days[4];
	}

	/**
	 * @return the dayFriday
	 */
	public boolean isDayFriday() {
		return days[5];
	}

	/**
	 * @return the daySaturday
	 */
	public boolean isDaySaturday() {
		return days[6];
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
		days[0] = daySunday;
	}

	/**
	 * @param dayMonday the dayMonday to set.
	 */
	public void setDayMonday(boolean dayMonday) {
		days[1] = dayMonday;
	}

	/**
	 * @param dayTuesday the dayTuesday to set.
	 */
	public void setDayTuesday(boolean dayTuesday) {
		days[2] = dayTuesday;
	}

	/**
	 * @param dayWednesday the dayWednesday to set.
	 */
	public void setDayWednesday(boolean dayWednesday) {
		days[3] = dayWednesday;
	}

	/**
	 * @param dayThursday the dayThursday to set.
	 */
	public void setDayThursday(boolean dayThursday) {
		days[4] = dayThursday;
	}

	/**
	 * @param dayFriday the dayFriday to set.
	 */
	public void setDayFriday(boolean dayFriday) {
		days[5] = dayFriday;
	}

	/**
	 * @param daySaturday the daySaturday to set.
	 */
	public void setDaySaturday(boolean daySaturday) {
		days[6] = daySaturday;
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

	public boolean [] getDays() {
		return days;
	}

	public void setDays(boolean [] days) {
		this.days = days;
	}
}