package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
<p><b>Title</b>: BusinessAttributeVO.java</p>
<p><b>Description: </b>Value object that encapsulates a RezDox business attribute.</p>
<p> 
<p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Tim Johnson
@version 1.0
@since Mar 8, 2018
<b>Changes:</b> 
***************************************************************************/

@Table(name="REZDOX_BUSINESS_ATTRIBUTE")
public class BusinessAttributeVO {

	private String attributeId;
	private String businessId;
	private String slugText;
	private String valueText;
	private Date createDate;
	private Date updateDate;

	public BusinessAttributeVO() {
		super();
	}
	
	/**
	 * Constructor to quickly populate the bean
	 * 
	 * @param businessId
	 * @param slugText
	 * @param valueText
	 */
	public BusinessAttributeVO(String businessId, String slugText, String valueText) {
		this.businessId = businessId;
		this.slugText = slugText;
		this.valueText = valueText;
	}

	/**
	 * @return the attributeId
	 */
	@Column(name="attribute_id", isPrimaryKey=true)
	public String getAttributeId() {
		return attributeId;
	}

	/**
	 * @param attributeId the attributeId to set
	 */
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

	/**
	 * @return the businessId
	 */
	@Column(name="business_id")
	public String getBusinessId() {
		return businessId;
	}

	/**
	 * @param businessId the businessId to set
	 */
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	/**
	 * @return the slugText
	 */
	@Column(name="slug_txt")
	public String getSlugText() {
		return slugText;
	}

	/**
	 * @param slugText the slugText to set
	 */
	public void setSlugText(String slugText) {
		this.slugText = slugText;
	}

	/**
	 * @return the valueText
	 */
	@Column(name="value_txt")
	public String getValueText() {
		return valueText;
	}

	/**
	 * @param valueText the valueText to set
	 */
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}