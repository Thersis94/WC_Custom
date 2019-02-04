package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: WeatherDeviceVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the weather devices
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_weather_device")
public class WeatherDeviceVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1078133238271341507L;

	// Members
	private String weatherDeviceId;
	private String name;
	private String oem;
	private String serialNumber;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public WeatherDeviceVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WeatherDeviceVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WeatherDeviceVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the weather_device_id
	 */
	@Column(name="weather_device_id", isPrimaryKey=true)
	public String getWeatherDeviceId() {
		return weatherDeviceId;
	}

	/**
	 * @return the name
	 */
	@Column(name="device_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the oem
	 */
	@Column(name="oem_nm")
	public String getOem() {
		return oem;
	}

	/**
	 * @return the serialNumber
	 */
	@Column(name="serial_number_txt")
	public String getSerialNumber() {
		return serialNumber;
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
	 * @param weather_device_id the weather_device_id to set
	 */
	public void setWeatherDeviceId(String weatherDeviceId) {
		this.weatherDeviceId = weatherDeviceId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param oem the oem to set
	 */
	public void setOem(String oem) {
		this.oem = oem;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
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
}

