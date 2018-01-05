package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>AttributeDevice.java<br/>
 * <b>Description: </b>Data Bean that extends DeviceAttribute.  
 * Defines which attribute is utilized by a given device<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 19, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_attribute_device")
public class AttributeDevice extends DeviceAttributeVO {

	/**
	 * DeviceAttributeVO
	 */
	private static final long serialVersionUID = 5352420977846321729L;
	
	// Member Variables
	private String attributeDeviceId;
	private String deviceId;
	private String displayTypeCode;
	
	// Helper Classed (SubBeans)
	private DeviceAttributeVO deviceAttribute;
	
	/**
	 * 
	 */
	public AttributeDevice() {
		super();
	}

	/**
	 * @param req
	 */
	public AttributeDevice(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public AttributeDevice(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the attributeDeviceId
	 */
	@Column(name="attribute_device_id", isPrimaryKey=true, isAutoGen=true)
	public String getAttributeDeviceId() {
		return attributeDeviceId;
	}

	/**
	 * @return the deviceId
	 */
	@Column(name="device_id")
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * @param attributeDeviceId the attributeDeviceId to set
	 */
	public void setAttributeDeviceId(String attributeDeviceId) {
		this.attributeDeviceId = attributeDeviceId;
	}

	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}


	/**
	 * @return the displayTypeCode
	 */
	@Column(name="display_type_cd")
	public String getDisplayTypeCode() {
		return displayTypeCode;
	}

	/**
	 * @param displayTypeCode the displayTypeCode to set
	 */
	public void setDisplayTypeCode(String displayTypeCode) {
		this.displayTypeCode = displayTypeCode;
	}

}
