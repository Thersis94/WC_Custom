package com.irricurb.action.data.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProjectDeviceVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean extending the device with project specific information
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 13, 2017
 * @updates:
 ****************************************************************************/
@Table(name="ic_project_device")
public class ProjectDeviceVO extends DeviceVO {

	private static final long serialVersionUID = 6922132132917524118L;

	private String projectDeviceId;
	private String projectZoneId;
	private String deviceId;
	private String projectId;
	private String ipAddressText;
	private String serialNumberText;
	private Double longitudeNumber = 0.0;
	private Double latitudeNumber = 0.0;
	private String statusCode;
	private Date createDate;
	private Date updateDate;
	
	
	/**
	 * Status Code Enum
	 */
	public enum ProjectDeviceStatusCode {
		ON("On"), 
		OFF("Off"),
		PROCESSING("Processing"),
		ACTIVE("Active"), 
		COMPLETE("Complete"),
		ERROR("Error"),
		CLOSED("Closed");
		private String statusName;
		ProjectDeviceStatusCode(String statusName) {
			this.statusName = statusName;
		}
		public String getStatusName() {
			return statusName;
		}
	}

	/**
	 * 
	 */
	public ProjectDeviceVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public ProjectDeviceVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public ProjectDeviceVO(ActionRequest req) {
		super(req);
	}
	
	/**
	 * @return the projectDeviceId
	 */
	@Column(name="project_device_id", isPrimaryKey=true)
	public String getProjectDeviceId() {
		return projectDeviceId;
	}

	/**
	 * @return the projectZoneId
	 */
	@Column(name="project_zone_id")
	public String getProjectZoneId() {
		return projectZoneId;
	}
	/**

	/**
	 * @return the deviceId
	 */
	@Column(name="device_id")
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * @return the projectId
	 */
	@Column(name="project_id")
	public String getProjectId() {
		return projectId;
	}
	
	/**
	 * @return the ipAddressText
	 */
	@Column(name="ip_address_txt")
	public String getIpAddressText() {
		return ipAddressText;
	}
	
	/**
	 * @return the serialNumberText
	 */
	@Column(name="serial_number_txt")
	public String getSerialNumberText() {
		return serialNumberText;
	}

	/**
	 * @return the longitudeNumber
	 */
	@Column(name="longitude_no")
	public Double getLongitudeNumber() {
		return longitudeNumber;
	}

	/**
	 * @return the latitudeNumber
	 */
	@Column(name="latitude_no")
	public Double getLatitudeNumber() {
		return latitudeNumber;
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd")
	public String getStatusCode() {
		return statusCode;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	
	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}
	

	/**
	 * @param projectDeviceId the projectDeviceId to set
	 */
	public void setProjectDeviceId(String projectDeviceId) {
		this.projectDeviceId = projectDeviceId;
	}

	/**
	 * @param projectZoneId the projectZoneId to set
	 */
	public void setProjectZoneId(String projectZoneId) {
		this.projectZoneId = projectZoneId;
	}

	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param ipAddressText the ipAddressText to set
	 */
	public void setIpAddressText(String ipAddressText) {
		this.ipAddressText = ipAddressText;
	}


	/**
	 * @param serialNumberText the serialNumberText to set
	 */
	public void setSerialNumberText(String serialNumberText) {
		this.serialNumberText = serialNumberText;
	}

	/**
	 * @param longitudeNumber the longitudeNumber to set
	 */
	public void setLongitudeNumber(Double longitudeNumber) {
		this.longitudeNumber = longitudeNumber;
	}

	/**
	 * @param latitudeNumber the latitudeNumber to set
	 */
	public void setLatitudeNumber(Double latitudeNumber) {
		this.latitudeNumber = latitudeNumber;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString(){
		return StringUtil.getToString(this);
	}
}
