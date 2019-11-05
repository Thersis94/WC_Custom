package com.biomed.smarttrak.admin.vo;

import com.siliconmtn.db.orm.Column;

public class GridUsageVO {
	
	private String itemId;
	private String itemType;
	private String itemName;
	private String xrId;
	private String xrName;
	
	
	/**
	 * @return the itemId
	 */
	@Column(name="item_id")
	public String getItemId() {
		return itemId;
	}
	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	/**
	 * @return the itemType
	 */
	@Column(name="type")
	public String getItemType() {
		return itemType;
	}
	/**
	 * @param itemType the itemType to set
	 */
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}
	/**
	 * @return the itemName
	 */
	@Column(name="item_nm")
	public String getItemName() {
		return itemName;
	}
	/**
	 * @param itemName the itemName to set
	 */
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	/**
	 * @return the xrId
	 */
	@Column(name="xr_id")
	public String getXrId() {
		return xrId;
	}
	/**
	 * @param xrId the xrId to set
	 */
	public void setXrId(String xrId) {
		this.xrId = xrId;
	}
	/**
	 * @return the xrN1ame
	 */
	@Column(name="xr_nm")
	public String getXRName() {
		return xrName;
	}
	/**
	 * @param xrN1ame the xrN1ame to set
	 */
	public void setXRName(String xrName) {
		this.xrName = xrName;
	}

}
