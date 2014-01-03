package com.ram.action.data;

// JDK 1.6.x
import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: InvertoryItemVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Stores the data elements for an individual inventory
 * item in the Ram Group inventory management application.  Each inventory
 * item is received form a physical inventory activity at a customer location
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 7, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InvertoryItemVO implements Serializable {
	// Member Variables
	private String inventoryItemId = null;
	private String inventoryEventId = null;
	private String productId = null;
	private String lotNumber = null;
	private String serialNumber = null;
	private String areaName = null;
	private Integer aisle = Integer.valueOf(0);
	private Integer row = Integer.valueOf(0);
	private Integer tier = Integer.valueOf(0);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public InvertoryItemVO() {
		
	}

	/**
	 * @return the inventoryItemId
	 */
	public String getInventoryItemId() {
		return inventoryItemId;
	}

	/**
	 * @param inventoryItemId the inventoryItemId to set
	 */
	public void setInventoryItemId(String inventoryItemId) {
		this.inventoryItemId = inventoryItemId;
	}

	/**
	 * @return the inventoryEventId
	 */
	public String getInventoryEventId() {
		return inventoryEventId;
	}

	/**
	 * @param inventoryEventId the inventoryEventId to set
	 */
	public void setInventoryEventId(String inventoryEventId) {
		this.inventoryEventId = inventoryEventId;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @return the lotNumber
	 */
	public String getLotNumber() {
		return lotNumber;
	}

	/**
	 * @param lotNumber the lotNumber to set
	 */
	public void setLotNumber(String lotNumber) {
		this.lotNumber = lotNumber;
	}

	/**
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @return the areaName
	 */
	public String getAreaName() {
		return areaName;
	}

	/**
	 * @param areaName the areaName to set
	 */
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	/**
	 * @return the aisle
	 */
	public Integer getAisle() {
		return aisle;
	}

	/**
	 * @param aisle the aisle to set
	 */
	public void setAisle(Integer aisle) {
		this.aisle = aisle;
	}

	/**
	 * @return the row
	 */
	public Integer getRow() {
		return row;
	}

	/**
	 * @param row the row to set
	 */
	public void setRow(Integer row) {
		this.row = row;
	}

	/**
	 * @return the tier
	 */
	public Integer getTier() {
		return tier;
	}

	/**
	 * @param tier the tier to set
	 */
	public void setTier(Integer tier) {
		this.tier = tier;
	}

}
