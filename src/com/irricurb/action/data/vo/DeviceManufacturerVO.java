package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

//SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>DeviceTypeVO.java<br/>
 * <b>Description: </b>Data Bean for the types of devices<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_device_manufacturer")
public class DeviceManufacturerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4435778339032923964L;

	// Member Variables 
	private String deviceManufacturerId;
	private String manufacturerName;
	private Date createDate;
	
	/**
	 * 
	 */
	public DeviceManufacturerVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public DeviceManufacturerVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public DeviceManufacturerVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the deviceManufacturerId
	 */
	@Column(name="device_manufacturer_id", isPrimaryKey=true )
	public String getDeviceManufacturerId() {
		return deviceManufacturerId;
	}

	/**
	 * @return the manufacturerName
	 */
	@Column(name="manufacturer_nm")
	public String getManufacturerName() {
		return manufacturerName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param deviceManufacturerId the deviceTypeCode to set
	 */
	public void setDeviceManufacturerId(String deviceManufacturerId) {
		this.deviceManufacturerId = deviceManufacturerId;
	}

	/**
	 * @param manufacturerName the deviceName to set
	 */
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
