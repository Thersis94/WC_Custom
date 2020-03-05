package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>ProjectZoneVO.java<br/>
 * <b>Description: </b>Data bean for the zone information<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_project_zone")
public class ProjectZoneVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7431300441014339125L;

	// Member Variables
	private String projectZoneId;
	private String projectLocationId;
	private String name;
	private String description;
	private String color;
	private Date createDate;
	private Date updateDate;
	
	// Subelements
	private List<ZoneGeocodeVO> points = new ArrayList<>(24);
	private List<ProjectDeviceVO> devices = new ArrayList<>(128);
	
	/**
	 * 
	 */
	public ProjectZoneVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProjectZoneVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProjectZoneVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the projectZoneId
	 */
	@Column(name="project_zone_id", isPrimaryKey=true)
	public String getProjectZoneId() {
		return projectZoneId;
	}

	/**
	 * @return the projectLocationId
	 */
	@Column(name="project_location_id")
	public String getProjectLocationId() {
		return projectLocationId;
	}

	/**
	 * @return the name
	 */
	@Column(name="zone_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	@Column(name="zone_desc")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the color
	 */
	@Column(name="color_txt")
	public String getColor() {
		return color;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the points
	 */
	public List<ZoneGeocodeVO> getPoints() {
		return points;
	}

	/**
	 * @param projectZoneId the projectZoneId to set
	 */
	public void setProjectZoneId(String projectZoneId) {
		this.projectZoneId = projectZoneId;
	}

	/**
	 * @param projectLocationId the projectLocationId to set
	 */
	public void setProjectLocationId(String projectLocationId) {
		this.projectLocationId = projectLocationId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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

	/**
	 * 
	 * @param point
	 */
	@BeanSubElement
	public void addPoint(ZoneGeocodeVO point) {
		points.add(point);
	}
	
	/**
	 * @param points the points to set
	 */
	public void setPoints(List<ZoneGeocodeVO> points) {
		this.points = points;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(String color) {
		this.color = color;
	}

	/**
	 * @return the devices
	 */
	public List<ProjectDeviceVO> getDevices() {
		return devices;
	}

	/**
	 * @param devices the devices to set
	 */
	public void setDevices(List<ProjectDeviceVO> devices) {
		this.devices = devices;
	}
	
	/**
	 * 
	 * @param device
	 */
	@BeanSubElement
	public void addDevice(ProjectDeviceVO device) {
		devices.add(device);
	}

}
