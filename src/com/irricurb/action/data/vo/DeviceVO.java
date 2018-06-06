package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>DeviceVO.java<br/>
 * <b>Description: </b>Data Bean to store Device Information<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_device")
public class DeviceVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5386150859874560253L;

	// Member Variables
	private String deviceId;
	private String manufacturerId;
	private String deviceTypeCode;
	private String name;
	private String modelNumber;
	private String icon;
	private String networkTypeCode;
	private String deviceClassName;
	private Date createDate;
	private Date updateDate;
	
	// Other classes
	private DeviceTypeVO type;
	private DeviceManufacturerVO manufacturer;
	private NetworkTypeVO network;
	
	/**
	 * 
	 */
	public DeviceVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DeviceVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DeviceVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the deviceId
	 */
	@Column(name="device_id", isPrimaryKey=true)
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * @return the name
	 */
	@Column(name="device_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the modelNumber
	 */
	@Column(name="model_number_txt")
	public String getModelNumber() {
		return modelNumber;
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
	 * @return the manufacturerId
	 */
	@Column(name="device_manufacturer_id")
	public String getManufacturerId() {
		return manufacturerId;
	}

	/**
	 * @return the deviceTypeCode
	 */
	@Column(name="device_type_cd")
	public String getDeviceTypeCode() {
		return deviceTypeCode;
	}
	
	/**
	 * @return the icon
	 */
	@Column(name="icon_info_txt")
	public String getIcon() {
		return icon;
	}
	
	/**
	 * @return the type
	 */
	@BeanSubElement
	public DeviceTypeVO getType() {
		return type;
	}

	/**
	 * @return the manufacturer
	 */
	@BeanSubElement
	public DeviceManufacturerVO getManufacturer() {
		return manufacturer;
	}

	/**
	 * @return the networkTypeCode
	 */
	@Column(name="network_type_cd")
	public String getNetworkTypeCode() {
		return networkTypeCode;
	}

	/**
	 * @return the deviceClassName
	 */
	@Column(name="class_nm")
	public String getDeviceClassName() {
		return deviceClassName;
	}

	/**
	 * @param deviceClassName the deviceClassName to set
	 */
	public void setDeviceClassName(String deviceClassName) {
		this.deviceClassName = deviceClassName;
	}

	/**
	 * @param network the network to set
	 */
	@BeanSubElement
	public void setNetwork(NetworkTypeVO network) {
		this.network = network;
	}
	
	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param modelNumber the modelNumber to set
	 */
	public void setModelNumber(String modelNumber) {
		this.modelNumber = modelNumber;
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
	 * @param type the type to set
	 */
	public void setType(DeviceTypeVO type) {
		this.type = type;
	}

	/**
	 * @param manufacturer the manufacturer to set
	 */
	public void setManufacturer(DeviceManufacturerVO manufacturer) {
		this.manufacturer = manufacturer;
	}

	/**
	 * @param manufacturerId the manufacturerId to set
	 */
	public void setManufacturerId(String manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	/**
	 * @param deviceTypeCode the deviceTypeCode to set
	 */
	public void setDeviceTypeCode(String deviceTypeCode) {
		this.deviceTypeCode = deviceTypeCode;
	}

	/**
	 * @param icon the icon to set
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * @param networkTypeCode the networkTypeCode to set
	 */
	public void setNetworkTypeCode(String networkTypeCode) {
		this.networkTypeCode = networkTypeCode;
	}

	/**
	 * @return the network
	 */
	public NetworkTypeVO getNetwork() {
		return network;
	}
}
