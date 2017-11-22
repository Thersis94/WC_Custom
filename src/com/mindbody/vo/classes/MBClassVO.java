package com.mindbody.vo.classes;

import java.util.Date;

/****************************************************************************
 * <b>Title:</b> MBClassVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Mindbody Class Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MBClassVO {

	private int classScheduleId;
	private int maxCapacity;
	private int webCapacity;
	private int totalBooked;
	private int totalBookedWaitlist;
	private int webBooked;
	private int semesterId;
	private int id;
	private boolean isCancelled;
	private boolean hasSubstitute;
	private boolean isActive;
	private boolean isWaitlistAvailable;
	private boolean isEnrolled;
	private boolean hideCancelled;
	private boolean isAvailable;
	private Date startDateTime;
	private Date endDateTime;
	private Date lastModifiedDateTime;
	private MBClassDescriptionVO classDescription;
	private MBStaffVO staff;
	private MBLocationVO location;

	public MBClassVO() {
		//Default Constructor
	}

	/**
	 * @return the classScheduleId
	 */
	public int getClassScheduleId() {
		return classScheduleId;
	}

	/**
	 * @return the maxCapacity
	 */
	public int getMaxCapacity() {
		return maxCapacity;
	}

	/**
	 * @return the webCapacity
	 */
	public int getWebCapacity() {
		return webCapacity;
	}

	/**
	 * @return the totalBooked
	 */
	public int getTotalBooked() {
		return totalBooked;
	}

	/**
	 * @return the totalBookedWaitlist
	 */
	public int getTotalBookedWaitlist() {
		return totalBookedWaitlist;
	}

	/**
	 * @return the webBooked
	 */
	public int getWebBooked() {
		return webBooked;
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
	 * @return the isCancelled
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * @return the hasSubstitute
	 */
	public boolean isHasSubstitute() {
		return hasSubstitute;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @return the isWaitlistActive
	 */
	public boolean isWaitlistAvailable() {
		return isWaitlistAvailable;
	}

	/**
	 * @return the isEnrolled
	 */
	public boolean isEnrolled() {
		return isEnrolled;
	}

	/**
	 * @return the hideCancelled
	 */
	public boolean isHideCancelled() {
		return hideCancelled;
	}

	/**
	 * @return the isAvailable
	 */
	public boolean isAvailable() {
		return isAvailable;
	}

	/**
	 * @return the startDateTime
	 */
	public Date getStartDateTime() {
		return startDateTime;
	}

	/**
	 * @return the endDateTime
	 */
	public Date getEndDateTime() {
		return endDateTime;
	}

	/**
	 * @return the lastModifiedDateTime
	 */
	public Date getLastModifiedDateTime() {
		return lastModifiedDateTime;
	}

	/**
	 * @return the classDescription
	 */
	public MBClassDescriptionVO getClassDescription() {
		return classDescription;
	}

	/**
	 * @return the staff
	 */
	public MBStaffVO getStaff() {
		return staff;
	}

	/**
	 * @return the location
	 */
	public MBLocationVO getLocation() {
		return location;
	}

	/**
	 * @param classScheduleId the classScheduleId to set.
	 */
	public void setClassScheduleId(int classScheduleId) {
		this.classScheduleId = classScheduleId;
	}

	/**
	 * @param maxCapacity the maxCapacity to set.
	 */
	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	/**
	 * @param webCapacity the webCapacity to set.
	 */
	public void setWebCapacity(int webCapacity) {
		this.webCapacity = webCapacity;
	}

	/**
	 * @param totalBooked the totalBooked to set.
	 */
	public void setTotalBooked(int totalBooked) {
		this.totalBooked = totalBooked;
	}

	/**
	 * @param totalBookedWaitlist the totalBookedWaitlist to set.
	 */
	public void setTotalBookedWaitlist(int totalBookedWaitlist) {
		this.totalBookedWaitlist = totalBookedWaitlist;
	}

	/**
	 * @param webBooked the webBooked to set.
	 */
	public void setWebBooked(int webBooked) {
		this.webBooked = webBooked;
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
	 * @param isCancelled the isCancelled to set.
	 */
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	/**
	 * @param hasSubstitute the hasSubstitute to set.
	 */
	public void setHasSubstitute(boolean hasSubstitute) {
		this.hasSubstitute = hasSubstitute;
	}

	/**
	 * @param isActive the isActive to set.
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * @param isWaitlistActive the isWaitlistActive to set.
	 */
	public void setWaitlistAvailable(boolean isWaitlistAvailable) {
		this.isWaitlistAvailable = isWaitlistAvailable;
	}

	/**
	 * @param isEnrolled the isEnrolled to set.
	 */
	public void setEnrolled(boolean isEnrolled) {
		this.isEnrolled = isEnrolled;
	}

	/**
	 * @param hideCancelled the hideCancelled to set.
	 */
	public void setHideCancelled(boolean hideCancelled) {
		this.hideCancelled = hideCancelled;
	}

	/**
	 * @param isAvailable the isAvailable to set.
	 */
	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	/**
	 * @param startDateTime the startDateTime to set.
	 */
	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}

	/**
	 * @param endDateTime the endDateTime to set.
	 */
	public void setEndDateTime(Date endDateTime) {
		this.endDateTime = endDateTime;
	}

	/**
	 * @param lastModifiedDateTime the lastModifiedDateTime to set.
	 */
	public void setLastModifiedDateTime(Date lastModifiedDateTime) {
		this.lastModifiedDateTime = lastModifiedDateTime;
	}

	/**
	 * @param classDescription the classDescription to set.
	 */
	public void setClassDescription(MBClassDescriptionVO classDescription) {
		this.classDescription = classDescription;
	}

	/**
	 * @param staff the staff to set.
	 */
	public void setStaff(MBStaffVO staff) {
		this.staff = staff;
	}

	/**
	 * @param location the location to set.
	 */
	public void setLocation(MBLocationVO location) {
		this.location = location;
	}
}