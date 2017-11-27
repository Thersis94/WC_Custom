package com.mindbody.vo.clients;

import java.util.Date;

import com.mindbody.vo.classes.MBLocationVO;
import com.mindbody.vo.classes.MBStaffVO;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title:</b> MBVisitVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Visit Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBVisitVO {

	private int appointmentId;
	private String appointmentStatus;
	private String appointmentGenderPreference;
	private int classId;
	private UserDataVO client;
	private Date endDateTime;
	private long id;
	private Date lastModifiedDateTime;
	private boolean lateCancelled;
	private MBLocationVO location;
	private boolean makeUp;
	private String name;
	private MBServiceVO service;
	private boolean signedIn;
	private MBStaffVO staff;
	private Date startDateTime;
	private boolean webSignup;

	public MBVisitVO() {
		//Default Constructor
	}

	/**
	 * @return the appointmentId
	 */
	public int getAppointmentId() {
		return appointmentId;
	}
	/**
	 * @return the appointmentStatus
	 */
	public String getAppointmentStatus() {
		return appointmentStatus;
	}
	/**
	 * @return the appointmentGenderPreference
	 */
	public String getAppointmentGenderPreference() {
		return appointmentGenderPreference;
	}
	/**
	 * @return the classId
	 */
	public int getClassId() {
		return classId;
	}
	/**
	 * @return the client
	 */
	public UserDataVO getClient() {
		return client;
	}
	/**
	 * @return the endDateTime
	 */
	public Date getEndDateTime() {
		return endDateTime;
	}
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @return the lastModifiedDateTime
	 */
	public Date getLastModifiedDateTime() {
		return lastModifiedDateTime;
	}
	/**
	 * @return the lateCancelled
	 */
	public boolean isLateCancelled() {
		return lateCancelled;
	}
	/**
	 * @return the location
	 */
	public MBLocationVO getLocation() {
		return location;
	}
	/**
	 * @return the makeUp
	 */
	public boolean isMakeUp() {
		return makeUp;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the service
	 */
	public MBServiceVO getService() {
		return service;
	}
	/**
	 * @return the signedIn
	 */
	public boolean isSignedIn() {
		return signedIn;
	}
	/**
	 * @return the staff
	 */
	public MBStaffVO getStaff() {
		return staff;
	}
	/**
	 * @return the startDateTime
	 */
	public Date getStartDateTime() {
		return startDateTime;
	}
	/**
	 * @return the webSignup
	 */
	public boolean isWebSignup() {
		return webSignup;
	}
	/**
	 * @param appointmentId the appointmentId to set.
	 */
	public void setAppointmentId(int appointmentId) {
		this.appointmentId = appointmentId;
	}
	/**
	 * @param appointmentStatus the appointmentStatus to set.
	 */
	public void setAppointmentStatus(String appointmentStatus) {
		this.appointmentStatus = appointmentStatus;
	}
	/**
	 * @param appointmentGenderPreference the appointmentGenderPreference to set.
	 */
	public void setAppointmentGenderPreference(String appointmentGenderPreference) {
		this.appointmentGenderPreference = appointmentGenderPreference;
	}
	/**
	 * @param classId the classId to set.
	 */
	public void setClassId(int classId) {
		this.classId = classId;
	}
	/**
	 * @param client the client to set.
	 */
	public void setClient(UserDataVO client) {
		this.client = client;
	}
	/**
	 * @param endDateTime the endDateTime to set.
	 */
	public void setEndDateTime(Date endDateTime) {
		this.endDateTime = endDateTime;
	}
	/**
	 * @param id the id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @param lastModifiedDateTime the lastModifiedDateTime to set.
	 */
	public void setLastModifiedDateTime(Date lastModifiedDateTime) {
		this.lastModifiedDateTime = lastModifiedDateTime;
	}
	/**
	 * @param lateCancelled the lateCancelled to set.
	 */
	public void setLateCancelled(boolean lateCancelled) {
		this.lateCancelled = lateCancelled;
	}
	/**
	 * @param location the location to set.
	 */
	public void setLocation(MBLocationVO location) {
		this.location = location;
	}
	/**
	 * @param makeUp the makeUp to set.
	 */
	public void setMakeUp(boolean makeUp) {
		this.makeUp = makeUp;
	}
	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param service the service to set.
	 */
	public void setService(MBServiceVO service) {
		this.service = service;
	}
	/**
	 * @param signedIn the signedIn to set.
	 */
	public void setSignedIn(boolean signedIn) {
		this.signedIn = signedIn;
	}
	/**
	 * @param staff the staff to set.
	 */
	public void setStaff(MBStaffVO staff) {
		this.staff = staff;
	}
	/**
	 * @param startDateTime the startDateTime to set.
	 */
	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}
	/**
	 * @param webSignup the webSignup to set.
	 */
	public void setWebSignup(boolean webSignup) {
		this.webSignup = webSignup;
	}	
}