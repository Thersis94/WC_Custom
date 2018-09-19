package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProviderLocationAttribute.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object to hold extended data for the provider
 * location
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider_location_attribute")
public class ProviderLocationAttribute extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3630670500479205159L;
	
	// Member Variables
	private String locationAttributeId;
	private String locationId;
	private String attributeName;
	private String value;
	private Date createDate;
	private Date updateDate;

	/**
	 * 
	 */
	public ProviderLocationAttribute() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderLocationAttribute(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProviderLocationAttribute(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the locationAttributeId
	 */
	@Column(name="locn_attribute_id", isPrimaryKey=true)
	public String getLocationAttributeId() {
		return locationAttributeId;
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id")
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @return the attributeName
	 */
	@Column(name="attribute_nm")
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
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
	 * @param locationAttributeId the locationAttributeId to set
	 */
	public void setLocationAttributeId(String locationAttributeId) {
		this.locationAttributeId = locationAttributeId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
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

