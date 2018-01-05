package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.3
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DeviceEntityDataVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean storing values for a single attribute in a single device
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 4, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_data_entity")
public class DeviceEntityDataVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2084898041601438230L;
	
	// Member Variables
	private String dataEntityId;
	private String projectDeviceDataId;
	private String deviceAttributeId;
	private double readingValue;
	private Date createDate;

	/**
	 * 
	 */
	public DeviceEntityDataVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DeviceEntityDataVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DeviceEntityDataVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the dataEntityId
	 */
	@Column(name="data_entity_id", isPrimaryKey=true, isAutoGen=true)
	public String getDataEntityId() {
		return dataEntityId;
	}

	/**
	 * @return the projectDeviceEntityId
	 */
	@Column(name="project_device_data_id")
	public String getProjectDeviceDataId() {
		return projectDeviceDataId;
	}

	/**
	 * @return the deviceAttributeId
	 */
	@Column(name="device_attribute_id")
	public String getDeviceAttributeId() {
		return deviceAttributeId;
	}

	/**
	 * @return the readingValue
	 */
	@Column(name="reading_value_no")
	public double getReadingValue() {
		return readingValue;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param dataEntityId the dataEntityId to set
	 */
	public void setDataEntityId(String dataEntityId) {
		this.dataEntityId = dataEntityId;
	}

	/**
	 * @param projectDeviceEntityId the projectDeviceEntityId to set
	 */
	public void setProjectDeviceDataId(String projectDeviceDataId) {
		this.projectDeviceDataId = projectDeviceDataId;
	}

	/**
	 * @param deviceAttributeId the deviceAttributeId to set
	 */
	public void setDeviceAttributeId(String deviceAttributeId) {
		this.deviceAttributeId = deviceAttributeId;
	}

	/**
	 * @param readingValue the readingValue to set
	 */
	public void setReadingValue(double readingValue) {
		this.readingValue = readingValue;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
