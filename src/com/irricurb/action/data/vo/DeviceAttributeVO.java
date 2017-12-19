package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>DeviceAttribueVO.java<br/>
 * <b>Description: </b>Data bean to store attribute information for a given device<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 19, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_device_attribute")
public class DeviceAttributeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6267492561261479254L;
	
	// Member Variables
	private String deviceAttributeId;
	private String parentId;
	private String deviceAttributeTypeCode;
	private String name;
	private String apiFieldName;
	private String deviceFieldName;
	private Date createDate;
	private Date updateDate;
	
	// Helper Classes
	private DeviceAttributeTypeVO attributeType;
	private List<DeviceAttributeVO> options = new ArrayList<>(64);
	
	/**
	 * 
	 */
	public DeviceAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DeviceAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DeviceAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the deviceAttributeId
	 */
	@Column(name="device_attribute_id", isPrimaryKey=true, isAutoGen=true)
	public String getDeviceAttributeId() {
		return deviceAttributeId;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the deviceAttributeTypeCode
	 */
	@Column(name="device_attribute_type_cd")
	public String getDeviceAttributeTypeCode() {
		return deviceAttributeTypeCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="attribute_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the apiFieldName
	 */
	@Column(name="api_field_nm")
	public String getApiFieldName() {
		return apiFieldName;
	}

	/**
	 * @return the deviceFieldName
	 */
	@Column(name="device_field_nm")
	public String getDeviceFieldName() {
		return deviceFieldName;
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
	 * @return the attributeType
	 */
	public DeviceAttributeTypeVO getAttributeType() {
		return attributeType;
	}

	/**
	 * @return the options
	 */
	public List<DeviceAttributeVO> getOptions() {
		return options;
	}

	/**
	 * @param deviceAttributeId the deviceAttributeId to set
	 */
	public void setDeviceAttributeId(String deviceAttributeId) {
		this.deviceAttributeId = deviceAttributeId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param deviceAttributeTypeCode the deviceAttributeTypeCode to set
	 */
	public void setDeviceAttributeTypeCode(String deviceAttributeTypeCode) {
		this.deviceAttributeTypeCode = deviceAttributeTypeCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param apiFieldName the apiFieldName to set
	 */
	public void setApiFieldName(String apiFieldName) {
		this.apiFieldName = apiFieldName;
	}

	/**
	 * @param deviceFieldName the deviceFieldName to set
	 */
	public void setDeviceFieldName(String deviceFieldName) {
		this.deviceFieldName = deviceFieldName;
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
	 * @param attributeType the attributeType to set
	 */
	public void setAttributeType(DeviceAttributeTypeVO attributeType) {
		this.attributeType = attributeType;
	}

	/**
	 * @param options the options to set
	 */
	public void setOptions(List<DeviceAttributeVO> options) {
		this.options = options;
	}

	/**
	 * 
	 * @param option
	 */
	public void addOption(DeviceAttributeVO option) {
		options.add(option);
	}
}
