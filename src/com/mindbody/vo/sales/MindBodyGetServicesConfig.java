package com.mindbody.vo.sales;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetServicesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Config for GetServices Endpoint.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetServicesConfig extends MindBodySalesConfig {

	private List<Integer> programIds;
	private List<Integer> sessionTypeIds;
	private List<String> serviceIds;
	private Integer classId;
	private Integer classScheduleId;
	private boolean sellOnline;
	private Integer locationId;
	private boolean hideRelatedPrograms;
	private Long staffId;
	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetServicesConfig(MindBodyCredentialVO source) {
		super(SaleDocumentType.GET_SERVICES, source, null);
		programIds = new ArrayList<>();
		sessionTypeIds = new ArrayList<>();
		serviceIds = new ArrayList<>();
	}
	/**
	 * @return the programIds
	 */
	public List<Integer> getProgramIds() {
		return programIds;
	}
	/**
	 * @return the sessionTypeIds
	 */
	public List<Integer> getSessionTypeIds() {
		return sessionTypeIds;
	}
	/**
	 * @return the serviceIds
	 */
	public List<String> getServiceIds() {
		return serviceIds;
	}
	/**
	 * @return the classId
	 */
	public Integer getClassId() {
		return classId;
	}
	/**
	 * @return the classScheduleId
	 */
	public Integer getClassScheduleId() {
		return classScheduleId;
	}
	/**
	 * @return the sellOnline
	 */
	public boolean isSellOnline() {
		return sellOnline;
	}
	/**
	 * @return the locationId
	 */
	public Integer getLocationId() {
		return locationId;
	}
	/**
	 * @return the hideRelatedPrograms
	 */
	public boolean isHideRelatedPrograms() {
		return hideRelatedPrograms;
	}
	/**
	 * @return the staffId
	 */
	public Long getStaffId() {
		return staffId;
	}
	/**
	 * @param programIds the programIds to set.
	 */
	public void setProgramIds(List<Integer> programIds) {
		this.programIds = programIds;
	}

	public void addProgramId(Integer pId) {
		programIds.add(pId);
	}
	/**
	 * @param sessionTypeIds the sessionTypeIds to set.
	 */
	public void setSessionTypeIds(List<Integer> sessionTypeIds) {
		this.sessionTypeIds = sessionTypeIds;
	}
	public void addSessionTypeId(Integer stid) {
		sessionTypeIds.add(stid);
	}
	/**
	 * @param serviceIds the serviceIds to set.
	 */
	public void setServiceIds(List<String> serviceIds) {
		this.serviceIds = serviceIds;
	}
	public void addServiceId(String sid) {
		serviceIds.add(sid);
	}
	/**
	 * @param classId the classId to set.
	 */
	public void setClassId(Integer classId) {
		this.classId = classId;
	}

	/**
	 * @param classScheduleId the classScheduleId to set.
	 */
	public void setClassScheduleId(Integer classScheduleId) {
		this.classScheduleId = classScheduleId;
	}

	/**
	 * @param sellOnline the sellOnline to set.
	 */
	public void setSellOnline(boolean sellOnline) {
		this.sellOnline = sellOnline;
	}
	/**
	 * @param locationId the locationId to set.
	 */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}
	/**
	 * @param hideRelatedPrograms the hideRelatedPrograms to set.
	 */
	public void setHideRelatedPrograms(boolean hideRelatedPrograms) {
		this.hideRelatedPrograms = hideRelatedPrograms;
	}
	/**
	 * @param staffId the staffId to set.
	 */
	public void setStaffId(Long staffId) {
		this.staffId = staffId;
	}
}