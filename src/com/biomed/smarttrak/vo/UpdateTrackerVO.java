package com.biomed.smarttrak.vo;
/****************************************************************************
 * Title: UpdateTrackerVO.java <p/>
 * Project: WC_Custom <p/>
 * Description: POJO used as a helper class to manage tracking updates throughout
 * related hierarchy levels for determining placement within update section collection <p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Jun 27, 2017
 ****************************************************************************/
public class UpdateTrackerVO {
	private String updateId;
	private int trackingCount;
	private boolean isUniqueLevel;
	
	/**
	 */
	public UpdateTrackerVO(){
		//no argument-constructor 
	}
	
	/**
	 * Constructor for initialization
	 * @param updateId
	 * @param trackingCount
	 * @param hasUniqueLevel
	 */
	public UpdateTrackerVO(String updateId, int trackingCount, boolean hasUniqueLevel){
		setUpdateId(updateId);
		setTrackingCount(trackingCount);
		setHasUniqueLevel(hasUniqueLevel);
	}
	
	/**
	 * @return the updateId
	 */
	public String getUpdateId() {
		return updateId;
	}
	/**
	 * @param updateId the updateId to set
	 */
	public void setUpdateId(String updateId) {
		this.updateId = updateId;
	}
	/**
	 * @return the trackingCount
	 */
	public int getTrackingCount() {
		return trackingCount;
	}
	/**
	 * @param trackingCount the trackingCount to set
	 */
	public void setTrackingCount(int trackingCount) {
		this.trackingCount = trackingCount;
	}
	/**
	 * @return the hasUniqueLevel
	 */
	public boolean isUniqueLevel() {
		return isUniqueLevel;
	}
	/**
	 * @param hasUniqueLevel the hasUniqueLevel to set
	 */
	public void setHasUniqueLevel(boolean hasUniqueLevel) {
		this.isUniqueLevel = hasUniqueLevel;
	}
}
