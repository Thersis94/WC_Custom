package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
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
	
	// Member Variables
	private String projectDeviceId;
	private String projectZoneId;
	private String projectId;
	private String ipAddressText;
	private String serialNumberText;
	private Double longitudeNumber = 0.0;
	private Double latitudeNumber = 0.0;
	private String networkGatewayText;
	private String networkAddressText;
	private String statusCode;
	
	// Sub-bean Elements
	private List<ProjectDeviceAttributeVO> attributes = new ArrayList<>(16);
	private ProjectZoneVO zone;
	
	/**
	 * Status Code Enum
	 */
	public enum ProjectDeviceStatusCode {
		ACTIVE("Active"), 
		ERROR("Error"),
		MAINTENANCE("Maintenance");
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
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString(){
		return StringUtil.getToString(this);
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
	 * @return the attributes
	 */
	public List<ProjectDeviceAttributeVO> getAttributes() {
		return attributes;
	}
	
	/**
	 * @return the zone
	 */
	public ProjectZoneVO getZone() {
		return zone;
	}
	
	/**
	 * @return the networkGatewayText
	 */
	@Column(name="network_gateway_txt")
	public String getNetworkGatewayText() {
		return networkGatewayText;
	}

	/**
	 * @return the networkAddressText
	 */
	@Column(name="network_address_txt")
	public String getNetworkAddressText() {
		return networkAddressText;
	}

	/**
	 * @param networkAddressText the networkAddressText to set
	 */
	public void setNetworkAddressText(String networkAddressText) {
		this.networkAddressText = networkAddressText;
	}

	/**
	 * @param networkGatewayText the networkGatewayText to set
	 */
	public void setNetworkGatewayText(String networkGatewayText) {
		this.networkGatewayText = networkGatewayText;
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
	 * @param zone the zone to set
	 */
	@BeanSubElement
	public void setZone(ProjectZoneVO zone) {
		this.zone = zone;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<ProjectDeviceAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * 
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(ProjectDeviceAttributeVO attribute) {
		attributes.add(attribute);
	}
}
