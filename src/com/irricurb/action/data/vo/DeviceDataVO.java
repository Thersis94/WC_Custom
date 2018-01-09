package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs 3.3
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DeviceDataVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data received from the irricurb controller containing 
 * readings for a single sensor
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 4, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_project_device_data")
public class DeviceDataVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2721119078788487707L;
	
	// Member Variables
	private String projectDeviceDataId;
	private String projectDeviceId;
	private Date readingDate;
	private Date createDate;
	
	// Sub-beans
	List<DeviceEntityDataVO> readings = new ArrayList<>(8);
	
	/**
	 * 
	 */
	public DeviceDataVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DeviceDataVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DeviceDataVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the projectDeviceDataId
	 */
	@Column(name="project_device_data_id", isPrimaryKey=true)
	public String getProjectDeviceDataId() {
		return projectDeviceDataId;
	}

	/**
	 * @return the projectDeviceId
	 */
	@Column(name="project_device_id")
	public String getProjectDeviceId() {
		return projectDeviceId;
	}

	/**
	 * @return the readingDate
	 */
	@Column(name="reading_dt")
	public Date getReadingDate() {
		return readingDate;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the readings
	 */
	public List<DeviceEntityDataVO> getReadings() {
		return readings;
	}

	/**
	 * @param projectDeviceDataId the projectDeviceDataId to set
	 */
	public void setProjectDeviceDataId(String projectDeviceDataId) {
		this.projectDeviceDataId = projectDeviceDataId;
	}

	/**
	 * @param projectDeviceId the projectDeviceId to set
	 */
	public void setProjectDeviceId(String projectDeviceId) {
		this.projectDeviceId = projectDeviceId;
	}

	/**
	 * @param readingDate the readingDate to set
	 */
	public void setReadingDate(Date readingDate) {
		this.readingDate = readingDate;
	}

	/**
	 * @param readings the readings to set
	 */
	public void setReadings(List<DeviceEntityDataVO> readings) {
		this.readings = readings;
	}

	/**
	 * 
	 * @param reading
	 */
	@BeanSubElement
	public void addReading(DeviceEntityDataVO reading) {
		readings.add(reading);
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
}
