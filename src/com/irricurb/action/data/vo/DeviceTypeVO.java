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
@Table(name="ic_device_type")
public class DeviceTypeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4435778339032923964L;

	// Member Variables 
	private String deviceTypeCode;
	private String deviceName;
	private Date createDate;
	
	/**
	 * 
	 */
	public DeviceTypeVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public DeviceTypeVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public DeviceTypeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the deviceTypeCode
	 */
	@Column(name="device_type_cd", isPrimaryKey=true )
	public String getDeviceTypeCode() {
		return deviceTypeCode;
	}

	/**
	 * @return the deviceName
	 */
	@Column(name="type_nm")
	public String getDeviceName() {
		return deviceName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param deviceTypeCode the deviceTypeCode to set
	 */
	public void setDeviceTypeCode(String deviceTypeCode) {
		this.deviceTypeCode = deviceTypeCode;
	}

	/**
	 * @param deviceName the deviceName to set
	 */
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
