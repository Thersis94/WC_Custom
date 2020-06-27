package com.mts.scheduler.job;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: CatVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO that holds the different types of SMT article categories
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since Jun 25, 2020
 * @updates:
 ****************************************************************************/
@Table(name="widget_meta_data")
public class MTSArticleCategoryVO {
	

	
	private String fieldName;
	private String fieldDesc;
	private String dataId;
	
	/**
	 * @return the fieldName
	 */
	@Column(name="field_nm")
	public String getFieldName() {
		return fieldName;
	}
	/**
	 * @param fieldName the fieldName to set
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	/**
	 * @return the fieldDesc
	 */
	@Column(name="field_desc")
	public String getFieldDesc() {
		return fieldDesc;
	}
	/**
	 * @param fieldDesc the fieldDesc to set
	 */
	public void setFieldDesc(String fieldDesc) {
		this.fieldDesc = fieldDesc;
	}
	/**
	 * @return the dataId
	 */
	@Column(name="widget_meta_data_id", isPrimaryKey=true)
	public String getDataId() {
		return dataId;
	}
	/**
	 * @param dataId the dataId to set
	 */
	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
	
}
