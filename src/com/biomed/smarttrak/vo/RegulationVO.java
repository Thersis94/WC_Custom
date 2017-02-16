/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: RegulationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Manages Regulation Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_REGULATORY_PRODUCT")
public class RegulationVO {

	private String regulatorId;
	private String regionId;
	private String regionName;
	private String pathId;
	private String pathName;
	private String productId;
	private String statusId;
	private String statusName;

	public RegulationVO() {
		super();
	}

	public RegulationVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		regulatorId = db.getStringVal("regulator_id", rs);
		regionId = db.getStringVal("region_id", rs);
		pathId = db.getStringVal("path_id", rs);
		productId = db.getStringVal("product_id", rs);
		statusId = db.getStringVal("status_id", rs);
		regionName = db.getStringVal("region_nm", rs);
		pathName = db.getStringVal("path_nm", rs);
		statusName = db.getStringVal("status_nm", rs);
	}

	/**
	 * @return the regulatorId
	 */
	@Column(name="regulatory_id", isPrimaryKey=true)
	public String getRegulatorId() {
		return regulatorId;
	}

	/**
	 * @return the regionId
	 */
	@Column(name="region_id")
	public String getRegionId() {
		return regionId;
	}

	@Column(name="region_nm", isReadOnly=true)
	public String getRegionName() {
		return regionName;
	}

	/**
	 * @return the pathId
	 */
	@Column(name="path_id")
	public String getPathId() {
		return pathId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the statusId
	 */
	@Column(name="status_id")
	public String getStatusId() {
		return statusId;
	}

	@Column(name="path_nm", isReadOnly=true)
	public String getPathName() {
		return pathName;
	}

	@Column(name="status_nm", isReadOnly=true)
	public String getStatusName() {
		return statusName;
	}


	/**
	 * @param regulatorId the regulatorId to set.
	 */
	public void setRegulatorId(String regulatorId) {
		this.regulatorId = regulatorId;
	}

	/**
	 * @param regionId the regionId to set.
	 */
	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	/**
	 * @param pathId the pathId to set.
	 */
	public void setPathId(String pathId) {
		this.pathId = pathId;
	}

	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param statusId the statusId to set.
	 */
	public void setStatusId(String statusId) {
		this.statusId = statusId;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}
	
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}
	

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}
}