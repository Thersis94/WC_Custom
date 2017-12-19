package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>ProjectDeviceAttributeVO.java<br/>
 * <b>Description: </b>Stores the value for a given attribute on a given
 * device for a specific location in a zone on a project<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 19, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_device_attribute_xr")
public class ProjectDeviceAttributeVO extends AttributeDevice {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4363487323521817071L;
	
	// Member Variables
	private String deviceAttributeXrId;
	private String projectDeviceId;
	private String value;

	/**
	 * 
	 */
	public ProjectDeviceAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProjectDeviceAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProjectDeviceAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the deviceAttributeXrId
	 */
	@Column(name="device_attribute_xr_id", isPrimaryKey=true, isAutoGen=true)
	public String getDeviceAttributeXrId() {
		return deviceAttributeXrId;
	}

	/**
	 * @return the projectDeviceId
	 */
	@Column(name="project_device_id")
	public String getProjectDeviceId() {
		return projectDeviceId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @param deviceAttributeXrId the deviceAttributeXrId to set
	 */
	public void setDeviceAttributeXrId(String deviceAttributeXrId) {
		this.deviceAttributeXrId = deviceAttributeXrId;
	}

	/**
	 * @param projectDeviceId the projectDeviceId to set
	 */
	public void setProjectDeviceId(String projectDeviceId) {
		this.projectDeviceId = projectDeviceId;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
