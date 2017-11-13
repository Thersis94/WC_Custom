package com.mindbody.vo.staff;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetStaffConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 11, 2017
 ****************************************************************************/
public class MindBodyGetStaffConfig extends MindBodyStaffConfig {

	private List<Integer> staffIds;
	private MindBodyCredentialVO staffCredentials;
	private List<Integer> sessionTypeId;
	private Date startDateTime;
	private Integer locationId;
	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetStaffConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(StaffDocumentType.GET_STAFF, source, user);
		this.staffIds = new ArrayList<>();
		this.sessionTypeId = new ArrayList<>();
	}

	public MindBodyGetStaffConfig(MindBodyCredentialVO source, MindBodyCredentialVO user, boolean addStaffViewableFilter, boolean addAppointmentInstructorFilter, boolean addFemaleFilter, boolean addStaffLocationField) {
		this(source, user);
		if(addStaffViewableFilter) {
			addFilter("StaffViewable");
		}

		if(addAppointmentInstructorFilter) {
			addFilter("AppointmentInstructor");
		}

		if(addFemaleFilter) {
			addFilter("Female");
		}

		if(addStaffLocationField) {
			addField("Staff.Locations");
		}
	}
	/**
	 * @return the staffIds
	 */
	public List<Integer> getStaffIds() {
		return staffIds;
	}
	/**
	 * @return the staffCredentials
	 */
	public MindBodyCredentialVO getStaffCredentials() {
		return staffCredentials;
	}
	/**
	 * @return the sessionTypeId
	 */
	public List<Integer> getSessionTypeId() {
		return sessionTypeId;
	}
	/**
	 * @return the startDateTime
	 */
	public Date getStartDateTime() {
		return startDateTime;
	}
	/**
	 * @return the locationId
	 */
	public Integer getLocationId() {
		return locationId;
	}
	/**
	 * @param staffIds the staffIds to set.
	 */
	public void setStaffIds(List<Integer> staffIds) {
		this.staffIds = staffIds;
	}
	/**
	 * @param staffCredentials the staffCredentials to set.
	 */
	public void setStaffCredentials(MindBodyCredentialVO staffCredentials) {
		this.staffCredentials = staffCredentials;
	}
	/**
	 * @param sessionTypeId the sessionTypeId to set.
	 */
	public void setSessionTypeId(List<Integer> sessionTypeId) {
		this.sessionTypeId = sessionTypeId;
	}
	/**
	 * @param startDateTime the startDateTime to set.
	 */
	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}
	/**
	 * @param locationId the locationId to set.
	 */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}
}