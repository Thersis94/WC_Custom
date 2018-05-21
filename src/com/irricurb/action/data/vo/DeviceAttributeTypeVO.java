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
 * <b>Title: </b>DeviceAttributeTypeVO.java<br/>
 * <b>Description: </b>Data Bean for the types of attributes<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_device_type")
public class DeviceAttributeTypeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4435778339032923964L;

	// Member Variables 
	private String attributeTypeCode;
	private String attributeName;
	private Date createDate;
	
	/**
	 * 
	 */
	public DeviceAttributeTypeVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public DeviceAttributeTypeVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public DeviceAttributeTypeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the deviceTypeCode
	 */
	@Column(name="device_attribute_type_cd", isPrimaryKey=true )
	public String getAttributeTypeCode() {
		return attributeTypeCode;
	}

	/**
	 * @return the deviceName
	 */
	@Column(name="attribute_type_nm")
	public String getAttributeName() {
		return attributeName;
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
	public void setAttributeTypeCode(String attributeTypeCode) {
		this.attributeTypeCode = attributeTypeCode;
	}

	/**
	 * @param deviceName the deviceName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
