/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

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
public class RegulationVO {

	private String regulatorId;
	private String regionId;
	private String pathId;
	private String productId;
	private String statusId;

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
	}

	/**
	 * @return the regulatorId
	 */
	public String getRegulatorId() {
		return regulatorId;
	}

	/**
	 * @return the regionId
	 */
	public String getRegionId() {
		return regionId;
	}

	/**
	 * @return the pathId
	 */
	public String getPathId() {
		return pathId;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the statusId
	 */
	public String getStatusId() {
		return statusId;
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
}